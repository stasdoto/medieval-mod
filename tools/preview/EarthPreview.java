import com.stasdoto.medieval.earth.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Draws top-down maps of the generated Earth without starting the game (used by the build to check the generator):
 * shaded ground coloured by what grows there, water by depth.
 *
 * java -cp build/classes/java/main:tools/preview EarthPreview <cache dir> <out dir> name:lat:lon:sizeBlocks:pixels ...
 */
public class EarthPreview {
	public static void main(String[] args) throws Exception {
		Path cache = Path.of(args[0]);
		Path out = Path.of(args[1]);
		Files.createDirectories(out);
		Dem.init(new TileStore("terrarium", cache, "png", "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png"));
		OsmTile.init(new TileStore("osm", cache, "pbf", null));
		OsmTile.resolveUrl(cache.resolve("osm-url.txt"));
		for (int a = 2; a < args.length; a++) {
			String[] p = args[a].split(":");
			String name = p[0];
			double lat = Double.parseDouble(p[1]), lon = Double.parseDouble(p[2]);
			int size = Integer.parseInt(p[3]);
			int pixels = Integer.parseInt(p[4]);
			long t0 = System.currentTimeMillis();
			int cx = (int) Projection.x(lon), cz = (int) Projection.z(lat);
			double step = (double) size / pixels;
			double[] g = new double[pixels * pixels];
			Terrain.Col[] cols = new Terrain.Col[pixels * pixels];
			int water = 0;
			double minY = 1e9, maxY = -1e9;
			for (int j = 0; j < pixels; j++) {
				for (int i = 0; i < pixels; i++) {
					int x = cx + (int) ((i - pixels / 2) * step), z = cz + (int) ((j - pixels / 2) * step);
					Terrain.Col c = Terrain.column(x, z);
					cols[j * pixels + i] = c;
					g[j * pixels + i] = c.ground;
					if (c.waterTop != Terrain.NO_WATER) {
						water++;
					}
					minY = Math.min(minY, c.ground);
					maxY = Math.max(maxY, c.ground);
				}
			}
			BufferedImage img = new BufferedImage(pixels, pixels, BufferedImage.TYPE_INT_RGB);
			for (int j = 0; j < pixels; j++) {
				for (int i = 0; i < pixels; i++) {
					Terrain.Col c = cols[j * pixels + i];
					int rgb;
					if (c.waterTop != Terrain.NO_WATER) {
						double depth = c.waterTop + 1 - c.ground;
						double k = Math.min(1, depth / 20.0);
						int b = (int) (200 - 120 * k), gg = (int) (150 - 90 * k), r = (int) (60 - 40 * k);
						if (c.waterKind == Terrain.W_RIVER) {
							gg += 25;
						}
						rgb = r << 16 | gg << 8 | b;
					} else {
						double e = g[j * pixels + Math.min(pixels - 1, i + 1)] - g[j * pixels + Math.max(0, i - 1)];
						double n = g[Math.min(pixels - 1, j + 1) * pixels + i] - g[Math.max(0, j - 1) * pixels + i];
						double shade = 0.75 + Math.max(-0.45, Math.min(0.35, (-e + n) / (4 * step) * 1.6));
						int base = switch (c.cover) {
							case Cover.WOOD -> 0x3E6B2F;
							case Cover.FARMLAND -> 0xB9B36A;
							case Cover.GRASS, Cover.MEADOW -> 0x8DB35A;
							case Cover.WETLAND, Cover.FLOODPLAIN, Cover.MUD -> 0x6F8F6A;
							case Cover.SAND, Cover.BEACH -> 0xE0D19A;
							case Cover.ROCK, Cover.SCREE -> 0x9A958C;
							case Cover.ICE -> 0xF2F6FA;
							case Cover.MODERN -> 0xC08A7A;
							case Cover.SCRUB, Cover.HEATH -> 0x7A8F4A;
							default -> 0xA5B880;
						};
						double h = Math.max(0, Math.min(1, (c.ground - 63) / 1500.0));
						int r = (int) Math.min(255, ((base >> 16 & 255) * (1 - h * 0.4) + 255 * h * 0.4) * shade);
						int gg = (int) Math.min(255, ((base >> 8 & 255) * (1 - h * 0.4) + 255 * h * 0.4) * shade);
						int b = (int) Math.min(255, ((base & 255) * (1 - h * 0.4) + 255 * h * 0.4) * shade);
						rgb = Math.max(0, r) << 16 | Math.max(0, gg) << 8 | Math.max(0, b);
					}
					img.setRGB(i, j, rgb);
				}
			}
			ImageIO.write(img, "png", out.resolve(name + ".png").toFile());
			Terrain.Col mid = cols[(pixels / 2) * pixels + pixels / 2];
			System.out.printf("RESULT %s: %d blocks across, ground y %.0f..%.0f, water %.1f%%, centre y %.1f (%.0f m) cover %s zone %d temp %.1f, %d ms, tiles downloaded %d failed %d%n",
					name, size, minY, maxY, 100.0 * water / cols.length, mid.ground, mid.meters, Cover.name(mid.cover), mid.zone, mid.temp,
					System.currentTimeMillis() - t0, TileStore.DOWNLOADED.get(), TileStore.FAILED.get());
		}
	}
}
