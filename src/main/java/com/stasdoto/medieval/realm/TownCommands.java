package com.stasdoto.medieval.realm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import com.stasdoto.medieval.construct.SettlementFeature;
import com.stasdoto.medieval.command.MedievalCommands;
import com.stasdoto.medieval.earth.Terrain;
import com.stasdoto.medieval.history.Culture;
import com.stasdoto.medieval.people.Names;
import com.stasdoto.medieval.people.People;
import com.stasdoto.medieval.people.PersonEntity;
import com.stasdoto.medieval.people.Role;
import com.stasdoto.medieval.settle.Layout;
import com.stasdoto.medieval.settle.Planner;
import com.stasdoto.medieval.settle.Site;
import com.stasdoto.medieval.settle.Sites;

/**
 * Ruling a settlement from the chat: everything is a clickable card ("/medieval town" in or by a settlement).
 * Become its lord (buy the charter or take it by force), set taxes, send people to work, order buildings, take silver
 * from the treasury, trade at its market, raise warriors who follow you.
 * Silver is counted in pennies; the player pays and is paid in emeralds (1 emerald = 10 pennies).
 */
public final class TownCommands {
	private TownCommands() {
	}

	public static final int PENNIES_PER_EMERALD = 10;
	private static final Map<Long, String> NOTIFIED = new HashMap<>();

	public static LiteralArgumentBuilder<CommandSourceStack> node() {
		return Commands.literal("town")
				.executes(ctx -> card(ctx.getSource()))
				.then(Commands.literal("tax").then(Commands.argument("delta", IntegerArgumentType.integer(-40, 40))
						.executes(ctx -> tax(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "delta")))))
				.then(Commands.literal("job").then(Commands.argument("job", StringArgumentType.word())
						.then(Commands.argument("delta", IntegerArgumentType.integer(-500, 500))
								.executes(ctx -> job(ctx.getSource(), StringArgumentType.getString(ctx, "job"), IntegerArgumentType.getInteger(ctx, "delta"))))))
				.then(Commands.literal("build").executes(ctx -> buildMenu(ctx.getSource()))
						.then(Commands.argument("what", StringArgumentType.word())
								.executes(ctx -> build(ctx.getSource(), StringArgumentType.getString(ctx, "what")))))
				.then(Commands.literal("charter").executes(ctx -> charter(ctx.getSource())))
				.then(Commands.literal("seize").executes(ctx -> seize(ctx.getSource())))
				.then(Commands.literal("take").then(Commands.argument("emeralds", IntegerArgumentType.integer(1, 640))
						.executes(ctx -> take(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "emeralds")))))
				.then(Commands.literal("recruit").then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
						.executes(ctx -> recruit(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))))
				.then(Commands.literal("market").executes(ctx -> market(ctx.getSource()))
						.then(Commands.argument("deal", StringArgumentType.word()).then(Commands.argument("good", StringArgumentType.word())
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 640))
										.executes(ctx -> trade(ctx.getSource(), StringArgumentType.getString(ctx, "deal"),
												StringArgumentType.getString(ctx, "good"), IntegerArgumentType.getInteger(ctx, "count")))))))
				.then(Commands.literal("chronicle").executes(ctx -> chronicle(ctx.getSource())));
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 200 != 0) {
				return;
			}
			try {
				tick(server);
			} catch (RuntimeException e) {
				com.stasdoto.medieval.Medieval.LOGGER.error("[medieval] town tick failed", e);
			}
		});
	}

	// ---- helpers ----

	private static Site siteAt(ServerPlayer p) {
		int x = p.getBlockX(), z = p.getBlockZ();
		Site best = null;
		double bd = Double.MAX_VALUE;
		for (Site s : Sites.near(x, z)) {
			double d = Math.hypot(x - s.x, z - s.z);
			if (d < s.fieldRadius() + 40 && d < bd) {
				bd = d;
				best = s;
			}
		}
		return best;
	}

	private record Here(ServerPlayer player, Site site, Town town, Towns towns) {
		boolean mine() {
			return town.owner.equals(player.getUUID().toString());
		}
	}

	private static Here here(CommandSourceStack src) {
		ServerPlayer p = src.getPlayer();
		if (p == null) {
			return null;
		}
		ServerLevel earth = MedievalCommands.earth(src.getServer());
		if (p.level() != earth) {
			p.sendSystemMessage(Component.literal("Поселения есть только в мире Земли: /medieval go kyiv").withStyle(ChatFormatting.GRAY));
			return null;
		}
		Site s = siteAt(p);
		if (s == null) {
			p.sendSystemMessage(Component.literal("Рядом нет поселения. Подойди к селу или городу (до края его полей).").withStyle(ChatFormatting.GRAY));
			return null;
		}
		Towns ts = Towns.get(src.getServer());
		return new Here(p, s, ts.town(src.getServer(), s), ts);
	}

	static MutableComponent button(String label, String command, String hover, ChatFormatting color) {
		return Component.literal("[" + label + "]").withStyle(st -> st.withColor(color).withClickEvent(new ClickEvent.RunCommand(command))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
	}

	private static MutableComponent line(String text, ChatFormatting color) {
		return Component.literal(text).withStyle(color);
	}

	private static int emeralds(ServerPlayer p) {
		int n = 0;
		var inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack st = inv.getItem(i);
			if (st.is(Items.EMERALD)) {
				n += st.getCount();
			}
		}
		return n;
	}

	private static boolean pay(ServerPlayer p, int count) {
		if (p.isCreative()) {
			return true;
		}
		if (emeralds(p) < count) {
			return false;
		}
		var inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize() && count > 0; i++) {
			ItemStack st = inv.getItem(i);
			if (st.is(Items.EMERALD)) {
				int k = Math.min(count, st.getCount());
				st.shrink(k);
				count -= k;
			}
		}
		return true;
	}

	private static void give(ServerPlayer p, Item item, int count) {
		while (count > 0) {
			int k = Math.min(count, item.getDefaultMaxStackSize());
			ItemStack st = new ItemStack(item, k);
			if (!p.getInventory().add(st)) {
				p.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), st));
			}
			count -= k;
		}
	}

	private static int count(ServerPlayer p, Item item) {
		int n = 0;
		var inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (inv.getItem(i).is(item)) {
				n += inv.getItem(i).getCount();
			}
		}
		return n;
	}

	private static void takeItems(ServerPlayer p, Item item, int count) {
		var inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize() && count > 0; i++) {
			ItemStack st = inv.getItem(i);
			if (st.is(item)) {
				int k = Math.min(count, st.getCount());
				st.shrink(k);
				count -= k;
			}
		}
	}

	static Item item(Res r) {
		return switch (r) {
			case FOOD -> Items.BREAD;
			case WOOD -> Items.OAK_LOG;
			case STONE -> Items.COBBLESTONE;
			case IRON -> Items.IRON_INGOT;
			case TOOLS -> Items.IRON_AXE;
			case WEAPONS -> Items.IRON_SWORD;
			case ARMOR -> Items.CHAINMAIL_CHESTPLATE;
			case CLOTH -> Items.STRING;
			case HIDES -> Items.LEATHER;
			case FURS -> Items.RABBIT_HIDE;
			case SALT -> Items.SUGAR;
			case HONEY -> Items.HONEY_BOTTLE;
			case HORSES -> Items.SADDLE;
		};
	}

	/** Charter price in emeralds. */
	static int charterPrice(Town t) {
		return Math.max(8, (int) Math.round(t.population * 0.6 + t.treasury / PENNIES_PER_EMERALD * 0.5));
	}

	// ---- the card ----

	private static int card(CommandSourceStack src) {
		Here h = here(src);
		if (h == null) {
			return 0;
		}
		Town t = h.town;
		ServerPlayer p = h.player;
		Site s = h.site;
		Culture c = s.culture;
		p.sendSystemMessage(line("═════ " + s.kind.title.toUpperCase() + " " + t.name + " ═════", ChatFormatting.GOLD));
		p.sendSystemMessage(line(c.title + (s.place != null ? " · " + s.place.realm() : "") + " · день " + t.day + ", " + Economy.seasonName(t.day),
				ChatFormatting.GRAY));
		String lord = t.owner.isEmpty() ? (s.place != null ? "князь/правитель " + s.place.realm() : "местный господин") : t.ownerName;
		p.sendSystemMessage(Component.literal("Господин: ").withStyle(ChatFormatting.YELLOW).append(line(lord, ChatFormatting.WHITE)));
		p.sendSystemMessage(Component.literal(String.format("Жителей %d · настроение %d/100 · налог %d%% · казна %.0f сер. · воинов %d",
				t.population, Math.round(t.mood), t.taxPercent, t.treasury, t.jobs.get(Job.WARRIORS))).withStyle(ChatFormatting.WHITE));
		StringBuilder st = new StringBuilder("Запасы: ");
		for (Res r : Res.values()) {
			double v = t.get(r);
			if (v >= 1) {
				st.append(r.title).append(' ').append(Math.round(v)).append(", ");
			}
		}
		p.sendSystemMessage(line(st.substring(0, st.length() - 2), ChatFormatting.AQUA));
		StringBuilder jb = new StringBuilder("Работы: ");
		for (Job j : Job.values()) {
			if (t.jobs.get(j) > 0) {
				jb.append(j.title).append(' ').append(t.jobs.get(j)).append(", ");
			}
		}
		p.sendSystemMessage(line(jb.substring(0, jb.length() - 2), ChatFormatting.GREEN));
		for (Town.Project pr : t.projects) {
			if (pr.progress < pr.work) {
				p.sendSystemMessage(line(String.format("Строится: %s — %d%%", Buildings.title(pr.kind), Math.round(100 * pr.progress / pr.work)),
						ChatFormatting.LIGHT_PURPLE));
			}
		}
		MutableComponent row = Component.empty();
		if (!h.mine()) {
			row.append(button("Купить грамоту: " + charterPrice(t) + " изумр.", "/medieval town charter",
					"Выкупить у прежнего господина право владеть этим поселением", ChatFormatting.GOLD)).append(" ");
			row.append(button("Взять силой", "/medieval town seize", "Если гарнизон разбит — поселение ваше", ChatFormatting.RED)).append(" ");
		} else {
			row.append(button("Налог −5%", "/medieval town tax -5", "Люди довольнее, казна растёт медленнее", ChatFormatting.GREEN)).append(" ");
			row.append(button("Налог +5%", "/medieval town tax 5", "Больше серебра, люди недовольнее", ChatFormatting.YELLOW)).append(" ");
			row.append(button("Работы", "/medieval town job list 0", "Отправить людей на работы", ChatFormatting.AQUA)).append(" ");
			row.append(button("Стройка", "/medieval town build", "Заказать новые постройки", ChatFormatting.LIGHT_PURPLE)).append(" ");
			row.append(button("Взять 10 изумр.", "/medieval town take 10", "Забрать серебро из казны", ChatFormatting.GOLD)).append(" ");
			row.append(button("Набрать 5 воинов", "/medieval town recruit 5", "Воины пойдут за вами (нужно оружие на складе)", ChatFormatting.RED)).append(" ");
		}
		row.append(button("Торг", "/medieval town market", "Купить или продать товары", ChatFormatting.WHITE)).append(" ");
		row.append(button("Летопись", "/medieval town chronicle", "Что случилось в поселении", ChatFormatting.GRAY));
		p.sendSystemMessage(row);
		return 1;
	}

	private static boolean mustOwn(Here h) {
		if (!h.mine()) {
			h.player.sendSystemMessage(line("Это не ваше поселение. Сначала станьте его господином.", ChatFormatting.RED));
			return false;
		}
		return true;
	}

	private static int tax(CommandSourceStack src, int delta) {
		Here h = here(src);
		if (h == null || !mustOwn(h)) {
			return 0;
		}
		h.town.taxPercent = Math.max(0, Math.min(40, h.town.taxPercent + delta));
		h.towns.setDirty();
		h.player.sendSystemMessage(line("Налог теперь " + h.town.taxPercent + "%.", ChatFormatting.YELLOW));
		return 1;
	}

	private static int job(CommandSourceStack src, String name, int delta) {
		Here h = here(src);
		if (h == null || !mustOwn(h)) {
			return 0;
		}
		Town t = h.town;
		if (!"list".equals(name)) {
			Job j;
			try {
				j = Job.valueOf(name.toUpperCase());
			} catch (IllegalArgumentException e) {
				h.player.sendSystemMessage(line("Нет такой работы.", ChatFormatting.RED));
				return 0;
			}
			if (j == Job.WARRIORS) {
				h.player.sendSystemMessage(line("Воинов набирают кнопкой «Набрать воинов».", ChatFormatting.GRAY));
				return 0;
			}
			if (delta > 0) {
				int move = Math.min(delta, t.jobs.get(Job.FARMERS));
				t.jobs.put(Job.FARMERS, t.jobs.get(Job.FARMERS) - move);
				t.jobs.put(j, t.jobs.get(j) + move);
			} else {
				int move = Math.min(-delta, t.jobs.get(j));
				t.jobs.put(j, t.jobs.get(j) - move);
				t.jobs.put(Job.FARMERS, t.jobs.get(Job.FARMERS) + move);
			}
			h.towns.setDirty();
		}
		h.player.sendSystemMessage(line("Кого куда послать (люди берутся из пахарей и возвращаются к ним):", ChatFormatting.AQUA));
		for (Job j : Job.values()) {
			if (j == Job.FARMERS || j == Job.WARRIORS) {
				continue;
			}
			MutableComponent row = Component.literal(String.format("%-22s %4d  ", j.title, t.jobs.get(j))).withStyle(ChatFormatting.WHITE);
			row.append(button("−5", "/medieval town job " + j.name().toLowerCase() + " -5", "Вернуть 5 человек в поле", ChatFormatting.RED)).append(" ");
			row.append(button("+5", "/medieval town job " + j.name().toLowerCase() + " 5", "Послать ещё 5 человек", ChatFormatting.GREEN));
			h.player.sendSystemMessage(row);
		}
		h.player.sendSystemMessage(line("Пахарей: " + t.jobs.get(Job.FARMERS), ChatFormatting.GREEN));
		return 1;
	}

	private static int buildMenu(CommandSourceStack src) {
		Here h = here(src);
		if (h == null || !mustOwn(h)) {
			return 0;
		}
		h.player.sendSystemMessage(line("Что построить (материалы берутся со склада по ходу работ, строят строители):", ChatFormatting.LIGHT_PURPLE));
		for (Buildings.Order o : Buildings.ALL) {
			MutableComponent row = Component.literal(String.format("%s — лес %d, камень %d, железо %d · %s  ", o.title(), o.wood(), o.stone(), o.iron(),
					o.effect())).withStyle(ChatFormatting.WHITE);
			row.append(button("Заказать", "/medieval town build " + o.key(), "Начать стройку", ChatFormatting.LIGHT_PURPLE));
			h.player.sendSystemMessage(row);
		}
		return 1;
	}

	private static int build(CommandSourceStack src, String key) {
		Here h = here(src);
		if (h == null || !mustOwn(h)) {
			return 0;
		}
		Buildings.Order o = Buildings.find(key);
		if (o == null) {
			h.player.sendSystemMessage(line("Нет такой постройки.", ChatFormatting.RED));
			return 0;
		}
		Town t = h.town;
		t.projects.add(Buildings.project(o));
		if (t.jobs.get(Job.BUILDERS) < 5) {
			int move = Math.min(8, t.jobs.get(Job.FARMERS));
			t.jobs.put(Job.FARMERS, t.jobs.get(Job.FARMERS) - move);
			t.jobs.put(Job.BUILDERS, t.jobs.get(Job.BUILDERS) + move);
		}
		t.log("заложена стройка: " + o.title());
		h.towns.setDirty();
		h.player.sendSystemMessage(line("Заложено: " + o.title() + ". Строителей: " + t.jobs.get(Job.BUILDERS)
				+ ". Когда достроят, здание появится на краю поселения.", ChatFormatting.LIGHT_PURPLE));
		return 1;
	}

	private static int charter(CommandSourceStack src) {
		Here h = here(src);
		if (h == null) {
			return 0;
		}
		if (h.mine()) {
			h.player.sendSystemMessage(line("Это поселение уже ваше.", ChatFormatting.GRAY));
			return 0;
		}
		int price = charterPrice(h.town);
		if (!pay(h.player, price)) {
			h.player.sendSystemMessage(line("Нужно " + price + " изумрудов (у вас " + emeralds(h.player) + ").", ChatFormatting.RED));
			return 0;
		}
		own(h, "купил грамоту на владение");
		return 1;
	}

	private static void own(Here h, String how) {
		h.town.owner = h.player.getUUID().toString();
		h.town.ownerName = h.player.getName().getString();
		h.town.log(h.town.ownerName + " " + how);
		h.towns.setDirty();
		h.player.sendSystemMessage(line("Теперь вы господин поселения " + h.town.name + "!", ChatFormatting.GOLD));
		card(h.player.createCommandSourceStack());
	}

	private static int seize(CommandSourceStack src) {
		Here h = here(src);
		if (h == null) {
			return 0;
		}
		if (h.mine()) {
			h.player.sendSystemMessage(line("Это поселение уже ваше.", ChatFormatting.GRAY));
			return 0;
		}
		ServerLevel lvl = (ServerLevel) h.player.level();
		int r = h.site.radius + 60;
		List<PersonEntity> guards = lvl.getEntitiesOfClass(PersonEntity.class, new AABB(h.site.x - r, -600, h.site.z - r, h.site.x + r, 2100, h.site.z + r),
				pe -> pe.isAlive() && pe.role() == Role.WARRIOR && pe.lord == null && h.town.name.equals(pe.settlement));
		if (!guards.isEmpty() && !h.player.isCreative()) {
			h.player.sendSystemMessage(line("Гарнизон ещё держится: " + guards.size() + " воинов. Разбейте их, тогда поселение сдастся.", ChatFormatting.RED));
			return 0;
		}
		h.town.mood = Math.max(0, h.town.mood - 20);
		h.town.jobs.put(Job.WARRIORS, 0);
		own(h, "взял поселение силой");
		return 1;
	}

	private static int take(CommandSourceStack src, int n) {
		Here h = here(src);
		if (h == null || !mustOwn(h)) {
			return 0;
		}
		int can = (int) Math.min(n, h.town.treasury / PENNIES_PER_EMERALD);
		if (can <= 0) {
			h.player.sendSystemMessage(line("Казна пуста.", ChatFormatting.RED));
			return 0;
		}
		h.town.treasury -= can * PENNIES_PER_EMERALD;
		give(h.player, Items.EMERALD, can);
		h.towns.setDirty();
		h.player.sendSystemMessage(line("Взято из казны: " + can + " изумр.", ChatFormatting.GOLD));
		return can;
	}

	private static int recruit(CommandSourceStack src, int n) {
		Here h = here(src);
		if (h == null || !mustOwn(h)) {
			return 0;
		}
		Town t = h.town;
		int can = Math.min(n, Math.min((int) t.get(Res.WEAPONS), t.jobs.get(Job.FARMERS)));
		if (h.player.isCreative()) {
			can = n;
		}
		if (can <= 0) {
			h.player.sendSystemMessage(line("Нет оружия на складе (или людей). Поставьте кузнецов и рудокопов.", ChatFormatting.RED));
			return 0;
		}
		t.add(Res.WEAPONS, -can);
		t.jobs.put(Job.FARMERS, Math.max(0, t.jobs.get(Job.FARMERS) - can));
		ServerLevel lvl = (ServerLevel) h.player.level();
		Culture c = h.site.culture;
		java.util.Random r = new java.util.Random();
		for (int i = 0; i < can; i++) {
			PersonEntity w = People.PERSON.create(lvl, EntitySpawnReason.MOB_SUMMONED);
			if (w == null) {
				continue;
			}
			int look = r.nextInt();
			w.setup(look, c, Role.WARRIOR, false, BlockPos.containing(h.site.x, h.player.getY(), h.site.z), t.name, Names.of(c, false, look));
			w.lord = h.player.getUUID();
			w.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(com.stasdoto.medieval.items.Weapons.forCulture(c, r)));
			if (t.get(Res.ARMOR) >= 1) {
				t.add(Res.ARMOR, -1);
				w.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
				w.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
			}
			w.snapTo(h.player.getX() + r.nextInt(7) - 3, h.player.getY(), h.player.getZ() + r.nextInt(7) - 3, r.nextFloat() * 360, 0);
			lvl.addFreshEntity(w);
		}
		t.log(t.ownerName + " набрал дружину: " + can + " воинов");
		h.towns.setDirty();
		h.player.sendSystemMessage(line("За вами идут " + can + " воинов из " + t.name + ".", ChatFormatting.RED));
		return can;
	}

	private static int market(CommandSourceStack src) {
		Here h = here(src);
		if (h == null) {
			return 0;
		}
		Town t = h.town;
		h.player.sendSystemMessage(line("Торг в " + t.name + " (цены в серебре; 1 изумруд = " + PENNIES_PER_EMERALD + " сер.). У вас " + emeralds(h.player)
				+ " изумр.", ChatFormatting.GOLD));
		for (Res r : Res.values()) {
			double pr = Economy.price(t, r);
			MutableComponent row = Component.literal(String.format("%-16s есть %5d · цена %.1f  ", r.title, Math.round(t.get(r)), pr)).withStyle(ChatFormatting.WHITE);
			int buyN = (int) Math.max(1, Math.round(PENNIES_PER_EMERALD * 5 / pr));
			row.append(button("Купить " + buyN, "/medieval town market buy " + r.name().toLowerCase() + " " + buyN, "За 5 изумрудов", ChatFormatting.GREEN)).append(" ");
			row.append(button("Продать 10", "/medieval town market sell " + r.name().toLowerCase() + " 10", "Отдать 10 штук на склад за серебро", ChatFormatting.YELLOW));
			h.player.sendSystemMessage(row);
		}
		return 1;
	}

	private static int trade(CommandSourceStack src, String deal, String good, int n) {
		Here h = here(src);
		if (h == null) {
			return 0;
		}
		Res r;
		try {
			r = Res.valueOf(good.toUpperCase());
		} catch (IllegalArgumentException e) {
			return 0;
		}
		Town t = h.town;
		double price = Economy.price(t, r);
		if ("buy".equals(deal)) {
			n = (int) Math.min(n, t.get(r));
			if (n <= 0) {
				h.player.sendSystemMessage(line("Этого товара нет на складе.", ChatFormatting.RED));
				return 0;
			}
			int cost = (int) Math.ceil(n * price / PENNIES_PER_EMERALD);
			if (!pay(h.player, cost)) {
				h.player.sendSystemMessage(line("Нужно " + cost + " изумр.", ChatFormatting.RED));
				return 0;
			}
			t.add(r, -n);
			t.treasury += cost * PENNIES_PER_EMERALD * 0.2;
			if (r == Res.HORSES) {
				ServerLevel lvl = (ServerLevel) h.player.level();
				for (int i = 0; i < n; i++) {
					var horse = net.minecraft.world.entity.EntityTypes.HORSE.create(lvl, EntitySpawnReason.TRIGGERED);
					if (horse != null) {
						horse.snapTo(h.player.getX() + 2, h.player.getY(), h.player.getZ() + 2, 0, 0);
						lvl.addFreshEntity(horse);
					}
				}
				give(h.player, Items.SADDLE, n);
			} else {
				give(h.player, item(r), n);
			}
			h.player.sendSystemMessage(line("Куплено: " + r.title + " × " + n + " за " + cost + " изумр.", ChatFormatting.GREEN));
		} else {
			Item it = item(r);
			int have = h.player.isCreative() ? n : count(h.player, it);
			n = Math.min(n, have);
			if (n <= 0 || r == Res.HORSES) {
				h.player.sendSystemMessage(line("Вам нечего продать.", ChatFormatting.RED));
				return 0;
			}
			takeItems(h.player, it, n);
			int earn = (int) Math.floor(n * price * 0.8 / PENNIES_PER_EMERALD);
			t.add(r, n);
			give(h.player, Items.EMERALD, Math.max(earn, 0));
			h.player.sendSystemMessage(line("Продано: " + r.title + " × " + n + " за " + earn + " изумр.", ChatFormatting.YELLOW));
		}
		h.towns.setDirty();
		return 1;
	}

	private static int chronicle(CommandSourceStack src) {
		Here h = here(src);
		if (h == null) {
			return 0;
		}
		h.player.sendSystemMessage(line("Летопись " + h.town.name + ":", ChatFormatting.GOLD));
		if (h.town.chronicle.isEmpty()) {
			h.player.sendSystemMessage(line("Пока ничего примечательного.", ChatFormatting.GRAY));
		}
		for (String s : h.town.chronicle.subList(0, Math.min(12, h.town.chronicle.size()))) {
			h.player.sendSystemMessage(line(s, ChatFormatting.GRAY));
		}
		return 1;
	}

	// ---- the passing of days ----

	private static final Map<Long, Long> RAID_DAY = new HashMap<>();

	private static void tick(MinecraftServer server) {
		Towns ts = Towns.get(server);
		long today = Towns.today(server);
		ServerLevel earth = MedievalCommands.earth(server);
		Raids.watch(earth, ts);
		for (Town t : ts.towns.values()) {
			if (t.owner.isEmpty()) {
				continue;
			}
			Economy.advance(t, today);
			Long lastRaidCheck = RAID_DAY.put(t.id, today);
			if (lastRaidCheck != null && lastRaidCheck < today) {
				ServerPlayer lord = server.getPlayerList().getPlayer(java.util.UUID.fromString(t.owner));
				Raids.daily(earth, t, lord, new java.util.Random(t.id * 7 + today));
			}
			ts.setDirty();
			ServerPlayer owner = server.getPlayerList().getPlayer(java.util.UUID.fromString(t.owner));
			// News for the lord.
			if (!t.chronicle.isEmpty()) {
				String head = t.chronicle.get(0);
				String seen = NOTIFIED.put(t.id, head);
				if (owner != null && seen != null && !seen.equals(head)) {
					owner.sendSystemMessage(Component.literal("[" + t.name + "] ").withStyle(ChatFormatting.GOLD).append(line(head, ChatFormatting.WHITE)));
				}
			}
			// Attracting people from unhappy neighbours.
			if (t.mood > 70 && t.taxPercent <= 10 && today % 3 == 0) {
				for (Town o : ts.towns.values()) {
					if (o != t && o.mood < 45 && o.population > 30 && Math.hypot(o.x - t.x, o.z - t.z) < 30000) {
						int move = Math.max(1, o.population / 100);
						o.population -= move;
						t.population += move;
						t.log("из " + o.name + " переселились " + move + " человек");
						break;
					}
				}
			}
			// Finished buildings go up on the edge of the settlement.
			for (Town.Project pr : t.projects) {
				if (pr.progress >= pr.work && !pr.placed) {
					if (placeProject(earth, t, pr)) {
						pr.placed = true;
						if (owner != null) {
							owner.sendSystemMessage(Component.literal("[" + t.name + "] ").withStyle(ChatFormatting.GOLD)
									.append(line("готово: " + Buildings.title(pr.kind), ChatFormatting.LIGHT_PURPLE)));
						}
					}
				}
			}
		}
	}

	private static boolean placeProject(ServerLevel earth, Town t, Town.Project pr) {
		Buildings.Order o = Buildings.find(pr.kind);
		if (o == null) {
			return true;
		}
		Site site = null;
		for (Site s : Sites.near(t.x, t.z)) {
			if (s.id == t.id) {
				site = s;
			}
		}
		if (site == null) {
			return true;
		}
		Layout lay = Planner.layout(site);
		// Rings round the centre from the edge of the houses outwards; the first flat, dry, free spot.
		int w = o.kind().w, d = o.kind().d;
		for (int ring = Math.max(20, lay.fieldInner - 30); ring < lay.fieldInner + 80; ring += 8) {
			for (int k = 0; k < 24; k++) {
				double a = k * Math.PI * 2 / 24 + ring * 0.37;
				int cx = site.x + (int) (Math.cos(a) * ring), cz = site.z + (int) (Math.sin(a) * ring);
				int rot = Math.abs(Math.cos(a)) > Math.abs(Math.sin(a)) ? (Math.cos(a) > 0 ? 1 : 3) : (Math.sin(a) > 0 ? 2 : 0);
				int ex = rot % 2 == 0 ? w : d, ez = rot % 2 == 0 ? d : w;
				int x0 = cx - ex / 2, z0 = cz - ez / 2, x1 = x0 + ex - 1, z1 = z0 + ez - 1;
				boolean clear = true;
				for (Layout.Lot l : lay.lots) {
					if (l.touches(x0, z0, x1, z1, 3)) {
						clear = false;
						break;
					}
				}
				if (!clear) {
					continue;
				}
				int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
				boolean dry = true;
				for (int i = 0; i <= 2 && dry; i++) {
					for (int j = 0; j <= 2; j++) {
						Terrain.Col c = Terrain.column(x0 + (x1 - x0) * i / 2, z0 + (z1 - z0) * j / 2);
						if (c.waterTop != Terrain.NO_WATER) {
							dry = false;
							break;
						}
						lo = Math.min(lo, c.top());
						hi = Math.max(hi, c.top());
					}
				}
				if (!dry || hi - lo > 6) {
					continue;
				}
				// The chunks must be there to build in.
				for (int x = x0 >> 4; x <= x1 >> 4; x++) {
					for (int z = z0 >> 4; z <= z1 >> 4; z++) {
						earth.getChunk(x, z);
					}
				}
				Layout.Lot lot = Layout.newLot(x0, z0, x1, z1, rot, (lo + hi) / 2, o.kind(), t.id * 31 + t.projects.indexOf(pr));
				lay.lots.add(lot);
				SettlementFeature.buildNow(earth, lay, lot);
				t.log("построено: " + o.title());
				return true;
			}
		}
		return false;
	}
}
