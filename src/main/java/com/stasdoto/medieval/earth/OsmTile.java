package com.stasdoto.medieval.earth;

import java.io.IOException;
import java.util.Map;

/**
 * One zoom-14 map square (about 1.5 × 1.5 km in Europe) of OpenStreetMap water and land cover, turned into grids:
 * <ul>
 * <li>{@link #sdf}: for every 1/512 of the square, how far it is from the shore (positive in water, negative on land), so
 * shores are drawn smoothly at block level;</li>
 * <li>{@link #ocean}: which water is the sea;</li>
 * <li>{@link #cover}: forest, meadow, fields, marsh, sand, rock or ice (1/256 of the square).</li>
 * </ul>
 * Modern reservoirs are left out (they did not exist in 1050) and become river flood meadows.
 */
public final class OsmTile {
	public static final int ZOOM = 14;
	public static final int N = 512;
	public static final int C = 256;
	/** Grid steps are half pixels: sdf / 2 = pixels. */
	public final byte[] sdf = new byte[N * N];
	public final long[] ocean = new long[N * N / 64];
	public final byte[] cover = new byte[C * C];
	/** False when there was no map data at all (offline): callers then fall back to the elevation. */
	public boolean valid;
	public boolean anyWater;

	private static volatile TileStore store;
	private static final TileCache<OsmTile> CACHE = new TileCache<>(200, OsmTile::load);
	private static final OsmTile EMPTY = new OsmTile();
	private static volatile boolean resolving;

	public static void init(TileStore s) {
		store = s;
		CACHE.clear();
	}

	/** Asks OpenFreeMap for the current address of its planet tiles (it changes when the map is rebuilt). */
	public static void resolveUrl(java.nio.file.Path saveTo) {
		if (resolving) {
			return;
		}
		resolving = true;
		TileStore s = store;
		try {
			if (saveTo != null && java.nio.file.Files.exists(saveTo) && s.urlTemplate() == null) {
				s.setUrlTemplate(java.nio.file.Files.readString(saveTo).trim());
			}
			String json = TileStore.fetchText("https://tiles.openfreemap.org/planet");
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"tiles\"\\s*:\\s*\\[\\s*\"([^\"]+)\"").matcher(json);
			if (m.find()) {
				s.setUrlTemplate(m.group(1));
				if (saveTo != null) {
					java.nio.file.Files.createDirectories(saveTo.getParent());
					java.nio.file.Files.writeString(saveTo, m.group(1));
				}
				EarthLog.info.accept("OpenStreetMap tiles: " + m.group(1));
			}
		} catch (IOException e) {
			EarthLog.warn.accept("OpenStreetMap tiles unavailable, using what is cached: " + e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			resolving = false;
		}
	}

	public static OsmTile get(int tx, int ty) {
		return CACHE.get(TileCache.key(ZOOM, tx, ty));
	}

	public boolean isOcean(int px, int py) {
		int i = py * N + px;
		return (ocean[i >> 6] >>> (i & 63) & 1L) != 0;
	}

	private void setOcean(int i) {
		ocean[i >> 6] |= 1L << (i & 63);
	}

	private static OsmTile load(long key) {
		int x = (int) (key >>> 29 & 0x1FFFFFFF);
		int y = (int) (key & 0x1FFFFFFF);
		TileStore s = store;
		if (s == null) {
			return EMPTY;
		}
		byte[] data = s.get(ZOOM, x, y);
		if (data == null) {
			return EMPTY;
		}
		OsmTile t = new OsmTile();
		t.valid = true;
		if (data.length == 0) {
			return t;
		}
		try {
			Map<String, Mvt.Layer> layers = Mvt.decode(data);
			t.build(layers, x, y);
		} catch (IOException | RuntimeException e) {
			EarthLog.warn.accept("bad map tile 14/" + x + "/" + y + ": " + e);
			t.valid = false;
		}
		return t;
	}

	private void build(Map<String, Mvt.Layer> layers, int tx, int ty) {
		boolean[] water = new boolean[N * N];
		boolean[] sea = new boolean[N * N];
		boolean[] flood = new boolean[N * N];
		double tileMeters = 40075016.686 * Math.cos(Math.toRadians(Projection.mercLat((ty + 0.5) * 256, ZOOM))) / (1 << ZOOM);
		double pxMeters = tileMeters / N;

		Mvt.Layer cover = layers.get("landcover");
		if (cover != null) {
			// Order matters: the later classes win where shapes overlap.
			String[][] order = {{"farmland"}, {"grass"}, {"sand"}, {"rock"}, {"ice"}, {"wetland"}, {"wood"}};
			for (String[] cls : order) {
				for (Mvt.Feature f : cover.features) {
					if (f.type != Mvt.POLYGON || !cls[0].equals(f.str("class"))) {
						continue;
					}
					byte code = coverCode(f.str("class"), f.str("subclass"));
					fillPolygon(f, cover.extent, C, this.cover, code, null);
				}
			}
		}
		Mvt.Layer landuse = layers.get("landuse");
		if (landuse != null) {
			for (Mvt.Feature f : landuse.features) {
				if (f.type != Mvt.POLYGON) {
					continue;
				}
				String c = f.str("class");
				if (c == null) {
					continue;
				}
				byte code = switch (c) {
					case "residential", "commercial", "industrial", "retail", "railway", "garages", "quarry", "military", "landfill",
						 "construction", "brownfield", "school", "university", "hospital", "stadium", "cemetery", "suburb",
						 "neighbourhood", "quarter", "kindergarten", "college", "library", "theme_park", "zoo", "playground" -> Cover.MODERN;
					case "meadow", "village_green", "allotments", "orchard", "vineyard", "farmyard" -> Cover.MEADOW;
					default -> 0;
				};
				if (code != 0) {
					fillPolygon(f, landuse.extent, C, this.cover, code, (byte) 0);
				}
			}
		}

		Mvt.Layer w = layers.get("water");
		if (w != null) {
			for (Mvt.Feature f : w.features) {
				if (f.type != Mvt.POLYGON) {
					continue;
				}
				String c = f.str("class");
				if ("swimming_pool".equals(c) || "dock".equals(c)) {
					continue;
				}
				Object inter = f.tags.get("intermittent");
				if (inter != null && !"0".equals(inter.toString()) && !"false".equals(inter.toString())) {
					continue;
				}
				boolean isSea = "ocean".equals(c);
				if (!isSea && reservoir(f, w.extent, tx, ty)) {
					fillMask(f, w.extent, flood);
					continue;
				}
				fillMask(f, w.extent, water);
				if (isSea) {
					fillMask(f, w.extent, sea);
				}
			}
		}
		Mvt.Layer ww = layers.get("waterway");
		if (ww != null) {
			for (Mvt.Feature f : ww.features) {
				if (f.type != Mvt.LINE || "tunnel".equals(f.str("brunnel"))) {
					continue;
				}
				Object inter = f.tags.get("intermittent");
				if (inter != null && !"0".equals(inter.toString()) && !"false".equals(inter.toString())) {
					continue;
				}
				String c = f.str("class");
				double widthM = c == null ? 0 : switch (c) {
					case "river" -> 18;
					case "stream" -> 3.5;
					default -> 0; // canals, ditches and drains are modern
				};
				if (widthM > 0) {
					strokeLines(f, ww.extent, water, Math.max(0.6, widthM / pxMeters / 2.0));
				}
			}
		}
		for (int i = 0; i < N * N; i++) {
			if (water[i]) {
				anyWater = true;
				if (sea[i]) {
					setOcean(i);
				}
			}
			if (flood[i] && !water[i]) {
				int cx = (i % N) / 2, cy = (i / N) / 2;
				this.cover[cy * C + cx] = Cover.FLOODPLAIN;
			}
		}
		distance(water);
	}

	private static byte coverCode(String cls, String sub) {
		if (sub != null) {
			switch (sub) {
				case "scrub", "shrubbery" -> {
					return Cover.SCRUB;
				}
				case "heath", "fell" -> {
					return Cover.HEATH;
				}
				case "beach" -> {
					return Cover.BEACH;
				}
				case "scree", "shingle" -> {
					return Cover.SCREE;
				}
				case "mud", "tidalflat" -> {
					return Cover.MUD;
				}
				case "meadow", "grassland" -> {
					return Cover.MEADOW;
				}
				default -> {
				}
			}
		}
		return switch (cls) {
			case "wood" -> Cover.WOOD;
			case "grass" -> Cover.GRASS;
			case "farmland" -> Cover.FARMLAND;
			case "wetland" -> Cover.WETLAND;
			case "sand" -> Cover.SAND;
			case "rock" -> Cover.ROCK;
			case "ice" -> Cover.ICE;
			default -> Cover.NONE;
		};
	}

	/** Is this lake one of the modern reservoirs (checked at a few of its points). */
	private static boolean reservoir(Mvt.Feature f, int extent, int tx, int ty) {
		int hits = 0, tries = 0;
		for (int[] ring : f.parts) {
			int step = Math.max(2, (ring.length / 2 / 6) * 2);
			for (int k = 0; k < ring.length && tries < 8; k += step) {
				double gx = tx * 256.0 + ring[k] * 256.0 / extent;
				double gy = ty * 256.0 + ring[k + 1] * 256.0 / extent;
				tries++;
				if (Regions.in(Projection.mercLat(gy, ZOOM), Projection.mercLon(gx, ZOOM), Regions.RESERVOIR)) {
					hits++;
				}
			}
			break;
		}
		return tries > 0 && hits * 2 >= tries;
	}

	private static void fillMask(Mvt.Feature f, int extent, boolean[] mask) {
		scanFill(f, extent, N, (row, x0, x1) -> {
			for (int x = x0; x <= x1; x++) {
				mask[row * N + x] = true;
			}
		});
	}

	private static void fillPolygon(Mvt.Feature f, int extent, int size, byte[] grid, byte code, Byte onlyOver) {
		scanFill(f, extent, size, (row, x0, x1) -> {
			for (int x = x0; x <= x1; x++) {
				int i = row * size + x;
				if (onlyOver == null || grid[i] == onlyOver) {
					grid[i] = code;
				}
			}
		});
	}

	private interface Span {
		void fill(int row, int x0, int x1);
	}

	/** Even-odd scanline fill of all the rings of a polygon feature, at pixel centres. */
	private static void scanFill(Mvt.Feature f, int extent, int size, Span span) {
		double s = (double) size / extent;
		double[] xs = new double[64];
		for (int row = 0; row < size; row++) {
			double y = (row + 0.5) / s;
			int n = 0;
			for (int[] r : f.parts) {
				int pts = r.length / 2;
				for (int i = 0, j = pts - 1; i < pts; j = i++) {
					double yi = r[i * 2 + 1], yj = r[j * 2 + 1];
					if ((yi > y) != (yj > y)) {
						double xi = r[i * 2], xj = r[j * 2];
						if (n == xs.length) {
							xs = java.util.Arrays.copyOf(xs, n * 2);
						}
						xs[n++] = (xi + (y - yi) / (yj - yi) * (xj - xi)) * s;
					}
				}
			}
			if (n < 2) {
				continue;
			}
			java.util.Arrays.sort(xs, 0, n);
			for (int k = 0; k + 1 < n; k += 2) {
				int x0 = (int) Math.ceil(xs[k] - 0.5);
				int x1 = (int) Math.floor(xs[k + 1] - 0.5);
				x0 = Math.max(0, x0);
				x1 = Math.min(size - 1, x1);
				if (x0 <= x1) {
					span.fill(row, x0, x1);
				}
			}
		}
	}

	private static void strokeLines(Mvt.Feature f, int extent, boolean[] mask, double radius) {
		double s = (double) N / extent;
		int r = (int) Math.ceil(radius);
		double r2 = radius * radius;
		for (int[] line : f.parts) {
			for (int k = 0; k + 3 < line.length; k += 2) {
				double ax = line[k] * s, ay = line[k + 1] * s, bx = line[k + 2] * s, by = line[k + 3] * s;
				double len = Math.hypot(bx - ax, by - ay);
				int steps = Math.max(1, (int) Math.ceil(len / 0.5));
				for (int st = 0; st <= steps; st++) {
					double cx = ax + (bx - ax) * st / steps, cy = ay + (by - ay) * st / steps;
					int ix = (int) Math.floor(cx), iy = (int) Math.floor(cy);
					for (int dy = -r; dy <= r; dy++) {
						for (int dx = -r; dx <= r; dx++) {
							int px = ix + dx, py = iy + dy;
							if (px < 0 || py < 0 || px >= N || py >= N) {
								continue;
							}
							double ddx = px + 0.5 - cx, ddy = py + 0.5 - cy;
							if (ddx * ddx + ddy * ddy <= r2) {
								mask[py * N + px] = true;
							}
						}
					}
				}
			}
		}
	}

	/** Signed distance to the shore in half pixels (two-pass chamfer), clamped to a byte. */
	private void distance(boolean[] water) {
		final int INF = 1 << 20;
		int[] din = new int[N * N];
		int[] dout = new int[N * N];
		for (int i = 0; i < N * N; i++) {
			din[i] = water[i] ? INF : 0;
			dout[i] = water[i] ? 0 : INF;
		}
		chamfer(din);
		chamfer(dout);
		for (int i = 0; i < N * N; i++) {
			// chamfer units: 3 per pixel -> half pixels = d * 2 / 3; the shore lies between the two pixels (half a pixel each side).
			int v = water[i] ? (din[i] * 2 / 3) - 1 : -(dout[i] * 2 / 3) + 1;
			if (water[i] && v < 1) {
				v = 1;
			}
			if (!water[i] && v > -1) {
				v = -1;
			}
			sdf[i] = (byte) Math.max(-127, Math.min(127, v));
		}
	}

	private static void chamfer(int[] d) {
		for (int y = 0; y < N; y++) {
			for (int x = 0; x < N; x++) {
				int i = y * N + x;
				int v = d[i];
				if (v == 0) {
					continue;
				}
				if (x > 0) {
					v = Math.min(v, d[i - 1] + 3);
				}
				if (y > 0) {
					v = Math.min(v, d[i - N] + 3);
					if (x > 0) {
						v = Math.min(v, d[i - N - 1] + 4);
					}
					if (x < N - 1) {
						v = Math.min(v, d[i - N + 1] + 4);
					}
				}
				d[i] = v;
			}
		}
		for (int y = N - 1; y >= 0; y--) {
			for (int x = N - 1; x >= 0; x--) {
				int i = y * N + x;
				int v = d[i];
				if (v == 0) {
					continue;
				}
				if (x < N - 1) {
					v = Math.min(v, d[i + 1] + 3);
				}
				if (y < N - 1) {
					v = Math.min(v, d[i + N] + 3);
					if (x < N - 1) {
						v = Math.min(v, d[i + N + 1] + 4);
					}
					if (x > 0) {
						v = Math.min(v, d[i + N - 1] + 4);
					}
				}
				d[i] = v;
			}
		}
	}

	public static void clear() {
		CACHE.clear();
	}
}
