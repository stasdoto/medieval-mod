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
