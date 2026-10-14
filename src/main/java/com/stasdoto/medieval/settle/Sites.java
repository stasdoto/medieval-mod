package com.stasdoto.medieval.settle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.stasdoto.medieval.earth.Climate;
import com.stasdoto.medieval.earth.Cover;
import com.stasdoto.medieval.earth.Dem;
import com.stasdoto.medieval.earth.Projection;
import com.stasdoto.medieval.earth.Terrain;
import com.stasdoto.medieval.earth.TileCache;
import com.stasdoto.medieval.history.Culture;
import com.stasdoto.medieval.history.Places;

/**
 * Where the settlements are: the real towns of 1050 at their places, and villages of the countryside spread over the land
 * the way they were (thick in fertile plains by rivers, thin in the taiga and mountains, nomad camps in the steppe, none
 * in deserts away from water). Worked out per region square and remembered.
 */
public final class Sites {
	private Sites() {
	}

	public static final int CELL = 4096;
	private static final TileCache<List<Site>> CELLS = new TileCache<>(512, Sites::compute);
	/** Off in tests that want bare land. */
	public static volatile boolean enabled = true;

	/** The settlements whose fields or houses may reach the point. */
	public static List<Site> near(int x, int z) {
		List<Site> out = new ArrayList<>(2);
		if (!enabled) {
			return out;
		}
		int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				for (Site s : cell(cx + i, cz + j)) {
					long dx = x - s.x, dz = z - s.z;
					long r = s.fieldRadius() + 64;
					if (dx * dx + dz * dz <= r * r) {
						out.add(s);
					}
				}
			}
		}
		return out;
	}

	/** All the settlements of one region square. */
	public static List<Site> cell(int cx, int cz) {
		return CELLS.get(key(cx, cz));
	}

	private static long key(int cx, int cz) {
		return ((long) (cx + (1 << 20)) << 21) | (cz + (1 << 20));
	}

	private static List<Site> compute(long key) {
		int cx = (int) (key >>> 21) - (1 << 20);
		int cz = (int) (key & ((1 << 21) - 1)) - (1 << 20);
		List<Site> list = new ArrayList<>();
		int x0 = cx * CELL, z0 = cz * CELL;
		// The towns of history.
		for (Places.Place p : Places.ALL) {
			int px = (int) Math.round(Projection.x(p.lon())), pz = (int) Math.round(Projection.z(p.lat()));
			if (Math.floorDiv(px, CELL) == cx && Math.floorDiv(pz, CELL) == cz) {
				Site.Kind k = switch (p.size()) {
					case 1 -> p.culture() == Culture.STEPPE ? Site.Kind.CAMP : Site.Kind.BIG_VILLAGE;
					case 2 -> Site.Kind.TOWN;
					case 3 -> Site.Kind.CITY;
					case 4 -> Site.Kind.GREAT_CITY;
					default -> Site.Kind.METROPOLIS;
				};
				int houses = Math.max(k.minHouses, Math.min(k.maxHouses * 2, p.people() / 6));
				int radius = (int) Math.max(k.radius * 0.6, Math.min(3200, 40 + 4.2 * Math.sqrt(p.people())));
				list.add(new Site(p.id().hashCode() * 31L + 7, px, pz, k, p.culture(), p.name(), p, radius, houses));
			}
		}
		// Villages of the countryside: a few candidates per square, each moved to the best spot nearby.
		Random r = new Random(key * 0x5DEECE66DL + 11);
		for (int gx = 0; gx < 2; gx++) {
			for (int gz = 0; gz < 2; gz++) {
				int x = x0 + gx * 2048 + 300 + r.nextInt(1448);
				int z = z0 + gz * 2048 + 300 + r.nextInt(1448);
				double roll = r.nextDouble();
				long seed = r.nextLong();
				Site s = village(x, z, roll, seed, list);
				if (s != null) {
					list.add(s);
				}
			}
		}
		return list;
	}

	private static double slopeAt(int x, int z) {
		double lat = Projection.lat(z + 0.5), lon = Projection.lon(x + 0.5);
		double a = Dem.meters(lat, lon);
		double b = Dem.meters(Projection.lat(z + 8.5), lon);
		double c = Dem.meters(lat, Projection.lon(x + 8.5));
		return Math.max(Math.abs(b - a), Math.abs(c - a)) / 8.0;
	}

	private static Site village(int x, int z, double roll, long seed, List<Site> others) {
		for (Site o : others) {
			if (Math.hypot(o.x - x, o.z - z) < o.radius + 700) {
				return null;
			}
		}
		// Also keep clear of towns in the neighbouring squares.
		for (Places.Place p : Places.ALL) {
			double px = Projection.x(p.lon()), pz = Projection.z(p.lat());
			if (Math.abs(px - x) < 5000 && Math.abs(pz - z) < 5000 && Math.hypot(px - x, pz - z) < 40 + 4.2 * Math.sqrt(p.people()) + 900) {
				return null;
			}
		}
		Terrain.Col c = Terrain.column(x, z);
		double chance = switch (c.zone) {
			case Climate.TEMPERATE -> 0.85;
			case Climate.MEDITERRANEAN, Climate.SUBTROPICAL -> 0.75;
			case Climate.STEPPE -> 0.3;
			case Climate.BOREAL -> 0.35;
			case Climate.TROPICAL -> 0.4;
			case Climate.DESERT, Climate.COLD_DESERT -> 0.04;
			case Climate.TUNDRA -> 0.05;
			default -> 0;
		};
		if (c.meters > 1400) {
			chance *= 0.15;
		} else if (c.meters > 800) {
			chance *= 0.5;
		}
		if (roll > chance) {
			return null;
		}
		// The best spot within ~500 blocks: dry, flat, by water, not in the wild forest, not too high.
		Random r = new Random(seed);
		double best = -1e9;
		int bx = x, bz = z;
		Terrain.Col bc = null;
		for (int i = -3; i <= 3; i++) {
			for (int j = -3; j <= 3; j++) {
				int sx = x + i * 150 + r.nextInt(40) - 20, sz = z + j * 150 + r.nextInt(40) - 20;
				Terrain.Col s = Terrain.column(sx, sz);
				if (s.waterTop != Terrain.NO_WATER || s.cover == Cover.WETLAND || s.cover == Cover.ICE || s.cover == Cover.ROCK) {
					continue;
				}
				double slope = slopeAt(sx, sz);
				double score = -slope * 40 - Math.hypot(i, j) * 0.6;
				if (s.shore > -160 && s.shore < -12) {
					score += 6; // by a river or lake
				}
				if (s.cover == Cover.FARMLAND || s.cover == Cover.MODERN || s.cover == Cover.GRASS || s.cover == Cover.MEADOW) {
					score += 2;
				}
				score -= Math.max(0, s.meters - 400) / 150.0;
				if (score > best) {
					best = score;
					bx = sx;
					bz = sz;
					bc = s;
				}
			}
		}
		if (bc == null || best < -25) {
			return null;
		}
		Culture cul = CultureMap.at(bc.lat, bc.lon, bc.zone);
		double k = r.nextDouble();
		Site.Kind kind;
		if (cul == Culture.STEPPE) {
			kind = Site.Kind.CAMP;
		} else if (k < 0.07 && (cul == Culture.LATIN || cul == Culture.WEST_SLAV || cul == Culture.MAGYAR || cul == Culture.CELTIC)) {
			kind = Site.Kind.CASTLE;
		} else if (k < 0.11 && (cul == Culture.RUS || cul == Culture.BYZANTINE || cul == Culture.LATIN || cul == Culture.CAUCASUS)) {
			kind = Site.Kind.MONASTERY;
		} else if (k < 0.45) {
			kind = Site.Kind.HAMLET;
		} else if (k < 0.9) {
			kind = Site.Kind.VILLAGE;
		} else {
			kind = Site.Kind.BIG_VILLAGE;
		}
		int houses = kind.minHouses + r.nextInt(kind.maxHouses - kind.minHouses + 1);
		int radius = (int) (kind.radius * (0.75 + 0.5 * Math.sqrt((houses - kind.minHouses + 1.0) / (kind.maxHouses - kind.minHouses + 1.0))));
		return new Site(seed, bx, bz, kind, cul, NameGen.name(cul, seed), null, radius, houses);
	}

	public static void clear() {
		CELLS.clear();
	}
}
