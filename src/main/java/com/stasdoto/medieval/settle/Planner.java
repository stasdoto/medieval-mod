package com.stasdoto.medieval.settle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.stasdoto.medieval.earth.Cover;
import com.stasdoto.medieval.earth.Dem;
import com.stasdoto.medieval.earth.Noise;
import com.stasdoto.medieval.earth.Projection;
import com.stasdoto.medieval.earth.Terrain;
import com.stasdoto.medieval.earth.TileCache;
import com.stasdoto.medieval.history.Culture;

/**
 * Lays out a settlement on the real ground the way old towns grew: crooked streets from the gates to the market and the
 * citadel, lanes branching off them and ending at each other, the walls following the hills and the water, quarters of
 * craftsmen, merchants and nobles (and a quarter of foreign merchants in the big cities), houses of every size wall to wall
 * in the middle and yards with gardens in the suburbs, mills and piers on the water, kilns and charcoal burners at the
 * edge, and the fields all around. Villages, castles and monasteries in the manner of each people.
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

	/** Footprints of the great landmarks, kept free of plots (set by the game; pure planning tools leave it empty). */
	public interface Blocker {
		boolean blocked(int x0, int z0, int x1, int z1, int margin);
	}

	public static volatile Blocker wonders = (x0, z0, x1, z1, m) -> false;

	private static final class Wonders {
		static boolean blocked(int x0, int z0, int x1, int z1, int margin) {
			return wonders.blocked(x0, z0, x1, z1, margin);
		}
	}

	// ---- occupancy ----

	/** One bit per block over the area of a settlement. */
	private static final class Grid {
		final int ox, oz, n;
		final long[] bits;

		Grid(int cx, int cz, int half) {
			ox = cx - half;
			oz = cz - half;
			n = 2 * half;
			bits = new long[(int) (((long) n * n + 63) / 64)];
		}

		boolean get(int x, int z) {
			int i = x - ox, j = z - oz;
			if (i < 0 || j < 0 || i >= n || j >= n) {
				return false;
			}
			long k = (long) j * n + i;
			return (bits[(int) (k >>> 6)] >>> (k & 63) & 1L) != 0;
		}

		void set(int x, int z) {
			int i = x - ox, j = z - oz;
			if (i < 0 || j < 0 || i >= n || j >= n) {
				return;
			}
			long k = (long) j * n + i;
			bits[(int) (k >>> 6)] |= 1L << (k & 63);
		}

		boolean free(int x0, int z0, int x1, int z1) {
			for (int z = z0; z <= z1; z++) {
				for (int x = x0; x <= x1; x++) {
					if (get(x, z)) {
						return false;
					}
				}
			}
			return true;
		}

		void mark(int x0, int z0, int x1, int z1) {
			for (int z = z0; z <= z1; z++) {
				for (int x = x0; x <= x1; x++) {
					set(x, z);
				}
			}
		}
	}

	private enum Zone {
		CITADEL, INNER, OUTER, SUBURB, VILLAGE
	}

	private enum Quarter {
		COMMON, CRAFT, MERCHANT, NOBLE, CLERGY, FOREIGN
	}

	private record District(int x, int z, Quarter q, Culture culture) {
	}

	private final Site s;
	private final Layout L;
	private final Random r;
	private final Grid used, streets;
	private final int seed;
	private int homes;
	// Town plan.
	private Layout.Wall citadel, middle, outer;
	private int mx, mz;
	private final List<District> districts = new ArrayList<>();
	private final List<Layout.Road> townStreets = new ArrayList<>();
	/** While growing lanes: how far to the side another street must stay (0 = no check). */
	private int clearance;

	private Planner(Site s) {
		this.s = s;
		this.L = new Layout(s);
		this.r = new Random(s.id ^ 0x2545F4914F6CDD1DL);
		int half = (int) (Math.max(s.radius, 120) * 1.35) + 420;
		this.used = new Grid(s.x, s.z, half);
		this.streets = new Grid(s.x, s.z, half);
		this.seed = (int) (s.id & 0x7FFFFFFF);
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
			built = Math.max(built, w.maxRadius() + Math.hypot(w.cx - s.x, w.cz - s.z) + 14);
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

	private boolean nearWall(int x0, int z0, int x1, int z1) {
		for (Layout.Wall w : L.walls) {
			int[][] pts = {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}, {(x0 + x1) / 2, (z0 + z1) / 2}};
			double sgn = 0;
			for (int[] p : pts) {
				double a = Math.atan2(p[1] - w.cz, p[0] - w.cx);
				double d = Math.hypot(p[0] - w.cx, p[1] - w.cz) - w.radiusAt(a);
				if (Math.abs(d) < (w.type == Layout.WallType.RAMPART ? 12 : 7)) {
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

	private static int ext(int rot, int w, int d, boolean x) {
		return (rot % 2 == 0) == x ? w : d;
	}

	/** Half the size of a w×d plot turned {@code rot}, measured along the unit direction (ux, uz): how far it reaches. */
	private static double reach(int rot, int w, int d, double ux, double uz) {
		return (ext(rot, w, d, true) * Math.abs(ux) + ext(rot, w, d, false) * Math.abs(uz)) / 2.0;
	}

	/** Tries to put a building of size w×d (in its own orientation) with its centre at (cx, cz) facing {@code rot}. */
	private Layout.Lot place(BKind k, int cx, int cz, int rot, int w, int d, int margin) {
		int ex = ext(rot, w, d, true), ez = ext(rot, w, d, false);
		int x0 = cx - ex / 2, z0 = cz - ez / 2, x1 = x0 + ex - 1, z1 = z0 + ez - 1;
		if (!used.free(x0 - margin, z0 - margin, x1 + margin, z1 + margin) || nearWall(x0, z0, x1, z1)
				|| Wonders.blocked(x0, z0, x1, z1, 10)) {
			return null;
		}
		int n = ex * ez > 400 ? 4 : 3;
		int[] hs = new int[n * n];
		int m = 0;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				int x = x0 + (x1 - x0) * i / (n - 1), z = z0 + (z1 - z0) * j / (n - 1);
				Terrain.Col c = Terrain.column(x, z);
				if (!dry(c) || c.cover == Cover.ICE) {
					return null;
				}
				if (c.cover == Cover.WETLAND && k != BKind.STALL) {
					return null;
				}
				hs[m++] = c.top();
			}
		}
		java.util.Arrays.sort(hs);
		int tol = k.big() || k == BKind.MOTTE ? 11 : k == BKind.HOMESTEAD || k == BKind.CEMETERY || k == BKind.GARDEN ? 9 : 7;
		if (hs[m - 1] - hs[0] > tol) {
			return null;
		}
		Layout.Lot lot = new Layout.Lot(x0, z0, x1, z1, rot, hs[m / 2], k, r.nextLong());
		lot.wealth = k == BKind.HOUSE_RICH || k == BKind.HALL ? 2 : 1;
		L.lots.add(lot);
		L.grow(x0, z0, x1, z1);
		used.mark(x0, z0, x1, z1);
		if (k.home()) {
			homes++;
		}
		return lot;
	}

	private Layout.Lot place(BKind k, int cx, int cz, int rot) {
		return place(k, cx, cz, rot, k.w, k.d, 1);
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
		return near(k, x, z, rot, tries, k.w, k.d, 1);
	}

	private Layout.Lot near(BKind k, int x, int z, int rot, int tries, int w, int d, int margin) {
		for (int t = 0; t < tries; t++) {
			int spread = t == 0 ? 0 : 6 + t * 3;
			int ox = t == 0 ? 0 : r.nextInt(2 * spread + 1) - spread;
			int oz = t == 0 ? 0 : r.nextInt(2 * spread + 1) - spread;
			Layout.Lot l = place(k, x + ox, z + oz, rot, w, d, margin);
			if (l != null) {
				return l;
			}
		}
		return null;
	}

	/** Same kind, somewhat varied size. */
	private int[] vary(BKind k, double lo, double hi) {
		double f = lo + r.nextDouble() * (hi - lo);
		double g = lo + r.nextDouble() * (hi - lo);
		return new int[]{Math.max(5, (int) Math.round(k.w * f)), Math.max(5, (int) Math.round(k.d * g))};
	}

	// ---- streets ----

	private interface Bound {
		boolean ok(double x, double z);
	}

	private static double turn(double a, double b) {
		double d = b - a;
		while (d > Math.PI) {
			d -= 2 * Math.PI;
		}
		while (d < -Math.PI) {
			d += 2 * Math.PI;
		}
		return d;
	}

	/**
	 * A street walked from a point: bends with the lie of the land, may be drawn towards a target, stops at water, steep
	 * slopes, houses, the edge of its area, and (if {@code join}) where it meets another street.
	 */
	private int[] walk(double x, double z, double a, int length, double maxSlope, double tx, double tz, double pull, boolean join, Bound bound) {
		List<Integer> pts = new ArrayList<>();
		double px = x, pz = z;
		pts.add((int) Math.round(px));
		pts.add((int) Math.round(pz));
		Terrain.Col c0 = Terrain.column((int) px, (int) pz);
		double lastH = c0.meters;
		int step = 4;
		int wob = seed + (int) (x * 31 + z * 17);
		for (int d = step; d <= length; d += step) {
			a += Noise.at(px / 55.0, pz / 55.0, wob) * 0.22;
			if (pull > 0) {
				a += turn(a, Math.atan2(tz - pz, tx - px)) * pull;
			}
			double nx = px + Math.cos(a) * step, nz = pz + Math.sin(a) * step;
			int ix = (int) Math.round(nx), iz = (int) Math.round(nz);
			Terrain.Col c = Terrain.column(ix, iz);
			if (!dry(c) || c.cover == Cover.WETLAND && pts.size() > 2) {
				break;
			}
			if (Math.abs(c.meters - lastH) / step > maxSlope) {
				break;
			}
			if (bound != null && !bound.ok(nx, nz)) {
				break;
			}
			if (used.get(ix, iz) && !streets.get(ix, iz)) {
				break;
			}
			if (Wonders.blocked(ix - 2, iz - 2, ix + 2, iz + 2, 4)) {
				break;
			}
			if (clearance > 0 && d >= 12) {
				// Keep room for a row of houses on each side: no running alongside another street.
				double sx = -Math.sin(a), sz = Math.cos(a);
				boolean close = false;
				for (int k = 6; k <= clearance && !close; k += 3) {
					close = streets.get((int) Math.round(nx + sx * k), (int) Math.round(nz + sz * k))
							|| streets.get((int) Math.round(nx - sx * k), (int) Math.round(nz - sz * k));
				}
				if (close && !streets.get(ix, iz)) {
					break;
				}
			}
			lastH = c.meters;
			px = nx;
			pz = nz;
			pts.add(ix);
			pts.add(iz);
			if (join && d >= 12 && (streets.get(ix, iz) || streets.get(ix + 2, iz) || streets.get(ix - 2, iz) || streets.get(ix, iz + 2)
					|| streets.get(ix, iz - 2))) {
				break;
			}
			if (pull > 0 && Math.hypot(tx - px, tz - pz) < 6) {
				break;
			}
		}
		int[] out = new int[pts.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = pts.get(i);
		}
		return out;
	}

	private int[] march(int x, int z, double angle, int length, double maxSlope) {
		return walk(x, z, angle, length, maxSlope, 0, 0, 0, false, null);
	}

	private Layout.Road road(int[] pts, int width, Layout.Surface surface) {
		if (pts.length < 4) {
			return null;
		}
		Layout.Road rd = new Layout.Road(pts, width, surface);
		L.roads.add(rd);
		int hw = (width + 1) / 2;
		for (int i = 0; i + 3 < pts.length; i += 2) {
			int ax = pts[i], az = pts[i + 1], bx = pts[i + 2], bz = pts[i + 3];
			int steps = Math.max(1, (int) Math.hypot(bx - ax, bz - az));
			for (int k = 0; k <= steps; k++) {
				int cx = ax + (bx - ax) * k / steps, cz = az + (bz - az) * k / steps;
				used.mark(cx - hw, cz - hw, cx + hw, cz + hw);
				streets.mark(cx - hw, cz - hw, cx + hw, cz + hw);
			}
		}
		return rd;
	}

	private static int[] join(int[] back, int[] fwd) {
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
		return surface(s.culture, s.kind.urban());
	}

	private static Layout.Surface surface(Culture c, boolean urban) {
		return switch (c) {
			case RUS, BALT_FINN -> urban ? Layout.Surface.LOGS : Layout.Surface.DIRT;
			case LATIN, WEST_SLAV, MAGYAR, CELTIC, NORSE -> urban ? Layout.Surface.GRAVEL : Layout.Surface.DIRT;
			case BYZANTINE, CAUCASUS -> urban ? Layout.Surface.COBBLE : Layout.Surface.GRAVEL;
			case ANDALUSI, ARAB, PERSIAN, STEPPE -> Layout.Surface.SAND;
		};
	}

	/** A point along a road at distance {@code dist} from its start: x, z, and the unit direction there. */
	private static double[] along(Layout.Road rd, double dist) {
		int[] p = rd.pts;
		double acc = 0;
		for (int i = 0; i + 3 < p.length; i += 2) {
			double len = Math.hypot(p[i + 2] - p[i], p[i + 3] - p[i + 1]);
			if (len < 0.01) {
				continue;
			}
			if (acc + len >= dist) {
				double f = (dist - acc) / len;
				return new double[]{p[i] + (p[i + 2] - p[i]) * f, p[i + 1] + (p[i + 3] - p[i + 1]) * f, (p[i + 2] - p[i]) / len, (p[i + 3] - p[i + 1]) / len};
			}
			acc += len;
		}
		int n = p.length;
		return new double[]{p[n - 2], p[n - 1], 1, 0};
	}

	private static double length(Layout.Road rd) {
		double acc = 0;
		for (int i = 0; i + 3 < rd.pts.length; i += 2) {
			acc += Math.hypot(rd.pts[i + 2] - rd.pts[i], rd.pts[i + 3] - rd.pts[i + 1]);
		}
		return acc;
	}

	/** Puts a building facing a street, near the given distance along it, on either side. */
	private Layout.Lot frontage(BKind k, int w, int d, Layout.Road rd, double dist, int margin) {
		double len = length(rd);
		for (int t = 0; t < 10; t++) {
			double at = Math.max(2, Math.min(len - 2, dist + (t / 2) * 6 * (t % 2 == 0 ? 1 : -1)));
			double[] q = along(rd, at);
			int side = r.nextBoolean() ? 1 : -1;
			for (int sd = 0; sd < 2; sd++, side = -side) {
				double nx = -q[3] * side, nz = q[2] * side;
				int rot = facing(-nx, -nz);
				double off = rd.width / 2.0 + 1.0 + reach(rot, w, d, nx, nz);
				Layout.Lot l = place(k, (int) Math.round(q[0] + nx * off), (int) Math.round(q[1] + nz * off), rot, w, d, margin);
				if (l != null) {
					return l;
				}
			}
		}
		return null;
	}

	// ---- what stands where in a town ----

	private Zone zoneAt(double x, double z) {
		if (citadel != null && inside(citadel, x, z, 0)) {
			return Zone.CITADEL;
		}
		double dm = Math.hypot(x - mx, z - mz);
		if (outer != null) {
			if (!inside(outer, x, z, 0)) {
				return Zone.SUBURB;
			}
			return dm < s.radius * 0.38 || middle != null && inside(middle, x, z, 0) ? Zone.INNER : Zone.OUTER;
		}
		return dm < s.radius * 0.4 ? Zone.INNER : dm < s.radius * 0.85 ? Zone.OUTER : Zone.SUBURB;
	}

	private static boolean inside(Layout.Wall w, double x, double z, double margin) {
		double a = Math.atan2(z - w.cz, x - w.cx);
		return Math.hypot(x - w.cx, z - w.cz) < w.radiusAt(a) - margin;
	}

	private District districtAt(double x, double z) {
		District best = null;
		double bd = Double.MAX_VALUE;
		for (District d : districts) {
			double dd = Math.hypot(x - d.x, z - d.z) * (1 + 0.25 * Noise.at(x / 90.0, z / 90.0, seed + 5));
			if (dd < bd) {
				bd = dd;
				best = d;
			}
		}
		return best;
	}

	private boolean nearWater(double x, double z, int dist) {
		for (int k = 0; k < 8; k++) {
			double a = k * Math.PI / 4;
			if (!dry(Terrain.column((int) (x + Math.cos(a) * dist), (int) (z + Math.sin(a) * dist)))) {
				return true;
			}
		}
		return false;
	}

	private boolean islam() {
		return s.muslim();
	}

	/** A craft workshop fitting the place. */
	private BKind craft(Zone zone, double x, double z) {
		boolean water = nearWater(x, z, 30);
		double k = r.nextDouble();
		if (zone == Zone.SUBURB) {
			if (water && k < 0.25) {
				return BKind.TANNERY;
			}
			if (k < 0.4) {
				return BKind.POTTERY;
			}
			if (k < 0.55) {
				return BKind.KILN;
			}
			if (k < 0.68) {
				return islam() ? BKind.POTTERY : BKind.BLOOMERY;
			}
			if (k < 0.8) {
				return BKind.CARPENTER;
			}
			if (k < 0.9) {
				return BKind.STABLE;
			}
			return s.culture == Culture.PERSIAN ? BKind.WINDMILL : BKind.SMITHY;
		}
		if (water && k < 0.15) {
			return BKind.TANNERY;
		}
		if (k < 0.3) {
			return BKind.SMITHY;
		}
		if (k < 0.45) {
			return BKind.WEAVER;
		}
		if (k < 0.58) {
			return BKind.BAKERY;
		}
		if (k < 0.7) {
			return BKind.CARPENTER;
		}
		if (k < 0.8) {
			return BKind.BREWERY;
		}
		if (k < 0.9) {
			return BKind.POTTERY;
		}
		return BKind.STABLE;
	}

	/** Kind of a plot on a town street. */
	private BKind pick(Zone zone, District d, double x, double z) {
		double k = r.nextDouble();
		Quarter q = d == null ? Quarter.COMMON : d.q;
		if (zone == Zone.CITADEL) {
			return k < 0.55 ? BKind.HOUSE_RICH : k < 0.62 ? BKind.STABLE : k < 0.67 ? BKind.GRANARY : BKind.HOUSE;
		}
		if (zone == Zone.SUBURB) {
			if (k < 0.16) {
				return craft(zone, x, z);
			}
			if (k < 0.18 && Math.hypot(x - s.x, z - s.z) > s.radius * 0.9) {
				return BKind.CHARCOAL;
			}
			if (k < 0.2) {
				return BKind.APIARY;
			}
			if (k < 0.23) {
				return BKind.GARDEN;
			}
			return k < 0.75 || islam() ? BKind.HOMESTEAD : BKind.HOUSE;
		}
		switch (q) {
			case CRAFT -> {
				return k < 0.42 ? craft(zone, x, z) : k < 0.45 ? BKind.TAVERN : BKind.HOUSE;
			}
			case MERCHANT -> {
				return k < 0.13 ? BKind.WAREHOUSE : k < 0.19 ? BKind.TAVERN : k < 0.45 ? BKind.HOUSE_RICH : k < 0.5 && islam() ? BKind.CARAVANSERAI
						: BKind.HOUSE;
			}
			case NOBLE -> {
				return k < 0.45 ? BKind.HOUSE_RICH : k < 0.5 ? BKind.STABLE : k < 0.55 ? BKind.GARDEN : k < 0.6 && towers() ? BKind.TOWER : BKind.HOUSE;
			}
			case CLERGY -> {
				return k < 0.06 ? BKind.GARDEN : k < 0.1 ? BKind.BAKERY : BKind.HOUSE;
			}
			default -> {
				if (k < 0.08) {
					return craft(zone, x, z);
				}
				if (k < 0.11) {
					return BKind.TAVERN;
				}
				if (k < 0.13 && (islam() || s.culture == Culture.BYZANTINE)) {
					return BKind.BATHHOUSE;
				}
				if (k < 0.15 && towers()) {
					return BKind.TOWER;
				}
				if (k < 0.16) {
					return BKind.SHED;
				}
				return k < (zone == Zone.INNER ? 0.26 : 0.2) ? BKind.HOUSE_RICH : BKind.HOUSE;
			}
		}
	}

	/** Towers of noble families: Italy and the Caucasus. */
	private boolean towers() {
		return s.culture == Culture.CAUCASUS || s.culture == Culture.LATIN && Projection.lat(s.z) < 45.8;
	}

	private int[] dims(BKind k, Zone zone) {
		return switch (k) {
			case HOUSE -> zone == Zone.INNER || zone == Zone.CITADEL ? new int[]{7 + r.nextInt(5), 9 + r.nextInt(5)} : new int[]{8 + r.nextInt(5), 10 + r.nextInt(5)};
			case HOUSE_RICH -> new int[]{12 + r.nextInt(6), 14 + r.nextInt(6)};
			case HOMESTEAD -> new int[]{13 + r.nextInt(7), 18 + r.nextInt(9)};
			case SHED, TOWER, WELL, SHRINE, STALL -> new int[]{k.w, k.d};
			default -> vary(k, 0.85, 1.2);
		};
	}

	/** Plots along both sides of a street, until the town has {@code limit} homes. */
	private void lotsAlong(Layout.Road rd, int limit, Culture foreignOk) {
		double len = length(rd);
		double next = 3 + r.nextInt(4);
		while (next < len - 2 && homes < limit) {
			double[] q = along(rd, next);
			Zone zone = zoneAt(q[0], q[1]);
			District dist = districtAt(q[0], q[1]);
			boolean dense = zone == Zone.INNER || zone == Zone.CITADEL;
			double step = 0;
			for (int side = -1; side <= 1; side += 2) {
				BKind k = pick(zone, dist, q[0], q[1]);
				int[] wd = dims(k, zone);
				double nx = -q[3] * side, nz = q[2] * side;
				int rot = facing(-nx, -nz);
				int margin = dense ? 0 : zone == Zone.OUTER ? 1 : 2;
				boolean alongX = Math.abs(nx) > Math.abs(nz);
				double depth = 2 * reach(rot, wd[0], wd[1], nx, nz);
				Layout.Lot got = null;
				outer:
				for (double back : dense ? new double[]{0.6, 2.0} : new double[]{2.0, 4.0}) {
					for (double sh : new double[]{0, 2, -2}) {
						double off = rd.width / 2.0 + back + depth / 2.0;
						got = place(k, (int) Math.round(q[0] + nx * off + q[2] * sh), (int) Math.round(q[1] + nz * off + q[3] * sh), rot, wd[0], wd[1], margin);
						if (got != null) {
							break outer;
						}
					}
				}
				if (got == null && k != BKind.HOUSE) {
					// The special one does not fit: an ordinary house then.
					k = zone == Zone.SUBURB && !islam() ? BKind.HOMESTEAD : BKind.HOUSE;
					wd = dims(k, zone);
					depth = 2 * reach(rot, wd[0], wd[1], nx, nz);
					double off = rd.width / 2.0 + (dense ? 0.6 : 2.0) + depth / 2.0;
					got = place(k, (int) Math.round(q[0] + nx * off), (int) Math.round(q[1] + nz * off), rot, wd[0], wd[1], margin);
				}
				if (got != null) {
					got.wealth = k == BKind.HOUSE_RICH ? 2 : k == BKind.HOUSE && (zone == Zone.SUBURB || zone == Zone.OUTER && r.nextBoolean()
							|| dist != null && dist.q == Quarter.CRAFT && r.nextInt(3) == 0) ? 0 : 1;
					if (dist != null && dist.q == Quarter.FOREIGN && foreignOk != null) {
						got.culture = dist.culture;
					}
					step = Math.max(step, 2 * reach(rot, wd[0], wd[1], q[2], q[3]));
					// A shed or workshop in the back yard.
					if (dense && got.kind.home() && r.nextInt(4) == 0) {
						double off2 = rd.width / 2.0 + 1 + depth + 4;
						place(BKind.SHED, (int) Math.round(q[0] + nx * off2), (int) Math.round(q[1] + nz * off2), rot, 5 + r.nextInt(3), 5 + r.nextInt(3), 0);
					}
				}
			}
			int gap = dense ? (r.nextInt(4) == 0 ? 1 : 0) : zone == Zone.OUTER ? 1 + r.nextInt(2) : 2 + r.nextInt(4);
			next += Math.max(4, step) + gap;
		}
	}

	// ---- walls ----

	/** An irregular ring wall: follows the hills and keeps off the water. */
	private Layout.Wall ring(int cx, int cz, double radius, Layout.WallType type, int height, boolean cit, double rough) {
		Layout.Wall w = new Layout.Wall(cx, cz, type, height, cit);
		int sd = (int) (s.id >>> 7) + (int) radius;
		// Two to four bulges and a dent: towns grew unevenly.
		int lobes = rough > 0 ? 2 + r.nextInt(3) : 0;
		double[] la = new double[lobes + 1], lh = new double[lobes + 1];
		for (int k = 0; k <= lobes; k++) {
			la[k] = r.nextDouble() * Math.PI * 2;
			lh[k] = k == lobes ? -0.12 * rough * 4 : (0.12 + r.nextDouble() * 0.22) * rough * 4;
		}
		if (rough <= 0) {
			lh[0] = 0;
		}
		for (int i = 0; i < 64; i++) {
			double a = i * 2 * Math.PI / 64;
			double f = 1 + Noise.fbm(Math.cos(a) * 1.4 + 3, Math.sin(a) * 1.4 + 3, sd, 3) * rough;
			for (int k = 0; k <= lobes; k++) {
				double dd = turn(a, la[k]);
				f += lh[k] * Math.exp(-dd * dd / 0.35);
			}
			double rr = radius * Math.max(0.55, f);
			// Pull the wall back from the water and from steep drops.
			for (int t = 0; t < 14; t++) {
				int x = cx + (int) (Math.cos(a) * rr), z = cz + (int) (Math.sin(a) * rr);
				Terrain.Col c = Terrain.column(x, z);
				if (dry(c) && Math.abs(h(x + 6, z) - h(x - 6, z)) + Math.abs(h(x, z + 6) - h(x, z - 6)) < 9) {
					break;
				}
				rr *= 0.93;
			}
			w.radius[i] = (float) rr;
		}
		float[] sm = w.radius.clone();
		for (int i = 0; i < 64; i++) {
			w.radius[i] = (sm[(i + 63) % 64] + 2 * sm[i] + sm[(i + 1) % 64]) / 4;
		}
		L.walls.add(w);
		return w;
	}

	/** A square enclosure (ribat, kastron): radius of a square of half-side {@code half}. */
	private Layout.Wall square(int cx, int cz, double half, Layout.WallType type, int height, boolean cit) {
		Layout.Wall w = new Layout.Wall(cx, cz, type, height, cit);
		for (int i = 0; i < 64; i++) {
			double a = i * 2 * Math.PI / 64;
			w.radius[i] = (float) (half / Math.max(Math.abs(Math.cos(a)), Math.abs(Math.sin(a))));
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

	private static int height(Layout.WallType t, boolean big) {
		return switch (t) {
			case RAMPART -> 7;
			case STONE -> big ? 12 : 9;
			case MUDBRICK -> big ? 10 : 8;
			case PALISADE -> 6;
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

	// ---- by the water ----

	/**
	 * Finds a spot on the shore between two distances from (cx, cz) for a building whose front stands over the water,
	 * and puts it there. {@code river}: only on rivers (mills).
	 */
	private Layout.Lot shore(BKind k, int cx, int cz, double rMin, double rMax, boolean river, int tries) {
		int[][] dirs = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};
		for (int t = 0; t < tries; t++) {
			double a = r.nextDouble() * Math.PI * 2, d = rMin + r.nextDouble() * (rMax - rMin);
			int x = cx + (int) (Math.cos(a) * d), z = cz + (int) (Math.sin(a) * d);
			Terrain.Col c = Terrain.column(x, z);
			if (!dry(c)) {
				continue;
			}
			for (int rot = 0; rot < 4; rot++) {
				int[] dir = dirs[rot];
				int found = -1;
				Terrain.Col wc = null;
				for (int st = 1; st <= 12; st++) {
					Terrain.Col q = Terrain.column(x + dir[0] * st, z + dir[1] * st);
					if (!dry(q)) {
						found = st;
						wc = q;
						break;
					}
				}
				if (found < 0 || river && wc.waterKind != Terrain.W_RIVER) {
					continue;
				}
				// The water's edge comes in the middle of the plot's depth.
				int[] wd = vary(k, 0.9, 1.15);
				int ex = ext(rot, wd[0], wd[1], true), ez = ext(rot, wd[0], wd[1], false);
				int ccx = x + dir[0] * found, ccz = z + dir[1] * found;
				int x0 = ccx - ex / 2, z0 = ccz - ez / 2, x1 = x0 + ex - 1, z1 = z0 + ez - 1;
				if (!used.free(x0 - 1, z0 - 1, x1 + 1, z1 + 1) || nearWall(x0, z0, x1, z1) || Wonders.blocked(x0, z0, x1, z1, 8)) {
					continue;
				}
				// The back must be dry and not a cliff, the front must be water.
				int depth = rot % 2 == 0 ? ez : ex;
				boolean ok = true;
				int landHi = Integer.MIN_VALUE;
				for (int i = 0; i < 3 && ok; i++) {
					double fx = (i - 1) * 0.35;
					for (double fz : new double[]{0.05, 0.25}) {
						// fz: from the back (0) to the front (1) of the plot.
						int lx = (int) Math.round(ccx + -dir[0] * depth * (0.5 - fz) + (dir[1] != 0 ? fx * ex : 0));
						int lz = (int) Math.round(ccz + -dir[1] * depth * (0.5 - fz) + (dir[0] != 0 ? fx * ez : 0));
						Terrain.Col lc = Terrain.column(lx, lz);
						if (!dry(lc)) {
							ok = false;
							break;
						}
						landHi = Math.max(landHi, lc.top());
					}
					int fxw = (int) Math.round(ccx + dir[0] * depth * 0.3 + (dir[1] != 0 ? fx * ex : 0));
					int fzw = (int) Math.round(ccz + dir[1] * depth * 0.3 + (dir[0] != 0 ? fx * ez : 0));
					if (dry(Terrain.column(fxw, fzw))) {
						ok = false;
					}
				}
				int floor = wc.waterTop + 1;
				if (!ok || landHi > floor + 4) {
					continue;
				}
				Layout.Lot lot = new Layout.Lot(x0, z0, x1, z1, rot, floor, k, r.nextLong());
				L.lots.add(lot);
				L.grow(x0, z0, x1, z1);
				used.mark(x0, z0, x1, z1);
				return lot;
			}
		}
		return null;
	}

	/** Mills, piers, fishermen and boatbuilders along the water near a settlement. */
	private void waterside(int count, double rMin, double rMax, boolean port) {
		for (int i = 0; i < count; i++) {
			double k = r.nextDouble();
			BKind kind;
			if (islam() && (s.culture == Culture.ANDALUSI || s.culture == Culture.ARAB) && k < 0.18) {
				kind = BKind.NORIA;
			} else if (k < 0.35) {
				kind = BKind.WATERMILL;
			} else if (k < (port ? 0.65 : 0.55)) {
				kind = BKind.PIER;
			} else if (k < 0.85) {
				kind = BKind.FISHERY;
			} else {
				kind = BKind.BOATYARD;
			}
			Layout.Lot l = shore(kind, s.x, s.z, rMin, rMax, kind == BKind.WATERMILL || kind == BKind.NORIA, 30);
			if (l == null && kind == BKind.WATERMILL) {
				shore(BKind.FISHERY, s.x, s.z, rMin, rMax, false, 20);
			}
		}
	}

	// ---- villages ----

	private void village() {
		Layout.Surface sf = surface();
		int style = 0;
		switch (s.culture) {
			case WEST_SLAV -> style = r.nextInt(3) == 0 ? 1 : 0;
			case LATIN, CELTIC, NORSE -> style = r.nextInt(2) == 0 ? 2 : 0;
			case ARAB, ANDALUSI, PERSIAN, BYZANTINE, CAUCASUS -> style = r.nextInt(3) == 0 ? 0 : 2;
			default -> style = r.nextInt(5) == 0 ? 2 : 0;
		}
		if (s.kind == Site.Kind.HAMLET) {
			style = r.nextInt(3) == 0 ? 2 : 0;
		}
		double dir = contour(s.x, s.z);
		if (style == 1) {
			roundVillage(sf);
		} else if (style == 2) {
			clusterVillage(sf);
		} else {
			streetVillage(dir, sf);
		}
		villageExtras(dir, sf);
	}

	private int[] homestead() {
		return new int[]{13 + r.nextInt(7), 18 + r.nextInt(8)};
	}

	/** Village plots along a road: yards of varied size with gaps between. */
	private void yardsAlong(Layout.Road rd, int limit) {
		double len = length(rd);
		double next = 6 + r.nextInt(6);
		while (next < len - 4 && homes < limit) {
			double[] q = along(rd, next);
			double step = 8;
			for (int side = -1; side <= 1; side += 2) {
				if (r.nextInt(7) == 0) {
					continue;
				}
				int[] wd = homestead();
				double nx = -q[3] * side, nz = q[2] * side;
				int rot = facing(-nx, -nz);
				boolean alongX = Math.abs(nx) > Math.abs(nz);
				double depth = 2 * reach(rot, wd[0], wd[1], nx, nz);
				Layout.Lot got = null;
				outer:
				for (double back : new double[]{2.5, 5.0, 8.0}) {
					for (double sh : new double[]{0, 3, -3}) {
						double off = rd.width / 2.0 + back + depth / 2.0;
						got = place(BKind.HOMESTEAD, (int) Math.round(q[0] + nx * off + q[2] * sh), (int) Math.round(q[1] + nz * off + q[3] * sh), rot, wd[0], wd[1], 1);
						if (got != null) {
							break outer;
						}
					}
				}
				if (got != null) {
					got.wealth = r.nextInt(6) == 0 ? 2 : r.nextInt(3) == 0 ? 0 : 1;
					step = Math.max(step, 2 * reach(rot, wd[0], wd[1], q[2], q[3]));
				}
			}
			next += step + 1 + r.nextInt(6);
		}
	}

	/** A street village (most of Rus and the plains): the main street along the slope, a cross street, lanes. */
	private void streetVillage(double dir, Layout.Surface sf) {
		int half = Math.max(45, s.houses * 10 / 2 + 20);
		Layout.Road main = road(join(march(s.x, s.z, dir + Math.PI, half, 0.5), march(s.x, s.z, dir, half, 0.5)), 4, sf);
		centre(dir);
		if (main != null) {
			yardsAlong(main, s.houses);
		}
		if (s.houses > 14 && homes < s.houses) {
			Layout.Road cross = road(join(march(s.x, s.z, dir - Math.PI / 2, half / 2, 0.5), march(s.x, s.z, dir + Math.PI / 2, half / 2, 0.5)), 3, sf);
			if (cross != null) {
				yardsAlong(cross, s.houses);
			}
		}
		for (int tries = 0; tries < 10 && homes < s.houses && !L.roads.isEmpty(); tries++) {
			Layout.Road from = L.roads.get(r.nextInt(L.roads.size()));
			double len = length(from);
			if (len < 20) {
				continue;
			}
			double[] q = along(from, 8 + r.nextDouble() * (len - 16));
			double ba = Math.atan2(q[3], q[2]) + (r.nextBoolean() ? Math.PI / 2 : -Math.PI / 2) + (r.nextDouble() - 0.5) * 0.5;
			Layout.Road lane = road(walk(q[0] + Math.cos(ba) * 4, q[1] + Math.sin(ba) * 4, ba, 40 + r.nextInt(60), 0.5, 0, 0, 0, true, null), 3, sf);
			if (lane != null) {
				yardsAlong(lane, s.houses);
			}
		}
	}

	/** A round village (Rundling) of the West Slavs: yards in a ring around a green with a pond or well. */
	private void roundVillage(Layout.Surface sf) {
		int n = Math.max(6, Math.min(s.houses, 18));
		double ring = 22 + n * 2.2;
		near(BKind.WELL, s.x, s.z, 0, 4);
		List<Integer> pts = new ArrayList<>();
		for (int i = 0; i <= 40; i++) {
			double a = i * 2 * Math.PI / 40;
			pts.add(s.x + (int) Math.round(Math.cos(a) * (ring - 12)));
			pts.add(s.z + (int) Math.round(Math.sin(a) * (ring - 12)));
		}
		road(pts.stream().mapToInt(Integer::intValue).toArray(), 3, sf);
		double a0 = r.nextDouble() * Math.PI * 2;
		for (int i = 0; i < n; i++) {
			double a = a0 + i * 2 * Math.PI / (n + 1);
			int x = s.x + (int) (Math.cos(a) * ring), z = s.z + (int) (Math.sin(a) * ring);
			int[] wd = homestead();
			Layout.Lot l = near(BKind.HOMESTEAD, x, z, facing(s.x - x, s.z - z), 3, wd[0], wd[1], 1);
			if (l != null) {
				l.wealth = r.nextInt(3) == 0 ? 0 : 1;
			}
		}
		// The way out through the gap in the ring.
		double a = a0 - Math.PI / (n + 1);
		road(march(s.x + (int) (Math.cos(a) * (ring - 12)), s.z + (int) (Math.sin(a) * (ring - 12)), a, 120, 0.5), 3, sf);
		if (homes < s.houses) {
			for (Layout.Road rd : new ArrayList<>(L.roads.subList(Math.max(0, L.roads.size() - 1), L.roads.size()))) {
				yardsAlong(rd, s.houses);
			}
		}
	}

	/** A clustered village (West, North, South): crooked lanes growing out from the middle, yards along them. */
	private void clusterVillage(Layout.Surface sf) {
		centre(r.nextDouble() * Math.PI);
		int branches = 3 + r.nextInt(3);
		for (int i = 0; i < branches; i++) {
			double a = i * 2 * Math.PI / branches + r.nextDouble() * 0.6;
			Layout.Road rd = road(walk(s.x + Math.cos(a) * 9, s.z + Math.sin(a) * 9, a, 50 + r.nextInt(60), 0.5, 0, 0, 0, true, null), 3, sf);
			if (rd != null) {
				yardsAlong(rd, s.houses);
			}
		}
		for (int tries = 0; tries < 14 && homes < s.houses && !L.roads.isEmpty(); tries++) {
			Layout.Road from = L.roads.get(r.nextInt(L.roads.size()));
			double len = length(from);
			if (len < 16) {
				continue;
			}
			double[] q = along(from, 6 + r.nextDouble() * (len - 12));
			double ba = Math.atan2(q[3], q[2]) + (r.nextBoolean() ? 1.2 : -1.2) + (r.nextDouble() - 0.5) * 0.8;
			Layout.Road lane = road(walk(q[0] + Math.cos(ba) * 4, q[1] + Math.sin(ba) * 4, ba, 30 + r.nextInt(50), 0.5, 0, 0, 0, true, null), 3, sf);
			if (lane != null) {
				yardsAlong(lane, s.houses);
			}
		}
	}

	/** The middle of a village: well, church or mosque, the lord's or elder's hall. */
	private void centre(double dir) {
		near(BKind.WELL, s.x + (int) (Math.cos(dir + Math.PI / 2) * 8), s.z + (int) (Math.sin(dir + Math.PI / 2) * 8), 0, 6);
		boolean church = s.christian() && s.culture != Culture.BALT_FINN
				&& (s.kind == Site.Kind.BIG_VILLAGE || s.kind == Site.Kind.VILLAGE && r.nextDouble() < 0.5);
		int cx = s.x + (int) (Math.cos(dir - Math.PI / 2) * 17), cz = s.z + (int) (Math.sin(dir - Math.PI / 2) * 17);
		if (church) {
			Layout.Lot c = near(s.kind == Site.Kind.BIG_VILLAGE ? BKind.CHURCH : BKind.CHURCH_SMALL, cx, cz, facing(s.x - cx, s.z - cz), 8);
			if (c != null && r.nextInt(3) != 0) {
				int bx = (c.x0 + c.x1) / 2 + (cx - s.x) / 2, bz = (c.z0 + c.z1) / 2 + (cz - s.z) / 2;
				int[] wd = vary(BKind.CEMETERY, 0.8, 1.1);
				near(BKind.CEMETERY, bx, bz, facing(s.x - bx, s.z - bz), 6, wd[0], wd[1], 1);
			}
		} else if (s.muslim() && s.kind != Site.Kind.HAMLET) {
			int[] wd = vary(BKind.MOSQUE, 0.65, 0.8);
			near(BKind.MOSQUE, cx, cz, facing(s.x - cx, s.z - cz), 8, wd[0], wd[1], 1);
		}
		if (s.culture == Culture.NORSE && s.kind == Site.Kind.BIG_VILLAGE) {
			int hx = s.x + (int) (Math.cos(dir + Math.PI / 2) * 24), hz = s.z + (int) (Math.sin(dir + Math.PI / 2) * 24);
			near(BKind.LONGHOUSE, hx, hz, facing(s.x - hx, s.z - hz), 8);
		} else if (s.kind == Site.Kind.BIG_VILLAGE && r.nextInt(3) == 0) {
			int hx = s.x + (int) (Math.cos(dir + Math.PI / 2) * 26), hz = s.z + (int) (Math.sin(dir + Math.PI / 2) * 26);
			int[] wd = vary(BKind.HOUSE_RICH, 1.0, 1.25);
			Layout.Lot l = near(BKind.HOUSE_RICH, hx, hz, facing(s.x - hx, s.z - hz), 8, wd[0], wd[1], 2);
			if (l != null) {
				l.wealth = 2;
			}
		}
	}

	/** Workshops, mills, shrines and the like around a village. */
	private void villageExtras(double dir, Layout.Surface sf) {
		int half = Math.max(45, s.houses * 10 / 2 + 20);
		boolean forest = s.culture == Culture.RUS || s.culture == Culture.BALT_FINN || s.culture == Culture.WEST_SLAV || s.culture == Culture.NORSE;
		if (s.kind != Site.Kind.HAMLET) {
			int ex = s.x + (int) (Math.cos(dir) * (half - 10)), ez = s.z + (int) (Math.sin(dir) * (half - 10));
			near(BKind.SMITHY, ex + (int) (Math.cos(dir + Math.PI / 2) * 14), ez + (int) (Math.sin(dir + Math.PI / 2) * 14),
					facing(-Math.cos(dir + Math.PI / 2), -Math.sin(dir + Math.PI / 2)), 8);
			near(BKind.GRANARY, s.x - (int) (Math.cos(dir) * (half - 14)), s.z - (int) (Math.sin(dir) * (half - 14)), r.nextInt(4), 8);
		}
		List<BKind> extra = new ArrayList<>();
		if (forest && r.nextInt(2) == 0 || r.nextInt(5) == 0) {
			extra.add(BKind.APIARY);
		}
		if (s.kind == Site.Kind.BIG_VILLAGE) {
			if (r.nextInt(3) == 0) {
				extra.add(BKind.POTTERY);
			}
			if (r.nextInt(3) == 0) {
				extra.add(BKind.CARPENTER);
			}
			if (r.nextInt(3) == 0) {
				extra.add(BKind.TAVERN);
			}
			if (r.nextInt(4) == 0) {
				extra.add(BKind.STABLE);
			}
			if (r.nextInt(4) == 0) {
				extra.add(s.culture == Culture.PERSIAN ? BKind.WINDMILL : BKind.KILN);
			}
		}
		if (s.kind != Site.Kind.HAMLET && r.nextInt(4) == 0) {
			extra.add(forest ? BKind.CHARCOAL : BKind.WEAVER);
		}
		if (s.kind != Site.Kind.HAMLET && r.nextInt(3) == 0) {
			extra.add(BKind.GARDEN);
		}
		for (BKind k : extra) {
			double a = r.nextDouble() * Math.PI * 2;
			double d = (k == BKind.CHARCOAL ? half + 30 : half * 0.7) + r.nextDouble() * 20;
			int x = s.x + (int) (Math.cos(a) * d), z = s.z + (int) (Math.sin(a) * d);
			int[] wd = vary(k, 0.9, 1.15);
			near(k, x, z, facing(s.x - x, s.z - z), 10, wd[0], wd[1], 2);
		}
		// By the water: a mill on the river, fishermen on lakes and the sea.
		if (s.kind != Site.Kind.HAMLET || r.nextBoolean()) {
			if (r.nextInt(3) != 0) {
				shore(BKind.WATERMILL, s.x, s.z, 20, half + 90, true, 24);
			}
			if (r.nextInt(2) == 0) {
				shore(r.nextInt(3) == 0 ? BKind.PIER : BKind.FISHERY, s.x, s.z, 20, half + 90, false, 24);
			}
		}
		// Wayside crosses (or stones) where the roads leave the village.
		if (r.nextInt(3) != 0) {
			for (Layout.Road rd : new ArrayList<>(L.roads.subList(0, Math.min(2, L.roads.size())))) {
				int n = rd.pts.length / 2;
				for (int end : new int[]{0, n - 1}) {
					if (r.nextBoolean()) {
						int x = rd.pts[end * 2] + r.nextInt(7) - 3, z = rd.pts[end * 2 + 1] + r.nextInt(7) - 3;
						near(BKind.SHRINE, x, z, r.nextInt(4), 4, 3, 3, 0);
					}
				}
			}
		}
	}

	private void camp() {
		int n = s.houses;
		double ring = 14 + n * 1.6;
		for (int i = 0; i < n; i++) {
			double a = i * 2 * Math.PI / n + r.nextDouble() * 0.2;
			double rr = ring * (0.8 + r.nextDouble() * 0.4);
			int x = s.x + (int) (Math.cos(a) * rr), z = s.z + (int) (Math.sin(a) * rr);
			int sz = 7 + r.nextInt(5);
			near(BKind.YURT, x, z, facing(s.x - x, s.z - z), 3, sz, sz, 1);
		}
		near(BKind.YURT, s.x, s.z, 0, 2, 11, 11, 1);
		// Kurgans of the ancestors with a stone figure on top, a little way off.
		if (r.nextBoolean()) {
			double a = r.nextDouble() * Math.PI * 2;
			int x = s.x + (int) (Math.cos(a) * (ring + 40)), z = s.z + (int) (Math.sin(a) * (ring + 40));
			near(BKind.CEMETERY, x, z, r.nextInt(4), 6, 13 + r.nextInt(6), 13 + r.nextInt(6), 2);
			near(BKind.SHRINE, x + 12, z, r.nextInt(4), 6, 3, 3, 0);
		}
	}

	// ---- castles ----

	private void castle() {
		Culture c = s.culture;
		Layout.Surface sf = surface(c, false);
		double dir = contour(s.x, s.z);
		switch (c) {
			case NORSE -> trelleborg(sf);
			case LATIN -> {
				if (r.nextInt(5) < 3) {
					motteAndBailey(dir, sf);
				} else {
					stoneCastle(dir, sf, Layout.WallType.STONE, 0);
				}
			}
			case CELTIC -> cashel(sf);
			case RUS, BALT_FINN, WEST_SLAV, MAGYAR -> gorodishche(dir, sf);
			case BYZANTINE, CAUCASUS -> stoneCastle(dir, sf, Layout.WallType.STONE, 3);
			case ANDALUSI, ARAB, PERSIAN -> ribat(sf);
			default -> stoneCastle(dir, sf, wallType(false), 2);
		}
	}

	/** Inside a castle's yard: buildings in a ring around the middle, facing it. */
	private void yard(int cx, int cz, double rad, BKind... kinds) {
		double a0 = r.nextDouble() * Math.PI * 2;
		for (int i = 0; i < kinds.length; i++) {
			BKind k = kinds[i];
			double a = a0 + i * 2 * Math.PI / kinds.length + (r.nextDouble() - 0.5) * 0.4;
			double d = rad * (0.45 + r.nextDouble() * 0.3);
			int x = cx + (int) (Math.cos(a) * d), z = cz + (int) (Math.sin(a) * d);
			int[] wd = vary(k, 0.85, 1.05);
			near(k, x, z, facing(cx - x, cz - z), 8, wd[0], wd[1], 1);
		}
	}

	/** The castle's road out of the gate with the village of its serfs along it. */
	private void castleRoad(int cx, int cz, double dir, Layout.Surface sf, Layout.Wall... walls) {
		Layout.Road rd = null;
		for (int t = 0; t < 6 && rd == null; t++) {
			rd = road(march(cx, cz, dir + t * 1.1, 170, 0.45 + t * 0.1), 4, sf);
		}
		for (Layout.Wall w : walls) {
			gates(w);
			boolean any = false;
			for (boolean g : w.gate) {
				any |= g;
			}
			if (!any) {
				// No way out found on the land: a gate all the same, facing the way the castle looks.
				w.gate[(int) Math.round((dir / (2 * Math.PI) * 64 % 64 + 64)) % 64] = true;
			}
		}
		if (rd != null) {
			yardsAlong(rd, homes + s.houses);
		}
	}

	private void motteAndBailey(double dir, Layout.Surface sf) {
		// The bailey around its centre; the motte stands on its edge, towards the rising ground.
		double ma = dir + Math.PI / 2;
		int bx = s.x, bz = s.z;
		int[] wd = vary(BKind.MOTTE, 0.9, 1.15);
		int mxx = bx + (int) (Math.cos(ma) * 28), mzz = bz + (int) (Math.sin(ma) * 28);
		near(BKind.MOTTE, mxx, mzz, facing(bx - mxx, bz - mzz), 8, wd[0], wd[0], 1);
		boolean stone = r.nextInt(3) == 0;
		Layout.Wall w = ring(bx, bz, 46 + r.nextInt(8), stone ? Layout.WallType.STONE : Layout.WallType.PALISADE, stone ? 8 : 6, true, 0.08);
		yard(bx - (int) (Math.cos(ma) * 6), bz - (int) (Math.sin(ma) * 6), 34, BKind.HALL, BKind.CHURCH_SMALL, BKind.STABLE, BKind.SMITHY,
				BKind.GRANARY, BKind.BAKERY, BKind.BARRACKS);
		near(BKind.WELL, bx, bz, 0, 6);
		castleRoad(bx, bz, ma + Math.PI, sf, w);
	}

	private void stoneCastle(double dir, Layout.Surface sf, Layout.WallType type, int keepVariant) {
		int bx = s.x, bz = s.z;
		near(BKind.KEEP, bx, bz, facing(Math.cos(dir), Math.sin(dir)), 8, 15 + r.nextInt(5), 15 + r.nextInt(4), 1);
		Layout.Wall w = ring(bx, bz, 38 + r.nextInt(10), type, height(type, false), true, 0.1);
		yard(bx, bz, 34, BKind.HALL, s.christian() ? BKind.CHURCH_SMALL : BKind.WELL, BKind.STABLE, BKind.SMITHY, BKind.GRANARY, BKind.BARRACKS,
				s.culture == Culture.CAUCASUS ? BKind.TOWER : BKind.BAKERY);
		near(BKind.WELL, bx + 14, bz + 8, 0, 6);
		castleRoad(bx, bz, dir, sf, w);
	}

	/** Rus, Balt and West Slav hill fort: an earth rampart with a log wall, the lord's terem, a church, stores. */
	private void gorodishche(double dir, Layout.Surface sf) {
		int bx = s.x, bz = s.z;
		Layout.Wall w = ring(bx, bz, 44 + r.nextInt(12), Layout.WallType.RAMPART, 7, true, 0.14);
		near(BKind.HALL, bx, bz, facing(Math.cos(dir), Math.sin(dir)), 8);
		yard(bx, bz, 36, BKind.KEEP, s.culture == Culture.BALT_FINN ? BKind.GRANARY : BKind.CHURCH_SMALL, BKind.GRANARY, BKind.SMITHY,
				BKind.HOUSE, BKind.HOUSE, BKind.STABLE, BKind.BARRACKS);
		near(BKind.WELL, bx - 12, bz + 10, 0, 6);
		castleRoad(bx, bz, dir, sf, w);
	}

	/** A Danish ring fortress of Harald Bluetooth's kind: a perfect circle, four gates, sixteen halls in four squares. */
	private void trelleborg(Layout.Surface sf) {
		int bx = s.x, bz = s.z;
		int R = 72;
		Layout.Wall w = ring(bx, bz, R, Layout.WallType.RAMPART, 7, true, 0);
		for (int i = 0; i < 64; i++) {
			w.radius[i] = R;
		}
		// The two crossing streets paved with planks, through the four gates.
		road(new int[]{bx - R - 30, bz, bx - R / 2, bz, bx, bz, bx + R / 2, bz, bx + R + 30, bz}, 4, Layout.Surface.PLANKS);
		road(new int[]{bx, bz - R - 30, bx, bz - R / 2, bx, bz, bx, bz + R / 2, bx, bz + R + 30}, 4, Layout.Surface.PLANKS);
		gates(w);
		for (int qx = -1; qx <= 1; qx += 2) {
			for (int qz = -1; qz <= 1; qz += 2) {
				int cx = bx + qx * 25, cz = bz + qz * 25;
				// Four halls around a square yard: two along x, two along z.
				place(BKind.LONGHOUSE, cx, cz - 16, 1, 11, 21, 0);
				place(BKind.LONGHOUSE, cx, cz + 16, 3, 11, 21, 0);
				place(BKind.LONGHOUSE, cx - 16, cz, 0, 11, 21, 0);
				place(BKind.LONGHOUSE, cx + 16, cz, 2, 11, 21, 0);
				place(BKind.SHED, cx, cz, r.nextInt(4), 6, 6, 0);
			}
		}
		Layout.Road out = road(march(bx, bz + R + 30, Math.PI / 2, 140, 0.45), 3, sf);
		if (out != null) {
			yardsAlong(out, homes + s.houses);
		}
	}

	/** An Irish cashel: a round drystone wall with round houses and the round tower of the monks nearby. */
	private void cashel(Layout.Surface sf) {
		int bx = s.x, bz = s.z;
		Layout.Wall w = ring(bx, bz, 26 + r.nextInt(6), Layout.WallType.STONE, 4, true, 0.06);
		for (int i = 0; i < 4; i++) {
			double a = i * Math.PI / 2 + r.nextDouble() * 0.5;
			int x = bx + (int) (Math.cos(a) * 12), z = bz + (int) (Math.sin(a) * 12);
			near(BKind.HOMESTEAD, x, z, facing(bx - x, bz - z), 4, 13, 15, 0);
		}
		near(BKind.TOWER, bx + 40, bz - 10, 0, 8);
		near(BKind.CHURCH_SMALL, bx + 46, bz + 8, 2, 8);
		castleRoad(bx, bz, r.nextDouble() * Math.PI * 2, sf, w);
	}

	/** A ribat or qasr: a square enclosure with towers, the garrison's cells round a court, a mosque, a watchtower. */
	private void ribat(Layout.Surface sf) {
		int bx = s.x, bz = s.z;
		int half = 26 + r.nextInt(6);
		Layout.Wall w = square(bx, bz, half, s.culture == Culture.ANDALUSI ? Layout.WallType.STONE : Layout.WallType.MUDBRICK, 9, true);
		place(BKind.TOWER, bx, bz - half + 10, 0, 7, 7, 1);
		place(BKind.CELLS, bx - half + 9, bz, 3, 9, 19, 1);
		place(BKind.CELLS, bx + half - 9, bz, 1, 9, 19, 1);
		near(BKind.MOSQUE, bx, bz + 4, 2, 6, 15, 17, 1);
		near(BKind.WELL, bx - 8, bz - 8, 0, 4);
		near(BKind.STABLE, bx + 9, bz - 10, 2, 4);
		Layout.Road rd = road(march(bx, bz, Math.PI / 2, 160, 0.45), 4, sf);
		gates(w);
		if (rd != null) {
			yardsAlong(rd, homes + s.houses);
		}
	}

	private void monastery() {
		Layout.WallType type = s.culture == Culture.RUS ? Layout.WallType.PALISADE : Layout.WallType.STONE;
		Layout.Lot church = near(BKind.CHURCH, s.x, s.z, 0, 6);
		int cx = church == null ? s.x : (church.x0 + church.x1) / 2, cz = church == null ? s.z : (church.z0 + church.z1) / 2;
		Layout.Wall w = ring(cx, cz, 40, type, 6, true, 0.1);
		Layout.Road rd = road(march(cx, cz + 14, Math.PI / 2, 160, 0.4), 3, surface());
		gates(w);
		near(BKind.CELLS, cx - 22, cz, 3, 4);
		near(BKind.CELLS, cx + 22, cz, 1, 4);
		near(BKind.GRANARY, cx - 12, cz - 24, 0, 4);
		near(BKind.WELL, cx + 10, cz + 18, 0, 4);
		near(BKind.CEMETERY, cx + 14, cz - 24, 0, 5);
		near(BKind.GARDEN, cx - 22, cz + 22, 0, 5);
		near(r.nextBoolean() ? BKind.BREWERY : BKind.BAKERY, cx + 24, cz + 20, 1, 5);
		if (s.culture == Culture.CELTIC) {
			near(BKind.TOWER, cx + 18, cz + 6, 0, 5);
		}
		near(BKind.APIARY, cx + 60, cz - 20, 0, 6);
		if (rd != null) {
			yardsAlong(rd, Math.min(s.houses, 6));
		}
	}

	// ---- towns ----

	private Culture foreigner() {
		return switch (s.culture) {
			case RUS -> r.nextBoolean() ? Culture.NORSE : Culture.LATIN;
			case BYZANTINE -> r.nextBoolean() ? Culture.LATIN : Culture.ARAB;
			case ANDALUSI -> Culture.LATIN;
			case ARAB -> r.nextBoolean() ? Culture.BYZANTINE : Culture.PERSIAN;
			case PERSIAN -> r.nextBoolean() ? Culture.ARAB : Culture.STEPPE;
			case LATIN -> r.nextBoolean() ? Culture.BYZANTINE : Culture.NORSE;
			case NORSE -> Culture.RUS;
			case WEST_SLAV, MAGYAR -> Culture.LATIN;
			case CAUCASUS -> Culture.PERSIAN;
			default -> null;
		};
	}

	private boolean landmarkCathedral() {
		if (s.place == null) {
			return false;
		}
		return switch (s.place.id()) {
			case "kyiv", "constantinople", "cordoba" -> true;
			default -> false;
		};
	}

	private void town() {
		int R = s.radius;
		Layout.Surface sf = surface();
		// The citadel on the highest dry, defensible spot near the centre - often by the water's edge.
		int bx = s.x, bz = s.z;
		double best = -1e9;
		for (int i = -3; i <= 3; i++) {
			for (int j = -3; j <= 3; j++) {
				int x = s.x + i * R / 9, z = s.z + j * R / 9;
				Terrain.Col c = Terrain.column(x, z);
				if (!dry(c) || Wonders.blocked(x - 60, z - 60, x + 60, z + 60, 0)) {
					continue;
				}
				double slope = Math.abs(h(x + 10, z) - h(x - 10, z)) + Math.abs(h(x, z + 10) - h(x, z - 10));
				double score = c.meters - slope * 1.5 - Math.hypot(i, j) * 2.5 + (nearWater(x, z, 70) ? 6 : 0);
				if (score > best) {
					best = score;
					bx = x;
					bz = z;
				}
			}
		}
		int rc = switch (s.kind) {
			case TOWN -> 50;
			case CITY -> 80;
			case GREAT_CITY -> 115;
			default -> 165;
		};
		Layout.WallType ct = wallType(true);
		citadel = ring(bx, bz, rc, ct, height(ct, true), true, 0.12);
		boolean walled = s.kind.ordinal() >= Site.Kind.CITY.ordinal() || r.nextDouble() < 0.5;
		if (walled) {
			Layout.WallType ot = s.kind == Site.Kind.TOWN ? wallType(false) : wallType(true);
			int ocx = (s.x * 3 + bx) / 4, ocz = (s.z * 3 + bz) / 4;
			outer = ring(ocx, ocz, Math.max(rc + 70, R * 0.78), ot, height(ot, s.kind != Site.Kind.TOWN), false, 0.16);
			if (s.kind.ordinal() >= Site.Kind.GREAT_CITY.ordinal()) {
				// The old town inside the new: a middle ring around the citadel's side of the city.
				middle = ring((bx + ocx) / 2, (bz + ocz) / 2, Math.max(rc + 60, R * 0.42), ot, height(ot, true), false, 0.14);
			}
		}
		// The market between the citadel and the middle of the town.
		double ma = Math.atan2(s.z - bz, s.x - bx);
		if (Math.hypot(s.z - bz, s.x - bx) < 1) {
			ma = r.nextDouble() * Math.PI * 2;
		}
		mx = bx + (int) (Math.cos(ma) * (rc + 40 + R * 0.08));
		mz = bz + (int) (Math.sin(ma) * (rc + 40 + R * 0.08));
		for (int t = 0; t < 8 && !dry(Terrain.column(mx, mz)); t++) {
			ma += 0.7;
			mx = bx + (int) (Math.cos(ma) * (rc + 40));
			mz = bz + (int) (Math.sin(ma) * (rc + 40));
		}
		// Quarters.
		districts.add(new District(mx, mz, Quarter.MERCHANT, null));
		districts.add(new District(bx + (int) (Math.cos(ma + Math.PI) * (rc + 30)), bz + (int) (Math.sin(ma + Math.PI) * (rc + 30)), Quarter.NOBLE, null));
		int nq = 3 + s.houses / 220;
		for (int i = 0; i < nq; i++) {
			double a = r.nextDouble() * Math.PI * 2, d = R * (0.3 + r.nextDouble() * 0.5);
			int x = s.x + (int) (Math.cos(a) * d), z = s.z + (int) (Math.sin(a) * d);
			Quarter q = i % 3 == 0 || nearWater(x, z, 60) && r.nextBoolean() ? Quarter.CRAFT : Quarter.COMMON;
			districts.add(new District(x, z, q, null));
		}
		if (s.christian() && s.kind.ordinal() >= Site.Kind.CITY.ordinal()) {
			double a = r.nextDouble() * Math.PI * 2;
			districts.add(new District(s.x + (int) (Math.cos(a) * R * 0.5), s.z + (int) (Math.sin(a) * R * 0.5), Quarter.CLERGY, null));
		}
		Culture foreign = s.kind.ordinal() >= Site.Kind.CITY.ordinal() ? foreigner() : null;
		if (foreign != null) {
			double a = ma + (r.nextBoolean() ? 1 : -1) * (0.6 + r.nextDouble() * 0.6);
			districts.add(new District(mx + (int) (Math.cos(a) * R * 0.3), mz + (int) (Math.sin(a) * R * 0.3), Quarter.FOREIGN, foreign));
		}

		// Inside the citadel.
		citadelInside(bx, bz, rc);

		// The market square: two or three overlapping rectangles, stalls, a well, a church or mosque on its side.
		market(sf);

		// Main streets from the gates: they wander towards the market and run into each other.
		int gatesN = switch (s.kind) {
			case TOWN -> 3 + r.nextInt(2);
			case CITY -> 4 + r.nextInt(2);
			case GREAT_CITY -> 6;
			default -> 8;
		};
		int streetW = s.kind == Site.Kind.TOWN ? 4 : 5;
		double reach = outer != null ? outer.maxRadius() : R * 0.95;
		int ocx = outer != null ? outer.cx : s.x, ocz = outer != null ? outer.cz : s.z;
		List<double[]> gateAt = new ArrayList<>();
		double g0 = r.nextDouble() * Math.PI * 2;
		for (int k = 0; k < gatesN; k++) {
			double a = g0 + k * 2 * Math.PI / gatesN + (r.nextDouble() - 0.5) * 0.5;
			double rr = (outer != null ? outer.radiusAt(a) : R * 0.9) + 25;
			double gx = ocx + Math.cos(a) * rr, gz = ocz + Math.sin(a) * rr;
			if (!dry(Terrain.column((int) gx, (int) gz))) {
				continue;
			}
			int[] pts = walk(gx, gz, a + Math.PI, (int) (rr * 1.6), 0.6, mx, mz, 0.18, k > 0, null);
			Layout.Road rd = road(pts, streetW, sf);
			if (rd != null) {
				townStreets.add(rd);
				gateAt.add(new double[]{gx, gz, a});
			}
		}
		// From the market up to the citadel gate.
		Layout.Road up = road(walk(mx, mz, Math.atan2(bz - mz, bx - mx), (int) (Math.hypot(bx - mx, bz - mz) + 20), 0.7, bx, bz, 0.35, false, null), streetW, sf);
		if (up != null) {
			townStreets.add(up);
		}
		// The lanes first: they branch off the streets and run into one another, until there is street front enough
		// for all the houses; then the houses go along them, the most crowded first.
		Bound inTown = (x, z) -> {
			if (citadel != null && inside(citadel, x, z, -8)) {
				return false;
			}
			if (outer != null) {
				return inside(outer, x, z, 9);
			}
			return Math.hypot(x - s.x, z - s.z) < R * 1.05;
		};
		double need = s.houses * 7.5, total = 0;
		for (Layout.Road rd : townStreets) {
			total += length(rd);
		}
		int budget = 200 + s.houses * 2;
		for (int t = 0; t < budget && total < need && !townStreets.isEmpty(); t++) {
			Layout.Road from = townStreets.get(r.nextInt(townStreets.size()));
			double len = length(from);
			if (len < 20) {
				continue;
			}
			double[] q = along(from, 6 + r.nextDouble() * (len - 12));
			if (!inTown.ok(q[0], q[1])) {
				continue;
			}
			double ba = Math.atan2(q[3], q[2]) + (r.nextBoolean() ? 1 : -1) * (Math.PI / 2 + (r.nextDouble() - 0.5) * 0.6);
			Zone z0 = zoneAt(q[0], q[1]);
			int lw = z0 == Zone.INNER ? 3 : r.nextInt(3) == 0 ? 4 : 3;
			clearance = z0 == Zone.INNER ? 15 : 21;
			int[] pts = walk(q[0] + Math.cos(ba) * (from.width / 2.0 + 2), q[1] + Math.sin(ba) * (from.width / 2.0 + 2), ba,
					30 + r.nextInt(z0 == Zone.INNER ? 50 : 90), 0.7, 0, 0, 0, true, inTown);
			clearance = 0;
			if (pts.length < 8) {
				continue;
			}
			Layout.Road lane = road(pts, lw, sf);
			if (lane != null) {
				townStreets.add(lane);
				total += length(lane);
			}
		}
		// Churches or mosques, inns, warehouses, barracks along the streets; then the houses.
		specials(sf);
		final int fmx = mx, fmz = mz;
		List<Layout.Road> order = new ArrayList<>(townStreets);
		order.sort((p, q) -> Double.compare(p.dist(fmx, fmz), q.dist(fmx, fmz)));
		for (Layout.Road rd : order) {
			lotsAlong(rd, s.houses, foreign);
			if (homes >= s.houses) {
				break;
			}
		}
		// Still room for more? Lanes into the empty land behind the houses, filled at once.
		for (int t = 0; t < s.houses && homes < s.houses && !townStreets.isEmpty(); t++) {
			Layout.Road from = townStreets.get(r.nextInt(townStreets.size()));
			double len = length(from);
			if (len < 20) {
				continue;
			}
			double[] q = along(from, 6 + r.nextDouble() * (len - 12));
			double ba = Math.atan2(q[3], q[2]) + (r.nextBoolean() ? 1 : -1) * (Math.PI / 2 + (r.nextDouble() - 0.5) * 0.6);
			double sx = q[0] + Math.cos(ba) * (from.width / 2.0 + 2), sz = q[1] + Math.sin(ba) * (from.width / 2.0 + 2);
			if (used.get((int) Math.round(sx), (int) Math.round(sz)) || !inTown.ok(sx, sz)) {
				continue;
			}
			clearance = 15;
			int[] pts = walk(sx, sz, ba, 30 + r.nextInt(70), 0.7, 0, 0, 0, true, inTown);
			clearance = 0;
			if (pts.length < 8) {
				continue;
			}
			Layout.Road lane = road(pts, 3, sf);
			if (lane != null) {
				townStreets.add(lane);
				lotsAlong(lane, s.houses, foreign);
			}
		}
		// Suburbs: the roads out of the gates, lined with yards, workshops, kilns, cemeteries.
		for (double[] g : gateAt) {
			double a = g[2];
			int out = s.fieldRadius() - s.radius + 120;
			Layout.Road rd = road(walk(g[0], g[1], a, out, 0.45, 0, 0, 0, false, null), 3, Layout.Surface.DIRT);
			if (rd != null) {
				int extra = Math.max(4, s.houses / 12);
				lotsAlong(rd, homes + extra, null);
				if (r.nextInt(3) == 0 || s.muslim()) {
					frontage(BKind.CEMETERY, 13 + r.nextInt(8), 13 + r.nextInt(8), rd, 30 + r.nextInt(40), 2);
				}
				frontage(BKind.SHRINE, 3, 3, rd, Math.min(length(rd) - 5, 80 + r.nextInt(60)), 0);
			}
		}
		// Wells at street corners.
		for (int i = 0; i < Math.max(2, s.houses / 50) && !townStreets.isEmpty(); i++) {
			Layout.Road rd = townStreets.get(r.nextInt(townStreets.size()));
			frontage(BKind.WELL, 5, 5, rd, r.nextDouble() * length(rd), 0);
		}
		// Mills, piers, boatyards on the water by the town.
		int wcount = Math.min(14, 2 + s.houses / 70);
		waterside(wcount, R * 0.3, R * 1.35, true);
		if (s.culture == Culture.PERSIAN) {
			for (int i = 0; i < 3; i++) {
				double a = r.nextDouble() * Math.PI * 2;
				int x = s.x + (int) (Math.cos(a) * R * 1.15), z = s.z + (int) (Math.sin(a) * R * 1.15);
				near(BKind.WINDMILL, x, z, facing(s.x - x, s.z - z), 8);
			}
		}
		gates(citadel);
		if (middle != null) {
			gates(middle);
		}
		if (outer != null) {
			gates(outer);
		}
	}

	private void citadelInside(int bx, int bz, int rc) {
		Culture c = s.culture;
		boolean cathedral = !landmarkCathedral();
		BKind holy = s.muslim() ? BKind.MOSQUE : s.christian() ? (s.kind == Site.Kind.TOWN ? BKind.CHURCH : BKind.CATHEDRAL) : BKind.HALL;
		if (cathedral || holy != BKind.CATHEDRAL) {
			near(holy, bx + rc / 4, bz - rc / 5, 0, 12);
		}
		near(BKind.HALL, bx - rc / 4, bz + rc / 6, 0, 12);
		switch (c) {
			case LATIN, CELTIC, WEST_SLAV, MAGYAR -> near(s.kind == Site.Kind.TOWN && c == Culture.LATIN && r.nextBoolean() ? BKind.MOTTE : BKind.KEEP,
					bx + rc / 3, bz + rc / 2 - 14, 2, 10);
			case RUS -> near(BKind.KEEP, bx + rc / 2 - 10, bz + rc / 3, 1, 10, 13, 13, 1);
			case ANDALUSI, ARAB, PERSIAN -> {
				near(BKind.GARDEN, bx - rc / 3, bz - rc / 3, 0, 10, 17, 17, 1);
				near(BKind.GARDEN, bx + rc / 3, bz + rc / 3, 2, 10);
				near(BKind.BATHHOUSE, bx + rc / 2 - 8, bz - rc / 4, 1, 10, 13, 13, 1);
				near(BKind.TOWER, bx - rc / 2 + 6, bz + rc / 3, 0, 10);
			}
			case BYZANTINE -> {
				near(BKind.GARDEN, bx - rc / 3, bz - rc / 3, 0, 10);
				near(BKind.BATHHOUSE, bx + rc / 3, bz + rc / 3, 2, 10, 13, 13, 1);
			}
			case CAUCASUS -> {
				near(BKind.TOWER, bx - rc / 3, bz - rc / 3, 0, 10);
				near(BKind.TOWER, bx + rc / 3, bz + rc / 3, 0, 10);
			}
			case NORSE -> near(BKind.LONGHOUSE, bx + rc / 3, bz, 0, 10);
			default -> {
			}
		}
		near(BKind.BARRACKS, bx - rc / 2 + 10, bz - rc / 4, 1, 10);
		near(BKind.STABLE, bx + rc / 4, bz + rc / 2 - 10, 2, 10);
		near(BKind.GRANARY, bx - rc / 3, bz + rc / 2 - 8, 2, 10);
		near(BKind.WELL, bx, bz, 0, 6);
		// Citadel streets: a crooked cross, lined with the houses of the nobles.
		for (int k = 0; k < 4; k++) {
			double a = k * Math.PI / 2 + 0.3 + (r.nextDouble() - 0.5) * 0.4;
			Layout.Road rd = road(walk(bx, bz, a, rc - 6, 0.6, 0, 0, 0, false, (x, z) -> inside(citadel, x, z, 6)), 4, surface());
			if (rd != null) {
				lotsAlong(rd, homes + 4 + rc / 20, null);
			}
		}
	}

	private void market(Layout.Surface sf) {
		int half = switch (s.kind) {
			case TOWN -> 11;
			case CITY -> 16;
			case GREAT_CITY -> 21;
			default -> 26;
		};
		Terrain.Col mc = Terrain.column(mx, mz);
		if (!dry(mc)) {
			return;
		}
		int parts = 1 + r.nextInt(3);
		for (int p = 0; p < parts; p++) {
			int hx = half - r.nextInt(half / 3 + 1), hz = half - r.nextInt(half / 3 + 1);
			int ox = p == 0 ? 0 : r.nextInt(half + 1) - half / 2, oz = p == 0 ? 0 : r.nextInt(half + 1) - half / 2;
			int x0 = mx + ox - hx, z0 = mz + oz - hz, x1 = mx + ox + hx, z1 = mz + oz + hz;
			if (Wonders.blocked(x0, z0, x1, z1, 4)) {
				continue;
			}
			L.squares.add(new Layout.Square(x0, z0, x1, z1, true));
			used.mark(x0, z0, x1, z1);
			streets.mark(x0, z0, x1, z1);
			for (int i = -hx + 4; i <= hx - 4; i += 6 + r.nextInt(3)) {
				for (int j = -hz + 4; j <= hz - 4; j += 8 + r.nextInt(3)) {
					if (r.nextDouble() < 0.55 && Math.abs(ox + i) + Math.abs(oz + j) > 5) {
						Layout.Lot st = new Layout.Lot(mx + ox + i - 2, mz + oz + j - 2, mx + ox + i + 2, mz + oz + j + 2, r.nextInt(4), mc.top(), BKind.STALL,
								r.nextLong());
						L.lots.add(st);
					}
				}
			}
		}
		L.lots.add(new Layout.Lot(mx - 2, mz - 2, mx + 2, mz + 2, 0, mc.top(), BKind.WELL, r.nextLong()));
	}

	private void specials(Layout.Surface sf) {
		if (townStreets.isEmpty()) {
			return;
		}
		List<BKind> list = new ArrayList<>();
		int n = s.houses;
		if (s.christian()) {
			int churches = Math.max(1, n / (s.culture == Culture.RUS ? 55 : 80));
			for (int i = 0; i < churches; i++) {
				list.add(i % 4 == 0 && n > 200 ? BKind.CHURCH : BKind.CHURCH_SMALL);
			}
			if (n > 150) {
				list.add(BKind.CELLS);
			}
		}
		if (s.muslim()) {
			if (!landmarkCathedral()) {
				list.add(BKind.MOSQUE);
			}
			for (int i = 0; i < n / 90; i++) {
				list.add(BKind.MOSQUE);
			}
			for (int i = 0; i < Math.max(1, n / 120); i++) {
				list.add(BKind.BATHHOUSE);
			}
		}
		if (s.muslim() || s.culture == Culture.PERSIAN || s.culture == Culture.BYZANTINE || s.culture == Culture.CAUCASUS
				|| s.culture == Culture.RUS && n > 300) {
			for (int i = 0; i < Math.max(1, n / 400); i++) {
				list.add(BKind.CARAVANSERAI);
			}
		}
		for (int i = 0; i < Math.max(1, n / 70); i++) {
			list.add(BKind.TAVERN);
		}
		for (int i = 0; i < Math.max(1, n / 60); i++) {
			list.add(BKind.WAREHOUSE);
		}
		for (int i = 0; i < Math.max(1, n / 300); i++) {
			list.add(BKind.BARRACKS);
		}
		for (int i = 0; i < Math.max(1, n / 90); i++) {
			list.add(BKind.SMITHY);
		}
		java.util.Collections.shuffle(list, r);
		for (BKind k : list) {
			Layout.Road rd = townStreets.get(r.nextInt(townStreets.size()));
			int[] wd = k == BKind.MOSQUE ? vary(k, 0.75, 1.0) : vary(k, 0.9, 1.1);
			Layout.Lot l = frontage(k, wd[0], wd[1], rd, r.nextDouble() * length(rd), 1);
			if (l != null) {
				District d = districtAt((l.x0 + l.x1) / 2.0, (l.z0 + l.z1) / 2.0);
				if (d != null && d.q == Quarter.FOREIGN) {
					// Foreign merchants pray in their own way.
					l.culture = d.culture;
				}
			}
		}
		// The foreigners' own church or mosque, in their quarter.
		for (District d : districts) {
			if (d.q != Quarter.FOREIGN || d.culture == null) {
				continue;
			}
			BKind k = d.culture == Culture.ARAB || d.culture == Culture.PERSIAN ? BKind.MOSQUE : BKind.CHURCH_SMALL;
			int[] wd = k == BKind.MOSQUE ? vary(k, 0.65, 0.75) : vary(k, 0.9, 1.0);
			Layout.Lot l = near(k, d.x, d.z, r.nextInt(4), 12, wd[0], wd[1], 1);
			if (l != null) {
				l.culture = d.culture;
			}
		}
	}
}
