package com.stasdoto.medieval.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.stasdoto.medieval.Medieval;
import com.stasdoto.medieval.command.MedievalCommands;
import com.stasdoto.medieval.earth.Cover;
import com.stasdoto.medieval.earth.Projection;
import com.stasdoto.medieval.earth.Terrain;
import com.stasdoto.medieval.earth.TileStore;

/**
 * Flies over real places of the generated Earth in a real game client and takes screenshots: the Dnieper at Kyiv, the
 * Matterhorn, the Golden Horn, the Venetian lagoon, a Norwegian fjord, the Nile at Giza.
 * Scenes can be limited with the MEDIEVAL_SCENES environment variable (space-separated names).
 */
@SuppressWarnings("UnstableApiUsage")
public class MedievalClientGameTest implements FabricClientGameTest {
	private static boolean scene(String name) {
		String only = System.getenv("MEDIEVAL_SCENES");
		return only == null || only.isBlank() || (" " + only + " ").contains(" " + name + " ");
	}

	@Override
	public void runTest(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> mc.options.renderDistance().set(12));
		try (TestSingleplayerContext sp = ctx.worldBuilder().setUseConsistentSettings(true).create()) {
			TestServerContext server = sp.getServer();
			server.runCommand("time set 5000");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @a");
			// name, camera lat, lon, height above ground, target lat, lon, wait ticks
			Object[][] scenes = {
					{"kyiv", 50.4560, 30.5480, 90.0, 50.4560, 30.6000, 900},
					{"matterhorn", 45.9950, 7.7050, 120.0, 45.9766, 7.6586, 1300},
					{"constantinople", 41.0300, 28.9750, 70.0, 41.0086, 28.9802, 1000},
					{"venice", 45.4500, 12.3600, 160.0, 45.4340, 12.3390, 900},
					{"fjord", 62.1080, 7.0500, 60.0, 62.1000, 7.2000, 1100},
					{"giza", 29.9900, 31.1000, 80.0, 30.0000, 31.2300, 900},
			};
			// Settlements: find the nearest one of the wanted kind around a point and look at it from above its edge.
			Object[][] towns = {
					{"kyiv_city", 50.4535, 30.5143, "CITY"},
					{"paris_city", 48.8530, 2.3490, "CITY"},
					{"rus_village", 51.40, 31.10, "VILLAGE"},
					{"latin_village", 49.30, 0.80, "VILLAGE"},
					{"norse_village", 59.35, 10.20, "VILLAGE"},
					{"cordoba_city", 37.8790, -4.7800, "CITY"},
					{"latin_castle", 49.30, 0.80, "CASTLE"},
					{"rus_castle", 51.40, 31.10, "CASTLE"},
					{"norse_castle", 55.40, 11.30, "CASTLE"},
					{"steppe_camp", 47.6, 35.2, "CAMP"},
			};
			for (Object[] tw : towns) {
				String name = (String) tw[0];
				if (!scene(name)) {
					continue;
				}
				long t0 = System.currentTimeMillis();
				int px = (int) Math.round(Projection.x((double) tw[2])), pz = (int) Math.round(Projection.z((double) tw[1]));
				com.stasdoto.medieval.settle.Site best = null;
				double bd = Double.MAX_VALUE;
				boolean wantCity = "CITY".equals(tw[3]);
				boolean wantCastle = "CASTLE".equals(tw[3]);
				int span = wantCastle ? 3 : 2;
				for (int i = -span; i <= span; i++) {
					for (int j = -span; j <= span; j++) {
						for (com.stasdoto.medieval.settle.Site st : com.stasdoto.medieval.settle.Sites.cell(Math.floorDiv(px, 4096) + i, Math.floorDiv(pz, 4096) + j)) {
							boolean ok = wantCity ? st.kind.urban() : wantCastle ? st.kind == com.stasdoto.medieval.settle.Site.Kind.CASTLE : "CAMP".equals(tw[3]) ? st.kind == com.stasdoto.medieval.settle.Site.Kind.CAMP
									: st.kind == com.stasdoto.medieval.settle.Site.Kind.VILLAGE || st.kind == com.stasdoto.medieval.settle.Site.Kind.BIG_VILLAGE;
							double d = Math.hypot(st.x - px, st.z - pz);
							if (ok && d < bd) {
								bd = d;
								best = st;
							}
						}
					}
				}
				if (best == null) {
					Medieval.LOGGER.info("RESULT {}: no settlement found", name);
					continue;
				}
				com.stasdoto.medieval.settle.Layout lay = com.stasdoto.medieval.settle.Planner.layout(best);
				double back = Math.max(40, Math.min(170, best.radius * 0.55));
				double camX = best.x + back * 0.7, camZ = best.z + back;
				Terrain.Col cc = Terrain.column((int) camX, (int) camZ);
				double camY = Math.max(cc.ground, lay.centerY) + back * 0.55 + 12;
				double dx = best.x - camX, dz = best.z - camZ;
				float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
				float pitch = (float) Math.toDegrees(Math.atan2(camY - lay.centerY, Math.hypot(dx, dz)));
				java.util.Map<com.stasdoto.medieval.settle.BKind, Integer> count = new java.util.TreeMap<>();
				for (var l : lay.lots) {
					count.merge(l.kind, 1, Integer::sum);
				}
				Medieval.LOGGER.info("RESULT {}: {} - {} plots, {} streets, {} walls; {}", name, best, lay.lots.size(), lay.roads.size(), lay.walls.size(), count);
				server.runCommand(String.format(java.util.Locale.ROOT, "execute in %s run tp @a %.1f %.1f %.1f %.1f %.1f",
						MedievalCommands.EARTH.identifier(), camX, camY, camZ, yaw, pitch));
				ctx.waitTicks(1100);
				ctx.takeScreenshot(name);
				// A second look from the ground in the middle.
				Terrain.Col mid = Terrain.column(best.x + 6, best.z + 9);
				server.runCommand(String.format(java.util.Locale.ROOT, "execute in %s run tp @a %.1f %.1f %.1f %.1f %.1f",
						MedievalCommands.EARTH.identifier(), best.x + 6.5, (double) Math.max(mid.top(), lay.centerY) + 3, best.z + 9.5, 160f, 5f));
				ctx.waitTicks(300);
				ctx.takeScreenshot(name + "_street");
				// Cities: a look at the citadel (walls, palace, cathedral) from above its edge.
				if (wantCity && !lay.walls.isEmpty()) {
					var cit = lay.walls.get(0);
					double rr = cit.maxRadius();
					double cx2 = cit.cx + rr * 0.9, cz2 = cit.cz + rr * 0.9;
					Terrain.Col c2 = Terrain.column((int) cx2, (int) cz2);
					double y2 = c2.ground + rr * 0.45 + 20;
					float yaw2 = (float) Math.toDegrees(Math.atan2(-(cit.cx - cx2), cit.cz - cz2));
					float pitch2 = (float) Math.toDegrees(Math.atan2(y2 - lay.centerY, Math.hypot(cit.cx - cx2, cit.cz - cz2)));
					server.runCommand(String.format(java.util.Locale.ROOT, "execute in %s run tp @a %.1f %.1f %.1f %.1f %.1f",
							MedievalCommands.EARTH.identifier(), cx2, y2, cz2, yaw2, pitch2));
					ctx.waitTicks(700);
					ctx.takeScreenshot(name + "_citadel");
				}
				// Inside a house and a workshop, and the waterside.
				if (wantCity || "rus_village".equals(name) || "latin_village".equals(name)) {
					inside(ctx, server, lay, best, name + "_home", com.stasdoto.medieval.settle.BKind.HOUSE_RICH, com.stasdoto.medieval.settle.BKind.HOUSE,
							com.stasdoto.medieval.settle.BKind.HOMESTEAD);
					inside(ctx, server, lay, best, name + "_workshop", com.stasdoto.medieval.settle.BKind.POTTERY, com.stasdoto.medieval.settle.BKind.WEAVER,
							com.stasdoto.medieval.settle.BKind.BAKERY, com.stasdoto.medieval.settle.BKind.SMITHY, com.stasdoto.medieval.settle.BKind.TAVERN);
					over(ctx, server, lay, best, name + "_water", com.stasdoto.medieval.settle.BKind.WATERMILL, com.stasdoto.medieval.settle.BKind.PIER,
							com.stasdoto.medieval.settle.BKind.FISHERY, com.stasdoto.medieval.settle.BKind.NORIA, com.stasdoto.medieval.settle.BKind.BOATYARD);
				}
				if (wantCity) {
					over(ctx, server, lay, best, name + "_crafts", com.stasdoto.medieval.settle.BKind.TANNERY, com.stasdoto.medieval.settle.BKind.POTTERY,
							com.stasdoto.medieval.settle.BKind.KILN, com.stasdoto.medieval.settle.BKind.BLOOMERY, com.stasdoto.medieval.settle.BKind.CARPENTER);
				}
				if ("rus_village".equals(name) && scene("rule")) {
					// Become the lord (creative: free), look at the card, raise warriors and look at them.
					server.runCommand("gamemode creative @a");
					server.runCommand("execute as @p run medieval town charter");
					server.runCommand("execute as @p run medieval town recruit 6");
					server.runCommand("execute as @p run medieval town build homestead");
					server.runCommand("execute as @p run medieval town market");
					ctx.waitTicks(80);
					ctx.takeScreenshot("rule_card");
					server.runCommand("gamemode spectator @a");
					server.runCommand("execute as @p at @s run tp @s ~ ~4 ~ 160 35");
					ctx.waitTicks(60);
					ctx.takeScreenshot("rule_warriors");
					int people = server.computeOnServer(sv -> {
						var lvl = sv.getLevel(MedievalCommands.EARTH);
						var pl = sv.getPlayerList().getPlayers().get(0);
						return lvl.getEntitiesOfClass(com.stasdoto.medieval.people.PersonEntity.class, pl.getBoundingBox().inflate(200)).size();
					});
					Medieval.LOGGER.info("RESULT rule: people around {}", people);
				}
				Medieval.LOGGER.info("RESULT {} done in {} s", name, (System.currentTimeMillis() - t0) / 1000);
			}
			ctx.runOnClient(mc -> mc.options.renderDistance().set(16));
			String[] wonders = {"giza_khufu", "hagia_sophia", "kyiv_sofia", "kyiv_golden_gate", "colosseum", "cordoba_mezquita", "pharos", "stonehenge",
					"theodosian_walls", "dome_of_rock", "pantheon", "hippodrome"};
			for (String id : wonders) {
				if (!scene("wonders") && !scene(id)) {
					continue;
				}
				com.stasdoto.medieval.construct.Landmarks.Mark m = null;
				for (var k : com.stasdoto.medieval.construct.Landmarks.all()) {
					if (k.id().equals(id)) {
						m = k;
					}
				}
				if (m == null) {
					Medieval.LOGGER.info("RESULT wonder {}: not found", id);
					continue;
				}
				long t0 = System.currentTimeMillis();
				double size = Math.max(m.halfX(), m.halfZ());
				int mxx = m.x(), mzz = m.z();
				if (size > 400) {
					// The land walls: a stretch of them near the Gate of St Romanus.
					mxx = (int) Math.round(Projection.x(28.92035));
					mzz = (int) Math.round(Projection.z(41.01430));
					size = 110;
				}
				if (id.equals("giza_khufu")) {
					// The three pyramids together: from the south-east, over the Sphinx.
					mxx = (int) Math.round(Projection.x(31.13417));
					mzz = (int) Math.round(Projection.z(29.97917));
					size = 150;
				}
				double dist = Math.max(70, Math.min(230, size * 1.25 + 30));
				double camX = mxx + dist * 0.6, camZ = mzz + dist * 0.8;
				Terrain.Col c = Terrain.column((int) camX, (int) camZ);
				Terrain.Col mc = Terrain.column(mxx, mzz);
				double camY = Math.max(c.ground, mc.ground) + Math.max(30, size * 0.55);
				double dx = mxx - camX, dz = mzz - camZ;
				float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
				float pitch = (float) Math.toDegrees(Math.atan2(camY - mc.ground - size * 0.15, Math.hypot(dx, dz)));
				server.runCommand(String.format(java.util.Locale.ROOT, "execute in %s run tp @a %.1f %.1f %.1f %.1f %.1f",
						MedievalCommands.EARTH.identifier(), camX, camY, camZ, yaw, pitch));
				ctx.waitTicks(900);
				ctx.takeScreenshot("wonder_" + id);
				Medieval.LOGGER.info("RESULT wonder {} ({}) done in {} s", id, m.name(), (System.currentTimeMillis() - t0) / 1000);
			}
			for (Object[] s : scenes) {
				String name = (String) s[0];
				if (!scene(name)) {
					continue;
				}
				long t0 = System.currentTimeMillis();
				double lat = (double) s[1], lon = (double) s[2];
				int x = (int) Math.round(Projection.x(lon)), z = (int) Math.round(Projection.z(lat));
				Terrain.Col c = Terrain.column(x, z);
				int ground = Math.max(c.top(), c.waterTop == Terrain.NO_WATER ? c.top() : c.waterTop);
				double tx = Projection.x((double) s[5]), tz = Projection.z((double) s[4]);
				Terrain.Col tc = Terrain.column((int) tx, (int) tz);
				double camY = ground + (double) s[3];
				double dx = tx - x, dz = tz - z;
				float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
				double dist = Math.hypot(dx, dz);
				float pitch = (float) Math.toDegrees(Math.atan2(camY - tc.ground, dist));
				pitch = Math.max(14, Math.min(60, pitch));
				Medieval.LOGGER.info("RESULT {}: block {} {} (lat {} lon {}), ground y {} ({} m), water top {}, cover {}, zone {}, {} °C; target {} m away, ground y {}",
						name, x, z, String.format("%.4f", lat), String.format("%.4f", lon), ground, Math.round(c.meters), c.waterTop,
						Cover.name(c.cover), c.zone, String.format("%.1f", c.temp), Math.round(dist), Math.round(tc.ground));
				server.runCommand(String.format(java.util.Locale.ROOT, "execute in %s run tp @a %.1f %.1f %.1f %.1f %.1f",
						MedievalCommands.EARTH.identifier(), x + 0.5, camY, z + 0.5, yaw, pitch));
				ctx.waitTicks((int) s[6]);
				ctx.takeScreenshot(name);
				int real = server.computeOnServer(sv -> {
					var lvl = sv.getLevel(MedievalCommands.EARTH);
					return lvl == null ? -9999 : lvl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
				});
				Medieval.LOGGER.info("RESULT {} done in {} s: world surface at camera {}, tiles downloaded {}, failed {}", name,
						(System.currentTimeMillis() - t0) / 1000, real, TileStore.DOWNLOADED.get(), TileStore.FAILED.get());
			}
		}
	}

	private static com.stasdoto.medieval.settle.Layout.Lot find(com.stasdoto.medieval.settle.Layout lay, com.stasdoto.medieval.settle.Site st,
			com.stasdoto.medieval.settle.BKind... kinds) {
		for (var k : kinds) {
			com.stasdoto.medieval.settle.Layout.Lot best = null;
			double bd = Double.MAX_VALUE;
			for (var l : lay.lots) {
				if (l.kind != k) {
					continue;
				}
				double d = Math.hypot((l.x0 + l.x1) / 2.0 - st.x, (l.z0 + l.z1) / 2.0 - st.z);
				if (d < bd) {
					bd = d;
					best = l;
				}
			}
			if (best != null) {
				return best;
			}
		}
		return null;
	}

	/** Stands in a building near its door and looks to the back wall. */
	private static void inside(ClientGameTestContext ctx, TestServerContext server, com.stasdoto.medieval.settle.Layout lay, com.stasdoto.medieval.settle.Site st,
			String shot, com.stasdoto.medieval.settle.BKind... kinds) {
		var l = find(lay, st, kinds);
		if (l == null) {
			Medieval.LOGGER.info("RESULT {}: none of {}", shot, java.util.Arrays.toString(kinds));
			return;
		}
		double cx = (l.x0 + l.x1 + 1) / 2.0, cz = (l.z0 + l.z1 + 1) / 2.0;
		double fx = 0, fz = 0;
		float yaw;
		switch (l.rot) {
			case 1 -> {
				fx = -1;
				yaw = 270f;
			}
			case 2 -> {
				fz = -1;
				yaw = 0f;
			}
			case 3 -> {
				fx = 1;
				yaw = 90f;
			}
			default -> {
				fz = 1;
				yaw = 180f;
			}
		}
		double off = Math.max(0, l.d / 2.0 - 3.5);
		double x = cx + fx * off, z = cz + fz * off;
		server.runCommand(String.format(java.util.Locale.ROOT, "execute in %s run tp @a %.1f %.1f %.1f %.1f %.1f", MedievalCommands.EARTH.identifier(), x,
				l.floor + 1.05, z, yaw, 12f));
		ctx.waitTicks(200);
		ctx.takeScreenshot(shot);
		Medieval.LOGGER.info("RESULT {}: {} {}x{} at {} {} {}", shot, l.kind, l.w, l.d, (int) x, l.floor, (int) z);
	}

	/** Looks down at a building from a little way off. */
	private static void over(ClientGameTestContext ctx, TestServerContext server, com.stasdoto.medieval.settle.Layout lay, com.stasdoto.medieval.settle.Site st,
			String shot, com.stasdoto.medieval.settle.BKind... kinds) {
		var l = find(lay, st, kinds);
		if (l == null) {
			Medieval.LOGGER.info("RESULT {}: none of {}", shot, java.util.Arrays.toString(kinds));
			return;
		}
		double cx = (l.x0 + l.x1 + 1) / 2.0, cz = (l.z0 + l.z1 + 1) / 2.0;
		double x = cx + 16, z = cz + 20, y = l.floor + 16;
		float yaw = (float) Math.toDegrees(Math.atan2(-(cx - x), cz - z));
		float pitch = (float) Math.toDegrees(Math.atan2(y - l.floor - 2, Math.hypot(cx - x, cz - z)));
		server.runCommand(String.format(java.util.Locale.ROOT, "execute in %s run tp @a %.1f %.1f %.1f %.1f %.1f", MedievalCommands.EARTH.identifier(), x, y, z,
				yaw, pitch));
		ctx.waitTicks(260);
		ctx.takeScreenshot(shot);
		Medieval.LOGGER.info("RESULT {}: {} {}x{} at {} {} {}", shot, l.kind, l.w, l.d, (int) cx, l.floor, (int) cz);
	}
}
