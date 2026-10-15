package com.stasdoto.medieval.construct;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.stasdoto.medieval.history.Culture;

/**
 * Castles of around 1050: the Norman motte with its timber tower, keeps and donjons of stone and logs, and the
 * Trelleborg hall of the Danish kings. Everything is drawn facing the street (+z).
 */
final class Forts {
	private Forts() {
	}

	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	/** Kinds of storeys inside a tower. */
	private static final int K_STORE = 0, K_ARMS = 1, K_SLEEP = 2, K_HALL = 3, K_WATCH = 4;

	// ================================================================ small helpers

	private static boolean in(Bp b, int x, int z) {
		return x >= 0 && x < b.w && z >= 0 && z < b.d;
	}

	/** Sets a block only inside the plot. */
	private static void put(Bp b, int x, int y, int z, BlockState s) {
		if (in(b, x, z)) {
			b.set(x, y, z, s);
		}
	}

	private static boolean free(Bp b, int x, int y, int z) {
		BlockState s = b.get(x, y, z);
		return s == null || s.isAir();
	}

	/** Puts furniture only into an empty cell inside the plot. */
	private static void furnish(Bp b, int x, int y, int z, BlockState s) {
		if (in(b, x, z) && free(b, x, y, z)) {
			b.set(x, y, z, s);
		}
	}

	/** Keeps a walkway free (a rug on it, so that nothing else is put there later). */
	private static void keepFree(Bp b, int x, int y, int z, DyeColor col) {
		furnish(b, x, y, z, Blocks.CARPET.pick(col).defaultBlockState());
	}

	/** A glass pane joined to the wall on both sides (the wall runs along x or along z). */
	private static BlockState pane(boolean alongX) {
		BlockState s = Blocks.GLASS_PANE.defaultBlockState();
		if (!s.hasProperty(BlockStateProperties.EAST) || !s.hasProperty(BlockStateProperties.NORTH)) {
			return s;
		}
		return alongX ? s.setValue(BlockStateProperties.EAST, true).setValue(BlockStateProperties.WEST, true)
				: s.setValue(BlockStateProperties.NORTH, true).setValue(BlockStateProperties.SOUTH, true);
	}

	private static BlockState barrel(Direction d) {
		return Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, d);
	}

	private static BlockState candles(int n) {
		return Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, Math.max(1, Math.min(4, n)))
				.setValue(BlockStateProperties.LIT, true);
	}

	private static BlockState banner(DyeColor c) {
		return Blocks.BANNER.pick(c).defaultBlockState();
	}

	private static BlockState wallBanner(DyeColor c, Direction f) {
		return Bp.facing(Blocks.WALL_BANNER.pick(c), f);
	}

	/** Stairs on a cone/ring roof face the middle (their high side is towards the centre). */
	private static Direction toCenter(double dx, double dz) {
		if (Math.abs(dx) > Math.abs(dz)) {
			return dx > 0 ? Direction.WEST : Direction.EAST;
		}
		return dz > 0 ? Direction.NORTH : Direction.SOUTH;
	}

	/** The direction from a cell on a square ring (half-size around (cx, cz)) towards the middle. */
	private static Direction ringInward(int x, int z, int x0, int z0, int x1, int z1) {
		if (z == z0) {
			return Direction.SOUTH;
		}
		if (z == z1) {
			return Direction.NORTH;
		}
		return x == x0 ? Direction.EAST : Direction.WEST;
	}

	/** Heraldic colours of the lord. */
	private static DyeColor[] colors(Bp b, Culture c) {
		DyeColor[][] sets = switch (c) {
			case LATIN, CELTIC -> new DyeColor[][]{{DyeColor.RED, DyeColor.YELLOW}, {DyeColor.BLUE, DyeColor.WHITE}, {DyeColor.RED, DyeColor.WHITE},
					{DyeColor.GREEN, DyeColor.YELLOW}, {DyeColor.BLACK, DyeColor.YELLOW}, {DyeColor.BLUE, DyeColor.YELLOW}};
			case BYZANTINE -> new DyeColor[][]{{DyeColor.PURPLE, DyeColor.YELLOW}, {DyeColor.RED, DyeColor.YELLOW}};
			case RUS, BALT_FINN, WEST_SLAV, MAGYAR -> new DyeColor[][]{{DyeColor.RED, DyeColor.YELLOW}, {DyeColor.RED, DyeColor.WHITE},
					{DyeColor.BLUE, DyeColor.YELLOW}};
			case NORSE -> new DyeColor[][]{{DyeColor.RED, DyeColor.BLACK}, {DyeColor.BLUE, DyeColor.WHITE}, {DyeColor.RED, DyeColor.WHITE}};
			case CAUCASUS -> new DyeColor[][]{{DyeColor.RED, DyeColor.WHITE}, {DyeColor.PURPLE, DyeColor.WHITE}};
			default -> new DyeColor[][]{{DyeColor.GREEN, DyeColor.WHITE}, {DyeColor.RED, DyeColor.WHITE}, {DyeColor.BLACK, DyeColor.WHITE},
					{DyeColor.WHITE, DyeColor.GREEN}};
		};
		return sets[b.r.nextInt(sets.length)];
	}

	// ================================================================ masonry of each people

	/** A set of stone materials for one building. */
	private static final class Mat {
		BlockState wall = Blocks.COBBLESTONE.defaultBlockState();
		BlockState alt = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
		BlockState band = Blocks.STONE_BRICKS.defaultBlockState();
		BlockState trim = Blocks.STONE_BRICKS.defaultBlockState();
		Block stairs = Blocks.STONE_BRICK_STAIRS;
		Block slab = Blocks.STONE_BRICK_SLAB;
		Block post = Blocks.COBBLESTONE_WALL;
		int bandEvery = 0, bandRows = 1, altChance = 7;

		BlockState at(java.util.Random r, int y) {
			if (bandEvery > 0 && Math.floorMod(y, bandEvery) < bandRows) {
				return band;
			}
			return altChance > 0 && r.nextInt(altChance) == 0 ? alt : wall;
		}
	}

	private static void northern(Mat m, Bp b) {
		if (b.r.nextBoolean()) {
			m.wall = Blocks.COBBLESTONE.defaultBlockState();
			m.alt = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
			m.trim = Blocks.STONE_BRICKS.defaultBlockState();
			m.stairs = Blocks.STONE_BRICK_STAIRS;
			m.slab = Blocks.STONE_BRICK_SLAB;
			m.post = Blocks.COBBLESTONE_WALL;
		} else {
			m.wall = Blocks.STONE_BRICKS.defaultBlockState();
			m.alt = b.r.nextBoolean() ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
			m.trim = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
			m.stairs = Blocks.STONE_BRICK_STAIRS;
			m.slab = Blocks.STONE_BRICK_SLAB;
			m.post = Blocks.STONE_BRICK_WALL;
		}
		m.band = m.wall;
	}

	private static Mat stone(Bp b, Culture c) {
		Mat m = new Mat();
		switch (c) {
			case BYZANTINE -> {
				// Stone with bands of red brick (opus mixtum).
				boolean lime = b.r.nextBoolean();
				m.wall = (lime ? Blocks.SMOOTH_SANDSTONE : Blocks.STONE_BRICKS).defaultBlockState();
				m.alt = (lime ? Blocks.SANDSTONE : Blocks.CRACKED_STONE_BRICKS).defaultBlockState();
				m.band = Blocks.BRICKS.defaultBlockState();
				m.trim = Blocks.BRICKS.defaultBlockState();
				m.stairs = lime ? Blocks.SMOOTH_SANDSTONE_STAIRS : Blocks.STONE_BRICK_STAIRS;
				m.slab = lime ? Blocks.SMOOTH_SANDSTONE_SLAB : Blocks.STONE_BRICK_SLAB;
				m.post = Blocks.BRICK_WALL;
				m.bandEvery = 5;
				m.bandRows = 2;
				m.altChance = 10;
			}
			case CAUCASUS -> {
				// Fine ashlar: tuff in Armenia, grey stone in Georgia.
				if (b.r.nextBoolean()) {
					m.wall = Blocks.TUFF_BRICKS.defaultBlockState();
					m.alt = Blocks.POLISHED_TUFF.defaultBlockState();
					m.band = Blocks.CHISELED_TUFF_BRICKS.defaultBlockState();
					m.trim = Blocks.CHISELED_TUFF_BRICKS.defaultBlockState();
					m.stairs = Blocks.TUFF_BRICK_STAIRS;
					m.slab = Blocks.TUFF_BRICK_SLAB;
					m.post = Blocks.TUFF_BRICK_WALL;
					m.bandEvery = 9;
					m.altChance = 9;
				} else {
					m.wall = Blocks.STONE_BRICKS.defaultBlockState();
					m.alt = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
					m.band = Blocks.POLISHED_ANDESITE.defaultBlockState();
					m.trim = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
					m.stairs = Blocks.STONE_BRICK_STAIRS;
					m.slab = Blocks.STONE_BRICK_SLAB;
					m.post = Blocks.STONE_BRICK_WALL;
					m.bandEvery = 6;
					m.altChance = 12;
				}
			}
			case ANDALUSI, ARAB, PERSIAN, STEPPE -> {
				int k = b.r.nextInt(3);
				if (k == 0 || (c == Culture.PERSIAN && k == 2)) {
					// Mud brick (baked brick in Iran, with a glazed band).
					m.wall = Blocks.MUD_BRICKS.defaultBlockState();
					m.alt = Blocks.PACKED_MUD.defaultBlockState();
					m.band = c == Culture.PERSIAN ? Blocks.DYED_TERRACOTTA.pick(DyeColor.CYAN).defaultBlockState() : Blocks.MUD_BRICKS.defaultBlockState();
					m.trim = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
					m.stairs = Blocks.MUD_BRICK_STAIRS;
					m.slab = Blocks.MUD_BRICK_SLAB;
					m.post = Blocks.MUD_BRICK_WALL;
					m.bandEvery = c == Culture.PERSIAN ? 8 : 0;
					m.altChance = 8;
				} else if (k == 1) {
					// Cut sandstone ashlar.
					m.wall = Blocks.CUT_SANDSTONE.defaultBlockState();
					m.alt = Blocks.SANDSTONE.defaultBlockState();
					m.band = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
					m.trim = Blocks.CHISELED_SANDSTONE.defaultBlockState();
					m.stairs = Blocks.SANDSTONE_STAIRS;
					m.slab = Blocks.CUT_SANDSTONE_SLAB;
					m.post = Blocks.SANDSTONE_WALL;
					m.bandEvery = 7;
					m.altChance = 6;
				} else {
					// Rammed earth (tapial) with brick dressings.
					m.wall = Blocks.PACKED_MUD.defaultBlockState();
					m.alt = Blocks.MUD_BRICKS.defaultBlockState();
					m.band = Blocks.MUD_BRICKS.defaultBlockState();
					m.trim = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
					m.stairs = Blocks.MUD_BRICK_STAIRS;
					m.slab = Blocks.MUD_BRICK_SLAB;
					m.post = Blocks.MUD_BRICK_WALL;
					m.bandEvery = 4;
					m.altChance = 0;
				}
			}
			case LATIN -> {
				if (b.r.nextInt(3) == 0) {
					// Italy: brick with white stone courses.
					m.wall = Blocks.BRICKS.defaultBlockState();
					m.alt = Blocks.BRICKS.defaultBlockState();
					m.band = Blocks.SMOOTH_STONE.defaultBlockState();
					m.trim = Blocks.STONE_BRICKS.defaultBlockState();
					m.stairs = Blocks.BRICK_STAIRS;
					m.slab = Blocks.BRICK_SLAB;
					m.post = Blocks.BRICK_WALL;
					m.bandEvery = 8;
					m.altChance = 0;
				} else {
					northern(m, b);
				}
			}
			default -> northern(m, b);
		}
		return m;
	}

	// ================================================================ windows

	private static void cell(Bp b, boolean alongX, int plane, int u, int y, BlockState s) {
		if (alongX) {
			put(b, u, y, plane, s);
		} else {
			put(b, plane, y, u, s);
		}
	}

	/** One light (1 wide, 2 high) through a wall {@code t} thick. */
	private static void light(Bp b, boolean alongX, int plane, int inward, int t, int u, int yb, BlockState glass) {
		for (int dd = 0; dd < t; dd++) {
			for (int y = yb; y <= yb + 1; y++) {
				cell(b, alongX, plane + inward * dd, u, y, dd == 0 && glass != null ? glass : AIR);
			}
		}
	}

	/**
	 * A row of windows in one wall: the wall runs along x (alongX) or z from corner u0 to corner u1, its outer face is at
	 * {@code plane}, the wall is {@code t} thick towards {@code inward}. Lights only between lo and hi. Double windows are
	 * two lights with a little column between and a round head.
	 */
	private static void windowRow(Bp b, Mat m, boolean alongX, int plane, int inward, int t, int u0, int u1, int lo, int hi, int yb,
			boolean dbl, BlockState glass) {
		int mid = (u0 + u1) / 2;
		if (u1 - u0 < 8) {
			if (mid >= lo && mid <= hi) {
				light(b, alongX, plane, inward, t, mid, yb, glass);
			}
			return;
		}
		for (int cc = lo; cc <= hi; cc++) {
			if (Math.floorMod(cc - mid - 2, 4) != 0) {
				continue;
			}
			if (dbl && cc - 1 >= lo && cc + 1 <= hi) {
				light(b, alongX, plane, inward, t, cc - 1, yb, glass);
				light(b, alongX, plane, inward, t, cc + 1, yb, glass);
				for (int y = yb; y <= yb + 1; y++) {
					cell(b, alongX, plane, cc, y, m.post.defaultBlockState());
				}
				cell(b, alongX, plane, cc, yb + 2, m.trim);
				cell(b, alongX, plane, cc - 1, yb + 2, Bp.stairs(m.stairs, alongX ? Direction.WEST : Direction.NORTH, true));
				cell(b, alongX, plane, cc + 1, yb + 2, Bp.stairs(m.stairs, alongX ? Direction.EAST : Direction.SOUTH, true));
			} else {
				light(b, alongX, plane, inward, t, cc, yb, glass);
			}
		}
	}

	// ================================================================ furniture

	/** The castle's weapons: grindstone, barrels of arrows and spears, chests, smithing and fletching tables. */
	private static void armory(Bp b, int ix0, int iz0, int ix1, int iz1, int y) {
		BlockState[] items = {Blocks.GRINDSTONE.defaultBlockState(), barrel(Direction.UP), Bp.facing(Blocks.CHEST, Direction.WEST),
				Blocks.SMITHING_TABLE.defaultBlockState(), barrel(Direction.UP), Blocks.FLETCHING_TABLE.defaultBlockState(),
				Bp.facing(Blocks.ANVIL, Direction.SOUTH)};
		int i = 0;
		for (int z = iz0; z <= iz1 && i < items.length; z++) {
			if (in(b, ix1, z) && free(b, ix1, y, z)) {
				b.set(ix1, y, z, items[i]);
				i++;
			}
		}
		for (int x = ix1 - 1; x >= ix0 + 2 && i < items.length; x--) {
			if (in(b, x, iz1) && free(b, x, y, iz1)) {
				b.set(x, y, iz1, items[i]);
				i++;
			}
		}
	}

	/** Furnishes one storey of a tower (the inner free floor is ix0..ix1 × iz0..iz1, furniture at y). */
	private static void storey(Bp b, Culture c, int kind, int ix0, int iz0, int ix1, int iz1, int y, Block bed, int wealth) {
		if (ix1 < ix0 || iz1 < iz0 || !in(b, ix0, iz0) || !in(b, ix1, iz1)) {
			return;
		}
		switch (kind) {
			case K_ARMS -> armory(b, ix0, iz0, ix1, iz1, y);
			case K_SLEEP -> {
				if (iz1 > iz0 && free(b, ix1, y, iz1) && free(b, ix1, y, iz1 - 1)) {
					b.bed(bed, ix1, y, iz1, Direction.NORTH);
				}
				furnish(b, ix1 - 1, y, iz1, Bp.facing(Blocks.CHEST, Direction.NORTH));
			}
			case K_WATCH -> {
				furnish(b, ix1, y, iz0, barrel(Direction.UP));
				furnish(b, ix1, y + 1, iz0, Bp.lantern(false));
				furnish(b, ix0, y, iz1, barrel(Direction.UP));
				furnish(b, ix1, y, iz1, Bp.facing(Blocks.CHEST, Direction.WEST));
			}
			case K_STORE -> {
				furnish(b, ix1, y, iz0, barrel(Direction.UP));
				furnish(b, ix1, y + 1, iz0, barrel(Direction.UP));
				furnish(b, ix0, y, iz1, Blocks.HAY_BLOCK.defaultBlockState());
			}
			default -> {
			}
		}
		Interior.Use use = switch (kind) {
			case K_STORE, K_ARMS -> Interior.Use.STORE;
			case K_SLEEP -> Interior.Use.SLEEPING;
			case K_HALL -> Interior.Use.HALL;
			default -> null;
		};
		if (use != null) {
			Interior.room(b, c, ix0, iz0, ix1, iz1, y, use, wealth);
		}
	}

	/** Little heaps of the castle's stores in the corners of the plot (only where nothing stands yet). */
	private static void cornerProps(Bp b, Palette p, int inset) {
		int[][] cs = {{inset, inset, 1, 1}, {b.w - 1 - inset, inset, -1, 1}, {inset, b.d - 1 - inset, 1, -1}, {b.w - 1 - inset, b.d - 1 - inset, -1, -1}};
		int k = b.r.nextInt(4);
		BlockState hay = Blocks.HAY_BLOCK.defaultBlockState();
		for (int[] q : cs) {
			int x = q[0], z = q[1], sx = q[2], sz = q[3];
			if (!free(b, x, 1, z)) {
				k++;
				continue;
			}
			switch (k++ % 4) {
				case 0 -> {
					furnish(b, x, 1, z, hay);
					furnish(b, x + sx, 1, z, hay);
					furnish(b, x, 1, z + sz, hay);
					furnish(b, x, 2, z, hay);
				}
				case 1 -> {
					furnish(b, x, 1, z, Bp.log(p.log, Direction.Axis.Z));
					furnish(b, x + sx, 1, z, Bp.log(p.log, Direction.Axis.Z));
					furnish(b, x + 2 * sx, 1, z, Bp.log(p.log, Direction.Axis.Z));
					furnish(b, x + sx, 2, z, Bp.log(p.log, Direction.Axis.Z));
				}
				case 2 -> {
					furnish(b, x, 1, z, barrel(Direction.UP));
					furnish(b, x + sx, 1, z, barrel(Direction.UP));
					furnish(b, x, 1, z + sz, Blocks.DECORATED_POT.defaultBlockState());
					furnish(b, x, 2, z, barrel(Direction.UP));
				}
				default -> {
					furnish(b, x, 1, z, Bp.log(p.log, Direction.Axis.Y));
					furnish(b, x + sx, 1, z, Blocks.GRINDSTONE.defaultBlockState());
					furnish(b, x, 1, z + sz, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
				}
			}
		}
	}

	// ================================================================ roofs

	/**
	 * A round cone roof from radius R at y0, shrinking by {@code step} per layer (1 = 45°, less = steeper). Returns the y
	 * of the apex block.
	 */
	private static int cone(Bp b, double cx, double cz, double R, int y0, double step, Block stair, BlockState full) {
		int xc = (int) Math.round(cx), zc = (int) Math.round(cz);
		for (int i = 0; i < 80; i++) {
			double rr = R - i * step;
			int y = y0 + i;
			if (rr < 0.75) {
				put(b, xc, y, zc, full);
				return y;
			}
			double next = rr - step;
			int span = (int) Math.ceil(rr) + 1;
			for (int x = xc - span; x <= xc + span; x++) {
				for (int z = zc - span; z <= zc + span; z++) {
					double dx = x - cx, dz = z - cz, d = Math.hypot(dx, dz);
					if (d > rr + 0.2 || d <= rr - 1.2) {
						continue;
					}
					put(b, x, y, z, d <= next + 0.2 ? full : Bp.stairs(stair, toCenter(dx, dz), false));
				}
			}
		}
		return y0 + 80;
	}

	// ================================================================ the motte

	private static int moundHeight(double r, double rt, double R0, int H) {
		if (r <= rt + 0.5) {
			return H;
		}
		if (r >= R0) {
			return 0;
		}
		double t = (R0 - r) / Math.max(1.0, R0 - rt - 0.5);
		return (int) Math.round(H * Math.pow(Math.min(1.0, t), 1.15));
	}

	/** A motte: an earth mound with a ditch around it and a tower on top (plot 29×29, varies 23..35). */
	static void motte(Bp b, Palette p, Culture c) {
		int W = b.w, D = b.d;
		int mx = (W - 1) / 2, mz = (D - 1) / 2;
		int m = Math.min(W, D);
		double R0 = Math.max(6.0, (m - 6) / 2.0);
		double rt = Math.max(3.5, Math.min(6.0, 4.5 + (m - 23) / 8.0));
		rt = Math.max(3.0, Math.min(rt, R0 - 2.5));
		double Rd = m / 2.0 - 0.3;
		int zg = mz + (int) Math.floor(rt + 0.05);
		int H = Math.max(3, Math.min(m >= 33 ? 10 : m >= 27 ? 9 : 8, D - 1 - zg));
		boolean shell = b.r.nextInt(3) == 0;
		DyeColor[] col = colors(b, c);
		int[][] surf = new int[W][D];
		BlockState dirt = Blocks.DIRT.defaultBlockState();
		BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
		BlockState coarse = Blocks.COARSE_DIRT.defaultBlockState();
		BlockState logY = Bp.log(Blocks.SPRUCE_LOG, Direction.Axis.Y);

		// The mound (grass on the slopes, trodden earth on top) and the dry ditch at its foot.
		for (int x = 0; x < W; x++) {
			for (int z = 0; z < D; z++) {
				double r = Math.hypot(x - mx, z - mz);
				int h = moundHeight(r, rt, R0, H);
				if (h > 0) {
					for (int y = -1; y < h; y++) {
						b.set(x, y, z, dirt);
					}
					boolean top = r <= rt + 0.5;
					int k = b.r.nextInt(10);
					BlockState s;
					if (top) {
						s = k < 4 ? coarse : k < 6 ? Blocks.DIRT_PATH.defaultBlockState() : grass;
					} else {
						s = k == 0 ? coarse : k == 1 ? Blocks.ROOTED_DIRT.defaultBlockState() : grass;
					}
					b.set(x, h, z, s);
					surf[x][z] = h;
					if (!top && s == grass && b.r.nextInt(6) == 0) {
						b.set(x, h + 1, z, b.r.nextInt(4) == 0 ? Blocks.FERN.defaultBlockState() : Blocks.SHORT_GRASS.defaultBlockState());
					}
				} else if (r > R0 - 0.6 && r <= Rd) {
					double dIn = Math.max(0.0, r - R0), dOut = Math.max(0.0, Rd - r);
					int depth = Math.min(3, 1 + (int) (Math.min(dIn, dOut) * 2.2));
					for (int y = 1 - depth; y <= 0; y++) {
						b.set(x, y, z, AIR);
					}
					b.set(x, -depth, z, b.r.nextInt(3) == 0 ? Blocks.GRAVEL.defaultBlockState() : coarse);
					b.set(x, -depth - 1, z, dirt);
					surf[x][z] = -depth;
					if (depth == 3 && b.r.nextInt(5) == 0) {
						// Sharpened stakes in the bottom of the ditch.
						b.set(x, -2, z, Blocks.SPRUCE_FENCE.defaultBlockState());
					} else if (b.r.nextInt(7) == 0) {
						b.set(x, 1 - depth, z, Blocks.SHORT_GRASS.defaultBlockState());
					}
				}
			}
		}

		// The ring on top: a palisade of sharpened logs, or a stone shell wall.
		Mat sm = shell ? stone(b, c) : null;
		for (int x = 0; x < W; x++) {
			for (int z = 0; z < D; z++) {
				double r = Math.hypot(x - mx, z - mz);
				boolean gate = Math.abs(x - mx) <= 1 && z > mz;
				if (sm == null) {
					if (r > rt - 1.15 && r <= rt + 0.05 && !gate) {
						for (int y = H + 1; y <= H + 3; y++) {
							b.set(x, y, z, logY);
						}
						b.set(x, H + 4, z, Blocks.SPRUCE_FENCE.defaultBlockState());
					}
				} else if (r > rt - 1.7 && r <= rt + 0.3) {
					b.set(x, H - 1, z, sm.wall);
					b.set(x, H, z, sm.wall);
					for (int y = H + 1; y <= H + 5; y++) {
						if (!gate || y > H + 3) {
							b.set(x, y, z, sm.at(b.r, y));
						}
					}
					if (r > rt - 0.75 && Math.floorMod(x + z, 2) == 0) {
						b.set(x, H + 6, z, sm.wall);
					}
				}
			}
		}
		if (sm == null) {
			// Gate frame in the palisade.
			for (int s = -2; s <= 2; s += 4) {
				for (int y = H + 1; y <= H + 5; y++) {
					put(b, mx + s, y, zg, logY);
				}
				put(b, mx + s, H + 3, zg + 1, Bp.wallTorch(Direction.SOUTH));
			}
			for (int x = mx - 1; x <= mx + 1; x++) {
				put(b, x, H + 4, zg, Bp.log(Blocks.SPRUCE_LOG, Direction.Axis.X));
				put(b, x, H + 5, zg, Bp.slab(Blocks.SPRUCE_SLAB, false));
			}
		} else {
			put(b, mx, H + 4, zg, sm.trim);
			put(b, mx - 1, H + 3, zg, Bp.stairs(sm.stairs, Direction.WEST, true));
			put(b, mx + 1, H + 3, zg, Bp.stairs(sm.stairs, Direction.EAST, true));
		}

		// The timber tower: log corners, plank walls, a ring beam at every floor.
		int th = (sm == null && rt >= 5.75) ? 3 : 2;
		int n = sm != null ? 3 : (m >= 27 ? 4 : 3);
		int tx0 = mx - th, tx1 = mx + th, tz0 = mz - th, tz1 = mz + th;
		int yTop = H + 4 * n;
		BlockState pl = p.planks.defaultBlockState();
		b.fill(tx0, H - 2, tz0, tx1, H - 1, tz1, Blocks.COBBLESTONE.defaultBlockState());
		b.fill(tx0, H, tz0, tx1, H, tz1, pl);
		for (int y = H + 1; y <= yTop; y++) {
			for (int x = tx0; x <= tx1; x++) {
				for (int z = tz0; z <= tz1; z++) {
					boolean ex = x == tx0 || x == tx1, ez = z == tz0 || z == tz1;
					if (!ex && !ez) {
						if ((y - H) % 4 == 0) {
							b.set(x, y, z, pl);
						}
						continue;
					}
					BlockState s;
					if (ex && ez) {
						s = Bp.log(p.log, Direction.Axis.Y);
					} else if ((y - H) % 4 == 0) {
						s = Bp.log(p.strippedLog, ez ? Direction.Axis.X : Direction.Axis.Z);
					} else {
						s = pl;
					}
					b.set(x, y, z, s);
				}
			}
		}
		b.door(p.door, mx, H + 1, tz1, Direction.SOUTH);
		put(b, mx - 1, H + 3, tz1 + 1, Bp.wallTorch(Direction.SOUTH));
		put(b, mx + 1, H + 3, tz1 + 1, Bp.wallTorch(Direction.SOUTH));
		// Arrow slits.
		for (int k = 0; k < n; k++) {
			int y = H + 4 * k + 2;
			for (int dy = 0; dy <= 1; dy++) {
				b.set(tx0, y + dy, mz, AIR);
				b.set(tx1, y + dy, mz, AIR);
				b.set(mx, y + dy, tz0, AIR);
				if (k > 0) {
					b.set(mx, y + dy, tz1, AIR);
				}
			}
		}
		// Ladders through all floors up to the fighting gallery.
		int lx = tx0 + 1, lz = tz0 + 1;
		for (int y = H + 1; y <= yTop; y++) {
			b.set(lx, y, lz, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}
		// Storeys: stores, armoury, (hall), the lord's bed.
		int[] kinds = n >= 4 ? new int[]{K_STORE, K_ARMS, K_HALL, K_SLEEP} : new int[]{K_STORE, K_ARMS, K_SLEEP};
		Block bed = Blocks.BED.pick(col[0]);
		for (int k = 0; k < n; k++) {
			int y = H + 4 * k + 1;
			keepFree(b, lx + 1, y, lz, col[1]);
			keepFree(b, lx, y, lz + 1, col[1]);
			if (k == 0) {
				keepFree(b, mx, y, tz1 - 1, col[1]);
			}
			storey(b, c, kinds[k], tx0 + 1, tz0 + 1, tx1 - 1, tz1 - 1, y, bed, 1);
			furnish(b, mx, y + 2, mz, Bp.lantern(true));
		}
		// Hoarding: an overhanging timber gallery on brackets.
		int g0x = tx0 - 1, g1x = tx1 + 1, g0z = tz0 - 1, g1z = tz1 + 1;
		for (int x = g0x; x <= g1x; x++) {
			for (int z = g0z; z <= g1z; z++) {
				boolean ring = x == g0x || x == g1x || z == g0z || z == g1z;
				if (!ring) {
					continue;
				}
				boolean corner = (x == g0x || x == g1x) && (z == g0z || z == g1z);
				boolean zEdge = z == g0z || z == g1z;
				int u = zEdge ? x : z;
				b.set(x, yTop, z, pl);
				if (!corner && Math.floorMod(u, 2) == 0) {
					b.set(x, yTop - 1, z, Bp.stairs(p.stairs, ringInward(x, z, g0x, g0z, g1x, g1z), true));
				}
				for (int y = yTop + 1; y <= yTop + 3; y++) {
					BlockState s;
					if (corner) {
						s = Bp.log(p.log, Direction.Axis.Y);
					} else if (y == yTop + 2 && Math.floorMod(u, 2) == 1) {
						s = AIR;
					} else if (y == yTop + 3) {
						s = Bp.log(p.strippedLog, zEdge ? Direction.Axis.X : Direction.Axis.Z);
					} else {
						s = pl;
					}
					b.set(x, y, z, s);
				}
			}
		}
		furnish(b, tx1, yTop + 1, tz1, barrel(Direction.UP));
		furnish(b, tx1, yTop + 2, tz1, Bp.lantern(false));
		furnish(b, tx0, yTop + 1, tz1, barrel(Direction.UP));
		furnish(b, tx1, yTop + 1, tz0, Blocks.DECORATED_POT.defaultBlockState());
		// Pyramid roof of planks with a banner.
		boolean dk = b.r.nextBoolean();
		Block rs = dk ? Blocks.DARK_OAK_STAIRS : Blocks.SPRUCE_STAIRS;
		BlockState rf = (dk ? Blocks.DARK_OAK_PLANKS : Blocks.SPRUCE_PLANKS).defaultBlockState();
		int a = th + 2;
		b.pyramidRoof(mx - a, mx + a, mz - a, mz + a, yTop + 4, rs, rf, null);
		int apex = yTop + 4 + a;
		b.set(mx, apex + 1, mz, Blocks.SPRUCE_FENCE.defaultBlockState());
		b.set(mx, apex + 2, mz, Blocks.SPRUCE_FENCE.defaultBlockState());
		b.set(mx, apex + 3, mz, banner(col[0]));

		// Stores on the mound top between the tower and the ring.
		furnish(b, tx0 - 1, H + 1, mz, Blocks.HAY_BLOCK.defaultBlockState());
		furnish(b, tx1 + 1, H + 1, mz - 1, barrel(Direction.UP));
		furnish(b, tx1 + 1, H + 1, mz, Bp.log(p.log, Direction.Axis.Z));
		if (sm != null) {
			furnish(b, tx0 - 1, H + 1, mz - 1, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
		}

		// The flying bridge: a steep timber stair from the gate down over the ditch to the street.
		Block bs = Blocks.SPRUCE_STAIRS;
		for (int k = 1; k <= H; k++) {
			int z = zg + k, y = H + 1 - k;
			if (z > D - 1) {
				break;
			}
			for (int x = mx - 1; x <= mx + 1; x++) {
				put(b, x, y, z, Bp.stairs(bs, Direction.NORTH, false));
				for (int yy = y + 1; yy <= y + 3; yy++) {
					put(b, x, yy, z, AIR);
				}
			}
			boolean trestle = k % 2 == 0 || k == H;
			for (int sx = mx - 2; sx <= mx + 2; sx += 4) {
				if (!in(b, sx, z)) {
					continue;
				}
				if (trestle) {
					for (int yy = surf[sx][z] + 1; yy <= y; yy++) {
						b.set(sx, yy, z, logY);
					}
				} else if (y > surf[sx][z]) {
					b.set(sx, y, z, Bp.log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Z));
				}
				b.set(sx, y + 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
				if (k == H) {
					b.set(sx, y + 1, z, logY);
					b.set(sx, y + 2, z, logY);
					b.set(sx, y + 3, z, Bp.lantern(false));
				}
			}
			if (trestle) {
				for (int x = mx - 1; x <= mx + 1; x++) {
					if (y - 1 > surf[x][z]) {
						put(b, x, y - 1, z, Bp.log(Blocks.SPRUCE_LOG, Direction.Axis.X));
					}
				}
			}
		}
		for (int z = zg + H + 1; z < D; z++) {
			for (int x = mx - 1; x <= mx + 1; x++) {
				put(b, x, 0, z, surf[x][z] < 0 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState());
			}
		}
		cornerProps(b, p, 1);
	}

	// ================================================================ keeps

	/** A keep/donjon (plot 15×15, varies 11..21). variant 0..3 chosen by the caller. */
	static void keep(Bp b, Palette p, Culture c, int variant) {
		switch (Math.floorMod(variant, 4)) {
			case 0 -> normanKeep(b, p, c);
			case 1 -> roundKeep(b, p, c);
			case 2 -> woodTower(b, p, c);
			default -> stoneTower(b, p, c);
		}
	}

	/**
	 * Variant 0: a Norman hall-keep. Thick walls with shallow pilaster buttresses and corner turrets, a forebuilding with a
	 * stair to the first-floor door, a store with a well below, the great hall with its fireplace, the lord's chamber and
	 * a chapel above, battlements on top.
	 */
	private static void normanKeep(Bp b, Palette p, Culture c) {
		int W = b.w, D = b.d;
		if (W < 11 || D < 11) {
			stoneTower(b, p, c);
			return;
		}
		Mat m = stone(b, c);
		DyeColor[] col = colors(b, c);
		int t = W >= 13 && D >= 13 ? 2 : 1;
		int x0 = 1, x1 = W - 2, z0 = 1, z1 = D - 4;
		int f1 = W >= 13 ? 5 : 4, f2 = f1 + 6, f3 = f2 + 5;
		int ix0 = x0 + t, ix1 = x1 - t, iz0 = z0 + t, iz1 = z1 - t;
		int mxi = (ix0 + ix1) / 2, mzi = (iz0 + iz1) / 2;
		int xmid = (x0 + x1) / 2, zmid = (z0 + z1) / 2;
		int fx0 = x0, fx1 = x0 + f1 + 4, fz0 = z1 + 1, fz1 = D - 1;
		int dX = fx0 + 3 + f1;
		int lx = ix1 - 1, lz = iz0;
		BlockState planks = p.planks.defaultBlockState();

		// Foundations and a paved apron.
		b.fill(x0 - 1, -4, z0 - 1, x1 + 1, -1, D - 1, m.wall);
		b.fill(x0 - 1, 0, z0 - 1, x1 + 1, 0, D - 1, m.wall);
		// The walls and floors of the keep.
		for (int y = 0; y <= f3; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					boolean wall = x < ix0 || x > ix1 || z < iz0 || z > iz1;
					if (wall) {
						b.set(x, y, z, m.at(b.r, y));
					} else if (y == 0) {
						b.set(x, 0, z, b.r.nextInt(4) == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
					} else if (y == f1 || y == f2) {
						b.set(x, y, z, planks);
					} else if (y == f3) {
						b.set(x, y, z, m.wall);
					}
				}
			}
		}
		// Sloping plinth.
		for (int x = x0; x <= x1; x++) {
			b.set(x, 1, z0 - 1, Bp.stairs(m.stairs, Direction.SOUTH, false));
			if (x > fx1) {
				b.set(x, 1, z1 + 1, Bp.stairs(m.stairs, Direction.NORTH, false));
			}
		}
		for (int z = z0; z <= z1; z++) {
			b.set(x0 - 1, 1, z, Bp.stairs(m.stairs, Direction.EAST, false));
			b.set(x1 + 1, 1, z, Bp.stairs(m.stairs, Direction.WEST, false));
		}
		// Shallow pilaster buttresses (and banners between them on the street side).
		for (int z = z0 + 2; z <= z1 - 2; z++) {
			if (Math.floorMod(z - zmid, 4) == 0) {
				for (int y = 1; y <= f3; y++) {
					b.set(x0 - 1, y, z, m.at(b.r, y));
					b.set(x1 + 1, y, z, m.at(b.r, y));
				}
			}
		}
		for (int x = x0 + 2; x <= x1 - 2; x++) {
			if (Math.floorMod(x - xmid, 4) == 0) {
				for (int y = 1; y <= f3; y++) {
					b.set(x, y, z0 - 1, m.at(b.r, y));
				}
				if (x > fx1) {
					for (int y = 1; y <= f2 + 1; y++) {
						b.set(x, y, z1 + 1, m.at(b.r, y));
					}
					b.set(x, f2 + 3, z1 + 1, wallBanner(col[0], Direction.SOUTH));
				}
			}
		}
		// Battlements.
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (x != x0 && x != x1 && z != z0 && z != z1) {
					continue;
				}
				b.set(x, f3 + 1, z, m.wall);
				if (Math.floorMod(x + z, 2) == 0) {
					b.set(x, f3 + 2, z, m.wall);
				}
			}
		}
		// Corner turrets rising above the battlements (the front-left one corbelled out over the forebuilding).
		int[][] corners = {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}};
		for (int[] k : corners) {
			for (int x = k[0] - 1; x <= k[0] + 1; x++) {
				for (int z = k[1] - 1; z <= k[1] + 1; z++) {
					int ys = (z > z1 && x <= fx1) ? f1 + 5 : 1;
					boolean inner = x >= ix0 && x <= ix1 && z >= iz0 && z <= iz1;
					for (int y = ys; y <= f3 + 3; y++) {
						if (y <= f3 && inner) {
							continue;
						}
						put(b, x, y, z, m.at(b.r, y));
					}
					if (ys > 1 && x < fx0) {
						put(b, x, ys - 1, z, Bp.stairs(m.stairs, Direction.NORTH, true));
					}
					if ((x != k[0] || z != k[1]) && Math.floorMod(x + z, 2) == 0) {
						put(b, x, f3 + 4, z, m.wall);
					}
				}
			}
		}
		// Windows: slits below, round-arched double windows in the hall and the chamber.
		windowRow(b, m, true, z0, 1, t, x0, x1, x0 + 2, Math.min(x1 - 2, lx - 1), 2, false, null);
		windowRow(b, m, false, x0, 1, t, z0, z1, z0 + 2, z1 - 2, 2, false, null);
		windowRow(b, m, false, x1, -1, t, z0, z1, z0 + 2, z1 - 2, 2, false, null);
		windowRow(b, m, true, z1, -1, t, x0, x1, Math.max(x0 + 2, fx1 + 2), x1 - 2, 2, false, null);
		windowRow(b, m, true, z0, 1, t, x0, x1, x0 + 2, Math.min(x1 - 2, lx - 1), f1 + 2, true, null);
		windowRow(b, m, false, x1, -1, t, z0, z1, z0 + 2, z1 - 2, f1 + 2, true, null);
		windowRow(b, m, true, z1, -1, t, x0, x1, Math.max(x0 + 2, fx1 + 1), x1 - 2, f1 + 2, true, null);
		windowRow(b, m, true, z0, 1, t, x0, x1, x0 + 2, Math.min(x1 - 2, lx - 1), f2 + 2, true, pane(true));
		windowRow(b, m, true, z1, -1, t, x0, x1, x0 + 2, x1 - 2, f2 + 2, true, pane(true));
		windowRow(b, m, false, x0, 1, t, z0, z1, z0 + 2, z1 - 2, f2 + 2, true, pane(false));
		windowRow(b, m, false, x1, -1, t, z0, z1, z0 + 2, z1 - 2, f2 + 2, true, pane(false));

		// The forebuilding: a stair along the front up to the hall door.
		for (int y = 0; y <= f1 + 3; y++) {
			for (int x = fx0; x <= fx1; x++) {
				for (int z = fz0; z <= fz1; z++) {
					boolean edge = x == fx0 || x == fx1 || z == fz1;
					if (y == 0) {
						b.set(x, 0, z, edge ? m.wall : Blocks.COBBLESTONE.defaultBlockState());
					} else if (edge) {
						b.set(x, y, z, m.at(b.r, y));
					}
				}
			}
		}
		for (int x = fx0; x <= fx1; x++) {
			for (int z = fz0; z <= fz1; z++) {
				b.set(x, f1 + 4, z, m.wall);
				boolean edge = x == fx0 || x == fx1 || z == fz1;
				if (edge && Math.floorMod(x + z, 2) == 0) {
					b.set(x, f1 + 5, z, m.wall);
				}
			}
		}
		b.door(p.door, fx0 + 1, 1, fz1, Direction.SOUTH);
		b.set(fx0 + 1, 3, fz1, m.trim);
		for (int k = 0; k < f1; k++) {
			int x = fx0 + 2 + k, y = 1 + k;
			for (int z = fz0; z < fz1; z++) {
				for (int yy = 1; yy < y; yy++) {
					b.set(x, yy, z, m.wall);
				}
				b.set(x, y, z, Bp.stairs(m.stairs, Direction.EAST, false));
			}
		}
		for (int x = fx0 + 2 + f1; x <= fx1 - 1; x++) {
			for (int z = fz0; z < fz1; z++) {
				for (int yy = 1; yy <= f1; yy++) {
					b.set(x, yy, z, m.wall);
				}
			}
		}
		b.set(fx0 + 1, 3, fz0, Bp.wallTorch(Direction.SOUTH));
		b.set(dX - 1, f1 + 3, fz0, Bp.wallTorch(Direction.SOUTH));
		b.set(dX - 1, f1 + 2, fz1, AIR);
		// The great door into the hall.
		b.door(p.door, dX, f1 + 1, z1, Direction.SOUTH);
		if (t == 2) {
			b.set(dX, f1 + 1, z1 - 1, AIR);
			b.set(dX, f1 + 2, z1 - 1, AIR);
		}
		b.set(dX, f1 + 3, z1, m.trim);

		// Ladder from the store through every floor to the roof.
		for (int y = 1; y <= f3; y++) {
			b.set(lx, y, lz, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}

		// Fireplace in the side wall of the hall, its flue going up to a smoking stack on the roof.
		int fxp = t == 2 ? ix0 - 1 : ix0;
		b.set(fxp, f1 + 1, mzi, Blocks.CAMPFIRE.defaultBlockState());
		b.set(fxp, f1 + 2, mzi, AIR);
		if (t == 1) {
			for (int y = f1 + 1; y <= f1 + 2; y++) {
				b.set(ix0, y, mzi - 1, m.trim);
				b.set(ix0, y, mzi + 1, m.trim);
			}
		}
		for (int z = mzi - 1; z <= mzi + 1; z++) {
			b.set(ix0, f1 + 3, z, Bp.stairs(m.stairs, Direction.WEST, true));
			for (int y = f1 + 4; y < f2; y++) {
				b.set(ix0, y, z, m.wall);
			}
		}
		for (int y = f2 + 1; y < f3; y++) {
			b.set(ix0, y, mzi, m.wall);
		}
		b.set(ix0, f3 + 1, mzi, m.wall);
		b.set(ix0, f3 + 2, mzi, m.wall);
		b.set(ix0, f3 + 3, mzi, Blocks.CAMPFIRE.defaultBlockState());

		// ---- the store with the well (ground floor)
		for (int y = -4; y <= 0; y++) {
			b.set(mxi, y, mzi, Blocks.WATER.defaultBlockState());
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx != 0 || dz != 0) {
					b.set(mxi + dx, 1, mzi + dz, m.post.defaultBlockState());
				}
			}
		}
		keepFree(b, lx, 1, lz + 1, DyeColor.BROWN);
		keepFree(b, lx - 1, 1, lz, DyeColor.BROWN);
		b.set(ix0, 1, iz0, Blocks.HAY_BLOCK.defaultBlockState());
		b.set(ix0 + 1, 1, iz0, barrel(Direction.UP));
		b.set(ix0, 2, iz0, barrel(Direction.UP));
		storey(b, c, K_STORE, ix0, iz0, ix1, iz1, 1, p.bed, 2);
		furnish(b, mxi, f1 - 1, mzi + 1, Bp.lantern(true));

		// ---- the great hall
		int yh = f1 + 1;
		DyeColor rug = col[0];
		// The lord's high table at the back.
		for (int x = ix0 + 2; x <= ix1 - 2; x++) {
			b.set(x, yh, iz0 + 1, Bp.slab(Blocks.DARK_OAK_SLAB, true));
		}
		b.set(mxi, yh, iz0, Bp.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
		furnish(b, mxi - 1, yh, iz0, Bp.stairs(p.stairs, Direction.NORTH, false));
		furnish(b, mxi + 1, yh, iz0, Bp.stairs(p.stairs, Direction.NORTH, false));
		furnish(b, mxi - 1, yh + 1, iz0 + 1, candles(3));
		furnish(b, mxi + 1, yh + 1, iz0 + 1, candles(2));
		b.set(mxi, yh + 3, iz0, wallBanner(col[0], Direction.SOUTH));
		b.set(ix0, yh + 2, mzi - 2, wallBanner(col[1], Direction.EAST));
		b.set(ix0, yh + 2, mzi + 2, wallBanner(col[1], Direction.EAST));
		// A rug from the door along the front and up the side to the ladder.
		for (int x = ix0 + 1; x <= ix1; x++) {
			keepFree(b, x, yh, iz1, rug);
		}
		for (int z = iz0 + 1; z <= iz1; z++) {
			keepFree(b, lx, yh, z, rug);
		}
		// The long table with benches.
		for (int z = iz0 + 3; z <= iz1 - 2; z++) {
			furnish(b, mxi, yh, z, Bp.slab(Blocks.OAK_SLAB, true));
			furnish(b, mxi - 1, yh, z, Bp.stairs(p.stairs, Direction.WEST, false));
			furnish(b, mxi + 1, yh, z, Bp.stairs(p.stairs, Direction.EAST, false));
		}
		furnish(b, mxi, yh + 1, iz0 + 3, candles(2));
		// The armoury corner by the door.
		armory(b, ix0, iz0 + 1, ix1, iz1 - 1, yh);
		for (int z = iz0 + 2; z <= iz1 - 1; z += 3) {
			furnish(b, mxi, f2 - 1, z, Bp.lantern(true));
		}
		furnish(b, lx, f2 - 1, iz1 - 1, Bp.lantern(true));
		Interior.room(b, c, ix0, iz0, ix1, iz1, yh, Interior.Use.HALL, 2);

		// ---- the lord's chamber and the chapel
		int yu = f2 + 1;
		keepFree(b, lx, yu, lz + 1, rug);
		keepFree(b, lx - 1, yu, lz, rug);
		boolean chapel = ix1 - ix0 >= 6 && iz1 - iz0 >= 5;
		int xp = ix1 - 3, zq = iz1 - 3;
		if (chapel) {
			for (int y = yu; y < f3; y++) {
				for (int z = zq; z <= iz1; z++) {
					b.set(xp, y, z, p.plaster);
				}
				for (int x = xp; x <= ix1; x++) {
					b.set(x, y, zq, p.plaster);
				}
			}
			b.door(p.door, xp, yu, iz1 - 1, Direction.WEST);
			keepFree(b, xp + 1, yu, iz1 - 1, DyeColor.RED);
			keepFree(b, xp - 1, yu, iz1 - 1, rug);
			for (int z = zq + 1; z <= iz1; z++) {
				b.set(ix1, yu, z, m.trim);
			}
			b.set(ix1, yu + 1, zq + 1, candles(1));
			b.set(ix1, yu + 1, zq + 2, candles(3));
			if (zq + 3 <= iz1) {
				b.set(ix1, yu + 1, zq + 3, candles(1));
			}
			b.set(ix1 - 1, yu, zq + 1, Bp.facing(Blocks.LECTERN, Direction.WEST));
			b.set(ix1 - 1, yu, iz1, Blocks.CARPET.pick(DyeColor.RED).defaultBlockState());
			b.set(ix1 - 1, f3 - 1, zq + 2, Bp.lantern(true));
		}
		b.bed(Blocks.BED.pick(col[0]), ix0 + 1, yu, iz0 + 1, Direction.NORTH);
		furnish(b, ix0 + 1, yu, iz0 + 2, Bp.facing(Blocks.CHEST, Direction.SOUTH));
		furnish(b, ix0 + 2, yu, iz0, Blocks.BOOKSHELF.defaultBlockState());
		furnish(b, ix0, yu, iz0, Bp.facing(Blocks.LECTERN, Direction.EAST));
		for (int x = ix0 + 2; x <= Math.min(ix0 + 3, (chapel ? xp : ix1) - 1); x++) {
			for (int z = iz0 + 2; z <= iz0 + 3; z++) {
				keepFree(b, x, yu, z, col[0]);
			}
		}
		furnish(b, ix0 + 2, f3 - 1, iz0 + 2, Bp.lantern(true));
		Interior.room(b, c, ix0, iz0, chapel ? xp - 1 : ix1, iz1, yu, Interior.Use.SLEEPING, 2);

		// ---- the roof: banner pole, stores of stones.
		for (int y = f3 + 1; y <= f3 + 3; y++) {
			b.set(mxi, y, mzi, p.fence.defaultBlockState());
		}
		b.set(mxi, f3 + 4, mzi, banner(col[0]));
		furnish(b, ix1, f3 + 1, iz1, barrel(Direction.UP));
		furnish(b, ix0 + 1, f3 + 1, iz1, Blocks.COBBLESTONE.defaultBlockState());
	}

	/** Variant 1: a round stone tower keep of four storeys, with a conical roof or battlements. */
	private static void roundKeep(Bp b, Palette p, Culture c) {
		int W = b.w, D = b.d;
		if (W < 9 || D < 9) {
			woodTower(b, p, c);
			return;
		}
		Mat m = stone(b, c);
		DyeColor[] col = colors(b, c);
		double cx = (W - 1) / 2.0, cz = (D - 1) / 2.0;
		double R = Math.max(3.5, Math.min(W, D) / 2.0 - 2.0);
		int t = R >= 5 ? 2 : 1;
		double rin = R - t - 0.15;
		int sh = 5, n = 4, top = n * sh;
		boolean conical = switch (c) {
			case CELTIC -> true;
			case LATIN, NORSE, RUS, WEST_SLAV, MAGYAR, BALT_FINN -> b.r.nextBoolean();
			default -> false;
		};
		int xc = (int) Math.round(cx), zc = (int) Math.round(cz);
		BlockState planks = p.planks.defaultBlockState();
		int zf = zc, zfi = zc, zbi = zc;
		for (int x = 0; x < W; x++) {
			for (int z = 0; z < D; z++) {
				double d = Math.hypot(x - cx, z - cz);
				if (d > R + 1.1) {
					continue;
				}
				for (int y = -3; y < 0; y++) {
					b.set(x, y, z, m.wall);
				}
				if (d > R + 0.15) {
					// The battered plinth.
					b.set(x, 0, z, m.wall);
					b.set(x, 1, z, m.at(b.r, 1));
					b.set(x, 2, z, Bp.slab(m.slab, false));
					continue;
				}
				boolean wall = d > rin;
				for (int y = 0; y <= top; y++) {
					if (wall) {
						b.set(x, y, z, m.at(b.r, y));
					} else if (y == 0) {
						b.set(x, 0, z, b.r.nextInt(4) == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
					} else if (y % sh == 0) {
						b.set(x, y, z, y == top && !conical ? m.wall : planks);
					}
				}
				if (x == xc) {
					if (wall && z > zc) {
						zf = Math.max(zf, z);
					}
					if (!wall) {
						zfi = Math.max(zfi, z);
						zbi = Math.min(zbi, z);
					}
				}
			}
		}
		// Door at the street with a step through the plinth.
		b.door(p.door, xc, 1, zf, Direction.SOUTH);
		for (int z = zfi + 1; z < zf; z++) {
			b.set(xc, 1, z, AIR);
			b.set(xc, 2, z, AIR);
		}
		b.set(xc, 3, zf, m.trim);
		put(b, xc, 0, zf + 1, m.wall);
		put(b, xc, 1, zf + 1, AIR);
		put(b, xc, 2, zf + 1, AIR);
		for (int z = zf + 2; z < D; z++) {
			put(b, xc, 0, z, Blocks.DIRT_PATH.defaultBlockState());
		}
		// Ladder up the back wall.
		for (int y = 1; y <= top; y++) {
			b.set(xc, y, zbi, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}
		// Windows: slits below, small windows above (glazed in the lord's room).
		for (int k = 0; k < n; k++) {
			int yb = k * sh + 2;
			boolean glazed = k == n - 1;
			roundWindow(b, cx, cz, rin, R, xc, zc, -1, 0, yb, glazed ? pane(false) : null);
			roundWindow(b, cx, cz, rin, R, xc, zc, 1, 0, yb, glazed ? pane(false) : null);
			if (k > 0) {
				roundWindow(b, cx, cz, rin, R, xc, zc, 0, 1, yb, glazed ? pane(true) : null);
				if (rin >= 2.5) {
					roundWindow(b, cx, cz, rin, R, xc + 2, zc, 0, -1, yb, glazed ? pane(true) : null);
				}
			}
		}
		// Storeys.
		int half = (int) Math.floor(rin / Math.sqrt(2.0));
		while (half > 0 && (Math.hypot(xc + half - cx, zc + half - cz) > rin || Math.hypot(xc - half - cx, zc - half - cz) > rin
				|| Math.hypot(xc + half - cx, zc - half - cz) > rin || Math.hypot(xc - half - cx, zc + half - cz) > rin)) {
			half--;
		}
		int[] kinds = {K_STORE, K_HALL, K_ARMS, K_SLEEP};
		for (int k = 0; k < n; k++) {
			int y = k * sh + 1;
			keepFree(b, xc, y, zbi + 1, col[1]);
			if (k == 0) {
				for (int z = zbi + 1; z <= zfi; z++) {
					keepFree(b, xc, y, z, DyeColor.BROWN);
				}
				// Stores along the curved wall.
				for (int x = 0; x < W; x++) {
					for (int z = 0; z < D; z++) {
						double d = Math.hypot(x - cx, z - cz);
						if (d <= rin && d > rin - 1.0 && Math.abs(x - xc) > 1 && b.r.nextInt(3) == 0) {
							furnish(b, x, y, z, b.r.nextBoolean() ? barrel(Direction.UP) : Blocks.HAY_BLOCK.defaultBlockState());
						}
					}
				}
			}
			if (half >= 1) {
				storey(b, c, kinds[k], xc - half, zc - half, xc + half, zc + half, y, Blocks.BED.pick(col[0]), 2);
			}
			furnish(b, xc, (k + 1) * sh - 1, zc, Bp.lantern(true));
		}
		// The top.
		if (conical) {
			Block cs;
			BlockState cf;
			double step = 1.0;
			if (c == Culture.CELTIC) {
				cs = m.stairs;
				cf = m.wall;
				step = 0.6;
			} else if (p.thatch) {
				cs = p.stairs;
				cf = planks;
			} else {
				cs = p.roofStairs;
				cf = p.roofFull;
			}
			// The attic stores.
			furnish(b, xc + 1, top + 1, zc, Blocks.HAY_BLOCK.defaultBlockState());
			furnish(b, xc - 1, top + 1, zc, barrel(Direction.UP));
			int apex = cone(b, cx, cz, R + 1.0, top + 1, step, cs, cf);
			if (c == Culture.CELTIC) {
				b.set(xc, apex + 1, zc, m.post.defaultBlockState());
			} else {
				b.set(xc, apex + 1, zc, p.fence.defaultBlockState());
				b.set(xc, apex + 2, zc, p.fence.defaultBlockState());
				b.set(xc, apex + 3, zc, banner(col[0]));
			}
		} else {
			for (int x = 0; x < W; x++) {
				for (int z = 0; z < D; z++) {
					double d = Math.hypot(x - cx, z - cz);
					if (d > R - 0.85 && d <= R + 0.15) {
						b.set(x, top + 1, z, m.wall);
						if (Math.floorMod(x + z, 2) == 0) {
							b.set(x, top + 2, z, m.wall);
						}
					}
				}
			}
			for (int y = top + 1; y <= top + 3; y++) {
				b.set(xc, y, zc, p.fence.defaultBlockState());
			}
			b.set(xc, top + 4, zc, banner(col[0]));
			furnish(b, xc + 1, top + 1, zc + 1, barrel(Direction.UP));
		}
		cornerProps(b, p, 1);
	}

	/** Cuts a window through a round wall along a straight line from (x, z); the outermost cell may be glazed. */
	private static void roundWindow(Bp b, double cx, double cz, double rin, double R, int x, int z, int dx, int dz, int yb, BlockState glass) {
		int lastX = -1, lastZ = -1;
		for (int s = 0; s < 64; s++, x += dx, z += dz) {
			if (!in(b, x, z)) {
				break;
			}
			double d = Math.hypot(x - cx, z - cz);
			if (d > R + 0.15) {
				break;
			}
			if (d > rin) {
				b.set(x, yb, z, AIR);
				b.set(x, yb + 1, z, AIR);
				lastX = x;
				lastZ = z;
			}
		}
		if (glass != null && lastX >= 0) {
			b.set(lastX, yb, lastZ, glass);
			b.set(lastX, yb + 1, lastZ, glass);
		}
	}

	/** A sharpened-log palisade round the plot edge, with a gate in the street side at gx. */
	private static void palisade(Bp b, Palette p, int gx) {
		int W = b.w, D = b.d;
		BlockState lg = Bp.log(p.log, Direction.Axis.Y);
		BlockState tip = p.fence.defaultBlockState();
		for (int x = 0; x < W; x++) {
			for (int z = 0; z < D; z++) {
				if (x != 0 && x != W - 1 && z != 0 && z != D - 1) {
					continue;
				}
				b.set(x, -1, z, Blocks.DIRT.defaultBlockState());
				if (z == D - 1 && Math.abs(x - gx) <= 1) {
					b.set(x, 0, z, Blocks.DIRT_PATH.defaultBlockState());
					continue;
				}
				b.set(x, 0, z, Blocks.COARSE_DIRT.defaultBlockState());
				for (int y = 1; y <= 3; y++) {
					b.set(x, y, z, lg);
				}
				b.set(x, 4, z, tip);
			}
		}
		for (int s = -2; s <= 2; s += 4) {
			for (int y = 1; y <= 5; y++) {
				put(b, gx + s, y, D - 1, lg);
			}
			put(b, gx + s, 6, D - 1, tip);
			put(b, gx + s, 3, D - 2, Bp.wallTorch(Direction.NORTH));
		}
		for (int x = gx - 1; x <= gx + 1; x++) {
			put(b, x, 4, D - 1, Bp.log(p.log, Direction.Axis.X));
			put(b, x, 5, D - 1, Bp.slab(p.slab, false));
		}
	}

	/**
	 * Variant 2: a log-cribbed wooden tower (вежа / повалуша) of the Rus, Slavs, Magyars and Balts, with an overhanging
	 * fighting gallery (облам), a steep tent roof and a little flag, inside a palisade.
	 */
	private static void woodTower(Bp b, Palette p, Culture c) {
		int W = b.w, D = b.d, mm = Math.min(W, D);
		DyeColor[] col = colors(b, c);
		int mx = (W - 1) / 2, mz = (D - 1) / 2;
		int th = Math.max(2, Math.min(4, (mm - 9) / 2));
		th = Math.max(1, Math.min(th, Math.min(mx, mz) - 3));
		int n = mm >= 17 ? 4 : 3, sh = 4, top = n * sh;
		int tx0 = mx - th, tx1 = mx + th, tz0 = mz - th, tz1 = mz + th;
		int ix0 = tx0 + 1, ix1 = tx1 - 1, iz0 = tz0 + 1, iz1 = tz1 - 1;
		Block log = p.log;
		BlockState planks = p.planks.defaultBlockState();

		palisade(b, p, mx);
		for (int x = 1; x < W - 1; x++) {
			for (int z = 1; z < D - 1; z++) {
				if (b.r.nextInt(3) == 0) {
					b.set(x, 0, z, Blocks.COARSE_DIRT.defaultBlockState());
				}
			}
		}
		for (int z = tz1 + 1; z < D - 1; z++) {
			b.set(mx, 0, z, Blocks.DIRT_PATH.defaultBlockState());
		}
		b.fill(tx0, -3, tz0, tx1, -1, tz1, Blocks.COBBLESTONE.defaultBlockState());
		b.fill(ix0, 0, iz0, ix1, 0, iz1, planks);
		// The log crib, the log ends crossing out at the corners.
		for (int y = 0; y <= top; y++) {
			for (int x = tx0; x <= tx1; x++) {
				b.set(x, y, tz0, Bp.log(log, Direction.Axis.X));
				b.set(x, y, tz1, Bp.log(log, Direction.Axis.X));
			}
			for (int z = tz0; z <= tz1; z++) {
				b.set(tx0, y, z, Bp.log(log, Direction.Axis.Z));
				b.set(tx1, y, z, Bp.log(log, Direction.Axis.Z));
			}
			boolean even = y % 2 == 0;
			Direction.Axis ax = even ? Direction.Axis.X : Direction.Axis.Z;
			b.set(tx0, y, tz0, Bp.log(log, ax));
			b.set(tx1, y, tz0, Bp.log(log, ax));
			b.set(tx0, y, tz1, Bp.log(log, ax));
			b.set(tx1, y, tz1, Bp.log(log, ax));
			if (y >= 1 && y < top) {
				if (even) {
					put(b, tx0 - 1, y, tz0, Bp.log(log, ax));
					put(b, tx1 + 1, y, tz0, Bp.log(log, ax));
					put(b, tx0 - 1, y, tz1, Bp.log(log, ax));
					put(b, tx1 + 1, y, tz1, Bp.log(log, ax));
				} else {
					put(b, tx0, y, tz0 - 1, Bp.log(log, ax));
					put(b, tx1, y, tz0 - 1, Bp.log(log, ax));
					put(b, tx0, y, tz1 + 1, Bp.log(log, ax));
					put(b, tx1, y, tz1 + 1, Bp.log(log, ax));
				}
			}
		}
		for (int k = 1; k <= n; k++) {
			b.fill(ix0, k * sh, iz0, ix1, k * sh, iz1, planks);
		}
		// Door with a little roofed porch (крыльцо).
		b.door(p.door, mx, 1, tz1, Direction.SOUTH);
		for (int s = -1; s <= 1; s += 2) {
			put(b, mx + s, 1, tz1 + 1, p.fence.defaultBlockState());
			put(b, mx + s, 2, tz1 + 1, p.fence.defaultBlockState());
		}
		for (int x = mx - 1; x <= mx + 1; x++) {
			put(b, x, 3, tz1 + 1, Bp.slab(p.slab, false));
		}
		// Small windows with shutters, and loopholes.
		for (int k = 1; k < n; k++) {
			int y = k * sh + 2;
			b.set(mx, y, tz1, AIR);
			put(b, mx, y, tz1 + 1, Bp.trapdoor(p.trapdoor, Direction.SOUTH, false, true));
			b.set(tx0, y, mz, AIR);
			put(b, tx0 - 1, y, mz, Bp.trapdoor(p.trapdoor, Direction.WEST, false, true));
			b.set(tx1, y, mz, AIR);
			put(b, tx1 + 1, y, mz, Bp.trapdoor(p.trapdoor, Direction.EAST, false, true));
			b.set(mx, y, tz0, AIR);
		}
		b.set(tx0, 2, mz, AIR);
		b.set(tx1, 2, mz, AIR);
		// Ladders.
		for (int y = 1; y <= top; y++) {
			b.set(ix0, y, iz0, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}
		int[] kinds = n >= 4 ? new int[]{K_STORE, K_HALL, K_ARMS, K_SLEEP} : new int[]{K_STORE, K_ARMS, K_SLEEP};
		for (int k = 0; k < n; k++) {
			int y = k * sh + 1;
			keepFree(b, ix0 + 1, y, iz0, col[1]);
			keepFree(b, ix0, y, iz0 + 1, col[1]);
			if (k == 0) {
				keepFree(b, mx, y, iz1, DyeColor.BROWN);
			}
			storey(b, c, kinds[k], ix0, iz0, ix1, iz1, y, p.bed, 1);
			furnish(b, mx, (k + 1) * sh - 1, mz, Bp.lantern(true));
		}
		// The fighting gallery (облам) overhanging on beam ends.
		int g0x = tx0 - 1, g1x = tx1 + 1, g0z = tz0 - 1, g1z = tz1 + 1;
		for (int x = g0x; x <= g1x; x++) {
			for (int z = g0z; z <= g1z; z++) {
				boolean ring = x == g0x || x == g1x || z == g0z || z == g1z;
				if (!ring) {
					continue;
				}
				boolean corner = (x == g0x || x == g1x) && (z == g0z || z == g1z);
				boolean zEdge = z == g0z || z == g1z;
				int u = zEdge ? x : z;
				if (corner) {
					b.set(x, top, z, planks);
				} else if (Math.floorMod(u, 3) == 0) {
					// A hatch to drop stones through.
					b.set(x, top, z, Bp.trapdoor(p.trapdoor, ringInward(x, z, g0x, g0z, g1x, g1z), true, false));
				} else {
					b.set(x, top, z, Bp.log(log, zEdge ? Direction.Axis.Z : Direction.Axis.X));
				}
				if (!corner && Math.floorMod(u, 2) == 0) {
					b.set(x, top - 1, z, Bp.stairs(p.stairs, ringInward(x, z, g0x, g0z, g1x, g1z), true));
				}
				for (int y = top + 1; y <= top + 3; y++) {
					BlockState s;
					if (corner) {
						s = Bp.log(log, Direction.Axis.Y);
					} else if (y == top + 2 && Math.floorMod(u, 2) == 1) {
						s = AIR;
					} else {
						s = Bp.log(log, zEdge ? Direction.Axis.X : Direction.Axis.Z);
					}
					b.set(x, y, z, s);
				}
				b.set(x, top + 4, z, planks);
				b.set(x, top + 5, z, planks);
			}
		}
		furnish(b, tx1, top + 1, tz1, barrel(Direction.UP));
		furnish(b, tx0, top + 1, tz1, barrel(Direction.UP));
		furnish(b, tx0, top + 2, tz1, Bp.lantern(false));
		furnish(b, tx1, top + 1, tz0, Blocks.COBBLESTONE.defaultBlockState());
		// The steep tent roof (шатёр) with a flag.
		int a = th + 2;
		int y = top + 4;
		for (int h = a; h >= 0; h--, y += 2) {
			if (h == 0) {
				b.set(mx, y, mz, planks);
				b.set(mx, y + 1, mz, p.fence.defaultBlockState());
				b.set(mx, y + 2, mz, p.fence.defaultBlockState());
				b.set(mx, y + 3, mz, banner(col[0]));
				break;
			}
			for (int x = mx - h; x <= mx + h; x++) {
				for (int z = mz - h; z <= mz + h; z++) {
					if (Math.max(Math.abs(x - mx), Math.abs(z - mz)) != h) {
						continue;
					}
					Direction f = Math.abs(z - mz) == h ? (z < mz ? Direction.SOUTH : Direction.NORTH) : (x < mx ? Direction.EAST : Direction.WEST);
					put(b, x, y, z, planks);
					put(b, x, y + 1, z, Bp.stairs(p.stairs, f, false));
				}
			}
		}
		// The yard: woodpile, beehive, trough.
		cornerProps(b, p, 1);
		furnish(b, 1, 1, mz, Bp.facing(Blocks.BEEHIVE, Direction.EAST));
		furnish(b, W - 2, 1, mz, Bp.log(log, Direction.Axis.Z));
		furnish(b, W - 2, 1, mz + 1, Bp.log(log, Direction.Axis.Z));
		furnish(b, W - 2, 2, mz, Bp.log(log, Direction.Axis.Z));
	}

	/** A low curtain wall round the plot edge with an arched gate in the street side. */
	private static void curtain(Bp b, Mat m, int gx, boolean pointed) {
		int W = b.w, D = b.d;
		for (int x = 0; x < W; x++) {
			for (int z = 0; z < D; z++) {
				if (x != 0 && x != W - 1 && z != 0 && z != D - 1) {
					continue;
				}
				for (int y = -2; y <= 0; y++) {
					b.set(x, y, z, m.wall);
				}
				boolean gate = z == D - 1 && Math.abs(x - gx) <= 1;
				for (int y = 1; y <= 4; y++) {
					if (!gate || y == 4) {
						b.set(x, y, z, m.at(b.r, y));
					}
				}
				if (Math.floorMod(x + z, 2) == 0) {
					b.set(x, 5, z, m.wall);
					if (pointed) {
						b.set(x, 6, z, m.post.defaultBlockState());
					}
				}
			}
		}
		put(b, gx - 1, 3, D - 1, Bp.stairs(m.stairs, Direction.WEST, true));
		put(b, gx + 1, 3, D - 1, Bp.stairs(m.stairs, Direction.EAST, true));
		put(b, gx, 4, D - 1, m.trim);
		for (int s = -2; s <= 2; s += 4) {
			for (int y = 1; y <= 6; y++) {
				put(b, gx + s, y, D - 1, m.at(b.r, y));
			}
			put(b, gx + s, 7, D - 1, pointed ? m.post.defaultBlockState() : m.wall);
			put(b, gx + s, 3, D - 2, Bp.wallTorch(Direction.NORTH));
		}
	}

	/**
	 * Variant 3: a stone tower of Byzantium, the Caucasus or the Islamic world inside a curtain wall: brick bands in
	 * stone, fine ashlar with a corbelled gallery and a stone pyramid roof, or mud brick / sandstone with pointed merlons.
	 */
	private static void stoneTower(Bp b, Palette p, Culture c) {
		int W = b.w, D = b.d, mm = Math.min(W, D);
		Mat m = stone(b, c);
		DyeColor[] col = colors(b, c);
		boolean islamic = c == Culture.ANDALUSI || c == Culture.ARAB || c == Culture.PERSIAN || c == Culture.STEPPE;
		boolean cauc = c == Culture.CAUCASUS;
		int mx = (W - 1) / 2, mz = (D - 1) / 2;
		int th = Math.max(2, Math.min(5, (mm - 7) / 2));
		th = Math.max(1, Math.min(th, Math.min(mx, mz) - 2));
		int t = th >= 4 ? 2 : 1;
		int n = cauc ? 5 : 4, sh = 5, top = n * sh;
		int tx0 = mx - th, tx1 = mx + th, tz0 = mz - th, tz1 = mz + th;
		int ix0 = tx0 + t, ix1 = tx1 - t, iz0 = tz0 + t, iz1 = tz1 - t;
		BlockState planks = p.planks.defaultBlockState();

		curtain(b, m, mx, islamic);
		for (int x = 1; x < W - 1; x++) {
			for (int z = 1; z < D - 1; z++) {
				int k = b.r.nextInt(4);
				b.set(x, 0, z, k == 0 ? Blocks.GRAVEL.defaultBlockState() : k == 1 ? Blocks.COARSE_DIRT.defaultBlockState() : p.floor);
			}
		}
		b.fill(tx0 - 1, -3, tz0 - 1, tx1 + 1, -1, tz1 + 1, m.wall);
		// A battered base where there is room.
		boolean talus = tx0 - 1 >= 2 && tz0 - 1 >= 2 && tx1 + 1 <= W - 3 && tz1 + 1 <= D - 3;
		if (talus) {
			for (int x = tx0 - 1; x <= tx1 + 1; x++) {
				for (int z = tz0 - 1; z <= tz1 + 1; z++) {
					if (x != tx0 - 1 && x != tx1 + 1 && z != tz0 - 1 && z != tz1 + 1) {
						continue;
					}
					b.set(x, 0, z, m.wall);
					b.set(x, 1, z, m.at(b.r, 1));
					b.set(x, 2, z, Bp.stairs(m.stairs, ringInward(x, z, tx0 - 1, tz0 - 1, tx1 + 1, tz1 + 1), false));
				}
			}
		}
		// The body of the tower.
		for (int y = 0; y <= top; y++) {
			for (int x = tx0; x <= tx1; x++) {
				for (int z = tz0; z <= tz1; z++) {
					boolean wall = x < ix0 || x > ix1 || z < iz0 || z > iz1;
					if (wall) {
						b.set(x, y, z, m.at(b.r, y));
					} else if (y == 0) {
						b.set(x, 0, z, p.floor);
					} else if (y == top) {
						b.set(x, y, z, m.wall);
					} else if (y % sh == 0) {
						b.set(x, y, z, planks);
					}
				}
			}
		}
		// Door with an arched head.
		b.door(p.door, mx, 1, tz1, Direction.SOUTH);
		if (t == 2) {
			b.set(mx, 1, tz1 - 1, AIR);
			b.set(mx, 2, tz1 - 1, AIR);
		}
		b.set(mx, 3, tz1, m.trim);
		put(b, mx - 1, 3, tz1 + 1, Bp.wallTorch(Direction.SOUTH));
		put(b, mx + 1, 3, tz1 + 1, Bp.wallTorch(Direction.SOUTH));
		if (talus) {
			b.set(mx, 0, tz1 + 1, m.wall);
			b.set(mx, 1, tz1 + 1, AIR);
			b.set(mx, 2, tz1 + 1, AIR);
		}
		for (int z = tz1 + (talus ? 2 : 1); z < D - 1; z++) {
			b.set(mx, 0, z, islamic ? Blocks.SMOOTH_SANDSTONE.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState());
		}
		// Windows.
		for (int k = 0; k < n; k++) {
			int yb = k * sh + 2;
			boolean dbl = k == 1;
			boolean high = k == n - 1 || (cauc && k == n - 2);
			BlockState gx = k == 0 ? null : high ? (islamic ? null : pane(true)) : null;
			BlockState gz = k == 0 ? null : high ? (islamic ? null : pane(false)) : null;
			windowRow(b, m, false, tx0, 1, t, tz0, tz1, tz0 + t, tz1 - t, yb, dbl, gz);
			windowRow(b, m, false, tx1, -1, t, tz0, tz1, tz0 + t, tz1 - t, yb, dbl, gz);
			if (k > 0) {
				windowRow(b, m, true, tz0, 1, t, tx0, tx1, tx0 + t, tx1 - t, yb, dbl, gx);
				windowRow(b, m, true, tz1, -1, t, tx0, tx1, tx0 + t, tx1 - t, yb, dbl, gx);
			}
		}
		if (islamic) {
			// Lattice screens in the upper windows.
			int yb = (n - 1) * sh + 2;
			furnish(b, mx, yb, tz1, Blocks.IRON_BARS.defaultBlockState());
			furnish(b, tx0, yb, mz, Blocks.IRON_BARS.defaultBlockState());
			furnish(b, tx1, yb, mz, Blocks.IRON_BARS.defaultBlockState());
		}
		// Ladders.
		for (int y = 1; y <= top; y++) {
			b.set(ix0, y, iz0, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}
		int[] kinds = {K_STORE, K_HALL, K_ARMS, K_SLEEP, K_WATCH};
		int bx = tx1 - tx0 < 8 ? mx + 1 : mx;
		for (int k = 0; k < n; k++) {
			int y = k * sh + 1;
			keepFree(b, ix0 + 1, y, iz0, col[1]);
			keepFree(b, ix0, y, iz0 + 1, col[1]);
			if (k == 0) {
				keepFree(b, mx, y, iz1, col[1]);
				furnish(b, ix1, y, iz1 - 1, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
			}
			if (kinds[k] == K_HALL) {
				furnish(b, bx, y + 2, iz0, wallBanner(col[0], Direction.SOUTH));
			}
			storey(b, c, kinds[Math.min(k, kinds.length - 1)], ix0, iz0, ix1, iz1, y, p.bed, 2);
			furnish(b, mx, (k + 1) * sh - 1, mz, Bp.lantern(true));
		}
		// The top.
		if (cauc) {
			// A corbelled gallery with drop-holes and a stone pyramid roof (Svan / Armenian tower).
			int g0x = tx0 - 1, g1x = tx1 + 1, g0z = tz0 - 1, g1z = tz1 + 1;
			for (int x = g0x; x <= g1x; x++) {
				for (int z = g0z; z <= g1z; z++) {
					boolean ring = x == g0x || x == g1x || z == g0z || z == g1z;
					if (!ring) {
						continue;
					}
					boolean corner = (x == g0x || x == g1x) && (z == g0z || z == g1z);
					int u = (z == g0z || z == g1z) ? x : z;
					boolean hole = !corner && Math.floorMod(u, 2) == 1;
					if (!corner && !hole) {
						b.set(x, top - 1, z, Bp.stairs(m.stairs, ringInward(x, z, g0x, g0z, g1x, g1z), true));
					}
					b.set(x, top, z, hole ? AIR : m.wall);
					b.set(x, top + 1, z, m.at(b.r, top + 1));
					b.set(x, top + 2, z, !corner && Math.floorMod(u, 2) == 0 ? AIR : m.wall);
					b.set(x, top + 3, z, m.wall);
				}
			}
			int a = th + 2;
			b.pyramidRoof(mx - a, mx + a, mz - a, mz + a, top + 4, m.stairs, m.wall, null);
			int apex = top + 4 + a;
			b.set(mx, apex + 1, mz, p.fence.defaultBlockState());
			b.set(mx, apex + 2, mz, p.fence.defaultBlockState());
			b.set(mx, apex + 3, mz, banner(col[0]));
			furnish(b, tx1, top + 1, tz1, barrel(Direction.UP));
		} else {
			for (int x = tx0; x <= tx1; x++) {
				for (int z = tz0; z <= tz1; z++) {
					if (x != tx0 && x != tx1 && z != tz0 && z != tz1) {
						continue;
					}
					if (c == Culture.PERSIAN) {
						b.set(x, top - 1, z, Blocks.DYED_TERRACOTTA.pick(DyeColor.CYAN).defaultBlockState());
					}
					b.set(x, top + 1, z, m.wall);
					if (Math.floorMod(x + z, 2) == 0) {
						b.set(x, top + 2, z, m.wall);
						if (islamic) {
							b.set(x, top + 3, z, m.post.defaultBlockState());
						}
					}
				}
			}
			for (int y = top + 1; y <= top + 3; y++) {
				b.set(mx, y, mz, p.fence.defaultBlockState());
			}
			b.set(mx, top + 4, mz, banner(col[0]));
			furnish(b, ix1, top + 1, iz1, barrel(Direction.UP));
			furnish(b, ix1 - 1, top + 1, iz1, Blocks.DECORATED_POT.defaultBlockState());
		}
		cornerProps(b, p, 1);
	}

	// ================================================================ the Trelleborg hall

	/**
	 * A Trelleborg-type Viking hall: a boat-shaped longhouse with bowed long walls (plot 11×31, varies 9..13 × 21..35),
	 * long axis along z; doors in the middle of both long sides and in both gable ends.
	 */
	static void longhouse(Bp b, Palette p, Culture c) {
		int W = b.w, D = b.d;
		if (W < 7 || D < 13) {
			Houses.longhouse(b, p, 1, 1, Math.max(2, W - 2), Math.max(2, D - 2));
			return;
		}
		int mx = (W - 1) / 2;
		int z0 = 1, z1 = D - 2;
		int zm = (z0 + z1) / 2;
		DyeColor[] col = colors(b, c);
		int hwMid = Math.max(2, Math.min(6, (W - 1) / 2 - 1));
		int bulge = W >= 12 && D >= 25 ? 2 : 1;
		int hwEnd = Math.max(2, hwMid - bulge);
		int[] hw = new int[D];
		for (int z = 0; z < D; z++) {
			int zz = Math.max(z0, Math.min(z1, z));
			double tt = (zz - z0) / (double) Math.max(1, z1 - z0);
			hw[z] = hwEnd + (int) Math.round((hwMid - hwEnd) * Math.sin(Math.PI * tt));
		}
		boolean[][] inside = new boolean[W][D];
		for (int x = 0; x < W; x++) {
			for (int z = 0; z < D; z++) {
				inside[x][z] = z >= z0 && z <= z1 && Math.abs(x - mx) <= hw[z];
			}
		}
		boolean[][] wall = new boolean[W][D];
		for (int x = 0; x < W; x++) {
			for (int z = 0; z < D; z++) {
				if (!inside[x][z]) {
					continue;
				}
				for (int dx = -1; dx <= 1 && !wall[x][z]; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						int nx = x + dx, nz = z + dz;
						if (nx < 0 || nx >= W || nz < 0 || nz >= D || !inside[nx][nz]) {
							wall[x][z] = true;
							break;
						}
					}
				}
			}
		}
		Block postLog = Blocks.DARK_OAK_LOG;
		Block stave = b.r.nextBoolean() ? Blocks.STRIPPED_OAK_LOG : Blocks.STRIPPED_DARK_OAK_LOG;
		BlockState boards = (b.r.nextBoolean() ? Blocks.OAK_PLANKS : Blocks.SPRUCE_PLANKS).defaultBlockState();
		Block rs = Blocks.DARK_OAK_STAIRS, rsl = Blocks.DARK_OAK_SLAB;
		BlockState rf = Blocks.DARK_OAK_PLANKS.defaultBlockState();
		Block door = p.door;
		int ye = W >= 11 ? 5 : 4;

		// Floor, sill stones and the bowed stave walls.
		for (int x = 0; x < W; x++) {
			for (int z = 0; z < D; z++) {
				if (!inside[x][z]) {
					continue;
				}
				b.set(x, -1, z, Blocks.DIRT.defaultBlockState());
				if (wall[x][z]) {
					b.set(x, 0, z, b.r.nextInt(3) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
					boolean end = z == z0 || z == z1;
					boolean post = end ? Math.floorMod(x - mx, 2) == 0 : Math.floorMod(z - zm, 3) == 0;
					for (int y = 1; y < ye; y++) {
						BlockState s;
						if (y == ye - 1) {
							s = Bp.log(postLog, end ? Direction.Axis.X : Direction.Axis.Z);
						} else {
							s = Bp.log(post ? postLog : stave, Direction.Axis.Y);
						}
						b.set(x, y, z, s);
					}
				} else {
					int k = b.r.nextInt(10);
					b.set(x, 0, z, k < 6 ? Blocks.PACKED_MUD.defaultBlockState() : k < 9 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT.defaultBlockState());
				}
			}
		}
		// External props along the long walls, carrying the eaves.
		for (int z = z0 + 1; z < z1; z++) {
			if (Math.floorMod(z - zm, 3) != 0 || Math.abs(z - zm) <= 1) {
				continue;
			}
			for (int s = -1; s <= 1; s += 2) {
				int x = mx + s * (hw[z] + 1);
				if (!in(b, x, z)) {
					continue;
				}
				b.set(x, 0, z, Blocks.COBBLESTONE.defaultBlockState());
				for (int y = 1; y < ye; y++) {
					b.set(x, y, z, Bp.log(postLog, Direction.Axis.Y));
				}
			}
		}
		// The roof of dark shingles, slice by slice: wider (and so higher) in the middle like an upturned boat.
		for (int z = Math.max(0, z0 - 1); z <= Math.min(D - 1, z1 + 1); z++) {
			int hr = hw[z] + 1;
			for (int i = 0; i <= hr; i++) {
				int y = ye + i;
				int xl = mx - hr + i, xr = mx + hr - i;
				if (xl == xr) {
					put(b, xl, y, z, Bp.slab(rsl, false));
					break;
				}
				put(b, xl, y, z, Bp.stairs(rs, Direction.EAST, false));
				put(b, xr, y, z, Bp.stairs(rs, Direction.WEST, false));
				if (i > 0) {
					put(b, xl, y - 1, z, rf);
					put(b, xr, y - 1, z, rf);
				}
			}
		}
		// Gable ends: board infill, a tie beam and a king post; crossed bargeboards above the ridge.
		for (int zE : new int[]{z0, z1}) {
			int hr = hw[zE] + 1;
			for (int y = ye; y < ye + hr; y++) {
				int lim = hr - (y - ye) - 1;
				for (int x = mx - lim; x <= mx + lim; x++) {
					if (!b.has(x, y, zE)) {
						b.set(x, y, zE, boards);
					}
				}
			}
			for (int x = mx - hw[zE]; x <= mx + hw[zE]; x++) {
				b.set(x, ye, zE, Bp.log(postLog, Direction.Axis.X));
			}
			for (int y = ye + 1; y < ye + hr - 1; y++) {
				b.set(mx, y, zE, Bp.log(postLog, Direction.Axis.Y));
			}
			if (hr >= 4) {
				b.set(mx - 2, ye + 1, zE, AIR);
				b.set(mx + 2, ye + 1, zE, AIR);
			}
		}
		for (int zE : new int[]{z0 - 1, z1 + 1}) {
			if (zE < 0 || zE >= D) {
				continue;
			}
			int ridge = ye + hw[zE] + 1;
			for (int i = 1; i <= 2; i++) {
				put(b, mx + i, ridge + i, zE, Bp.stairs(rs, Direction.EAST, false));
				put(b, mx - i, ridge + i, zE, Bp.stairs(rs, Direction.WEST, false));
			}
		}
		// Doors: both long sides (with porches) and both gable ends; the +z one faces the street.
		int xl = mx - hw[zm], xr = mx + hw[zm];
		b.door(door, xl, 1, zm, Direction.WEST);
		b.door(door, xr, 1, zm, Direction.EAST);
		b.door(door, mx, 1, z0, Direction.NORTH);
		b.door(door, mx, 1, z1, Direction.SOUTH);
		for (int s = -1; s <= 1; s += 2) {
			int px = mx + s * (hw[zm] + 1);
			for (int dz = -1; dz <= 1; dz += 2) {
				for (int y = 1; y < ye; y++) {
					put(b, px, y, zm + dz, Bp.log(postLog, Direction.Axis.Y));
				}
			}
			put(b, px, 0, zm, Blocks.DIRT_PATH.defaultBlockState());
		}
		put(b, mx, 0, z1 + 1, Blocks.DIRT_PATH.defaultBlockState());
		put(b, mx, 0, z0 - 1, Blocks.DIRT_PATH.defaultBlockState());
		put(b, mx - 1, 3, z1 + 1, Bp.wallTorch(Direction.SOUTH));
		put(b, mx + 1, 3, z1 + 1, Bp.wallTorch(Direction.SOUTH));

		// Partition walls closing off a room at each end.
		int e = Math.max(3, (z1 - z0) / 5);
		int zp0 = z0 + e, zp1 = z1 - e;
		for (int zp : new int[]{zp0, zp1}) {
			int hr = hw[zp] + 1;
			for (int x = mx - hw[zp] + 1; x <= mx + hw[zp] - 1; x++) {
				if (wall[x][zp]) {
					continue;
				}
				for (int y = 1; y < ye; y++) {
					b.set(x, y, zp, boards);
				}
			}
			for (int y = ye; y < ye + hr; y++) {
				int lim = hr - (y - ye) - 1;
				for (int x = mx - lim; x <= mx + lim; x++) {
					if (!b.has(x, y, zp)) {
						b.set(x, y, zp, boards);
					}
				}
			}
			b.door(door, mx, 1, zp, zp == zp0 ? Direction.NORTH : Direction.SOUTH);
		}
		put(b, mx + 1, 3, zp0 + 1, Bp.wallTorch(Direction.SOUTH));
		put(b, mx - 1, 3, zp1 - 1, Bp.wallTorch(Direction.NORTH));
		put(b, mx + 1, 2, zp0 - 1, Bp.wallTorch(Direction.NORTH));
		put(b, mx - 1, 2, zp1 + 1, Bp.wallTorch(Direction.SOUTH));

		// ---- the hall: long hearth, raised sleeping platforms, posts and tie beams, the high seat.
		int pw = hwMid >= 5 ? 2 : 1;
		int hsz = zm + 3 <= zp1 - 2 ? zm + 3 : zm - 3;
		DyeColor[] furs = {DyeColor.BROWN, DyeColor.WHITE, DyeColor.GRAY, DyeColor.BLACK};
		for (int z = zp0 + 1; z <= zp1 - 1; z++) {
			if (z >= zp0 + 2 && z <= zp1 - 2) {
				int k = Math.floorMod(z - zm, 3);
				b.set(mx, 0, z, k == 1 ? Blocks.CAMPFIRE.defaultBlockState()
						: b.r.nextBoolean() ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.GRAVEL.defaultBlockState());
				for (int s = -1; s <= 1; s += 2) {
					if (!wall[mx + s][z]) {
						b.set(mx + s, 0, z, b.r.nextInt(4) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
					}
				}
			}
			for (int s = -1; s <= 1; s += 2) {
				for (int j = 1; j <= pw; j++) {
					int x = mx + s * (hw[z] - j);
					if (wall[x][z] || Math.abs(z - zm) <= 1 || Math.abs(x - mx) <= 1) {
						continue;
					}
					b.set(x, 1, z, b.r.nextInt(6) == 0 ? Bp.slab(Blocks.WOOL_SLAB.pick(furs[b.r.nextInt(furs.length)]), false)
							: Bp.slab(Blocks.SPRUCE_SLAB, false));
				}
			}
		}
		// Beds on the platforms.
		for (int z = zp0 + 3; z <= zp1 - 2; z += 3) {
			if (Math.abs(z - zm) <= 1 || Math.abs(z - 1 - zm) <= 1 || hw[z] != hw[z - 1]) {
				continue;
			}
			for (int s = -1; s <= 1; s += 2) {
				if (s > 0 && (Math.abs(z - hsz) <= 1 || Math.abs(z - 1 - hsz) <= 1)) {
					continue;
				}
				int x = mx + s * (hw[z] - 1);
				if (!wall[x][z] && !wall[x][z - 1] && Math.abs(x - mx) > 1) {
					b.bed(p.bed, x, 1, z, Direction.NORTH);
				}
			}
		}
		// Roof posts in two rows with tie beams across the hall, lanterns hanging from them.
		for (int z = zp0 + 2; z <= zp1 - 2; z += 4) {
			if (Math.abs(z - zm) <= 1) {
				continue;
			}
			int off = hw[z] - pw;
			if (off < 2) {
				continue;
			}
			int hr = hw[z] + 1;
			int yTopPost = ye + (hr - off) - 2;
			for (int s = -1; s <= 1; s += 2) {
				int x = mx + s * off;
				if (wall[x][z] || (s > 0 && Math.abs(z - hsz) <= 1)) {
					continue;
				}
				BlockState at1 = b.get(x, 1, z);
				if (at1 != null && at1.is(p.bed)) {
					continue;
				}
				for (int y = 1; y <= yTopPost; y++) {
					b.set(x, y, z, Bp.log(postLog, Direction.Axis.Y));
				}
			}
			for (int x = mx - off + 1; x <= mx + off - 1; x++) {
				b.set(x, ye, z, Bp.log(postLog, Direction.Axis.X));
			}
			b.set(mx - 1, ye - 1, z, Bp.lantern(true));
		}
		// The chieftain's high seat between two carved pillars, his banner behind.
		int hx = mx + hw[hsz] - 1;
		if (hsz - 1 > zp0 && hsz + 1 < zp1 && !wall[hx][hsz] && !wall[hx][hsz - 1] && !wall[hx][hsz + 1] && hx - mx > 1) {
			b.set(hx, 1, hsz, Bp.stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
			for (int dz = -1; dz <= 1; dz += 2) {
				b.set(hx, 1, hsz + dz, Bp.log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y));
				b.set(hx, 2, hsz + dz, Bp.log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y));
				b.set(hx, 3, hsz + dz, Blocks.DARK_OAK_FENCE.defaultBlockState());
			}
			b.set(hx, 3, hsz, wallBanner(col[0], Direction.WEST));
			furnish(b, hx - 1, 1, hsz, Bp.slab(Blocks.OAK_SLAB, true));
			furnish(b, hx - 1, 2, hsz, candles(2));
		}
		// Benches by the partitions, a chest and barrels of ale.
		for (int s = -1; s <= 1; s += 2) {
			furnish(b, mx + s, 1, zp0 + 1, Bp.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
			furnish(b, mx + s, 1, zp1 - 1, Bp.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
		}
		int cz0 = zp0 + 1, cz1 = zp1 - 1;
		if (!wall[mx - hw[cz0] + 1][cz0]) {
			b.set(mx - hw[cz0] + 1, 1, cz0, Bp.facing(Blocks.CHEST, Direction.EAST));
		}
		if (!wall[mx + hw[cz0] - 1][cz0]) {
			b.set(mx + hw[cz0] - 1, 1, cz0, barrel(Direction.UP));
		}
		if (!wall[mx - hw[cz1] + 1][cz1]) {
			b.set(mx - hw[cz1] + 1, 1, cz1, barrel(Direction.UP));
		}
		if (!wall[mx + hw[cz1] - 1][cz1]) {
			b.set(mx + hw[cz1] - 1, 1, cz1, Bp.facing(Blocks.CHEST, Direction.WEST));
		}

		// ---- the north end room: weaving and stores.
		int hminN = Integer.MAX_VALUE;
		for (int z = z0 + 1; z <= zp0 - 1; z++) {
			hminN = Math.min(hminN, hw[z]);
		}
		if (zp0 - 1 >= z0 + 1) {
			for (int z = z0 + 1; z <= zp0 - 1; z++) {
				keepFree(b, mx, 1, z, DyeColor.BROWN);
			}
			int ax0 = mx - hminN + 1, ax1 = mx + hminN - 1;
			furnish(b, ax0, 1, z0 + 1, Bp.facing(Blocks.LOOM, Direction.SOUTH));
			furnish(b, ax1, 1, z0 + 1, Bp.facing(Blocks.LOOM, Direction.SOUTH));
			furnish(b, ax0, 1, zp0 - 1, Bp.facing(Blocks.CHEST, Direction.EAST));
			furnish(b, ax1, 1, zp0 - 1, barrel(Direction.UP));
			Interior.room(b, c, ax0, z0 + 1, ax1, zp0 - 1, 1, Interior.Use.STORE, 1);
		}
		// ---- the south end room by the street door: the cooking pot over a fire, quern, barrels.
		int hminS = Integer.MAX_VALUE;
		for (int z = zp1 + 1; z <= z1 - 1; z++) {
			hminS = Math.min(hminS, hw[z]);
		}
		if (z1 - 1 >= zp1 + 1) {
			for (int z = zp1 + 1; z <= z1 - 1; z++) {
				keepFree(b, mx, 1, z, DyeColor.BROWN);
			}
			int ax0 = mx - hminS + 1, ax1 = mx + hminS - 1;
			int zc = (zp1 + 1 + z1 - 1) / 2;
			if (ax1 > mx) {
				b.set(ax1, 0, zc, Blocks.CAMPFIRE.defaultBlockState());
				b.set(ax1, 1, zc, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 2));
			}
			furnish(b, ax0, 1, zp1 + 1, Blocks.GRINDSTONE.defaultBlockState());
			furnish(b, ax0, 1, z1 - 1, barrel(Direction.UP));
			furnish(b, ax1, 1, z1 - 1, barrel(Direction.UP));
			Interior.room(b, c, ax0, zp1 + 1, ax1, z1 - 1, 1, Interior.Use.KITCHEN, 1);
		}
	}
}
