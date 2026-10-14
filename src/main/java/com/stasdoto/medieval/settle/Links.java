package com.stasdoto.medieval.settle;

import java.util.ArrayList;
import java.util.List;

import com.stasdoto.medieval.earth.Terrain;
import com.stasdoto.medieval.earth.TileCache;

/**
 * Country tracks between neighbouring settlements: each settlement is joined to its two nearest neighbours within a
 * few kilometres. The track heads for the other place, turning aside around water and steep slopes; small brooks are
 * forded or bridged by the road writer.
 */
public final class Links {
	private Links() {
	}

	public record Link(Site a, Site b, Layout.Road road) {
	}

	private static final TileCache<List<Link>> CELLS = new TileCache<>(256, Links::compute);
	public static final int MAX = 3600;

	/** The tracks that start in a region square (each pair is kept once, in the square of its western end). */
	public static List<Link> cell(int cx, int cz) {
		return CELLS.get(((long) (cx + (1 << 20)) << 21) | (cz + (1 << 20)));
	}

	/** Tracks that may pass through the square of blocks x0..x1, z0..z1. */
	public static List<Link> near(int x0, int z0, int x1, int z1) {
		List<Link> out = new ArrayList<>();
		int cx0 = Math.floorDiv(x0 - MAX, Sites.CELL), cx1 = Math.floorDiv(x1 + MAX, Sites.CELL);
		int cz0 = Math.floorDiv(z0 - MAX, Sites.CELL), cz1 = Math.floorDiv(z1 + MAX, Sites.CELL);
		for (int cx = cx0; cx <= cx1; cx++) {
			for (int cz = cz0; cz <= cz1; cz++) {
				for (Link l : cell(cx, cz)) {
					Layout.Road r = l.road;
					if (r != null && r.maxX >= x0 && r.minX <= x1 && r.maxZ >= z0 && r.minZ <= z1) {
						out.add(l);
					}
				}
			}
		}
		return out;
	}

	private static List<Link> compute(long key) {
		int cx = (int) (key >>> 21) - (1 << 20);
		int cz = (int) (key & ((1 << 21) - 1)) - (1 << 20);
		List<Link> out = new ArrayList<>();
		if (!Sites.enabled) {
			return out;
		}
		List<Site> mine = Sites.cell(cx, cz);
		List<Site> around = new ArrayList<>();
		for (int i = -2; i <= 2; i++) {
			for (int j = -2; j <= 2; j++) {
				around.addAll(Sites.cell(cx + i, cz + j));
			}
		}
		for (Site a : mine) {
			if (a.kind == Site.Kind.CAMP) {
				continue;
			}
			// The two nearest neighbours.
			Site n1 = null, n2 = null;
			double d1 = Double.MAX_VALUE, d2 = Double.MAX_VALUE;
			for (Site b : around) {
				if (b == a || b.kind == Site.Kind.CAMP) {
					continue;
				}
				double d = Math.hypot(b.x - a.x, b.z - a.z);
				if (d > MAX) {
					continue;
				}
				if (d < d1) {
					d2 = d1;
					n2 = n1;
					d1 = d;
					n1 = b;
				} else if (d < d2) {
					d2 = d;
					n2 = b;
				}
			}
			for (Site b : new Site[]{n1, n2}) {
				if (b == null) {
					continue;
				}
				// Keep each pair once: from the one with the smaller id.
				if (a.id > b.id && bothNear(b, a, around)) {
					continue;
				}
				Layout.Road r = route(a, b);
				if (r != null) {
					out.add(new Link(a, b, r));
				}
			}
		}
		return out;
	}

	/** Would b also pick a as one of its two nearest (so that the pair is made from b's side)? */
	private static boolean bothNear(Site b, Site a, List<Site> around) {
		int closer = 0;
		double dab = Math.hypot(b.x - a.x, b.z - a.z);
		for (Site c : around) {
			if (c != b && c != a && c.kind != Site.Kind.CAMP && Math.hypot(c.x - b.x, c.z - b.z) < dab) {
				closer++;
			}
		}
		return closer < 2 && Sites.cell(Math.floorDiv(b.x, Sites.CELL), Math.floorDiv(b.z, Sites.CELL)).contains(b);
	}

	/** Walks from a to b in steps of 8 blocks, turning aside around water and cliffs. */
	static Layout.Road route(Site a, Site b) {
		List<Integer> pts = new ArrayList<>();
		double x = a.x, z = a.z;
		pts.add(a.x);
		pts.add(a.z);
		double lastH = Terrain.column(a.x, a.z).meters;
		int step = 8;
		int maxSteps = (int) (Math.hypot(b.x - a.x, b.z - a.z) / step * 2.2) + 20;
		double heading = Math.atan2(b.z - a.z, b.x - a.x);
		for (int i = 0; i < maxSteps; i++) {
			double dx = b.x - x, dz = b.z - z;
			double dist = Math.hypot(dx, dz);
			if (dist < step * 1.5) {
				pts.add(b.x);
				pts.add(b.z);
				break;
			}
			double want = Math.atan2(dz, dx);
			// Turn gently towards the goal.
			double diff = Math.atan2(Math.sin(want - heading), Math.cos(want - heading));
			heading += Math.max(-0.5, Math.min(0.5, diff));
			boolean moved = false;
			for (double turn : new double[]{0, 0.35, -0.35, 0.7, -0.7, 1.1, -1.1, 1.5, -1.5}) {
				double h = heading + turn;
				double nx = x + Math.cos(h) * step, nz = z + Math.sin(h) * step;
				Terrain.Col c = Terrain.column((int) nx, (int) nz);
				boolean wet = c.waterTop != Terrain.NO_WATER && !(c.waterKind == Terrain.W_RIVER && c.waterTop - c.top() <= 2);
				if (wet || Math.abs(c.meters - lastH) / step > 0.45) {
					continue;
				}
				x = nx;
				z = nz;
				heading = h;
				lastH = c.meters;
				pts.add((int) Math.round(x));
				pts.add((int) Math.round(z));
				moved = true;
				break;
			}
			if (!moved) {
				return null; // blocked (a wide river, a mountain): no road here
			}
		}
		if (pts.size() < 6) {
			return null;
		}
		int last = pts.size() - 2;
		if (Math.hypot(pts.get(last) - b.x, pts.get(last + 1) - b.z) > step * 2) {
			return null;
		}
		int[] arr = pts.stream().mapToInt(Integer::intValue).toArray();
		return new Layout.Road(arr, 3, Layout.Surface.DIRT);
	}
}
