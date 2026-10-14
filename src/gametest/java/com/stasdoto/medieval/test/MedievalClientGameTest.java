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
					{"kyiv", 50.4560, 30.5300, 45.0, 50.4560, 30.6000, 900},
					{"matterhorn", 45.9950, 7.7050, 120.0, 45.9766, 7.6586, 1300},
					{"constantinople", 41.0300, 28.9750, 70.0, 41.0086, 28.9802, 1000},
					{"venice", 45.4500, 12.3600, 160.0, 45.4340, 12.3390, 900},
					{"fjord", 62.1080, 7.0500, 60.0, 62.1000, 7.2000, 1100},
					{"giza", 29.9900, 31.1000, 80.0, 30.0000, 31.2300, 900},
			};
			// Settlements: find the nearest one of the wanted kind around a point and look at it from above its edge.
			Object[][] towns = {
					{"kyiv_city", 50.4535, 30.5143, "CITY"},
					{"rus_village", 51.40, 31.10, "VILLAGE"},
					{"latin_village", 49.30, 0.80, "VILLAGE"},
					{"norse_village", 59.35, 10.20, "VILLAGE"},
					{"cordoba_city", 37.8790, -4.7800, "CITY"},
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
				for (int i = -2; i <= 2; i++) {
					for (int j = -2; j <= 2; j++) {
						for (com.stasdoto.medieval.settle.Site st : com.stasdoto.medieval.settle.Sites.cell(Math.floorDiv(px, 4096) + i, Math.floorDiv(pz, 4096) + j)) {
							boolean ok = wantCity ? st.kind.urban() : "CAMP".equals(tw[3]) ? st.kind == com.stasdoto.medieval.settle.Site.Kind.CAMP
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
				Medieval.LOGGER.info("RESULT {}: {} - {} plots, {} streets, {} walls", name, best, lay.lots.size(), lay.roads.size(), lay.walls.size());
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
				Medieval.LOGGER.info("RESULT {} done in {} s", name, (System.currentTimeMillis() - t0) / 1000);
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
				pitch = Math.max(-10, Math.min(60, pitch));
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
}
