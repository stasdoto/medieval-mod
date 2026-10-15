package com.stasdoto.medieval.construct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import com.stasdoto.medieval.Medieval;
import com.stasdoto.medieval.earth.Projection;
import com.stasdoto.medieval.earth.Terrain;

/**
 * The wonders of the world as they stood around 1050, at their real places and at their real size (1 block = 1 m): the
 * pyramids and the Sphinx of Giza, Stonehenge, the Pharos of Alexandria, the Colosseum, the land walls and the Hippodrome
 * of Constantinople, and the great buildings drawn as blueprints in {@link Monuments}.
 *
 * Each landmark sits on its own levelled pad: the ground under it is filled with masonry down to the real ground or cut
 * away above, once for the whole landmark (one base height per landmark, the median of the ground under it), and the edge
 * of the pad is blended back into the land. Everything is written chunk by chunk from {@link #write}, which is called
 * concurrently from several world generation threads.
 */
public final class Landmarks {
	private Landmarks() {
	}

	public interface Sink {
		void set(int x, int y, int z, BlockState s);
	}

	public record Mark(String id, String name, double lat, double lon, int x, int z, int halfX, int halfZ) {
	}

	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	/** Width of the band around a levelled pad where the ground is blended back into the natural land. */
	private static final int BLEND = 6;
	/** Deepest foundation under a pad (below that the ground is left as it is). */
	private static final int MAX_FOUND = 64;

	private static final List<Lm> LIST = new ArrayList<>();
	private static final List<Mark> MARKS = new ArrayList<>();
	private static final List<Mark> VIEW = Collections.unmodifiableList(MARKS);

	static {
		init();
	}

	/** All landmarks (for the /medieval commands and the map). */
	public static List<Mark> all() {
		return VIEW;
	}

	/** Writes every part of every landmark that falls inside x0..x1 × z0..z1 (one chunk, 16×16). */
	public static void write(Sink out, int x0, int z0, int x1, int z1) {
		Sink safe = null;
		for (Lm l : LIST) {
			if (x1 < l.bx0 || x0 > l.bx1 || z1 < l.bz0 || z0 > l.bz1) {
				continue;
			}
			if (safe == null) {
				safe = (x, y, z, s) -> {
					if (x >= x0 && x <= x1 && z >= z0 && z <= z1 && y >= Projection.MIN_Y && y <= Projection.MAX_Y && s != null) {
						out.set(x, y, z, s);
					}
				};
			}
			try {
				l.write(safe, x0, z0, x1, z1);
			} catch (RuntimeException e) {
				if (l.errors.incrementAndGet() <= 3) {
					Medieval.LOGGER.error("[medieval] landmark {} failed at {} {}", l.mark.id(), x0, z0, e);
				}
			}
		}
	}

	/** True if the rectangle comes within {@code margin} blocks of a landmark's footprint (town planners keep plots off it). */
	public static boolean blocked(int x0, int z0, int x1, int z1, int margin) {
		for (Lm l : LIST) {
			if (x1 < l.bx0 - margin || x0 > l.bx1 + margin || z1 < l.bz0 - margin || z0 > l.bz1 + margin) {
				continue;
			}
			try {
				if (l.blocked(Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1), Math.max(z0, z1), margin)) {
					return true;
				}
			} catch (RuntimeException e) {
				return true;
			}
		}
		return false;
	}

	// ---- the registry ----

	private static void init() {
		// Giza: the three great pyramids, the queens' pyramids and the Sphinx. The small pyramids are placed by their real
		// distance (metres) from their king's pyramid, so they never run into it where the map squeezes east-west distances.
		Mark khufu = mark("giza_khufu", "Великая пирамида Хеопса (Гиза)", 29.97917, 31.13417, 119, 119);
		add(new Pyramid(khufu, true, 115, 146, 144, 0, 0, true));
		Mark khafre = mark("giza_khafre", "Пирамида Хефрена (Гиза)", 29.97611, 31.13083, 112, 112);
		add(new Pyramid(khafre, true, 107.5, 143.5, 143.5, 0, 2, false));
		Mark menk = mark("giza_menkaure", "Пирамида Микерина (Гиза)", 29.97250, 31.12806, 56, 56);
		add(new Pyramid(menk, true, 51.7, 65, 65, 0, 16, false));
		add(new Pyramid(sub(khufu, "giza_g1a", 167, 35, 29), false, 24.75, 30, 30, 0, 0, false));
		add(new Pyramid(sub(khufu, "giza_g1b", 172, 92, 29), false, 24.5, 30, 30, 0, 0, false));
		add(new Pyramid(sub(khufu, "giza_g1c", 178, 147, 27), false, 23, 29.5, 29.5, 0, 0, false));
		add(new Pyramid(sub(menk, "giza_g3a", 59, 100, 26), false, 22, 28, 28, 0, 0, false));
		add(new Pyramid(sub(menk, "giza_g3b", -6, 100, 20), false, 15.5, 21, 21, 2, 0, false));
		add(new Pyramid(sub(menk, "giza_g3c", -66, 100, 20), false, 15.5, 21, 21, 2, 0, false));
		addBp("giza_sphinx", "Большой сфинкс (Гиза)", 29.97528, 31.13750, 31, 80, 3, Landmarks::sphinx, null)
				.ground(Blocks.SAND, Blocks.SAND, Blocks.SANDSTONE, Blocks.SAND);

		// Stonehenge: a round pad for the henge and a strip for the start of the Avenue with the Heel Stone.
		double ax = Math.sin(Math.toRadians(50)), az = -Math.cos(Math.toRadians(50));
		addBp("stonehenge", "Стоунхендж", 51.17886, -1.82622, 200, 200, 0, Landmarks::stonehenge, (dx, dz) -> {
			double circle = Math.hypot(dx, dz) - 58;
			double along = dx * ax + dz * az, across = dx * -az + dz * ax;
			double strip = Math.max(Math.abs(across) - 16, Math.max(50 - along, along - 85));
			return Math.min(circle, strip);
		}).ground(Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.STONE, Blocks.DIRT);

		addBp("pharos", "Фаросский маяк (Александрия)", 31.21400, 29.88560, 72, 72, 0, Landmarks::pharos, null)
				.ground(Blocks.SAND, Blocks.SAND, Blocks.STONE, Blocks.SAND);

		add(new Colosseum(mark("colosseum", "Колизей (Рим)", 41.89021, 12.49223, 106, 106)));
		add(Walls.create());
		add(new Hippodrome(mark("hippodrome", "Ипподром (Константинополь)", 41.00570, 28.97520, 175, 245)));

		addBp("hagia_sophia", "Святая София (Константинополь)", 41.00839, 28.98008, 0, 0, 1, () -> Monuments.build("hagia_sophia"), null);
		addBp("kyiv_sofia", "Софийский собор (Киев)", 50.45290, 30.51430, 0, 0, 1, () -> Monuments.build("kyiv_sofia"), null);
		addBp("kyiv_golden_gate", "Золотые ворота (Киев)", 50.44890, 30.51310, 0, 0, 1, () -> Monuments.build("kyiv_golden_gate"), null);
		addBp("cordoba_mezquita", "Большая мечеть (Кордова)", 37.87890, -4.77940, 0, 0, 2, () -> Monuments.build("cordoba_mezquita"), null)
				.ground(Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.STONE, Blocks.DIRT);
		addBp("dome_of_rock", "Купол Скалы (Иерусалим)", 31.77800, 35.23540, 0, 0, 0, () -> Monuments.build("dome_of_rock"), null)
				.ground(Blocks.COARSE_DIRT, Blocks.COARSE_DIRT, Blocks.STONE, Blocks.DIRT);
		addBp("pantheon", "Пантеон (Рим)", 41.89861, 12.47685, 0, 0, 2, () -> Monuments.build("pantheon"), null);
	}

	private static Mark mark(String id, String name, double lat, double lon, int halfX, int halfZ) {
		return new Mark(id, name, lat, lon, (int) Math.round(Projection.x(lon)), (int) Math.round(Projection.z(lat)), halfX, halfZ);
	}

	/** A small pyramid at a real distance (metres east, metres south) from its king's pyramid. */
	private static Mark sub(Mark parent, String id, int east, int south, int half) {
		int x = parent.x() + east, z = parent.z() + south;
		return new Mark(id, "Пирамида царицы (Гиза)", Projection.lat(z + 0.5), Projection.lon(x + 0.5), x, z, half, half);
	}

	private static void add(Lm l) {
		LIST.add(l);
		if (l.listed) {
			MARKS.add(l.mark);
		}
	}

	/** A blueprint landmark of w × d (0 = the size Monuments gives for that id), turned by rot (0..3 = none/cw/180/ccw). */
	private static Lm addBp(String id, String name, double lat, double lon, int w, int d, int rot, Supplier<Bp> maker, Shape shape) {
		if (w <= 0 || d <= 0) {
			int[] sz = Monuments.size(id);
			w = sz[0];
			d = sz[1];
		}
		int ww = rot % 2 == 1 ? d : w, dd = rot % 2 == 1 ? w : d;
		Mark m = mark(id, name, lat, lon, (ww + 1) / 2, (dd + 1) / 2);
		BpLm l = new BpLm(m, w, d, rot, maker, shape);
		add(l);
		return l;
	}

	// ---- small helpers ----

	private static BlockState st(Block b) {
		return b.defaultBlockState();
	}

	/** A repeatable random number in [0, 1) for a block position. */
	static double h3(int x, int y, int z, int s) {
		long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ s * 0x27D4EB2F165667C5L;
		h ^= h >>> 31;
		h *= 0x7FB5D329728EA185L;
		h ^= h >>> 27;
		h *= 0x81DADEF4BC2DD44DL;
		h ^= h >>> 33;
		return (h >>> 11) * 0x1.0p-53;
	}

	private static double angDiff(double a, double b) {
		return Math.IEEEremainder(a - b, 2 * Math.PI);
	}

	private static double clamp(double v, double lo, double hi) {
		return v < lo ? lo : v > hi ? hi : v;
	}

	/** The nearest of the four horizontal directions to the vector (x, z) (north = -z). */
	private static Direction cardinal(double x, double z) {
		if (Math.abs(x) >= Math.abs(z)) {
			return x >= 0 ? Direction.EAST : Direction.WEST;
		}
		return z >= 0 ? Direction.SOUTH : Direction.NORTH;
	}

	private static BlockState stairs(Block b, Direction f) {
		return Bp.stairs(b, f, false);
	}

	// ---- landmark base ----

	private interface Shape {
		/** Signed distance (blocks) outside the levelled pad, for a point (dx, dz) from the landmark's centre. */
		double dist(double dx, double dz);
	}

	private abstract static class Lm {
		final Mark mark;
		final boolean listed;
		/** World box of everything this landmark may write (including the blended edge). */
		int bx0, bz0, bx1, bz1;
		/** Pad surface, blended ground surface, foundation fill, subsoil of the blend. */
		Block pave = Blocks.GRASS_BLOCK, soil = Blocks.GRASS_BLOCK, fill = Blocks.STONE, sub = Blocks.DIRT;
		/** How high above the ground the pad is cleared of trees and hills. */
		int clearH = 20;
		final AtomicInteger errors = new AtomicInteger();
		private volatile int base = Integer.MIN_VALUE;

		Lm(Mark mark, boolean listed) {
			this.mark = mark;
			this.listed = listed;
			int m = BLEND + 4;
			bx0 = mark.x() - mark.halfX() - m;
			bx1 = mark.x() + mark.halfX() + m;
			bz0 = mark.z() - mark.halfZ() - m;
			bz1 = mark.z() + mark.halfZ() + m;
		}

		Lm ground(Block pave, Block soil, Block fill, Block sub) {
			this.pave = pave;
			this.soil = soil;
			this.fill = fill;
			this.sub = sub;
			return this;
		}

		/** Signed distance (blocks) of a column outside the levelled pad; <= 0 inside. */
		abstract double padDist(int x, int z);

		/** Builds the landmark's blocks for the columns x0..x1 × z0..z1 (already inside its box). */
		abstract void build(Sink out, int x0, int z0, int x1, int z1, int base);

		boolean blocked(int x0, int z0, int x1, int z1, int margin) {
			int m = margin + BLEND;
			return x1 >= mark.x() - mark.halfX() - m && x0 <= mark.x() + mark.halfX() + m && z1 >= mark.z() - mark.halfZ() - m
					&& z0 <= mark.z() + mark.halfZ() + m;
		}

		/** The one floor height of the whole landmark (y of the top block of its pad), computed once. */
		int base() {
			int b = base;
			if (b != Integer.MIN_VALUE) {
				return b;
			}
			synchronized (this) {
				b = base;
				if (b == Integer.MIN_VALUE) {
					b = computeBase();
					base = b;
				}
			}
			return b;
		}

		private int computeBase() {
			int[] v = new int[25];
			int n = 0;
			int water = Integer.MIN_VALUE;
			for (int i = 0; i < 5; i++) {
				for (int j = 0; j < 5; j++) {
					int x = mark.x() + (int) Math.round((i - 2) / 2.0 * mark.halfX() * 0.8);
					int z = mark.z() + (int) Math.round((j - 2) / 2.0 * mark.halfZ() * 0.8);
					if (padDist(x, z) > 0) {
						continue;
					}
					Terrain.Col c = Terrain.column(x, z);
					if (c.waterTop != Terrain.NO_WATER) {
						water = Math.max(water, c.waterTop);
						continue;
					}
					v[n++] = c.top();
				}
			}
			if (n == 0) {
				Terrain.Col c = Terrain.column(mark.x(), mark.z());
				return c.waterTop != Terrain.NO_WATER ? c.waterTop + 2 : c.top();
			}
			Arrays.sort(v, 0, n);
			int b = v[n / 2];
			if (water != Integer.MIN_VALUE) {
				b = Math.max(b, water + 1);
			}
			return b;
		}

		void write(Sink out, int x0, int z0, int x1, int z1) {
			int ax0 = Math.max(x0, bx0), ax1 = Math.min(x1, bx1), az0 = Math.max(z0, bz0), az1 = Math.min(z1, bz1);
			if (ax0 > ax1 || az0 > az1) {
				return;
			}
			int b = base();
			for (int x = ax0; x <= ax1; x++) {
				for (int z = az0; z <= az1; z++) {
					double pd = padDist(x, z);
					if (pd > BLEND) {
						continue;
					}
					ground(out, x, z, Terrain.column(x, z), b, pd);
				}
			}
			build(out, ax0, az0, ax1, az1, b);
		}

		/** Levels one column to the pad (pd <= 0) or blends it back into the land around (0 < pd <= BLEND). */
		void ground(Sink out, int x, int z, Terrain.Col c, int b, double pd) {
			int g = c.top();
			boolean water = c.waterTop != Terrain.NO_WATER;
			if (pd <= 0) {
				BlockState f = st(fill);
				for (int y = Math.max(g + 1, b - MAX_FOUND); y < b; y++) {
					out.set(x, y, z, f);
				}
				out.set(x, b, z, st(pave));
				int from = b + 1;
				if (water) {
					from = Math.max(from, c.waterTop + 1);
				}
				int top = Math.max(g, b) + clearH;
				for (int y = from; y <= top; y++) {
					out.set(x, y, z, AIR);
				}
				return;
			}
			if (water) {
				return;
			}
			double t = pd / (BLEND + 1.0);
			int target = (int) Math.round(b + (g - b) * t);
			if (target > g) {
				BlockState s = st(sub);
				for (int y = Math.max(g, target - MAX_FOUND); y < target; y++) {
					out.set(x, y, z, s);
				}
			}
			if (target != g) {
				out.set(x, target, z, st(soil));
			}
			int top = Math.max(g, target) + clearH;
			for (int y = target + 1; y <= top; y++) {
				out.set(x, y, z, AIR);
			}
		}
	}

	/** A landmark computed column by column (solid shapes far too big for a blueprint). */
	private abstract static class Columns extends Lm {
		Columns(Mark m, boolean listed) {
			super(m, listed);
		}

		abstract void column(Sink out, int x, int z, int base);

		@Override
		void build(Sink out, int x0, int z0, int x1, int z1, int base) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					column(out, x, z, base);
				}
			}
		}
	}

	// ---- blueprint landmarks ----

	private static final class BpLm extends Lm {
		final int w, d, rot, ww, dd, ox, oz;
		final Supplier<Bp> maker;
		final Shape shape;
		private volatile Map<Long, Part> parts;

		/** The blocks of the blueprint that fall in one chunk: packed (y + 4096) << 8 | lx << 4 | lz, and their states. */
		private static final class Part {
			int[] pos = new int[64];
			BlockState[] st = new BlockState[64];
			int n;

			void add(int p, BlockState s) {
				if (n == pos.length) {
					pos = Arrays.copyOf(pos, n * 2);
					st = Arrays.copyOf(st, n * 2);
				}
				pos[n] = p;
				st[n] = s;
				n++;
			}

			void trim() {
				pos = Arrays.copyOf(pos, n);
				st = Arrays.copyOf(st, n);
			}
		}

		BpLm(Mark m, int w, int d, int rot, Supplier<Bp> maker, Shape shape) {
			super(m, true);
			this.w = w;
			this.d = d;
			this.rot = rot & 3;
			this.maker = maker;
			this.shape = shape;
			ww = this.rot % 2 == 1 ? d : w;
			dd = this.rot % 2 == 1 ? w : d;
			ox = m.x() - ww / 2;
			oz = m.z() - dd / 2;
			int mm = BLEND + 4;
			bx0 = ox - mm;
			bz0 = oz - mm;
			bx1 = ox + ww - 1 + mm;
			bz1 = oz + dd - 1 + mm;
		}

		@Override
		double padDist(int x, int z) {
			if (shape != null) {
				return shape.dist(x + 0.5 - mark.x(), z + 0.5 - mark.z());
			}
			int ex = Math.max(ox - x, x - (ox + ww - 1));
			int ez = Math.max(oz - z, z - (oz + dd - 1));
			return Math.max(ex, ez);
		}

		private static long ckey(int cx, int cz) {
			return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
		}

		Map<Long, Part> parts() {
			Map<Long, Part> p = parts;
			if (p != null) {
				return p;
			}
			synchronized (this) {
				p = parts;
				if (p == null) {
					try {
						p = bucket(maker.get());
					} catch (RuntimeException e) {
						Medieval.LOGGER.error("[medieval] landmark {} could not be drawn", mark.id(), e);
						p = new HashMap<>();
					}
					parts = p;
				}
			}
			return p;
		}

		private Map<Long, Part> bucket(Bp bp) {
			Rotation r = switch (rot) {
				case 1 -> Rotation.CLOCKWISE_90;
				case 2 -> Rotation.CLOCKWISE_180;
				case 3 -> Rotation.COUNTERCLOCKWISE_90;
				default -> Rotation.NONE;
			};
			int W = bp.w, D = bp.d;
			Map<Long, Part> m = new HashMap<>();
			for (Map.Entry<Long, BlockState> e : bp.blocks.entrySet()) {
				long k = e.getKey();
				int lx = Bp.kx(k), ly = Bp.ky(k), lz = Bp.kz(k);
				int wx, wz;
				switch (rot) {
					case 1 -> {
						wx = D - 1 - lz;
						wz = lx;
					}
					case 2 -> {
						wx = W - 1 - lx;
						wz = D - 1 - lz;
					}
					case 3 -> {
						wx = lz;
						wz = W - 1 - lx;
					}
					default -> {
						wx = lx;
						wz = lz;
					}
				}
				int x = ox + wx, z = oz + wz;
				BlockState s = e.getValue();
				if (s == null) {
					continue;
				}
				if (r != Rotation.NONE) {
					s = s.rotate(r);
				}
				long ck = ckey(x >> 4, z >> 4);
				Part p = m.get(ck);
				if (p == null) {
					p = new Part();
					m.put(ck, p);
				}
				p.add((ly + 4096) << 8 | (x & 15) << 4 | (z & 15), s);
			}
			for (Part p : m.values()) {
				p.trim();
			}
			return m;
		}

		@Override
		void build(Sink out, int x0, int z0, int x1, int z1, int base) {
			Map<Long, Part> ps = parts();
			if (ps.isEmpty()) {
				return;
			}
			for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
				for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
					Part p = ps.get(ckey(cx, cz));
					if (p == null) {
						continue;
					}
					for (int i = 0; i < p.n; i++) {
						int v = p.pos[i];
						int x = (cx << 4) | (v >> 4 & 15), z = (cz << 4) | (v & 15);
						if (x < x0 || x > x1 || z < z0 || z > z1) {
							continue;
						}
						out.set(x, base + (v >> 8) - 4096, z, p.st[i]);
					}
				}
			}
		}
	}

	// =====================================================================================================================
	// Giza
	// =====================================================================================================================

	/**
	 * A pyramid, solid, aligned with the compass. style 0: a true pyramid with its smooth white casing (still on in 1050),
	 * the outer surface smoothed with stairs; style 2: a small stepped pyramid. {@code granite}: the lowest courses are
	 * cased in red granite (Menkaure, Khafre). Khufu has its passages and chambers inside.
	 */
	private static final class Pyramid extends Columns {
		final double half, height, cap, slope, cx, cz;
		final int style, granite;
		final double[][] carves;

		Pyramid(Mark m, boolean listed, double half, double height, double cap, int style, int granite, boolean khufu) {
			super(m, listed);
			this.half = half;
			this.height = height;
			this.cap = cap;
			this.style = style;
			this.granite = granite;
			this.slope = height / half;
			this.cx = m.x();
			this.cz = m.z();
			this.carves = khufu ? KHUFU : null;
			ground(Blocks.SMOOTH_SANDSTONE, Blocks.SAND, Blocks.SANDSTONE, Blocks.SAND);
			clearH = 10;
		}

		/**
		 * Khufu's inner ways, in cells from the centre (x east, z south) and heights above the base: {x0, x1, z0, z1, floor y
		 * at zA, zA, rise per +z, height, corbel (full width only this high, 0 = always), material (0 air, 1 granite, 2
		 * polished granite)}. Linings come first, the rooms after them.
		 */
		static final double[][] KHUFU = {
				// King's Chamber lining (granite floor, walls and ceiling)
				{-4, 8, 6, 12, 42, 0, 0, 8, 0, 1},
				// Al-Ma'mun's tunnel (c. 820): the way in that everybody used in 1050
				{2, 3, -125, -79, 5, 0, 0, 3, 0, 0},
				{2, 7, -80, -77, 5, 0, 0, 3, 0, 0},
				// The original entrance high on the north face and the Descending Passage into the rock
				{6, 7, -108, -9, 17, -102, -0.5, 3, 0, 0},
				// Subterranean Chamber and its blind passage
				{-2, 9, -9, -2, -31, 0, 0, 4, 0, 0},
				{6, 7, -2, 12, -31, 0, 0, 2, 0, 0},
				// Ascending Passage
				{6, 7, -79, -44, 5, -78, 0.5, 3, 0, 0},
				// Grand Gallery (corbelled: the full width only near the floor)
				{5, 8, -45, -2, 22, -44, 0.5, 9, 3, 0},
				// Passage to the Queen's Chamber and the chamber
				{6, 7, -44, -4, 22, 0, 0, 2, 0, 0},
				{2, 8, -4, 1, 21, 0, 0, 6, 0, 0},
				// Antechamber and the way into the King's Chamber
				{6, 7, -2, 7, 43, 0, 0, 3, 0, 0},
				{5, 8, 1, 3, 43, 0, 0, 4, 0, 0},
				// King's Chamber and the granite sarcophagus at its west end
				{-3, 7, 7, 11, 43, 0, 0, 6, 0, 0},
				{-2, -2, 8, 10, 43, 0, 0, 1, 0, 2},
		};

		@Override
		double padDist(int x, int z) {
			return Math.max(Math.abs(x + 0.5 - cx), Math.abs(z + 0.5 - cz)) - (half + 4);
		}

		@Override
		void column(Sink out, int x, int z, int base) {
			double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
			double ax = Math.abs(dx), az = Math.abs(dz);
			double e = half - Math.max(ax, az);
			if (e <= 0) {
				return;
			}
			double hc;
			if (style == 2) {
				double tier = half / 4.0;
				int k = Math.min(3, (int) (e / tier));
				hc = (k + 1) * height / 4.0;
			} else {
				hc = Math.min(e * slope, cap);
			}
			int full = (int) Math.floor(hc);
			for (int y = 1; y <= full; y++) {
				out.set(x, base + y, z, stone(x, y, z, e));
			}
			if (style != 2 && hc < cap && hc - full >= 0.5) {
				out.set(x, base + full + 1, z, surface(x, full + 1, z, dx, dz, ax, az));
			}
			if (carves != null) {
				carve(out, x, z, base);
			}
		}

		private boolean worn(int x, int y, int z) {
			return h3(x, y, z, 7) < (y < 8 ? 0.15 : 0.012);
		}

		private BlockState stone(int x, int y, int z, double e) {
			if (style == 2) {
				return e < 1.5 || h3(x, y, z, 3) < 0.2 ? st(Blocks.CUT_SANDSTONE) : st(Blocks.SANDSTONE);
			}
			double depth = e - (y - 0.5) / slope;
			if (depth >= 2.5) {
				return st(Blocks.SANDSTONE);
			}
			if (y <= granite) {
				return st(Blocks.GRANITE);
			}
			return worn(x, y, z) ? st(Blocks.SANDSTONE) : st(Blocks.SMOOTH_SANDSTONE);
		}

		private BlockState surface(int x, int y, int z, double dx, double dz, double ax, double az) {
			Block stair, slab;
			if (y <= granite) {
				stair = Blocks.GRANITE_STAIRS;
				slab = Blocks.GRANITE_SLAB;
			} else if (worn(x, y, z)) {
				stair = Blocks.SANDSTONE_STAIRS;
				slab = Blocks.SANDSTONE_SLAB;
			} else {
				stair = Blocks.SMOOTH_SANDSTONE_STAIRS;
				slab = Blocks.SMOOTH_SANDSTONE_SLAB;
			}
			if (ax > az + 0.01) {
				return stairs(stair, dx > 0 ? Direction.WEST : Direction.EAST);
			}
			if (az > ax + 0.01) {
				return stairs(stair, dz > 0 ? Direction.NORTH : Direction.SOUTH);
			}
			return Bp.slab(slab, false);
		}

		private void carve(Sink out, int x, int z, int base) {
			int lx = x - (int) cx, lz = z - (int) cz;
			for (double[] c : carves) {
				if (lx < c[0] || lx > c[1] || lz < c[2] || lz > c[3]) {
					continue;
				}
				double fy = c[4] + (lz - c[5]) * c[6];
				int f = (int) Math.floor(fy);
				boolean half = fy - f >= 0.5;
				int h = (int) c[7];
				if (c[8] > 0 && (lx == (int) c[0] || lx == (int) c[1])) {
					h = (int) c[8];
				}
				int mat = (int) c[9];
				if (mat == 0) {
					int from = f + 1;
					int top = f + h;
					if (half) {
						out.set(x, base + f + 1, z, Bp.slab(Blocks.SANDSTONE_SLAB, false));
						from = f + 2;
						top++;
					}
					for (int y = from; y <= top; y++) {
						out.set(x, base + y, z, AIR);
					}
				} else {
					BlockState s = mat == 1 ? st(Blocks.GRANITE) : st(Blocks.POLISHED_GRANITE);
					for (int y = f + 1; y <= f + h; y++) {
						out.set(x, base + y, z, s);
					}
				}
			}
		}
	}

	/** The Great Sphinx, lying with its paws to the front (+z = east), the sand drifted against its flanks. */
	private static Bp sphinx() {
		Bp b = new Bp(31, 80, 2558L);
		int c = 15;
		// The lion's body: rump at the back, chest at z 59.
		for (int z = 4; z <= 59; z++) {
			double hw = z < 10 ? 5 + (z - 4) * 0.34 : 7;
			double hb = z < 10 ? 8.5 + (z - 4) * 0.55 : z >= 50 ? 13 : 12;
			for (int x = 0; x < b.w; x++) {
				double ax = Math.abs(x - c);
				if (ax > hw + 0.5) {
					continue;
				}
				int top = (int) Math.round(hb - Math.max(0, ax - (hw - 3)) * 1.7);
				for (int y = 1; y <= top; y++) {
					b.set(x, y, z, body(b, y));
				}
			}
		}
		// Folded hind legs bulging at the sides.
		for (int z = 6; z <= 22; z++) {
			for (int x = 0; x < b.w; x++) {
				double ax = Math.abs(x - c);
				if (ax <= 7.5 || ax > 10) {
					continue;
				}
				int top = (int) Math.round(6 - (ax - 7.5) * 1.6 - (z > 18 ? (z - 18) * 0.8 : 0));
				for (int y = 1; y <= top; y++) {
					b.set(x, y, z, body(b, y));
				}
			}
		}
		// The tail along the right haunch.
		for (int z = 9; z <= 21; z++) {
			b.soft(c + 11, 1, z, Bp.slab(Blocks.SMOOTH_SANDSTONE_SLAB, false));
		}
		// Forelegs and paws stretched forward.
		for (int z = 50; z <= 76; z++) {
			int top = z <= 61 ? 5 : z >= 74 ? 2 : 3;
			for (int x = c - 6; x <= c + 6; x++) {
				if (Math.abs(x - c) < 3) {
					continue;
				}
				for (int y = 1; y <= top; y++) {
					b.set(x, y, z, body(b, y));
				}
			}
		}
		// The head with the nemes headdress (striped) and the face.
		for (int z = 51; z <= 61; z++) {
			for (int x = c - 4; x <= c + 4; x++) {
				for (int y = 12; y <= 19; y++) {
					boolean face = z == 61 && Math.abs(x - c) <= 3 && y <= 18;
					b.set(x, y, z, face ? st(Blocks.SMOOTH_SANDSTONE) : nemes(y));
				}
			}
		}
		for (int z = 52; z <= 60; z++) {
			for (int x = c - 3; x <= c + 3; x++) {
				b.set(x, 20, z, nemes(20));
			}
		}
		for (int z = 55; z <= 60; z++) {
			for (int y = 8; y <= 17; y++) {
				b.set(c - 5, y, z, nemes(y));
				b.set(c + 5, y, z, nemes(y));
			}
		}
		for (int z = 56; z <= 59; z++) {
			for (int y = 9; y <= 15; y++) {
				b.set(c - 6, y, z, nemes(y));
				b.set(c + 6, y, z, nemes(y));
			}
		}
		// Face: eyes, brows, nose, mouth, the cobra on the brow and the false beard (still on in 1050).
		b.set(c - 2, 17, 61, Blocks.TERRACOTTA);
		b.set(c + 2, 17, 61, Blocks.TERRACOTTA);
		b.set(c - 2, 18, 61, Blocks.CUT_SANDSTONE);
		b.set(c + 2, 18, 61, Blocks.CUT_SANDSTONE);
		b.set(c, 15, 62, Blocks.SANDSTONE);
		b.set(c, 16, 62, Blocks.SANDSTONE);
		for (int x = c - 1; x <= c + 1; x++) {
			b.set(x, 13, 61, Blocks.TERRACOTTA);
		}
		b.set(c, 19, 62, Blocks.CUT_SANDSTONE);
		for (int y = 9; y <= 11; y++) {
			b.set(c, y, 61, Blocks.CUT_SANDSTONE);
		}
		// The Dream Stela of Thutmose IV between the paws.
		for (int x = c - 2; x <= c + 2; x++) {
			for (int y = 1; y <= 4; y++) {
				b.set(x, y, 63, Blocks.POLISHED_GRANITE);
			}
		}
		for (int x = c - 1; x <= c + 1; x++) {
			b.set(x, 5, 63, Blocks.GRANITE);
		}
		// Sand drifted against the flanks and the back.
		boolean[] occ = new boolean[b.w * b.d];
		for (int x = 0; x < b.w; x++) {
			for (int z = 0; z < b.d; z++) {
				occ[z * b.w + x] = b.has(x, 1, z);
			}
		}
		for (int x = 0; x < b.w; x++) {
			for (int z = 0; z < 52; z++) {
				if (occ[z * b.w + x]) {
					continue;
				}
				double best = 99;
				for (int i = -7; i <= 7; i++) {
					for (int j = -7; j <= 7; j++) {
						int xx = x + i, zz = z + j;
						if (xx >= 0 && zz >= 0 && xx < b.w && zz < b.d && occ[zz * b.w + xx]) {
							best = Math.min(best, Math.hypot(i, j));
						}
					}
				}
				int h = (int) Math.round(4.5 - best * 0.75 + (b.r.nextDouble() - 0.5));
				for (int y = 1; y <= h; y++) {
					b.soft(x, y, z, st(Blocks.SAND));
				}
			}
		}
		return b;
	}

	private static BlockState body(Bp b, int y) {
		if (b.r.nextDouble() < 0.08) {
			return st(Blocks.SANDSTONE);
		}
		return switch (y % 4) {
			case 0 -> st(Blocks.CUT_SANDSTONE);
			case 2 -> st(Blocks.SMOOTH_SANDSTONE);
			default -> st(Blocks.SANDSTONE);
		};
	}

	private static BlockState nemes(int y) {
		return y % 2 == 0 ? st(Blocks.SMOOTH_SANDSTONE) : st(Blocks.CUT_SANDSTONE);
	}

	// =====================================================================================================================
	// Stonehenge
	// =====================================================================================================================

	/** Stonehenge in ruin, drawn with north at -z (rotation 0); the centre of the circle is at (100, 100). */
	private static Bp stonehenge() {
		Bp b = new Bp(200, 200, 1866L);
		double axis = Math.toRadians(50);
		double ux = Math.sin(axis), uz = -Math.cos(axis); // along the solstice axis, towards the north-east
		double vx = -uz, vz = ux; // across it
		// Uprights of the sarsen circle clockwise from the entrance: S standing, F fallen, M gone.
		String upr = "SSSSSSSFFSSFMFFSMMMMSFSMFMSSSS";
		boolean[] lintel = new boolean[30];
		for (int i : new int[]{29, 0, 1, 2, 3, 4, 5, 9, 26, 27, 28}) {
			lintel[i] = true;
		}
		BlockState grass = st(Blocks.GRASS_BLOCK), dirt = st(Blocks.DIRT);
		for (int x = 0; x < 200; x++) {
			for (int z = 0; z < 200; z++) {
				double dx = x + 0.5 - 100, dz = z + 0.5 - 100;
				double r = Math.hypot(dx, dz);
				double brg = Math.atan2(dx, -dz);
				double along = dx * ux + dz * uz, across = dx * vx + dz * vz;
				// The earthwork: a bank inside a ditch, broken by the north-east entrance and a small southern one.
				boolean gapNE = Math.abs(across) < 6 && along > 0;
				boolean gapS = Math.abs(angDiff(brg, Math.toRadians(165))) * r < 2.5;
				if (!gapNE && !gapS) {
					if (r >= 52 && r < 56.5) {
						b.set(x, 0, z, AIR);
						if (r >= 52.8 && r < 55.7) {
							b.set(x, -1, z, AIR);
							b.set(x, -2, z, grass);
						} else {
							b.set(x, -1, z, grass);
						}
					} else if (r >= 47 && r < 52) {
						int h = r >= 48.2 && r < 50.8 ? 2 : 1;
						for (int y = 1; y <= h; y++) {
							b.set(x, y, z, y == h ? grass : dirt);
						}
					}
				}
				// The Avenue: two low banks with ditches outside, leading off to the north-east.
				if (along > 56 && along < 84) {
					double aa = Math.abs(across);
					if (aa >= 10 && aa < 12.5) {
						b.set(x, 1, z, grass);
						b.set(x, 0, z, dirt);
					} else if (aa >= 12.5 && aa < 14.5) {
						b.set(x, 0, z, AIR);
						b.set(x, -1, z, grass);
					}
				}
				// The Heel Stone, leaning towards the circle.
				if (Math.abs(along - 77) < 1.3 && Math.abs(across - 2.5) < 1.1) {
					for (int y = 1; y <= 3; y++) {
						b.set(x, y, z, sarsen(b));
					}
				}
				if (Math.abs(along - 76.2) < 1.2 && Math.abs(across - 2.5) < 1.0) {
					b.set(x, 4, z, sarsen(b));
					b.set(x, 5, z, sarsen(b));
				}
				// The Slaughter Stone, fallen in the entrance.
				if (Math.abs(along - 58.5) < 1.9 && Math.abs(across + 1) < 0.9) {
					b.set(x, 1, z, sarsen(b));
				}
				// The sarsen circle: 30 uprights with a ring of lintels, half of it fallen or taken away.
				if (r > 14.6 && r < 21) {
					double deg = Math.toDegrees(angDiff(brg, axis));
					double fi = deg / 12.0 - 0.5;
					int ii = (int) Math.round(fi);
					double off = Math.toRadians((fi - ii) * 12) * r;
					int i = Math.floorMod(ii, 30);
					char s = upr.charAt(i);
					if (r < 16.4 && Math.abs(off) < 1.05 && s == 'S') {
						for (int y = 1; y <= 4; y++) {
							b.set(x, y, z, sarsen(b));
						}
					} else if (r >= 16.4 && r < 20.4 && Math.abs(off) < 1.0 && s == 'F') {
						b.set(x, 1, z, sarsen(b));
					}
					if (r >= 15.0 && r < 16.3) {
						int j = Math.floorMod((int) Math.floor(fi), 30);
						if (lintel[j] && upr.charAt(j) == 'S' && upr.charAt((j + 1) % 30) == 'S') {
							b.set(x, 5, z, sarsen(b));
						}
					}
				}
				// The bluestone circle.
				if (r >= 11.2 && r < 12.3) {
					double deg = Math.toDegrees(angDiff(brg, axis));
					if (Math.abs(deg) > 8) {
						double fi = deg / 9.0;
						int i = (int) Math.round(fi);
						double off = Math.toRadians((fi - i) * 9) * r;
						double hh = h3(i, 0, 0, 77);
						if (Math.abs(off) < 0.6 && hh < 0.7) {
							int top = hh < 0.5 ? 2 : 1;
							for (int y = 1; y <= top; y++) {
								b.set(x, y, z, bluestone(b));
							}
						}
					}
				}
				// The bluestone horseshoe inside the trilithons, open to the north-east.
				if (r >= 6.3 && r < 7.3) {
					double rel = Math.floorMod((int) Math.round(Math.toDegrees(angDiff(brg, axis)) * 10), 3600) / 10.0;
					if (rel >= 50 && rel <= 310) {
						double fi = (rel - 55) / 13.9;
						int i = (int) Math.round(fi);
						double off = Math.toRadians((fi - i) * 13.9) * r;
						if (Math.abs(off) < 0.6 && h3(i, 1, 0, 78) < 0.65) {
							int top = Math.abs(rel - 180) < 60 ? 3 : 2;
							for (int y = 1; y <= top; y++) {
								b.set(x, y, z, bluestone(b));
							}
						}
					}
				}
				// The Altar Stone, fallen across the axis under the great trilithon.
				double ax2 = dx + 4.5 * ux, az2 = dz + 4.5 * uz;
				if (Math.abs(ax2 * vx + az2 * vz) < 2.6 && Math.abs(ax2 * ux + az2 * uz) < 0.6) {
					b.set(x, 1, z, Blocks.SMOOTH_STONE);
				}
				// Two Station Stones (one standing, one fallen).
				double s1x = Math.sin(Math.toRadians(140)) * 40.5, s1z = -Math.cos(Math.toRadians(140)) * 40.5;
				if (Math.hypot(dx - s1x, dz - s1z) < 0.75) {
					for (int y = 1; y <= 2; y++) {
						b.set(x, y, z, sarsen(b));
					}
				}
				double s2x = Math.sin(Math.toRadians(300)) * 40.5, s2z = -Math.cos(Math.toRadians(300)) * 40.5;
				if (Math.hypot(dx - s2x, dz - s2z) < 1.1) {
					b.set(x, 1, z, sarsen(b));
				}
			}
		}
		// The five trilithons in a horseshoe, open to the north-east; the great one is broken, one has lost its lintel.
		double[] tb = {axis + Math.toRadians(70), axis + Math.toRadians(125), axis + Math.PI, axis - Math.toRadians(125), axis - Math.toRadians(70)};
		double[] tr = {10.5, 10, 9.5, 10, 10.5};
		int[] th = {6, 6, 7, 6, 6};
		int[] state = {0, 0, 2, 3, 0};
		for (int t = 0; t < 5; t++) {
			double psi = tb[t], R = tr[t];
			double pcx = Math.sin(psi) * R, pcz = -Math.cos(psi) * R;
			double tx = Math.cos(psi), tz = Math.sin(psi), rx = Math.sin(psi), rz = -Math.cos(psi);
			for (int x = (int) Math.floor(100 + pcx - 9); x <= (int) Math.ceil(100 + pcx + 9); x++) {
				for (int z = (int) Math.floor(100 + pcz - 9); z <= (int) Math.ceil(100 + pcz + 9); z++) {
					double dx = x + 0.5 - 100 - pcx, dz = z + 0.5 - 100 - pcz;
					double p = dx * tx + dz * tz, q = dx * rx + dz * rz;
					boolean left = p > -2.8 && p < -0.5, right = p > 0.5 && p < 2.8;
					if (Math.abs(q) <= 1.0 && (left || right)) {
						if (!(state[t] == 2 && right)) {
							int top = th[t];
							for (int y = 1; y <= top; y++) {
								b.set(x, y, z, sarsen(b));
							}
						}
					}
					if (state[t] == 2 && right && q < -1.0 && q > -1.0 - th[t]) {
						b.set(x, 1, z, sarsen(b));
					}
					if (state[t] == 0 && Math.abs(q) <= 0.9 && Math.abs(p) < 2.9) {
						b.set(x, th[t] + 1, z, sarsen(b));
					}
					if ((state[t] == 2 || state[t] == 3) && q > 1.4 && q < 2.6 && Math.abs(p) < 2.9) {
						b.set(x, 1, z, sarsen(b));
					}
				}
			}
		}
		// Tufts of grass on the downland inside the bank.
		for (int x = 50; x < 150; x++) {
			for (int z = 50; z < 150; z++) {
				double r = Math.hypot(x + 0.5 - 100, z + 0.5 - 100);
				if (r < 45 && !b.has(x, 1, z) && b.r.nextDouble() < 0.06) {
					b.set(x, 1, z, Blocks.SHORT_GRASS);
				}
			}
		}
		return b;
	}

	private static BlockState sarsen(Bp b) {
		double r = b.r.nextDouble();
		return r < 0.45 ? st(Blocks.STONE) : r < 0.8 ? st(Blocks.ANDESITE) : r < 0.93 ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.COBBLESTONE);
	}

	private static BlockState bluestone(Bp b) {
		return b.r.nextDouble() < 0.75 ? st(Blocks.TUFF) : st(Blocks.POLISHED_ANDESITE);
	}

	// =====================================================================================================================
	// The Pharos of Alexandria
	// =====================================================================================================================

	/**
	 * The lighthouse on its island platform: a square tier (~55 m) with floors and stairs inside, an octagonal tier, a round
	 * tier and the little mosque that was built on top. The door is high up, reached by a ramp on arches (front, +z).
	 */
	private static Bp pharos() {
		Bp b = new Bp(72, 72, 285L);
		final int P = 3;
		BlockState brick = st(Blocks.STONE_BRICKS), smooth = st(Blocks.SMOOTH_STONE), calcite = st(Blocks.CALCITE);
		// The rock platform with its paving.
		b.fill(4, -3, 4, 67, P - 1, 67, brick);
		for (int x = 4; x <= 67; x++) {
			for (int z = 4; z <= 67; z++) {
				b.set(x, P, z, (x + z) % 2 == 0 ? smooth : calcite);
			}
		}
		// The enclosure wall with round corner towers and a gate to the south.
		for (int x = 4; x <= 67; x++) {
			for (int z = 4; z <= 67; z++) {
				boolean edge = x < 6 || x > 65 || z < 6 || z > 65;
				boolean outer = x == 4 || x == 67 || z == 4 || z == 67;
				if (edge) {
					for (int y = P + 1; y <= P + 6; y++) {
						b.set(x, y, z, wallStone(b));
					}
					if (outer && (x + z) % 2 == 0) {
						b.set(x, P + 7, z, wallStone(b));
					}
				}
			}
		}
		int[][] corners = {{6, 6}, {65, 6}, {6, 65}, {65, 65}};
		for (int[] cc : corners) {
			for (int x = cc[0] - 4; x <= cc[0] + 4; x++) {
				for (int z = cc[1] - 4; z <= cc[1] + 4; z++) {
					double d = Math.hypot(x - cc[0], z - cc[1]);
					if (d <= 3.6) {
						for (int y = P + 1; y <= P + 10; y++) {
							b.set(x, y, z, wallStone(b));
						}
						if (d > 2.6 && (x + z) % 2 == 0) {
							b.set(x, P + 11, z, wallStone(b));
						}
					}
				}
			}
		}
		for (int x = 33; x <= 38; x++) {
			for (int z = 64; z <= 67; z++) {
				for (int y = P + 1; y <= P + 4; y++) {
					b.air(x, y, z);
				}
			}
			for (int i = 0; i < 3; i++) {
				int z = 68 + i, y = P - i;
				b.set(x, y, z, stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH));
				for (int yy = -2; yy < y; yy++) {
					b.set(x, yy, z, brick);
				}
			}
		}
		// Tier 1: the square tower (21..50), a little narrower in its upper half.
		int top1 = P + 55;
		for (int y = P + 1; y <= top1; y++) {
			int lo = y >= P + 29 ? 22 : 21, hi = y >= P + 29 ? 49 : 50;
			BlockState band = y % 6 == 0 ? st(Blocks.POLISHED_DIORITE) : null;
			for (int x = lo; x <= hi; x++) {
				for (int z = lo; z <= hi; z++) {
					if (x < lo + 2 || x > hi - 2 || z < lo + 2 || z > hi - 2) {
						b.set(x, y, z, band != null ? band : lightStone(b));
					}
				}
			}
		}
		int[] floors = {P, P + 9, P + 18, P + 27, P + 36, P + 45, top1};
		for (int k = 0; k < floors.length; k++) {
			int y = floors[k];
			int lo = y >= P + 29 ? 22 : 21, hi = y >= P + 29 ? 49 : 50;
			int a = k == floors.length - 1 ? lo : lo + 2, e = k == floors.length - 1 ? hi : hi - 2;
			for (int x = a; x <= e; x++) {
				for (int z = a; z <= e; z++) {
					b.set(x, y, z, smooth);
				}
			}
		}
		// Windows on every face of every storey.
		for (int k = 0; k < floors.length - 1; k++) {
			int y = floors[k] + 5;
			int lo = y >= P + 29 ? 22 : 21, hi = y >= P + 29 ? 49 : 50;
			for (int i = lo + 3; i <= hi - 3; i++) {
				if ((i - lo) % 5 != 2) {
					continue;
				}
				for (int yy = y; yy <= y + 1; yy++) {
					for (int t = 0; t < 2; t++) {
						b.air(i, yy, lo + t);
						b.air(i, yy, hi - t);
						b.air(lo + t, yy, i);
						b.air(hi - t, yy, i);
					}
				}
			}
		}
		// Stairs from floor to floor, alternating along the north and the south wall.
		for (int k = 0; k + 1 < floors.length; k++) {
			int rise = floors[k + 1] - floors[k];
			for (int i = 0; i < rise; i++) {
				int y = floors[k] + 1 + i;
				for (int w = 0; w < 2; w++) {
					int x = k % 2 == 0 ? 26 + i : 45 - i;
					int z = k % 2 == 0 ? 24 + w : 46 + w;
					b.set(x, y, z, stairs(Blocks.POLISHED_DIORITE_STAIRS, k % 2 == 0 ? Direction.EAST : Direction.WEST));
					for (int yy = y + 1; yy <= y + 3; yy++) {
						b.air(x, yy, z);
					}
				}
			}
		}
		// The high door on the south face and the ramp on arches leading up to it.
		for (int x = 35; x <= 36; x++) {
			for (int z = 49; z <= 50; z++) {
				for (int y = P + 10; y <= P + 12; y++) {
					b.air(x, y, z);
				}
			}
		}
		for (int i = 0; i <= 8; i++) {
			int z = 51 + i, y = P + 9 - i;
			for (int x = 34; x <= 37; x++) {
				b.set(x, y, z, stairs(Blocks.POLISHED_DIORITE_STAIRS, Direction.NORTH));
				for (int yy = P + 1; yy < y; yy++) {
					boolean arch = z >= 54 && z <= 56 && yy <= P + 3;
					if (!arch) {
						b.set(x, yy, z, brick);
					}
				}
			}
		}
		// Parapet and the four bronze tritons on the corners of tier 1.
		for (int x = 22; x <= 49; x++) {
			for (int z = 22; z <= 49; z++) {
				boolean rim = x == 22 || x == 49 || z == 22 || z == 49;
				if (rim && (x + z) % 2 == 0) {
					b.set(x, top1 + 1, z, lightStone(b));
				}
			}
		}
		for (int[] cc : new int[][]{{22, 22}, {49, 22}, {22, 49}, {49, 49}}) {
			b.set(cc[0], top1 + 1, cc[1], Blocks.POLISHED_DIORITE);
			b.set(cc[0], top1 + 2, cc[1], Blocks.DYED_TERRACOTTA.pick(DyeColor.GREEN));
			b.set(cc[0], top1 + 3, cc[1], Blocks.DYED_TERRACOTTA.pick(DyeColor.GREEN));
		}
		// Tier 2: the octagon.
		int top2 = top1 + 27;
		for (int y = top1 + 1; y <= top2; y++) {
			for (int x = 24; x <= 47; x++) {
				for (int z = 24; z <= 47; z++) {
					if (oct(x, z, 9) && !oct(x, z, 7)) {
						b.set(x, y, z, y % 6 == 0 ? st(Blocks.POLISHED_DIORITE) : lightStone(b));
					}
				}
			}
		}
		int[] oFloors = {top1, top1 + 9, top1 + 18, top2};
		for (int k = 1; k < oFloors.length; k++) {
			for (int x = 24; x <= 47; x++) {
				for (int z = 24; z <= 47; z++) {
					if (oct(x, z, k == oFloors.length - 1 ? 9 : 7)) {
						b.set(x, oFloors[k], z, smooth);
					}
				}
			}
		}
		for (int k = 0; k + 1 < oFloors.length; k++) {
			for (int i = 0; i < 9; i++) {
				int y = oFloors[k] + 1 + i;
				for (int w = 0; w < 2; w++) {
					int x = k % 2 == 0 ? 31 + i : 39 - i;
					int z = k % 2 == 0 ? 30 + w : 39 + w;
					b.set(x, y, z, stairs(Blocks.POLISHED_DIORITE_STAIRS, k % 2 == 0 ? Direction.EAST : Direction.WEST));
					for (int yy = y + 1; yy <= y + 3; yy++) {
						b.air(x, yy, z);
					}
				}
			}
			int wy = oFloors[k] + 4;
			for (int yy = wy; yy <= wy + 1; yy++) {
				for (int t = 0; t < 2; t++) {
					b.air(35, yy, 26 + t);
					b.air(35, yy, 44 - t);
					b.air(26 + t, yy, 35);
					b.air(44 - t, yy, 35);
				}
			}
		}
		for (int x = 35; x <= 36; x++) {
			for (int z = 43; z <= 44; z++) {
				b.air(x, top1 + 1, z);
				b.air(x, top1 + 2, z);
			}
		}
		for (int x = 24; x <= 47; x++) {
			for (int z = 24; z <= 47; z++) {
				if (oct(x, z, 9) && !oct(x, z, 8) && (x + z) % 2 == 0) {
					b.set(x, top2 + 1, z, lightStone(b));
				}
			}
		}
		// Tier 3: the round lantern tower, with a ladder inside.
		int top3 = top2 + 9;
		for (int y = top2 + 1; y <= top3; y++) {
			for (int x = 29; x <= 42; x++) {
				for (int z = 29; z <= 42; z++) {
					double d = Math.hypot(x - 35, z - 35);
					if (y == top3 ? d <= 5.5 : d > 4.4 && d <= 5.5) {
						b.set(x, y, z, y == top3 ? smooth : lightStone(b));
					}
				}
			}
		}
		for (int y = top2 + 4; y <= top2 + 5; y++) {
			b.air(40, y, 35);
			b.air(30, y, 35);
		}
		b.air(35, top2 + 1, 40);
		b.air(35, top2 + 2, 40);
		for (int y = top2 + 1; y <= top3; y++) {
			b.set(35, y, 31, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}
		// The little mosque on the top and the beacon fires.
		for (int x = 29; x <= 42; x++) {
			for (int z = 29; z <= 42; z++) {
				double d = Math.hypot(x - 35, z - 35);
				if (d > 4.4 && d <= 5.5 && !(x == 35 && z == 31)) {
					b.set(x, top3 + 1, z, Bp.slab(Blocks.SMOOTH_STONE_SLAB, false));
				}
			}
		}
		b.walls(33, top3 + 1, 33, 37, top3 + 4, 37, calcite);
		b.air(35, top3 + 1, 37);
		b.air(35, top3 + 2, 37);
		b.fill(33, top3 + 5, 33, 37, top3 + 5, 37, calcite);
		b.dome(35.5, top3 + 5, 35.5, 2.6, calcite, st(Blocks.GOLD_BLOCK));
		b.set(34, top3 + 1, 34, Blocks.CARPET.pick(DyeColor.RED));
		b.set(35, top3 + 1, 34, Blocks.CARPET.pick(DyeColor.RED));
		b.set(36, top3 + 1, 34, Blocks.CARPET.pick(DyeColor.RED));
		b.set(35, top3 + 4, 35, Bp.lantern(true));
		b.set(34, top3 + 1, 39, Blocks.CAMPFIRE);
		b.set(36, top3 + 1, 39, Blocks.CAMPFIRE);
		return b;
	}

	/** Inside a regular octagon of apothem a around the tower axis (cells 35/36 are the middle). */
	private static boolean oct(int x, int z, double a) {
		double dx = Math.abs(x + 0.5 - 35.5), dz = Math.abs(z + 0.5 - 35.5);
		return dx <= a && dz <= a && dx + dz <= a * 1.4142;
	}

	private static BlockState lightStone(Bp b) {
		double r = b.r.nextDouble();
		return r < 0.82 ? st(Blocks.CALCITE) : r < 0.94 ? st(Blocks.SMOOTH_SANDSTONE) : st(Blocks.DIORITE);
	}

	private static BlockState wallStone(Bp b) {
		double r = b.r.nextDouble();
		return r < 0.7 ? st(Blocks.STONE_BRICKS) : r < 0.85 ? st(Blocks.CALCITE) : st(Blocks.MOSSY_STONE_BRICKS);
	}

	// =====================================================================================================================
	// The Colosseum
	// =====================================================================================================================

	/**
	 * The Flavian amphitheatre in 1050: an ellipse of 189 × 156 m, four storeys high; the outer ring has fallen in the
	 * south-west (rubble mounds there), grass and moss on the stones, a few families living in walled-up arches.
	 */
	private static final class Colosseum extends Columns {
		static final double A = 94.5, B = 78.0;
		final double cx, cz, ca, sa;

		Colosseum(Mark m) {
			super(m, true);
			cx = m.x();
			cz = m.z();
			double brg = Math.toRadians(74); // the long axis runs ENE-WSW
			ca = Math.sin(brg);
			sa = -Math.cos(brg);
			ground(Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.STONE, Blocks.DIRT);
			clearH = 16;
		}

		private static double f(double u, double v, double t) {
			double a = A - t, b = B - t;
			return u * u / (a * a) + v * v / (b * b) - 1;
		}

		/** How deep (blocks) a point lies inside the outer ellipse (negative outside). */
		static double depth(double u, double v) {
			double lo = -40, hi = B - 0.3;
			if (f(u, v, lo) > 0) {
				return lo - 1;
			}
			if (f(u, v, hi) < 0) {
				return hi;
			}
			for (int i = 0; i < 22; i++) {
				double mid = (lo + hi) / 2;
				if (f(u, v, mid) > 0) {
					hi = mid;
				} else {
					lo = mid;
				}
			}
			return (lo + hi) / 2;
		}

		@Override
		double padDist(int x, int z) {
			double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
			double u = dx * ca + dz * sa, v = -dx * sa + dz * ca;
			return -depth(u, v) - 12;
		}

		private static BlockState trav(int x, int y, int z) {
			double r = h3(x, y, z, 3);
			return r < 0.6 ? st(Blocks.CALCITE) : r < 0.82 ? st(Blocks.SMOOTH_SANDSTONE) : r < 0.93 ? st(Blocks.CUT_SANDSTONE) : st(Blocks.TUFF);
		}

		private static BlockState brick(int x, int y, int z) {
			double r = h3(x, y, z, 4);
			return r < 0.45 ? st(Blocks.BRICKS) : r < 0.8 ? st(Blocks.TUFF_BRICKS) : r < 0.9 ? st(Blocks.TUFF) : trav(x, y, z);
		}

		private static BlockState rubble(int x, int y, int z) {
			double r = h3(x, y, z, 5);
			return r < 0.35 ? st(Blocks.COBBLESTONE) : r < 0.6 ? st(Blocks.MOSSY_COBBLESTONE) : r < 0.75 ? st(Blocks.GRAVEL)
					: r < 0.88 ? st(Blocks.CALCITE) : st(Blocks.STONE);
		}

		/** A grassy or mossy top on ruined masonry, and now and then a tuft of grass on it. */
		private static void overgrow(Sink out, int x, int y, int z) {
			double r = h3(x, y, z, 6);
			if (r < 0.45) {
				out.set(x, y, z, st(Blocks.GRASS_BLOCK));
				if (r < 0.2) {
					out.set(x, y + 1, z, st(Blocks.SHORT_GRASS));
				}
			} else if (r < 0.6) {
				out.set(x, y, z, st(Blocks.MOSS_BLOCK));
			}
		}

		@Override
		void column(Sink out, int x, int z, int base) {
			double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
			double u = dx * ca + dz * sa, v = -dx * sa + dz * ca;
			double t = depth(u, v);
			if (t < -12) {
				return;
			}
			double tt = Math.max(0, Math.min(t, 50));
			double phi = Math.atan2(v / (B - tt), u / (A - tt));
			double bayF = (phi / (2 * Math.PI) + 1) * 80;
			int bay = Math.floorMod((int) Math.floor(bayF), 80);
			double p = bayF - Math.floor(bayF);
			double bayLen = 2 * Math.PI * Math.sqrt(((A - tt) * (A - tt) + (B - tt) * (B - tt)) / 2) / 80;
			double q = (p - 0.5) * bayLen;
			// The fallen south-west.
			double sw = Math.abs(angDiff(Math.atan2(dz, dx), 0.75 * Math.PI));
			double ruin = clamp((0.85 - sw) / 0.35, 0, 1);
			int hW1 = ruin > 0 ? (int) Math.round(48 * (1 - ruin) + ruin * (1 + 6 * h3(bay, 0, 0, 5))) : (h3(x, 0, z, 9) < 0.25 ? 47 : 48);
			boolean gate = Math.abs(v) < 2.5 || Math.abs(u) < 2.5;
			boolean house = bay % 11 == 4 && ruin == 0 && !gate;

			if (t < 0) {
				// Outside: the remains of the travertine paving, the cornices and the rubble mounds of the fallen arcades.
				if (t >= -8) {
					double r = h3(x, 0, z, 12);
					if (r < 0.25) {
						out.set(x, base, z, st(Blocks.GRAVEL));
					} else if (r < 0.5) {
						out.set(x, base, z, st(Blocks.SMOOTH_SANDSTONE));
					}
				}
				if (t >= -1 && !gate) {
					for (int y : new int[]{10, 21, 32, 47}) {
						if (y < hW1) {
							out.set(x, base + y, z, Bp.slab(Blocks.SMOOTH_SANDSTONE_SLAB, true));
						}
					}
				}
				rubbleMound(out, x, z, base, t, ruin);
				return;
			}
			if (t >= 51) {
				arena(out, x, z, base, u, v);
				return;
			}
			if (t < 2) {
				outerWall(out, x, z, base, t, q, bay, hW1, ruin, house, u, v);
			} else if (t < 6.5) {
				ambulatory(out, x, z, base, true, p, t, hW1, ruin, house);
				rubbleMound(out, x, z, base, t, ruin);
			} else if (t < 8 || (t >= 12.5 && t < 14)) {
				innerWall(out, x, z, base, q, bay, t < 8 ? ruin : 0);
			} else if (t < 12.5) {
				ambulatory(out, x, z, base, false, p, t, hW1, ruin, false);
			} else if (t < 47) {
				cavea(out, x, z, base, t, ruin, bay, q);
			} else {
				podium(out, x, z, base, t);
			}
			if (gate && t < 51) {
				for (int y = 1; y <= 7; y++) {
					out.set(x, base + y, z, AIR);
				}
				out.set(x, base, z, st(Blocks.STONE_BRICKS));
			}
		}

		private void rubbleMound(Sink out, int x, int z, int base, double t, double ruin) {
			if (ruin <= 0) {
				return;
			}
			double m = ruin * Math.max(0, 7 * (1 - Math.abs(t - 1) / 9)) * (0.7 + 0.6 * h3(x, 1, z, 11));
			int n = (int) m;
			for (int y = 1; y <= n; y++) {
				out.set(x, base + y, z, rubble(x, y, z));
			}
			if (n > 0) {
				overgrow(out, x, base + n, z);
			}
		}

		private void outerWall(Sink out, int x, int z, int base, double t, double q, int bay, int hW1, double ruin, boolean house, double u, double v) {
			for (int y = 1; y <= hW1; y++) {
				boolean open;
				if (y <= 32) {
					int yb = y <= 10 ? 0 : y <= 21 ? 10 : 21;
					int ys = y - yb;
					double aq = Math.abs(q);
					open = ys <= 7 && aq <= 2.1 || ys == 8 && aq <= 1.6 || ys == 9 && aq <= 0.9;
				} else {
					open = bay % 2 == 0 && y >= 38 && y <= 39 && Math.abs(q) <= 0.8;
				}
				if (open && house && y <= 10) {
					// A family lives in this arch: walled up with a door and a window.
					if (Math.abs(q) < 0.5 && y <= 2) {
						if (t >= 1) {
							double gu = u / (A * A), gv = v / (B * B);
							Direction outDir = cardinal(gu * ca - gv * sa, gu * sa + gv * ca);
							BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, outDir.getOpposite())
									.setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT).setValue(BlockStateProperties.OPEN, false)
									.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, y == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
							out.set(x, base + y, z, door);
						} else {
							out.set(x, base + y, z, AIR);
						}
					} else if (Math.abs(q) < 0.5 && y == 5) {
						out.set(x, base + y, z, AIR);
					} else {
						out.set(x, base + y, z, h3(x, y, z, 13) < 0.3 ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.COBBLESTONE));
					}
					continue;
				}
				if (open) {
					out.set(x, base + y, z, AIR);
					continue;
				}
				out.set(x, base + y, z, y == 10 || y == 21 || y == 32 || y == 47 ? st(Blocks.CUT_SANDSTONE) : trav(x, y, z));
			}
			if (hW1 < 46) {
				overgrow(out, x, base + hW1, z);
			} else if (h3(x, 2, z, 14) < 0.1) {
				out.set(x, base + hW1 + 1, z, st(Blocks.MOSS_CARPET));
			}
		}

		private void ambulatory(Sink out, int x, int z, int base, boolean outer, double p, double t, int hW1, double ruin, boolean house) {
			out.set(x, base, z, h3(x, 0, z, 15) < 0.3 ? st(Blocks.GRAVEL) : st(Blocks.STONE_BRICKS));
			for (int y : new int[]{10, 21, 32}) {
				if (!outer || y < hW1 - 1) {
					out.set(x, base + y, z, brick(x, y, z));
					if (y == 32 && h3(x, y, z, 16) < 0.35) {
						overgrow(out, x, base + y, z);
					}
				}
			}
			if (!house) {
				return;
			}
			// The home in the arcade: partitions at the ends of the bay and a few belongings.
			if (p < 0.07 || p > 0.93) {
				for (int y = 1; y <= 9; y++) {
					out.set(x, base + y, z, st(Blocks.COBBLESTONE));
				}
			} else if (p >= 0.3 && p < 0.4 && t >= 5) {
				out.set(x, base + 1, z, st(Blocks.BARREL));
			} else if (p >= 0.4 && p < 0.5 && t >= 5.5) {
				out.set(x, base + 1, z, st(Blocks.CRAFTING_TABLE));
			} else if (p >= 0.6 && p < 0.75 && t >= 4.5) {
				out.set(x, base + 1, z, st(Blocks.HAY_BLOCK));
			} else if (p >= 0.5 && p < 0.56 && t >= 3.5 && t < 4.5) {
				out.set(x, base + 1, z, st(Blocks.CAMPFIRE));
			}
		}

		private void innerWall(Sink out, int x, int z, int base, double q, int bay, double ruin) {
			int h = 32 - (int) Math.round(ruin * 8 * h3(bay, 1, 0, 17));
			for (int y = 1; y <= h; y++) {
				int yb = y <= 10 ? 0 : y <= 21 ? 10 : 21;
				int ys = y - yb;
				double aq = Math.abs(q);
				boolean open = ys <= 6 && aq <= 1.8 || ys == 7 && aq <= 1.2;
				out.set(x, base + y, z, open ? AIR : brick(x, y, z));
			}
			if (h < 32) {
				overgrow(out, x, base + h, z);
			}
		}

		private void cavea(Sink out, int x, int z, int base, double t, double ruin, int bay, double q) {
			double ys = 32 - (t - 14) * 28.0 / 33.0;
			int top = (int) Math.floor(ys);
			if (ruin > 0 && t < 26) {
				top -= (int) Math.round(ruin * 5 * h3(x, 3, z, 18));
			}
			for (int y = 1; y < top; y++) {
				out.set(x, base + y, z, h3(x, y, z, 19) < 0.7 ? st(Blocks.TUFF) : st(Blocks.BRICKS));
			}
			double r = h3(x, top, z, 20);
			BlockState seat = r < 0.3 ? st(Blocks.GRASS_BLOCK) : r < 0.42 ? st(Blocks.MOSS_BLOCK) : r < 0.7 ? st(Blocks.STONE_BRICKS)
					: r < 0.85 ? st(Blocks.SMOOTH_STONE) : st(Blocks.CRACKED_STONE_BRICKS);
			out.set(x, base + top, z, seat);
			if (r < 0.12) {
				out.set(x, base + top + 1, z, st(Blocks.SHORT_GRASS));
			}
			// The walls between the sections of seats.
			if (t >= 25.5 && t < 26.5 || t >= 37.5 && t < 38.5) {
				out.set(x, base + top + 1, z, st(Blocks.STONE_BRICKS));
			}
		}

		private void podium(Sink out, int x, int z, int base, double t) {
			for (int y = 1; y <= 3; y++) {
				out.set(x, base + y, z, brick(x, y, z));
			}
			out.set(x, base + 4, z, h3(x, 4, z, 21) < 0.3 ? st(Blocks.GRASS_BLOCK) : st(Blocks.SMOOTH_STONE));
			if (t >= 50) {
				out.set(x, base + 4, z, trav(x, 4, z));
				out.set(x, base + 5, z, trav(x, 5, z));
			}
		}

		private void arena(Sink out, int x, int z, int base, double u, double v) {
			// The arena floor is long gone; the hypogeum is filled with earth, its walls showing here and there.
			double av = Math.abs(v);
			if (Math.abs(u) < 38 && av < 20 && av % 4 < 1 && h3(x, 0, z, 22) < 0.35) {
				out.set(x, base + 1, z, h3(x, 1, z, 23) < 0.5 ? st(Blocks.MOSSY_STONE_BRICKS) : st(Blocks.STONE_BRICKS));
				return;
			}
			double r = h3(x, 1, z, 24);
			if (r < 0.15) {
				out.set(x, base + 1, z, st(Blocks.SHORT_GRASS));
			} else if (r < 0.16) {
				out.set(x, base + 1, z, st(Blocks.BUSH));
			}
		}
	}

	// =====================================================================================================================
	// The Theodosian land walls of Constantinople
	// =====================================================================================================================

	/**
	 * The triple land wall, along its real line from the Golden Horn to the Sea of Marmara. Across the line, outwards from
	 * the city (east = city): a road, the inner wall with its great towers, the inner terrace, the outer wall with smaller
	 * towers, the outer terrace, the breastwork and the dry moat. Five gates, the Golden Gate of marble near the south end.
	 * Every column follows the ground under the wall line at that point.
	 */
	private static final class Walls extends Lm {
		static final double V_IN = -8, V_OUT = 61.5;
		final double[] px, pz, cum, len;
		final double total;
		final double[] gates;
		final int golden;
		final double[] towers;
		final double[] outerTowers;

		static Walls create() {
			double[] lat = {41.03835, 41.02980, 41.02175, 41.01430, 41.00285, 40.99370};
			double[] lon = {28.94475, 28.93410, 28.92440, 28.92035, 28.91905, 28.92380};
			int n = lat.length;
			double[] px = new double[n], pz = new double[n];
			double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
			for (int i = 0; i < n; i++) {
				px[i] = Projection.x(lon[i]);
				pz[i] = Projection.z(lat[i]);
				minX = Math.min(minX, px[i]);
				maxX = Math.max(maxX, px[i]);
				minZ = Math.min(minZ, pz[i]);
				maxZ = Math.max(maxZ, pz[i]);
			}
			int cx = (int) Math.round((minX + maxX) / 2), cz = (int) Math.round((minZ + maxZ) / 2);
			int hx = (int) Math.ceil((maxX - minX) / 2) + 70, hz = (int) Math.ceil((maxZ - minZ) / 2) + 70;
			Mark m = new Mark("theodosian_walls", "Феодосиевы стены (Константинополь)", Projection.lat(cz + 0.5), Projection.lon(cx + 0.5), cx, cz, hx, hz);
			return new Walls(m, px, pz);
		}

		Walls(Mark m, double[] px, double[] pz) {
			super(m, true);
			this.px = px;
			this.pz = pz;
			int n = px.length;
			cum = new double[n];
			len = new double[n];
			for (int i = 0; i + 1 < n; i++) {
				len[i] = Math.max(1e-6, Math.hypot(px[i + 1] - px[i], pz[i + 1] - pz[i]));
				cum[i + 1] = cum[i] + len[i];
			}
			total = cum[n - 1];
			// Gates (north to south): Charisius, St Romanus, Rhegium, Pege, and the Golden Gate.
			double[][] g = {{41.02920, 28.93330}, {41.01880, 28.92280}, {41.00870, 28.91980}, {41.00460, 28.91920}, {40.99470, 28.92330}};
			gates = new double[g.length];
			double[] sv = new double[4];
			for (int i = 0; i < g.length; i++) {
				locate(Projection.x(g[i][1]), Projection.z(g[i][0]), sv, true);
				gates[i] = sv[0];
			}
			golden = g.length - 1;
			List<Double> tw = new ArrayList<>();
			List<Double> ot = new ArrayList<>();
			for (double s = 30; s < total - 20; s += 55) {
				if (nearGate(s) > 22) {
					tw.add(s);
				}
				double so = s + 27.5;
				if (so < total - 15 && nearGate(so) > 14) {
					ot.add(so);
				}
			}
			for (int i = 0; i < gates.length; i++) {
				if (i != golden) {
					tw.add(gates[i] - 9);
					tw.add(gates[i] + 9);
				}
			}
			towers = tw.stream().mapToDouble(Double::doubleValue).sorted().toArray();
			outerTowers = ot.stream().mapToDouble(Double::doubleValue).toArray();
			ground(Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.STONE_BRICKS, Blocks.DIRT);
		}

		private double nearGate(double s) {
			double best = Double.MAX_VALUE;
			for (int i = 0; i < gates.length; i++) {
				double d = Math.abs(s - gates[i]);
				if (i == golden) {
					d -= 10;
				}
				best = Math.min(best, d);
			}
			return best;
		}

		/**
		 * The nearest point of the wall line: out = {s (along, from the north end), v (offset outwards = west), outward
		 * normal x, z}. False beyond the two ends (unless {@code any}).
		 */
		boolean locate(double x, double z, double[] out, boolean any) {
			double best = Double.MAX_VALUE;
			boolean ok = false;
			int last = px.length - 2;
			for (int i = 0; i <= last; i++) {
				double ax = px[i], az = pz[i], dx = px[i + 1] - ax, dz = pz[i + 1] - az;
				double L = len[i];
				double tx = dx / L, tz = dz / L;
				double rx = x - ax, rz = z - az;
				double along = rx * tx + rz * tz;
				double east = rx * tz - rz * tx;
				double t = along / L;
				if (!any && (i == 0 && t < 0 || i == last && t > 1)) {
					continue;
				}
				double tc = Math.max(0, Math.min(1, t));
				double dist = Math.hypot(x - (ax + tc * dx), z - (az + tc * dz));
				if (dist < best) {
					best = dist;
					out[0] = cum[i] + tc * L;
					out[1] = east >= 0 ? -dist : dist;
					out[2] = -tz;
					out[3] = tx;
					ok = true;
				}
			}
			return ok;
		}

		@Override
		double padDist(int x, int z) {
			return 1e9; // not used: the walls level their own band
		}

		@Override
		void build(Sink out, int x0, int z0, int x1, int z1, int base) {
		}

		@Override
		boolean blocked(int x0, int z0, int x1, int z1, int margin) {
			double xc = (x0 + x1 + 1) / 2.0, zc = (z0 + z1 + 1) / 2.0;
			double hd = Math.hypot(x1 - x0 + 1, z1 - z0 + 1) / 2;
			double[] sv = new double[4];
			if (!locate(xc, zc, sv, true)) {
				return false;
			}
			return sv[1] + hd > V_IN - BLEND - margin && sv[1] - hd < V_OUT + BLEND + margin;
		}

		private double pointX(double s, int i) {
			return px[i] + (s - cum[i]) / len[i] * (px[i + 1] - px[i]);
		}

		private double pointZ(double s, int i) {
			return pz[i] + (s - cum[i]) / len[i] * (pz[i + 1] - pz[i]);
		}

		/** The ground (top block) under the wall line at s, or MIN_VALUE in water. */
		private int groundAt(double s, Map<Long, Integer> cache) {
			long k = Math.round(s * 4);
			Integer v = cache.get(k);
			if (v != null) {
				return v;
			}
			double ss = clamp(s, 0, total);
			int i = 0;
			while (i < px.length - 2 && ss > cum[i + 1]) {
				i++;
			}
			Terrain.Col c = Terrain.column((int) Math.floor(pointX(ss, i)), (int) Math.floor(pointZ(ss, i)));
			int g = c.waterTop != Terrain.NO_WATER ? Integer.MIN_VALUE : c.top();
			cache.put(k, g);
			return g;
		}

		private static double nearest(double[] a, double s) {
			double best = Double.MAX_VALUE;
			double at = 0;
			for (double t : a) {
				double d = Math.abs(s - t);
				if (d < best) {
					best = d;
					at = t;
				}
			}
			return at;
		}

		@Override
		void write(Sink out, int x0, int z0, int x1, int z1) {
			int ax0 = Math.max(x0, bx0), ax1 = Math.min(x1, bx1), az0 = Math.max(z0, bz0), az1 = Math.min(z1, bz1);
			if (ax0 > ax1 || az0 > az1) {
				return;
			}
			double[] sv = new double[4];
			// Most chunks in the box are far from the line: test the middle of the area first.
			double hd = Math.hypot(ax1 - ax0 + 1, az1 - az0 + 1) / 2 + 1;
			if (!locate((ax0 + ax1 + 1) / 2.0, (az0 + az1 + 1) / 2.0, sv, true) || sv[1] - hd > V_OUT + BLEND || sv[1] + hd < V_IN - BLEND) {
				return;
			}
			Map<Long, Integer> cache = new HashMap<>();
			for (int x = ax0; x <= ax1; x++) {
				for (int z = az0; z <= az1; z++) {
					if (!locate(x + 0.5, z + 0.5, sv, false)) {
						continue;
					}
					double s = sv[0], v = sv[1];
					if (v < V_IN - BLEND || v > V_OUT + BLEND) {
						continue;
					}
					Terrain.Col c = Terrain.column(x, z);
					if (c.waterTop != Terrain.NO_WATER) {
						continue;
					}
					double tc = nearest(towers, s);
					double key = Math.abs(s - tc) <= 6 && v < 10 ? tc : Math.round(s / 3.0) * 3.0;
					int gl = groundAt(key, cache);
					if (gl == Integer.MIN_VALUE) {
						continue;
					}
					column(out, x, z, s, v, sv[2], sv[3], c.top(), gl, tc);
				}
			}
		}

		private static BlockState stone(int x, int y, int z, int gl) {
			if (Math.floorMod(y - gl, 5) == 4) {
				return st(Blocks.BRICKS);
			}
			double r = h3(x, y, z, 31);
			return r < 0.06 ? st(Blocks.MOSSY_STONE_BRICKS) : r < 0.12 ? st(Blocks.CRACKED_STONE_BRICKS) : st(Blocks.STONE_BRICKS);
		}

		private static BlockState marble(int x, int y, int z) {
			return h3(x, y, z, 32) < 0.7 ? st(Blocks.CALCITE) : st(Blocks.QUARTZ_BLOCK);
		}

		private static boolean merlon(double s) {
			return Math.floorMod((int) Math.floor(s), 2) == 0;
		}

		private void column(Sink out, int x, int z, double s, double v, double nx, double nz, int g, int gl, double tc) {
			// Blend back into the land on both sides.
			if (v < V_IN || v >= V_OUT) {
				double dout = v < V_IN ? V_IN - v : v - V_OUT;
				double t = dout / (BLEND + 1.0);
				int target = (int) Math.round(gl + (g - gl) * t);
				for (int y = Math.max(g, target - MAX_FOUND); y < target; y++) {
					out.set(x, y, z, st(Blocks.DIRT));
				}
				if (target != g) {
					out.set(x, target, z, st(Blocks.GRASS_BLOCK));
				}
				for (int y = target + 1; y <= Math.max(g, target) + 14; y++) {
					out.set(x, y, z, AIR);
				}
				return;
			}
			int gi = -1;
			double ds = 1e9;
			for (int i = 0; i < gates.length; i++) {
				double d = s - gates[i];
				if (Math.abs(d) < Math.abs(ds)) {
					ds = d;
					gi = i;
				}
			}
			boolean isGolden = gi == golden && Math.abs(ds) < 26;
			double ads = Math.abs(ds);
			boolean road = ads < 2.2;
			boolean moat = v >= 41.5 && v < 61.5;
			// Level the band.
			int floor = moat ? gl - 8 : gl - 1;
			BlockState f = st(Blocks.DIRT);
			for (int y = Math.max(g + 1, gl - MAX_FOUND); y <= floor; y++) {
				out.set(x, y, z, f);
			}
			for (int y = Math.max(floor + 1, gl - 7); y <= Math.max(g, gl) + 14; y++) {
				out.set(x, y, z, AIR);
			}
			out.set(x, gl, z, road ? (h3(x, 0, z, 33) < 0.3 ? st(Blocks.GRAVEL) : st(Blocks.COBBLESTONE)) : st(Blocks.GRASS_BLOCK));
			if (v < -2.5 && !road) {
				out.set(x, gl, z, h3(x, 0, z, 34) < 0.3 ? st(Blocks.GRAVEL) : st(Blocks.DIRT_PATH));
			}

			if (isGolden && goldenGate(out, x, z, ds, v, gl)) {
				return;
			}
			double ao = s - tc;
			int tk = (int) Math.round(tc / 55.0);
			// Inner towers.
			boolean octag = Math.floorMod(tk, 3) == 1;
			boolean inTower = Math.abs(ao) <= 5.5 && v >= -2.5 && v < 9 && !(octag && Math.abs(ao) - 3.5 + (v - 6) > 2) && !isGolden;
			if (inTower) {
				innerTower(out, x, z, ao, v, nx, nz, gl);
				return;
			}
			if (v < -2.5) {
				return;
			}
			if (v < 2.5) {
				// The inner wall.
				boolean pass = gi >= 0 && gi != golden && ads < 3.0;
				for (int y = gl + 1; y <= gl + 12; y++) {
					boolean air = pass && (y <= gl + 7 || y == gl + 8 && ads < 2.2 || y == gl + 9 && ads < 1.2);
					out.set(x, y, z, air ? AIR : stone(x, y, z, gl));
				}
				if (v >= 1.5) {
					out.set(x, gl + 13, z, stone(x, gl + 13, z, gl));
					if (merlon(s)) {
						out.set(x, gl + 14, z, stone(x, gl + 14, z, gl));
					}
				} else if (v < -1.5) {
					out.set(x, gl + 13, z, Bp.slab(Blocks.STONE_BRICK_SLAB, false));
				}
				if (pass) {
					out.set(x, gl, z, st(Blocks.COBBLESTONE));
				}
				return;
			}
			// Outer towers.
			double to = nearest(outerTowers, s);
			double oo = s - to;
			int ok = (int) Math.round(to / 55.0);
			boolean round = Math.floorMod(ok, 2) == 1;
			if (Math.abs(oo) <= 4 && v >= 23 && v < 30 && !(round && v > 26 && Math.hypot(oo, v - 26) > 4) && !isGolden) {
				boolean hollow = Math.abs(oo) < 2.5 && v >= 24 && v < 28.5;
				for (int y = gl + 1; y <= gl + 12; y++) {
					boolean air = hollow && y < gl + 12 || v < 24 && Math.abs(oo) < 1 && y <= gl + 2;
					out.set(x, y, z, air ? AIR : stone(x, y, z, gl));
				}
				boolean rim = Math.abs(oo) > 3 || v >= 29 || round && v > 26 && Math.hypot(oo, v - 26) > 3;
				if (rim && merlon(s + v)) {
					out.set(x, gl + 13, z, stone(x, gl + 13, z, gl));
				}
				return;
			}
			if (v >= 23 && v < 25.5) {
				// The outer wall.
				boolean pass = gi >= 0 && ads < 2.5;
				for (int y = gl + 1; y <= gl + 8; y++) {
					boolean air = pass && (y <= gl + 5 || y == gl + 6 && ads < 1.5);
					BlockState sb = isGolden && ads < 10 ? marble(x, y, z) : stone(x, y, z, gl);
					out.set(x, y, z, air ? AIR : sb);
				}
				if (v >= 24.5 && merlon(s)) {
					out.set(x, gl + 9, z, stone(x, gl + 9, z, gl));
				}
				return;
			}
			if (v >= 40 && v < 41.5) {
				// The breastwork on the inner edge of the moat.
				if (gi >= 0 && ads < 3) {
					return;
				}
				for (int y = gl + 1; y <= gl + 2; y++) {
					out.set(x, y, z, stone(x, y, z, gl));
				}
				if (merlon(s)) {
					out.set(x, gl + 3, z, stone(x, gl + 3, z, gl));
				}
				return;
			}
			if (moat) {
				boolean causeway = gi >= 0 && ads < 3.5;
				boolean dam = Math.floorMod((int) Math.floor(s), 120) < 2;
				boolean side = v < 42.5 || v >= 60.5;
				if (causeway || dam) {
					for (int y = gl - 7; y < gl; y++) {
						out.set(x, y, z, stone(x, y, z, gl));
					}
					out.set(x, gl, z, causeway ? st(Blocks.COBBLESTONE) : stone(x, gl, z, gl));
					return;
				}
				if (side) {
					for (int y = gl - 7; y <= gl; y++) {
						out.set(x, y, z, stone(x, y, z, gl));
					}
					return;
				}
				out.set(x, gl - 7, z, h3(x, 0, z, 35) < 0.4 ? st(Blocks.COARSE_DIRT) : st(Blocks.GRASS_BLOCK));
				for (int y = gl - 6; y <= gl; y++) {
					out.set(x, y, z, AIR);
				}
				if (h3(x, 1, z, 36) < 0.15) {
					out.set(x, gl - 6, z, st(Blocks.SHORT_GRASS));
				}
			}
		}

		private void innerTower(Sink out, int x, int z, double ao, double v, double nx, double nz, int gl) {
			boolean hollow = Math.abs(ao) < 4 && v >= -1.5 && v < 7.5;
			boolean ladder = Math.abs(ao) < 0.5 && v >= 6.5 && v < 7.5;
			for (int y = gl + 1; y <= gl + 20; y++) {
				BlockState s;
				if (ladder) {
					s = Bp.facing(Blocks.LADDER, cardinal(-nx, -nz));
				} else if (hollow && y != gl + 12 && y != gl + 20) {
					s = AIR;
				} else if (y >= gl + 13 && y <= gl + 14 && Math.abs(ao) >= 4 && v >= -1.5 && v < 1.5) {
					s = AIR; // doors onto the wall-walk
				} else if (v < -1.5 && Math.abs(ao) < 1 && y <= gl + 2) {
					s = AIR; // the door from the city
				} else if (v >= 8 && Math.abs(ao) < 0.6 && (y == gl + 5 || y == gl + 6 || y == gl + 15 || y == gl + 16)) {
					s = AIR; // arrow slits
				} else {
					s = stone(x, y, z, gl);
				}
				out.set(x, y, z, s);
			}
			boolean rim = Math.abs(ao) > 4.5 || v < -1.5 || v >= 8;
			if (rim && Math.floorMod(x + z, 2) == 0) {
				out.set(x, gl + 21, z, stone(x, gl + 21, z, gl));
			}
		}

		/** The Golden Gate: a marble triumphal arch with three openings between two great marble towers. */
		private boolean goldenGate(Sink out, int x, int z, double ds, double v, int gl) {
			double a = Math.abs(ds);
			if (a <= 24 && a >= 12 && v >= -4 && v < 12) {
				for (int y = gl + 1; y <= gl + 24; y++) {
					out.set(x, y, z, y % 6 == 0 ? st(Blocks.CHISELED_QUARTZ_BLOCK) : marble(x, y, z));
				}
				boolean rim = a > 23 || a < 13 || v < -3 || v >= 11;
				if (rim && Math.floorMod(x + z, 2) == 0) {
					out.set(x, gl + 25, z, marble(x, gl + 25, z));
				}
				return true;
			}
			if (a < 12 && v >= -2.5 && v < 2.5) {
				for (int y = gl + 1; y <= gl + 16; y++) {
					int h = y - gl;
					boolean air = a < 3.5 && h <= 10 || a < 2.6 && h == 11 || a < 1.4 && h == 12
							|| a >= 6 && a <= 9 && h <= 7 || a >= 6.5 && a <= 8.5 && h == 8;
					out.set(x, y, z, air ? AIR : h == 16 ? st(Blocks.CHISELED_QUARTZ_BLOCK) : marble(x, y, z));
				}
				if (a < 1.5) {
					out.set(x, gl + 17, z, st(Blocks.GOLD_BLOCK));
				}
				if (a < 3.5) {
					out.set(x, gl, z, st(Blocks.QUARTZ_BLOCK));
				}
				return true;
			}
			return false;
		}
	}

	// =====================================================================================================================
	// The Hippodrome of Constantinople
	// =====================================================================================================================

	/**
	 * The Hippodrome, 450 × 130 m, its long axis running north-east (the starting gates, carceres, with the gilded quadriga)
	 * to south-west (the curved sphendone). Tiers of marble seats, the Kathisma (the emperor's box) on the east side, the
	 * spina in the middle with the obelisks, the Serpent Column, statues and a water channel.
	 */
	private static final class Hippodrome extends Columns {
		static final double L = 450, W = 65;
		final double cx, cz, ax, az, nx, nz;
		final int[][] mons; // {world x, world z, kind}

		Hippodrome(Mark m) {
			super(m, true);
			cx = m.x() + 0.5;
			cz = m.z() + 0.5;
			double x0 = Projection.x(28.97660), z0 = Projection.z(41.00760), x1 = Projection.x(28.97380), z1 = Projection.z(41.00380);
			double l = Math.hypot(x1 - x0, z1 - z0);
			ax = (x1 - x0) / l;
			az = (z1 - z0) / l;
			nx = az;
			nz = -ax;
			double[][] list = {
					{180, 0, 0}, // Obelisk of Theodosius
					{215, 0, 1}, // Serpent Column
					{262, 0, 2}, // Walled Obelisk
					{100, 0, 3}, {128, 0, 4}, {150, 0, 3}, {240, 0, 4}, {292, 0, 3}, {316, 0, 4}, // statues
					{72, -1.6, 5}, {72, 0, 5}, {72, 1.6, 5}, {338, -1.6, 5}, {338, 0, 5}, {338, 1.6, 5}, // turning posts
			};
			mons = new int[list.length][];
			for (int i = 0; i < list.length; i++) {
				double a = list[i][0] - L / 2, b = list[i][1];
				mons[i] = new int[]{(int) Math.floor(cx + a * ax + b * nx), (int) Math.floor(cz + a * az + b * nz), (int) list[i][2]};
			}
			ground(Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.STONE_BRICKS, Blocks.DIRT);
			clearH = 18;
		}

		private double la(double x, double z) {
			return (x - cx) * ax + (z - cz) * az + L / 2;
		}

		private double lb(double x, double z) {
			return (x - cx) * nx + (z - cz) * nz;
		}

		/** Depth inside the outline (0 at the outer wall, negative outside). */
		private static double depth(double a, double b) {
			if (a <= L - W) {
				return a < 0 ? Math.min(W - Math.abs(b), a) : W - Math.abs(b);
			}
			return W - Math.hypot(a - (L - W), b);
		}

		private static boolean kathisma(double a, double b) {
			return a >= 186 && a <= 214 && b >= 36 && b <= 74;
		}

		@Override
		double padDist(int x, int z) {
			double a = la(x + 0.5, z + 0.5), b = lb(x + 0.5, z + 0.5);
			double k = Math.max(Math.max(186 - a, a - 214), Math.max(36 - b, b - 74));
			return Math.min(-depth(a, b), k) - 3;
		}

		@Override
		boolean blocked(int x0, int z0, int x1, int z1, int margin) {
			double xc = (x0 + x1 + 1) / 2.0, zc = (z0 + z1 + 1) / 2.0;
			double hd = Math.hypot(x1 - x0 + 1, z1 - z0 + 1) / 2 + margin + BLEND + 3;
			double a = la(xc, zc), b = lb(xc, zc);
			return a > -hd && a < L + hd && Math.abs(b) < W + 9 + hd;
		}

		private static BlockState wall(int x, int y, int z) {
			return Math.floorMod(y, 5) == 0 ? st(Blocks.STONE_BRICKS) : st(Blocks.BRICKS);
		}

		@Override
		void column(Sink out, int x, int z, int base) {
			double a = la(x + 0.5, z + 0.5), b = lb(x + 0.5, z + 0.5);
			if (kathisma(a, b)) {
				kathismaColumn(out, x, z, base, a, b);
				return;
			}
			double s = depth(a, b);
			if (s < 0) {
				return;
			}
			if (a < 14) {
				carceres(out, x, z, base, a, b);
				return;
			}
			if (s < 2) {
				// The outer wall with an arcade at the ground and blind arches above.
				double per = a <= L - W ? a : (L - W) + Math.atan2(a - (L - W), Math.abs(b)) * W;
				double pb = per / 7.0;
				double q = (pb - Math.floor(pb) - 0.5) * 7;
				double aq = Math.abs(q);
				for (int y = 1; y <= 20; y++) {
					boolean open = y <= 6 && aq <= 2 || y == 7 && aq <= 1.3;
					boolean blind = y >= 11 && y <= 15 && aq <= 1.5 || y == 16 && aq <= 0.8;
					out.set(x, base + y, z, open ? AIR : blind ? st(Blocks.TUFF_BRICKS) : wall(x, y, z));
				}
				if (s < 1 && merlonAt(x, z)) {
					out.set(x, base + 21, z, wall(x, 21, z));
				}
				return;
			}
			if (s < 6) {
				// The vaulted corridor behind the arcade, and the top promenade above it.
				out.set(x, base, z, st(Blocks.STONE_BRICKS));
				for (int y = 8; y <= 18; y++) {
					out.set(x, base + y, z, y == 18 ? st(Blocks.SMOOTH_STONE) : st(Blocks.BRICKS));
				}
				return;
			}
			if (s < 22) {
				// Tiers of marble seats.
				int top = (int) Math.floor(17 - (s - 6) * 13.0 / 16.0);
				for (int y = 1; y < top; y++) {
					out.set(x, base + y, z, st(Blocks.BRICKS));
				}
				out.set(x, base + top, z, top % 2 == 0 ? st(Blocks.POLISHED_DIORITE) : st(Blocks.SMOOTH_STONE));
				return;
			}
			if (s < 25) {
				// The podium around the track; steps down to the track in a few places.
				boolean steps = Math.abs(a - 120) < 1.5 || Math.abs(a - 260) < 1.5;
				int top = steps ? Math.max(1, 3 - (int) Math.floor(s - 22)) : 3;
				for (int y = 1; y <= top; y++) {
					out.set(x, base + y, z, y == top ? st(Blocks.POLISHED_ANDESITE) : st(Blocks.BRICKS));
				}
				if (!steps && s >= 24.3) {
					out.set(x, base + 4, z, st(Blocks.CALCITE));
				}
				return;
			}
			// The track and the spina.
			out.set(x, base, z, st(Blocks.SAND));
			double ab = Math.abs(b);
			if (ab <= 3 && a >= 70 && a <= 340) {
				out.set(x, base + 1, z, st(Blocks.CALCITE));
				boolean water = ab <= 1 && a >= 72 && a <= 338;
				out.set(x, base + 2, z, water ? st(Blocks.WATER) : st(Blocks.POLISHED_DIORITE));
			}
			for (int[] m : mons) {
				int ix = x - m[0], iz = z - m[1];
				if (Math.abs(ix) <= 3 && Math.abs(iz) <= 3) {
					monument(out, x, z, base, m[2], ix, iz);
				}
			}
		}

		private static boolean merlonAt(int x, int z) {
			return Math.floorMod(x + z, 2) == 0;
		}

		private void monument(Sink out, int x, int z, int base, int kind, int ix, int iz) {
			int ax = Math.abs(ix), az = Math.abs(iz);
			switch (kind) {
				case 0 -> { // Obelisk of Theodosius on its marble base
					if (ax <= 2 && az <= 2) {
						for (int y = 1; y <= 5; y++) {
							out.set(x, base + y, z, st(Blocks.CALCITE));
						}
						out.set(x, base + 6, z, st(Blocks.CHISELED_QUARTZ_BLOCK));
					}
					if (ax <= 1 && az <= 1) {
						if (ax + az != 1) {
							out.set(x, base + 7, z, Blocks.DYED_TERRACOTTA.pick(DyeColor.GREEN).defaultBlockState());
						}
						for (int y = 8; y <= 23; y++) {
							out.set(x, base + y, z, ax + az == 1 && y % 3 == 0 ? st(Blocks.GRANITE) : st(Blocks.POLISHED_GRANITE));
						}
						if (ax + az == 0) {
							out.set(x, base + 24, z, st(Blocks.GRANITE));
							out.set(x, base + 25, z, Bp.slab(Blocks.GRANITE_SLAB, false));
						} else if (ax + az == 1) {
							Direction f = ix > 0 ? Direction.WEST : ix < 0 ? Direction.EAST : iz > 0 ? Direction.NORTH : Direction.SOUTH;
							out.set(x, base + 24, z, stairs(Blocks.GRANITE_STAIRS, f));
						}
					}
				}
				case 1 -> { // Serpent Column (bronze) from Delphi
					if (ax <= 1 && az <= 1) {
						for (int y = 1; y <= 4; y++) {
							out.set(x, base + y, z, st(Blocks.POLISHED_ANDESITE));
						}
					}
					BlockState bronze = Blocks.DYED_TERRACOTTA.pick(DyeColor.GREEN).defaultBlockState();
					if (ax == 0 && az == 0) {
						for (int y = 5; y <= 12; y++) {
							out.set(x, base + y, z, bronze);
						}
						out.set(x, base + 13, z, st(Blocks.GOLD_BLOCK));
					}
					if (ix == 1 && iz == 0 || ix == -1 && iz == 0 || ix == 0 && iz == 1) {
						out.set(x, base + 12, z, bronze);
					}
				}
				case 2 -> { // Walled Obelisk of rough stone with remains of gilded plates
					if (ax <= 2 && az <= 2) {
						for (int y = 1; y <= 5; y++) {
							out.set(x, base + y, z, st(Blocks.STONE_BRICKS));
						}
					}
					if (ax <= 1 && az <= 1) {
						for (int y = 6; y <= 29; y++) {
							double r = h3(x, y, z, 41);
							out.set(x, base + y, z, r < 0.06 ? st(Blocks.GOLD_BLOCK) : r < 0.45 ? st(Blocks.COBBLESTONE) : r < 0.7 ? st(Blocks.STONE)
									: r < 0.85 ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.ANDESITE));
						}
					}
					if (ax == 0 && az == 0) {
						for (int y = 30; y <= 32; y++) {
							out.set(x, base + y, z, st(Blocks.STONE));
						}
						out.set(x, base + 33, z, Bp.slab(Blocks.STONE_SLAB, false));
					}
				}
				case 3, 4 -> { // statues on pedestals: bronze, or gilded
					if (ax == 0 && az == 0) {
						for (int y = 1; y <= 4; y++) {
							out.set(x, base + y, z, st(Blocks.CALCITE));
						}
						BlockState fig = kind == 4 ? st(Blocks.GOLD_BLOCK) : Blocks.DYED_TERRACOTTA.pick(DyeColor.GREEN).defaultBlockState();
						for (int y = 5; y <= 7; y++) {
							out.set(x, base + y, z, fig);
						}
					}
				}
				default -> { // turning posts (metae): marble cones with gilded tops
					if (ax == 0 && az == 0) {
						for (int y = 1; y <= 6; y++) {
							out.set(x, base + y, z, st(Blocks.CALCITE));
						}
						out.set(x, base + 7, z, st(Blocks.GOLD_BLOCK));
					}
				}
			}
		}

		private void carceres(Sink out, int x, int z, int base, double a, double b) {
			double ab = Math.abs(b);
			boolean tower = ab <= 7;
			boolean endTower = ab >= W - 9;
			int h = tower ? 24 : endTower ? 20 : 14;
			boolean shell = a < 2 || a >= 12 || ab >= W - 2 || tower && ab >= 5.5 || endTower && ab <= W - 7.5;
			out.set(x, base, z, st(Blocks.STONE_BRICKS));
			for (int y = 1; y <= h; y++) {
				BlockState s;
				if (shell) {
					boolean open = false;
					if (a >= 12 && ab <= 38 && y <= 6) {
						double gb = (b + 39) - Math.floor((b + 39) / 6.5) * 6.5;
						open = y <= 5 && gb >= 1.3 && gb <= 5.7 || y == 6 && gb >= 2 && gb <= 5;
					}
					if (a < 2 && y <= 7 && (ab <= 3 || Math.abs(ab - 25) <= 2)) {
						open = true;
					}
					s = open ? AIR : a >= 12 && y <= 14 ? (Math.floorMod(y, 5) == 0 ? st(Blocks.STONE_BRICKS) : st(Blocks.CALCITE)) : wall(x, y, z);
				} else {
					s = y == 7 || y == h ? st(Blocks.SMOOTH_STONE) : AIR;
				}
				out.set(x, base + y, z, s);
			}
			if (shell && merlonAt(x, z)) {
				out.set(x, base + h + 1, z, wall(x, h + 1, z));
			}
			if (tower) {
				// The gilded quadriga on the tower over the starting gates.
				for (double hb : new double[]{-3, -1, 1, 3}) {
					if (Math.abs(b - hb) < 0.6) {
						if (a >= 5.5 && a < 6.5 || a >= 7.5 && a < 8.5) {
							out.set(x, base + h + 1, z, st(Blocks.GOLD_BLOCK));
						}
						if (a >= 5.5 && a < 8.5) {
							out.set(x, base + h + 2, z, st(Blocks.GOLD_BLOCK));
						}
						if (a >= 8.5 && a < 9.5) {
							out.set(x, base + h + 2, z, st(Blocks.GOLD_BLOCK));
							out.set(x, base + h + 3, z, st(Blocks.GOLD_BLOCK));
						}
					}
				}
				if (a >= 3 && a < 5 && ab <= 3) {
					out.set(x, base + h + 1, z, st(Blocks.GOLD_BLOCK));
					out.set(x, base + h + 2, z, st(Blocks.GOLD_BLOCK));
				}
			}
		}

		/** The Kathisma: the emperor's palace box on the east side, joined to the Great Palace, with a loggia over the track. */
		private void kathismaColumn(Sink out, int x, int z, int base, double a, double b) {
			out.set(x, base, z, st(Blocks.POLISHED_ANDESITE));
			if (b < 40) {
				// The loggia: a marble base, the box floor at 9, columns and a roof.
				for (int y = 1; y <= 8; y++) {
					out.set(x, base + y, z, y % 4 == 0 ? st(Blocks.POLISHED_GRANITE) : st(Blocks.CALCITE));
				}
				out.set(x, base + 9, z, st(Blocks.POLISHED_DIORITE));
				double ca = (a - 186) - Math.floor((a - 186) / 4) * 4;
				boolean col = Math.abs(ca - 2) < 0.6 && b < 37.5;
				for (int y = 10; y <= 16; y++) {
					out.set(x, base + y, z, col ? Bp.log(Blocks.QUARTZ_PILLAR, Direction.Axis.Y) : y == 10 && b < 37 ? st(Blocks.CALCITE) : AIR);
				}
				if (b < 37 && !col) {
					out.set(x, base + 10, z, Bp.slab(Blocks.QUARTZ_SLAB, false));
				}
				out.set(x, base + 17, z, st(Blocks.CALCITE));
				return;
			}
			boolean shell = a < 188 || a > 212 || b < 42 || b > 72;
			for (int y = 1; y <= 22; y++) {
				BlockState s;
				if (shell) {
					boolean window = (y >= 13 && y <= 15 || y >= 3 && y <= 5) && Math.floorMod((int) Math.floor(a + b), 4) == 0 && b >= 42;
					boolean box = b < 42 && y >= 10 && y <= 14 && a >= 190 && a <= 210;
					s = window || box ? AIR : Math.floorMod(y, 6) == 0 ? st(Blocks.CALCITE) : st(Blocks.BRICKS);
				} else {
					s = y == 9 || y == 22 ? st(Blocks.POLISHED_DIORITE) : y == 1 && h3(x, 1, z, 42) < 0.1 ? st(Blocks.CARPET.pick(DyeColor.PURPLE)) : AIR;
				}
				out.set(x, base + y, z, s);
			}
			if (!shell && b >= 50 && b <= 64 && (int) Math.floor(a) % 6 == 0 && (int) Math.floor(b) % 6 == 0) {
				out.set(x, base + 21, z, Bp.lantern(true));
				out.set(x, base + 8, z, Bp.lantern(true));
			}
			// A lead dome over the middle.
			double dc = Math.hypot(a - 200, b - 57);
			if (dc <= 7.5) {
				for (int y = 23; y <= 30; y++) {
					double d3 = Math.sqrt(dc * dc + (y - 23) * (y - 23) * 1.0);
					if (d3 <= 7.5 && d3 > 6.2) {
						out.set(x, base + y, z, st(Blocks.SMOOTH_STONE));
					}
				}
				if (dc < 0.8) {
					out.set(x, base + 31, z, st(Blocks.GOLD_BLOCK));
				}
			}
		}
	}
}
