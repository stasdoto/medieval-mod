package com.stasdoto.medieval.earth;

/**
 * Everything about one column of the world: ground height, water, what grows there and the climate. Pure Java (no game
 * classes), so the same code draws the preview maps outside the game.
 */
public final class Terrain {
	private Terrain() {
	}

	public static final int NO_WATER = Integer.MIN_VALUE;
	public static final int W_NONE = 0, W_SEA = 1, W_LAKE = 2, W_RIVER = 3;

	/** The result for one column. */
	public static final class Col {
		/** Height of the ground surface (fractional); the top solid block is round(ground) - 1. */
		public double ground;
		/** Real elevation (m) of the dry ground before carving. */
		public double meters;
		/** Top water block, or {@link #NO_WATER}. */
		public int waterTop = NO_WATER;
		public int waterKind;
		/** Distance to the shore in blocks: positive in water, negative on land (only meaningful near water). */
		public double shore;
		public byte cover;
		public int zone;
		public double temp;
		public double lat, lon;
		/** True when no online map data was available here (only the elevation was used). */
		public boolean noMap;

		public int top() {
			return (int) Math.round(ground) - 1;
		}
	}

	/** Water levels of one map square: 32×32 cells of 16×16 grid pixels, metres (NaN = no water near). */
	private static final class Levels {
		final float[] level = new float[32 * 32];
		/** Cells where the water is wide (a river or lake, not a brook): those use the smoothed level. */
		final boolean[] wide = new boolean[32 * 32];
	}

	private static final TileCache<Levels> LEVELS = new TileCache<>(200, Terrain::levels);

	public static Col column(int x, int z) {
		Col c = new Col();
		column(x, z, c);
		return c;
	}

	public static void column(int x, int z, Col c) {
		double lat = Projection.lat(z + 0.5);
		double lon = Projection.lon(x + 0.5);
		c.lat = lat;
		c.lon = lon;
		double raw = Dem.meters(lat, lon);
		double m = raw;
		// Below sea level only under the sea or in the few real depressions; elsewhere it is a hole in the data.
		if (m < -2 && !lowland(lat, lon)) {
			m = -2;
		}
		c.meters = m;
		double gx = Projection.mercX(lon, OsmTile.ZOOM) / 256.0;
		double gy = Projection.mercY(lat, OsmTile.ZOOM) / 256.0;
		int tx = (int) Math.floor(gx), ty = (int) Math.floor(gy);
		OsmTile t = OsmTile.get(tx, ty);
		double fx = (gx - tx) * OsmTile.N, fy = (gy - ty) * OsmTile.N;
		double pxMeters = 40075016.686 * Math.cos(Math.toRadians(lat)) / (1 << OsmTile.ZOOM) / OsmTile.N;
		double pxBlocks = pxMeters / Projection.metersPerBlock;

		double temp = Climate.temp(lat, lon, m);
		c.temp = temp;
		c.zone = Climate.zone(lat, lon, temp);
		c.waterTop = NO_WATER;
		c.waterKind = W_NONE;
		c.noMap = !t.valid;

		// Small natural unevenness that the 30 m elevation data cannot show.
		double rough = Noise.fbm(x / 48.0, z / 48.0, 11, 3) * 0.9 + Noise.at(x / 9.0, z / 9.0, 12) * 0.35;
		double ground = Projection.y(m) + rough;

		double s; // shore distance in grid pixels
		boolean sea;
		if (t.valid) {
			s = sdf(t, fx, fy);
			sea = s > 0 && t.isOcean(clamp((int) fx), clamp((int) fy));
			int ci = clamp((int) fx) / 2, cj = clamp((int) fy) / 2;
			c.cover = t.cover[cj * OsmTile.C + ci];
		} else {
			// No map: the sea is where the ground is below sea level.
			s = raw < -0.5 ? 10 : -10;
			sea = s > 0;
			c.cover = Cover.NONE;
		}
		c.shore = s * pxBlocks;

		if (s > 0) {
			double level;
			if (sea) {
				level = 0;
			} else if (wideAt(t, tx, ty, fx, fy)) {
				level = levelAt(t, tx, ty, fx, fy);
				if (Double.isNaN(level)) {
					level = m;
				}
			} else {
				// A brook: it runs in the bottom of its own little valley.
				double lo = m;
				double d = 3 * Projection.metersPerBlock;
				lo = Math.min(lo, Dem.meters(Projection.lat(z + 0.5 + d), lon));
				lo = Math.min(lo, Dem.meters(Projection.lat(z + 0.5 - d), lon));
				lo = Math.min(lo, Dem.meters(lat, Projection.lon(x + 0.5 + d)));
				lo = Math.min(lo, Dem.meters(lat, Projection.lon(x + 0.5 - d)));
				level = lo - 0.7;
			}
			int wTop = (int) Math.round(Projection.y(level)) - 1;
			double distM = s * pxMeters;
			double depthBlocks;
			if (sea) {
				// Near the shore the depth grows with the distance; further out it follows the charted sea floor.
				double bathy = raw < 0 ? (wTop + 1) - Projection.y(raw) : 0;
				double wgt = Math.min(1, distM / 190.0);
				wgt = wgt * wgt * (3 - 2 * wgt);
				depthBlocks = Math.max(1 + distM * 0.05, bathy * wgt);
				ground = wTop + 1 - depthBlocks;
				c.waterKind = W_SEA;
			} else {
				boolean river = distM < 400;
				depthBlocks = river ? Math.min(14, 1 + distM * 0.16) : Math.min(22, 1.5 + distM * 0.09);
				depthBlocks /= Projection.metersPerBlock;
				ground = Math.min(wTop + 1 - depthBlocks, Math.min(ground, wTop));
				c.waterKind = river ? W_RIVER : W_LAKE;
			}
			ground += Noise.at(x / 6.0, z / 6.0, 13) * 0.4;
			c.waterTop = wTop;
			if (ground > wTop - 0.5) {
				ground = wTop - 0.5;
			}
		} else if (t.valid && t.anyWater && s > -6 && wideAt(t, tx, ty, fx, fy)) {
			// Banks: never lower than the water next to them (or it would spill), and gently sloping down to it.
			double level = levelAt(t, tx, ty, fx, fy);
			if (!Double.isNaN(level)) {
				int wTop = (int) Math.round(Projection.y(level)) - 1;
				double edge = wTop + 1.2;
				double k = Math.max(0, Math.min(1, (-s - 1) / 5));
				if (ground < edge) {
					ground = edge;
				} else {
					ground = edge + (ground - edge) * (0.35 + 0.65 * k);
				}
			}
		}
		c.ground = Math.max(Projection.MIN_Y + 4, Math.min(Projection.MAX_Y - 4, ground));
	}

	/** Places that really lie below sea level: the Caspian, the Dead Sea and the Jordan, Qattara, Danakil, Turpan, the Low Countries. */
	static boolean lowland(double lat, double lon) {
		return lat > 36 && lat < 48 && lon > 45 && lon < 56 // Caspian
				|| lat > 29.5 && lat < 33.5 && lon > 34.8 && lon < 36 // Dead Sea, Jordan
				|| lat > 28.5 && lat < 30.6 && lon > 25.5 && lon < 29.5 // Qattara
				|| lat > 12 && lat < 15 && lon > 39.5 && lon < 42 // Danakil
				|| lat > 42.3 && lat < 43.3 && lon > 88.5 && lon < 90.5 // Turpan
				|| lat > 51 && lat < 53.7 && lon > 3 && lon < 7.3; // Low Countries
	}

	private static int clamp(int p) {
		return Math.max(0, Math.min(OsmTile.N - 1, p));
	}

	/** The shore distance (grid pixels) between grid points. */
	private static double sdf(OsmTile t, double fx, double fy) {
		double x = fx - 0.5, y = fy - 0.5;
		int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
		double ax = x - x0, ay = y - y0;
		int x1 = clamp(x0 + 1), y1 = clamp(y0 + 1);
		x0 = clamp(x0);
		y0 = clamp(y0);
		double a = t.sdf[y0 * OsmTile.N + x0], b = t.sdf[y0 * OsmTile.N + x1], c = t.sdf[y1 * OsmTile.N + x0], d = t.sdf[y1 * OsmTile.N + x1];
		return ((a + (b - a) * ax) * (1 - ay) + (c + (d - c) * ax) * ay) / 2.0;
	}

	private static boolean wideAt(OsmTile t, int tx, int ty, double fx, double fy) {
		Levels l = LEVELS.get(TileCache.key(OsmTile.ZOOM, tx, ty));
		int cx = Math.max(0, Math.min(31, (int) (fx / 16))), cy = Math.max(0, Math.min(31, (int) (fy / 16)));
		return l.wide[cy * 32 + cx];
	}

	private static double levelAt(OsmTile t, int tx, int ty, double fx, double fy) {
		Levels l = LEVELS.get(TileCache.key(OsmTile.ZOOM, tx, ty));
		double x = fx / 16.0 - 0.5, y = fy / 16.0 - 0.5;
		int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
		double ax = x - x0, ay = y - y0;
		double sum = 0, w = 0;
		double min = Double.NaN;
		for (int j = 0; j < 2; j++) {
			for (int i = 0; i < 2; i++) {
				int cx = Math.max(0, Math.min(31, x0 + i)), cy = Math.max(0, Math.min(31, y0 + j));
				float v = l.level[cy * 32 + cx];
				if (Float.isNaN(v)) {
					continue;
				}
				double wt = (i == 0 ? 1 - ax : ax) * (j == 0 ? 1 - ay : ay);
				sum += v * wt;
				w += wt;
				min = Double.isNaN(min) ? v : Math.min(min, v);
			}
		}
		if (w <= 1e-6) {
			return min;
		}
		return sum / w;
	}

	/**
	 * Water surface heights for a map square, from the elevation under the water: a low quantile per cell (the elevation
	 * data is noisy on water), then each cell takes the lowest of its neighbours (water runs downhill) and the levels spread
	 * a few cells onto the land so that banks can match them.
	 */
	private static Levels levels(long key) {
		int tx = (int) (key >>> 29 & 0x1FFFFFFF);
		int ty = (int) (key & 0x1FFFFFFF);
		OsmTile t = OsmTile.get(tx, ty);
		Levels l = new Levels();
		java.util.Arrays.fill(l.level, Float.NaN);
		if (!t.valid || !t.anyWater) {
			return l;
		}
		float[] raw = new float[32 * 32];
		java.util.Arrays.fill(raw, Float.NaN);
		double[] vals = new double[64];
		for (int cy = 0; cy < 32; cy++) {
			for (int cx = 0; cx < 32; cx++) {
				int n = 0;
				boolean sea = false;
				int maxSdf = 0;
				for (int j = 0; j < 16; j++) {
					for (int i = 0; i < 16; i++) {
						maxSdf = Math.max(maxSdf, t.sdf[(cy * 16 + j) * OsmTile.N + cx * 16 + i]);
					}
				}
				boolean wideCell = maxSdf >= 8;
				for (int j = 1; j < 16; j += 2) {
					for (int i = 1; i < 16; i += 2) {
						int px = cx * 16 + i, py = cy * 16 + j;
						if (t.sdf[py * OsmTile.N + px] <= 0) {
							continue;
						}
						if (t.isOcean(px, py)) {
							sea = true;
							continue;
						}
						if (n < vals.length) {
							double gx = tx * 256.0 + (px + 0.5) * 256.0 / OsmTile.N;
							double gy = ty * 256.0 + (py + 0.5) * 256.0 / OsmTile.N;
							vals[n++] = Dem.meters(Projection.mercLat(gy, OsmTile.ZOOM), Projection.mercLon(gx, OsmTile.ZOOM));
						}
					}
				}
				l.wide[cy * 32 + cx] = wideCell || sea;
				if (n > 0) {
					java.util.Arrays.sort(vals, 0, n);
					raw[cy * 32 + cx] = (float) vals[Math.min(n - 1, n / 4)];
				} else if (sea) {
					raw[cy * 32 + cx] = 0f;
				}
			}
		}
		// A cell next to wide water counts as wide too (the shallow edge of a lake belongs to the lake).
		boolean[] wide = l.wide.clone();
		for (int cy = 0; cy < 32; cy++) {
			for (int cx = 0; cx < 32; cx++) {
				for (int j = -1; j <= 1 && !l.wide[cy * 32 + cx]; j++) {
					for (int i = -1; i <= 1; i++) {
						int x = cx + i, y = cy + j;
						if (x >= 0 && y >= 0 && x < 32 && y < 32 && wide[y * 32 + x]) {
							l.wide[cy * 32 + cx] = true;
							break;
						}
					}
				}
			}
		}
		// Lowest of the neighbours for the water cells.
		for (int cy = 0; cy < 32; cy++) {
			for (int cx = 0; cx < 32; cx++) {
				float v = raw[cy * 32 + cx];
				if (Float.isNaN(v)) {
					continue;
				}
				float mn = v;
				for (int j = -1; j <= 1; j++) {
					for (int i = -1; i <= 1; i++) {
						int x = cx + i, y = cy + j;
						if (x < 0 || y < 0 || x > 31 || y > 31) {
							continue;
						}
						float u = raw[y * 32 + x];
						if (!Float.isNaN(u) && u < mn) {
							mn = u;
						}
					}
				}
				l.level[cy * 32 + cx] = mn;
			}
		}
		// Spread onto the land around (for the banks).
		for (int pass = 0; pass < 3; pass++) {
			float[] next = l.level.clone();
			for (int cy = 0; cy < 32; cy++) {
				for (int cx = 0; cx < 32; cx++) {
					if (!Float.isNaN(l.level[cy * 32 + cx])) {
						continue;
					}
					float mn = Float.NaN;
					for (int j = -1; j <= 1; j++) {
						for (int i = -1; i <= 1; i++) {
							int x = cx + i, y = cy + j;
							if (x < 0 || y < 0 || x > 31 || y > 31) {
								continue;
							}
							float u = l.level[y * 32 + x];
							if (!Float.isNaN(u) && (Float.isNaN(mn) || u < mn)) {
								mn = u;
							}
						}
					}
					next[cy * 32 + cx] = mn;
				}
			}
			System.arraycopy(next, 0, l.level, 0, next.length);
		}
		return l;
	}

	public static void clear() {
		LEVELS.clear();
		Dem.clear();
		OsmTile.clear();
	}
}
