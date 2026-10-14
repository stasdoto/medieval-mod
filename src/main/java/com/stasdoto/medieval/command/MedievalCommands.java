package com.stasdoto.medieval.command;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import com.stasdoto.medieval.Medieval;
import com.stasdoto.medieval.earth.Cover;
import com.stasdoto.medieval.earth.Projection;
import com.stasdoto.medieval.earth.Terrain;
import com.stasdoto.medieval.earth.TileStore;
import com.stasdoto.medieval.history.Places;
import com.stasdoto.medieval.world.EarthChunkGenerator;

/**
 * /medieval go <place> - travel to a town of 1050 (Kyiv, Novgorod, Constantinople, Paris...)
 * /medieval go <lat> <lon> - travel to any point of the Earth
 * /medieval places - the list of towns
 * /medieval where - where you are on the real Earth
 * /medieval offline on|off - use only the map pieces already downloaded
 */
public final class MedievalCommands {
	private MedievalCommands() {
	}

	public static final ResourceKey<Level> EARTH = ResourceKey.create(Registries.DIMENSION, Medieval.id("earth"));

	/** The world with the Earth: the main world when it was created as "Earth", otherwise the extra Earth dimension. */
	public static ServerLevel earth(MinecraftServer server) {
		ServerLevel ow = server.overworld();
		if (ow.getChunkSource().getGenerator() instanceof EarthChunkGenerator) {
			return ow;
		}
		ServerLevel e = server.getLevel(EARTH);
		return e != null ? e : ow;
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
				Commands.literal("medieval")
						.then(Commands.literal("go")
								.then(Commands.argument("place", StringArgumentType.greedyString())
										.suggests((ctx, b) -> {
											for (Places.Place p : Places.ALL) {
												b.suggest(p.id());
											}
											return b.buildFuture();
										})
										.executes(ctx -> go(ctx.getSource(), StringArgumentType.getString(ctx, "place")))))
						.then(Commands.literal("places").executes(ctx -> {
							StringBuilder sb = new StringBuilder();
							for (Places.Place p : Places.ALL) {
								if (!sb.isEmpty()) {
									sb.append(", ");
								}
								sb.append(p.name()).append(" (").append(p.id()).append(")");
							}
							ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
							return Places.ALL.size();
						}))
						.then(Commands.literal("where").executes(ctx -> where(ctx.getSource())))
						.then(Commands.literal("offline")
								.then(Commands.literal("on").executes(ctx -> offline(ctx.getSource(), true)))
								.then(Commands.literal("off").executes(ctx -> offline(ctx.getSource(), false))))));
	}

	private static int offline(CommandSourceStack src, boolean on) {
		TileStore.online = !on;
		src.sendSuccess(() -> Component.literal(on ? "Карты больше не скачиваются: используется только то, что уже на диске."
				: "Недостающие куски карты снова скачиваются из интернета."), false);
		return 1;
	}

	private static int where(CommandSourceStack src) {
		int x = (int) Math.floor(src.getPosition().x), z = (int) Math.floor(src.getPosition().z);
		CompletableFuture.supplyAsync(() -> Terrain.column(x, z)).thenAccept(c -> src.getServer().execute(() -> src.sendSuccess(() -> Component.literal(
				String.format("Широта %.5f, долгота %.5f, высота над морем %.0f м, %s, средняя температура года %.1f °C",
						c.lat, c.lon, c.meters, Cover.name(c.cover), c.temp)), false)));
		return 1;
	}

	private static int go(CommandSourceStack src, String q) {
		double lat, lon;
		String title;
		String[] parts = q.trim().split("[\\s,]+");
		if (parts.length == 2 && parts[0].matches("-?[0-9.]+") && parts[1].matches("-?[0-9.]+")) {
			lat = Double.parseDouble(parts[0]);
			lon = Double.parseDouble(parts[1]);
			title = String.format("%.4f, %.4f", lat, lon);
		} else {
			Places.Place p = Places.find(q.trim());
			if (p == null) {
				src.sendFailure(Component.literal("Нет такого места. Список: /medieval places"));
				return 0;
			}
			lat = p.lat();
			lon = p.lon();
			title = p.name() + " — " + p.realm() + ", около " + p.people() + " жителей";
		}
		int x = (int) Math.round(Projection.x(lon)), z = (int) Math.round(Projection.z(lat));
		MinecraftServer server = src.getServer();
		ServerLevel level = earth(server);
		src.sendSuccess(() -> Component.literal("Загружаю карту местности: " + title + "…"), false);
		CompletableFuture.supplyAsync(() -> Terrain.column(x, z)).thenAccept(c -> server.execute(() -> {
			int y = Math.max(c.top(), c.waterTop == Terrain.NO_WATER ? c.top() : c.waterTop) + 2;
			String dim = level.dimension().identifier().toString();
			server.getCommands().performPrefixedCommand(src, "execute in " + dim + " run tp @s " + (x + 0.5) + " " + y + " " + (z + 0.5));
			src.sendSuccess(() -> Component.literal(title), false);
		}));
		return 1;
	}
}
