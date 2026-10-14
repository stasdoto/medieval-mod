package com.stasdoto.medieval.earth;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Large named areas from Natural Earth: deserts, steppes, tundra, modern reservoirs (which did not exist in 1050 and are
 * turned back into river valleys) and the great marshes of the time.
 */
public final class Regions {
	public static final int NONE = 0, DESERT = 1, STEPPE = 2, TUNDRA = 3, RESERVOIR = 4, MARSH = 5, ARID = 6;

	private record Poly(int kind, String name, double minLon, double minLat, double maxLon, double maxLat, double[][] rings) {
	}

	private static final List<Poly> POLYS = new ArrayList<>();
	/** Polygons by 1° cell (key lonCell * 1000 + latCell). */
	private static final Map<Integer, List<Poly>> GRID = new HashMap<>();
	private static volatile boolean loaded;

	private Regions() {
	}

	public static synchronized void load() {
		if (loaded) {
			return;
		}
		try (InputStream raw = Regions.class.getResourceAsStream("/medieval/geo/regions.bin.gz")) {
			if (raw == null) {
				EarthLog.warn.accept("regions data missing");
				loaded = true;
				return;
			}
			DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(new GZIPInputStream(raw)));
			int count = in.readInt();
			for (int i = 0; i < count; i++) {
				int kind = in.readByte();
				byte[] nb = new byte[in.readShort()];
				in.readFully(nb);
				int nr = in.readInt();
				double[][] rings = new double[nr][];
				double a = 999, b = 999, c = -999, d = -999;
				for (int r = 0; r < nr; r++) {
					int n = in.readInt();
					double[] ring = new double[n * 2];
					for (int k = 0; k < n; k++) {
						double lon = in.readInt() / 1e6, lat = in.readInt() / 1e6;
						ring[k * 2] = lon;
						ring[k * 2 + 1] = lat;
						a = Math.min(a, lon);
						b = Math.min(b, lat);
						c = Math.max(c, lon);
						d = Math.max(d, lat);
					}
					rings[r] = ring;
				}
				Poly p = new Poly(kind, new String(nb, java.nio.charset.StandardCharsets.UTF_8), a, b, c, d, rings);
				POLYS.add(p);
				for (int x = (int) Math.floor(a); x <= (int) Math.floor(c); x++) {
					for (int y = (int) Math.floor(b); y <= (int) Math.floor(d); y++) {
						GRID.computeIfAbsent(cell(x, y), k -> new ArrayList<>()).add(p);
					}
				}
			}
			EarthLog.info.accept("regions: " + POLYS.size() + " areas");
		} catch (IOException e) {
			EarthLog.warn.accept("cannot read regions: " + e);
		}
		loaded = true;
	}

	private static int cell(int lonCell, int latCell) {
		return (lonCell + 200) * 1000 + (latCell + 100);
	}

	/** The kind of area at a point (the first match), or {@link #NONE}. */
	public static int kind(double lat, double lon) {
		return kind(lat, lon, -1);
	}

	/** Is the point inside an area of the given kind. */
	public static boolean in(double lat, double lon, int kind) {
		return kind(lat, lon, kind) == kind;
	}

	private static int kind(double lat, double lon, int only) {
		if (!loaded) {
			load();
		}
		List<Poly> list = GRID.get(cell((int) Math.floor(lon), (int) Math.floor(lat)));
		if (list == null) {
			return NONE;
		}
		for (Poly p : list) {
			if (only >= 0 && p.kind != only) {
				continue;
			}
			if (lon < p.minLon || lon > p.maxLon || lat < p.minLat || lat > p.maxLat) {
				continue;
			}
			if (inside(p.rings, lon, lat)) {
				return p.kind;
			}
		}
		return NONE;
	}

	/** Name of the area of a kind at a point, or null. */
	public static String name(double lat, double lon, int kind) {
		if (!loaded) {
			load();
		}
		List<Poly> list = GRID.get(cell((int) Math.floor(lon), (int) Math.floor(lat)));
		if (list != null) {
			for (Poly p : list) {
				if (p.kind == kind && inside(p.rings, lon, lat)) {
					return p.name;
				}
			}
		}
		return null;
	}

	static boolean inside(double[][] rings, double x, double y) {
		boolean in = false;
		for (double[] r : rings) {
			int n = r.length / 2;
			for (int i = 0, j = n - 1; i < n; j = i++) {
				double xi = r[i * 2], yi = r[i * 2 + 1], xj = r[j * 2], yj = r[j * 2 + 1];
				if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
					in = !in;
				}
			}
		}
		return in;
	}
}
