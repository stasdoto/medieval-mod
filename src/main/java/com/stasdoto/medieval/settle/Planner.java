package com.stasdoto.medieval.settle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.stasdoto.medieval.earth.Cover;
import com.stasdoto.medieval.earth.Dem;
import com.stasdoto.medieval.earth.Noise;
import com.stasdoto.medieval.earth.Projection;
import com.stasdoto.medieval.earth.Terrain;
import com.stasdoto.medieval.earth.TileCache;
import com.stasdoto.medieval.history.Culture;

/**
 * Lays out a settlement on the real ground: streets that follow the slopes and stop at the water, plots along them,
 * the church or mosque and the well in the middle, the citadel on the highest point with its walls, the market by the
 * gate, and the fields all around.
 */
public final class Planner {
	private static final TileCache<Layout> CACHE = new TileCache<>(96, Planner::compute);
	private static final java.util.Map<Long, Site> BY_ID = new java.util.concurrent.ConcurrentHashMap<>();

	public static Layout layout(Site s) {
		BY_ID.put(s.id, s);
		return CACHE.get(s.id);
	}

	private static Layout compute(long id) {
		Site s = BY_ID.get(id);
		return new Planner(s).plan();
	}

	public static void clear() {
		CACHE.clear();
	}

	private final Site s;
	private final Layout L;
	private final Random r;
	private final Set<Long> used = new HashSet<>();
	private int homes;

	private Planner(Site s) {
		this.s = s;
		this.L = new Layout(s);
		this.r = new Random(s.id ^ 0x2545F4914F6CDD1DL);
	}

	private Layout plan() {
		L.centerY = Terrain.column(s.x, s.z).top();
		switch (s.kind) {
			case CAMP -> camp();
			case HAMLET, VILLAGE, BIG_VILLAGE -> village();
			case CASTLE -> castle();
			case MONASTERY -> monastery();
			default -> town();
		}
		double built = 0;
		for (Layout.Lot l : L.lots) {
			built = Math.max(built, Math.hypot((l.x0 + l.x1) / 2.0 - s.x, (l.z0 + l.z1) / 2.0 - s.z) + 12);
		}
		for (Layout.Wall w : L.walls) {
			if (!w.citadel) {
				built = Math.max(built, w.maxRadius() + Math.hypot(w.cx - s.x, w.cz - s.z) + 14);
			}
		}
		L.fieldInner = (int) built;
		L.fieldOuter = s.kind == Site.Kind.CAMP ? 0 : Math.max((int) built + 40, s.fieldRadius());
		return L;
	}

	// ---- terrain helpers ----

	private static boolean dry(Terrain.Col c) {
		return c.waterTop == Terrain.NO_WATER;
	}

	private static double h(int x, int z) {
		return Dem.meters(Projection.lat(z + 0.5), Projection.lon(x + 0.5));
	}

	/** Direction along the slope's contour (streets of villages run along the hillside), radians. */
	private double contour(int x, int z) {
		double gx = h(x + 20, z) - h(x - 20, z), gz = h(x, z + 20) - h(x, z - 20);
		if (Math.hypot(gx, gz) < 0.6) {
			return r.nextDouble() * Math.PI;
		}
		return Math.atan2(gz, gx) + Math.PI / 2;
	}

	private static long cell(int x, int z) {
		return ((long) Math.floorDiv(x, 2) << 32) ^ (Math.floorDiv(z, 2) & 0xFFFFFFFFL);
	}

	private boolean free(int x0, int z0, int x1, int z1) {
		for (int x = x0; x <= x1; x += 2) {
			for (int z = z0; z <= z1; z += 2) {
				if (used.contains(cell(x, z))) {
					return false;
				}
			}
		}
		return !used.contains(cell(x1, z1)) && !used.contains(cell(x0, z1)) && !used.contains(cell(x1, z0));
	}

	private void mark(int x0, int z0, int x1, int z1) {
		for (int x = x0; x <= x1 + 1; x += 2) {
			for (int z = z0; z <= z1 + 1; z += 2) {
				used.add(cell(x, z));
			}
		}
	}

	private boolean nearWall(int x0, int z0, int x1, int z1) {
		for (Layout.Wall w : L.walls) {
			int[][] pts = {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}, {(x0 + x1) / 2, (z0 + z1) / 2}};
			double sgn = 0;
			for (int[] p : pts) {
				double a = Math.atan2(p[1] - w.cz, p[0] - w.cx);
				double d = Math.hypot(p[0] - w.cx, p[1] - w.cz) - w.radiusAt(a);
				if (Math.abs(d) < (w.type == Layout.WallType.RAMPART ? 11 : 6)) {
					return true;
				}
				if (sgn != 0 && Math.signum(d) != sgn) {
					return true;
				}
				sgn = Math.signum(d);
			}
		}
		return false;
	}

	/** Tries to put a building with its centre at (cx, cz) facing {@code rot}. */
	private Layout.Lot place(BKind k, int cx, int cz, int rot) {
		int ex = rot % 2 == 0 ? k.w : k.d, ez = rot % 2 == 0 ? k.d : k.w;
		int x0 = cx - ex / 2, z0 = cz - ez / 2, x1 = x0 + ex - 1, z1 = z0 + ez - 1;
		if (!free(x0 - 1, z0 - 1, x1 + 1, z1 + 1) || nearWall(x0, z0, x1, z1)) {
			return null;
		}
		int[] hs = new int[9];
		int n = 0;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				int x = x0 + (x1 - x0) * i / 2, z = z0 + (z1 - z0) * j / 2;
				Terrain.Col c = Terrain.column(x, z);
				if (!dry(c) || c.cover == Cover.ICE) {
					return null;
				}
				if (c.cover == Cover.WETLAND && k != BKind.STALL) {
					return null;
				}
				hs[n++] = c.top();
			}
		}
		java.util.Arrays.sort(hs);
		int tol = k.big() ? 10 : k == BKind.HOMESTEAD ? 9 : 7;
		if (hs[8] - hs[0] > tol) {
			return null;
		}
		int floor = hs[4];
		Layout.Lot lot = new Layout.Lot(x0, z0, x1, z1, rot, floor, k, r.nextLong());
		L.lots.add(lot);
		L.grow(x0, z0, x1, z1);
		mark(x0 - 1, z0 - 1, x1 + 1, z1 + 1);
		if (k == BKind.HOMESTEAD || k == BKind.HOUSE || k == BKind.HOUSE_RICH || k == BKind.YURT) {
			homes++;
		}
		return lot;
	}

	/** Rotation whose front faces the direction (dx, dz). */
	private static int facing(double dx, double dz) {
		if (Math.abs(dx) > Math.abs(dz)) {
			return dx > 0 ? 3 : 1;
		}
		return dz > 0 ? 0 : 2;
	}

	/** Puts a building near a point, trying a few spots around it. */
	private Layout.Lot near(BKind k, int x, int z, int rot, int tries) {
		for (int t = 0; t < tries; t++) {
			int ox = t == 0 ? 0 : r.nextInt(2 * (8 + t * 3) + 1) - (8 + t * 3);
			int oz = t == 0 ? 0 : r.nextInt(2 * (8 + t * 3) + 1) - (8 + t * 3);
			Layout.Lot l = place(k, x + ox, z + oz, rot);
			if (l != null) {
				return l;
			}
		}
		return null;
	}

	// ---- streets ----

	/** A street from a point in a direction, bending a little, stopping at water, steep slopes or the given distance. */
	private int[] march(int x, int z, double angle, int length, double maxSlope) {
		List<Integer> pts = new ArrayList<>();
		double px = x, pz = z, a = angle;
		pts.add(x);
		pts.add(z);
		double lastH = h(x, z);
		int step = 6;
		for (int d = step; d <= length; d += step) {
			a += Noise.at(px / 90.0, pz / 90.0, (int) (s.id & 0xFFFF)) * 0.12;
			double nx = px + Math.cos(a) * step, nz = pz + Math.sin(a) * step;
			Terrain.Col c = Terrain.column((int) nx, (int) nz);
			if (!dry(c)) {
				break;
			}
			double hh = c.meters;
			if (Math.abs(hh - lastH) / step > maxSlope) {
				break;
			}
			lastH = hh;
			px = nx;
			pz = nz;
			pts.add((int) Math.round(px));
			pts.add((int) Math.round(pz));
		}
		int[] out = new int[pts.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = pts.get(i);
		}
		return out;
	}

	private Layout.Road road(int[] pts, int width, Layout.Surface surface) {
		if (pts.length < 4) {
			return null;
		}
		Layout.Road rd = new Layout.Road(pts, width, surface);
		L.roads.add(rd);
		// Mark the street as taken so that no house stands on it.
		for (int i = 0; i + 3 < pts.length; i += 2) {
			int ax = pts[i], az = pts[i + 1], bx = pts[i + 2], bz = pts[i + 3];
			int steps = Math.max(1, (int) Math.hypot(bx - ax, bz - az));
			for (int k = 0; k <= steps; k++) {
				int cx = ax + (bx - ax) * k / steps, cz = az + (bz - az) * k / steps;
				int hw = (width + 1) / 2;
				mark(cx - hw, cz - hw, cx + hw, cz + hw);
			}
		}
		return rd;
	}

	private static int[] join(int[] back, int[] fwd) {
		// back runs from the centre outwards: reverse it, then continue with fwd (both start at the centre).
		int n = back.length / 2, m = fwd.length / 2;
		int[] out = new int[(n + m - 1) * 2];
		int k = 0;
		for (int i = n - 1; i >= 0; i--) {
			out[k++] = back[i * 2];
			out[k++] = back[i * 2 + 1];
		}
		for (int i = 1; i < m; i++) {
			out[k++] = fwd[i * 2];
			out[k++] = fwd[i * 2 + 1];
		}
		return out;
	}

	private Layout.Surface surface() {
		return switch (s.culture) {
			case RUS, BALT_FINN -> s.kind.urban() ? Layout.Surface.LOGS : Layout.Surface.DIRT;
			case LATIN, WEST_SLAV, MAGYAR, CELTIC, NORSE -> s.kind.urban() ? Layout.Surface.GRAVEL : Layout.Surface.DIRT;
			case BYZANTINE, CAUCASUS -> s.kind.urban() ? Layout.Surface.COBBLE : Layout.Surface.GRAVEL;
			case ANDALUSI, ARAB, PERSIAN, STEPPE -> Layout.Surface.SAND;
		};
	}

	/** Plots along both sides of a street until {@code limit} homes are placed. */
	private void lotsAlong(Layout.Road rd, BKind kind, int limit, double richShare) {
		int[] p = rd.pts;
		int n = p.length / 2;
		if (n < 2) {
			return;
		}
		double acc = 0;
		double spacing = kind.w + 1.5;
		double next = spacing / 2;
		for (int i = 0; i + 1 < n && homes < limit; i++) {
			double ax = p[i * 2], az = p[i * 2 + 1], bx = p[i * 2 + 2], bz = p[i * 2 + 3];
			double len = Math.hypot(bx - ax, bz - az);
			if (len < 0.01) {
				continue;
			}
			double tx = (bx - ax) / len, tz = (bz - az) / len;
			while (next <= acc + len && homes < limit) {
				double f = next - acc;
				double px = ax + tx * f, pz = az + tz * f;
				for (int side = -1; side <= 1; side += 2) {
					BKind k = kind;
					if (kind == BKind.HOUSE && r.nextDouble() < richShare) {
						k = BKind.HOUSE_RICH;
					}
					double nx = -tz * side, nz = tx * side;
					int rot = facing(-nx, -nz);
					// Right by the street first; then a little along it or further back.
					outer:
					for (double back : new double[]{3.5, 6.0}) {
						for (double along : new double[]{0, 3, -3}) {
							double off = rd.width / 2.0 + back + k.d / 2.0;
							if (place(k, (int) Math.round(px + nx * off + tx * along), (int) Math.round(pz + nz * off + tz * along), rot) != null) {
								break outer;
							}
						}
					}
				}
				next += spacing + r.nextInt(3);
			}
			acc += len;
		}
	}

	// ---- kinds of settlements ----

	private void village() {
		double dir = contour(s.x, s.z);
		int half = Math.max(45, s.houses * 10 / 2 + 20);
		Layout.Surface sf = surface();
		Layout.Road main = road(join(march(s.x, s.z, dir + Math.PI, half, 0.5), march(s.x, s.z, dir, half, 0.5)), 4, sf);
		// The middle: well, church or mosque, smithy.
		near(BKind.WELL, s.x + (int) (Math.cos(dir + Math.PI / 2) * 8), s.z + (int) (Math.sin(dir + Math.PI / 2) * 8), 0, 6);
		boolean church = s.christian() && (s.kind == Site.Kind.BIG_VILLAGE || s.kind == Site.Kind.VILLAGE && r.nextDouble() < 0.45);
		if (church) {
			int cx = s.x + (int) (Math.cos(dir - Math.PI / 2) * 16), cz = s.z + (int) (Math.sin(dir - Math.PI / 2) * 16);
			near(s.kind == Site.Kind.BIG_VILLAGE ? BKind.CHURCH : BKind.CHURCH_SMALL, cx, cz, facing(s.x - cx, s.z - cz), 8);
		} else if (s.muslim() && s.kind != Site.Kind.HAMLET) {
			int cx = s.x + (int) (Math.cos(dir - Math.PI / 2) * 16), cz = s.z + (int) (Math.sin(dir - Math.PI / 2) * 16);
			near(BKind.MOSQUE, cx, cz, facing(s.x - cx, s.z - cz), 8);
		}
		if (s.culture == Culture.NORSE && s.kind == Site.Kind.BIG_VILLAGE) {
			int cx = s.x + (int) (Math.cos(dir + Math.PI / 2) * 22), cz = s.z + (int) (Math.sin(dir + Math.PI / 2) * 22);
			near(BKind.HALL, cx, cz, facing(s.x - cx, s.z - cz), 8);
		}
		if (main != null) {
			lotsAlong(main, BKind.HOMESTEAD, s.houses, 0);
		}
		if (s.houses > 14 && homes < s.houses) {
			Layout.Road cross = road(join(march(s.x, s.z, dir - Math.PI / 2, half / 2, 0.5), march(s.x, s.z, dir + Math.PI / 2, half / 2, 0.5)), 3, sf);
			if (cross != null) {
				lotsAlong(cross, BKind.HOMESTEAD, s.houses, 0);
			}
		}
		// Lanes branching off the streets until the village has its homes.
		for (int tries = 0; tries < 8 && homes < s.houses && !L.roads.isEmpty(); tries++) {
			Layout.Road from = L.roads.get(r.nextInt(L.roads.size()));
			int n = from.pts.length / 2;
			if (n < 4) {
				continue;
			}
			int k = 1 + r.nextInt(n - 2);
			int bx = from.pts[k * 2], bz = from.pts[k * 2 + 1];
			double ta = Math.atan2(from.pts[k * 2 + 3] - from.pts[k * 2 - 1], from.pts[k * 2 + 2] - from.pts[k * 2 - 2]);
			double ba = ta + (r.nextBoolean() ? Math.PI / 2 : -Math.PI / 2);
			Layout.Road lane = road(march(bx + (int) (Math.cos(ba) * 4), bz + (int) (Math.sin(ba) * 4), ba, 50 + r.nextInt(50), 0.5), 3, sf);
			if (lane != null) {
				lotsAlong(lane, BKind.HOMESTEAD, s.houses, 0);
			}
		}
		if (s.kind != Site.Kind.HAMLET) {
			int ex = s.x + (int) (Math.cos(dir) * (half - 10)), ez = s.z + (int) (Math.sin(dir) * (half - 10));
			near(BKind.SMITHY, ex + (int) (Math.cos(dir + Math.PI / 2) * 14), ez + (int) (Math.sin(dir + Math.PI / 2) * 14), facing(-Math.cos(dir + Math.PI / 2), -Math.sin(dir + Math.PI / 2)), 6);
			near(BKind.GRANARY, s.x - (int) (Math.cos(dir) * (half - 14)), s.z - (int) (Math.sin(dir) * (half - 14)), 0, 6);
		}
	}

	private void camp() {
		int n = s.houses;
		double ring = 14 + n * 1.6;
		for (int i = 0; i < n; i++) {
			double a = i * 2 * Math.PI / n + r.nextDouble() * 0.2;
			double rr = ring * (0.8 + r.nextDouble() * 0.4);
			int x = s.x + (int) (Math.cos(a) * rr), z = s.z + (int) (Math.sin(a) * rr);
			near(BKind.YURT, x, z, facing(s.x - x, s.z - z), 3);
		}
		near(BKind.YURT, s.x, s.z, 0, 2);
	}

	private Layout.Wall ring(int cx, int cz, double radius, Layout.WallType type, int height, boolean citadel) {
		Layout.Wall w = new Layout.Wall(cx, cz, type, height, citadel);
		int seed = (int) (s.id >>> 7);
		for (int i = 0; i < 64; i++) {
			double a = i * 2 * Math.PI / 64;
			double rr = radius * (1 + Noise.at(Math.cos(a) * 1.3, Math.sin(a) * 1.3, seed) * 0.18);
			// Pull the wall back from water and cliffs edges.
			for (int t = 0; t < 12; t++) {
				Terrain.Col c = Terrain.column(cx + (int) (Math.cos(a) * rr), cz + (int) (Math.sin(a) * rr));
				if (dry(c)) {
					break;
				}
				rr *= 0.9;
			}
			w.radius[i] = (float) rr;
		}
		// Smooth.
		float[] sm = w.radius.clone();
		for (int i = 0; i < 64; i++) {
			w.radius[i] = (sm[(i + 63) % 64] + 2 * sm[i] + sm[(i + 1) % 64]) / 4;
		}
		L.walls.add(w);
		return w;
	}

	private Layout.WallType wallType(boolean big) {
		return switch (s.culture) {
			case RUS, BALT_FINN, WEST_SLAV, MAGYAR -> Layout.WallType.RAMPART;
			case NORSE, CELTIC -> Layout.WallType.PALISADE;
			case LATIN -> big ? Layout.WallType.STONE : Layout.WallType.PALISADE;
			case BYZANTINE, CAUCASUS -> Layout.WallType.STONE;
			case ANDALUSI, ARAB, PERSIAN -> Layout.WallType.MUDBRICK;
			case STEPPE -> Layout.WallType.PALISADE;
		};
	}

	/** Opens gates where streets cross a wall. */
	private void gates(Layout.Wall w) {
		for (Layout.Road rd : L.roads) {
			int[] p = rd.pts;
			for (int i = 0; i + 1 < p.length / 2; i++) {
				double a0 = Math.atan2(p[i * 2 + 1] - w.cz, p[i * 2] - w.cx);
				double d0 = Math.hypot(p[i * 2] - w.cx, p[i * 2 + 1] - w.cz) - w.radiusAt(a0);
				double a1 = Math.atan2(p[i * 2 + 3] - w.cz, p[i * 2 + 2] - w.cx);
				double d1 = Math.hypot(p[i * 2 + 2] - w.cx, p[i * 2 + 3] - w.cz) - w.radiusAt(a1);
				if (Math.signum(d0) != Math.signum(d1)) {
					int k = (int) Math.round((a0 / (2 * Math.PI) * 64 + 64)) % 64;
					w.gate[k] = true;
				}
			}
		}
	}

	private void castle() {
		Layout.WallType type = s.culture == Culture.LATIN && r.nextBoolean() ? Layout.WallType.STONE : wallType(false);
		double dir = contour(s.x, s.z);
		Layout.Lot keep = near(BKind.KEEP, s.x, s.z, facing(Math.cos(dir), Math.sin(dir)), 6);
		int kx = keep == null ? s.x : (keep.x0 + keep.x1) / 2, kz = keep == null ? s.z : (keep.z0 + keep.z1) / 2;
		Layout.Wall w = ring(kx, kz, 30 + r.nextInt(8), type, type == Layout.WallType.STONE ? 9 : 6, true);
		Layout.Road rd = road(march(kx, kz, dir, 140, 0.4), 4, surface());
		gates(w);
		// Inside the bailey.
		near(BKind.WELL, kx + 12, kz + 3, 0, 4);
		near(BKind.HOUSE, kx - 14, kz + 6, 1, 4);
		near(BKind.GRANARY, kx + 8, kz - 15, 2, 4);
		near(BKind.SMITHY, kx - 10, kz - 14, 2, 4);
		if (rd != null) {
			lotsAlong(rd, BKind.HOMESTEAD, s.houses, 0);
		}
	}

	private void monastery() {
		Layout.WallType type = s.culture == Culture.RUS ? Layout.WallType.PALISADE : Layout.WallType.STONE;
		Layout.Lot church = near(BKind.CHURCH, s.x, s.z, 0, 6);
		int cx = church == null ? s.x : (church.x0 + church.x1) / 2, cz = church == null ? s.z : (church.z0 + church.z1) / 2;
		Layout.Wall w = ring(cx, cz, 34, type, 6, true);
		Layout.Road rd = road(march(cx, cz + 14, Math.PI / 2, 160, 0.4), 3, surface());
		gates(w);
		near(BKind.CELLS, cx - 20, cz, 3, 4);
		near(BKind.CELLS, cx + 20, cz, 1, 4);
		near(BKind.GRANARY, cx, cz - 22, 0, 4);
		near(BKind.WELL, cx + 10, cz + 18, 0, 4);
		if (rd != null) {
			lotsAlong(rd, BKind.HOMESTEAD, Math.min(s.houses, 6), 0);
		}
	}

	private void town() {
		int R = s.radius;
		// The citadel on the highest dry, not too steep point near the centre.
		int bx = s.x, bz = s.z;
		double bestH = -1e9;
		for (int i = -2; i <= 2; i++) {
			for (int j = -2; j <= 2; j++) {
				int x = s.x + i * R / 7, z = s.z + j * R / 7;
				Terrain.Col c = Terrain.column(x, z);
				if (!dry(c)) {
					continue;
				}
				double slope = Math.abs(h(x + 10, z) - h(x - 10, z)) + Math.abs(h(x, z + 10) - h(x, z - 10));
				double score = c.meters - slope * 1.5 - Math.hypot(i, j) * 3;
				if (score > bestH) {
					bestH = score;
					bx = x;
					bz = z;
				}
			}
		}
		int rc = switch (s.kind) {
			case TOWN -> 50;
			case CITY -> 85;
			case GREAT_CITY -> 130;
			default -> 200;
		};
		Layout.WallType ct = wallType(true);
		Layout.Wall cit = ring(bx, bz, rc, ct, ct == Layout.WallType.RAMPART ? 7 : ct == Layout.WallType.STONE ? 12 : 8, true);
		Layout.Wall outer = null;
		if (s.kind.ordinal() >= Site.Kind.CITY.ordinal() || r.nextDouble() < 0.4) {
			Layout.WallType ot = s.kind == Site.Kind.TOWN ? wallType(false) : wallType(true);
			outer = ring(s.x, s.z, Math.max(rc + 70, R * 0.82), ot, ot == Layout.WallType.STONE ? 10 : 7, false);
		}
		Layout.Surface sf = surface();
		int streetW = s.kind == Site.Kind.TOWN ? 4 : 5;

		// Inside the citadel: the palace, the cathedral (or great mosque), a keep in the West, a well.
		BKind holy = s.muslim() ? BKind.MOSQUE : s.christian() ? (s.kind == Site.Kind.TOWN ? BKind.CHURCH : BKind.CATHEDRAL) : BKind.HALL;
		near(holy, bx + rc / 4, bz - rc / 5, 0, 10);
		near(BKind.HALL, bx - rc / 4, bz + rc / 6, 0, 10);
		if (s.culture == Culture.LATIN || s.culture == Culture.CELTIC || s.culture == Culture.WEST_SLAV || s.culture == Culture.MAGYAR) {
			near(BKind.KEEP, bx, bz + rc / 2 - 12, 2, 8);
		}
		near(BKind.WELL, bx, bz, 0, 6);
		// Citadel streets: a cross.
		for (int k = 0; k < 4; k++) {
			double a = k * Math.PI / 2 + 0.3;
			Layout.Road rd = road(march(bx, bz, a, rc - 6, 0.5), 4, sf);
			if (rd != null) {
				lotsAlong(rd, BKind.HOUSE_RICH, homes + 6, 1);
			}
		}

		// Radial streets out of the citadel to the edge, and ring streets.
		int radials = switch (s.kind) {
			case TOWN -> 4;
			case CITY -> 6;
			case GREAT_CITY -> 8;
			default -> 11;
		};
		List<Layout.Road> streets = new ArrayList<>();
		double a0 = r.nextDouble() * Math.PI * 2;
		for (int k = 0; k < radials; k++) {
			double a = a0 + k * 2 * Math.PI / radials + (r.nextDouble() - 0.5) * 0.3;
			int sx = bx + (int) (Math.cos(a) * (cit.radiusAt(a) - 4)), sz = bz + (int) (Math.sin(a) * (cit.radiusAt(a) - 4));
			Layout.Road rd = road(march(sx, sz, a, (int) (R * 1.0), 0.5), streetW + (k % 2 == 0 ? 1 : 0), sf);
			if (rd != null) {
				streets.add(rd);
			}
		}
		double[] rings = switch (s.kind) {
			case TOWN -> new double[]{0.62};
			case CITY -> new double[]{0.45, 0.72};
			case GREAT_CITY -> new double[]{0.38, 0.6, 0.8};
			default -> new double[]{0.3, 0.46, 0.62, 0.78, 0.92};
		};
		for (double f : rings) {
			double rr = R * f;
			if (rr < rc + 25) {
				continue;
			}
			List<Integer> pts = new ArrayList<>();
			int segs = Math.max(24, (int) (rr * 2 * Math.PI / 7));
			for (int i = 0; i <= segs; i++) {
				double a = i * 2 * Math.PI / segs;
				int x = s.x + (int) Math.round(Math.cos(a) * rr), z = s.z + (int) Math.round(Math.sin(a) * rr);
				Terrain.Col c = Terrain.column(x, z);
				boolean ok = dry(c) && Math.abs(h(x + 5, z) - h(x - 5, z)) < 5;
				if (ok) {
					pts.add(x);
					pts.add(z);
				}
				if ((!ok || i == segs) && pts.size() >= 8) {
					int[] arr = pts.stream().mapToInt(Integer::intValue).toArray();
					Layout.Road rd = road(arr, streetW - 1, sf);
					if (rd != null) {
						streets.add(rd);
					}
				}
				if (!ok) {
					pts.clear();
				}
			}
		}
		// The market square by the citadel, on the first street.
		double ma = a0;
		int mx = bx + (int) (Math.cos(ma) * (rc + 34)), mz = bz + (int) (Math.sin(ma) * (rc + 34));
		int half = s.kind == Site.Kind.TOWN ? 12 : s.kind == Site.Kind.CITY ? 18 : 24;
		{
			Terrain.Col mc = Terrain.column(mx, mz);
			if (dry(mc)) {
				L.squares.add(new Layout.Square(mx - half, mz - half, mx + half, mz + half, true));
				mark(mx - half, mz - half, mx + half, mz + half);
				for (int i = -half + 4; i <= half - 4; i += 7) {
					for (int j = -half + 4; j <= half - 4; j += 9) {
						if (r.nextDouble() < 0.6) {
							Layout.Lot st = new Layout.Lot(mx + i - 2, mz + j - 2, mx + i + 2, mz + j + 2, r.nextInt(4), mc.top(), BKind.STALL, r.nextLong());
							L.lots.add(st);
						}
					}
				}
			}
		}
		// Churches, taverns and smithies spread over the town, then homes along all streets.
		int specials = Math.max(2, s.houses / 30);
		for (int i = 0; i < specials; i++) {
			Layout.Road rd = streets.isEmpty() ? null : streets.get(r.nextInt(streets.size()));
			if (rd == null || rd.pts.length < 6) {
				continue;
			}
			int k = (rd.pts.length / 2 - 1) * (1 + r.nextInt(3)) / 4;
			int px = rd.pts[k * 2], pz = rd.pts[k * 2 + 1];
			BKind kind = i % 3 == 0 ? (s.muslim() ? BKind.MOSQUE : s.christian() ? BKind.CHURCH_SMALL : BKind.TAVERN) : i % 3 == 1 ? BKind.TAVERN : BKind.SMITHY;
			int ox = (int) (Math.signum(r.nextDouble() - 0.5) * (rd.width / 2 + 2 + kind.d / 2));
			near(kind, px + ox, pz, facing(-ox, 0), 6);
		}
		final int fx = bx, fz = bz;
		streets.sort((p, q) -> Double.compare(p.dist(fx, fz), q.dist(fx, fz)));
		int limit = s.houses;
		double rich = s.kind.ordinal() >= Site.Kind.CITY.ordinal() ? 0.12 : 0.06;
		for (Layout.Road rd : streets) {
			lotsAlong(rd, BKind.HOUSE, limit, rich);
			if (homes >= limit) {
				break;
			}
		}
		// Then lanes: a grid of narrow streets between the main ones, from the citadel outwards, until the town has its homes.
		if (homes < limit) {
			double ga = a0;
			double cosA = Math.cos(ga), sinA = Math.sin(ga);
			int spacing = 2 * BKind.HOUSE.d + 10;
			double reach = (outer != null ? outer.maxRadius() : R * 0.8);
			java.util.List<Layout.Road> lanes = new java.util.ArrayList<>();
			for (int i = 1; i * spacing < reach && homes < limit; i++) {
				for (int sgn = -1; sgn <= 1; sgn += 2) {
					// The lane runs along direction ga, offset sideways by i*spacing from the citadel.
					int ox = bx + (int) Math.round(-sinA * sgn * i * spacing), oz = bz + (int) Math.round(cosA * sgn * i * spacing);
					double laneHalf = Math.sqrt(Math.max(0, reach * reach - (double) i * spacing * i * spacing));
					if (laneHalf < 30) {
						continue;
					}
					Terrain.Col oc = Terrain.column(ox, oz);
					if (!dry(oc)) {
						continue;
					}
					int[] lane = join(march(ox, oz, ga + Math.PI, (int) laneHalf, 0.6), march(ox, oz, ga, (int) laneHalf, 0.6));
					Layout.Road rd = lanes.size() < 400 ? road(lane, 3, sf) : null;
					if (rd != null) {
						lanes.add(rd);
						lotsAlong(rd, BKind.HOUSE, limit, rich * 0.5);
					}
				}
			}
		}
		gates(cit);
		if (outer != null) {
			gates(outer);
		}
		// Roads out of town: each radial street continues as a country track.
		for (Layout.Road rd : new ArrayList<>(streets.subList(0, Math.min(streets.size(), radials)))) {
			int n = rd.pts.length / 2;
			if (n < 3) {
				continue;
			}
			int ex = rd.pts[(n - 1) * 2], ez = rd.pts[(n - 1) * 2 + 1];
			double a = Math.atan2(ez - rd.pts[(n - 3) * 2 + 1], ex - rd.pts[(n - 3) * 2]);
			road(march(ex, ez, a, s.fieldRadius() - s.radius + 60, 0.45), 3, Layout.Surface.DIRT);
		}
	}
}
