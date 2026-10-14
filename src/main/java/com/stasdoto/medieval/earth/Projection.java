package com.stasdoto.medieval.earth;

/**
 * Where a block is on the real Earth.
 *
 * Horizontally the world is the Earth at 1:1 (one block is one metre), in a plain latitude/longitude grid ("equirectangular"):
 * north is always -Z, east is +X, so the sun and the map agree. Block (0, 0) is Kyiv. East-west distances are exact at
 * {@link #REF_LAT} (47° N, the middle of Europe) and a little off further north or south (about -15% at the Mediterranean,
 * +20% at Novgorod), which keeps every town in the right place relative to its neighbours.
 *
 * Vertically, everything up to {@link #EXACT_HEIGHT} metres above the sea is exact too (y = 63 + metres). Higher ground is
 * squeezed smoothly so that the Alps and the Caucasus still fit under the build limit (Everest ends near y = 2020).
 */
public final class Projection {
	private Projection() {
	}

	public static final double ORIGIN_LAT = 50.4501;
	public static final double ORIGIN_LON = 30.5234;
	public static final double REF_LAT = 47.0;
	/** Metres per degree of latitude (and of longitude at the equator). */
	public static final double M_PER_DEG = 111_320.0;
	/** Metres per block (1 = the real size). */
	public static volatile double metersPerBlock = 1.0;

	public static final int SEA_LEVEL = 63;
	public static final int MIN_Y = -512;
	public static final int HEIGHT = 2544;
	public static final int MAX_Y = MIN_Y + HEIGHT - 1;
	public static final double EXACT_HEIGHT = 600.0;
	private static final double HIGH_SPAN = MAX_Y - 10 - (SEA_LEVEL + EXACT_HEIGHT);
	public static final double EXACT_DEPTH = 150.0;
	private static final double DEEP_SPAN = 330.0;

	public static double blocksPerDegLat() {
		return M_PER_DEG / metersPerBlock;
	}

	public static double blocksPerDegLon() {
		return M_PER_DEG * Math.cos(Math.toRadians(REF_LAT)) / metersPerBlock;
	}

	public static double lat(double z) {
		double lat = ORIGIN_LAT - z / blocksPerDegLat();
		return Math.max(-85.0, Math.min(85.0, lat));
	}

	public static double lon(double x) {
		double lon = ORIGIN_LON + x / blocksPerDegLon();
		lon = ((lon + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
		return lon;
	}

	public static double x(double lon) {
		double d = lon - ORIGIN_LON;
		if (d > 180) {
			d -= 360;
		} else if (d < -180) {
			d += 360;
		}
		return d * blocksPerDegLon();
	}

	public static double z(double lat) {
		return (ORIGIN_LAT - lat) * blocksPerDegLat();
	}

	/** World height (fractional y of the ground surface) for an elevation in metres. */
	public static double y(double meters) {
		double m = meters / metersPerBlock;
		if (m >= 0) {
			if (m <= EXACT_HEIGHT) {
				return SEA_LEVEL + m;
			}
			return SEA_LEVEL + EXACT_HEIGHT + HIGH_SPAN * (1 - Math.exp(-(m - EXACT_HEIGHT) / HIGH_SPAN));
		}
		double d = -m;
		if (d <= EXACT_DEPTH) {
			return SEA_LEVEL - d;
		}
		return SEA_LEVEL - EXACT_DEPTH - DEEP_SPAN * (1 - Math.exp(-(d - EXACT_DEPTH) / DEEP_SPAN));
	}

	/** Inverse of {@link #y(double)}: the real elevation in metres of a world height. */
	public static double meters(double y) {
		double h = y - SEA_LEVEL;
		double m;
		if (h >= 0) {
			m = h <= EXACT_HEIGHT ? h : EXACT_HEIGHT - HIGH_SPAN * Math.log(Math.max(1e-9, 1 - (h - EXACT_HEIGHT) / HIGH_SPAN));
		} else {
			double d = -h;
			m = -(d <= EXACT_DEPTH ? d : EXACT_DEPTH - DEEP_SPAN * Math.log(Math.max(1e-9, 1 - (d - EXACT_DEPTH) / DEEP_SPAN)));
		}
		return m * metersPerBlock;
	}

	// ---- Web Mercator (the grid of the online map tiles) ----

	/** Global pixel X at a zoom for 256-pixel tiles. */
	public static double mercX(double lon, int zoom) {
		return (lon + 180.0) / 360.0 * (256.0 * (1 << zoom));
	}

	public static double mercY(double lat, int zoom) {
		double r = Math.toRadians(lat);
		return (1.0 - Math.log(Math.tan(r) + 1.0 / Math.cos(r)) / Math.PI) / 2.0 * (256.0 * (1 << zoom));
	}

	public static double mercLon(double px, int zoom) {
		return px / (256.0 * (1 << zoom)) * 360.0 - 180.0;
	}

	public static double mercLat(double py, int zoom) {
		double n = Math.PI - 2.0 * Math.PI * py / (256.0 * (1 << zoom));
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}
}
