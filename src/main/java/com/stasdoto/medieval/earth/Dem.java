package com.stasdoto.medieval.earth;

import java.io.IOException;

/**
 * Ground height of the real Earth from the open "Terrarium" elevation tiles (Mapzen / AWS Open Data: SRTM, GMTED, ETOPO1
 * and national surveys). Zoom 14 gives one height every ~6 m in Europe; between them the ground is curved smoothly (bicubic).
 * Under the sea the tiles carry the depth of the sea floor.
 */
public final class Dem {
	private Dem() {
	}

	public static final int ZOOM = 14;
	private static final int OFFSET = 12000;
	static volatile TileStore store;
	private static final TileCache<char[]> CACHE = new TileCache<>(320, Dem::load);
	/** A tile that could not be loaded at any zoom: flat sea level. */
	private static final char[] FLAT = new char[0];

	public static void init(TileStore s) {
		store = s;
		CACHE.clear();
	}

	/** Elevation in metres at a latitude/longitude. */
	public static double meters(double lat, double lon) {
		double gx = Projection.mercX(lon, ZOOM) - 0.5;
		double gy = Projection.mercY(lat, ZOOM) - 0.5;
		int ix = (int) Math.floor(gx);
		int iy = (int) Math.floor(gy);
		double fx = gx - ix;
		double fy = gy - iy;
		double[] col = new double[4];
		for (int j = 0; j < 4; j++) {
			double p0 = px(ix - 1, iy - 1 + j), p1 = px(ix, iy - 1 + j), p2 = px(ix + 1, iy - 1 + j), p3 = px(ix + 2, iy - 1 + j);
			col[j] = cubic(p0, p1, p2, p3, fx);
		}
		return cubic(col[0], col[1], col[2], col[3], fy);
	}

	/** Catmull-Rom, with the overshoot clamped so that cliffs do not ring. */
	private static double cubic(double p0, double p1, double p2, double p3, double t) {
		double v = p1 + 0.5 * t * (p2 - p0 + t * (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3 + t * (3.0 * (p1 - p2) + p3 - p0)));
		double lo = Math.min(p1, p2), hi = Math.max(p1, p2);
		double slack = (hi - lo) * 0.25 + 0.5;
		return Math.max(lo - slack, Math.min(hi + slack, v));
	}

	/** One elevation sample in metres (global pixel at {@link #ZOOM}). */
	public static double px(int gx, int gy) {
		int n = 1 << ZOOM;
		int size = n * 256;
		gx = Math.floorMod(gx, size);
		gy = Math.max(0, Math.min(size - 1, gy));
		char[] t = CACHE.get(TileCache.key(ZOOM, gx >> 8, gy >> 8));
		if (t != FLAT && t.length > 0) {
			char v = t[(gy & 255) * 256 + (gx & 255)];
			if (v != 0) {
				return v * 0.5 - OFFSET;
			}
		}
		// The detailed tile is missing: look at coarser zooms.
		for (int k = 1; k <= 8; k++) {
			int z = ZOOM - k;
			int sx = gx >> k, sy = gy >> k;
			char[] p = CACHE.get(TileCache.key(z, sx >> 8, sy >> 8));
			if (p != FLAT && p.length > 0) {
				char v = p[(sy & 255) * 256 + (sx & 255)];
				if (v != 0) {
					return v * 0.5 - OFFSET;
				}
			}
		}
		return 0;
	}

	private static char[] load(long key) {
		int z = (int) (key >>> 58);
		int x = (int) (key >>> 29 & 0x1FFFFFFF);
		int y = (int) (key & 0x1FFFFFFF);
		TileStore s = store;
		if (s == null) {
			return FLAT;
		}
		byte[] data = s.get(z, x, y);
		if (data == null || data.length == 0) {
			return FLAT;
		}
		try {
			Png png = Png.decode(data);
			if (png.width != 256 || png.height != 256) {
				return FLAT;
			}
			char[] out = new char[256 * 256];
			for (int i = 0; i < out.length; i++) {
				int c = png.rgb[i];
				double m = ((c >> 16 & 0xFF) * 256 + (c >> 8 & 0xFF) + (c & 0xFF) / 256.0) - 32768.0;
				// 0 marks a hole in the data (it is then filled from a coarser zoom).
				out[i] = m < -11500 ? 0 : (char) Math.max(1, Math.min(65535, Math.round((m + OFFSET) * 2.0)));
			}
			return out;
		} catch (IOException | RuntimeException e) {
			EarthLog.warn.accept("bad elevation tile " + z + "/" + x + "/" + y + ": " + e);
			return FLAT;
		}
	}

	public static void clear() {
		CACHE.clear();
	}
}
