package com.stasdoto.medieval.realm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.stasdoto.medieval.earth.Terrain;
import com.stasdoto.medieval.history.Culture;
import com.stasdoto.medieval.people.Names;
import com.stasdoto.medieval.people.People;
import com.stasdoto.medieval.people.PersonEntity;
import com.stasdoto.medieval.people.Role;

/**
 * Raids on the lord's settlements: Cuman horsemen from the steppe on Rus and the Danube lands, Norse sea raiders on
 * the coasts and the North, a rival count's men in the West, Bedouin and Turkmen in the Islamic lands. When the lord is
 * near, the raiders really come and must be beaten; when he is away, the garrison fights them alone (by numbers).
 */
public final class Raids {
	private Raids() {
	}

	private record Raid(long town, List<UUID> men, long until) {
	}

	private static final Map<Long, Raid> ACTIVE = new HashMap<>();

	static Culture raiders(Town t) {
		Culture c = Culture.valueOf(t.culture);
		return switch (c) {
			case RUS, MAGYAR, BYZANTINE, WEST_SLAV, CAUCASUS -> Culture.STEPPE;
			case LATIN, CELTIC, BALT_FINN -> t.coast > 0 || t.north > 0 ? Culture.NORSE : Culture.LATIN;
			case NORSE -> Culture.NORSE;
			case ANDALUSI -> Culture.LATIN;
			case ARAB, PERSIAN -> Culture.STEPPE;
			case STEPPE -> Culture.RUS;
		};
	}

	static String raidersName(Culture c) {
		return switch (c) {
			case STEPPE -> "половцы";
			case NORSE -> "викинги";
			case LATIN -> "воины соседнего графа";
			case RUS -> "русская дружина";
			default -> "разбойники";
		};
	}

	/** Called once a day for each lord's settlement. */
	static void daily(ServerLevel earth, Town t, ServerPlayer owner, Random r) {
		if (ACTIVE.containsKey(t.id)) {
			return;
		}
		double chance = 0.035 + (t.population > 300 ? 0.01 : 0) + (Culture.valueOf(t.culture) == Culture.RUS ? 0.015 : 0);
		if (r.nextDouble() > chance) {
			return;
		}
		Culture who = raiders(t);
		int men = 4 + Math.min(16, t.population / 40) + r.nextInt(4);
		boolean near = owner != null && owner.level() == earth && Math.hypot(owner.getX() - t.x, owner.getZ() - t.z) < 600;
		if (!near) {
			// Fought out by the garrison alone.
			int guards = t.jobs.get(Job.WARRIORS);
			double armour = Math.min(1.0, t.get(Res.ARMOR) / Math.max(1, guards));
			double ours = guards * (1.0 + armour) + t.population * 0.02;
			if (ours >= men * 1.2) {
				int lost = Math.min(guards, r.nextInt(Math.max(1, men / 3) + 1));
				t.jobs.put(Job.WARRIORS, guards - lost);
				t.mood += 6;
				t.log(raidersName(who) + " напали, но гарнизон отбил набег (павших: " + lost + ")");
			} else {
				loot(t, r);
				t.jobs.put(Job.WARRIORS, Math.max(0, guards - men / 2));
				t.log(raidersName(who) + " разорили поселение: увели людей, разграбили склады и казну");
			}
			return;
		}
		// The raiders come for real, from a direction away from the lord.
		double a = r.nextDouble() * Math.PI * 2;
		int dist = 110;
		int sx = t.x + (int) (Math.cos(a) * dist), sz = t.z + (int) (Math.sin(a) * dist);
		Terrain.Col c = Terrain.column(sx, sz);
		int sy = Math.max(c.top(), c.waterTop == Terrain.NO_WATER ? c.top() : c.waterTop) + 1;
		earth.getChunk(sx >> 4, sz >> 4);
		List<UUID> ids = new ArrayList<>();
		for (int i = 0; i < men; i++) {
			PersonEntity e = People.PERSON.create(earth, EntitySpawnReason.EVENT);
			if (e == null) {
				continue;
			}
			int look = r.nextInt();
			e.setup(look, who, Role.WARRIOR, false, new BlockPos(sx, sy, sz), "", Names.of(who, false, look));
			e.enemyOf = t.owner;
			e.raidOf = t.name;
			e.raidTarget = new BlockPos(t.x, Terrain.column(t.x, t.z).top() + 1, t.z);
			e.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(com.stasdoto.medieval.items.Weapons.forCulture(who, r)));
			if (r.nextInt(3) == 0) {
				e.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
			}
			e.snapTo(sx + r.nextInt(9) - 4, sy, sz + r.nextInt(9) - 4, r.nextFloat() * 360, 0);
			earth.addFreshEntity(e);
			ids.add(e.getUUID());
		}
		ACTIVE.put(t.id, new Raid(t.id, ids, earth.getGameTime() + 2400 * 3));
		owner.sendSystemMessage(Component.literal("⚔ " + raidersName(who) + " идут на " + t.name + "! " + ids.size() + " воинов, с "
				+ direction(a) + ".").withStyle(ChatFormatting.RED));
		t.log("набег: " + raidersName(who) + " (" + ids.size() + ")");
	}

	private static String direction(double a) {
		String[] dirs = {"востока", "юго-востока", "юга", "юго-запада", "запада", "северо-запада", "севера", "северо-востока"};
		int i = (int) Math.round(a / (Math.PI / 4)) & 7;
		return dirs[i];
	}

	private static void loot(Town t, Random r) {
		for (Res res : Res.values()) {
			t.add(res, -t.get(res) * (0.15 + r.nextDouble() * 0.2));
		}
		t.treasury *= 0.6;
		t.population = Math.max(5, t.population - Math.max(2, t.population / 25));
		t.mood -= 15;
	}

	/** Checks the raids under way (every few seconds). */
	static void watch(ServerLevel earth, Towns ts) {
		var it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Raid raid = it.next().getValue();
			Town t = ts.towns.get(raid.town);
			int alive = 0;
			for (UUID id : raid.men) {
				Entity e = earth.getEntity(id);
				if (e != null && e.isAlive()) {
					alive++;
				}
			}
			ServerPlayer owner = t == null || t.owner.isEmpty() ? null : earth.getServer().getPlayerList().getPlayer(UUID.fromString(t.owner));
			if (alive == 0) {
				it.remove();
				if (t != null) {
					t.mood = Math.min(100, t.mood + 12);
					t.log("набег отбит!");
					ts.setDirty();
				}
				if (owner != null) {
					owner.sendSystemMessage(Component.literal("Набег на " + (t == null ? "поселение" : t.name) + " отбит!").withStyle(ChatFormatting.GREEN));
				}
			} else if (earth.getGameTime() > raid.until) {
				it.remove();
				for (UUID id : raid.men) {
					Entity e = earth.getEntity(id);
					if (e != null) {
						e.discard();
					}
				}
				if (t != null) {
					loot(t, new Random());
					t.log("набежчики разграбили поселение и ушли");
					ts.setDirty();
				}
				if (owner != null) {
					owner.sendSystemMessage(Component.literal("Набежчики разграбили " + (t == null ? "поселение" : t.name) + " и ушли с добычей.")
							.withStyle(ChatFormatting.DARK_RED));
				}
			}
		}
	}
}
