package com.stasdoto.medieval.construct;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import com.stasdoto.medieval.history.Culture;

/**
 * Furniture for the rooms of a house, in the way of each people: the great stove and the red corner of the Slavs, the open
 * hearth, trestle table and curtained beds of the West, the side platforms and long fire of the North, couches, icons and
 * amphorae of Byzantium, cushions, low tables, lamps and carpets of the Islamic world, felt and chests of the steppe.
 * <p>
 * A room only fills cells that are empty (never set or set to air), keeps the cells inside every door, the foot of every
 * ladder and staircase and a walkway free, and keeps every free cell reachable from the doors.
 */
final class Interior {
	private Interior() {
	}

	enum Use { LIVING, SLEEPING, KITCHEN, STORE, HALL, SHOP, WORKSHOP }

	/** A wood family: logs, boards and joinery of one tree. */
	enum Wood {
		OAK(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_STAIRS, Blocks.OAK_SLAB, Blocks.OAK_FENCE,
				Blocks.OAK_FENCE_GATE, Blocks.OAK_DOOR, Blocks.OAK_TRAPDOOR, Blocks.OAK_SHELF),
		SPRUCE(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_FENCE,
				Blocks.SPRUCE_FENCE_GATE, Blocks.SPRUCE_DOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.SPRUCE_SHELF),
		DARK_OAK(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB,
				Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_FENCE_GATE, Blocks.DARK_OAK_DOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.DARK_OAK_SHELF),
		BIRCH(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG, Blocks.BIRCH_PLANKS, Blocks.BIRCH_STAIRS, Blocks.BIRCH_SLAB, Blocks.BIRCH_FENCE,
				Blocks.BIRCH_FENCE_GATE, Blocks.BIRCH_DOOR, Blocks.BIRCH_TRAPDOOR, Blocks.BIRCH_SHELF),
		JUNGLE(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG, Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_SLAB, Blocks.JUNGLE_FENCE,
				Blocks.JUNGLE_FENCE_GATE, Blocks.JUNGLE_DOOR, Blocks.JUNGLE_TRAPDOOR, Blocks.JUNGLE_SHELF),
		ACACIA(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG, Blocks.ACACIA_PLANKS, Blocks.ACACIA_STAIRS, Blocks.ACACIA_SLAB, Blocks.ACACIA_FENCE,
				Blocks.ACACIA_FENCE_GATE, Blocks.ACACIA_DOOR, Blocks.ACACIA_TRAPDOOR, Blocks.ACACIA_SHELF);

		final Block log, stripped, planks, stairs, slab, fence, gate, door, trapdoor, shelf;

		Wood(Block log, Block stripped, Block planks, Block stairs, Block slab, Block fence, Block gate, Block door, Block trapdoor, Block shelf) {
			this.log = log;
			this.stripped = stripped;
			this.planks = planks;
			this.stairs = stairs;
			this.slab = slab;
			this.fence = fence;
			this.gate = gate;
			this.door = door;
			this.trapdoor = trapdoor;
			this.shelf = shelf;
		}
	}

	/** Furnishing traditions. */
	private enum Style { SLAV, WEST, NORSE, BYZ, CAUC, ISLAM, STEPPE }

	private static Style style(Culture c) {
		return switch (c) {
			case RUS, BALT_FINN, WEST_SLAV -> Style.SLAV;
			case LATIN, CELTIC, MAGYAR -> Style.WEST;
			case NORSE -> Style.NORSE;
			case BYZANTINE -> Style.BYZ;
			case CAUCASUS -> Style.CAUC;
			case ANDALUSI, ARAB, PERSIAN -> Style.ISLAM;
			case STEPPE -> Style.STEPPE;
		};
	}

	static Wood woodFor(Culture c, Random r) {
		return switch (c) {
			case RUS, BALT_FINN -> r.nextInt(3) == 0 ? Wood.OAK : r.nextInt(5) == 0 ? Wood.BIRCH : Wood.SPRUCE;
			case WEST_SLAV, MAGYAR -> r.nextInt(3) == 0 ? Wood.SPRUCE : Wood.OAK;
			case LATIN, CELTIC -> r.nextBoolean() ? Wood.OAK : Wood.DARK_OAK;
			case NORSE -> r.nextBoolean() ? Wood.SPRUCE : Wood.DARK_OAK;
			case BYZANTINE, CAUCASUS -> r.nextBoolean() ? Wood.DARK_OAK : Wood.OAK;
			case ANDALUSI, ARAB, PERSIAN -> r.nextInt(3) == 0 ? Wood.DARK_OAK : r.nextBoolean() ? Wood.JUNGLE : Wood.ACACIA;
			case STEPPE -> r.nextBoolean() ? Wood.BIRCH : Wood.SPRUCE;
		};
	}

	/** Furnishes a room: inner free floor x0..x1 × z0..z1, furniture at height y (floor block at y-1). */
	static void room(Bp b, Culture c, int x0, int z0, int x1, int z1, int y, Use use, int wealth) {
		if (b == null || c == null || use == null) {
			return;
		}
		int ax = Math.min(x0, x1), bx = Math.max(x0, x1), az = Math.min(z0, z1), bz = Math.max(z0, z1);
		if (bx - ax > 96 || bz - az > 96) {
			return;
		}
		try {
			new Room(b, c, ax, az, bx, bz, y, use, Math.max(0, Math.min(3, wealth))).furnish();
		} catch (RuntimeException e) {
			// Furniture must never cost the building it stands in.
		}
	}

	/**
	 * The great Slavic stove (печь) with its corner cell at (cx, cz), growing into the room by (ix, iz): 2 cells across and
	 * {@code len} along Z ({@code longZ}) or X. Its mouth (a smoker) is the far inner cell, facing along the long side; the
	 * cell in front of it must be kept free. A bed of felt (лежанка) lies on top. If {@code chimneyTop} is above the stove,
	 * a chimney rises from the corner cell to that height with smoke on top; otherwise the hut is smoked (по-чёрному).
	 * Sets blocks unconditionally (call it before the room is furnished).
	 */
	static void stove(Bp b, int cx, int cz, int ix, int iz, int y, boolean longZ, int len, int wealth, int chimneyTop) {
		stoveBlocks(b, cx, cz, ix, iz, y, longZ, len, wealth, chimneyTop, true);
	}

	private static void stoveBlocks(Bp b, int cx, int cz, int ix, int iz, int y, boolean longZ, int len, int wealth, int chimneyTop, boolean force) {
		ix = ix >= 0 ? 1 : -1;
		iz = iz >= 0 ? 1 : -1;
		len = Math.max(1, Math.min(3, len));
		BlockState mat = wealth >= 2 ? (b.r.nextBoolean() ? Blocks.CALCITE.defaultBlockState() : Blocks.BRICKS.defaultBlockState())
				: wealth == 1 ? (b.r.nextBoolean() ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.MUD_BRICKS.defaultBlockState())
				: Blocks.MUD_BRICKS.defaultBlockState();
		int ax = longZ ? 2 : len, az = longZ ? len : 2;
		for (int i = 0; i < ax; i++) {
			for (int j = 0; j < az; j++) {
				int x = cx + i * ix, z = cz + j * iz;
				place(b, x, y, z, mat, force);
				place(b, x, y + 1, z, mat, force);
			}
		}
		int mx = cx + (ax - 1) * ix, mz = cz + (az - 1) * iz;
		Direction md = longZ ? dir(0, iz) : dir(ix, 0);
		place(b, mx, y, mz, Bp.facing(Blocks.SMOKER, md), true);
		DyeColor felt = b.r.nextInt(3) == 0 ? DyeColor.BROWN : b.r.nextBoolean() ? DyeColor.RED : DyeColor.WHITE;
		boolean chimney = chimneyTop >= y + 2;
		for (int i = 0; i < ax; i++) {
			for (int j = 0; j < az; j++) {
				int x = cx + i * ix, z = cz + j * iz;
				if ((x == mx && z == mz) || (chimney && x == cx && z == cz)) {
					continue;
				}
				place(b, x, y + 2, z, Blocks.CARPET.pick(felt).defaultBlockState(), false);
			}
		}
		if (chimney) {
			BlockState pipe = wealth >= 2 ? Blocks.BRICKS.defaultBlockState() : Blocks.MUD_BRICKS.defaultBlockState();
			for (int yy = y + 2; yy <= chimneyTop; yy++) {
				b.set(cx, yy, cz, pipe);
			}
			b.set(cx, chimneyTop + 1, cz, Blocks.CAMPFIRE.defaultBlockState());
		}
	}

	private static void place(Bp b, int x, int y, int z, BlockState s, boolean force) {
		BlockState o = b.get(x, y, z);
		if (force || o == null || o.isAir()) {
			b.set(x, y, z, s);
		}
	}

	static Direction dir(int dx, int dz) {
		return dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
	}

	/** Clockwise turn of a horizontal direction (seen from above). */
	static Direction cw(Direction d) {
		return switch (d) {
			case NORTH -> Direction.EAST;
			case EAST -> Direction.SOUTH;
			case SOUTH -> Direction.WEST;
			default -> Direction.NORTH;
		};
	}

	static BlockState candle(int n, DyeColor col) {
		Block c = col == null ? Blocks.CANDLE : Blocks.DYED_CANDLE.pick(col);
		return c.defaultBlockState().setValue(BlockStateProperties.CANDLES, Math.max(1, Math.min(4, n))).setValue(BlockStateProperties.LIT, true);
	}

	static BlockState barrel() {
		return Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP);
	}

	static BlockState water() {
		return Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3);
	}

	private static final BlockState[] POTTED = {Blocks.POTTED_POPPY.defaultBlockState(), Blocks.POTTED_CORNFLOWER.defaultBlockState(),
			Blocks.POTTED_DANDELION.defaultBlockState(), Blocks.POTTED_RED_TULIP.defaultBlockState(), Blocks.POTTED_FERN.defaultBlockState(),
			Blocks.POTTED_AZURE_BLUET.defaultBlockState(), Blocks.POTTED_ALLIUM.defaultBlockState(), Blocks.POTTED_OXEYE_DAISY.defaultBlockState()};

	static BlockState flowerPot(Random r) {
		return POTTED[r.nextInt(POTTED.length)];
	}

	private static final Direction[] HORIZ = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

	/** A floor cell against a wall; {@code wall} points from the cell to the wall. */
	private record Spot(int x, int z, Direction wall) {
	}

	// =====================================================================================================================

	private static final class Room {
		final Bp b;
		final Culture c;
		final Style st;
		final Random r;
		final Use use;
		final int x0, z0, x1, z1, y, W, D, wealth;
		final Wood wood;
		final DyeColor[] cols;
		final boolean[][] res, taken;
		boolean[][] reach;
		final List<int[]> seeds = new ArrayList<>();
		final List<int[]> needs = new ArrayList<>();
		/** Table tops and other cells where small things may stand at y + 1. */
		final List<int[]> tops = new ArrayList<>();
		final List<Spot> walls = new ArrayList<>();
		int hx = Integer.MIN_VALUE, hz = Integer.MIN_VALUE;

		Room(Bp b, Culture c, int x0, int z0, int x1, int z1, int y, Use use, int wealth) {
			this.b = b;
			this.c = c;
			this.st = style(c);
			this.r = b.r;
			this.use = use;
			this.x0 = x0;
			this.z0 = z0;
			this.x1 = x1;
			this.z1 = z1;
			this.y = y;
			this.W = x1 - x0 + 1;
			this.D = z1 - z0 + 1;
			this.wealth = wealth;
			this.wood = woodFor(c, r);
			this.cols = colours();
			this.res = new boolean[W][D];
			this.taken = new boolean[W][D];
			findEntries();
			findWalls();
			reach = flood();
		}

		// ------------------------------------------------------------------ cells

		boolean inside(int x, int z) {
			return x >= x0 && x <= x1 && z >= z0 && z <= z1;
		}

		boolean empty(int x, int yy, int z) {
			BlockState s = b.get(x, yy, z);
			return s == null || s.isAir();
		}

		boolean solid(int x, int yy, int z) {
			return !empty(x, yy, z);
		}

		boolean hole(int x, int z) {
			BlockState s = b.get(x, y - 1, z);
			if (s == null) {
				return y - 1 > 0;
			}
			return s.isAir() || s.getBlock() == Blocks.WATER || s.getBlock() == Blocks.LAVA;
		}

		boolean put(int x, int yy, int z, BlockState s) {
			if (s == null || !empty(x, yy, z)) {
				return false;
			}
			b.set(x, yy, z, s);
			return true;
		}

		void reserve(int x, int z) {
			if (inside(x, z)) {
				res[x - x0][z - z0] = true;
			}
		}

		boolean walk(int x, int z) {
			return inside(x, z) && !taken[x - x0][z - z0] && empty(x, y, z) && empty(x, y + 1, z);
		}

		boolean claimable(int x, int z) {
			return inside(x, z) && !res[x - x0][z - z0] && !taken[x - x0][z - z0] && empty(x, y, z) && !hole(x, z);
		}

		int area() {
			return W * D;
		}

		static boolean isDoor(BlockState s) {
			return s != null && s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && s.hasProperty(BlockStateProperties.DOOR_HINGE);
		}

		static boolean isGate(BlockState s) {
			return s != null && s.hasProperty(BlockStateProperties.IN_WALL);
		}

		static boolean isLadder(BlockState s) {
			return s != null && s.getBlock() == Blocks.LADDER;
		}

		static boolean isStairs(BlockState s) {
			return s != null && s.hasProperty(BlockStateProperties.STAIRS_SHAPE) && s.hasProperty(BlockStateProperties.HALF)
					&& s.getValue(BlockStateProperties.HALF) == Half.BOTTOM;
		}

		static boolean isHearth(BlockState s) {
			if (s == null) {
				return false;
			}
			Block k = s.getBlock();
			return k == Blocks.CAMPFIRE || k == Blocks.FURNACE || k == Blocks.SMOKER || k == Blocks.BLAST_FURNACE;
		}

		// ------------------------------------------------------------------ doors, ladders and walkways

		void findEntries() {
			List<int[]> open = new ArrayList<>();
			int doors = 0;
			for (int x = x0; x <= x1; x++) {
				doors += edge(x, z0 - 1, x, z0, open);
				doors += edge(x, z1 + 1, x, z1, open);
			}
			for (int z = z0; z <= z1; z++) {
				doors += edge(x0 - 1, z, x0, z, open);
				doors += edge(x1 + 1, z, x1, z, open);
			}
			int ring = 2 * (W + D);
			if (!open.isEmpty() && open.size() * 3 <= ring) {
				boolean otherWay = doors > 0;
				for (int[] o : open) {
					if (o[3] != -1) {
						otherWay = true;
					}
				}
				for (int[] o : open) {
					// A shop's open front is where the counter stands, if the room can be entered some other way.
					if (use == Use.SHOP && o[3] == -1 && otherWay) {
						continue;
					}
					entry(o[0], o[1], o[2], o[3]);
				}
			}
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					BlockState s = b.get(x, y, z), up = b.get(x, y + 1, z), below = b.get(x, y - 1, z);
					if (isDoor(s) || isDoor(up) || isGate(s)) {
						for (Direction d : HORIZ) {
							reserve(x + d.getStepX(), z + d.getStepZ());
						}
						seeds.add(new int[]{x + 1, z});
						seeds.add(new int[]{x - 1, z});
						seeds.add(new int[]{x, z + 1});
						seeds.add(new int[]{x, z - 1});
					}
					if (isLadder(s) || isLadder(up)) {
						Direction f = (isLadder(s) ? s : up).getValue(BlockStateProperties.HORIZONTAL_FACING);
						reserve(x + f.getStepX(), z + f.getStepZ());
						seeds.add(new int[]{x + f.getStepX(), z + f.getStepZ()});
					}
					if (isStairs(s)) {
						// The foot of a staircase going up (or of a bench: keeping its seat free does no harm).
						Direction f = s.getValue(BlockStateProperties.HORIZONTAL_FACING);
						reserve(x - f.getStepX(), z - f.getStepZ());
					}
					if (isLadder(below) || isStairs(below)) {
						reserve(x, z);
						seeds.add(new int[]{x, z});
						Direction f = below.getValue(BlockStateProperties.HORIZONTAL_FACING);
						if (isStairs(below)) {
							reserve(x + f.getStepX(), z + f.getStepZ());
						}
					}
					if (hole(x, z)) {
						reserve(x, z);
					}
					if (isHearth(s) && s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
						Direction f = s.getValue(BlockStateProperties.HORIZONTAL_FACING);
						reserve(x + f.getStepX(), z + f.getStepZ());
					}
				}
			}
			// Keep only seeds inside the room.
			seeds.removeIf(sd -> !inside(sd[0], sd[1]));
			if (seeds.isEmpty()) {
				int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
				int best = Integer.MAX_VALUE;
				int[] pick = null;
				for (int x = x0; x <= x1; x++) {
					for (int z = z0; z <= z1; z++) {
						if (walk(x, z)) {
							int dd = Math.abs(x - cx) + Math.abs(z - cz);
							if (dd < best) {
								best = dd;
								pick = new int[]{x, z};
							}
						}
					}
				}
				if (pick != null) {
					reserve(pick[0], pick[1]);
					seeds.add(pick);
				}
			}
		}

		private int edge(int xr, int zr, int xi, int zi, List<int[]> open) {
			BlockState s0 = b.get(xr, y, zr), s1 = b.get(xr, y + 1, zr);
			if (isDoor(s0) || isDoor(s1) || isGate(s0)) {
				entry(xi, zi, xi - xr, zi - zr);
				return 1;
			}
			if (empty(xr, y, zr) && empty(xr, y + 1, zr)) {
				open.add(new int[]{xi, zi, xi - xr, zi - zr});
			}
			return 0;
		}

		private void entry(int x, int z, int dx, int dz) {
			reserve(x, z);
			reserve(x + dx, z + dz);
			seeds.add(new int[]{x, z});
		}

		void findWalls() {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					for (Direction d : HORIZ) {
						int nx = x + d.getStepX(), nz = z + d.getStepZ();
						if (inside(nx, nz)) {
							continue;
						}
						BlockState s = b.get(nx, y, nz);
						if (solid(nx, y, nz) && solid(nx, y + 1, nz) && !isDoor(s) && !isGate(s)) {
							walls.add(new Spot(x, z, d));
						}
					}
				}
			}
		}

		int dist(int x, int z) {
			int best = 999;
			for (int[] s : seeds) {
				best = Math.min(best, Math.abs(x - s[0]) + Math.abs(z - s[1]));
			}
			return best == 999 ? 0 : best;
		}

		/** Wall spots, those far from the doors first. */
		List<Spot> far() {
			List<Spot> l = new ArrayList<>(walls);
			Collections.shuffle(l, r);
			l.sort((a, q) -> Integer.compare(dist(q.x, q.z), dist(a.x, a.z)));
			return l;
		}

		/** Wall spots, those near the doors first. */
		List<Spot> near() {
			List<Spot> l = new ArrayList<>(walls);
			Collections.shuffle(l, r);
			l.sort((a, q) -> Integer.compare(dist(a.x, a.z), dist(q.x, q.z)));
			return l;
		}

		/** Wall spots, those near (x, z) first. */
		List<Spot> nearTo(int x, int z) {
			List<Spot> l = new ArrayList<>(walls);
			Collections.shuffle(l, r);
			l.sort((a, q) -> Integer.compare(Math.abs(a.x - x) + Math.abs(a.z - z), Math.abs(q.x - x) + Math.abs(q.z - z)));
			return l;
		}

		List<Spot> shuffled() {
			List<Spot> l = new ArrayList<>(walls);
			Collections.shuffle(l, r);
			return l;
		}

		boolean wallCell(int x, int z, Direction wall) {
			if (!inside(x, z)) {
				return false;
			}
			int nx = x + wall.getStepX(), nz = z + wall.getStepZ();
			return !inside(nx, nz) && solid(nx, y, nz) && solid(nx, y + 1, nz) && !isDoor(b.get(nx, y, nz));
		}

		boolean[][] flood() {
			boolean[][] v = new boolean[W][D];
			ArrayDeque<int[]> q = new ArrayDeque<>();
			for (int[] s : seeds) {
				if (walk(s[0], s[1]) && !v[s[0] - x0][s[1] - z0]) {
					v[s[0] - x0][s[1] - z0] = true;
					q.add(s);
				}
			}
			while (!q.isEmpty()) {
				int[] p = q.poll();
				for (Direction d : HORIZ) {
					int nx = p[0] + d.getStepX(), nz = p[1] + d.getStepZ();
					if (walk(nx, nz) && !v[nx - x0][nz - z0]) {
						v[nx - x0][nz - z0] = true;
						q.add(new int[]{nx, nz});
					}
				}
			}
			return v;
		}

		boolean touches(int[] xz, boolean[][] v) {
			for (int i = 0; i + 1 < xz.length; i += 2) {
				for (Direction d : HORIZ) {
					int nx = xz[i] + d.getStepX(), nz = xz[i + 1] + d.getStepZ();
					if (inside(nx, nz) && v[nx - x0][nz - z0]) {
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * Takes floor cells (x, z pairs) for a piece of furniture if they are free and the room stays walkable: every cell
		 * reachable before stays reachable, and every piece that must be reached still is.
		 */
		boolean claim(boolean access, int... xz) {
			if (xz.length < 2) {
				return false;
			}
			for (int i = 0; i + 1 < xz.length; i += 2) {
				if (!claimable(xz[i], xz[i + 1])) {
					return false;
				}
				for (int j = 0; j < i; j += 2) {
					if (xz[j] == xz[i] && xz[j + 1] == xz[i + 1]) {
						return false;
					}
				}
			}
			for (int i = 0; i + 1 < xz.length; i += 2) {
				taken[xz[i] - x0][xz[i + 1] - z0] = true;
			}
			boolean[][] nr = flood();
			boolean ok = true;
			for (int i = 0; i < W && ok; i++) {
				for (int j = 0; j < D; j++) {
					if (reach[i][j] && !taken[i][j] && !nr[i][j]) {
						ok = false;
						break;
					}
				}
			}
			if (ok) {
				for (int[] g : needs) {
					if (!touches(g, nr)) {
						ok = false;
						break;
					}
				}
			}
			if (ok && access && !touches(xz, nr)) {
				ok = false;
			}
			if (!ok) {
				for (int i = 0; i + 1 < xz.length; i += 2) {
					taken[xz[i] - x0][xz[i + 1] - z0] = false;
				}
				return false;
			}
			reach = nr;
			if (access) {
				needs.add(xz.clone());
			}
			return true;
		}

		/** Height of the first solid block above the cell (2 = right over the head), or -1 if open to the sky. */
		int ceil(int x, int z) {
			for (int k = 2; k <= 10; k++) {
				if (solid(x, y + k, z)) {
					return k;
				}
			}
			return -1;
		}

		// ------------------------------------------------------------------ colours

		DyeColor[] colours() {
			DyeColor[][] sets = switch (st) {
				case SLAV -> new DyeColor[][]{{DyeColor.RED, DyeColor.WHITE, DyeColor.BROWN}, {DyeColor.WHITE, DyeColor.RED, DyeColor.YELLOW}};
				case WEST -> new DyeColor[][]{{DyeColor.GREEN, DyeColor.RED, DyeColor.BROWN}, {DyeColor.BLUE, DyeColor.WHITE, DyeColor.RED},
						{DyeColor.RED, DyeColor.YELLOW, DyeColor.BROWN}};
				case NORSE -> new DyeColor[][]{{DyeColor.BROWN, DyeColor.WHITE, DyeColor.GRAY}, {DyeColor.LIGHT_GRAY, DyeColor.BROWN, DyeColor.RED}};
				case BYZ -> new DyeColor[][]{{DyeColor.PURPLE, DyeColor.YELLOW, DyeColor.RED}, {DyeColor.RED, DyeColor.YELLOW, DyeColor.MAGENTA}};
				case CAUC -> new DyeColor[][]{{DyeColor.RED, DyeColor.BLUE, DyeColor.ORANGE}, {DyeColor.BROWN, DyeColor.RED, DyeColor.WHITE}};
				case ISLAM -> c == Culture.PERSIAN ? new DyeColor[][]{{DyeColor.RED, DyeColor.BLUE, DyeColor.CYAN}, {DyeColor.BLUE, DyeColor.RED, DyeColor.WHITE}}
						: new DyeColor[][]{{DyeColor.RED, DyeColor.ORANGE, DyeColor.BLUE}, {DyeColor.ORANGE, DyeColor.RED, DyeColor.YELLOW},
								{DyeColor.GREEN, DyeColor.RED, DyeColor.WHITE}};
				case STEPPE -> new DyeColor[][]{{DyeColor.RED, DyeColor.ORANGE, DyeColor.BROWN}, {DyeColor.BROWN, DyeColor.RED, DyeColor.WHITE}};
			};
			return sets[r.nextInt(sets.length)];
		}

		// ------------------------------------------------------------------ the plan of a room

		void furnish() {
			boolean hearth = findHearth();
			switch (use) {
				case LIVING -> living(hearth);
				case KITCHEN -> kitchen(hearth);
				case SLEEPING -> sleeping();
				case STORE -> store();
				case HALL -> hall(hearth);
				case SHOP -> shop();
				case WORKSHOP -> workshop();
			}
			lights();
			floorCover();
		}

		boolean findHearth() {
			// The walls count too: a fire in a wall recess under a chimney.
			for (int x = x0 - 1; x <= x1 + 1; x++) {
				for (int z = z0 - 1; z <= z1 + 1; z++) {
					if (isHearth(b.get(x, y, z)) || isHearth(b.get(x, y - 1, z)) || isHearth(b.get(x, y + 1, z))) {
						hx = x;
						hz = z;
						return true;
					}
				}
			}
			return false;
		}

		void living(boolean hearth) {
			if (!hearth) {
				hearth(false);
			}
			switch (st) {
				case SLAV -> {
					redCorner();
					cauldronByHearth();
					chest(far());
					if (wealth >= 2) {
						chest(far());
					}
					if (r.nextInt(3) > 0) {
						loom();
					}
					highShelves(1 + wealth);
					if (wealth >= 1 || area() >= 30) {
						bed(pickBed(), false);
					}
					if (r.nextInt(3) == 0) {
						cradle();
					}
					benches(area() / 2);
				}
				case WEST -> {
					table(Math.max(2, Math.min(4, Math.max(W, D) / 3)), false);
					int beds = Math.min(3, 1 + wealth);
					for (int i = 0; i < beds; i++) {
						bed(pickBed(), wealth >= 2);
					}
					chest(far());
					if (wealth >= 1) {
						chest(far());
					}
					barrels(1 + r.nextInt(2), false);
					if (r.nextBoolean()) {
						loom();
					}
					cauldronByHearth();
					highShelves(1 + wealth);
					if (wealth >= 2) {
						banners(2);
					}
				}
				case NORSE -> {
					platforms();
					table(2, false);
					chest(far());
					chest(far());
					loom();
					barrels(1 + r.nextInt(2), false);
					if (wealth >= 1) {
						bed(pickBed(), false);
					}
					highShelves(1);
				}
				case BYZ -> {
					couch(Math.min(3, 2 + wealth));
					table(2, false);
					iconCorner(true);
					amphorae(2 + wealth);
					chest(far());
					if (area() >= 20) {
						bed(pickBed(), wealth >= 2);
					}
					highShelves(1);
				}
				case CAUC -> {
					table(2, false);
					amphorae(2);
					chest(far());
					if (r.nextBoolean()) {
						loom();
					}
					if (area() >= 20) {
						bed(pickBed(), false);
					}
					iconCorner(false);
					benches(area() / 2);
				}
				case ISLAM -> {
					cushions(Math.max(3, area() / 3));
					lowTables(1 + area() / 25);
					amphorae(1 + wealth);
					chest(far());
					if (r.nextBoolean()) {
						loom();
					}
					if (wealth >= 1 && area() >= 16) {
						bed(pickBed(), false);
					}
					floorLamps(1 + wealth);
					niches(1 + wealth);
				}
				case STEPPE -> {
					for (int i = 0; i < 2 + wealth; i++) {
						chest(far());
					}
					bed(pickBed(), false);
					cushions(Math.max(2, area() / 4));
					if (r.nextBoolean()) {
						loom();
					}
					barrels(1, false);
				}
			}
		}

		void kitchen(boolean hearth) {
			if (!hearth) {
				hearth(true);
			}
			cauldronByHearth();
			barrels(1 + r.nextInt(2), r.nextBoolean());
			workTable();
			amphorae(st == Style.BYZ || st == Style.ISLAM || st == Style.CAUC ? 2 + r.nextInt(2) : r.nextInt(2));
			highShelves(2);
			chest(near());
			if (r.nextInt(3) == 0) {
				wallBlock(far(), d -> Blocks.COMPOSTER.defaultBlockState(), true);
			}
			if (st == Style.SLAV) {
				benches(area() / 3);
			}
		}

		void sleeping() {
			int n = Math.min(4, 1 + wealth + (area() >= 30 ? 1 : 0));
			if (area() < 6) {
				n = 1;
			}
			boolean canopy = wealth >= 2 && st != Style.ISLAM && st != Style.STEPPE && st != Style.NORSE;
			Block bed = pickBed();
			for (int i = 0; i < n; i++) {
				if (!bed(bed, canopy)) {
					break;
				}
			}
			chest(far());
			if (wealth >= 1) {
				chest(far());
			}
			switch (st) {
				case SLAV -> {
					iconCorner(false);
					benches(4);
					if (r.nextInt(3) == 0) {
						cradle();
					}
				}
				case BYZ -> {
					iconCorner(true);
					amphorae(1);
				}
				case CAUC -> iconCorner(false);
				case ISLAM -> {
					cushions(4);
					niches(1);
					floorLamps(1);
				}
				case WEST -> {
					if (r.nextBoolean()) {
						barrels(1, true);
					}
					if (r.nextInt(3) == 0) {
						cradle();
					}
				}
				default -> {
				}
			}
			highShelves(1);
		}

		void store() {
			BlockState[] goods = goods();
			int budget = Math.max(2, (W * D) * 2 / 3);
			for (Spot s : far()) {
				if (budget <= 0) {
					break;
				}
				BlockState g = goods[r.nextInt(goods.length)];
				if (g.getBlock() == Blocks.CHEST) {
					g = Bp.facing(Blocks.CHEST, s.wall.getOpposite());
				}
				if (claim(true, s.x, s.z)) {
					put(s.x, y, s.z, g);
					budget--;
					Block k = g.getBlock();
					if ((k == Blocks.BARREL || k == Blocks.HAY_BLOCK || k == Blocks.DECORATED_POT || k == Blocks.WOOL.pick(DyeColor.WHITE)
							|| k == Blocks.WOOL.pick(DyeColor.BROWN)) && r.nextInt(3) > 0 && empty(s.x, y + 2, s.z)) {
						put(s.x, y + 1, s.z, k == Blocks.DECORATED_POT ? Blocks.DECORATED_POT.defaultBlockState() : g);
					}
				}
			}
			// Rows of stock in the middle of a big store, with aisles between them.
			if (W >= 6 && D >= 6) {
				boolean alongX = W >= D;
				for (int x = x0 + 2; x <= x1 - 2; x++) {
					for (int z = z0 + 2; z <= z1 - 2; z++) {
						int lane = alongX ? z - z0 : x - x0;
						if (lane % 3 != 2) {
							continue;
						}
						BlockState g = goods[r.nextInt(goods.length)];
						if (g.getBlock() == Blocks.CHEST) {
							g = barrel();
						}
						if (claim(true, x, z)) {
							put(x, y, z, g);
						}
					}
				}
			}
			highShelves(2);
		}

		BlockState[] goods() {
			return switch (st) {
				case BYZ, ISLAM, CAUC -> new BlockState[]{Blocks.DECORATED_POT.defaultBlockState(), Blocks.DECORATED_POT.defaultBlockState(), barrel(),
						Blocks.CHEST.defaultBlockState(), Blocks.HAY_BLOCK.defaultBlockState(), Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState(),
						Blocks.MELON.defaultBlockState(), Blocks.CAULDRON.defaultBlockState()};
				case STEPPE -> new BlockState[]{Blocks.CHEST.defaultBlockState(), Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState(),
						Blocks.WOOL.pick(DyeColor.BROWN).defaultBlockState(), Blocks.HAY_BLOCK.defaultBlockState(), barrel()};
				default -> new BlockState[]{barrel(), barrel(), Blocks.CHEST.defaultBlockState(), Blocks.HAY_BLOCK.defaultBlockState(),
						Blocks.WOOL.pick(DyeColor.BROWN).defaultBlockState(), Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState(),
						Bp.log(wood.stripped, r.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z), Blocks.COMPOSTER.defaultBlockState(),
						Blocks.DECORATED_POT.defaultBlockState()};
			};
		}

		void hall(boolean hearth) {
			boolean hearthWall = (st == Style.WEST || st == Style.CAUC) && wealth >= 1;
			if (!hearth && !(hearthWall && fireplace())) {
				hearth(false);
			}
			if (st == Style.ISLAM || st == Style.STEPPE) {
				cushions(Math.max(6, (W + D) * 2));
				lowTables(1 + area() / 20);
				if (wealth >= 1) {
					throne();
				}
				floorLamps(2 + wealth);
				chest(far());
				amphorae(2);
			} else {
				int len = Math.max(2, Math.max(W, D) - 4);
				table(len, true);
				if (wealth >= 1) {
					throne();
				}
				banners(wealth >= 1 ? 2 + 2 * wealth : 1);
				chest(far());
				barrels(1 + wealth, true);
				if (st == Style.SLAV) {
					iconCorner(false);
					benches((W + D));
				} else if (st == Style.NORSE) {
					platforms();
				} else if (st == Style.BYZ) {
					iconCorner(true);
					amphorae(2);
				}
			}
		}

		void shop() {
			// The counter along the front, open to the street.
			BlockState top = Bp.slab(wood.slab, true);
			for (int x = x0; x <= x1; x++) {
				if (claim(false, x, z1)) {
					put(x, y, z1, r.nextInt(5) == 0 ? barrel() : top);
					tops.add(new int[]{x, z1});
				}
			}
			int kind = r.nextInt(4);
			BlockState[] wares = switch (kind) {
				case 0 -> new BlockState[]{Blocks.WOOL.pick(cols[0]).defaultBlockState(), Blocks.WOOL.pick(cols[1]).defaultBlockState(),
						Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState(), Blocks.LOOM.defaultBlockState()};
				case 1 -> new BlockState[]{Blocks.DECORATED_POT.defaultBlockState(), Blocks.FLOWER_POT.defaultBlockState(), Blocks.CAULDRON.defaultBlockState()};
				case 2 -> new BlockState[]{Blocks.HAY_BLOCK.defaultBlockState(), barrel(), Blocks.COMPOSTER.defaultBlockState()};
				default -> new BlockState[]{barrel(), Blocks.CHEST.defaultBlockState(), Blocks.DECORATED_POT.defaultBlockState()};
			};
			// Goods on the counter.
			for (int[] t : tops) {
				if (r.nextInt(3) == 0) {
					continue;
				}
				BlockState g = switch (kind) {
					case 0 -> Blocks.CARPET.pick(r.nextBoolean() ? cols[0] : cols[1]).defaultBlockState();
					case 1 -> r.nextBoolean() ? Blocks.DECORATED_POT.defaultBlockState() : Blocks.FLOWER_POT.defaultBlockState();
					case 2 -> r.nextBoolean() ? Blocks.MELON.defaultBlockState() : barrel();
					default -> r.nextBoolean() ? candle(1 + r.nextInt(3), null) : Blocks.DECORATED_POT.defaultBlockState();
				};
				if (b.get(t[0], y, t[1]) != null && b.get(t[0], y, t[1]).getBlock() == wood.slab) {
					put(t[0], y + 1, t[1], g);
				}
			}
			// Shelves and stock along the other walls.
			int n = 0;
			for (Spot s : far()) {
				if (s.z == z1 && s.wall == Direction.SOUTH) {
					continue;
				}
				if (n > W + D) {
					break;
				}
				BlockState g = r.nextInt(3) == 0 ? Bp.facing(wood.shelf, s.wall.getOpposite()) : wares[r.nextInt(wares.length)];
				if (g.getBlock() == Blocks.CHEST || g.getBlock() == Blocks.LOOM) {
					g = Bp.facing(g.getBlock(), s.wall.getOpposite());
				}
				if (claim(true, s.x, s.z)) {
					put(s.x, y, s.z, g);
					n++;
				}
			}
			// A stool for the seller behind the counter.
			for (int x = x0; x <= x1; x++) {
				if (D >= 3 && b.get(x, y, z1) != null && claim(false, x, z1 - 1)) {
					put(x, y, z1 - 1, Bp.stairs(wood.stairs, Direction.NORTH, false));
					break;
				}
			}
			highShelves(2);
		}

		void workshop() {
			int trade = r.nextInt(5);
			switch (trade) {
				case 0 -> { // carpenter, cooper
					wallBlock(near(), d -> Blocks.CRAFTING_TABLE.defaultBlockState(), true);
					for (int i = 0; i < 3; i++) {
						wallBlock(far(), d -> Bp.log(wood.stripped, d.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X), false);
					}
					barrels(2, true);
					wallBlock(far(), d -> Blocks.STONECUTTER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, d), true);
				}
				case 1 -> { // potter
					wallBlock(near(), d -> Bp.facing(Blocks.FURNACE, d), true);
					for (int i = 0; i < 4; i++) {
						wallBlock(far(), d -> Blocks.DECORATED_POT.defaultBlockState(), false);
					}
					wallBlock(far(), d -> Blocks.CLAY.defaultBlockState(), false);
					wallBlock(near(), d -> water(), true);
					wallBlock(far(), d -> Blocks.CRAFTING_TABLE.defaultBlockState(), true);
				}
				case 2 -> { // weaver
					loom();
					loom();
					for (int i = 0; i < 3; i++) {
						DyeColor col = cols[r.nextInt(cols.length)];
						wallBlock(far(), d -> Blocks.WOOL.pick(col).defaultBlockState(), false);
					}
					wallBlock(far(), d -> Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), true);
				}
				case 3 -> { // tanner, dyer
					wallBlock(near(), d -> water(), true);
					wallBlock(near(), d -> water(), true);
					barrels(3, false);
					wallBlock(far(), d -> Blocks.CRAFTING_TABLE.defaultBlockState(), true);
				}
				default -> { // bowyer, fletcher
					wallBlock(near(), d -> Blocks.FLETCHING_TABLE.defaultBlockState(), true);
					barrels(2, false);
					for (int i = 0; i < 2; i++) {
						wallBlock(far(), d -> Bp.log(wood.stripped, Direction.Axis.Y), false);
					}
				}
			}
			workTable();
			chest(far());
			for (int i = 0; i < 2; i++) {
				wallBlock(shuffled(), d -> Bp.facing(wood.shelf, d), true);
			}
			if (area() >= 24) {
				bed(pickBed(), false);
			}
			highShelves(2);
		}

		// ------------------------------------------------------------------ hearths

		void hearth(boolean kitchen) {
			int lo = Math.min(W, D), hi = Math.max(W, D);
			// No open fire on a wooden upper floor: a stone fireplace on the wall or a brazier, if anything.
			if (y >= 4 && (st == Style.WEST || st == Style.NORSE || st == Style.CAUC || st == Style.STEPPE)) {
				if (wealth >= 1 && !fireplace() && kitchen) {
					brazier();
				}
				return;
			}
			switch (st) {
				case SLAV -> slavStove();
				case WEST -> {
					if (c == Culture.MAGYAR && r.nextBoolean() && oven(Blocks.MUD_BRICKS.defaultBlockState())) {
						return;
					}
					if (wealth >= 2 && fireplace()) {
						return;
					}
					if (lo >= 5 && centreHearth(true)) {
						return;
					}
					if (!wallHearth() && kitchen) {
						oven(Blocks.COBBLESTONE.defaultBlockState());
					}
				}
				case NORSE -> {
					if (lo >= 5 && hi >= 8 && longFire()) {
						return;
					}
					if (lo >= 5 && centreHearth(true)) {
						return;
					}
					wallHearth();
				}
				case BYZ -> {
					if (kitchen) {
						if (!oven(Blocks.BRICKS.defaultBlockState())) {
							wallHearth();
						}
					} else if (wealth >= 1) {
						brazier();
					}
				}
				case CAUC -> {
					if (kitchen && oven(Blocks.COBBLESTONE.defaultBlockState())) {
						return;
					}
					if (lo >= 5 && centreHearth(true)) {
						return;
					}
					wallHearth();
				}
				case ISLAM -> {
					if (kitchen) {
						if (!oven(Blocks.MUD_BRICKS.defaultBlockState())) {
							brazier();
						}
					} else if (wealth >= 1) {
						brazier();
					}
				}
				case STEPPE -> {
					if (!centreHearth(lo >= 5)) {
						brazier();
					}
				}
			}
		}

		/** The great stove in the corner by the door; the red corner lies diagonally across from it. */
		boolean slavStove() {
			if (W < 3 || D < 3) {
				return wallBlock(far(), d -> Bp.facing(Blocks.SMOKER, d), true) && markHearth();
			}
			int[][] corners = {{x0, z0, 1, 1}, {x1, z0, -1, 1}, {x0, z1, 1, -1}, {x1, z1, -1, -1}};
			List<int[]> order = new ArrayList<>(List.of(corners));
			Collections.shuffle(order, r);
			order.sort((a, q) -> Integer.compare(dist(a[0], a[1]), dist(q[0], q[1])));
			for (int[] k : order) {
				for (int t = 0; t < 2; t++) {
					boolean longZ = (t == 0) == (D >= W);
					int along = longZ ? D : W, across = longZ ? W : D;
					if (across < 4) {
						continue;
					}
					int len = along >= 6 ? 3 : 2;
					int ax = longZ ? 2 : len, az = longZ ? len : 2;
					int[] cells = new int[ax * az * 2];
					int n = 0;
					for (int i = 0; i < ax; i++) {
						for (int j = 0; j < az; j++) {
							cells[n++] = k[0] + i * k[2];
							cells[n++] = k[1] + j * k[3];
						}
					}
					int mx = k[0] + (ax - 1) * k[2], mz = k[1] + (az - 1) * k[3];
					int fx = mx + (longZ ? 0 : k[2]), fz = mz + (longZ ? k[3] : 0);
					if (!walk(fx, fz)) {
						continue;
					}
					if (!claim(true, cells)) {
						continue;
					}
					reserve(fx, fz);
					stoveBlocks(b, k[0], k[1], k[2], k[3], y, longZ, len, wealth, -1, false);
					hx = k[0];
					hz = k[1];
					polati(k[0], k[1], k[2], k[3], longZ, len);
					return true;
				}
			}
			return wallBlock(far(), d -> Bp.facing(Blocks.SMOKER, d), true) && markHearth();
		}

		boolean markHearth() {
			findHearth();
			return true;
		}

		/** Sleeping boards under the ceiling beside the stove. */
		void polati(int cx, int cz, int ix, int iz, boolean longZ, int len) {
			BlockState slab = Bp.slab(wood.slab, false);
			for (int a = 2; a <= 3; a++) {
				for (int j = 0; j < 2; j++) {
					int x = longZ ? cx + a * ix : cx + j * ix;
					int z = longZ ? cz + j * iz : cz + a * iz;
					if (!inside(x, z)) {
						continue;
					}
					if (longZ ? Math.abs(x - (ix > 0 ? x1 : x0)) < 1 : Math.abs(z - (iz > 0 ? z1 : z0)) < 1) {
						continue;
					}
					if (empty(x, y + 2, z) && empty(x, y + 3, z) && !hole(x, z)) {
						put(x, y + 2, z, slab);
					}
				}
			}
		}

		boolean centreHearth(boolean ring) {
			int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
			List<int[]> cand = new ArrayList<>();
			for (int x = x0 + 1; x <= x1 - 1; x++) {
				for (int z = z0 + 1; z <= z1 - 1; z++) {
					cand.add(new int[]{x, z});
				}
			}
			if (cand.isEmpty()) {
				for (int x = x0; x <= x1; x++) {
					for (int z = z0; z <= z1; z++) {
						cand.add(new int[]{x, z});
					}
				}
			}
			cand.sort((a, q) -> Integer.compare(Math.abs(a[0] - cx) + Math.abs(a[1] - cz), Math.abs(q[0] - cx) + Math.abs(q[1] - cz)));
			BlockState stone = st == Style.NORSE || r.nextBoolean() ? Blocks.COBBLESTONE_SLAB.defaultBlockState() : Blocks.STONE_SLAB.defaultBlockState();
			int tries = 0;
			for (int[] p : cand) {
				if (++tries > 30) {
					break;
				}
				boolean ok = ring ? claim(true, p[0], p[1], p[0] + 1, p[1], p[0] - 1, p[1], p[0], p[1] + 1, p[0], p[1] - 1) : claim(true, p[0], p[1]);
				if (ok) {
					put(p[0], y, p[1], Blocks.CAMPFIRE.defaultBlockState());
					if (ring) {
						put(p[0] + 1, y, p[1], stone);
						put(p[0] - 1, y, p[1], stone);
						put(p[0], y, p[1] + 1, stone);
						put(p[0], y, p[1] - 1, stone);
					}
					hx = p[0];
					hz = p[1];
					return true;
				}
			}
			return false;
		}

		/** The long fire down the middle of a Norse hall. */
		boolean longFire() {
			boolean alongX = W >= D;
			int len = alongX ? W : D;
			int a = len / 4, e = len - 1 - len / 4;
			int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
			boolean any = false;
			for (int i = a; i <= e; i++) {
				int x = alongX ? x0 + i : cx, z = alongX ? cz : z0 + i;
				boolean fire = (i - a) % 3 == 1;
				if (claim(fire, x, z)) {
					put(x, y, z, fire ? Blocks.CAMPFIRE.defaultBlockState() : Blocks.COBBLESTONE_SLAB.defaultBlockState());
					if (fire && !any) {
						hx = x;
						hz = z;
						any = true;
					}
				}
			}
			return any;
		}

		/** A small hearth against a wall: a fire between two stones. */
		boolean wallHearth() {
			for (Spot s : far()) {
				Direction t = cw(s.wall);
				int ax = s.x - t.getStepX(), az = s.z - t.getStepZ(), bx = s.x + t.getStepX(), bz = s.z + t.getStepZ();
				if (wallCell(ax, az, s.wall) && wallCell(bx, bz, s.wall) && claim(true, ax, az, s.x, s.z, bx, bz)) {
					put(s.x, y, s.z, Blocks.CAMPFIRE.defaultBlockState());
					put(ax, y, az, Blocks.COBBLESTONE_SLAB.defaultBlockState());
					put(bx, y, bz, Blocks.COBBLESTONE_SLAB.defaultBlockState());
					hx = s.x;
					hz = s.z;
					return true;
				}
			}
			for (Spot s : far()) {
				if (claim(true, s.x, s.z)) {
					put(s.x, y, s.z, Blocks.CAMPFIRE.defaultBlockState());
					hx = s.x;
					hz = s.z;
					return true;
				}
			}
			return false;
		}

		/** A stone fireplace with a hood and a chimney breast. */
		boolean fireplace() {
			for (Spot s : far()) {
				Direction t = cw(s.wall);
				int ax = s.x - t.getStepX(), az = s.z - t.getStepZ(), bx = s.x + t.getStepX(), bz = s.z + t.getStepZ();
				int k = ceil(s.x, s.z);
				if (k != -1 && k < 3) {
					continue;
				}
				if (wallCell(ax, az, s.wall) && wallCell(bx, bz, s.wall) && claim(true, ax, az, s.x, s.z, bx, bz)) {
					BlockState sb = Blocks.STONE_BRICKS.defaultBlockState();
					put(ax, y, az, sb);
					put(bx, y, bz, sb);
					put(s.x, y, s.z, Blocks.CAMPFIRE.defaultBlockState());
					put(ax, y + 1, az, sb);
					put(bx, y + 1, bz, sb);
					put(s.x, y + 1, s.z, Bp.slab(Blocks.STONE_BRICK_SLAB, true));
					int top = k == -1 ? y + 4 : y + k - 1;
					for (int yy = y + 2; yy <= top; yy++) {
						put(s.x, yy, s.z, sb);
					}
					put(ax, y + 2, az, Bp.stairs(Blocks.STONE_BRICK_STAIRS, s.wall, false));
					put(bx, y + 2, bz, Bp.stairs(Blocks.STONE_BRICK_STAIRS, s.wall, false));
					hx = s.x;
					hz = s.z;
					return true;
				}
			}
			return false;
		}

		/** A brazier: fire in a stone bowl on a post. */
		boolean brazier() {
			int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
			List<int[]> cand = new ArrayList<>();
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					cand.add(new int[]{x, z});
				}
			}
			cand.sort((a, q) -> Integer.compare(Math.abs(a[0] - cx) + Math.abs(a[1] - cz), Math.abs(q[0] - cx) + Math.abs(q[1] - cz)));
			int tries = 0;
			for (int[] p : cand) {
				if (++tries > 20) {
					break;
				}
				if (empty(p[0], y + 1, p[1]) && claim(true, p[0], p[1])) {
					put(p[0], y, p[1], (st == Style.ISLAM ? Blocks.SANDSTONE_WALL : Blocks.STONE_BRICK_WALL).defaultBlockState());
					put(p[0], y + 1, p[1], Blocks.CAMPFIRE.defaultBlockState());
					hx = p[0];
					hz = p[1];
					return true;
				}
			}
			return false;
		}

		/** A baking oven against the wall: a smoker mouth in a block of clay or brick. */
		boolean oven(BlockState mat) {
			for (Spot s : far()) {
				Direction t = cw(s.wall);
				int bx = s.x + t.getStepX(), bz = s.z + t.getStepZ();
				if (wallCell(bx, bz, s.wall) && claim(true, s.x, s.z, bx, bz)) {
					put(s.x, y, s.z, Bp.facing(Blocks.SMOKER, s.wall.getOpposite()));
					put(bx, y, bz, mat);
					put(s.x, y + 1, s.z, mat);
					put(bx, y + 1, bz, mat);
					reserve(s.x - s.wall.getStepX(), s.z - s.wall.getStepZ());
					hx = s.x;
					hz = s.z;
					return true;
				}
			}
			return false;
		}

		// ------------------------------------------------------------------ furniture

		Block pickBed() {
			DyeColor col = switch (st) {
				case SLAV -> r.nextBoolean() ? DyeColor.RED : DyeColor.WHITE;
				case WEST -> r.nextInt(3) == 0 ? DyeColor.GREEN : r.nextBoolean() ? DyeColor.BLUE : DyeColor.RED;
				case NORSE -> r.nextBoolean() ? DyeColor.BROWN : DyeColor.GRAY;
				case BYZ -> wealth >= 1 ? DyeColor.PURPLE : DyeColor.RED;
				case CAUC -> r.nextBoolean() ? DyeColor.RED : DyeColor.BROWN;
				case ISLAM -> r.nextBoolean() ? DyeColor.ORANGE : r.nextBoolean() ? DyeColor.BLUE : DyeColor.WHITE;
				case STEPPE -> r.nextBoolean() ? DyeColor.RED : DyeColor.BROWN;
			};
			return Blocks.BED.pick(col);
		}

		/** A bed with its head to a wall (or along it), far from the door; a canopy for the rich. */
		boolean bed(Block bed, boolean canopy) {
			for (int pass = 0; pass < 2; pass++) {
				for (Spot s : far()) {
					Direction in = s.wall.getOpposite();
					int fx, fz;
					Direction head;
					if (pass == 0) {
						fx = s.x + in.getStepX();
						fz = s.z + in.getStepZ();
						head = s.wall;
					} else {
						Direction t = cw(s.wall);
						fx = s.x + t.getStepX();
						fz = s.z + t.getStepZ();
						head = t.getOpposite();
						if (!wallCell(fx, fz, s.wall)) {
							continue;
						}
					}
					if (!claim(true, s.x, s.z, fx, fz)) {
						continue;
					}
					b.bed(bed, fx, y, fz, head);
					if (canopy) {
						BlockState cloth = Bp.slab(Blocks.WOOL_SLAB.pick(cols[0]), true);
						if (empty(s.x, y + 2, s.z) && empty(fx, y + 2, fz)) {
							put(s.x, y + 2, s.z, cloth);
							put(fx, y + 2, fz, cloth);
						}
					}
					// A chest at the foot of the bed.
					if (pass == 0 && r.nextInt(3) == 0) {
						int cx = fx + in.getStepX(), cz = fz + in.getStepZ();
						if (claim(true, cx, cz)) {
							put(cx, y, cz, Bp.facing(Blocks.CHEST, in));
						}
					}
					return true;
				}
			}
			return false;
		}

		boolean wallBlock(List<Spot> order, Function<Direction, BlockState> f, boolean access) {
			for (Spot s : order) {
				if (claim(access, s.x, s.z)) {
					put(s.x, y, s.z, f.apply(s.wall.getOpposite()));
					return true;
				}
			}
			return false;
		}

		boolean chest(List<Spot> order) {
			for (Spot s : order) {
				if (claim(true, s.x, s.z)) {
					put(s.x, y, s.z, Bp.facing(Blocks.CHEST, s.wall.getOpposite()));
					if (wealth >= 1 && r.nextInt(3) == 0) {
						put(s.x, y + 1, s.z, candle(1 + r.nextInt(2), null));
					}
					return true;
				}
			}
			return false;
		}

		void loom() {
			wallBlock(near(), d -> Bp.facing(Blocks.LOOM, d), true);
		}

		void cradle() {
			wallBlock(far(), d -> Blocks.COMPOSTER.defaultBlockState(), true);
		}

		void barrels(int n, boolean stack) {
			for (int i = 0; i < n; i++) {
				for (Spot s : far()) {
					if (claim(true, s.x, s.z)) {
						put(s.x, y, s.z, barrel());
						if (stack && empty(s.x, y + 2, s.z)) {
							put(s.x, y + 1, s.z, barrel());
						}
						break;
					}
				}
			}
		}

		void amphorae(int n) {
			for (int i = 0; i < n; i++) {
				wallBlock(r.nextBoolean() ? far() : shuffled(), d -> Blocks.DECORATED_POT.defaultBlockState(), false);
			}
		}

		void cauldronByHearth() {
			List<Spot> l = hx != Integer.MIN_VALUE ? nearTo(hx, hz) : near();
			wallBlock(l, d -> water(), true);
		}

		/** Shelves on the walls above head height (or a peg rail of trapdoors for the poor). */
		void highShelves(int n) {
			int placed = 0;
			List<Spot> l = hx != Integer.MIN_VALUE ? nearTo(hx, hz) : shuffled();
			for (Spot s : l) {
				if (placed >= n) {
					return;
				}
				int wx = s.x + s.wall.getStepX(), wz = s.z + s.wall.getStepZ();
				if (empty(s.x, y + 2, s.z) && solid(wx, y + 2, wz) && !res[s.x - x0][s.z - z0] && !isDoor(b.get(wx, y + 1, wz))) {
					BlockState sh = wealth == 0 && r.nextBoolean() ? Bp.trapdoor(wood.trapdoor, s.wall.getOpposite(), true, false)
							: Bp.facing(wood.shelf, s.wall.getOpposite());
					if (put(s.x, y + 2, s.z, sh)) {
						placed++;
					}
				}
			}
		}

		/** A work bench of boards against a wall. */
		void workTable() {
			for (Spot s : far()) {
				Direction t = cw(s.wall);
				int bx = s.x + t.getStepX(), bz = s.z + t.getStepZ();
				if (wallCell(bx, bz, s.wall) && claim(true, s.x, s.z, bx, bz)) {
					put(s.x, y, s.z, Bp.slab(wood.slab, true));
					put(bx, y, bz, Bp.slab(wood.slab, true));
					tops.add(new int[]{s.x, s.z});
					tops.add(new int[]{bx, bz});
					return;
				}
			}
		}

		/**
		 * A table of {@code len} boards with benches or chairs: free-standing in the middle of a big enough room, against a
		 * wall in a small one.
		 */
		void table(int len, boolean hall) {
			BlockState top = r.nextInt(3) == 0 && !hall ? Bp.trapdoor(wood.trapdoor, Direction.NORTH, true, false) : Bp.slab(wood.slab, true);
			boolean alongX = W >= D;
			if (Math.min(W, D) >= 5) {
				int span = alongX ? W - 4 : D - 4;
				len = Math.max(1, Math.min(len, span));
				int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
				List<int[]> cand = new ArrayList<>();
				for (int x = x0 + 2; x <= x1 - 2; x++) {
					for (int z = z0 + 2; z <= z1 - 2; z++) {
						cand.add(new int[]{x, z});
					}
				}
				cand.sort((a, q) -> Integer.compare(Math.abs(a[0] - cx) + Math.abs(a[1] - cz), Math.abs(q[0] - cx) + Math.abs(q[1] - cz)));
				int tries = 0;
				for (int[] p : cand) {
					if (++tries > 25) {
						break;
					}
					int[] cells = new int[len * 2];
					for (int i = 0; i < len; i++) {
						int off = i - (len - 1) / 2;
						cells[2 * i] = alongX ? p[0] + off : p[0];
						cells[2 * i + 1] = alongX ? p[1] : p[1] + off;
					}
					if (!claim(true, cells)) {
						continue;
					}
					for (int i = 0; i < len; i++) {
						put(cells[2 * i], y, cells[2 * i + 1], top);
						tops.add(new int[]{cells[2 * i], cells[2 * i + 1]});
					}
					// Seats on both long sides.
					boolean bench = st == Style.SLAV || st == Style.NORSE || r.nextBoolean();
					for (int i = 0; i < len; i++) {
						for (int sgn = -1; sgn <= 1; sgn += 2) {
							int sx = alongX ? cells[2 * i] : cells[2 * i] + sgn, sz = alongX ? cells[2 * i + 1] + sgn : cells[2 * i + 1];
							if ((hall || i % 2 == 0 || bench) && claim(false, sx, sz)) {
								Direction away = alongX ? dir(0, sgn) : dir(sgn, 0);
								put(sx, y, sz, bench ? Bp.slab(wood.slab, false) : Bp.stairs(wood.stairs, away, false));
							}
						}
					}
					// Chairs at the ends for a small table.
					if (!hall && len <= 2) {
						int ex = alongX ? cells[0] - 1 : cells[0], ez = alongX ? cells[1] : cells[1] - 1;
						if (claim(false, ex, ez)) {
							put(ex, y, ez, Bp.stairs(wood.stairs, alongX ? Direction.WEST : Direction.NORTH, false));
						}
					}
					return;
				}
			}
			// Against a wall, with a bench in front.
			for (Spot s : far()) {
				Direction in = s.wall.getOpposite();
				int sx = s.x + in.getStepX(), sz = s.z + in.getStepZ();
				if (claim(true, s.x, s.z)) {
					put(s.x, y, s.z, top);
					tops.add(new int[]{s.x, s.z});
					if (claim(false, sx, sz)) {
						put(sx, y, sz, Bp.stairs(wood.stairs, in, false));
					}
					return;
				}
			}
		}

		/** Лавки: plank benches along the walls. */
		void benches(int max) {
			BlockState slab = Bp.slab(wood.slab, false);
			int n = 0;
			for (Spot s : walls) {
				if (n >= max) {
					return;
				}
				if (claim(false, s.x, s.z)) {
					put(s.x, y, s.z, slab);
					n++;
				}
			}
		}

		/** The red corner: benches along two walls, the table, and the icon shelf with a lamp. */
		void redCorner() {
			int cx, cz;
			if (hx != Integer.MIN_VALUE) {
				cx = Math.abs(hx - x0) <= Math.abs(hx - x1) ? x1 : x0;
				cz = Math.abs(hz - z0) <= Math.abs(hz - z1) ? z1 : z0;
			} else {
				int[][] corners = {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}};
				int[] best = corners[0];
				for (int[] k : corners) {
					if (dist(k[0], k[1]) > dist(best[0], best[1])) {
						best = k;
					}
				}
				cx = best[0];
				cz = best[1];
			}
			int ix = cx == x0 ? 1 : -1, iz = cz == z0 ? 1 : -1;
			BlockState slab = Bp.slab(wood.slab, false);
			int lz = Math.min(D - 1, 3), lx = Math.min(W - 1, 3);
			for (int j = 0; j < Math.max(1, lz); j++) {
				if (claim(false, cx, cz + j * iz)) {
					put(cx, y, cz + j * iz, slab);
				}
			}
			for (int i = 1; i < lx; i++) {
				if (claim(false, cx + i * ix, cz)) {
					put(cx + i * ix, y, cz, slab);
				}
			}
			if (W >= 3 && D >= 3) {
				int tx = cx + ix, tz = cz + iz;
				BlockState top = r.nextBoolean() ? Bp.slab(wood.slab, true) : Bp.trapdoor(wood.trapdoor, Direction.NORTH, true, false);
				if (claim(true, tx, tz)) {
					put(tx, y, tz, top);
					tops.add(new int[]{tx, tz});
					boolean longX = W > D;
					int ux = longX ? tx + ix : tx, uz = longX ? tz : tz + iz;
					if (W >= 5 && D >= 5 && claim(true, ux, uz)) {
						put(ux, y, uz, top);
						tops.add(new int[]{ux, uz});
					}
				}
			}
			icon(cx, cz, ix, iz);
		}

		/** The icon corner of an Orthodox house (far from the door). */
		void iconCorner(boolean rich) {
			int[][] corners = {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}};
			int[] best = corners[0];
			for (int[] k : corners) {
				if (dist(k[0], k[1]) > dist(best[0], best[1])) {
					best = k;
				}
			}
			int ix = best[0] == x0 ? 1 : -1, iz = best[1] == z0 ? 1 : -1;
			icon(best[0], best[1], ix, iz);
			if (rich) {
				int x = best[0], z = best[1];
				if (claim(true, x, z)) {
					put(x, y, z, Bp.slab(Blocks.POLISHED_ANDESITE_SLAB, true));
					put(x, y + 1, z, candle(2 + r.nextInt(3), DyeColor.YELLOW));
				}
			}
		}

		void icon(int cx, int cz, int ix, int iz) {
			int k = ceil(cx, cz);
			if (empty(cx, y + 2, cz) && (k == -1 || k >= 4) && empty(cx, y + 3, cz)) {
				put(cx, y + 2, cz, Bp.slab(wood.slab, true));
				put(cx, y + 3, cz, wealth >= 1 && r.nextBoolean() ? Bp.lantern(false) : candle(1 + wealth, null));
			} else if (k == 3) {
				put(cx, y + 2, cz, Bp.lantern(true));
			}
			// A towel (рушник) or an icon cloth on the wall next to it.
			int nx = cx + ix, nz = cz;
			Direction wall = iz > 0 ? Direction.NORTH : Direction.SOUTH;
			int wx = nx + wall.getStepX(), wz = nz + wall.getStepZ();
			if (inside(nx, nz) && empty(nx, y + 2, nz) && solid(wx, y + 2, wz) && !isDoor(b.get(wx, y + 1, wz))) {
				DyeColor col = st == Style.BYZ ? DyeColor.YELLOW : st == Style.CAUC ? DyeColor.RED : DyeColor.WHITE;
				put(nx, y + 2, nz, Blocks.WALL_BANNER.pick(col).defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, wall.getOpposite()));
			}
		}

		/** Norse side platforms (set) along the long walls, some covered with furs. */
		void platforms() {
			boolean alongX = W >= D;
			int width = Math.min(W, D) >= 9 ? 2 : 1;
			BlockState board = Bp.slab(wood.slab, false);
			for (Spot s : walls) {
				boolean longWall = alongX ? (s.wall == Direction.NORTH || s.wall == Direction.SOUTH) : (s.wall == Direction.WEST || s.wall == Direction.EAST);
				if (!longWall) {
					continue;
				}
				for (int k = 0; k < width; k++) {
					int x = s.x - s.wall.getStepX() * k, z = s.z - s.wall.getStepZ() * k;
					if (claim(false, x, z)) {
						BlockState fur = r.nextInt(3) == 0 ? Bp.slab(Blocks.WOOL_SLAB.pick(r.nextBoolean() ? cols[0] : cols[1]), false) : board;
						put(x, y, z, fur);
					}
				}
			}
		}

		/** A couch of cushioned stairs along a wall. */
		void couch(int len) {
			Block seat = wealth >= 1 ? Blocks.WOOL_STAIRS.pick(cols[0]) : wood.stairs;
			for (Spot s : far()) {
				Direction t = cw(s.wall);
				int[] cells = new int[len * 2];
				boolean ok = true;
				for (int i = 0; i < len; i++) {
					cells[2 * i] = s.x + t.getStepX() * i;
					cells[2 * i + 1] = s.z + t.getStepZ() * i;
					if (!wallCell(cells[2 * i], cells[2 * i + 1], s.wall)) {
						ok = false;
					}
				}
				if (ok && claim(true, cells)) {
					for (int i = 0; i < len; i++) {
						put(cells[2 * i], y, cells[2 * i + 1], Bp.stairs(seat, s.wall, false));
					}
					// A low table in front of it.
					int fx = s.x + t.getStepX() * (len / 2) - s.wall.getStepX(), fz = s.z + t.getStepZ() * (len / 2) - s.wall.getStepZ();
					if (claim(false, fx, fz)) {
						put(fx, y, fz, Bp.trapdoor(wood.trapdoor, Direction.NORTH, false, false));
					}
					return;
				}
			}
		}

		/** Cushions (wool slabs) along the walls. */
		void cushions(int max) {
			int n = 0;
			List<Spot> l = far();
			for (Spot s : l) {
				if (n >= max) {
					return;
				}
				DyeColor col = n % 4 == 3 ? cols[2] : cols[n % 2];
				if (claim(false, s.x, s.z)) {
					put(s.x, y, s.z, Bp.slab(Blocks.WOOL_SLAB.pick(col), false));
					n++;
				}
			}
		}

		/** Low tables (closed trapdoors) out in the room. */
		void lowTables(int n) {
			int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
			int placed = 0;
			for (int d = 0; d < Math.max(W, D) && placed < n; d++) {
				for (int x = cx - d; x <= cx + d && placed < n; x++) {
					for (int z = cz - d; z <= cz + d && placed < n; z++) {
						if (Math.max(Math.abs(x - cx), Math.abs(z - cz)) != d || (x + z) % 3 != 0) {
							continue;
						}
						if (claim(false, x, z)) {
							put(x, y, z, Bp.trapdoor(wood.trapdoor, Direction.NORTH, false, false));
							placed++;
						}
					}
				}
			}
		}

		/** Standing brass lamps in the corners. */
		void floorLamps(int n) {
			int placed = 0;
			int[][] corners = {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}};
			for (int[] k : corners) {
				if (placed >= n) {
					return;
				}
				if (claim(false, k[0], k[1])) {
					put(k[0], y, k[1], Bp.lantern(false));
					placed++;
				}
			}
		}

		/** Pots in the wall niches, on a shelf above the cushions. */
		void niches(int n) {
			int placed = 0;
			for (Spot s : shuffled()) {
				if (placed >= n) {
					return;
				}
				int wx = s.x + s.wall.getStepX(), wz = s.z + s.wall.getStepZ();
				if (taken[s.x - x0][s.z - z0] && empty(s.x, y + 2, s.z) && solid(wx, y + 2, wz) && empty(s.x, y + 1, s.z)) {
					BlockState below = b.get(s.x, y, s.z);
					if (below != null && below.getBlock() == Blocks.CHEST) {
						continue;
					}
					if (put(s.x, y + 2, s.z, Bp.slab(Blocks.SMOOTH_SANDSTONE_SLAB, true))) {
						put(s.x, y + 3, s.z, r.nextBoolean() ? Blocks.DECORATED_POT.defaultBlockState() : flowerPot(r));
						placed++;
					}
				}
			}
		}

		/** The high seat at the far wall, with banners behind it. */
		void throne() {
			List<Spot> l = far();
			for (Spot s : l) {
				if (!claim(true, s.x, s.z)) {
					continue;
				}
				Block seat = st == Style.ISLAM || st == Style.STEPPE ? Blocks.WOOL_STAIRS.pick(cols[0]) : Blocks.DARK_OAK_STAIRS;
				put(s.x, y, s.z, Bp.stairs(seat, s.wall, false));
				Direction t = cw(s.wall);
				for (int sgn = -1; sgn <= 1; sgn += 2) {
					int ax = s.x + t.getStepX() * sgn, az = s.z + t.getStepZ() * sgn;
					int wx = ax + s.wall.getStepX(), wz = az + s.wall.getStepZ();
					if (inside(ax, az) && empty(ax, y + 2, az) && solid(wx, y + 2, wz)) {
						put(ax, y + 2, az, Blocks.WALL_BANNER.pick(cols[1]).defaultBlockState()
								.setValue(BlockStateProperties.HORIZONTAL_FACING, s.wall.getOpposite()));
					}
				}
				int fx = s.x - s.wall.getStepX(), fz = s.z - s.wall.getStepZ();
				if (inside(fx, fz) && empty(fx, y, fz) && !hole(fx, fz)) {
					put(fx, y, fz, Blocks.CARPET.pick(cols[0]).defaultBlockState());
				}
				return;
			}
		}

		/** Banners on the walls above head height. */
		void banners(int n) {
			int placed = 0;
			for (Spot s : shuffled()) {
				if (placed >= n) {
					return;
				}
				int wx = s.x + s.wall.getStepX(), wz = s.z + s.wall.getStepZ();
				if (empty(s.x, y + 2, s.z) && solid(wx, y + 2, wz) && solid(wx, y + 1, wz) && !isDoor(b.get(wx, y + 1, wz))) {
					DyeColor col = placed % 2 == 0 ? cols[0] : cols[1];
					if (put(s.x, y + 2, s.z, Blocks.WALL_BANNER.pick(col).defaultBlockState()
							.setValue(BlockStateProperties.HORIZONTAL_FACING, s.wall.getOpposite()))) {
						placed++;
					}
				}
			}
		}

		// ------------------------------------------------------------------ light and floor

		void lights() {
			boolean poor = wealth == 0;
			int want;
			if (st == Style.ISLAM) {
				want = Math.max(1, area() / 14);
			} else if (poor) {
				want = area() >= 20 ? 1 : 0;
			} else {
				want = Math.max(1, area() / (wealth >= 2 ? 12 : 20));
			}
			int placed = 0;
			if (want > 0 && !(st == Style.SLAV && poor)) {
				int nx = Math.max(1, Math.round(W / 5f)), nz = Math.max(1, Math.round(D / 5f));
				outer:
				for (int i = 0; i < nx; i++) {
					for (int j = 0; j < nz; j++) {
						if (placed >= want) {
							break outer;
						}
						int x = x0 + (2 * i + 1) * W / (2 * nx), z = z0 + (2 * j + 1) * D / (2 * nz);
						if (hang(x, z) || hang(x + 1, z) || hang(x, z + 1)) {
							placed++;
						}
					}
				}
			}
			if (placed == 0) {
				// A splinter light or torch on the wall, or a lamp on the table.
				boolean done = false;
				List<Spot> l = tops.isEmpty() ? shuffled() : nearTo(tops.get(0)[0], tops.get(0)[1]);
				for (Spot s : l) {
					int wx = s.x + s.wall.getStepX(), wz = s.z + s.wall.getStepZ();
					if (empty(s.x, y + 2, s.z) && solid(wx, y + 2, wz) && !isDoor(b.get(wx, y + 1, wz)) && !res[s.x - x0][s.z - z0]) {
						if (put(s.x, y + 2, s.z, Bp.wallTorch(s.wall.getOpposite()))) {
							done = true;
							break;
						}
					}
				}
				if (!done && !tops.isEmpty()) {
					int[] t = tops.get(r.nextInt(tops.size()));
					put(t[0], y + 1, t[1], Bp.lantern(false));
				}
			}
			for (int[] t : tops) {
				if (r.nextInt(3) == 0) {
					BlockState g = r.nextInt(3) == 0 ? flowerPot(r) : candle(1 + r.nextInt(3), null);
					put(t[0], y + 1, t[1], g);
				}
			}
		}

		boolean hang(int x, int z) {
			if (!inside(x, z)) {
				return false;
			}
			int k = ceil(x, z);
			if (k < 3) {
				return false;
			}
			BlockState above = b.get(x, y + k, z);
			if (above == null || isHearth(above) || above.getBlock() == Blocks.LANTERN
					|| (above.hasProperty(BlockStateProperties.SLAB_TYPE) && above.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.TOP)) {
				return false;
			}
			if (!empty(x, y + k - 1, z)) {
				return false;
			}
			return put(x, y + k - 1, z, Bp.lantern(true));
		}

		boolean carpetable(int x, int z) {
			if (!inside(x, z) || !empty(x, y, z) || hole(x, z)) {
				return false;
			}
			BlockState below = b.get(x, y - 1, z);
			return below != null || y - 1 <= 0;
		}

		void floorCover() {
			switch (st) {
				case ISLAM -> {
					if (wealth >= 1 || use == Use.LIVING || use == Use.HALL || use == Use.SLEEPING) {
						rug(x0, z0, x1, z1, cols[0], cols[1], cols[2]);
					}
				}
				case STEPPE -> rug(x0, z0, x1, z1, cols[0], cols[1], cols[2]);
				case BYZ, CAUC -> {
					if (wealth >= 1 && (use == Use.LIVING || use == Use.HALL || use == Use.SLEEPING) && W >= 4 && D >= 4) {
						rug(x0 + 1, z0 + 1, x1 - 1, z1 - 1, cols[0], cols[1], cols[2]);
					}
				}
				case SLAV -> {
					if (wealth >= 1 && (use == Use.LIVING || use == Use.SLEEPING || use == Use.HALL)) {
						runner();
					}
				}
				case WEST -> {
					if (wealth >= 2 && (use == Use.LIVING || use == Use.HALL || use == Use.SLEEPING) && W >= 5 && D >= 5) {
						int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
						rug(cx - 1, cz - 1, cx + 1, cz + 1, cols[1], cols[0], cols[0]);
					}
				}
				default -> {
				}
			}
		}

		/** A rug with a border and a middle; carpets only on free floor. */
		void rug(int ax, int az, int bx, int bz, DyeColor border, DyeColor field, DyeColor middle) {
			ax = Math.max(ax, x0);
			az = Math.max(az, z0);
			bx = Math.min(bx, x1);
			bz = Math.min(bz, z1);
			int mx = (ax + bx) / 2, mz = (az + bz) / 2;
			for (int x = ax; x <= bx; x++) {
				for (int z = az; z <= bz; z++) {
					if (!carpetable(x, z)) {
						continue;
					}
					boolean edge = x == ax || x == bx || z == az || z == bz;
					boolean mid = Math.abs(x - mx) + Math.abs(z - mz) <= Math.min(bx - ax, bz - az) / 4;
					DyeColor col = edge ? border : mid ? middle : field;
					if (st == Style.STEPPE && !edge && (x + z) % 4 == 0) {
						col = middle;
					}
					put(x, y, z, Blocks.CARPET.pick(col).defaultBlockState());
				}
			}
		}

		/** A woven runner from the door into the room. */
		void runner() {
			if (seeds.isEmpty()) {
				return;
			}
			int[] s = seeds.get(0);
			int dx = 0, dz = 0;
			if (s[0] == x0) {
				dx = 1;
			} else if (s[0] == x1) {
				dx = -1;
			} else if (s[1] == z0) {
				dz = 1;
			} else {
				dz = -1;
			}
			int x = s[0], z = s[1];
			for (int i = 0; i < 12 && inside(x, z); i++) {
				if (!carpetable(x, z)) {
					break;
				}
				put(x, y, z, Blocks.CARPET.pick(i % 3 == 2 ? cols[1] : cols[0]).defaultBlockState());
				x += dx;
				z += dz;
			}
		}
	}
}
