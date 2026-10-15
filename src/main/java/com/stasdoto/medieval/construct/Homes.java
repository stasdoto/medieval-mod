package com.stasdoto.medieval.construct;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.stasdoto.medieval.history.Culture;
import com.stasdoto.medieval.construct.Interior.Use;
import com.stasdoto.medieval.construct.Interior.Wood;

/**
 * Dwellings of every people, many kinds each, with real rooms: village yards (house, outbuildings, garden, fence and gate)
 * and town houses (on the street front, a yard behind). Every plot is drawn facing the street at +z.
 */
final class Homes {
	private Homes() {
	}

	/** A village yard (plot ~17×23, varies 12..24 × 16..32): house(s), outbuildings, garden, fence, gate to the street (+z). */
	static void village(Bp b, Palette p, Culture c) {
		if (b == null || p == null || c == null || b.w < 6 || b.d < 6) {
			return;
		}
		int wealth = b.r.nextInt(10) < 6 ? 0 : b.r.nextInt(10) < 8 ? 1 : 2;
		K k = new K(b, p, c, wealth, false);
		try {
			Yards.village(k);
		} catch (RuntimeException e) {
			// Never let one yard cost the chunk.
		}
		try {
			finish(k);
		} catch (RuntimeException e) {
			// as above
		}
	}

	/**
	 * A town house plot (w 7..17, d 9..19). wealth 0 poor, 1 common, 2 rich. The house fills the street front of the
	 * plot; a yard or garden lies behind.
	 */
	static void town(Bp b, Palette p, Culture c, int wealth) {
		if (b == null || p == null || c == null || b.w < 5 || b.d < 6) {
			return;
		}
		K k = new K(b, p, c, Math.max(0, Math.min(2, wealth)), true);
		try {
			Yards.town(k);
		} catch (RuntimeException e) {
			// Never let one house cost the chunk.
		}
		try {
			finish(k);
		} catch (RuntimeException e) {
			// as above
		}
	}

	// =================================================================================================================
	// The plot being drawn

	static final class K {
		final Bp b;
		final Palette p;
		final Culture c;
		final Random r;
		final int wealth;
		final boolean town;
		final int W, D;
		/** Joinery wood of this house. */
		final Wood w;
		/** Cells of the yard already taken by buildings and props. */
		final boolean[][] used;
		final List<int[]> doors = new ArrayList<>();

		K(Bp b, Palette p, Culture c, int wealth, boolean town) {
			this.b = b;
			this.p = p;
			this.c = c;
			this.r = b.r;
			this.wealth = wealth;
			this.town = town;
			this.W = b.w;
			this.D = b.d;
			this.w = Interior.woodFor(c, b.r);
			this.used = new boolean[W][D];
		}

		boolean in(int x, int z) {
			return x >= 0 && z >= 0 && x < W && z < D;
		}

		BlockState st(Block bl) {
			return bl.defaultBlockState();
		}

		/** Sets a block only inside the plot. */
		void setIn(int x, int y, int z, BlockState s) {
			if (in(x, z)) {
				b.set(x, y, z, s);
			}
		}

		/** Sets a block only inside the plot and only where nothing is yet. */
		void soft(int x, int y, int z, BlockState s) {
			if (in(x, z) && empty(x, y, z)) {
				b.set(x, y, z, s);
			}
		}

		boolean empty(int x, int y, int z) {
			BlockState s = b.get(x, y, z);
			return s == null || s.isAir();
		}

		void mark(int x0, int z0, int x1, int z1) {
			for (int x = Math.max(0, Math.min(x0, x1)); x <= Math.min(W - 1, Math.max(x0, x1)); x++) {
				for (int z = Math.max(0, Math.min(z0, z1)); z <= Math.min(D - 1, Math.max(z0, z1)); z++) {
					used[x][z] = true;
				}
			}
		}

		boolean free(int x0, int z0, int x1, int z1) {
			if (x0 > x1 || z0 > z1 || x0 < 1 || z0 < 1 || x1 > W - 2 || z1 > D - 2) {
				return false;
			}
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					if (used[x][z]) {
						return false;
					}
				}
			}
			return true;
		}

		/** Finds a free w×d rectangle (with a margin of 1 free cell) in the region; null if none. */
		int[] spot(int w, int d, int rx0, int rz0, int rx1, int rz1) {
			List<int[]> c = new ArrayList<>();
			for (int x = rx0; x + w - 1 <= rx1; x++) {
				for (int z = rz0; z + d - 1 <= rz1; z++) {
					if (free(x, z, x + w - 1, z + d - 1) && free(Math.max(1, x - 1), Math.max(1, z - 1), Math.min(W - 2, x + w), Math.min(D - 2, z + d))) {
						c.add(new int[]{x, z});
					}
				}
			}
			return c.isEmpty() ? null : c.get(r.nextInt(c.size()));
		}

		int pick(int... v) {
			return v[r.nextInt(v.length)];
		}

		boolean chance(int n) {
			return r.nextInt(n) == 0;
		}
	}

	static final Direction[] HORIZ = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
	static final BlockState AIR = Blocks.AIR.defaultBlockState();

	static BlockState s(Block b) {
		return b.defaultBlockState();
	}

	static BlockState log(Block b, Direction.Axis a) {
		return Bp.log(b, a);
	}

	static boolean isDoor(BlockState s) {
		return s != null && s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && s.hasProperty(BlockStateProperties.DOOR_HINGE);
	}

	// =================================================================================================================
	// Walls

	/** A wall recipe: posts every {@code step} cells (0 = corners only, no posts at all if post is null). */
	record Wall(BlockState fill, BlockState fill2, int chance2, Block post, int step, BlockState base, Block plate) {
	}

	static Wall plain(BlockState fill) {
		return new Wall(fill, null, 0, null, 0, null, null);
	}

	/** Hollow walls x0..x1 × z0..z1, rows ya..yb. */
	static void box(K k, int x0, int z0, int x1, int z1, int ya, int yb, Wall w) {
		for (int y = ya; y <= yb; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					if (x != x0 && x != x1 && z != z0 && z != z1) {
						continue;
					}
					boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
					int along = (z == z0 || z == z1) ? x - x0 : z - z0;
					boolean xWall = z == z0 || z == z1;
					BlockState s;
					if (w.post != null && (corner || (w.step > 0 && along % w.step == 0))) {
						s = log(w.post, Direction.Axis.Y);
					} else if (y == ya && w.base != null) {
						s = w.base;
					} else if (y == yb && w.plate != null) {
						s = log(w.plate, xWall ? Direction.Axis.X : Direction.Axis.Z);
					} else if (w.fill2 != null && w.chance2 > 0 && k.r.nextInt(w.chance2) == 0) {
						s = w.fill2;
					} else {
						s = w.fill;
					}
					k.b.set(x, y, z, s);
				}
			}
		}
	}

	/** Log walls (сруб) with the ends crossing at the corners; {@code ends}: log ends stick out by one ("в обло"). */
	static void logWalls(K k, Block log, int x0, int z0, int x1, int z1, int ya, int yb, boolean ends) {
		for (int y = ya; y <= yb; y++) {
			for (int x = x0; x <= x1; x++) {
				k.b.set(x, y, z0, log(log, Direction.Axis.X));
				k.b.set(x, y, z1, log(log, Direction.Axis.X));
			}
			for (int z = z0 + 1; z < z1; z++) {
				k.b.set(x0, y, z, log(log, Direction.Axis.Z));
				k.b.set(x1, y, z, log(log, Direction.Axis.Z));
			}
			boolean even = (y - ya) % 2 == 0;
			Direction.Axis ax = even ? Direction.Axis.X : Direction.Axis.Z;
			for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
				k.b.set(c[0], y, c[1], log(log, ax));
			}
			if (ends) {
				if (even) {
					k.soft(x0 - 1, y, z0, log(log, Direction.Axis.X));
					k.soft(x1 + 1, y, z0, log(log, Direction.Axis.X));
					k.soft(x0 - 1, y, z1, log(log, Direction.Axis.X));
					k.soft(x1 + 1, y, z1, log(log, Direction.Axis.X));
				} else {
					k.soft(x0, y, z0 - 1, log(log, Direction.Axis.Z));
					k.soft(x1, y, z0 - 1, log(log, Direction.Axis.Z));
					k.soft(x0, y, z1 + 1, log(log, Direction.Axis.Z));
					k.soft(x1, y, z1 + 1, log(log, Direction.Axis.Z));
				}
			}
		}
	}

	/** An inner wall along X at z (x0..x1 are the outer walls) or along Z at x, rows ya..yb, with a door at {@code at}. */
	static void partition(K k, boolean alongX, int pos, int a0, int a1, int ya, int yb, BlockState s, Block door, int at) {
		for (int y = ya; y <= yb; y++) {
			for (int i = a0 + 1; i < a1; i++) {
				if (alongX) {
					k.b.set(i, y, pos, s);
				} else {
					k.b.set(pos, y, i, s);
				}
			}
		}
		if (door != null && at > a0 && at < a1) {
			if (alongX) {
				k.b.door(door, at, ya + 1, pos, Direction.SOUTH);
			} else {
				k.b.door(door, pos, ya + 1, at, Direction.EAST);
			}
		}
	}

	static void found(K k, int x0, int z0, int x1, int z1, BlockState s) {
		k.b.fill(x0, -3, z0, x1, -1, z1, s);
	}

	static void floor(K k, int x0, int z0, int x1, int z1, int y, BlockState s) {
		k.b.fill(x0, y, z0, x1, y, z1, s);
	}

	/** Clears the inside of a box (rows ya..yb) so nothing of the ground or earlier drawing is left in the rooms. */
	static void hollow(K k, int x0, int z0, int x1, int z1, int ya, int yb) {
		for (int y = ya; y <= yb; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					k.b.set(x, y, z, AIR);
				}
			}
		}
	}

	/** A door opening towards {@code out}; remembered so that its outside is kept free. */
	static void door(K k, Block d, int x, int y, int z, Direction out) {
		k.b.door(d, x, y, z, out);
		k.doors.add(new int[]{x, y, z, out.getStepX(), out.getStepZ()});
	}

	// ---- windows

	static final int OPEN = 0, GLASS = 1, BARS = 2, NALICH = 3, LATTICE = 4, SLIT = 5, ARCH = 6;

	/** One window in the wall cell (x, y, z) looking out towards {@code out}. */
	static void window(K k, int x, int y, int z, Direction out, int kind, Block trap) {
		BlockState cur = k.b.get(x, y, z);
		if (cur == null || isDoor(cur)) {
			return;
		}
		Direction t = Interior.cw(out);
		int ox = x + out.getStepX(), oz = z + out.getStepZ();
		switch (kind) {
			case GLASS -> k.b.set(x, y, z, s(Blocks.GLASS_PANE));
			case BARS -> k.b.set(x, y, z, s(Blocks.IRON_BARS));
			case LATTICE -> k.b.set(x, y, z, Bp.trapdoor(trap, out, false, true));
			case NALICH -> k.b.set(x, y, z, k.wealth >= 1 ? s(Blocks.GLASS_PANE) : AIR);
			case ARCH -> {
				k.b.set(x, y, z, k.wealth >= 1 ? s(Blocks.GLASS_PANE) : AIR);
				BlockState up = k.b.get(x, y + 1, z);
				if (up != null && !up.isAir()) {
					k.b.set(x, y + 1, z, k.wealth >= 1 ? s(Blocks.GLASS_PANE) : AIR);
				}
			}
			default -> k.b.set(x, y, z, AIR);
		}
		if (kind == OPEN || kind == GLASS || kind == NALICH) {
			// Shutters folded back against the wall on both sides.
			BlockState sh = Bp.trapdoor(trap, out, false, true);
			k.soft(ox + t.getStepX(), y, oz + t.getStepZ(), sh);
			k.soft(ox - t.getStepX(), y, oz - t.getStepZ(), sh);
		}
		if (kind == NALICH) {
			// Carved frame: a hood above and a sill below.
			k.soft(ox, y + 1, oz, Bp.trapdoor(trap, out, false, false));
			k.soft(ox, y - 1, oz, Bp.trapdoor(trap, out, true, false));
		}
	}

	/** Windows along all walls of a box, every {@code step} cells, avoiding corners, doors and posts. */
	static void windows(K k, int x0, int z0, int x1, int z1, int y, int kind, int step, Block trap, Block post, Direction... sides) {
		step = Math.max(2, step);
		for (Direction out : sides.length == 0 ? HORIZ : sides) {
			boolean xWall = out == Direction.NORTH || out == Direction.SOUTH;
			int a0 = xWall ? x0 : z0, a1 = xWall ? x1 : z1;
			int len = a1 - a0 - 1;
			if (len < 1) {
				continue;
			}
			int start = a0 + 1 + (len % step) / 2 + (step > 2 ? 1 : 0);
			for (int a = start; a < a1; a += step) {
				if (a <= a0 || a >= a1) {
					continue;
				}
				int x = xWall ? a : (out == Direction.WEST ? x0 : x1);
				int z = xWall ? (out == Direction.NORTH ? z0 : z1) : a;
				if (nearDoor(k, x, y, z)) {
					continue;
				}
				BlockState cur = k.b.get(x, y, z);
				if (cur == null || cur.isAir() || (post != null && cur.getBlock() == post && cur.hasProperty(BlockStateProperties.AXIS)
						&& cur.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y)) {
					continue;
				}
				// Not where an inner wall meets this one.
				int ix = x - out.getStepX(), iz = z - out.getStepZ();
				BlockState inner = k.b.get(ix, y, iz);
				if (inner != null && !inner.isAir()) {
					continue;
				}
				window(k, x, y, z, out, kind, trap);
			}
		}
	}

	static boolean nearDoor(K k, int x, int y, int z) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int dy = -1; dy <= 1; dy++) {
					if (isDoor(k.b.get(x + dx, y + dy, z + dz))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// =================================================================================================================
	// Roofs

	static final int GABLE = 0, HIP = 1, STEP = 2, STEP_HIP = 3, LOW = 4, FLAT = 5;

	/** A roof covering: stairs, slab and full block for shingles or tiles; top/under for thatch or turf. */
	record RoofM(int kind, Block stairs, Block slab, BlockState full, BlockState top, BlockState under) {
	}

	static RoofM boards(Block stairs, Block slab, BlockState full) {
		return new RoofM(GABLE, stairs, slab, full, full, full);
	}

	static RoofM hipBoards(Block stairs, Block slab, BlockState full) {
		return new RoofM(HIP, stairs, slab, full, full, full);
	}

	static RoofM thatch(K k, boolean hip) {
		BlockState top = k.chance(4) ? s(Blocks.DRIED_KELP_BLOCK) : s(Blocks.HAY_BLOCK);
		return new RoofM(hip ? STEP_HIP : STEP, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, top, top, s(Blocks.HAY_BLOCK));
	}

	static RoofM turf(boolean hip) {
		return new RoofM(hip ? STEP_HIP : STEP, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, s(Blocks.GRASS_BLOCK), s(Blocks.GRASS_BLOCK), s(Blocks.DIRT));
	}

	static RoofM low(Block stairs, Block slab, BlockState full) {
		return new RoofM(LOW, stairs, slab, full, full, full);
	}

	/**
	 * A roof over x0..x1 × z0..z1 (the outer walls) starting at y; returns the height of its ridge. alongX: the ridge runs
	 * along X (eaves to the front and back).
	 */
	static int roof(K k, RoofM m, int x0, int x1, int z0, int z1, int y, boolean alongX, int over, BlockState gable) {
		int nAcross = (alongX ? z1 - z0 : x1 - x0) + 2 * over + 1;
		int nMin = Math.min(x1 - x0, z1 - z0) + 2 * over + 1;
		switch (m.kind()) {
			case GABLE -> {
				k.b.gableRoof(x0, x1, z0, z1, y, alongX, over, m.stairs(), m.slab(), m.full(), gable);
				return y + (nAcross - 1) / 2;
			}
			case HIP -> {
				k.b.pyramidRoof(x0 - over, x1 + over, z0 - over, z1 + over, y, m.stairs(), m.full(), null);
				return y + (nMin - 1) / 2;
			}
			case STEP -> {
				stepGable(k.b, x0, x1, z0, z1, y, alongX, over, m.top(), m.under(), gable);
				return y + (nAcross - 1) / 2;
			}
			case STEP_HIP -> {
				return stepHip(k.b, x0, x1, z0, z1, y, over, m.top(), m.under());
			}
			case LOW -> {
				return lowRoof(k.b, x0, x1, z0, z1, y, alongX, over, m.slab(), gable);
			}
			default -> {
				flatRoof(k, x0, z0, x1, z1, y, m.full(), m.slab() != null ? Bp.slab(m.slab(), false) : null);
				return y + 1;
			}
		}
	}

	/** A stepped roof of full blocks (thatch, turf): a thick top layer over an under layer, gables filled. */
	static void stepGable(Bp b, int x0, int x1, int z0, int z1, int y, boolean alongX, int over, BlockState top, BlockState under, BlockState gable) {
		int lo = (alongX ? z0 : x0) - over, hi = (alongX ? z1 : x1) + over, L = 0;
		int a0 = (alongX ? x0 : z0) - over, a1 = (alongX ? x1 : z1) + over;
		while (lo <= hi) {
			for (int a = a0; a <= a1; a++) {
				int[] cs = lo == hi ? new int[]{lo} : new int[]{lo, hi};
				for (int c : cs) {
					int x = alongX ? a : c, z = alongX ? c : a;
					b.set(x, y + L, z, top);
					if (L > 0) {
						b.set(x, y + L - 1, z, under);
					}
				}
			}
			if (gable != null) {
				for (int c = lo + 1; c <= hi - 1; c++) {
					if (alongX ? (c >= z0 && c <= z1) : (c >= x0 && c <= x1)) {
						if (alongX) {
							b.set(x0, y + L, c, gable);
							b.set(x1, y + L, c, gable);
						} else {
							b.set(c, y + L, z0, gable);
							b.set(c, y + L, z1, gable);
						}
					}
				}
			}
			lo++;
			hi--;
			L++;
		}
	}

	/** A stepped hipped roof of full blocks. Returns the ridge height. */
	static int stepHip(Bp b, int x0, int x1, int z0, int z1, int y, int over, BlockState top, BlockState under) {
		int ax = x0 - over, bx = x1 + over, az = z0 - over, bz = z1 + over, L = 0;
		while (ax <= bx && az <= bz) {
			boolean last = ax == bx || az == bz || ax + 1 == bx || az + 1 == bz;
			for (int x = ax; x <= bx; x++) {
				for (int z = az; z <= bz; z++) {
					boolean ring = x == ax || x == bx || z == az || z == bz;
					if (ring || last) {
						b.set(x, y + L, z, top);
						if (L > 0) {
							b.set(x, y + L - 1, z, under);
						}
					}
				}
			}
			if (last) {
				return y + L;
			}
			ax++;
			bx--;
			az++;
			bz--;
			L++;
		}
		return y + L;
	}

	/** A low-pitched roof of slabs (half a block per step): southern tiles. Returns the ridge height. */
	static int lowRoof(Bp b, int x0, int x1, int z0, int z1, int y, boolean alongX, int over, Block slab, BlockState gable) {
		int lo = (alongX ? z0 : x0) - over, hi = (alongX ? z1 : x1) + over;
		int a0 = (alongX ? x0 : z0) - over, a1 = (alongX ? x1 : z1) + over;
		int i = 0, top = y;
		while (lo <= hi) {
			int yy = y + i / 2;
			BlockState sl = Bp.slab(slab, i % 2 == 1);
			for (int a = a0; a <= a1; a++) {
				if (alongX) {
					b.set(a, yy, lo, sl);
					b.set(a, yy, hi, sl);
				} else {
					b.set(lo, yy, a, sl);
					b.set(hi, yy, a, sl);
				}
			}
			top = yy;
			lo++;
			hi--;
			i++;
		}
		if (gable != null) {
			int c0 = alongX ? z0 : x0, c1 = alongX ? z1 : x1;
			for (int c = c0; c <= c1; c++) {
				int j = Math.min(c - (c0 - over), (c1 + over) - c);
				int yy = y + j / 2;
				for (int g = y; g < yy; g++) {
					if (alongX) {
						b.set(x0, g, c, gable);
						b.set(x1, g, c, gable);
					} else {
						b.set(c, g, z0, gable);
						b.set(c, g, z1, gable);
					}
				}
			}
		}
		return top;
	}

	/** A flat roof with a parapet (null = none). */
	static void flatRoof(K k, int x0, int z0, int x1, int z1, int y, BlockState s, BlockState parapet) {
		k.b.fill(x0, y, z0, x1, y, z1, s);
		if (parapet != null) {
			for (int x = x0; x <= x1; x++) {
				k.b.set(x, y + 1, z0, parapet);
				k.b.set(x, y + 1, z1, parapet);
			}
			for (int z = z0; z <= z1; z++) {
				k.b.set(x0, y + 1, z, parapet);
				k.b.set(x1, y + 1, z, parapet);
			}
		}
	}

	/** A chimney stack from y0 to y1 with smoke on top. */
	static void chimney(K k, int x, int z, int y0, int y1, BlockState s) {
		for (int y = y0; y <= y1; y++) {
			k.b.set(x, y, z, s);
		}
		k.b.set(x, y1 + 1, z, s(Blocks.CAMPFIRE));
	}

	/** A ladder against a wall from y0 up to y1 (both included), cutting through the floors on the way. */
	static void ladder(K k, int x, int z, int y0, int y1, Direction face) {
		for (int y = y0; y <= y1; y++) {
			k.b.set(x, y, z, Bp.facing(Blocks.LADDER, face));
		}
	}

	/** A ridge log (охлупень) along the ridge of a gable roof whose ridge runs along Z at x. */
	static void ridgeLog(K k, Block log, int x, int y, int z0, int z1) {
		for (int z = z0; z <= z1; z++) {
			k.setIn(x, y, z, log(log, Direction.Axis.Z));
		}
	}

	// =================================================================================================================
	// Rooms

	static void room(K k, int x0, int z0, int x1, int z1, int y, Use use) {
		if (x1 < x0 || z1 < z0) {
			return;
		}
		Interior.room(k.b, k.c, x0, z0, x1, z1, y, use, k.wealth);
	}

	// =================================================================================================================
	// Finishing touches

	/** Keeps every door's outside free and walkable, then lays the yard surface wherever nothing is. */
	static void finish(K k) {
		for (int[] d : k.doors) {
			int ox = d[0] + d[3], oz = d[2] + d[4], y = d[1];
			// Branches of a yard tree must not hang in a doorway.
			int ix = d[0] - d[3], iz = d[2] - d[4];
			for (int yy = y; yy <= y + 1; yy++) {
				for (int[] c : new int[][]{{ox, oz}, {ix, iz}}) {
					BlockState s = k.b.get(c[0], yy, c[1]);
					if (s != null && k.in(c[0], c[1]) && s.hasProperty(BlockStateProperties.PERSISTENT)) {
						k.b.set(c[0], yy, c[1], AIR);
					}
				}
			}
			if (!k.in(ox, oz)) {
				continue;
			}
			for (int yy = y; yy <= y + 1; yy++) {
				BlockState s = k.b.get(ox, yy, oz);
				boolean step = yy == y && s != null && (s.hasProperty(BlockStateProperties.STAIRS_SHAPE)
						&& s.getValue(BlockStateProperties.HALF) == net.minecraft.world.level.block.state.properties.Half.BOTTOM);
				if (s != null && !s.isAir() && !isDoor(s) && !step) {
					k.b.set(ox, yy, oz, AIR);
				}
			}
			BlockState below = k.b.get(ox, y - 1, oz);
			if (y - 1 >= 1 && (below == null || below.isAir())) {
				k.b.set(ox, y - 1, oz, Bp.slab(k.w.slab, true));
			} else if (y - 1 == 0 && (below == null || below.getBlock() == Blocks.FARMLAND || below.getBlock() == Blocks.GRASS_BLOCK)) {
				k.b.set(ox, 0, oz, s(Blocks.DIRT_PATH));
			}
		}
		boolean south = k.c == Culture.ANDALUSI || k.c == Culture.ARAB || k.c == Culture.PERSIAN || k.c == Culture.BYZANTINE;
		boolean steppe = k.c == Culture.STEPPE;
		for (int x = 0; x < k.W; x++) {
			for (int z = 0; z < k.D; z++) {
				if (k.b.has(x, 0, z)) {
					continue;
				}
				int n = k.r.nextInt(10);
				BlockState g;
				if (south) {
					g = n < 5 ? s(Blocks.PACKED_MUD) : n < 8 ? s(Blocks.COARSE_DIRT) : s(Blocks.GRAVEL);
				} else if (steppe) {
					g = n < 7 ? s(Blocks.GRASS_BLOCK) : s(Blocks.COARSE_DIRT);
				} else if (k.town) {
					g = n < 4 ? s(Blocks.GRASS_BLOCK) : n < 8 ? s(Blocks.COARSE_DIRT) : s(Blocks.PACKED_MUD);
				} else {
					g = n < 7 ? s(Blocks.GRASS_BLOCK) : n < 9 ? s(Blocks.COARSE_DIRT) : s(Blocks.PODZOL);
				}
				k.b.set(x, 0, z, g);
			}
		}
	}

	// =================================================================================================================
	// Rus, Baltic and Finnic peoples: log houses

	static Block rusLog(K k) {
		int n = k.r.nextInt(20);
		boolean bare = k.chance(4);
		if (n < 11) {
			return bare ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.SPRUCE_LOG;
		}
		if (n < 16) {
			return bare ? Blocks.STRIPPED_OAK_LOG : Blocks.OAK_LOG;
		}
		return bare ? Blocks.STRIPPED_DARK_OAK_LOG : Blocks.DARK_OAK_LOG;
	}

	/** A board roof (тёс) of some wood, now and then hipped; poor houses may be thatched. */
	static RoofM rusRoof(K k, boolean thatchOk) {
		if (thatchOk && k.wealth == 0 && k.chance(3)) {
			return thatch(k, k.chance(3));
		}
		Wood wd = k.chance(3) ? Wood.DARK_OAK : k.chance(2) ? Wood.OAK : Wood.SPRUCE;
		if (k.chance(6)) {
			return hipBoards(wd.stairs, wd.slab, s(wd.planks));
		}
		return boards(wd.stairs, wd.slab, s(wd.planks));
	}

	static void logBody(K k, Block log, int x0, int z0, int x1, int z1, int ya, int yb, BlockState fl) {
		if (ya <= 0) {
			found(k, x0, z0, x1, z1, s(Blocks.COBBLESTONE));
		}
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, ya + 1, yb);
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, ya, fl);
		logWalls(k, log, x0, z0, x1, z1, ya, yb, true);
	}

	/** Gable-end carving of a board roof whose ridge runs along Z: ridge log, attic window, the hanging "towel". */
	static void rusGable(K k, RoofM m, Block log, int x0, int x1, int z0, int z1, int h, int ridge) {
		if (m.kind() != GABLE || (x1 - x0) % 2 != 0) {
			return;
		}
		int mid = (x0 + x1) / 2;
		Block bark = log == Blocks.STRIPPED_SPRUCE_LOG ? Blocks.SPRUCE_LOG : log == Blocks.STRIPPED_OAK_LOG ? Blocks.OAK_LOG
				: log == Blocks.STRIPPED_DARK_OAK_LOG ? Blocks.DARK_OAK_LOG : log;
		ridgeLog(k, bark, mid, ridge, z0 - 1, z1 + 1);
		if (ridge >= h + 3) {
			window(k, mid, h + 2, z1, Direction.SOUTH, NALICH, k.w.trapdoor);
		}
		if (ridge - 1 > h + 3) {
			k.setIn(mid, ridge - 1, z1 + 1, Bp.trapdoor(k.w.trapdoor, Direction.SOUTH, false, true));
		}
		// A carved finial on the front end of the ridge log.
		k.soft(mid, ridge + 1, z1 + 1, s(k.w.fence));
	}

	/** Smoke hole: an opening in the ridge with the smouldering smoke under it (курная изба). */
	static void smokeHole(K k, int x, int z, int yLow, int ridge) {
		for (int y = ridge + 1; y >= yLow + 1; y--) {
			BlockState s = k.b.get(x, y, z);
			if (s != null && !s.isAir()) {
				k.b.set(x, y, z, AIR);
				if (y - 1 > yLow && k.empty(x, yLow + 1, z)) {
					k.b.set(x, yLow + 1, z, s(Blocks.CAMPFIRE));
				}
				return;
			}
		}
	}

	/** A small roofed porch (крыльцо) outside a door at ground level. */
	static void porch(K k, int ox, int dz, boolean alongZ, int top) {
		for (int s = -1; s <= 1; s += 2) {
			int x = alongZ ? ox : ox + s, z = alongZ ? dz + s : dz;
			for (int y = 1; y < top; y++) {
				k.soft(x, y, z, s(k.w.fence));
			}
		}
		for (int s = -1; s <= 1; s++) {
			k.soft(alongZ ? ox : ox + s, top, alongZ ? dz + s : dz, Bp.slab(k.w.slab, false));
		}
	}

	/**
	 * A log house, gable to the street, entered from the yard side ({@code side} EAST or WEST). kind: 0 smoke hut (курная
	 * изба), 1 izba with a chimney, 2 izba with a hall (сени) behind, 3 two-room house (пятистенок).
	 */
	static void rusHouse(K k, int x0, int z0, int x1, int z1, Direction side, int kind) {
		if ((kind == 2 || kind == 3) && z1 - z0 < 8) {
			kind = 1;
		}
		int h = k.wealth >= 2 ? 4 : 3;
		Block log = rusLog(k);
		Wood wd = k.w;
		logBody(k, log, x0, z0, x1, z1, 0, h, s(wd.planks));
		boolean east = side != Direction.WEST;
		int sx = east ? x1 : x0, inX = east ? x1 - 1 : x0 + 1, farX = east ? x0 + 1 : x1 - 1, ix = east ? 1 : -1;
		int mid = (x0 + x1) / 2;
		int rz0 = z0 + 1, rz1 = z1 - 1, dz = z0 + 2, zs = -1, zp = -1;
		BlockState inner = log(log, Direction.Axis.X);
		if (kind == 2) {
			zs = z0 + 3;
			partition(k, true, zs, x0, x1, 0, h, inner, wd.door, inX);
			rz0 = zs + 1;
			dz = z0 + 1 + k.r.nextInt(2);
		} else if (kind == 3) {
			zp = (z0 + z1) / 2;
			partition(k, true, zp, x0, x1, 0, h, inner, wd.door, mid);
			rz1 = zp - 1;
			for (int y = 0; y <= h; y++) {
				k.soft(x0 - 1, y, zp, inner);
				k.soft(x1 + 1, y, zp, inner);
			}
			dz = Math.min(dz, rz1 - 1);
		}
		door(k, wd.door, sx, 1, dz, side);
		boolean kurna = kind == 0;
		windows(k, x0, z0, x1, z1, 2, kurna ? SLIT : NALICH, 3, wd.trapdoor, null, Direction.SOUTH, side);
		if (!kurna && k.chance(2)) {
			windows(k, x0, z0, x1, z1, 2, SLIT, 4, wd.trapdoor, null, east ? Direction.WEST : Direction.EAST);
		}
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, h + 1, s(wd.planks));
		RoofM m = rusRoof(k, true);
		int ridge = roof(k, m, x0, x1, z0, z1, h + 1, false, 1, s(wd.planks));
		rusGable(k, m, log, x0, x1, z0, z1, h, ridge);
		int len = rz1 - rz0 + 1 >= 6 ? 3 : 2;
		if (x1 - x0 >= 4 && rz1 - rz0 >= len) {
			Interior.stove(k.b, farX, rz0, ix, 1, 1, true, len, k.wealth, kurna ? -1 : ridge + 1);
		}
		if (kurna) {
			smokeHole(k, mid, rz0 + 1, h + 1, ridge);
		}
		porch(k, sx + ix, dz, true, Math.min(3, h));
		// The earth bench along the front (завалинка).
		if (k.chance(2)) {
			for (int x = x0; x <= x1; x++) {
				k.soft(x, 1, z1 + 1, Bp.slab(wd.slab, false));
			}
		}
		switch (kind) {
			case 2 -> {
				room(k, x0 + 1, z0 + 1, x1 - 1, zs - 1, 1, Use.STORE);
				room(k, x0 + 1, zs + 1, x1 - 1, z1 - 1, 1, Use.LIVING);
			}
			case 3 -> {
				room(k, x0 + 1, z0 + 1, x1 - 1, zp - 1, 1, Use.LIVING);
				room(k, x0 + 1, zp + 1, x1 - 1, z1 - 1, 1, Use.SLEEPING);
			}
			default -> room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, Use.LIVING);
		}
	}

	/**
	 * A two-storey log house: a storage floor (подклет) below, the living floor above reached by an outside stair along the
	 * yard side; a balcony on the street gable. {@code terem}: a boyar's house with a look-out tower room and a tent roof.
	 * Needs a depth of at least 8.
	 */
	static void rusTall(K k, int x0, int z0, int x1, int z1, Direction side, boolean terem) {
		int hu = k.wealth >= 2 ? 4 : 3;
		int fu = 4, top = fu + hu;
		Block log = rusLog(k);
		Wood wd = k.w;
		found(k, x0, z0, x1, z1, s(Blocks.COBBLESTONE));
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, top);
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 0, s(Blocks.COARSE_DIRT));
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, fu, s(wd.planks));
		logWalls(k, log, x0, z0, x1, z1, 0, top, true);
		int mid = (x0 + x1) / 2;
		door(k, wd.door, mid, 1, z1, Direction.SOUTH);
		windows(k, x0, z0, x1, z1, 2, SLIT, 4, wd.trapdoor, null, Direction.NORTH, Direction.WEST, Direction.EAST);
		int zp = z1 - z0 >= 10 && x1 - x0 >= 6 ? (z0 + z1) / 2 + 1 : -1;
		if (zp > 0) {
			partition(k, true, zp, x0, x1, fu, top, log(log, Direction.Axis.X), wd.door, mid);
		}
		boolean east = side != Direction.WEST;
		int sx = east ? x1 : x0, ox = sx + (east ? 1 : -1), farX = east ? x0 + 1 : x1 - 1, ix = east ? 1 : -1;
		int dz = z1 - 2;
		door(k, wd.door, sx, fu + 1, dz, side);
		for (int i = 0; i < fu; i++) {
			int zz = dz - fu + i;
			k.setIn(ox, 1 + i, zz, Bp.stairs(wd.stairs, Direction.SOUTH, false));
			for (int y = 1; y < 1 + i; y++) {
				k.setIn(ox, y, zz, s(wd.planks));
			}
		}
		k.setIn(ox, fu, dz, s(wd.planks));
		for (int y = 1; y < fu; y++) {
			k.setIn(ox, y, dz, log(log, Direction.Axis.Y));
		}
		for (int s = -1; s <= 1; s++) {
			k.setIn(ox, fu + 3, dz + s, Bp.slab(wd.slab, false));
		}
		windows(k, x0, z0, x1, z1, fu + 2, NALICH, 3, wd.trapdoor, null);
		// A balcony over the street door.
		boolean balcony = (k.wealth >= 1 || terem) && k.in(mid, z1 + 1);
		if (balcony) {
			door(k, wd.door, mid, fu + 1, z1, Direction.SOUTH);
			for (int s = -1; s <= 1; s++) {
				k.setIn(mid + s, fu, z1 + 1, s(wd.planks));
			}
			for (int s = -1; s <= 1; s += 2) {
				k.setIn(mid + s, fu + 1, z1 + 1, Bp.trapdoor(wd.trapdoor, Direction.NORTH, false, true));
				k.setIn(mid + s, fu - 1, z1 + 1, Bp.stairs(wd.stairs, Direction.NORTH, true));
			}
		}
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, top + 1, s(wd.planks));
		RoofM m = rusRoof(k, false);
		if (terem && m.kind() != GABLE) {
			m = boards(m.stairs(), m.slab(), m.full());
		}
		int ridge = roof(k, m, x0, x1, z0, z1, top + 1, false, 1, s(wd.planks));
		rusGable(k, m, log, x0, x1, z0, z1, top, ridge);
		int rz0 = zp > 0 ? zp + 1 : z0 + 1;
		int depth = z1 - 1 - rz0 + 1;
		int len = depth >= 6 ? 3 : 2;
		if (x1 - x0 >= 4 && depth > len) {
			Interior.stove(k.b, farX, rz0, ix, 1, fu + 1, true, len, k.wealth, ridge + 1);
		}
		if (terem && x1 - x0 >= 6 && z1 - z0 >= 7) {
			// The look-out tower room (смотрильня) on the street corner opposite the stair, with a tent roof.
			int tx1 = east ? x0 + 4 : x1, tx0 = tx1 - 4, tz1 = z1, tz0 = z1 - 4;
			int ty = top + 1;
			hollow(k, tx0 + 1, tz0 + 1, tx1 - 1, tz1 - 1, ty + 1, ty + 4);
			floor(k, tx0 + 1, tz0 + 1, tx1 - 1, tz1 - 1, ty, s(wd.planks));
			logWalls(k, log, tx0, tz0, tx1, tz1, ty, ty + 4, false);
			windows(k, tx0, tz0, tx1, tz1, ty + 2, NALICH, 2, wd.trapdoor, null);
			Block rs = m.stairs();
			k.b.pyramidRoof(tx0 - 1, tx1 + 1, tz0 - 1, tz1 + 1, ty + 5, rs, m.full(), s(wd.fence));
			int lx = east ? tx0 + 1 : tx1 - 1;
			ladder(k, lx, tz1 - 1, fu + 1, ty, Direction.NORTH);
			room(k, tx0 + 1, tz0 + 1, tx1 - 1, tz1 - 1, ty + 1, Use.SLEEPING);
		}
		room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, Use.STORE);
		if (zp > 0) {
			room(k, x0 + 1, zp + 1, x1 - 1, z1 - 1, fu + 1, Use.LIVING);
			room(k, x0 + 1, z0 + 1, x1 - 1, zp - 1, fu + 1, Use.SLEEPING);
		} else {
			room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, fu + 1, Use.LIVING);
		}
	}

	/**
	 * The northern house-and-barn under one roof (дом-кошель): a log house with a covered barn on its side under a long,
	 * low slope. Needs a width of about 10 or more.
	 */
	static void koshel(K k, int x0, int z0, int x1, int z1, boolean barnEast) {
		int width = x1 - x0 + 1;
		int hw = width >= 13 ? 7 : 5;
		int bw = Math.max(3, Math.min(5, width - hw));
		int h = 3;
		int hx0 = barnEast ? x0 : x1 - hw + 1, hx1 = hx0 + hw - 1;
		int bx0 = barnEast ? hx1 + 1 : hx0 - bw, bx1 = barnEast ? hx1 + bw : hx0 - 1;
		Block log = rusLog(k);
		Wood wd = k.w;
		logBody(k, log, hx0, z0, hx1, z1, 0, h, s(wd.planks));
		found(k, bx0, z0, bx1, z1, s(Blocks.DIRT));
		hollow(k, bx0, z0 + 1, bx1, z1 - 1, 1, h + 3);
		floor(k, bx0, z0 + 1, bx1, z1 - 1, 0, s(Blocks.COARSE_DIRT));
		floor(k, hx0 + 1, z0 + 1, hx1 - 1, z1 - 1, h + 1, s(wd.planks));
		RoofM m = rusRoof(k, false);
		if (m.kind() != GABLE) {
			m = boards(m.stairs(), m.slab(), m.full());
		}
		int ridge = roof(k, m, hx0, hx1, z0, z1, h + 1, false, 1, s(wd.planks));
		rusGable(k, m, log, hx0, hx1, z0, z1, h, ridge);
		// The long slope over the barn, half a block per step, and the barn walls under it.
		int outer = barnEast ? bx1 : bx0, dirx = barnEast ? 1 : -1;
		int shared = barnEast ? hx1 : hx0;
		for (int t = 0; t <= bw; t++) {
			int x = shared + dirx * (t + 1);
			int hh = 2 * (h + 1) + 1 - t, yy = hh / 2;
			for (int z = z0 - 1; z <= z1 + 1; z++) {
				k.setIn(x, yy, z, Bp.slab(m.slab(), hh % 2 == 1));
			}
			if (t < bw) {
				for (int y = 0; y < yy; y++) {
					k.b.set(x, y, z0, s(wd.planks));
					k.b.set(x, y, z1, s(wd.planks));
					if (x == outer) {
						for (int z = z0; z <= z1; z++) {
							k.b.set(x, y, z, (z == z0 || z == z1) ? log(log, Direction.Axis.Y) : s(wd.planks));
						}
					}
				}
			}
		}
		// Wide gates of the barn to the street, the house door into the barn.
		int g0 = barnEast ? bx0 : bx1, g1 = g0 + dirx;
		for (int g : new int[]{g0, g1}) {
			if (g != outer || bw <= 2) {
				k.b.set(g, 1, z1, wd.gate.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
						.setValue(BlockStateProperties.OPEN, true));
				k.b.set(g, 2, z1, AIR);
			}
		}
		door(k, wd.door, shared, 1, z0 + 2, barnEast ? Direction.EAST : Direction.WEST);
		windows(k, hx0, z0, hx1, z1, 2, NALICH, 3, wd.trapdoor, null, Direction.SOUTH, barnEast ? Direction.WEST : Direction.EAST);
		boolean kurna = k.wealth == 0 && k.chance(2);
		int farX = barnEast ? hx0 + 1 : hx1 - 1;
		Interior.stove(k.b, farX, z0 + 1, barnEast ? 1 : -1, 1, 1, true, z1 - z0 >= 8 ? 3 : 2, k.wealth, kurna ? -1 : ridge + 1);
		if (kurna) {
			smokeHole(k, (hx0 + hx1) / 2, z0 + 2, h + 1, ridge);
		}
		room(k, hx0 + 1, z0 + 1, hx1 - 1, z1 - 1, 1, Use.LIVING);
		room(k, barnEast ? bx0 : bx0 + 1, z0 + 1, barnEast ? bx1 - 1 : bx1, z1 - 1, 1, Use.STORE);
	}

	/** A Finnic or Rus bath-house (баня): a little log hut with a stone heater, benches and a tub. */
	static void banya(K k, int x0, int z0, int x1, int z1) {
		Block log = rusLog(k);
		Wood wd = k.w;
		logBody(k, log, x0, z0, x1, z1, 0, 2, s(wd.planks));
		door(k, wd.door, (x0 + x1) / 2, 1, z1, Direction.SOUTH);
		RoofM m = k.chance(2) ? turf(false) : rusRoof(k, true);
		roof(k, m, x0, x1, z0, z1, 3, true, 1, s(wd.planks));
		k.b.set(x0 + 1, 1, z0 + 1, s(Blocks.COBBLESTONE));
		k.b.set(x0 + 1, 2, z0 + 1, s(Blocks.CAMPFIRE));
		if (x1 - x0 >= 3) {
			k.b.set(x1 - 1, 1, z0 + 1, Bp.slab(wd.slab, true));
		}
		if (z1 - z0 >= 4) {
			k.b.set(x1 - 1, 1, z1 - 1, Interior.water());
		}
		window(k, x0, 2, z0 + 1, Direction.WEST, SLIT, wd.trapdoor);
	}

	// =================================================================================================================
	// West Slavs and Magyars: whitewashed wattle and daub, log houses with arcaded porches, half-sunken houses

	static BlockState whitewash(K k) {
		int n = k.r.nextInt(6);
		return n < 3 ? s(Blocks.CALCITE) : n < 5 ? s(Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE)) : s(Blocks.PACKED_MUD);
	}

	/**
	 * A wattle-and-daub house (мазанка), whitewashed, under a thick thatch. Long side to the front: a hall in the middle
	 * with rooms either side; gable to the front: entered from the side. {@code porch}: a gentry porch on posts.
	 */
	static void mazanka(K k, int x0, int z0, int x1, int z1, boolean porch) {
		int h = 3;
		Wood wd = k.w;
		BlockState wash = whitewash(k);
		boolean framed = k.chance(2);
		BlockState base = k.chance(2) ? s(Blocks.MUD_BRICKS) : s(Blocks.PACKED_MUD);
		Wall wall = new Wall(wash, null, 0, framed ? wd.log : null, framed ? 3 : 0, base, framed ? wd.stripped : null);
		found(k, x0, z0, x1, z1, s(Blocks.COBBLESTONE));
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, h);
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 0, s(Blocks.PACKED_MUD));
		box(k, x0, z0, x1, z1, 0, h, wall);
		boolean longX = x1 - x0 >= z1 - z0;
		int mid = (x0 + x1) / 2, midz = (z0 + z1) / 2;
		Direction side = k.chance(2) ? Direction.EAST : Direction.WEST;
		if (longX) {
			door(k, wd.door, mid, 1, z1, Direction.SOUTH);
			if (x1 - x0 >= 8) {
				int xa = mid - 2, xb = mid + 2;
				partition(k, false, xa, z0, z1, 0, h, wash, wd.door, midz);
				partition(k, false, xb, z0, z1, 0, h, wash, wd.door, midz);
			}
		} else {
			door(k, wd.door, side == Direction.EAST ? x1 : x0, 1, z0 + 2, side);
			if (z1 - z0 >= 8) {
				partition(k, true, midz + 1, x0, x1, 0, h, wash, wd.door, mid);
			}
		}
		windows(k, x0, z0, x1, z1, 2, OPEN, 3, wd.trapdoor, framed ? wd.log : null, Direction.SOUTH, Direction.EAST, Direction.WEST);
		RoofM m = k.c == Culture.WEST_SLAV && k.chance(4) ? boards(Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, s(Blocks.SPRUCE_PLANKS)) : thatch(k, k.r.nextInt(10) < 7);
		int ridge = roof(k, m, x0, x1, z0, z1, h + 1, longX, 1, wash);
		if (k.wealth >= 1 && k.chance(2)) {
			chimney(k, longX ? mid - 3 : mid, longX ? midz : z0 + 2, h + 1, ridge + 1, s(Blocks.MUD_BRICKS));
		}
		// The clay bench along the foot of the walls (призьба), painted.
		if (k.chance(2)) {
			for (int x = x0 - 1; x <= x1 + 1; x++) {
				k.soft(x, 1, z1 + 1, Bp.slab(Blocks.MUD_BRICK_SLAB, false));
			}
		}
		if (porch && longX && k.in(mid, z1 + 2)) {
			for (int dx = -1; dx <= 1; dx++) {
				k.setIn(mid + dx, 0, z1 + 1, s(wd.planks));
				k.setIn(mid + dx, 0, z1 + 2, s(wd.planks));
			}
			for (int dx = -1; dx <= 1; dx += 2) {
				for (int y = 1; y <= h; y++) {
					k.setIn(mid + dx, y, z1 + 2, log(wd.stripped, Direction.Axis.Y));
				}
			}
			k.b.gableRoof(mid - 1, mid + 1, z1 + 1, z1 + 2, h + 1, false, 0, wd.stairs, wd.slab, s(wd.planks), wash);
		}
		if (longX && x1 - x0 >= 8) {
			int xa = mid - 2, xb = mid + 2;
			room(k, xa + 1, z0 + 1, xb - 1, z1 - 1, 1, Use.KITCHEN);
			room(k, x0 + 1, z0 + 1, xa - 1, z1 - 1, 1, Use.LIVING);
			room(k, xb + 1, z0 + 1, x1 - 1, z1 - 1, 1, k.wealth >= 1 ? Use.SLEEPING : Use.STORE);
		} else if (!longX && z1 - z0 >= 8) {
			room(k, x0 + 1, z0 + 1, x1 - 1, midz, 1, Use.LIVING);
			room(k, x0 + 1, midz + 2, x1 - 1, z1 - 1, 1, k.wealth >= 1 ? Use.SLEEPING : Use.STORE);
		} else {
			room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, Use.LIVING);
		}
	}

	/**
	 * A half-sunken house (veremház, полуземлянка): the floor dug one block down, a single low course of daub above the
	 * ground, and the thatch coming down nearly to the earth. The door is in the front gable, down a step.
	 */
	static void sunken(K k, int x0, int z0, int x1, int z1, boolean turfRoof) {
		Wood wd = k.w;
		BlockState wash = k.chance(2) ? whitewash(k) : s(Blocks.PACKED_MUD);
		k.b.fill(x0, -2, z0, x1, -2, z1, s(Blocks.DIRT));
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
				k.b.set(x, -1, z, edge ? s(Blocks.DIRT) : s(Blocks.PACKED_MUD));
				k.b.set(x, 0, z, edge ? s(Blocks.COARSE_DIRT) : AIR);
				k.b.set(x, 1, z, edge ? wash : AIR);
			}
		}
		int mid = (x0 + x1) / 2;
		RoofM m = turfRoof ? turf(false) : thatch(k, false);
		roof(k, m, x0, x1, z0, z1, 2, false, 1, wash);
		door(k, wd.door, mid, 0, z1, Direction.SOUTH);
		if (k.in(mid, z1 + 1)) {
			// A step down to the door.
			k.b.set(mid, -1, z1 + 1, s(Blocks.PACKED_MUD));
			k.b.set(mid, 0, z1 + 1, Bp.stairs(wd.stairs, Direction.SOUTH, false));
			k.b.set(mid, 1, z1 + 1, AIR);
		}
		window(k, mid, 2, z0, Direction.NORTH, SLIT, wd.trapdoor);
		int ridge = 2 + (x1 - x0 + 2) / 2;
		smokeHole(k, mid, (z0 + z1) / 2, ridge - 1, ridge);
		room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 0, k.r.nextInt(3) == 0 ? Use.WORKSHOP : Use.LIVING);
	}

	/**
	 * A West Slav log house, gable to the street, whose roof runs on over an open porch on posts (podcienie). Needs a
	 * depth of 8 or more.
	 */
	static void porchHouse(K k, int x0, int z0, int x1, int z1) {
		int h = 3;
		Block log = k.chance(2) ? k.w.log : rusLog(k);
		Wood wd = k.w;
		int zf = z1 - 3;
		logBody(k, log, x0, z0, x1, zf, 0, h, s(wd.planks));
		floor(k, x0, z1 - 2, x1, z1, 0, s(wd.planks));
		found(k, x0, zf + 1, x1, z1, s(Blocks.COBBLESTONE));
		for (int x = x0; x <= x1; x++) {
			boolean post = x == x0 || x == x1 || (x - x0) % 3 == 0;
			for (int y = 1; y < h; y++) {
				if (post) {
					k.b.set(x, y, z1, log(log, Direction.Axis.Y));
				}
			}
			k.b.set(x, h, z1, log(log, Direction.Axis.X));
		}
		for (int z = zf + 1; z < z1; z++) {
			k.b.set(x0, h, z, log(log, Direction.Axis.Z));
			k.b.set(x1, h, z, log(log, Direction.Axis.Z));
		}
		int mid = (x0 + x1) / 2;
		door(k, wd.door, mid, 1, zf, Direction.SOUTH);
		windows(k, x0, z0, x1, zf, 2, OPEN, 3, wd.trapdoor, null, Direction.SOUTH, Direction.EAST, Direction.WEST);
		k.b.set(x0 + 1, 1, z1 - 1, Bp.stairs(wd.stairs, Direction.WEST, false));
		RoofM m = k.chance(2) ? thatch(k, false) : boards(Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, s(Blocks.SPRUCE_PLANKS));
		int ridge = roof(k, m, x0, x1, z0, z1, h + 1, false, 1, s(wd.planks));
		if (k.wealth >= 1 && x1 - x0 >= 5 && zf - z0 >= 5) {
			Interior.stove(k.b, x0 + 1, z0 + 1, 1, 1, 1, true, 2, k.wealth, ridge + 1);
		}
		room(k, x0 + 1, z0 + 1, x1 - 1, zf - 1, 1, Use.LIVING);
	}

	// =================================================================================================================
	// The Latin West and the Celts: timber frame, stone town houses, thatched cottages, byre-houses, round houses

	/** The roof of the Latin West from the palette: slate, shingles or thatch. */
	static RoofM westRoof(K k) {
		if (k.p.thatch) {
			return thatch(k, false);
		}
		return boards(k.p.roofStairs, k.p.roofSlab, k.p.roofFull);
	}

	/** Side overhang of a roof: none where a town house touches its neighbours. */
	static int over(K k, int x0, int x1) {
		return k.town && (x0 <= 0 || x1 >= k.W - 1) ? 0 : 1;
	}

	/**
	 * A town house of the West: a stone (or framed) ground floor, timber-framed storeys above, each jettied one block over
	 * the street if there is room, a steep roof, a chimney on the back wall. {@code shop}: the ground floor is a shop with
	 * fold-down shutters to the street.
	 */
	static void frameHouse(K k, int x0, int z0, int x1, int z1, int floors, boolean stoneGround, boolean shop) {
		Palette p = k.p;
		Wood wd = k.w;
		Block post = k.c == Culture.LATIN || k.c == Culture.CELTIC ? p.log : wd.log;
		Block beam = k.c == Culture.LATIN || k.c == Culture.CELTIC ? p.strippedLog : wd.stripped;
		floors = Math.max(1, Math.min(3, floors));
		boolean jetty = floors >= 2 && z1 + floors - 1 <= k.D - 1 && k.r.nextInt(3) > 0;
		int[] fy = new int[floors], fz = new int[floors];
		for (int i = 0; i < floors; i++) {
			fy[i] = 4 * i;
			fz[i] = z1 + (jetty ? i : 0);
		}
		found(k, x0, z0, x1, z1, p.stone);
		for (int i = 0; i < floors; i++) {
			hollow(k, x0 + 1, z0 + 1, x1 - 1, fz[i] - 1, fy[i] + 1, fy[i] + 3);
			floor(k, x0 + 1, z0 + 1, x1 - 1, fz[i] - 1, fy[i], i == 0 ? p.floor : s(wd.planks));
			Wall wall = i == 0 && stoneGround ? new Wall(p.stone, p.stone2, 6, null, 0, null, null)
					: new Wall(p.plaster, null, 0, post, 3, log(beam, Direction.Axis.X), beam);
			box(k, x0, z0, x1, fz[i], fy[i], fy[i] + 3, wall);
			if (i > 0 && jetty) {
				for (int x = x0; x <= x1; x += 2) {
					k.setIn(x, fy[i] - 1, fz[i], Bp.stairs(wd.stairs, Direction.NORTH, true));
				}
			}
			if (i > 0) {
				windows(k, x0, z0, x1, fz[i], fy[i] + 2, k.wealth >= 1 ? GLASS : OPEN, 3, wd.trapdoor, post, Direction.SOUTH, Direction.NORTH);
				if (x0 > 0 && x1 < k.W - 1) {
					windows(k, x0, z0, x1, fz[i], fy[i] + 2, OPEN, 4, wd.trapdoor, post, Direction.EAST, Direction.WEST);
				}
			}
		}
		int last = floors - 1, top = fy[last] + 3;
		floor(k, x0 + 1, z0 + 1, x1 - 1, fz[last] - 1, top + 1, s(wd.planks));
		int dx = x1 - x0 >= 6 ? (k.chance(2) ? x0 + 2 : x1 - 2) : (x0 + x1) / 2;
		door(k, p.door, dx, 1, z1, Direction.SOUTH);
		k.soft(dx + 1, 3, z1 + 1, Bp.lantern(false));
		if (shop) {
			for (int x = x0 + 1; x < x1; x++) {
				if (Math.abs(x - dx) > 1) {
					k.b.set(x, 2, z1, AIR);
					k.soft(x, 1, z1 + 1, Bp.trapdoor(wd.trapdoor, Direction.SOUTH, true, false));
				}
			}
		} else {
			windows(k, x0, z0, x1, z1, 2, OPEN, 3, wd.trapdoor, post, Direction.SOUTH, Direction.NORTH);
		}
		if (floors > 1) {
			ladder(k, x1 - 1, z0 + 1, 1, fy[last], Direction.SOUTH);
		}
		boolean alongX = k.town ? k.r.nextInt(3) > 0 : k.chance(2);
		RoofM m = westRoof(k);
		int ridge = roof(k, m, x0, x1, z0, fz[last], top + 1, alongX, over(k, x0, x1), p.plaster);
		if (!alongX) {
			window(k, (x0 + x1) / 2, top + 2, fz[last], Direction.SOUTH, OPEN, wd.trapdoor);
		}
		// A fire in a recess of the back wall, its chimney stack outside.
		int cx = x0 + 2;
		if (k.in(cx, z0 - 1) && cx < x1 - 1) {
			k.b.set(cx, 1, z0, s(Blocks.CAMPFIRE));
			for (int y = 0; y <= ridge + 1; y++) {
				k.b.set(cx, y, z0 - 1, y == ridge + 1 ? s(Blocks.CAMPFIRE) : p.stone);
			}
		}
		Use ground = shop ? Use.SHOP : floors == 1 ? Use.LIVING : (k.chance(3) ? Use.WORKSHOP : Use.LIVING);
		for (int i = 0; i < floors; i++) {
			int zz1 = fz[i] - 1, y = fy[i] + 1;
			Use use;
			if (i == 0) {
				use = ground;
			} else if (i == 1) {
				use = ground == Use.LIVING ? Use.SLEEPING : Use.LIVING;
			} else {
				use = Use.SLEEPING;
			}
			if (i > 0 && zz1 - z0 >= 9 && x1 - x0 >= 4) {
				int zp = (z0 + zz1) / 2 + 1;
				partition(k, true, zp, x0, x1, fy[i], fy[i] + 3, p.plaster, p.door, (x0 + x1) / 2);
				room(k, x0 + 1, zp + 1, x1 - 1, zz1, y, use);
				room(k, x0 + 1, z0 + 1, x1 - 1, zp - 1, y, Use.SLEEPING);
			} else {
				room(k, x0 + 1, z0 + 1, x1 - 1, zz1, y, use);
			}
		}
	}

	/**
	 * A stone house of the Romanesque West (round-headed windows, slate or shingle roof, crenels for the rich) or of
	 * Italy (plastered in ochre or white, a low tiled roof, a loggia). {@code tower}: a tower house rising over the roof.
	 */
	static void stoneHouse(K k, int x0, int z0, int x1, int z1, int floors, boolean shop, boolean italian, boolean tower) {
		Palette p = k.p;
		Wood wd = k.w;
		floors = Math.max(1, Math.min(3, floors));
		BlockState wallS, wall2;
		if (italian) {
			int t = k.r.nextInt(5);
			wallS = t == 0 ? s(Blocks.CALCITE) : t == 1 ? s(Blocks.SMOOTH_SANDSTONE) : t == 2 ? s(Blocks.TERRACOTTA)
					: t == 3 ? s(Blocks.DYED_TERRACOTTA.pick(DyeColor.ORANGE)) : s(Blocks.DYED_TERRACOTTA.pick(DyeColor.YELLOW));
			wall2 = wallS;
		} else {
			wallS = k.chance(2) ? s(Blocks.STONE_BRICKS) : p.stone;
			wall2 = k.chance(2) ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.CRACKED_STONE_BRICKS);
		}
		Wall wall = new Wall(wallS, wall2, 7, null, 0, italian ? s(Blocks.STONE_BRICKS) : null, null);
		found(k, x0, z0, x1, z1, s(Blocks.COBBLESTONE));
		int top = 4 * floors - 1;
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, top);
		box(k, x0, z0, x1, z1, 0, top, wall);
		for (int i = 0; i < floors; i++) {
			floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 4 * i, i == 0 ? (italian ? s(Blocks.TERRACOTTA) : s(Blocks.STONE_BRICKS)) : s(wd.planks));
			if (i > 0) {
				windows(k, x0, z0, x1, z1, 4 * i + 2, ARCH, 3, wd.trapdoor, null, Direction.SOUTH, Direction.NORTH);
			}
		}
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, top + 1, s(wd.planks));
		int dx = (x0 + x1) / 2 + (x1 - x0 >= 6 ? k.pick(-2, 2) : 0);
		door(k, p.door, dx, 1, z1, Direction.SOUTH);
		if (shop) {
			for (int x = x0 + 1; x < x1; x++) {
				if (Math.abs(x - dx) > 1 && (x - x0) % 3 != 0) {
					k.b.set(x, 1, z1, AIR);
					k.b.set(x, 2, z1, AIR);
				}
			}
		} else {
			windows(k, x0, z0, x1, z1, 2, italian ? BARS : SLIT, 3, wd.trapdoor, null, Direction.SOUTH);
		}
		// A loggia: the street front of the top floor opens in arches over a balustrade.
		if (italian && floors >= 2) {
			int y = 4 * (floors - 1);
			for (int x = x0 + 1; x < x1; x++) {
				if ((x - x0) % 2 == 1) {
					k.b.set(x, y + 1, z1, s(Blocks.STONE_BRICK_WALL));
					k.b.set(x, y + 2, z1, AIR);
				}
			}
		}
		if (floors > 1) {
			ladder(k, x1 - 1, z0 + 1, 1, 4 * (floors - 1), Direction.SOUTH);
		}
		int ridge;
		boolean crenel = !italian && k.wealth >= 2 && k.chance(2);
		if (italian) {
			Block sl = k.chance(2) ? Blocks.GRANITE_SLAB : Blocks.BRICK_SLAB;
			ridge = lowRoof(k.b, x0, x1, z0, z1, top + 1, true, over(k, x0, x1) == 0 ? 0 : 1, sl, wallS);
		} else if (crenel) {
			flatRoof(k, x0, z0, x1, z1, top + 1, s(wd.planks), null);
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					if (x == x0 || x == x1 || z == z0 || z == z1) {
						k.b.set(x, top + 1, z, wallS);
						if ((x + z) % 2 == 0) {
							k.b.set(x, top + 2, z, wallS);
						}
					}
				}
			}
			ridge = top + 2;
		} else {
			ridge = roof(k, westRoof(k), x0, x1, z0, z1, top + 1, true, over(k, x0, x1), wallS);
		}
		if (tower && x1 - x0 >= 6 && z1 - z0 >= 6) {
			int tx0 = x0, tx1 = x0 + 4, tz0 = z0, tz1 = z0 + 4, ty = top + 1, tt = ridge + 6;
			hollow(k, tx0 + 1, tz0 + 1, tx1 - 1, tz1 - 1, ty, tt);
			box(k, tx0, tz0, tx1, tz1, ty, tt, wall);
			floor(k, tx0 + 1, tz0 + 1, tx1 - 1, tz1 - 1, ty, s(wd.planks));
			windows(k, tx0, tz0, tx1, tz1, ty + 2, SLIT, 2, wd.trapdoor, null);
			windows(k, tx0, tz0, tx1, tz1, tt - 1, SLIT, 2, wd.trapdoor, null);
			for (int x = tx0; x <= tx1; x++) {
				for (int z = tz0; z <= tz1; z++) {
					boolean edge = x == tx0 || x == tx1 || z == tz0 || z == tz1;
					k.b.set(x, tt + 1, z, edge ? wallS : s(wd.planks));
					if (edge && (x + z) % 2 == 0) {
						k.b.set(x, tt + 2, z, wallS);
					}
				}
			}
			ladder(k, tx0 + 1, tz0 + 1, top - 2, tt + 1, Direction.SOUTH);
			room(k, tx0 + 1, tz0 + 2, tx1 - 1, tz1 - 1, ty + 1, Use.STORE);
		}
		for (int i = 0; i < floors; i++) {
			Use use = i == 0 ? (shop ? Use.SHOP : floors == 1 ? Use.LIVING : Use.KITCHEN) : i == floors - 1 ? Use.SLEEPING : Use.HALL;
			if (floors == 2 && i == 1) {
				use = k.wealth >= 2 ? Use.HALL : Use.LIVING;
			}
			room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 4 * i + 1, use);
		}
	}

	/**
	 * A thatched cottage of cob or rubble (Latin West, Celtic lands), smoke going out under the ridge; {@code byre}: people
	 * and cattle under one roof, with a partition and a door each; {@code drystone}: a Hebridean black house with thick
	 * low walls, rounded corners and the thatch sitting inside the wall head.
	 */
	static void cottage(K k, int x0, int z0, int x1, int z1, boolean byre, boolean drystone) {
		Palette p = k.p;
		Wood wd = k.w;
		int h = drystone ? 2 : 3;
		Wall wall = drystone ? new Wall(s(Blocks.COBBLESTONE), s(Blocks.MOSSY_COBBLESTONE), 3, null, 0, null, null)
				: new Wall(k.chance(2) ? p.plaster : s(Blocks.PACKED_MUD), null, 0, k.chance(2) ? p.log : null, 0, p.stone, null);
		found(k, x0, z0, x1, z1, s(Blocks.COBBLESTONE));
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, h + 4);
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 0, k.chance(2) ? s(Blocks.PACKED_MUD) : s(Blocks.COARSE_DIRT));
		box(k, x0, z0, x1, z1, 0, h, wall);
		boolean longX = x1 - x0 >= z1 - z0;
		int dx = longX ? x0 + 2 + k.r.nextInt(Math.max(1, (x1 - x0) / 3)) : (x0 + x1) / 2;
		int xb = -1;
		if (byre && longX && x1 - x0 >= 9) {
			xb = Math.max(dx + 2, x0 + (x1 - x0) * 3 / 5);
			partition(k, false, xb, z0, z1, 0, h, wall.fill(), wd.door, (z0 + z1) / 2);
			door(k, p.door, Math.min(x1 - 1, xb + 2), 1, z1, Direction.SOUTH);
		}
		door(k, p.door, dx, 1, z1, Direction.SOUTH);
		windows(k, x0, z0, x1, z1, 2, OPEN, 4, wd.trapdoor, null, Direction.SOUTH);
		RoofM m = thatch(k, !drystone && k.chance(3));
		int ridge;
		if (drystone) {
			for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
				for (int y = 1; y <= h; y++) {
					k.b.set(c[0], y, c[1], AIR);
				}
			}
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					if ((x == x0 || x == x1 || z == z0 || z == z1) && k.b.has(x, h, z) && !k.empty(x, h, z)) {
						k.b.set(x, h + 1, z, s(Blocks.MOSS_CARPET));
					}
				}
			}
			ridge = roof(k, m, x0 + 1, x1 - 1, z0 + 1, z1 - 1, h + 1, longX, 0, s(Blocks.COBBLESTONE));
		} else {
			ridge = roof(k, m, x0, x1, z0, z1, h + 1, longX, 1, wall.fill());
		}
		smokeHole(k, longX ? x0 + 2 : (x0 + x1) / 2, longX ? (z0 + z1) / 2 : z0 + 2, ridge - 1, ridge);
		if (xb > 0) {
			room(k, x0 + 1, z0 + 1, xb - 1, z1 - 1, 1, Use.LIVING);
			for (int x = xb + 2; x < x1; x += 2) {
				k.soft(x, 1, z0 + 1, s(Blocks.HAY_BLOCK));
				k.soft(x, 1, z0 + 2, s(wd.fence));
			}
			room(k, xb + 1, z0 + 1, x1 - 1, z1 - 1, 1, Use.STORE);
		} else {
			room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, Use.LIVING);
		}
	}

	/** A round house: a ring of stone or wattle, a conical thatch with a smoke hole, the fire in the middle. */
	static void roundhouse(K k, int cx, int cz, int rad, boolean wattle) {
		rad = Math.max(3, Math.min(5, rad));
		int h = rad <= 3 ? 2 : 3;
		Wood wd = k.w;
		BlockState wall = wattle ? (k.chance(2) ? s(Blocks.PACKED_MUD) : k.p.plaster) : s(Blocks.COBBLESTONE);
		BlockState top = k.chance(4) ? s(Blocks.DRIED_KELP_BLOCK) : s(Blocks.HAY_BLOCK);
		for (int x = -rad - 1; x <= rad + 1; x++) {
			for (int z = -rad - 1; z <= rad + 1; z++) {
				double dd = Math.sqrt(x * x + z * z);
				if (dd > rad + 0.3) {
					continue;
				}
				k.b.set(cx + x, 0, cz + z, s(Blocks.PACKED_MUD));
				k.b.set(cx + x, -1, cz + z, s(Blocks.COBBLESTONE));
				boolean ring = dd > rad - 0.7;
				for (int y = 1; y <= h + rad + 2; y++) {
					k.b.set(cx + x, y, cz + z, ring && y <= h ? (!wattle && k.chance(5) ? s(Blocks.MOSSY_COBBLESTONE) : wall) : AIR);
				}
			}
		}
		for (int layer = 0; layer <= rad + 1; layer++) {
			double rr = rad + 1.2 - layer;
			for (int x = -rad - 1; x <= rad + 1; x++) {
				for (int z = -rad - 1; z <= rad + 1; z++) {
					double dd = Math.sqrt(x * x + z * z);
					if (dd <= rr && dd > rr - 1.3 && !(x == 0 && z == 0 && layer > rad - 1)) {
						k.b.set(cx + x, h + 1 + layer, cz + z, top);
					}
				}
			}
		}
		door(k, k.p.door, cx, 1, cz + rad, Direction.SOUTH);
		// The fire in the middle, sleeping places round the wall, stores at the back.
		k.b.set(cx, 1, cz, s(Blocks.CAMPFIRE));
		k.b.set(cx + 1, 1, cz, s(Blocks.COBBLESTONE_SLAB));
		k.b.set(cx - 1, 1, cz, s(Blocks.COBBLESTONE_SLAB));
		k.b.set(cx, 1, cz - 1, s(Blocks.COBBLESTONE_SLAB));
		Block bed = Blocks.BED.pick(k.chance(2) ? DyeColor.BROWN : DyeColor.GREEN);
		k.b.bed(bed, cx - rad + 2, 1, cz + 1, Direction.WEST);
		if (rad >= 4) {
			k.b.bed(bed, cx + rad - 2, 1, cz + 1, Direction.EAST);
		}
		k.b.set(cx, 1, cz - rad + 1, Bp.facing(Blocks.CHEST, Direction.SOUTH));
		k.b.set(cx - 1, 1, cz - rad + 1, Bp.facing(Blocks.LOOM, Direction.SOUTH));
		k.b.set(cx + 1, 1, cz - rad + 1, Interior.barrel());
		if (rad >= 4) {
			k.b.set(cx - 2, 1, cz - rad + 2, Interior.water());
			k.b.set(cx + 2, 1, cz - rad + 2, s(Blocks.DECORATED_POT));
			k.b.set(cx + 2, 1, cz + rad - 2, Bp.slab(wd.slab, false));
		}
		k.b.set(cx + 1, 2, cz - rad + 1, Bp.lantern(false));
	}

	// =================================================================================================================
	// The North: longhouses, turf houses, store-houses on posts

	/**
	 * A Norse longhouse along Z, gable to the street: low stave walls, a turf or shingle roof (hipped ends for some), a
	 * byre at the back end in the country, the hall with its long fire, a store room at the front end if long enough.
	 */
	static void longhouse(K k, int x0, int z0, int x1, int z1, boolean byre) {
		Wood wd = k.w;
		Block post = k.chance(2) ? Blocks.DARK_OAK_LOG : Blocks.SPRUCE_LOG;
		int h = 2;
		int wallKind = k.r.nextInt(3);
		Wall wall = wallKind == 0 ? new Wall(s(wd.planks), null, 0, post, 2, null, null)
				: wallKind == 1 ? new Wall(s(Blocks.PACKED_MUD), s(Blocks.MUD_BRICKS), 4, post, 3, null, null)
				: new Wall(s(Blocks.STRIPPED_SPRUCE_WOOD), s(Blocks.STRIPPED_DARK_OAK_WOOD), 5, post, 4, null, null);
		found(k, x0, z0, x1, z1, s(Blocks.DIRT));
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, h + 6);
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 0, s(Blocks.PACKED_MUD));
		box(k, x0, z0, x1, z1, 0, h, wall);
		int len = z1 - z0;
		int zb = byre && len >= 14 ? z0 + len / 3 : -1;
		int zf = len >= 16 ? z1 - 4 : -1;
		BlockState part = s(wd.planks);
		if (zb > 0) {
			partition(k, true, zb, x0, x1, 0, h, part, wd.door, (x0 + x1) / 2);
		}
		if (zf > 0) {
			partition(k, true, zf, x0, x1, 0, h, part, wd.door, (x0 + x1) / 2);
		}
		boolean east = k.chance(2);
		int hall0 = zb > 0 ? zb + 1 : z0 + 1, hall1 = zf > 0 ? zf - 1 : z1 - 1;
		int dz = Math.max(hall0 + 1, hall1 - 1);
		door(k, wd.door, east ? x1 : x0, 1, dz, east ? Direction.EAST : Direction.WEST);
		if (zb > 0) {
			door(k, wd.door, east ? x0 : x1, 1, z0 + 2, east ? Direction.WEST : Direction.EAST);
		}
		door(k, wd.door, (x0 + x1) / 2, 1, z1, Direction.SOUTH);
		RoofM m = k.r.nextInt(3) > 0 ? turf(k.chance(3)) : boards(Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, s(Blocks.DARK_OAK_PLANKS));
		int ridge = roof(k, m, x0, x1, z0, z1, h + 1, false, 1, s(wd.planks));
		if (m.kind() == STEP || m.kind() == GABLE) {
			// Crossed gable boards (vindskeier) over both ends.
			int mid = (x0 + x1) / 2;
			k.soft(mid - 1, ridge + 1, z1 + 1, s(wd.fence));
			k.soft(mid + 1, ridge + 1, z1 + 1, s(wd.fence));
			k.soft(mid - 1, ridge + 1, z0 - 1, s(wd.fence));
			k.soft(mid + 1, ridge + 1, z0 - 1, s(wd.fence));
		}
		smokeHole(k, (x0 + x1) / 2, (hall0 + hall1) / 2, ridge - 1, ridge);
		if (zb > 0) {
			for (int z = z0 + 1; z < zb; z += 2) {
				k.soft(x0 + 2, 1, z, s(wd.fence));
				k.soft(x1 - 2, 1, z, s(wd.fence));
			}
			room(k, x0 + 1, z0 + 1, x1 - 1, zb - 1, 1, Use.STORE);
		}
		room(k, x0 + 1, hall0, x1 - 1, hall1, 1, Use.LIVING);
		if (zf > 0) {
			room(k, x0 + 1, zf + 1, x1 - 1, z1 - 1, 1, k.chance(2) ? Use.STORE : Use.WORKSHOP);
		}
	}

	/** A turf house: thick walls of sod, a timber front gable with the door, a grass roof. */
	static void turfHouse(K k, int x0, int z0, int x1, int z1) {
		Wood wd = k.w;
		int h = 2;
		found(k, x0, z0, x1, z1, s(Blocks.DIRT));
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, h + 5);
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 0, s(wd.planks));
		box(k, x0, z0, x1, z1, 0, h, new Wall(s(Blocks.DIRT), s(Blocks.COARSE_DIRT), 3, null, 0, null, null));
		// The sod walls are two thick on the sides and the back.
		for (int y = 1; y <= h; y++) {
			for (int z = z0 - 1; z <= z1; z++) {
				k.soft(x0 - 1, y, z, s(Blocks.ROOTED_DIRT));
				k.soft(x1 + 1, y, z, s(Blocks.ROOTED_DIRT));
			}
			for (int x = x0; x <= x1; x++) {
				k.soft(x, y, z0 - 1, s(Blocks.ROOTED_DIRT));
			}
		}
		for (int x = x0 - 1; x <= x1 + 1; x++) {
			for (int z = z0 - 1; z <= z1; z++) {
				if (k.in(x, z) && (x < x0 || x > x1 || z < z0)) {
					k.soft(x, h + 1, z, s(Blocks.GRASS_BLOCK));
				}
			}
		}
		for (int x = x0; x <= x1; x++) {
			for (int y = 1; y <= h; y++) {
				k.b.set(x, y, z1, (x == x0 || x == x1) ? log(Blocks.SPRUCE_LOG, Direction.Axis.Y) : s(wd.planks));
			}
		}
		int mid = (x0 + x1) / 2;
		door(k, wd.door, mid, 1, z1, Direction.SOUTH);
		roof(k, turf(false), x0, x1, z0, z1, h + 1, false, 1, s(wd.planks));
		window(k, mid, h + 2, z1, Direction.SOUTH, SLIT, wd.trapdoor);
		room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, Use.LIVING);
	}

	/** A store-house raised on posts against damp and mice (амбар, stabbur). */
	static void stilted(K k, int x0, int z0, int x1, int z1) {
		Wood wd = k.w;
		Block log = k.c == Culture.NORSE ? Blocks.DARK_OAK_LOG : wd.log;
		for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
			k.b.set(c[0], 0, c[1], s(Blocks.COBBLESTONE));
			k.b.set(c[0], 1, c[1], log(log, Direction.Axis.Y));
		}
		floor(k, x0, z0, x1, z1, 2, s(wd.planks));
		logWalls(k, log, x0, z0, x1, z1, 2, 4, false);
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 3, 4);
		RoofM m = k.c == Culture.NORSE ? turf(false) : k.p.thatch ? thatch(k, false) : boards(k.p.roofStairs, k.p.roofSlab, k.p.roofFull);
		roof(k, m, x0, x1, z0, z1, 5, true, 1, s(wd.planks));
		int mid = (x0 + x1) / 2;
		door(k, wd.door, mid, 3, z1, Direction.SOUTH);
		k.setIn(mid, 1, z1 + 1, Bp.stairs(wd.stairs, Direction.NORTH, false));
		room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 3, Use.STORE);
	}

	// =================================================================================================================
	// Byzantium and the Caucasus

	/** Brick walls laid in bands with courses of stone every fourth row (opus mixtum); some plastered. */
	static void banded(K k, int x0, int z0, int x1, int z1, int ya, int yb) {
		boolean plastered = k.chance(4);
		BlockState brick = plastered ? k.p.plaster : s(Blocks.BRICKS);
		BlockState band = k.chance(2) ? s(Blocks.STONE_BRICKS) : s(Blocks.POLISHED_ANDESITE);
		for (int y = ya; y <= yb; y++) {
			box(k, x0, z0, x1, z1, y, y, plain((y - ya) % 4 == 3 ? band : brick));
		}
	}

	/**
	 * A Byzantine town house of two or three storeys in banded brick: an arcaded shop or a stable below, rooms above, a
	 * hanging wooden bay (σαχνισί) with lattice windows on the top floor, a tiled roof. {@code outsideStair}: a village house
	 * whose living floor is reached by a stone stair along the side.
	 */
	static void byzHouse(K k, int x0, int z0, int x1, int z1, int floors, boolean shop, boolean outsideStair) {
		Wood wd = k.w;
		floors = Math.max(1, Math.min(3, floors));
		int top = 4 * floors - 1;
		found(k, x0, z0, x1, z1, s(Blocks.STONE_BRICKS));
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, top);
		banded(k, x0, z0, x1, z1, 0, top);
		for (int i = 0; i < floors; i++) {
			floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 4 * i, i == 0 ? k.p.floor : s(wd.planks));
			if (i > 0) {
				windows(k, x0, z0, x1, z1, 4 * i + 2, k.chance(2) ? ARCH : LATTICE, 3, wd.trapdoor, null, Direction.SOUTH, Direction.NORTH);
			}
		}
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, top + 1, s(wd.planks));
		int mid = (x0 + x1) / 2;
		int dx = x1 - x0 >= 6 ? mid + k.pick(-2, 2) : mid;
		door(k, wd.door, dx, 1, z1, Direction.SOUTH);
		if (shop) {
			for (int x = x0 + 1; x < x1; x++) {
				if (Math.abs(x - dx) > 1 && (x - x0) % 3 != 0) {
					k.b.set(x, 1, z1, AIR);
					k.b.set(x, 2, z1, AIR);
					k.soft(x, 3, z1 + 1, Bp.slab(wd.slab, true));
				}
			}
		} else {
			windows(k, x0, z0, x1, z1, 2, BARS, 4, wd.trapdoor, null, Direction.SOUTH);
		}
		int upperDoorZ = -1;
		boolean east = k.chance(2);
		if (outsideStair && floors >= 2) {
			int sx = east ? x1 : x0, ox = sx + (east ? 1 : -1);
			int dz = z1 - 2;
			if (dz - 4 >= 0) {
				door(k, wd.door, sx, 5, dz, east ? Direction.EAST : Direction.WEST);
				for (int i = 0; i < 4; i++) {
					int zz = dz - 4 + i;
					k.setIn(ox, 1 + i, zz, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, false));
					for (int y = 0; y < 1 + i; y++) {
						k.setIn(ox, y, zz, s(Blocks.STONE_BRICKS));
					}
				}
				for (int y = 0; y <= 4; y++) {
					k.setIn(ox, y, dz, s(Blocks.STONE_BRICKS));
				}
				upperDoorZ = dz;
			}
		}
		if (floors > 1) {
			ladder(k, east ? x0 + 1 : x1 - 1, z0 + 1, 1, 4 * (floors - 1), Direction.SOUTH);
		}
		// The hanging bay on beams over the street.
		int fyTop = 4 * (floors - 1);
		if (floors >= 2 && k.in(mid, z1 + 1) && x1 - x0 >= 6 && (k.wealth >= 1 || k.chance(2))) {
			int bx0 = mid - 2, bx1 = mid + 2;
			for (int x = bx0; x <= bx1; x++) {
				boolean side = x == bx0 || x == bx1;
				k.setIn(x, fyTop, z1 + 1, Bp.stairs(wd.stairs, Direction.NORTH, true));
				k.setIn(x, fyTop + 1, z1 + 1, side ? s(wd.planks) : Bp.trapdoor(wd.trapdoor, Direction.SOUTH, false, true));
				k.setIn(x, fyTop + 2, z1 + 1, side ? s(wd.planks) : Bp.trapdoor(wd.trapdoor, Direction.SOUTH, false, true));
				k.setIn(x, fyTop + 3, z1 + 1, s(wd.planks));
				k.setIn(x, fyTop + 4, z1 + 1, Bp.stairs(k.p.roofStairs, Direction.NORTH, false));
				if (!side) {
					k.b.set(x, fyTop + 1, z1, AIR);
					k.b.set(x, fyTop + 2, z1, AIR);
				}
			}
			k.setIn(bx0, fyTop - 1, z1 + 1, Bp.stairs(wd.stairs, Direction.NORTH, true));
			k.setIn(bx1, fyTop - 1, z1 + 1, Bp.stairs(wd.stairs, Direction.NORTH, true));
		}
		int t = k.r.nextInt(3);
		int ov = over(k, x0, x1);
		if (t == 0) {
			k.b.pyramidRoof(x0 - ov, x1 + ov, z0 - ov, z1 + ov, top + 1, k.p.roofStairs, k.p.roofFull, null);
		} else if (t == 1) {
			lowRoof(k.b, x0, x1, z0, z1, top + 1, true, ov, k.chance(2) ? Blocks.BRICK_SLAB : Blocks.GRANITE_SLAB, s(Blocks.BRICKS));
		} else {
			roof(k, boards(k.p.roofStairs, k.p.roofSlab, k.p.roofFull), x0, x1, z0, z1, top + 1, true, ov, s(Blocks.BRICKS));
		}
		for (int i = 0; i < floors; i++) {
			Use use = i == 0 ? (shop ? Use.SHOP : outsideStair ? Use.STORE : floors == 1 ? Use.LIVING : Use.KITCHEN)
					: i == floors - 1 && floors == 3 ? Use.SLEEPING : Use.LIVING;
			if (i == 1 && floors == 2 && upperDoorZ < 0 && !shop) {
				use = Use.LIVING;
			}
			room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 4 * i + 1, use);
		}
	}

	static BlockState caucStone(K k) {
		int n = k.r.nextInt(5);
		return n == 0 ? s(Blocks.TUFF) : n == 1 ? s(Blocks.TUFF_BRICKS) : n == 2 ? s(Blocks.STONE_BRICKS) : n == 3 ? s(Blocks.COBBLESTONE) : s(Blocks.POLISHED_TUFF);
	}

	/**
	 * A Caucasian stone house under flat earthen roofs, built like a terrace on a hillside: a two-storey block behind, a
	 * one-storey block in front whose roof is the terrace before the upper rooms, reached by a stone stair.
	 */
	static void flatHouse(K k, int x0, int z0, int x1, int z1) {
		Wood wd = k.w;
		BlockState st = caucStone(k);
		Wall wall = new Wall(st, s(Blocks.MOSSY_COBBLESTONE), 9, null, 0, null, null);
		BlockState earth = s(Blocks.COARSE_DIRT);
		int zm = z1 - z0 >= 9 ? z0 + (z1 - z0) / 2 : -1;
		found(k, x0, z0, x1, z1, s(Blocks.COBBLESTONE));
		int mid = (x0 + x1) / 2;
		if (zm < 0) {
			hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, 3);
			floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 0, s(Blocks.PACKED_MUD));
			box(k, x0, z0, x1, z1, 0, 3, wall);
			flatRoof(k, x0, z0, x1, z1, 4, earth, Bp.slab(Blocks.COBBLESTONE_SLAB, false));
			door(k, wd.door, mid, 1, z1, Direction.SOUTH);
			windows(k, x0, z0, x1, z1, 2, SLIT, 3, wd.trapdoor, null);
			if (k.in(x1 + 1, z1 - 1)) {
				for (int y = 1; y <= 4; y++) {
					k.b.set(x1 + 1, y, z1 - 1, Bp.facing(Blocks.LADDER, Direction.EAST));
				}
				k.b.set(x1, 5, z1 - 1, AIR);
			}
			room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, Use.LIVING);
			return;
		}
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, 7);
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 0, s(Blocks.PACKED_MUD));
		box(k, x0, z0, x1, zm, 0, 7, wall);
		box(k, x0, zm, x1, z1, 0, 3, wall);
		floor(k, x0 + 1, z0 + 1, x1 - 1, zm - 1, 4, s(wd.planks));
		flatRoof(k, x0, z0, x1, zm, 8, earth, Bp.slab(Blocks.COBBLESTONE_SLAB, false));
		flatRoof(k, x0, zm, x1, z1, 4, s(Blocks.PACKED_MUD), Bp.slab(Blocks.COBBLESTONE_SLAB, false));
		for (int x = x0; x <= x1; x++) {
			k.b.set(x, 4, zm, st);
			k.b.set(x, 5, zm, st);
		}
		door(k, wd.door, mid, 1, z1, Direction.SOUTH);
		door(k, wd.door, mid, 5, zm, Direction.SOUTH);
		partition(k, true, zm, x0, x1, 0, 3, st, wd.door, x1 - 2);
		windows(k, x0, zm, x1, z1, 2, SLIT, 3, wd.trapdoor, null, Direction.SOUTH, Direction.EAST, Direction.WEST);
		windows(k, x0, z0, x1, zm, 6, k.chance(2) ? LATTICE : SLIT, 3, wd.trapdoor, null, Direction.SOUTH, Direction.NORTH);
		// A wooden balcony on the terrace, and a stone stair up to it at the side.
		boolean east = k.chance(2);
		int sx = east ? x1 : x0, ox = sx + (east ? 1 : -1);
		for (int i = 0; i < 4; i++) {
			int zz = z1 - i;
			if (zz <= zm) {
				break;
			}
			k.setIn(ox, 1 + i, zz, Bp.stairs(Blocks.COBBLESTONE_STAIRS, Direction.NORTH, false));
			for (int y = 0; y < 1 + i; y++) {
				k.setIn(ox, y, zz, st);
			}
		}
		k.b.set(sx, 5, Math.max(zm + 1, z1 - 3), AIR);
		k.b.set(sx, 5, Math.max(zm + 1, z1 - 4), AIR);
		for (int x = x0 + 1; x < x1; x += 3) {
			k.soft(x, 5, zm + 1, s(wd.fence));
			k.soft(x, 6, zm + 1, s(wd.fence));
		}
		for (int x = x0; x <= x1; x++) {
			k.soft(x, 7, zm + 1, Bp.slab(wd.slab, false));
		}
		room(k, x0 + 1, zm + 1, x1 - 1, z1 - 1, 1, Use.LIVING);
		room(k, x0 + 1, z0 + 1, x1 - 1, zm - 1, 1, Use.STORE);
		room(k, x0 + 1, z0 + 1, x1 - 1, zm - 1, 5, Use.SLEEPING);
	}

	/**
	 * The darbazi: a square hall of stone whose wooden ceiling rises in steps of corbelled logs to a smoke and light hole
	 * over the hearth, the whole mounded over with earth outside.
	 */
	static void darbazi(K k, int x0, int z0, int x1, int z1) {
		Wood wd = k.w;
		BlockState st = caucStone(k);
		found(k, x0, z0, x1, z1, s(Blocks.COBBLESTONE));
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, 12);
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 0, s(Blocks.PACKED_MUD));
		box(k, x0, z0, x1, z1, 0, 3, new Wall(st, s(Blocks.MOSSY_COBBLESTONE), 8, null, 0, null, null));
		Block log = k.chance(2) ? Blocks.DARK_OAK_LOG : Blocks.OAK_LOG;
		int L = 0;
		while (true) {
			int ax = x0 + L, bx = x1 - L, az = z0 + L, bz = z1 - L;
			if (bx - ax < 3 || bz - az < 3) {
				break;
			}
			Direction.Axis axis = L % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z;
			for (int x = ax; x <= bx; x++) {
				for (int z = az; z <= bz; z++) {
					if (x == ax || x == bx || z == az || z == bz) {
						k.b.set(x, 4 + L, z, log(log, axis));
						k.b.set(x, 5 + L, z, L == 0 && (x == x0 || x == x1 || z == z0 || z == z1) ? s(Blocks.GRASS_BLOCK) : s(Blocks.DIRT));
					}
				}
			}
			L++;
		}
		// Grass over the top ring of earth; the hole in the middle stays open.
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = 12; y >= 5; y--) {
					BlockState s = k.b.get(x, y, z);
					if (s != null && s.getBlock() == Blocks.DIRT) {
						if (k.empty(x, y + 1, z)) {
							k.b.set(x, y, z, s(Blocks.GRASS_BLOCK));
						}
						break;
					}
				}
			}
		}
		int mid = (x0 + x1) / 2;
		door(k, wd.door, mid, 1, z1, Direction.SOUTH);
		windows(k, x0, z0, x1, z1, 2, SLIT, 4, wd.trapdoor, null, Direction.EAST, Direction.WEST);
		room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, Use.LIVING);
	}

	/** A Svan defence tower: five storeys of stone, slits, ladders, a little gabled cap. */
	static void tower(K k, int x0, int z0, int size) {
		size = Math.max(4, Math.min(5, size));
		int x1 = x0 + size - 1, z1 = z0 + size - 1;
		BlockState st = caucStone(k);
		int floors = 5, top = 4 * floors - 1;
		found(k, x0, z0, x1, z1, s(Blocks.COBBLESTONE));
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, top);
		box(k, x0, z0, x1, z1, 0, top, new Wall(st, s(Blocks.MOSSY_STONE_BRICKS), 10, null, 0, null, null));
		for (int i = 1; i < floors; i++) {
			floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 4 * i, s(k.w.planks));
			windows(k, x0, z0, x1, z1, 4 * i + 2, SLIT, 2, k.w.trapdoor, null);
		}
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, top + 1, s(k.w.planks));
		ladder(k, x0 + 1, z0 + 1, 1, top + 1, Direction.SOUTH);
		door(k, k.w.door, (x0 + x1) / 2, 1, z1, Direction.SOUTH);
		k.b.gableRoof(x0, x1, z0, z1, top + 2, true, 0, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, s(Blocks.DEEPSLATE_TILES), st);
		for (int x = x0; x <= x1; x++) {
			k.b.set(x, top + 1, z0, st);
			k.b.set(x, top + 1, z1, st);
		}
		room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, Use.STORE);
	}

	// =================================================================================================================
	// The Islamic world: courtyard houses, tall houses with lattice bays, domed houses, wind-catchers

	static BlockState southTone(K k) {
		int n = k.r.nextInt(8);
		return switch (k.c) {
			case ANDALUSI -> n < 4 ? s(Blocks.CALCITE) : n < 6 ? s(Blocks.DYED_TERRACOTTA.pick(DyeColor.WHITE)) : s(Blocks.SMOOTH_SANDSTONE);
			case PERSIAN -> n < 3 ? s(Blocks.MUD_BRICKS) : n < 5 ? s(Blocks.PACKED_MUD) : n < 7 ? s(Blocks.SMOOTH_SANDSTONE) : s(Blocks.BRICKS);
			default -> n < 3 ? s(Blocks.MUD_BRICKS) : n < 5 ? s(Blocks.PACKED_MUD) : n < 7 ? s(Blocks.SMOOTH_SANDSTONE) : s(Blocks.CALCITE);
		};
	}

	static Block parapetSlab(K k, BlockState tone) {
		Block bl = tone.getBlock();
		return bl == Blocks.MUD_BRICKS || bl == Blocks.PACKED_MUD ? Blocks.MUD_BRICK_SLAB : bl == Blocks.BRICKS ? Blocks.BRICK_SLAB : Blocks.SMOOTH_SANDSTONE_SLAB;
	}

	/** A flat roof terrace with a parapet: low slabs, or stepped merlons in al-Andalus and for the rich. */
	static void terrace(K k, int x0, int z0, int x1, int z1, int y, BlockState tone, boolean merlons) {
		k.b.fill(x0, y, z0, x1, y, z1, tone);
		Block sl = parapetSlab(k, tone);
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (x == x0 || x == x1 || z == z0 || z == z1) {
					k.b.set(x, y + 1, z, merlons ? ((x + z) % 2 == 0 ? tone : Bp.slab(sl, false)) : Bp.slab(sl, false));
				}
			}
		}
	}

	/** A wind-catcher (бадгир): a square tower over the roof with slots near the top to catch the wind. */
	static void windcatcher(K k, int x, int z, int y0, int h, BlockState tone) {
		for (int y = y0; y <= y0 + h; y++) {
			for (int dx = 0; dx <= 2; dx++) {
				for (int dz = 0; dz <= 2; dz++) {
					if (dx == 1 && dz == 1) {
						continue;
					}
					boolean slot = y >= y0 + h - 1 && (dx == 1 || dz == 1);
					k.setIn(x + dx, y, z + dz, slot ? AIR : tone);
				}
			}
		}
		Block sl = parapetSlab(k, tone);
		for (int dx = 0; dx <= 2; dx++) {
			for (int dz = 0; dz <= 2; dz++) {
				k.setIn(x + dx, y0 + h + 1, z + dz, Bp.slab(sl, false));
			}
		}
	}

	/** An enclosed wooden bay with lattice windows hanging over the street on beams (mashrabiya). */
	static void bay(K k, int bx0, int bx1, int fy, int z1, BlockState frame) {
		Block trap = k.chance(2) ? Blocks.JUNGLE_TRAPDOOR : k.w.trapdoor;
		for (int x = bx0; x <= bx1; x++) {
			boolean side = x == bx0 || x == bx1;
			k.setIn(x, fy, z1 + 1, Bp.stairs(k.w.stairs, Direction.NORTH, true));
			k.setIn(x, fy + 1, z1 + 1, side ? frame : Bp.trapdoor(trap, Direction.SOUTH, false, true));
			k.setIn(x, fy + 2, z1 + 1, side ? frame : Bp.trapdoor(trap, Direction.SOUTH, false, true));
			k.setIn(x, fy + 3, z1 + 1, frame);
			k.setIn(x, fy + 4, z1 + 1, Bp.slab(k.w.slab, false));
			if (!side) {
				k.b.set(x, fy + 1, z1, AIR);
				k.b.set(x, fy + 2, z1, AIR);
			}
		}
	}

	/** A door with a surround of glazed tiles or a pointed hood for the rich. */
	static void grandDoor(K k, int x, int z, BlockState tone) {
		door(k, k.w.door, x, 1, z, Direction.SOUTH);
		if (k.wealth >= 1) {
			BlockState tile = k.c == Culture.PERSIAN ? s(Blocks.DYED_TERRACOTTA.pick(DyeColor.CYAN))
					: k.c == Culture.ANDALUSI ? s(Blocks.DYED_TERRACOTTA.pick(DyeColor.RED)) : s(Blocks.CUT_SANDSTONE);
			for (int y = 1; y <= 3; y++) {
				k.b.set(x - 1, y, z, tile);
				k.b.set(x + 1, y, z, tile);
			}
			k.b.set(x, 3, z, tile);
		}
	}

	/**
	 * A house round a courtyard: blank walls to the street, a bent entrance through the street wing, rooms in four wings
	 * opening onto the court (fountain, a tree, an arcade for the rich), flat roof terraces, a wind-catcher in Iran.
	 */
	static void courtyard(K k, int x0, int z0, int x1, int z1) {
		if (x1 - x0 < 10 || z1 - z0 < 10) {
			tallHouse(k, x0, z0, x1, z1, 2);
			return;
		}
		BlockState tone = southTone(k);
		BlockState inner = k.chance(3) ? s(Blocks.CALCITE) : tone;
		int h = k.wealth >= 2 ? 4 : 3;
		int rw = (x1 - x0 >= 14 && z1 - z0 >= 14) ? 4 : 3;
		int cx0 = x0 + rw, cx1 = x1 - rw, cz0 = z0 + rw, cz1 = z1 - rw;
		found(k, x0, z0, x1, z1, s(Blocks.CUT_SANDSTONE));
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, h);
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 0, k.wealth >= 1 ? s(Blocks.SMOOTH_SANDSTONE) : s(Blocks.PACKED_MUD));
		box(k, x0, z0, x1, z1, 0, h, plain(tone));
		partition(k, true, cz0, x0, x1, 0, h, inner, null, 0);
		partition(k, true, cz1, x0, x1, 0, h, inner, null, 0);
		partition(k, false, cx0, cz0, cz1, 0, h, inner, null, 0);
		partition(k, false, cx1, cz0, cz1, 0, h, inner, null, 0);
		hollow(k, cx0 + 1, cz0 + 1, cx1 - 1, cz1 - 1, 0, h + 2);
		int mx = (x0 + x1) / 2, mz = (cz0 + cz1) / 2;
		// The court: paving, a fountain or well, a tree, flowers.
		for (int x = cx0 + 1; x < cx1; x++) {
			for (int z = cz0 + 1; z < cz1; z++) {
				k.b.set(x, 0, z, (x + z) % 2 == 0 ? s(Blocks.SMOOTH_SANDSTONE) : s(Blocks.CUT_SANDSTONE));
			}
		}
		int fx = (cx0 + cx1) / 2;
		k.b.set(fx, 0, mz, s(Blocks.WATER));
		k.b.set(fx, -1, mz, s(Blocks.CUT_SANDSTONE));
		for (Direction d : HORIZ) {
			k.b.set(fx + d.getStepX(), 1, mz + d.getStepZ(), Bp.slab(Blocks.SMOOTH_SANDSTONE_SLAB, false));
		}
		if (cx1 - cx0 >= 6) {
			k.b.set(cx0 + 1, 0, cz0 + 1, s(Blocks.GRASS_BLOCK));
			k.b.set(cx0 + 1, 1, cz0 + 1, k.chance(2) ? s(Blocks.FLOWERING_AZALEA) : s(Blocks.AZALEA));
			k.b.set(cx1 - 1, 0, cz0 + 1, s(Blocks.GRASS_BLOCK));
			k.b.set(cx1 - 1, 1, cz0 + 1, Interior.flowerPot(k.r));
		}
		// Arches from every wing onto the court.
		int[][] arches = {{fx, cz0}, {fx, cz1}, {cx0, mz}, {cx1, mz}};
		for (int[] a : arches) {
			k.b.set(a[0], 1, a[1], AIR);
			k.b.set(a[0], 2, a[1], AIR);
		}
		windows(k, cx0, cz0, cx1, cz1, 2, LATTICE, 3, k.w.trapdoor, null);
		windows(k, x0, z0, x1, z1, h - 1, k.chance(2) ? LATTICE : BARS, 4, k.w.trapdoor, null);
		// The street door opens into the side of the street wing, not straight into the court.
		int dx = mx + (mx - fx == 0 ? k.pick(-2, 2) : 0);
		if (Math.abs(dx - fx) < 2) {
			dx = fx + 2 <= x1 - 2 ? fx + 2 : fx - 2;
		}
		grandDoor(k, dx, z1, tone);
		// Roof terraces, with an arcade round the court for the rich.
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean court = x > cx0 && x < cx1 && z > cz0 && z < cz1;
				if (!court) {
					k.b.set(x, h + 1, z, tone);
				}
			}
		}
		Block sl = parapetSlab(k, tone);
		boolean merlons = k.c == Culture.ANDALUSI || k.wealth >= 2;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean outer = x == x0 || x == x1 || z == z0 || z == z1;
				boolean courtEdge = (x == cx0 || x == cx1) && z >= cz0 && z <= cz1 || (z == cz0 || z == cz1) && x >= cx0 && x <= cx1;
				if (outer) {
					k.b.set(x, h + 2, z, merlons && (x + z) % 2 == 0 ? tone : Bp.slab(sl, false));
				} else if (courtEdge) {
					k.b.set(x, h + 2, z, Bp.slab(sl, false));
				}
			}
		}
		if (k.wealth >= 1 && cx1 - cx0 >= 5 && cz1 - cz0 >= 5) {
			for (int x = cx0 + 1; x < cx1; x++) {
				for (int z = cz0 + 1; z < cz1; z++) {
					boolean edge = x == cx0 + 1 || x == cx1 - 1 || z == cz0 + 1 || z == cz1 - 1;
					if (!edge) {
						continue;
					}
					k.b.set(x, h, z, Bp.slab(sl, true));
					boolean column = (x == cx0 + 1 || x == cx1 - 1) && (z == cz0 + 1 || z == cz1 - 1) || (x - cx0) % 3 == 0 && (z == cz0 + 1 || z == cz1 - 1)
							|| (z - cz0) % 3 == 0 && (x == cx0 + 1 || x == cx1 - 1);
					if (column && !(x == fx || z == mz)) {
						for (int y = 1; y < h; y++) {
							k.b.set(x, y, z, k.c == Culture.ANDALUSI ? s(Blocks.SANDSTONE_WALL) : Bp.log(Blocks.STRIPPED_JUNGLE_LOG, Direction.Axis.Y));
						}
					}
				}
			}
		}
		// A ladder up to the roof from the court; a wind-catcher over the hall in Iran.
		ladder(k, cx1 - 1, cz1 - 1, 1, h + 1, Direction.NORTH);
		k.b.set(cx1 - 1, h + 2, cz1, AIR);
		if (k.c == Culture.PERSIAN || (k.c == Culture.ARAB && k.chance(4))) {
			windcatcher(k, x0 + 1, z0 + 1, h + 2, 4 + k.r.nextInt(3), tone);
		}
		room(k, x0 + 1, z0 + 1, x1 - 1, cz0 - 1, 1, k.wealth >= 1 ? Use.HALL : Use.LIVING);
		room(k, x0 + 1, cz1 + 1, x1 - 1, z1 - 1, 1, Use.STORE);
		room(k, x0 + 1, cz0 + 1, cx0 - 1, cz1 - 1, 1, Use.SLEEPING);
		room(k, cx1 + 1, cz0 + 1, x1 - 1, cz1 - 1, 1, Use.KITCHEN);
	}

	/**
	 * A narrow tall house of two or three storeys: a shop or store below, rooms above with a lattice bay over the street,
	 * a roof terrace with a parapet reached by a ladder, a wind-catcher in Iran.
	 */
	static void tallHouse(K k, int x0, int z0, int x1, int z1, int floors) {
		floors = Math.max(1, Math.min(3, floors));
		BlockState tone = southTone(k);
		int top = 4 * floors - 1;
		found(k, x0, z0, x1, z1, s(Blocks.CUT_SANDSTONE));
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, top);
		box(k, x0, z0, x1, z1, 0, top, new Wall(tone, s(Blocks.CUT_SANDSTONE), 0, null, 0, s(Blocks.CUT_SANDSTONE), null));
		for (int i = 0; i < floors; i++) {
			floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 4 * i, i == 0 ? s(Blocks.PACKED_MUD) : s(k.w.planks));
			if (i > 0) {
				windows(k, x0, z0, x1, z1, 4 * i + 2, LATTICE, 3, k.w.trapdoor, null, Direction.SOUTH, Direction.NORTH);
			}
			// Beam ends showing in the wall where each floor lies.
			if (i > 0 && k.chance(2)) {
				for (int x = x0 + 1; x < x1; x += 2) {
					k.soft(x, 4 * i, z1 + 1, Bp.trapdoor(k.w.trapdoor, Direction.SOUTH, true, false));
				}
			}
		}
		int mid = (x0 + x1) / 2;
		boolean shop = k.town && k.chance(2);
		int dx = x1 - x0 >= 6 ? x0 + 2 : mid;
		grandDoor(k, dx, z1, tone);
		if (shop) {
			for (int x = dx + 2; x < x1; x++) {
				k.b.set(x, 1, z1, AIR);
				k.b.set(x, 2, z1, AIR);
				k.soft(x, 3, z1 + 1, Bp.slab(Blocks.WOOL_SLAB.pick(k.chance(2) ? DyeColor.WHITE : DyeColor.RED), true));
			}
		} else {
			windows(k, x0, z0, x1, z1, 3, BARS, 4, k.w.trapdoor, null, Direction.SOUTH);
		}
		if (floors >= 2 && x1 - x0 >= 6 && k.in(mid, z1 + 1)) {
			bay(k, mid - 1, Math.min(x1 - 1, mid + 2), 4 * (floors - 1), z1, s(k.w.planks));
		}
		terrace(k, x0, z0, x1, z1, top + 1, tone, k.c == Culture.ANDALUSI || k.wealth >= 2);
		ladder(k, x1 - 1, z0 + 1, 1, top + 1, Direction.SOUTH);
		if (k.c == Culture.PERSIAN && x1 - x0 >= 5) {
			windcatcher(k, x0, z0, top + 2, 4 + k.r.nextInt(3), tone);
		} else if (k.chance(3) && x1 - x0 >= 6 && z1 - z0 >= 6) {
			// A summer room on the roof.
			int ux1 = x0 + 4, uz1 = z0 + 3;
			box(k, x0, z0, ux1, uz1, top + 2, top + 4, plain(tone));
			hollow(k, x0 + 1, z0 + 1, ux1 - 1, uz1 - 1, top + 2, top + 4);
			k.b.fill(x0, top + 5, z0, ux1, top + 5, uz1, tone);
			door(k, k.w.door, ux1, top + 2, z0 + 2, Direction.EAST);
			window(k, x0 + 2, top + 3, uz1, Direction.SOUTH, LATTICE, k.w.trapdoor);
			room(k, x0 + 1, z0 + 1, ux1 - 1, uz1 - 1, top + 2, Use.SLEEPING);
		}
		for (int i = 0; i < floors; i++) {
			Use use = i == 0 ? (shop ? Use.SHOP : floors == 1 ? Use.LIVING : Use.KITCHEN) : i == floors - 1 && floors == 3 ? Use.SLEEPING : Use.LIVING;
			room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 4 * i + 1, use);
		}
	}

	/**
	 * A house of one or two domed rooms in mud brick (Iran and Syria); {@code beehive}: tall corbelled cones instead of
	 * round domes.
	 */
	static void domeHouse(K k, int x0, int z0, int x1, int z1, boolean beehive) {
		BlockState tone = k.c == Culture.PERSIAN && k.chance(2) ? s(Blocks.MUD_BRICKS) : southTone(k);
		int w = x1 - x0 + 1;
		int cells = w >= 11 ? 2 : 1;
		int cw = cells == 2 ? (w + 1) / 2 : w;
		found(k, x0, z0, x1, z1, s(Blocks.COBBLESTONE));
		int h = 3;
		for (int c = 0; c < cells; c++) {
			int ax = x0 + c * (cw - 1), bx = Math.min(x1, ax + cw - 1);
			int az = z0, bz = Math.min(z1, z0 + (bx - ax));
			hollow(k, ax + 1, az + 1, bx - 1, bz - 1, 1, h + 8);
			floor(k, ax + 1, az + 1, bx - 1, bz - 1, 0, s(Blocks.PACKED_MUD));
			box(k, ax, az, bx, bz, 0, h, plain(tone));
			double cx = (ax + bx) / 2.0, cz = (az + bz) / 2.0, rad = (bx - ax) / 2.0;
			for (int x = ax; x <= bx; x++) {
				for (int z = az; z <= bz; z++) {
					double dd = Math.hypot(x - cx, z - cz);
					if (dd > rad - 1.3) {
						k.b.set(x, h + 1, z, tone);
					}
				}
			}
			if (beehive) {
				for (int L = 0; L < 12; L++) {
					double rr = rad - 0.55 * L;
					if (rr < 0.6) {
						k.b.set((int) Math.round(cx), h + 2 + L, (int) Math.round(cz), tone);
						break;
					}
					for (int x = ax; x <= bx; x++) {
						for (int z = az; z <= bz; z++) {
							double dd = Math.hypot(x - cx, z - cz);
							if (dd <= rr && dd > rr - 1.2) {
								k.b.set(x, h + 2 + L, z, tone);
							}
						}
					}
				}
			} else {
				k.b.dome(cx, h + 1, cz, rad, tone, Bp.slab(parapetSlab(k, tone), false));
			}
			if (cells == 2 && c == 1) {
				k.b.set(ax, 1, (az + bz) / 2, AIR);
				k.b.set(ax, 2, (az + bz) / 2, AIR);
			}
			int mid = (ax + bx) / 2;
			if (c == 0) {
				door(k, k.w.door, mid, 1, bz, Direction.SOUTH);
			} else {
				window(k, mid, 2, bz, Direction.SOUTH, LATTICE, k.w.trapdoor);
			}
			window(k, ax, 2, (az + bz) / 2, Direction.WEST, SLIT, k.w.trapdoor);
			room(k, ax + 1, az + 1, bx - 1, bz - 1, 1, c == 0 ? Use.LIVING : (k.chance(2) ? Use.SLEEPING : Use.STORE));
		}
	}

	/** A flat-roofed house of mud brick with a stair up to the roof terrace (villages of the South). */
	static void mudHouse(K k, int x0, int z0, int x1, int z1) {
		BlockState tone = southTone(k);
		found(k, x0, z0, x1, z1, s(Blocks.COBBLESTONE));
		hollow(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, 3);
		floor(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 0, s(Blocks.PACKED_MUD));
		box(k, x0, z0, x1, z1, 0, 3, plain(tone));
		boolean longX = x1 - x0 >= z1 - z0;
		int mid = (x0 + x1) / 2;
		int xp = longX && x1 - x0 >= 9 ? x0 + (x1 - x0) / 2 + 1 : -1;
		if (xp > 0) {
			partition(k, false, xp, z0, z1, 0, 3, tone, k.w.door, (z0 + z1) / 2);
			mid = (x0 + xp) / 2;
		}
		door(k, k.w.door, mid, 1, z1, Direction.SOUTH);
		windows(k, x0, z0, x1, z1, 2, k.chance(2) ? LATTICE : SLIT, 4, k.w.trapdoor, null, Direction.SOUTH, Direction.EAST, Direction.WEST);
		terrace(k, x0, z0, x1, z1, 4, tone, k.c == Culture.ANDALUSI && k.chance(2));
		// Roof beams sticking out under the parapet.
		if (k.c != Culture.ANDALUSI && k.chance(2)) {
			for (int x = x0 + 1; x < x1; x += 2) {
				k.soft(x, 3, z1 + 1, Bp.trapdoor(k.w.trapdoor, Direction.SOUTH, true, false));
			}
		}
		// A stair along the side up to the roof.
		boolean east = k.chance(2);
		int ox = east ? x1 + 1 : x0 - 1;
		if (k.in(ox, z1) && z1 - z0 >= 4) {
			for (int i = 0; i < 4; i++) {
				int zz = z1 - i;
				k.setIn(ox, 1 + i, zz, Bp.stairs(parapetSlab(k, tone) == Blocks.MUD_BRICK_SLAB ? Blocks.MUD_BRICK_STAIRS : Blocks.SANDSTONE_STAIRS,
						Direction.NORTH, false));
				for (int y = 0; y < 1 + i; y++) {
					k.setIn(ox, y, zz, tone);
				}
			}
			k.b.set(east ? x1 : x0, 5, z1 - 3, AIR);
		}
		if (xp > 0) {
			room(k, x0 + 1, z0 + 1, xp - 1, z1 - 1, 1, Use.LIVING);
			room(k, xp + 1, z0 + 1, x1 - 1, z1 - 1, 1, k.chance(2) ? Use.SLEEPING : Use.KITCHEN);
		} else {
			room(k, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, Use.LIVING);
		}
	}

	// =================================================================================================================
	// The steppe: yurts, wagons, winter dugouts

	static final DyeColor[] FELT = {DyeColor.WHITE, DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.BROWN, DyeColor.WHITE};
	static final DyeColor[] BAND = {DyeColor.RED, DyeColor.BLUE, DyeColor.ORANGE, DyeColor.BLACK, DyeColor.RED};

	/** A felt yurt of radius 3..5 with a coloured band, a crown open to the sky and the household inside. */
	static void yurt(K k, int cx, int cz, int rad) {
		rad = Math.max(3, Math.min(5, rad));
		DyeColor felt = FELT[k.r.nextInt(FELT.length)], band = BAND[k.r.nextInt(BAND.length)];
		DyeColor[] rug = {DyeColor.RED, DyeColor.ORANGE, DyeColor.BROWN, DyeColor.YELLOW};
		double R = rad + 0.4;
		for (int x = -rad - 1; x <= rad + 1; x++) {
			for (int z = -rad - 1; z <= rad + 1; z++) {
				double dd = Math.sqrt(x * x + z * z);
				if (dd > R + 0.2) {
					continue;
				}
				int X = cx + x, Z = cz + z;
				k.b.set(X, -1, Z, s(Blocks.COARSE_DIRT));
				k.b.set(X, 0, Z, s(Blocks.PACKED_MUD));
				boolean ring = dd > R - 0.9;
				for (int y = 1; y <= rad + 2; y++) {
					k.b.set(X, y, Z, AIR);
				}
				if (ring) {
					k.b.set(X, 1, Z, s(Blocks.WOOL.pick(felt)));
					k.b.set(X, 2, Z, s(Blocks.WOOL.pick(band)));
				}
				// The roof: felt over the rafters, rising in rings to the crown.
				int tier = (int) Math.floor((R + 0.2 - dd) / 1.3);
				double crown = 1.1;
				if (dd > crown) {
					k.b.set(X, 3 + tier, Z, Bp.slab(Blocks.WOOL_SLAB.pick(felt), false));
				}
			}
		}
		k.b.set(cx, 3 + (int) Math.floor((R + 0.2) / 1.3), cz, AIR);
		door(k, k.chance(2) ? Blocks.SPRUCE_DOOR : Blocks.ACACIA_DOOR, cx, 1, cz + rad, Direction.SOUTH);
		// The hearth under the crown, the place of honour with the chests opposite the door.
		k.b.set(cx, 1, cz, s(Blocks.CAMPFIRE));
		for (int dx = -1; dx <= 1; dx++) {
			k.b.set(cx + dx, 1, cz - rad + 1, Bp.facing(Blocks.CHEST, Direction.SOUTH));
		}
		Block bed = Blocks.BED.pick(k.chance(2) ? DyeColor.RED : DyeColor.ORANGE);
		k.b.bed(bed, cx - rad + 2, 1, cz + 1, Direction.WEST);
		if (rad >= 4) {
			k.b.bed(bed, cx + rad - 2, 1, cz + 1, Direction.EAST);
			k.b.set(cx - 2, 1, cz - rad + 2, Bp.facing(Blocks.LOOM, Direction.EAST));
			k.b.set(cx + 2, 1, cz - rad + 2, Interior.barrel());
		}
		for (int x = -rad; x <= rad; x++) {
			for (int z = -rad; z <= rad; z++) {
				double dd = Math.sqrt(x * x + z * z);
				if (dd <= R - 0.9 && k.empty(cx + x, 1, cz + z) && !(x == 0 && z == rad - 1)) {
					DyeColor col = Math.abs(x) + Math.abs(z) <= 1 ? rug[0] : (x + z) % 3 == 0 ? rug[1] : rug[(Math.abs(x) + 2) % 4];
					k.b.set(cx + x, 1, cz + z, s(Blocks.CARPET.pick(col)));
				}
			}
		}
	}

	/** A kibitka: a small felt tent on a two-wheeled wagon, its shafts to the front. */
	static void kibitka(K k, int x0, int z0) {
		DyeColor felt = FELT[k.r.nextInt(FELT.length)];
		Block wheel = Blocks.DARK_OAK_LOG;
		for (int z = z0; z <= z0 + 4; z++) {
			for (int x = x0 + 1; x <= x0 + 3; x++) {
				k.setIn(x, 1, z, s(k.w.planks));
			}
		}
		for (int z : new int[]{z0 + 1, z0 + 3}) {
			k.setIn(x0, 1, z, log(wheel, Direction.Axis.X));
			k.setIn(x0 + 4, 1, z, log(wheel, Direction.Axis.X));
		}
		k.setIn(x0 + 1, 1, z0 + 5, s(k.w.fence));
		k.setIn(x0 + 3, 1, z0 + 5, s(k.w.fence));
		k.setIn(x0 + 1, 1, z0 + 6, s(k.w.fence));
		k.setIn(x0 + 3, 1, z0 + 6, s(k.w.fence));
		for (int x = x0 + 1; x <= x0 + 3; x++) {
			for (int z = z0; z <= z0 + 3; z++) {
				boolean edge = x == x0 + 1 || x == x0 + 3 || z == z0;
				k.setIn(x, 2, z, edge ? s(Blocks.WOOL.pick(felt)) : s(Blocks.CARPET.pick(DyeColor.RED)));
				k.setIn(x, 3, z, edge ? s(Blocks.WOOL.pick(felt)) : AIR);
				k.setIn(x, 4, z, Bp.slab(Blocks.WOOL_SLAB.pick(felt), false));
			}
		}
		k.setIn(x0 + 2, 2, z0 + 1, Bp.facing(Blocks.CHEST, Direction.SOUTH));
		k.setIn(x0 + 2, 5, z0 + 1, Bp.slab(Blocks.WOOL_SLAB.pick(felt), false));
	}

	// =================================================================================================================
	// Yards: things that stand about a house

	static BlockState leaves(Block l) {
		return l.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
	}

	static boolean north(Culture c) {
		return c == Culture.RUS || c == Culture.BALT_FINN || c == Culture.NORSE || c == Culture.WEST_SLAV || c == Culture.CELTIC;
	}

	static boolean south(Culture c) {
		return c == Culture.ANDALUSI || c == Culture.ARAB || c == Culture.PERSIAN || c == Culture.BYZANTINE;
	}

	/** Garden beds with paths between them, on the free cells of the rectangle. */
	static void garden(K k, int x0, int z0, int x1, int z1) {
		boolean warm = south(k.c) || k.c == Culture.CAUCASUS;
		boolean irrigated = k.c == Culture.ARAB || k.c == Culture.PERSIAN || k.c == Culture.ANDALUSI;
		boolean rows = k.chance(2);
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (!k.in(x, z) || k.used[x][z] || k.b.has(x, 0, z)) {
					continue;
				}
				int lane = rows ? x - x0 : z - z0;
				int bed = lane / 3;
				if (lane % 3 == 2) {
					k.b.set(x, 0, z, irrigated && bed % 2 == 0 ? s(Blocks.WATER) : s(Blocks.DIRT_PATH));
					if (irrigated && bed % 2 == 0) {
						k.b.set(x, -1, z, s(Blocks.PACKED_MUD));
					}
					k.used[x][z] = true;
					continue;
				}
				k.b.set(x, 0, z, s(Blocks.FARMLAND));
				int kind = Math.floorMod(bed * 3 + (rows ? z0 : x0), 5);
				BlockState crop = switch (kind) {
					case 0 -> Blocks.CARROTS.defaultBlockState().setValue(BlockStateProperties.AGE_7, 3 + k.r.nextInt(5));
					case 1 -> Blocks.BEETROOTS.defaultBlockState().setValue(BlockStateProperties.AGE_3, 1 + k.r.nextInt(3));
					case 2 -> Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 2 + k.r.nextInt(6));
					case 3 -> warm ? (k.chance(3) ? s(Blocks.MELON) : Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7))
							: Blocks.CARROTS.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7);
					default -> k.chance(4) ? s(Blocks.CORNFLOWER) : k.chance(3) ? s(Blocks.POPPY) : s(Blocks.SHORT_GRASS);
				};
				if (crop.getBlock() == Blocks.MELON || crop.getBlock() == Blocks.CORNFLOWER || crop.getBlock() == Blocks.POPPY
						|| crop.getBlock() == Blocks.SHORT_GRASS) {
					k.b.set(x, 0, z, s(Blocks.DIRT));
				}
				k.b.set(x, 1, z, crop);
				k.used[x][z] = true;
			}
		}
	}

	/** A fruit or shade tree fit for the land: apple, birch, cherry, cypress, a date palm. */
	static void tree(K k, int x, int z) {
		if (!k.in(x, z)) {
			return;
		}
		k.b.set(x, 0, z, s(Blocks.GRASS_BLOCK));
		boolean palm = (k.c == Culture.ARAB || k.c == Culture.ANDALUSI || k.c == Culture.PERSIAN) && k.chance(2);
		if (palm) {
			int h = 5 + k.r.nextInt(2);
			for (int y = 1; y <= h; y++) {
				k.b.set(x, y, z, log(Blocks.JUNGLE_LOG, Direction.Axis.Y));
			}
			BlockState lf = leaves(Blocks.JUNGLE_LEAVES);
			k.b.set(x, h + 1, z, lf);
			for (Direction d : HORIZ) {
				k.soft(x + d.getStepX(), h, z + d.getStepZ(), lf);
				k.soft(x + 2 * d.getStepX(), h - 1, z + 2 * d.getStepZ(), lf);
			}
			return;
		}
		boolean cypress = (south(k.c) || k.c == Culture.CAUCASUS) && k.chance(2);
		if (cypress) {
			for (int y = 1; y <= 6; y++) {
				k.b.set(x, y, z, y <= 1 ? log(Blocks.SPRUCE_LOG, Direction.Axis.Y) : leaves(Blocks.SPRUCE_LEAVES));
			}
			return;
		}
		Block lg = k.c == Culture.RUS || k.c == Culture.BALT_FINN ? (k.chance(2) ? Blocks.BIRCH_LOG : Blocks.OAK_LOG) : Blocks.OAK_LOG;
		Block lv = lg == Blocks.BIRCH_LOG ? Blocks.BIRCH_LEAVES : south(k.c) && k.chance(2) ? Blocks.CHERRY_LEAVES : Blocks.OAK_LEAVES;
		if (lv == Blocks.CHERRY_LEAVES) {
			lg = Blocks.CHERRY_LOG;
		}
		int h = 3 + k.r.nextInt(2);
		for (int y = 1; y <= h; y++) {
			k.b.set(x, y, z, log(lg, Direction.Axis.Y));
		}
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = -1; dy <= 1; dy++) {
					int dd = Math.abs(dx) + Math.abs(dz) + Math.abs(dy);
					if (dd <= 3 && !(dx == 0 && dz == 0 && dy < 1)) {
						k.soft(x + dx, h + dy, z + dz, leaves(lv));
					}
				}
			}
		}
	}

	static void haystack(K k, int x, int z) {
		for (int dx = 0; dx <= 1; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				k.soft(x + dx, 1, z + dz, s(Blocks.HAY_BLOCK));
			}
		}
		k.soft(x, 2, z, s(Blocks.HAY_BLOCK));
		k.soft(x + 1, 2, z + 1, s(Blocks.HAY_BLOCK));
		k.soft(x, 3, z, s(k.w.fence));
	}

	static void woodpile(K k, int x, int z, int len, boolean alongX) {
		Block lg = k.w.log;
		for (int i = 0; i < len; i++) {
			int xx = alongX ? x + i : x, zz = alongX ? z : z + i;
			k.soft(xx, 1, zz, log(lg, alongX ? Direction.Axis.Z : Direction.Axis.X));
			if (i > 0 && i < len - 1) {
				k.soft(xx, 2, zz, log(lg, alongX ? Direction.Axis.Z : Direction.Axis.X));
			}
		}
		// The chopping block.
		k.soft(alongX ? x + len : x, 1, alongX ? z : z + len, log(k.w.stripped, Direction.Axis.Y));
	}

	/** A well: a stone ring over water, a roof on two posts or a sweep (журавль, gémeskút). */
	static void well(K k, int x, int z) {
		for (int dx = 0; dx <= 2; dx++) {
			for (int dz = 0; dz <= 2; dz++) {
				boolean mid = dx == 1 && dz == 1;
				for (int y = -4; y <= 0; y++) {
					k.setIn(x + dx, y, z + dz, mid ? s(Blocks.WATER) : s(Blocks.COBBLESTONE));
				}
				k.setIn(x + dx, 1, z + dz, mid ? AIR : (south(k.c) ? s(Blocks.SANDSTONE_WALL) : s(Blocks.COBBLESTONE_WALL)));
			}
		}
		boolean sweep = (k.c == Culture.RUS || k.c == Culture.MAGYAR || k.c == Culture.BALT_FINN || k.c == Culture.WEST_SLAV) && k.chance(2);
		if (sweep) {
			for (int y = 1; y <= 3; y++) {
				k.setIn(x + 3, y, z + 1, log(k.w.log, Direction.Axis.Y));
			}
			k.setIn(x + 3, 4, z + 1, s(k.w.fence));
			k.setIn(x + 2, 5, z + 1, s(k.w.fence));
			k.setIn(x + 1, 5, z + 1, s(k.w.fence));
			k.setIn(x + 1, 4, z + 1, s(Blocks.IRON_CHAIN));
			k.setIn(x + 4, 4, z + 1, s(k.w.fence));
		} else {
			for (int y = 2; y <= 3; y++) {
				k.setIn(x, y, z + 1, s(k.w.fence));
				k.setIn(x + 2, y, z + 1, s(k.w.fence));
			}
			k.setIn(x + 1, 3, z + 1, log(k.w.log, Direction.Axis.X));
			k.setIn(x + 1, 2, z + 1, s(Blocks.IRON_CHAIN));
			Block rs = south(k.c) ? Blocks.BRICK_STAIRS : k.w.stairs;
			for (int dx = 0; dx <= 2; dx++) {
				k.setIn(x + dx, 4, z, Bp.stairs(rs, Direction.SOUTH, false));
				k.setIn(x + dx, 4, z + 2, Bp.stairs(rs, Direction.NORTH, false));
				k.setIn(x + dx, 4, z + 1, Bp.slab(k.w.slab, false));
			}
		}
	}

	/** A cart with two wheels, standing in the yard with something in it. */
	static void cart(K k, int x, int z) {
		Wood wd = k.w;
		for (int i = 0; i < 3; i++) {
			k.soft(x + i, 1, z, Bp.slab(wd.slab, true));
		}
		k.soft(x + 1, 1, z - 1, Bp.trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.NORTH, false, true));
		k.soft(x + 1, 1, z + 1, Bp.trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH, false, true));
		k.soft(x + 3, 1, z, s(wd.fence));
		k.soft(x + 1, 2, z, k.chance(2) ? s(Blocks.HAY_BLOCK) : Interior.barrel());
	}

	static void doghouse(K k, int x, int z) {
		Wood wd = k.w;
		k.soft(x, 1, z, s(wd.planks));
		k.soft(x + 1, 1, z, s(wd.planks));
		k.soft(x, 2, z, Bp.stairs(wd.stairs, Direction.WEST, false));
		k.soft(x + 1, 2, z, Bp.stairs(wd.stairs, Direction.EAST, false));
		k.soft(x, 2, z + 1, Bp.stairs(wd.stairs, Direction.WEST, false));
		k.soft(x + 1, 2, z + 1, Bp.stairs(wd.stairs, Direction.EAST, false));
		k.soft(x, 1, z + 1, s(wd.planks));
	}

	static void beehives(K k, int x, int z, int n) {
		for (int i = 0; i < n; i++) {
			k.soft(x + 2 * i, 1, z, log(k.w.log, Direction.Axis.Y));
			k.soft(x + 2 * i, 2, z, Bp.facing(Blocks.BEEHIVE, Direction.SOUTH));
		}
	}

	/** A pen for beasts: a fence ring with a gate, hay, a trough and a little shelter. */
	static void pen(K k, int x0, int z0, int x1, int z1) {
		Wood wd = k.w;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
				if (edge) {
					k.soft(x, 1, z, x == (x0 + x1) / 2 && z == z1 ? Bp.facing(wd.gate, Direction.SOUTH) : s(wd.fence));
				} else {
					k.soft(x, 0, z, k.chance(3) ? s(Blocks.MUD) : s(Blocks.COARSE_DIRT));
				}
			}
		}
		k.soft(x0 + 1, 1, z0 + 1, s(Blocks.HAY_BLOCK));
		if (x1 - x0 >= 4) {
			k.soft(x0 + 2, 1, z0 + 1, Interior.water());
			k.soft(x1 - 1, 1, z0 + 1, s(Blocks.COMPOSTER));
			for (int x = x0; x <= x1; x++) {
				k.soft(x, 3, z0, Bp.slab(wd.slab, false));
				k.soft(x, 3, z0 + 1, Bp.slab(wd.slab, false));
			}
			k.soft(x0, 2, z0 + 1, s(wd.fence));
			k.soft(x1, 2, z0 + 1, s(wd.fence));
			k.soft(x0, 2, z0, s(wd.fence));
			k.soft(x1, 2, z0, s(wd.fence));
		}
	}

	/** An open shed on posts with a lean-to roof and firewood under it. */
	static void shed(K k, int x0, int z0, int x1, int z1) {
		Wood wd = k.w;
		for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
			for (int y = 1; y <= 2; y++) {
				k.setIn(c[0], y, c[1], log(wd.log, Direction.Axis.Y));
			}
		}
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				k.setIn(x, 3, z, z == z1 ? Bp.slab(wd.slab, false) : s(wd.planks));
				k.setIn(x, 0, z, s(Blocks.COARSE_DIRT));
			}
			if (x > x0 && x < x1) {
				k.setIn(x, 1, z0, log(wd.log, Direction.Axis.X));
				k.setIn(x, 2, z0, k.chance(2) ? log(wd.log, Direction.Axis.X) : AIR);
			}
		}
		k.soft(x0 + 1, 1, z1, k.chance(2) ? s(Blocks.CRAFTING_TABLE) : Interior.barrel());
	}

	/** A bread oven in the yard (tannur, peč): a mound of clay round a fire. */
	static void oven(K k, int x, int z) {
		k.soft(x, 1, z, Bp.facing(Blocks.SMOKER, Direction.SOUTH));
		k.soft(x + 1, 1, z, s(Blocks.MUD_BRICKS));
		k.soft(x, 2, z, Bp.slab(Blocks.MUD_BRICK_SLAB, false));
		k.soft(x + 1, 2, z, Bp.slab(Blocks.MUD_BRICK_SLAB, false));
	}

	/** Big storage jars standing about, half buried (pithoi, kvevri). */
	static void jars(K k, int x, int z, int n) {
		for (int i = 0; i < n; i++) {
			k.soft(x + i, 1, z, s(Blocks.DECORATED_POT));
		}
	}

	/** A stack of cut peat drying (Celtic lands) or of dung bricks (the steppe). */
	static void peat(K k, int x, int z) {
		BlockState pt = k.c == Culture.STEPPE ? s(Blocks.PACKED_MUD) : s(Blocks.MUD);
		for (int dx = 0; dx <= 2; dx++) {
			k.soft(x + dx, 1, z, pt);
			if (dx == 1) {
				k.soft(x + dx, 2, z, pt);
			}
		}
	}

	static void hitchingPost(K k, int x, int z) {
		k.soft(x, 1, z, s(k.w.fence));
		k.soft(x + 2, 1, z, s(k.w.fence));
		k.soft(x, 2, z, log(k.w.log, Direction.Axis.X));
		k.soft(x + 1, 2, z, log(k.w.log, Direction.Axis.X));
		k.soft(x + 2, 2, z, log(k.w.log, Direction.Axis.X));
		k.soft(x + 1, 1, z + 1, Interior.water());
	}

	// =================================================================================================================
	// Plots

	static final class Yards {
		private Yards() {
		}

		static int odd(int w) {
			return w % 2 == 0 ? w - 1 : w;
		}

		/**
		 * A w×d rectangle in the yard: pos 0 front-left, 1 front-right, 2 front-middle, 3 back-left, 4 back-right,
		 * 5 back-middle (front = towards the street, leaving room for the fence and a path).
		 */
		static int[] at(K k, int w, int d, int pos) {
			w = Math.max(5, Math.min(w, k.W - 4));
			d = Math.max(5, Math.min(d, k.D - 6));
			int x0 = switch (pos % 3) {
				case 0 -> 2;
				case 1 -> k.W - 2 - w;
				default -> (k.W - w) / 2;
			};
			x0 = Math.max(1, x0);
			int z1 = pos < 3 ? k.D - 4 : 2 + d - 1;
			return new int[]{x0, z1 - d + 1, x0 + w - 1, z1};
		}

		static void mark(K k, int[] r, int m) {
			k.mark(r[0] - m, r[1] - m, r[2] + m, r[3] + m);
		}

		// ------------------------------------------------------------------------------------------------------------
		// Villages

		static void village(K k) {
			int[] h = switch (k.c) {
				case RUS, BALT_FINN -> rus(k);
				case WEST_SLAV, MAGYAR -> slavMagyar(k);
				case LATIN -> latin(k);
				case CELTIC -> celtic(k);
				case NORSE -> norse(k);
				case BYZANTINE -> byz(k);
				case CAUCASUS -> cauc(k);
				case ANDALUSI, ARAB, PERSIAN -> islam(k);
				case STEPPE -> steppe(k);
			};
			if (h != null) {
				mark(k, h, 2);
			}
			int gx = gateX(k);
			if (k.c != Culture.STEPPE) {
				path(k, gx);
			}
			outbuildings(k);
			int gz = Math.max(2, Math.min(k.D / 3, k.D - 8));
			if (k.c != Culture.STEPPE) {
				garden(k, 1, 1, k.W - 2, gz);
			}
			props(k);
			fence(k, gx);
		}

		static int gateX(K k) {
			int gx = k.W / 2;
			if (!k.doors.isEmpty()) {
				int[] d = k.doors.get(0);
				gx = d[0] + d[3];
			}
			return Math.max(2, Math.min(k.W - 3, gx));
		}

		/** A beaten path from the gate to the door. */
		static void path(K k, int gx) {
			int tx = gx, tz = k.D / 2;
			if (!k.doors.isEmpty()) {
				int[] d = k.doors.get(0);
				tx = d[0] + d[3];
				tz = d[2] + d[4];
			}
			tz = Math.max(1, Math.min(k.D - 1, tz));
			tx = Math.max(1, Math.min(k.W - 2, tx));
			BlockState ps = south(k.c) || k.c == Culture.CAUCASUS ? s(Blocks.PACKED_MUD) : k.chance(3) ? s(Blocks.GRAVEL) : s(Blocks.DIRT_PATH);
			for (int z = k.D - 1; z >= tz; z--) {
				pathCell(k, gx, z, ps);
			}
			for (int x = Math.min(gx, tx); x <= Math.max(gx, tx); x++) {
				pathCell(k, x, tz, ps);
			}
		}

		static void pathCell(K k, int x, int z, BlockState ps) {
			if (!k.in(x, z)) {
				return;
			}
			if (!k.b.has(x, 0, z)) {
				k.b.set(x, 0, z, ps);
			}
			k.used[x][z] = true;
		}

		/** The fence, palisade or wall round the yard, and the gate in it. */
		static void fence(K k, int gx) {
			int W = k.W, D = k.D;
			Wood wd = k.w;
			int style;
			switch (k.c) {
				case STEPPE -> {
					return;
				}
				case ANDALUSI, ARAB, PERSIAN -> style = 3;
				case BYZANTINE, CAUCASUS -> style = 2;
				case CELTIC -> style = k.r.nextInt(5) < 3 ? 2 : 0;
				case LATIN -> style = k.chance(3) ? 2 : 0;
				case RUS, BALT_FINN -> style = k.wealth >= 2 ? 1 : 0;
				default -> style = 0;
			}
			BlockState tone = style == 3 ? southTone(k) : null;
			int gw = (style == 1 || (k.wealth >= 1 && style == 0 && k.chance(2))) ? 2 : 1;
			int g0 = Math.max(1, Math.min(W - 1 - gw, gx - (gw == 2 ? k.r.nextInt(2) : 0)));
			for (int x = 0; x < W; x++) {
				for (int z = 0; z < D; z++) {
					if (x != 0 && x != W - 1 && z != 0 && z != D - 1) {
						continue;
					}
					if (z == D - 1 && x >= g0 && x < g0 + gw) {
						continue;
					}
					switch (style) {
						case 1 -> {
							k.soft(x, 1, z, log(wd.log, Direction.Axis.Y));
							k.soft(x, 2, z, log(wd.log, Direction.Axis.Y));
							k.soft(x, 3, z, (x + z) % 2 == 0 ? s(wd.fence) : AIR);
						}
						case 2 -> {
							if (k.c == Culture.CELTIC || k.c == Culture.LATIN) {
								k.soft(x, 1, z, k.chance(4) ? s(Blocks.MOSSY_COBBLESTONE_WALL) : s(Blocks.COBBLESTONE_WALL));
							} else {
								k.soft(x, 1, z, k.chance(5) ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.COBBLESTONE));
								k.soft(x, 2, z, Bp.slab(Blocks.COBBLESTONE_SLAB, false));
							}
						}
						case 3 -> {
							k.soft(x, 1, z, tone);
							k.soft(x, 2, z, tone);
							k.soft(x, 3, z, Bp.slab(parapetSlab(k, tone), false));
						}
						default -> k.soft(x, 1, z, s(wd.fence));
					}
				}
			}
			int z = D - 1;
			if (style == 3) {
				k.b.set(g0, 1, z, AIR);
				door(k, wd.door, g0, 1, z, Direction.SOUTH);
				k.soft(g0, 3, z, tone);
				return;
			}
			for (int x = g0; x < g0 + gw; x++) {
				k.b.set(x, 1, z, wd.gate.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
				k.b.set(x, 2, z, AIR);
			}
			boolean roofed = (k.c == Culture.RUS || k.c == Culture.BALT_FINN || k.c == Culture.WEST_SLAV) && (k.wealth >= 1 || style == 1);
			if (roofed || style == 1) {
				// Gate posts with a little roof over them.
				int a = g0 - 1, b = g0 + gw;
				for (int y = 1; y <= 3; y++) {
					k.setIn(a, y, z, log(wd.log, Direction.Axis.Y));
					k.setIn(b, y, z, log(wd.log, Direction.Axis.Y));
				}
				for (int x = a; x <= b; x++) {
					k.setIn(x, 4, z, log(wd.log, Direction.Axis.X));
					k.setIn(x, 5, z, Bp.slab(k.p.roofSlab, false));
				}
				k.setIn(a, 3, z, log(wd.log, Direction.Axis.Y));
			}
		}

		/** Barns, sheds, bath-houses and the like, each where it fits. */
		static void outbuildings(K k) {
			int W = k.W, D = k.D;
			List<int[]> want = new ArrayList<>();
			switch (k.c) {
				case RUS, BALT_FINN -> {
					want.add(new int[]{0, k.pick(6, 7), k.pick(6, 7)});
					if (k.c == Culture.BALT_FINN || k.chance(2)) {
						want.add(new int[]{1, 4, 5});
					}
					if (k.chance(2)) {
						want.add(new int[]{2, 4, 4});
					}
				}
				case WEST_SLAV, MAGYAR -> {
					want.add(new int[]{k.chance(2) ? 0 : 3, k.pick(6, 7), k.pick(5, 6)});
					if (k.chance(2)) {
						want.add(new int[]{2, 4, 4});
					}
				}
				case LATIN, CELTIC -> {
					want.add(new int[]{k.chance(3) ? 3 : 0, k.pick(6, 7), k.pick(6, 7)});
					if (k.chance(2)) {
						want.add(new int[]{4, 5, 3});
					}
				}
				case NORSE -> {
					want.add(new int[]{5, k.pick(5, 6), k.pick(6, 7)});
					want.add(new int[]{2, 4, 4});
				}
				case BYZANTINE, CAUCASUS, ANDALUSI, ARAB, PERSIAN -> {
					want.add(new int[]{3, k.pick(6, 8), k.pick(5, 6)});
				}
				case STEPPE -> {
					want.add(new int[]{3, k.pick(6, 8), k.pick(6, 8)});
				}
			}
			for (int[] w : want) {
				int[] sp = k.spot(w[1], w[2], 1, 1, W - 2, D - 2);
				if (sp == null) {
					sp = k.spot(Math.max(4, w[1] - 1), Math.max(4, w[2] - 1), 1, 1, W - 2, D - 2);
					if (sp == null) {
						continue;
					}
					w[1] = Math.max(4, w[1] - 1);
					w[2] = Math.max(4, w[2] - 1);
				}
				int x0 = sp[0], z0 = sp[1], x1 = x0 + w[1] - 1, z1 = z0 + w[2] - 1;
				k.mark(x0 - 1, z0 - 1, x1 + 1, z1 + 1);
				switch (w[0]) {
					case 0 -> Houses.barn(k.b, k.p, x0, z0, x1, z1);
					case 1 -> banya(k, x0, z0, x1, z1);
					case 2 -> stilted(k, x0, z0, x1, z1);
					case 3 -> pen(k, x0, z0, x1, z1);
					case 4 -> shed(k, x0, z0, x1, z1);
					default -> {
						if (k.c == Culture.NORSE) {
							sunken(k, x0, z0, x1, z1, k.chance(2));
						} else {
							pen(k, x0, z0, x1, z1);
						}
					}
				}
			}
		}

		/** Small things about the yard. */
		static void props(K k) {
			int W = k.W, D = k.D;
			List<Integer> l = new ArrayList<>();
			switch (k.c) {
				case RUS, BALT_FINN, WEST_SLAV -> l.addAll(List.of(0, 1, 2, 3, 4, 5, 6, 7));
				case MAGYAR -> l.addAll(List.of(0, 1, 2, 4, 9, 7));
				case LATIN -> l.addAll(List.of(0, 1, 2, 3, 4, 5, 7));
				case CELTIC -> l.addAll(List.of(0, 1, 10, 4, 7));
				case NORSE -> l.addAll(List.of(1, 0, 3, 4, 7));
				case BYZANTINE, CAUCASUS -> l.addAll(List.of(2, 8, 9, 7, 7, 0));
				case ANDALUSI, ARAB, PERSIAN -> l.addAll(List.of(8, 9, 7, 7, 2));
				case STEPPE -> l.addAll(List.of(11, 10, 0, 9));
			}
			java.util.Collections.shuffle(l, k.r);
			int n = 3 + k.r.nextInt(3);
			for (int i = 0; i < l.size() && n > 0; i++) {
				int what = l.get(i);
				int w = switch (what) {
					case 0 -> 2;
					case 1 -> 5;
					case 2 -> 5;
					case 3 -> 5;
					case 4 -> 4;
					case 5 -> 2;
					case 6 -> 5;
					case 8 -> 2;
					case 9 -> 3;
					case 10 -> 3;
					case 11 -> 3;
					default -> 1;
				};
				int d = switch (what) {
					case 0 -> 2;
					case 2 -> 3;
					case 3 -> 3;
					case 5 -> 2;
					case 6 -> 1;
					case 11 -> 2;
					default -> 1;
				};
				int[] sp = k.spot(w, d, 1, 1, W - 2, D - 2);
				if (sp == null) {
					continue;
				}
				int x = sp[0], z = sp[1];
				k.mark(x - 1, z - 1, x + w, z + d);
				switch (what) {
					case 0 -> haystack(k, x, z);
					case 1 -> woodpile(k, x, z, 4, true);
					case 2 -> well(k, x, z);
					case 3 -> cart(k, x, z + 1);
					case 4 -> beehives(k, x, z, 2);
					case 5 -> doghouse(k, x, z);
					case 6 -> beehives(k, x, z, 3);
					case 8 -> jars(k, x, z, 2);
					case 9 -> oven(k, x, z);
					case 10 -> peat(k, x, z);
					case 11 -> hitchingPost(k, x, z);
					default -> tree(k, x, z);
				}
				n--;
			}
		}

		// ---- the house of each people in its village yard

		static int[] rus(K k) {
			int W = k.W, D = k.D;
			int n = k.r.nextInt(100);
			boolean balt = k.c == Culture.BALT_FINN;
			int pos = k.pick(0, 1, 0, 1, 3, 4);
			Direction side = pos % 3 == 0 ? Direction.EAST : Direction.WEST;
			int[] r;
			if (k.wealth == 2 && W >= 15 && D >= 20 && n < 50) {
				r = at(k, odd(9), 10, pos);
				rusTall(k, r[0], r[1], r[2], r[3], side, true);
			} else if (W >= 14 && n < (balt ? 30 : 15)) {
				r = at(k, Math.min(12, W - 4), k.pick(9, 10), pos % 3 == 0 ? 0 : 1);
				koshel(k, r[0], r[1], r[2], r[3], pos % 3 == 0);
			} else if (n < 32 && D >= 18) {
				r = at(k, 7, 9, pos);
				rusTall(k, r[0], r[1], r[2], r[3], side, false);
			} else if (n < 50 && k.wealth >= 1) {
				r = at(k, 7, 11, pos);
				rusHouse(k, r[0], r[1], odd(r[2] - r[0] + 1) + r[0] - 1, r[3], side, 3);
			} else if (n < 70) {
				r = at(k, 7, 10, pos);
				rusHouse(k, r[0], r[1], odd(r[2] - r[0] + 1) + r[0] - 1, r[3], side, 2);
			} else {
				r = at(k, k.chance(3) ? 5 : 7, k.pick(7, 8), pos);
				rusHouse(k, r[0], r[1], odd(r[2] - r[0] + 1) + r[0] - 1, r[3], side, k.wealth == 0 && k.chance(2) ? 0 : 1);
			}
			return r;
		}

		static int[] slavMagyar(K k) {
			int n = k.r.nextInt(100);
			int pos = k.pick(0, 1, 2, 3, 4);
			int[] r;
			if (k.c == Culture.MAGYAR && k.wealth == 0 && n < 30) {
				r = at(k, k.pick(5, 6), k.pick(6, 7), pos);
				sunken(k, r[0], r[1], r[2], r[3], false);
				mark(k, r, 2);
				if (k.chance(2)) {
					int[] y = k.spot(9, 9, 1, 1, k.W - 2, k.D - 2);
					if (y != null) {
						mark(k, new int[]{y[0], y[1], y[0] + 8, y[1] + 8}, 1);
						yurt(k, y[0] + 4, y[1] + 4, 3);
					}
				}
			} else if (n < 55) {
				r = at(k, k.pick(9, 10, 11), k.pick(6, 7), k.pick(0, 1, 2, 5));
				mazanka(k, r[0], r[1], r[2], r[3], k.wealth >= 2);
			} else if (n < 75) {
				r = at(k, 7, k.pick(9, 10), pos);
				mazanka(k, r[0], r[1], r[2], r[3], false);
			} else if (k.c == Culture.WEST_SLAV && n < 90) {
				r = at(k, 7, 10, pos);
				porchHouse(k, r[0], r[1], r[2], r[3]);
			} else {
				r = at(k, 7, k.pick(8, 10), pos);
				rusHouse(k, r[0], r[1], r[2], r[3], pos % 3 == 0 ? Direction.EAST : Direction.WEST, k.chance(2) ? 2 : 1);
			}
			return r;
		}

		static int[] latin(K k) {
			int n = k.r.nextInt(100);
			int pos = k.pick(0, 1, 2, 3, 4, 5);
			int[] r;
			boolean italian = k.chance(3);
			if (k.wealth == 2 && n < 50) {
				r = at(k, k.pick(9, 10), k.pick(9, 10), pos);
				stoneHouse(k, r[0], r[1], r[2], r[3], 2, false, italian, true);
			} else if (n < 40) {
				r = at(k, k.W >= 16 ? k.pick(11, 13) : 9, k.pick(6, 7), k.pick(0, 1, 2, 5));
				cottage(k, r[0], r[1], r[2], r[3], r[2] - r[0] >= 9, false);
			} else if (n < 70) {
				r = at(k, k.pick(7, 8, 9), k.pick(7, 8, 9), pos);
				frameHouse(k, r[0], r[1], r[2], r[3], 1 + k.r.nextInt(2), true, false);
			} else {
				r = at(k, k.pick(7, 8, 9), k.pick(7, 8), pos);
				stoneHouse(k, r[0], r[1], r[2], r[3], k.wealth >= 1 ? 2 : 1, false, italian, false);
			}
			mark(k, r, 2);
			if (k.chance(2)) {
				int[] t = k.spot(1, 1, 1, 1, k.W - 2, k.D - 5);
				if (t != null) {
					k.mark(t[0] - 2, t[1] - 2, t[0] + 2, t[1] + 2);
					tree(k, t[0], t[1]);
				}
			}
			return r;
		}

		static int[] celtic(K k) {
			int n = k.r.nextInt(100);
			int pos = k.pick(0, 1, 2, 3, 4, 5);
			if (n < 55) {
				int rad = k.W >= 15 ? k.pick(4, 5) : k.pick(3, 4);
				int[] r = at(k, 2 * rad + 1, 2 * rad + 1, pos);
				roundhouse(k, (r[0] + r[2]) / 2, (r[1] + r[3]) / 2, rad, k.chance(2));
				mark(k, r, 1);
				int[] s2 = k.spot(7, 7, 1, 1, k.W - 2, k.D - 2);
				if (s2 != null && k.chance(2)) {
					k.mark(s2[0] - 1, s2[1] - 1, s2[0] + 7, s2[1] + 7);
					roundhouse(k, s2[0] + 3, s2[1] + 3, 3, k.chance(2));
				}
				return r;
			}
			int[] r = at(k, k.pick(9, 11), k.pick(6, 7), k.pick(0, 1, 2, 5));
			cottage(k, r[0], r[1], r[2], r[3], k.chance(2), n < 85);
			return r;
		}

		static int[] norse(K k) {
			int n = k.r.nextInt(100);
			int[] r;
			if (n < 65) {
				int len = Math.min(k.D - 6, k.pick(14, 16, 18, 20));
				r = at(k, k.pick(7, 9), len, k.pick(0, 1, 3, 4));
				longhouse(k, r[0], r[1], r[2], r[3], true);
			} else {
				r = at(k, 7, k.pick(8, 9), k.pick(0, 1, 2));
				turfHouse(k, r[0], r[1], r[2], r[3]);
			}
			return r;
		}

		static int[] byz(K k) {
			int[] r = at(k, k.pick(7, 9), k.pick(7, 8), k.pick(0, 1, 2, 5));
			byzHouse(k, r[0], r[1], r[2], r[3], k.chance(3) ? 1 : 2, false, true);
			mark(k, r, 2);
			for (int i = 0; i < 2; i++) {
				int[] t = k.spot(1, 1, 1, 1, k.W - 2, k.D - 5);
				if (t != null) {
					k.mark(t[0] - 1, t[1] - 1, t[0] + 1, t[1] + 1);
					tree(k, t[0], t[1]);
				}
			}
			return r;
		}

		static int[] cauc(K k) {
			int n = k.r.nextInt(100);
			int pos = k.pick(0, 1, 3, 4);
			int[] r;
			if (n < 40) {
				r = at(k, k.pick(7, 9), k.pick(10, 11), pos);
				flatHouse(k, r[0], r[1], r[2], r[3]);
			} else if (n < 70) {
				int s = k.pick(9, 11);
				r = at(k, s, s, pos);
				darbazi(k, r[0], r[1], r[2], r[3]);
			} else {
				r = at(k, 7, 7, pos);
				flatHouse(k, r[0], r[1], r[2], r[3]);
			}
			mark(k, r, 2);
			if ((k.wealth >= 1 || n >= 70) && k.W >= 13) {
				int[] t = k.spot(5, 5, 1, 1, k.W - 2, k.D - 2);
				if (t != null) {
					k.mark(t[0] - 1, t[1] - 1, t[0] + 5, t[1] + 5);
					tower(k, t[0], t[1], 5);
				}
			}
			return r;
		}

		static int[] islam(K k) {
			int n = k.r.nextInt(100);
			int[] r;
			if (k.W >= 15 && k.D >= 18 && (k.wealth >= 1 || n < 30)) {
				r = at(k, k.W - 4, Math.min(k.D - 6, k.W - 2), 2);
				courtyard(k, r[0], r[1], r[2], r[3]);
			} else if ((k.c == Culture.PERSIAN || k.c == Culture.ARAB) && n < 55) {
				int w = k.W >= 15 ? 11 : 7;
				r = at(k, w, 7, k.pick(0, 1, 2, 5));
				domeHouse(k, r[0], r[1], r[2], r[3], k.c == Culture.ARAB && k.chance(2));
			} else {
				r = at(k, k.pick(7, 9, 11), k.pick(6, 7), k.pick(0, 1, 2, 5));
				mudHouse(k, r[0], r[1], r[2], r[3]);
			}
			mark(k, r, 2);
			for (int i = 0; i < 1 + k.r.nextInt(2); i++) {
				int[] t = k.spot(1, 1, 1, 1, k.W - 2, k.D - 5);
				if (t != null) {
					k.mark(t[0] - 2, t[1] - 2, t[0] + 2, t[1] + 2);
					tree(k, t[0], t[1]);
				}
			}
			return r;
		}

		static int[] steppe(K k) {
			int rad = k.W >= 15 ? k.pick(4, 5) : 4;
			int[] r = at(k, 2 * rad + 1, 2 * rad + 1, k.pick(3, 4, 5));
			yurt(k, (r[0] + r[2]) / 2, (r[1] + r[3]) / 2, rad);
			mark(k, r, 1);
			if (k.chance(3)) {
				int[] s2 = k.spot(6, 7, 1, 1, k.W - 2, k.D - 2);
				if (s2 != null) {
					k.mark(s2[0] - 1, s2[1] - 1, s2[0] + 6, s2[1] + 7);
					sunken(k, s2[0], s2[1], s2[0] + 5, s2[1] + 6, true);
				}
			} else {
				int[] s2 = k.spot(7, 7, 1, 1, k.W - 2, k.D - 2);
				if (s2 != null) {
					k.mark(s2[0] - 1, s2[1] - 1, s2[0] + 7, s2[1] + 7);
					yurt(k, s2[0] + 3, s2[1] + 3, 3);
				}
			}
			int[] s3 = k.spot(5, 7, 1, 1, k.W - 2, k.D - 2);
			if (s3 != null && k.chance(2)) {
				k.mark(s3[0] - 1, s3[1] - 1, s3[0] + 5, s3[1] + 7);
				kibitka(k, s3[0], s3[1]);
			}
			return r;
		}

		// ------------------------------------------------------------------------------------------------------------
		// Towns

		static void town(K k) {
			int W = k.W, D = k.D;
			int z1 = D - 2;
			int back = D >= 14 ? 3 + k.r.nextInt(3) : D >= 11 ? 2 : 1;
			int z0 = Math.max(1, z1 - (D - 2 - back) + 1);
			int depth = z1 - z0 + 1;
			int wealth = k.wealth;
			int n = k.r.nextInt(100);
			boolean fenced = true;
			switch (k.c) {
				case RUS, BALT_FINN -> {
					int hw = Math.max(5, odd(Math.min(wealth == 2 ? 9 : 7, W - 3)));
					boolean right = k.chance(2);
					int x0 = right ? W - hw - (W - hw >= 4 ? 1 : 0) : (W - hw >= 4 ? 1 : 0);
					int x1 = x0 + hw - 1;
					Direction side = right ? Direction.WEST : Direction.EAST;
					if (wealth == 2 && depth >= 8 && hw >= 7) {
						rusTall(k, x0, z0, x1, z1, side, true);
					} else if (wealth >= 1 && depth >= 8 && n < 45) {
						rusTall(k, x0, z0, x1, z1, side, false);
					} else if (wealth >= 1) {
						rusHouse(k, x0, z0, x1, z1, side, depth >= 9 && n < 75 ? 3 : 2);
					} else {
						rusHouse(k, x0, z0, x1, z1, side, n < 40 ? 0 : n < 70 ? 1 : 2);
					}
					k.mark(x0 - 1, z0 - 1, x1 + 1, z1 + 1);
					// The passage beside the house is fenced from the street with a gate.
					int px0 = right ? 0 : x1 + 1, px1 = right ? x0 - 1 : W - 1;
					for (int x = px0; x <= px1; x++) {
						k.soft(x, 1, D - 1, s(k.w.fence));
					}
					int gx = right ? Math.max(0, x0 - 2) : Math.min(W - 1, x1 + 2);
					if (k.in(gx, D - 1)) {
						k.b.set(gx, 1, D - 1, k.w.gate.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
						if (wealth >= 1) {
							// Carved gate posts.
							for (int dx = -1; dx <= 1; dx += 2) {
								if (k.in(gx + dx, D - 1) && !k.used[gx + dx][D - 1]) {
									k.b.set(gx + dx, 1, D - 1, log(k.w.log, Direction.Axis.Y));
									k.b.set(gx + dx, 2, D - 1, log(k.w.log, Direction.Axis.Y));
									k.b.set(gx + dx, 3, D - 1, s(k.w.fence));
								}
							}
						}
					}
					for (int x = Math.min(px0, px1); x <= Math.max(px0, px1); x++) {
						for (int z = z0; z < D - 1; z++) {
							pathCell(k, x, z, s(Blocks.COARSE_DIRT));
						}
					}
				}
				case WEST_SLAV -> {
					int x0 = 0, x1 = W - 1;
					if (n < 40) {
						frameHouse(k, x0, z0, x1, z1, wealth >= 1 ? 2 : 1, false, wealth == 1 && k.chance(2));
					} else if (n < 70 && depth >= 8) {
						porchHouse(k, W >= 9 ? 1 : 0, z0, W >= 9 ? W - 2 : W - 1, z1);
					} else {
						mazanka(k, x0, z0, x1, z1, false);
					}
				}
				case MAGYAR -> {
					if (W >= 11 && n < 50) {
						int dz0 = Math.max(z0, z1 - 6);
						mazanka(k, 0, dz0, W - 1, z1, wealth >= 2);
					} else {
						mazanka(k, W >= 9 ? 1 : 0, z0, W >= 9 ? W - 2 : W - 1, z1, false);
					}
				}
				case LATIN -> {
					boolean italian = k.chance(3);
					if (wealth == 2) {
						if (n < 55) {
							stoneHouse(k, 0, z0, W - 1, z1, 2 + k.r.nextInt(2), false, italian, n < 35);
						} else {
							frameHouse(k, 0, z0, W - 1, z1, 3, true, false);
						}
					} else if (wealth == 1) {
						if (n < 30) {
							stoneHouse(k, 0, z0, W - 1, z1, 2, k.chance(2), italian, false);
						} else {
							frameHouse(k, 0, z0, W - 1, z1, 2 + (k.chance(3) ? 1 : 0), k.chance(2), k.chance(2));
						}
					} else {
						if (n < 35 && W >= 9) {
							cottage(k, 0, z0, W - 1, z1, false, false);
						} else {
							frameHouse(k, 0, z0, W - 1, z1, 1 + k.r.nextInt(2), k.chance(3), k.chance(3));
						}
					}
				}
				case CELTIC -> {
					if (n < 40 && W >= 9 && depth >= 9) {
						int rad = Math.min(4, Math.min((W - 3) / 2, (depth - 1) / 2));
						roundhouse(k, W / 2, z1 - rad, rad, true);
					} else if (n < 70) {
						cottage(k, 0, z0, W - 1, z1, false, k.chance(2));
					} else {
						frameHouse(k, 0, z0, W - 1, z1, 1, true, false);
					}
				}
				case NORSE -> {
					int x0 = W >= 9 ? 1 : 0, x1 = W >= 9 ? W - 2 : W - 1;
					if (n < 50 && depth >= 9) {
						longhouse(k, x0, z0, x1, z1, false);
					} else if (n < 75 && depth >= 7) {
						turfHouse(k, Math.max(1, x0), z0, Math.min(W - 2, x1), z1);
					} else {
						cottage(k, x0, z0, x1, z1, false, false);
					}
				}
				case BYZANTINE -> {
					fenced = false;
					byzHouse(k, 0, z0, W - 1, z1, 1 + wealth + (k.chance(3) ? 1 : 0), wealth <= 1 && k.chance(2), false);
				}
				case CAUCASUS -> {
					if (n < 40) {
						flatHouse(k, 0, z0, W - 1, z1);
					} else if (n < 70 || W < 11) {
						byzHouse(k, 0, z0, W - 1, z1, 2, wealth == 0 && k.chance(2), false);
					} else {
						flatHouse(k, 0, z0, W - 6, z1);
						tower(k, W - 5, z0, 5);
					}
				}
				case ANDALUSI, ARAB, PERSIAN -> {
					fenced = false;
					if (W >= 11 && D >= 11 && (wealth == 2 || n < 35)) {
						courtyard(k, 0, 0, W - 1, D - 1);
						return;
					} else if (k.c == Culture.PERSIAN && wealth == 0 && n < 60) {
						domeHouse(k, 0, z0, W - 1, z1, false);
					} else {
						tallHouse(k, 0, z0, W - 1, z1, 2 + (wealth >= 1 && k.chance(2) ? 1 : 0));
					}
				}
				case STEPPE -> {
					if (n < 50 || W < 9) {
						mudHouse(k, 0, z0, W - 1, z1);
					} else {
						int rad = Math.min(4, Math.min((W - 2) / 2, (depth - 1) / 2));
						yurt(k, W / 2, z1 - rad, rad);
					}
				}
			}
			k.mark(0, z0 - 1, W - 1, D - 1);
			backyard(k, z0, fenced);
		}

		/** The little yard behind a town house: a shed, firewood, a well or a garden bed, a fence or a wall round it. */
		static void backyard(K k, int z0, boolean fenced) {
			int W = k.W;
			int zb = z0 - 2;
			if (zb < 0) {
				return;
			}
			boolean wall = south(k.c) || k.c == Culture.CAUCASUS || k.c == Culture.BYZANTINE;
			BlockState tone = wall ? (south(k.c) && k.c != Culture.BYZANTINE ? southTone(k) : s(Blocks.COBBLESTONE)) : null;
			for (int x = 0; x < W; x++) {
				if (wall) {
					k.soft(x, 1, 0, tone);
					k.soft(x, 2, 0, tone);
				} else if (fenced) {
					k.soft(x, 1, 0, s(k.w.fence));
				}
			}
			for (int z = 0; z <= zb; z++) {
				for (int x : new int[]{0, W - 1}) {
					if (wall) {
						k.soft(x, 1, z, tone);
						k.soft(x, 2, z, tone);
					} else if (fenced) {
						k.soft(x, 1, z, s(k.w.fence));
					}
				}
			}
			if (zb >= 2 && W >= 7) {
				int pick = k.r.nextInt(4);
				switch (pick) {
					case 0 -> garden(k, 1, 1, W - 2, zb);
					case 1 -> {
						woodpile(k, 1, 1, Math.min(4, W - 4), true);
						if (W >= 10) {
							well(k, W - 5, 1);
						}
					}
					case 2 -> {
						if (W >= 8 && zb >= 3) {
							shed(k, 1, 1, Math.min(W - 3, 5), Math.min(zb, 3));
						}
					}
					default -> tree(k, W / 2, Math.max(1, zb / 2));
				}
			} else if (zb >= 1) {
				k.soft(1, 1, 1, Interior.barrel());
				if (W >= 6) {
					woodpile(k, 2, zb, Math.min(3, W - 5), true);
				}
			}
		}
	}
}
