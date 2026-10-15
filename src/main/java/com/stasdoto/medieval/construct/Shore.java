package com.stasdoto.medieval.construct;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

import com.stasdoto.medieval.history.Culture;

/**
 * Buildings on the water's edge: water mills, norias, fishermen's huts, piers and boatyards (and the Persian windmill,
 * which is an ordinary land plot). For the shore plots the front (+z) faces the WATER: the back part of the plot (small z)
 * is dry land levelled to y = 0, the front part may lie over the river or sea, whose surface is at y = -1. Over the water
 * no AIR is ever set at y <= -1 (it would cut holes in the river); posts, hulls and wheels replace the water instead.
 * Access from the land is at the back (z = 0).
 */
final class Shore {
	private Shore() {
	}

	static final BlockState AIR = Blocks.AIR.defaultBlockState();
	/** Wall styles. */
	static final int LOG = 0, FRAME = 1, STONE = 2;

	// ================================================================ shared helpers (also used by Public)

	static boolean in(Bp b, int x, int z) {
		return x >= 0 && x < b.w && z >= 0 && z < b.d;
	}

	/** Sets a block only inside the plot. */
	static void put(Bp b, int x, int y, int z, BlockState s) {
		if (in(b, x, z)) {
			b.set(x, y, z, s);
		}
	}

	static void put(Bp b, int x, int y, int z, Block s) {
		put(b, x, y, z, s.defaultBlockState());
	}

	/** Sets a block inside the plot only where nothing has been set yet. */
	static void soft(Bp b, int x, int y, int z, BlockState s) {
		if (in(b, x, z) && !b.has(x, y, z)) {
			b.set(x, y, z, s);
		}
	}

	static void soft(Bp b, int x, int y, int z, Block s) {
		soft(b, x, y, z, s.defaultBlockState());
	}

	static int clamp(int v, int lo, int hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	/** The first row (from the back) that may lie over the water. */
	static int shoreRow(Bp b) {
		return Math.max(2, b.d / 2);
	}

	/** Leaves that never decay. */
	static BlockState leaves(Block leaf) {
		return leaf.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
	}

	static BlockState candle(int n) {
		return Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, clamp(n, 1, 4))
				.setValue(BlockStateProperties.LIT, true);
	}

	/** A burning furnace or smoker. */
	static BlockState lit(Block furnace, Direction f) {
		return Bp.facing(furnace, f).setValue(BlockStateProperties.LIT, true);
	}

	/** A grindstone standing on the floor. */
	static BlockState grindstone(Direction f) {
		return Bp.facing(Blocks.GRINDSTONE, f).setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
	}

	static BlockState barrel(Direction f) {
		return Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, f);
	}

	static BlockState waterCauldron() {
		return Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3);
	}

	static BlockState doubleSlab(Block slab) {
		return slab.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);
	}

	static BlockState hay(Direction.Axis a) {
		return Bp.log(Blocks.HAY_BLOCK, a);
	}

	static BlockState wool(DyeColor c) {
		return Blocks.WOOL.pick(c).defaultBlockState();
	}

	static BlockState carpet(DyeColor c) {
		return Blocks.CARPET.pick(c).defaultBlockState();
	}

	/** Wooden stairs matching the palette's planks (the palette's own stairs may be stone in the south). */
	static Block woodStairs(Palette p) {
		if (p.planks == Blocks.JUNGLE_PLANKS) {
			return Blocks.JUNGLE_STAIRS;
		}
		if (p.planks == Blocks.OAK_PLANKS) {
			return Blocks.OAK_STAIRS;
		}
		if (p.planks == Blocks.DARK_OAK_PLANKS) {
			return Blocks.DARK_OAK_STAIRS;
		}
		return Blocks.SPRUCE_STAIRS;
	}

	static Block woodSlab(Palette p) {
		if (p.planks == Blocks.JUNGLE_PLANKS) {
			return Blocks.JUNGLE_SLAB;
		}
		if (p.planks == Blocks.OAK_PLANKS) {
			return Blocks.OAK_SLAB;
		}
		if (p.planks == Blocks.DARK_OAK_PLANKS) {
			return Blocks.DARK_OAK_SLAB;
		}
		return Blocks.SPRUCE_SLAB;
	}

	/** A post of logs standing on the river bed (y = -4) up to {@code top}. */
	static void post(Bp b, int x, int z, Block log, int top) {
		for (int y = -4; y <= top; y++) {
			put(b, x, y, z, Bp.log(log, Direction.Axis.Y));
		}
	}

	/** How a people builds the walls of a small working building. */
	static int style(Culture c, java.util.Random r) {
		return switch (c) {
			case RUS, BALT_FINN -> LOG;
			case NORSE -> r.nextInt(3) == 0 ? LOG : FRAME;
			case WEST_SLAV -> r.nextBoolean() ? LOG : FRAME;
			case MAGYAR -> FRAME;
			case LATIN -> r.nextInt(3) == 0 ? STONE : FRAME;
			case CELTIC -> r.nextBoolean() ? STONE : FRAME;
			default -> STONE;
		};
	}

	/** Flat roofs in the dry south, tiles or thatch elsewhere. */
	static boolean flatRoof(Culture c, java.util.Random r) {
		return switch (c) {
			case ARAB, PERSIAN, STEPPE -> true;
			case ANDALUSI -> r.nextBoolean();
			default -> false;
		};
	}

	/**
	 * Walls of a box x0..x1 × z0..z1 up to height h: a log frame (сруб) with crossed corners, a timber frame with infill,
	 * or stone with weathered patches. Every style stands on a plinth at y = 0.
	 */
	static void box(Bp b, Palette p, Culture c, int st, int x0, int z0, int x1, int z1, int h) {
		BlockState infill = c == Culture.NORSE ? p.planks.defaultBlockState() : p.plaster;
		BlockState plinth = st == STONE ? p.stone2 : Blocks.COBBLESTONE.defaultBlockState();
		for (int y = 0; y <= h; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					boolean ex = x == x0 || x == x1, ez = z == z0 || z == z1;
					if (!ex && !ez) {
						continue;
					}
					boolean corner = ex && ez;
					BlockState s;
					if (st == LOG) {
						Direction.Axis ax = corner ? (y % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z) : ez ? Direction.Axis.X : Direction.Axis.Z;
						s = Bp.log(p.log, ax);
					} else if (y == 0) {
						s = plinth;
					} else if (st == FRAME) {
						boolean post = corner || (ez && (x - x0) % 3 == 0) || (ex && (z - z0) % 3 == 0);
						if (y == h) {
							s = Bp.log(p.strippedLog, ez ? Direction.Axis.X : Direction.Axis.Z);
						} else if (post) {
							s = Bp.log(p.log, Direction.Axis.Y);
						} else {
							s = infill;
						}
					} else {
						boolean band = c == Culture.BYZANTINE && y % 3 == 0;
						s = band || b.r.nextInt(7) == 0 ? p.stone2 : p.stone;
					}
					b.set(x, y, z, s);
				}
			}
		}
	}

	/** A roof over x0..x1 × z0..z1 at height y: flat with a parapet, thatch, terracotta tiles or the palette's roof. */
	static void roof(Bp b, Palette p, Culture c, boolean flat, int x0, int x1, int z0, int z1, int y, boolean alongX, int over) {
		if (flat) {
			b.fill(x0, y, z0, x1, y, z1, p.roofFull);
			for (int x = x0; x <= x1; x++) {
				b.set(x, y + 1, z0, Bp.slab(p.roofSlab, false));
				b.set(x, y + 1, z1, Bp.slab(p.roofSlab, false));
			}
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y + 1, z, Bp.slab(p.roofSlab, false));
				b.set(x1, y + 1, z, Bp.slab(p.roofSlab, false));
			}
		} else if (p.thatch) {
			Houses.thatch(b, x0, x1, z0, z1, y, alongX, p);
		} else if (c == Culture.ANDALUSI || c == Culture.ARAB || c == Culture.PERSIAN || c == Culture.STEPPE) {
			b.gableRoof(x0, x1, z0, z1, y, alongX, over, Blocks.BRICK_STAIRS, Blocks.BRICK_SLAB, Blocks.BRICKS.defaultBlockState(), p.stone);
		} else {
			BlockState gable = c == Culture.BYZANTINE || c == Culture.CAUCASUS ? p.stone : p.planks.defaultBlockState();
			b.gableRoof(x0, x1, z0, z1, y, alongX, over, p.roofStairs, p.roofSlab, p.roofFull, gable);
		}
	}

	/** A small tree: a trunk of {@code h} logs and a round crown, some of it of a second kind of leaves (blossom). */
	static void tree(Bp b, int x, int z, Block log, Block leaf, Block leaf2, int h, int r) {
		for (int y = 1; y <= h; y++) {
			put(b, x, y, z, Bp.log(log, Direction.Axis.Y));
		}
		for (int y = h; y <= h + 2; y++) {
			int rr = y == h + 2 ? Math.max(0, r - 1) : r;
			for (int dx = -rr; dx <= rr; dx++) {
				for (int dz = -rr; dz <= rr; dz++) {
					if (dx * dx + dz * dz <= rr * rr + (rr >= 2 ? 1 : 0)) {
						Block l = leaf2 != null && b.r.nextInt(3) == 0 ? leaf2 : leaf;
						soft(b, x + dx, y, z + dz, leaves(l));
					}
				}
			}
		}
	}

	/** A slim cypress: a column of dark leaves. */
	static void cypress(Bp b, int x, int z, int h) {
		put(b, x, 1, z, Bp.log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
		for (int y = 2; y <= h; y++) {
			put(b, x, y, z, leaves(Blocks.SPRUCE_LEAVES));
		}
		if (h >= 5) {
			for (int y = 3; y <= h - 2; y++) {
				soft(b, x + 1, y, z, leaves(Blocks.SPRUCE_LEAVES));
				soft(b, x - 1, y, z, leaves(Blocks.SPRUCE_LEAVES));
				soft(b, x, y, z + 1, leaves(Blocks.SPRUCE_LEAVES));
				soft(b, x, y, z - 1, leaves(Blocks.SPRUCE_LEAVES));
			}
		}
	}

	/** A cart along z: plank bed on two wheels with shafts and a load. */
	static void cart(Bp b, Palette p, int x, int z, BlockState load) {
		soft(b, x, 1, z, Bp.slab(woodSlab(p), true));
		soft(b, x, 1, z + 1, Bp.slab(woodSlab(p), true));
		soft(b, x - 1, 1, z, Bp.log(p.strippedLog, Direction.Axis.X));
		soft(b, x + 1, 1, z, Bp.log(p.strippedLog, Direction.Axis.X));
		soft(b, x, 1, z + 2, p.fence.defaultBlockState());
		if (load != null) {
			soft(b, x, 2, z, load);
		}
	}

	/** Furnishes the rest of a room (only empty cells are filled). */
	static void furnish(Bp b, Culture c, int x0, int z0, int x1, int z1, int y, Interior.Use use, int wealth) {
		if (x1 >= x0 && z1 >= z0) {
			Interior.room(b, c, x0, z0, x1, z1, y, use, wealth);
		}
	}

	/**
	 * A vertical wheel standing in the x–y plane at z = wz around (cx, cy): a rim of stripped logs, spokes and a hub
	 * (its axle runs along z). A mill wheel gets paddles outside the rim, a noria clay pots on the rim and a few paddles.
	 */
	static void wheel(Bp b, Palette p, int cx, int cy, int wz, int rad, boolean pots) {
		for (int x = cx - rad; x <= cx + rad; x++) {
			for (int y = cy - rad; y <= cy + rad; y++) {
				double dx = x - cx, dy = y - cy;
				double dd = Math.sqrt(dx * dx + dy * dy);
				if (dd > rad - 0.5 && dd <= rad + 0.5) {
					put(b, x, y, wz, Bp.log(p.strippedLog, Math.abs(dy) >= Math.abs(dx) ? Direction.Axis.X : Direction.Axis.Y));
				}
			}
		}
		for (int t = 1; t <= rad - 1; t++) {
			put(b, cx + t, cy, wz, Bp.log(p.strippedLog, Direction.Axis.X));
			put(b, cx - t, cy, wz, Bp.log(p.strippedLog, Direction.Axis.X));
			put(b, cx, cy + t, wz, Bp.log(p.strippedLog, Direction.Axis.Y));
			put(b, cx, cy - t, wz, Bp.log(p.strippedLog, Direction.Axis.Y));
		}
		for (int k = 1; k * 1.414 < rad - 0.6; k++) {
			put(b, cx + k, cy + k, wz, p.fence.defaultBlockState());
			put(b, cx - k, cy + k, wz, p.fence.defaultBlockState());
			put(b, cx + k, cy - k, wz, p.fence.defaultBlockState());
			put(b, cx - k, cy - k, wz, p.fence.defaultBlockState());
		}
		put(b, cx, cy, wz, Bp.log(p.log, Direction.Axis.Z));
		int n = 12;
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n;
			if (pots) {
				int x = cx + (int) Math.round(rad * Math.cos(a)), y = cy + (int) Math.round(rad * Math.sin(a));
				put(b, x, y, wz, Blocks.DECORATED_POT.defaultBlockState());
				if (i % 3 == 1) {
					double a2 = a + Math.PI / n;
					put(b, cx + (int) Math.round((rad + 1) * Math.cos(a2)), cy + (int) Math.round((rad + 1) * Math.sin(a2)), wz,
							p.planks.defaultBlockState());
				}
			} else {
				double a2 = a + Math.PI / n;
				put(b, cx + (int) Math.round((rad + 1) * Math.cos(a2)), cy + (int) Math.round((rad + 1) * Math.sin(a2)), wz,
						p.planks.defaultBlockState());
			}
		}
	}

	/**
	 * A boat lying in the water along z from z0 (stern) to z1 (bow), keel at x = hx: bottom planks at y = -1 (they
	 * replace the water), curved sides at y = 0, raised stem and stern, thwarts and oars; with a mast and a square
	 * (northern) or lateen (southern) sail when {@code mast}.
	 */
	static void boat(Bp b, Palette p, Culture c, int hx, int z0, int z1, boolean mast) {
		if (z1 - z0 < 2) {
			return;
		}
		Block st = woodStairs(p);
		BlockState pl = p.planks.defaultBlockState();
		for (int z = z0; z <= z1; z++) {
			put(b, hx, -1, z, Bp.log(p.strippedLog, Direction.Axis.Z));
			if (z == z0 || z == z1) {
				put(b, hx, 0, z, pl);
			} else {
				put(b, hx - 1, -1, z, pl);
				put(b, hx + 1, -1, z, pl);
				put(b, hx - 1, 0, z, Bp.stairs(st, Direction.EAST, true));
				put(b, hx + 1, 0, z, Bp.stairs(st, Direction.WEST, true));
			}
		}
		put(b, hx, 1, z1, Bp.stairs(st, Direction.SOUTH, false));
		put(b, hx, 1, z0, Bp.stairs(st, Direction.NORTH, false));
		if (c == Culture.NORSE) {
			// The tall curved stem of a Norse ship.
			put(b, hx, 1, z1, pl);
			put(b, hx, 2, z1, Bp.stairs(st, Direction.SOUTH, false));
			put(b, hx, 1, z0, pl);
			put(b, hx, 2, z0, Bp.stairs(st, Direction.NORTH, false));
		}
		for (int z = z0 + 2; z < z1 - 1; z += 2) {
			put(b, hx, 0, z, Bp.slab(woodSlab(p), false));
			for (int s = -1; s <= 1; s += 2) {
				int ox = hx + 2 * s;
				if (in(b, ox, z) && !b.has(ox, 0, z)) {
					b.set(ox, 0, z, p.fence.defaultBlockState());
				}
			}
		}
		soft(b, hx, 0, z0 + 1, barrel(Direction.UP));
		if (!mast) {
			return;
		}
		int zm = (z0 + z1) / 2;
		int mh = 6 + (z1 - z0) / 3;
		put(b, hx, 0, zm, pl);
		for (int y = 1; y <= mh; y++) {
			put(b, hx, y, zm, Bp.log(p.strippedLog, Direction.Axis.Y));
		}
		int sh = Math.min(2, Math.min(hx, b.w - 1 - hx));
		int zs = zm + 1;
		boolean lateen = c == Culture.ANDALUSI || c == Culture.ARAB || c == Culture.PERSIAN || c == Culture.BYZANTINE;
		boolean stripes = c == Culture.NORSE || (c != Culture.RUS && !lateen && b.r.nextBoolean());
		DyeColor stripe = c == Culture.NORSE && b.r.nextBoolean() ? DyeColor.BLUE : DyeColor.RED;
		for (int x = hx - sh; x <= hx + sh; x++) {
			put(b, x, mh - 1, zs, Bp.log(p.strippedLog, Direction.Axis.X));
		}
		for (int i = 0; i < 3; i++) {
			int y = mh - 2 - i;
			int xr = lateen ? hx - sh + (2 * sh * (i + 1)) / 3 : hx + sh;
			for (int x = hx - sh; x <= xr; x++) {
				boolean dark = stripes && Math.floorMod(x - hx, 2) == 1;
				put(b, x, y, zs, wool(dark ? stripe : DyeColor.WHITE));
			}
		}
	}

	// ================================================================ 1. water mill

	/**
	 * A water mill: the mill house on the bank (logs, a timber frame or stone with a flat or tiled roof), an undershot
	 * wheel in the river in front of it, its axle running into the house to the millstones; grain sacks, flour and the
	 * miller's bed inside.
	 */
	static void watermill(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d, sz = shoreRow(b);
		int st = style(c, b.r);
		boolean flat = st == STONE && flatRoof(c, b.r);
		int hx0 = w >= 10 ? 1 : 0, hx1 = w - 1 - hx0;
		int hz0 = d >= 12 ? 1 : 0;
		int hz1 = Math.min(sz, d - 4);
		if (hz1 - hz0 < 3) {
			hz1 = Math.min(d - 1, hz0 + 3);
		}
		if (hx1 - hx0 < 4 || hz1 - hz0 < 3) {
			return;
		}
		int cx = w / 2;
		int rad = clamp(Math.min(cx, w - 1 - cx) - 1, 2, 4);
		int cy = Math.max(1, rad - 2);
		int wz = Math.min(d - 1, hz1 + 2);
		int wh = 5;
		int ix0 = hx0 + 1, ix1 = hx1 - 1, iz0 = hz0 + 1, iz1 = hz1 - 1;
		int mz = (hz0 + hz1) / 2;

		// The house, its foundations reaching down into the river bed at the front.
		b.fill(hx0, -3, hz0, hx1, -1, hz1, st == STONE ? p.stone2 : Blocks.COBBLESTONE.defaultBlockState());
		b.fill(ix0, 0, iz0, ix1, 0, iz1, st == STONE ? p.floor : p.planks.defaultBlockState());
		box(b, p, c, st, hx0, hz0, hx1, hz1, wh);
		if (!flat) {
			// A tie beam to hang the lamp from.
			for (int x = hx0; x <= hx1; x++) {
				b.set(x, wh, mz, Bp.log(p.strippedLog, Direction.Axis.X));
			}
		}
		roof(b, p, c, flat, hx0, hx1, hz0, hz1, wh + 1, true, hx0 >= 1 && hz0 >= 1 ? 1 : 0);

		// Windows: on the sides and over the wheel.
		for (int x : new int[]{hx0, hx1}) {
			b.air(x, 2, mz);
			b.air(x, 3, mz);
			int ox = x == hx0 ? x - 1 : x + 1;
			if (in(b, ox, mz) && st != STONE) {
				b.set(ox, 2, mz, Bp.trapdoor(p.trapdoor, x == hx0 ? Direction.WEST : Direction.EAST, false, true));
			}
		}
		if (cx - 2 > hx0) {
			b.set(cx - 2, 3, hz1, st == STONE ? Blocks.IRON_BARS.defaultBlockState() : AIR);
		}

		// Door at the back, to the land, with a path.
		boolean sideDoor = hz0 == 0 && hx0 >= 1 && b.r.nextBoolean();
		if (sideDoor) {
			b.door(p.door, hx0, 1, iz0, Direction.WEST);
			b.air(ix0, 1, iz0);
			b.air(ix0, 2, iz0);
		} else {
			b.door(p.door, ix0, 1, hz0, Direction.NORTH);
			b.air(ix0, 1, iz0);
			b.air(ix0, 2, iz0);
			for (int z = 0; z < hz0; z++) {
				b.set(ix0, 0, z, Blocks.DIRT_PATH.defaultBlockState());
			}
		}

		// The wheel, its axle into the house, bearings and a plank walk along the house front.
		if (wz >= hz1 + 2) {
			for (int z = iz1; z <= Math.min(d - 1, wz + 1); z++) {
				put(b, cx, cy, z, Bp.log(p.log, Direction.Axis.Z));
			}
			for (int x = hx0; x <= hx1; x++) {
				if (x != cx) {
					b.set(x, 0, hz1 + 1, Bp.slab(woodSlab(p), true));
				}
			}
			b.set(hx0, 1, hz1 + 1, p.fence.defaultBlockState());
			b.set(hx1, 1, hz1 + 1, p.fence.defaultBlockState());
			for (int y = -4; y < cy; y++) {
				put(b, cx, y, hz1 + 1, Bp.log(p.log, Direction.Axis.Y));
				if (wz + 1 <= d - 1) {
					put(b, cx, y, wz + 1, Bp.log(p.log, Direction.Axis.Y));
				}
			}
			wheel(b, p, cx, cy, wz, rad, false);
		}

		// Inside: the millstones at the end of the axle, a grindstone, sacks of grain and of flour, the miller's bed.
		int ms = Math.max(iz0, iz1 - 1);
		for (int y = 1; y < cy; y++) {
			b.set(cx, y, ms, p.planks.defaultBlockState());
		}
		b.set(cx, cy, ms, doubleSlab(Blocks.SMOOTH_STONE_SLAB));
		b.set(cx, cy + 1, ms, Blocks.COMPOSTER.defaultBlockState());
		if (cx - 1 >= ix0 && !b.has(cx - 1, 1, ms)) {
			b.set(cx - 1, 1, ms, grindstone(Direction.EAST));
		}
		if (cx + 1 <= ix1) {
			soft(b, cx + 1, 1, ms, wool(DyeColor.WHITE));
			soft(b, cx + 1, 2, ms, Blocks.WOOL_SLAB.pick(DyeColor.WHITE).defaultBlockState());
		}
		soft(b, ix0, 1, iz1, hay(Direction.Axis.Y));
		soft(b, ix0, 2, iz1, hay(Direction.Axis.X));
		if (ix0 + 1 < cx) {
			soft(b, ix0 + 1, 1, iz1, hay(Direction.Axis.Z));
		}
		if (ix1 > cx + 1 && !b.has(ix1, 1, iz1) && !b.has(ix1, 1, iz1 - 1)) {
			b.bed(p.bed, ix1, 1, iz1, Direction.NORTH);
		}
		soft(b, ix1, 1, iz0, Blocks.CHEST.defaultBlockState());
		soft(b, ix1 - 1, 1, iz0, barrel(Direction.UP));
		soft(b, cx, flat ? wh : wh - 1, mz, Bp.lantern(true));
		furnish(b, c, ix0, iz0, ix1, iz1, 1, Interior.Use.WORKSHOP, 1);

		// The land behind: carts and sacks waiting for the miller, or firewood.
		int yard = b.r.nextInt(3);
		if (hz0 >= 1) {
			if (yard == 0) {
				soft(b, ix0 + 2, 1, 0, barrel(Direction.UP));
				soft(b, ix0 + 3, 1, 0, barrel(Direction.EAST));
				soft(b, ix0 + 3, 2, 0, hay(Direction.Axis.X));
			} else if (yard == 1) {
				for (int x = ix0 + 2; x <= Math.min(ix1, ix0 + 4); x++) {
					soft(b, x, 1, 0, hay(Direction.Axis.X));
				}
				soft(b, ix0 + 3, 2, 0, hay(Direction.Axis.X));
			} else {
				for (int x = ix0 + 2; x <= Math.min(ix1, ix0 + 5); x++) {
					soft(b, x, 1, 0, Bp.log(p.log, Direction.Axis.Z));
				}
			}
		}
		if (hx0 >= 1) {
			soft(b, 0, 1, hz0 + 1, barrel(Direction.UP));
			soft(b, w - 1, 1, hz0 + 1, hay(Direction.Axis.Y));
		}
	}

	// ================================================================ 2. noria

	/**
	 * An Islamic noria (like the Albolafia at Córdoba): a tall wheel half in the river with clay pots on its rim, turning
	 * on a stone pier; the pots empty into an aqueduct channel that runs back over arches to a head on the land, from
	 * which a rill fills a garden basin.
	 */
	static void noria(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d, sz = shoreRow(b);
		int cx = (w - 1) / 2;
		int rad = clamp(Math.min(cx, w - 1 - cx), 2, 5);
		int cy = rad - 2;
		int wz = Math.min(d - 1, Math.max(sz, Math.min(d - 2, sz + 1)));
		if (wz < 2) {
			return;
		}
		int ta = cy + rad;
		int zEnd = wz - 1;
		BlockState stone = p.stone, stone2 = p.stone2;
		boolean andalusi = c == Culture.ANDALUSI;
		Block archStairs = p.stairs == Blocks.SMOOTH_SANDSTONE_STAIRS ? p.stairs
				: c == Culture.BYZANTINE ? Blocks.BRICK_STAIRS : Blocks.STONE_BRICK_STAIRS;

		// The land: paved around the head of the aqueduct.
		int pave = b.r.nextInt(3);
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < Math.min(sz, d); z++) {
				if (Math.abs(x - cx) <= 1) {
					continue;
				}
				BlockState s = pave == 0 ? p.floor : pave == 1 ? Blocks.PACKED_MUD.defaultBlockState()
						: (b.r.nextInt(3) == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState());
				b.set(x, 0, z, s);
			}
		}

		// The aqueduct: piers every three blocks, arches between, the channel with water on top.
		for (int z = 0; z <= zEnd; z++) {
			boolean pier = isPier(z, zEnd);
			for (int x = cx - 1; x <= cx + 1; x++) {
				BlockState deck = andalusi ? ((z + x) % 2 == 0 ? Blocks.BRICKS.defaultBlockState() : Blocks.CALCITE.defaultBlockState()) : stone2;
				if (pier) {
					for (int y = -4; y <= ta - 2; y++) {
						put(b, x, y, z, stone);
					}
				} else if (ta - 2 >= 0) {
					if (z > 0 && isPier(z - 1, zEnd)) {
						put(b, x, ta - 2, z, Bp.stairs(archStairs, Direction.NORTH, true));
					} else if (isPier(z + 1, zEnd)) {
						put(b, x, ta - 2, z, Bp.stairs(archStairs, Direction.SOUTH, true));
					}
				}
				put(b, x, ta - 1, z, deck);
			}
			if (z == 0) {
				// The head: a raised cistern block.
				for (int x = cx - 1; x <= cx + 1; x++) {
					put(b, x, ta, z, stone);
					put(b, x, ta + 1, z, Bp.slab(p.roofSlab, false));
				}
				if (c == Culture.PERSIAN) {
					put(b, cx, ta, z, Blocks.GLAZED_TERRACOTTA.pick(DyeColor.LIGHT_BLUE).defaultBlockState());
				}
			} else {
				put(b, cx - 1, ta, z, stone);
				put(b, cx + 1, ta, z, stone);
				put(b, cx, ta, z, Blocks.WATER.defaultBlockState());
			}
		}

		// The wheel on its pier: the near bearing is the last aqueduct pier, the far one a stone post in the river.
		put(b, cx, cy, zEnd, Bp.log(p.log, Direction.Axis.Z));
		if (wz + 1 <= d - 1) {
			for (int y = -4; y < cy; y++) {
				put(b, cx, y, wz + 1, stone);
			}
			put(b, cx, cy, wz + 1, Bp.log(p.log, Direction.Axis.Z));
			put(b, cx, cy + 1, wz + 1, Bp.slab(p.roofSlab, false));
		}
		wheel(b, p, cx, cy, wz, rad, true);

		// A basin on the land fed by a rill from the head; or a trough when there is no room.
		int bx0 = cx + 3, bx1 = w - 2, bz0 = 1, bz1 = Math.min(sz - 2, 4);
		if (bx1 - bx0 >= 1 && bz1 - bz0 >= 1) {
			for (int x = bx0 - 1; x <= bx1 + 1; x++) {
				for (int z = bz0 - 1; z <= bz1 + 1; z++) {
					boolean rim = x == bx0 - 1 || x == bx1 + 1 || z == bz0 - 1 || z == bz1 + 1;
					b.set(x, -1, z, stone2);
					b.set(x, 0, z, rim ? stone2 : Blocks.WATER.defaultBlockState());
					if (rim && b.r.nextInt(4) == 0) {
						b.set(x, 1, z, b.r.nextBoolean() ? Blocks.POTTED_RED_TULIP.defaultBlockState() : Blocks.DECORATED_POT.defaultBlockState());
					}
				}
			}
			for (int x = cx + 2; x <= bx0 - 1; x++) {
				b.set(x, -1, bz0, stone2);
				b.set(x, 0, bz0, Blocks.WATER.defaultBlockState());
			}
		} else if (in(b, cx + 2, 1)) {
			b.set(cx + 2, 1, 1, waterCauldron());
		}

		// The other side: an orange tree or a bench in the shade, a lamp post by the head.
		if (cx - 2 >= 1) {
			if (b.r.nextBoolean() && sz >= 4) {
				tree(b, 1, 1, Blocks.JUNGLE_LOG, Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES, 2, 1);
			} else {
				soft(b, 1, 1, 1, Bp.stairs(p.stairs, Direction.WEST, false));
				soft(b, 1, 1, 2, Bp.stairs(p.stairs, Direction.WEST, false));
				soft(b, 0, 1, 0, Blocks.DECORATED_POT.defaultBlockState());
			}
		}
		if (cx - 2 >= 0 && sz - 1 > 0) {
			soft(b, cx - 2, 1, sz - 1, p.fence.defaultBlockState());
			soft(b, cx - 2, 2, sz - 1, Bp.lantern(false));
		}
	}

	private static boolean isPier(int z, int zEnd) {
		return z == 0 || z == zEnd || (zEnd - z) % 3 == 0;
	}

	// ================================================================ 3. Persian windmill

	/**
	 * A Persian horizontal windmill of the Sistan kind (an ordinary land plot facing the street): thick mud-brick walls,
	 * an upper wind chamber open to the wind through vertical slots, a vertical axle with sails of matting turning in
	 * it, and the millstone room below. Wide plots get two mills side by side.
	 */
	static void windmill(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d;
		int m = w >= 8 && d >= 8 ? 1 : 0;
		int x0 = m, x1 = w - 1 - m, z0 = m, z1 = d - 1 - m;
		if (x1 - x0 < 4 || z1 - z0 < 4) {
			return;
		}
		int h = 8 + b.r.nextInt(2);
		if (x1 - x0 + 1 >= 13) {
			int xm = (x0 + x1) / 2;
			millUnit(b, p, c, x0, z0, xm, z1, h);
			millUnit(b, p, c, xm, z0, x1, z1, h + (b.r.nextBoolean() ? 1 : 0));
		} else {
			millUnit(b, p, c, x0, z0, x1, z1, h);
		}
		// Buttresses against the thrust of the wind, stepped at the top.
		if (m == 1) {
			for (int z : new int[]{z0, z1}) {
				for (int y = 1; y <= h - 2; y++) {
					b.set(0, y, z, p.stone);
					b.set(w - 1, y, z, p.stone);
				}
				b.set(0, h - 1, z, Bp.stairs(p.stairs, Direction.EAST, false));
				b.set(w - 1, h - 1, z, Bp.stairs(p.stairs, Direction.WEST, false));
			}
			// The front: trodden earth, a trough for the donkeys, sacks waiting.
			for (int x = 0; x < w; x++) {
				b.set(x, 0, d - 1, b.r.nextInt(3) == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState());
			}
			soft(b, 1, 1, d - 1, waterCauldron());
			soft(b, w - 2, 1, d - 1, hay(Direction.Axis.X));
			if (b.r.nextBoolean()) {
				soft(b, w - 3, 1, d - 1, hay(Direction.Axis.Y));
			}
		}
	}

	private static void millUnit(Bp b, Palette p, Culture c, int x0, int z0, int x1, int z1, int h) {
		BlockState wall = p.stone, band = p.stone2;
		b.fill(x0, -2, z0, x1, -1, z1, band);
		b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, Blocks.PACKED_MUD.defaultBlockState());
		for (int y = 0; y <= h; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					if (x == x0 || x == x1 || z == z0 || z == z1) {
						b.set(x, y, z, y == 0 || y == h || y == 4 ? band : wall);
					}
				}
			}
		}
		int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
		// Floor of the wind chamber, ceiling of the mill room.
		b.fill(x0 + 1, 4, z0 + 1, x1 - 1, 4, z1 - 1, p.planks.defaultBlockState());
		// The wind comes in through slots in the front wall and leaves by one at the back.
		for (int x = x0 + 1; x < x1; x++) {
			if ((x - x0) % 2 == 1) {
				for (int y = 6; y <= h - 1; y++) {
					b.air(x, y, z1);
				}
			}
		}
		for (int y = 6; y <= h - 1; y++) {
			b.air(x1 - 1, y, z0);
		}
		// Crenels on the side walls.
		for (int z = z0; z <= z1; z += 2) {
			b.set(x0, h + 1, z, band);
			b.set(x1, h + 1, z, band);
		}
		// The millstones and the vertical axle up to a beam across the top.
		b.set(cx, 1, cz, Blocks.SMOOTH_STONE.defaultBlockState());
		b.set(cx, 2, cz, doubleSlab(Blocks.SMOOTH_STONE_SLAB));
		for (int y = 3; y < h; y++) {
			b.set(cx, y, cz, Bp.log(p.log, Direction.Axis.Y));
		}
		for (int x = x0; x <= x1; x++) {
			b.set(x, h, cz, Bp.log(p.strippedLog, Direction.Axis.X));
		}
		// Sails of reed matting on wooden arms.
		int len = Math.min(2, Math.min(Math.min(cx - x0 - 1, x1 - cx - 1), Math.min(cz - z0 - 1, z1 - cz - 1)));
		if (len >= 1) {
			int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
			for (int y = 5; y <= h - 1; y++) {
				for (int[] dv : dirs) {
					for (int k = 1; k <= len; k++) {
						BlockState s = k < len ? p.fence.defaultBlockState()
								: y == 5 || y == h - 1 ? p.planks.defaultBlockState() : wool(DyeColor.WHITE);
						b.set(cx + dv[0] * k, y, cz + dv[1] * k, s);
					}
				}
			}
		}
		// Ladder up through the ceiling.
		for (int y = 1; y <= 4; y++) {
			b.set(x0 + 1, y, z0 + 1, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}
		// Door to the street.
		b.door(p.door, cx, 1, z1, Direction.SOUTH);
		b.set(cx, 3, z1, Bp.stairs(p.stairs, Direction.SOUTH, true));
		b.air(cx, 1, z1 - 1);
		b.air(cx, 2, z1 - 1);
		b.air(x0, 2, cz);
		// Grain, flour, a bin, light.
		soft(b, x1 - 1, 1, z0 + 1, hay(Direction.Axis.Y));
		soft(b, x1 - 1, 2, z0 + 1, hay(Direction.Axis.X));
		soft(b, x1 - 2, 1, z0 + 1, hay(Direction.Axis.Z));
		soft(b, x0 + 1, 1, z1 - 1, wool(DyeColor.WHITE));
		soft(b, x0 + 1, 2, z1 - 1, Blocks.WOOL_SLAB.pick(DyeColor.WHITE).defaultBlockState());
		soft(b, cx + 1, 1, cz, Blocks.COMPOSTER.defaultBlockState());
		soft(b, cx - 1, 3, cz + 1, Bp.lantern(true));
		furnish(b, c, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, Interior.Use.STORE, 1);
	}

	// ================================================================ 4. fishery

	/**
	 * A fisherman's place on the shore: a small hut, drying racks with fish, a net hung on a frame, barrels, a boat
	 * upturned on the beach for tarring, and a landing stage into the water with the working boat moored to it.
	 */
	static void fishery(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d, sz = shoreRow(b);
		int st = style(c, b.r);
		boolean flat = st == STONE && flatRoof(c, b.r);
		int hx0 = w >= 10 ? 1 : 0;
		int hx1 = Math.min(w - 2, hx0 + (w >= 11 ? 5 : 4));
		int hz0 = d >= 12 ? 1 : 0;
		int hz1 = Math.min(sz - 2, hz0 + 4);
		if (hz1 - hz0 < 3) {
			hz1 = Math.min(sz - 1, hz0 + 3);
		}
		if (hz1 - hz0 < 2 || hx1 - hx0 < 3) {
			return;
		}

		// The beach: sand at the water, then gravel and trodden earth.
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < Math.min(sz, d); z++) {
				int t = sz - 1 - z;
				if (t == 0 || (t == 1 && b.r.nextInt(3) > 0)) {
					b.set(x, -1, z, Blocks.SANDSTONE.defaultBlockState());
					b.set(x, 0, z, Blocks.SAND.defaultBlockState());
				} else if (t <= 2 && b.r.nextInt(3) == 0) {
					b.set(x, -1, z, Blocks.COBBLESTONE.defaultBlockState());
					b.set(x, 0, z, Blocks.GRAVEL.defaultBlockState());
				} else if (b.r.nextInt(3) == 0) {
					b.set(x, 0, z, Blocks.COARSE_DIRT.defaultBlockState());
				}
			}
		}

		// The hut, its door to the yard.
		int h = 3;
		b.fill(hx0, -2, hz0, hx1, -1, hz1, Blocks.COBBLESTONE.defaultBlockState());
		b.fill(hx0 + 1, 0, hz0 + 1, hx1 - 1, 0, hz1 - 1, st == STONE ? p.floor : p.planks.defaultBlockState());
		box(b, p, c, st, hx0, hz0, hx1, hz1, h);
		roof(b, p, c, flat, hx0, hx1, hz0, hz1, h + 1, true, hx0 >= 1 && hz0 >= 1 ? 1 : 0);
		int ix0 = hx0 + 1, ix1 = hx1 - 1, iz0 = hz0 + 1, iz1 = hz1 - 1;
		int dz = (hz0 + hz1 + 1) / 2;
		b.door(p.door, hx1, 1, dz, Direction.EAST);
		b.air(ix1, 1, dz);
		b.air(ix1, 2, dz);
		put(b, hx1 + 1, 0, dz, Blocks.DIRT_PATH.defaultBlockState());
		int wx = (hx0 + hx1) / 2;
		b.air(wx, 2, hz1);
		b.set(wx, 2, hz1 + 1, Bp.trapdoor(p.trapdoor, Direction.SOUTH, false, true));
		// Inside: a bed, a smoker for the fish, a lamp.
		if (iz1 > iz0) {
			if (!b.has(ix0, 1, iz1) && !b.has(ix0, 1, iz1 - 1)) {
				b.bed(p.bed, ix0, 1, iz1, Direction.NORTH);
			}
		} else if (ix1 > ix0 + 1 && !b.has(ix0, 1, iz0) && !b.has(ix0 + 1, 1, iz0)) {
			b.bed(p.bed, ix0 + 1, 1, iz0, Direction.WEST);
		}
		soft(b, ix1, 1, iz0, lit(Blocks.SMOKER, Direction.WEST));
		if (flat) {
			soft(b, wx, h, (iz0 + iz1) / 2, Bp.lantern(true));
		} else {
			soft(b, ix0, 2, iz0, Bp.wallTorch(Direction.EAST));
		}
		furnish(b, c, ix0, iz0, ix1, iz1, 1, Interior.Use.LIVING, 0);

		// The yard beside the hut: barrels at the door, a drying rack, a boat upturned for tarring.
		int yard = w - 1 - hx1;
		if (dz + 1 < sz) {
			soft(b, hx1 + 1, 1, dz + 1, barrel(Direction.UP));
		}
		if (dz - 1 >= 0) {
			soft(b, hx1 + 1, 1, dz - 1, b.r.nextBoolean() ? Blocks.CHEST.defaultBlockState() : barrel(Direction.NORTH));
		}
		int rx = w - 1 - (yard >= 5 ? 1 : 0);
		boolean sideRack = rx >= hx1 + 2;
		int rz0 = d >= 12 ? 1 : 0, rz1 = Math.min(sz - 1, rz0 + 4);
		if (sideRack && rz1 - rz0 >= 2) {
			rack(b, p, rx, rz0, rx, rz1);
			if (yard >= 6 && b.r.nextBoolean()) {
				rack(b, p, rx - 2, rz0, rx - 2, rz1);
				rx -= 2;
			}
		}
		int ub = hx1 + 2;
		if (ub + 1 < rx && ub + 1 <= w - 1) {
			int bz0 = rz0, bz1 = Math.min(sz - 3, bz0 + 4);
			if (bz1 - bz0 >= 2) {
				Block s = woodStairs(p);
				// Keel up: each side a stair rising towards the middle.
				for (int z = bz0; z <= bz1; z++) {
					b.set(ub, 1, z, Bp.stairs(s, Direction.EAST, false));
					b.set(ub + 1, 1, z, Bp.stairs(s, Direction.WEST, false));
				}
				soft(b, ub, 1, bz1 + 1, Bp.stairs(s, Direction.NORTH, false));
				soft(b, ub + 1, 1, bz1 + 1, Bp.stairs(s, Direction.NORTH, false));
				if (bz0 >= 1) {
					soft(b, ub, 1, bz0 - 1, Bp.stairs(s, Direction.SOUTH, false));
					soft(b, ub + 1, 1, bz0 - 1, Bp.stairs(s, Direction.SOUTH, false));
				}
			}
		}

		// The landing stage into the water on log posts.
		int sx = Math.min(w - 2, w / 2 + 1);
		int se = Math.min(d - 1, sz + Math.max(3, (d - sz) * 2 / 3));
		for (int z = sz - 1; z <= se; z++) {
			b.set(sx, 0, z, p.planks.defaultBlockState());
			b.set(sx + 1, 0, z, b.r.nextInt(5) == 0 ? Bp.slab(woodSlab(p), true) : p.planks.defaultBlockState());
			if (z > sz - 1 && (z - sz) % 2 == 1) {
				post(b, sx, z, p.log, -1);
				post(b, sx + 1, z, p.log, -1);
			}
		}
		post(b, sx, se, p.log, -1);
		post(b, sx + 1, se, p.log, -1);
		b.set(sx + 1, 1, se, p.fence.defaultBlockState());
		b.set(sx, 1, se, p.fence.defaultBlockState());
		b.set(sx, 2, se, Bp.lantern(false));
		soft(b, sx + 1, 1, sz, barrel(Direction.UP));
		if (b.r.nextBoolean()) {
			soft(b, sx, 1, sz + 1, Blocks.COMPOSTER.defaultBlockState());
		}

		// A net hung on a frame on the beach in front of the hut, or a second rack where there is no side yard.
		if (hz1 < sz - 1) {
			int nz = sz - 1;
			int nx = hx0;
			if (!sideRack && nx + 3 < sx) {
				rack(b, p, nx, nz, nx + 3, nz);
			} else if (nx + 2 < sx) {
				for (int y = 1; y <= 2; y++) {
					b.set(nx, y, nz, p.fence.defaultBlockState());
					b.set(nx + 2, y, nz, p.fence.defaultBlockState());
				}
				b.set(nx + 1, 2, nz, p.fence.defaultBlockState());
				b.set(nx + 1, 1, nz, Blocks.COBWEB.defaultBlockState());
			}
		}

		// The working boat moored beside the stage.
		int bz0 = sz + 1, bz1 = Math.min(d - 1, bz0 + 3 + (d >= 14 ? 1 : 0));
		if (bz1 - bz0 >= 3) {
			if (sx + 4 <= w - 1) {
				boat(b, p, c, sx + 3, bz0, bz1, false);
			} else if (sx - 3 >= 0) {
				boat(b, p, c, sx - 2, bz0, bz1, false);
			}
		}
	}

	/** A drying rack between two posts with fish hung below the bar and laid on it. */
	private static void rack(Bp b, Palette p, int xa, int za, int xb, int zb) {
		int n = Math.max(Math.abs(xb - xa), Math.abs(zb - za));
		int sx = Integer.signum(xb - xa), sz = Integer.signum(zb - za);
		for (int i = 0; i <= n; i++) {
			int x = xa + sx * i, z = za + sz * i;
			boolean end = i == 0 || i == n;
			if (end) {
				put(b, x, 1, z, p.fence.defaultBlockState());
			} else if (i % 2 == 1) {
				put(b, x, 1, z, Blocks.DRIED_KELP_BLOCK.defaultBlockState());
			}
			put(b, x, 2, z, p.fence.defaultBlockState());
			if (!end && i % 2 == 0) {
				put(b, x, 3, z, carpet(DyeColor.BROWN));
			}
		}
	}

	// ================================================================ 5. pier

	/**
	 * A wooden pier: a plank deck from the land out over the water on log posts, bollards, cargo, a lamp post at the end
	 * (sometimes a T-head), and a ship moored alongside: a Rus ладья or a Norse knarr with a square sail, a lateen-rigged
	 * boat in the south.
	 */
	static void pier(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d, sz = shoreRow(b);
		int dw = w >= 9 ? 4 : w >= 7 ? 3 : 2;
		int px0 = 0, px1 = dw - 1;
		boolean tHead = w >= 7 && d >= 13 && b.r.nextInt(3) == 0;
		boolean boards = b.r.nextBoolean();
		for (int z = 0; z < d; z++) {
			for (int x = px0; x <= px1; x++) {
				BlockState s;
				if (x == px0 || x == px1) {
					s = Bp.log(p.strippedLog, Direction.Axis.Z);
				} else if (boards) {
					s = Bp.log(p.strippedLog, Direction.Axis.X);
				} else {
					s = b.r.nextInt(6) == 0 ? Bp.slab(woodSlab(p), true) : p.planks.defaultBlockState();
				}
				b.set(x, 0, z, s);
			}
		}
		if (tHead) {
			for (int z = d - 2; z < d; z++) {
				for (int x = px1 + 1; x < w; x++) {
					b.set(x, 0, z, x == w - 1 ? Bp.log(p.strippedLog, Direction.Axis.Z) : p.planks.defaultBlockState());
				}
				post(b, w - 1, z, p.log, -1);
				post(b, (px1 + w) / 2, z, p.log, -1);
			}
			b.set(w - 1, 1, d - 1, p.fence.defaultBlockState());
		}
		// Posts and bollards.
		for (int z = sz - 1; z < d; z++) {
			if ((z - (sz - 1)) % 3 == 0 || z == d - 1) {
				post(b, px0, z, p.log, -1);
				post(b, px1, z, p.log, -1);
				if (z >= sz) {
					b.set(px1, 1, z, p.fence.defaultBlockState());
				}
			}
		}
		// The lamp post at the end.
		b.set(px0, 1, d - 1, p.fence.defaultBlockState());
		b.set(px0, 2, d - 1, p.fence.defaultBlockState());
		b.set(px0, 3, d - 1, Bp.lantern(false));

		// Cargo along the land end of the deck.
		boolean south = c == Culture.ANDALUSI || c == Culture.ARAB || c == Culture.PERSIAN || c == Culture.BYZANTINE;
		for (int z = 1; z <= sz - 2; z++) {
			int k = b.r.nextInt(6);
			BlockState s = switch (k) {
				case 0 -> Blocks.CHEST.defaultBlockState();
				case 1 -> hay(Direction.Axis.Z);
				case 2 -> south ? Blocks.DECORATED_POT.defaultBlockState() : barrel(Direction.UP);
				case 3 -> wool(b.r.nextBoolean() ? DyeColor.RED : DyeColor.WHITE);
				default -> barrel(b.r.nextBoolean() ? Direction.UP : Direction.EAST);
			};
			if (k == 5) {
				continue;
			}
			b.set(px0, 1, z, s);
			if (b.r.nextInt(3) == 0) {
				b.set(px0, 2, z, barrel(Direction.UP));
			}
		}
		if (w - 1 >= px1 + 3 && sz >= 5 && b.r.nextBoolean()) {
			cart(b, p, w - 2, sz - 4, barrel(Direction.UP));
		}
		// Goods piled on the land beside the pier.
		for (int z = 0; z <= sz - 2; z++) {
			for (int x = px1 + 2; x < w; x++) {
				int k = b.r.nextInt(7);
				if (k == 0) {
					soft(b, x, 1, z, barrel(Direction.UP));
				} else if (k == 1) {
					soft(b, x, 1, z, hay(Direction.Axis.X));
				} else if (k == 2 && south) {
					soft(b, x, 1, z, Blocks.DECORATED_POT.defaultBlockState());
				} else if (k == 2) {
					soft(b, x, 1, z, Blocks.CHEST.defaultBlockState());
				}
			}
		}

		// The ship alongside.
		int hx = px1 + 2;
		int bz0 = sz + 1, bz1 = tHead ? d - 4 : d - 2;
		if (bz1 - bz0 >= 4 && hx + 1 <= w - 1) {
			boat(b, p, c, hx, bz0, bz1, bz1 - bz0 >= 6);
		}
	}

	// ================================================================ 6. boatyard

	/**
	 * A shipyard: a ship being built on a slipway that runs down into the water: keel, stem and stern posts, ribs, half
	 * of the planking; a workshop shed, piles of logs, a log on sawhorses and a tar cauldron over a fire.
	 */
	static void boatyard(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d, sz = shoreRow(b);
		boolean shed = w >= 12 && sz >= 4;
		int xs = shed ? 4 : 0;
		int cx = (xs + w - 1) / 2;
		int hb = clamp(Math.min(cx - xs, w - 1 - cx) - 1, 1, 3);
		int kz0 = d >= 17 ? 3 : 2;
		int kz1 = Math.min(d - 3, sz + 2);
		if (kz1 - kz0 < 4) {
			kz1 = Math.min(d - 1, kz0 + 4);
		}
		if (kz1 - kz0 < 3 || w < 5) {
			return;
		}

		// Ground of the yard: trodden earth, chips, gravel.
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < Math.min(sz, d); z++) {
				int k = b.r.nextInt(5);
				if (k == 0) {
					b.set(x, 0, z, Blocks.COARSE_DIRT.defaultBlockState());
				} else if (k == 1) {
					b.set(x, -1, z, Blocks.COBBLESTONE.defaultBlockState());
					b.set(x, 0, z, Blocks.GRAVEL.defaultBlockState());
				} else if (k == 2) {
					b.set(x, 0, z, Blocks.DIRT_PATH.defaultBlockState());
				}
			}
		}

		// The slipway: two rails and sleepers running down into the water.
		for (int z = kz0 - 1; z < d; z++) {
			int ry = railY(z, sz);
			put(b, cx - 1, ry, z, Bp.log(p.strippedLog, Direction.Axis.Z));
			put(b, cx + 1, ry, z, Bp.log(p.strippedLog, Direction.Axis.Z));
			if (z % 2 == 0) {
				put(b, cx, ry, z, p.planks.defaultBlockState());
				put(b, cx - 2, ry, z, p.planks.defaultBlockState());
				put(b, cx + 2, ry, z, p.planks.defaultBlockState());
			}
		}

		// The ship: keel on its cradle, ribs, the stern half planked.
		int len = kz1 - kz0;
		int mid = kz0 + len / 2;
		for (int z = kz0; z <= kz1; z++) {
			put(b, cx, 1, z, Bp.log(p.strippedLog, Direction.Axis.Z));
			int ry = railY(z, sz);
			for (int y = ry + 1; y <= 0; y++) {
				put(b, cx, y, z, Bp.log(p.log, Direction.Axis.Y));
			}
			if (z == kz0 || z == kz1) {
				continue;
			}
			double t = (z - kz0) / (double) len;
			double f = Math.sqrt(Math.sin(Math.PI * t));
			boolean planked = z <= mid;
			boolean frame = (z - kz0) % 2 == 0;
			if (!planked && !frame) {
				continue;
			}
			for (int j = 0; j <= hb; j++) {
				int hw = Math.max(1, (int) Math.round(Math.min(hb, j + 1) * f));
				BlockState s;
				if (planked) {
					s = j == hb ? Bp.log(p.strippedLog, Direction.Axis.Z) : p.planks.defaultBlockState();
				} else {
					s = p.fence.defaultBlockState();
				}
				put(b, cx - hw, 1 + j, z, s);
				put(b, cx + hw, 1 + j, z, s);
			}
		}
		// Stem and stern posts.
		for (int y = 1; y <= hb + 2; y++) {
			put(b, cx, y, kz1, Bp.log(p.strippedLog, Direction.Axis.Y));
		}
		put(b, cx, hb + 2, kz1 + 1, Bp.stairs(woodStairs(p), Direction.SOUTH, false));
		if (c == Culture.NORSE) {
			put(b, cx, hb + 3, kz1, p.fence.defaultBlockState());
		}
		for (int y = 1; y <= hb + 1; y++) {
			put(b, cx, y, kz0, Bp.log(p.strippedLog, Direction.Axis.Y));
		}
		// Shores holding the hull upright (on the land only).
		for (int z = kz0 + 1; z < Math.min(kz1, sz); z += 3) {
			for (int s = -1; s <= 1; s += 2) {
				int x = cx + s * (hb + 1);
				if (in(b, x, z) && !b.has(x, 1, z)) {
					b.set(x, 1, z, Bp.log(p.log, Direction.Axis.Y));
					b.set(x, 2, z, p.fence.defaultBlockState());
				}
			}
		}

		// The workshop shed, open towards the ship.
		if (shed) {
			int sz1 = Math.min(5, sz - 1);
			int st = style(c, b.r);
			boolean flat = st == STONE && flatRoof(c, b.r);
			b.fill(0, -2, 0, 3, -1, sz1, Blocks.COBBLESTONE.defaultBlockState());
			b.fill(1, 0, 1, 2, 0, sz1 - 1, p.planks.defaultBlockState());
			box(b, p, c, st, 0, 0, 3, sz1, 3);
			for (int z = 1; z < sz1; z++) {
				b.air(3, 1, z);
				b.air(3, 2, z);
			}
			roof(b, p, c, flat, 0, 3, 0, sz1, 4, false, 0);
			b.set(1, 1, 1, Blocks.CRAFTING_TABLE.defaultBlockState());
			b.set(2, 1, 1, Blocks.CHEST.defaultBlockState());
			b.set(1, 1, sz1 - 1, grindstone(Direction.EAST));
			soft(b, 2, 1, sz1 - 1, barrel(Direction.UP));
			if (sz1 >= 4) {
				soft(b, 1, 1, 2, p.planks.defaultBlockState());
				soft(b, 1, 2, 2, Bp.slab(woodSlab(p), false));
			}
			soft(b, 1, 2, sz1 / 2 + 1, Bp.wallTorch(Direction.EAST));
			// Logs seasoning in front of the shed.
			for (int z = sz1 + 1; z <= Math.min(sz1 + 2, sz - 1); z++) {
				for (int x = 0; x <= 2; x++) {
					soft(b, x, 1, z, Bp.log(p.log, Direction.Axis.X));
				}
				soft(b, 1, 2, z, Bp.log(p.log, Direction.Axis.X));
			}
		}

		// The back of the yard: a log on sawhorses, the tar cauldron over its fire, barrels of tar.
		int sh0 = Math.max(xs, cx - 3);
		if (sh0 + 2 < cx) {
			soft(b, sh0, 1, 0, p.fence.defaultBlockState());
			soft(b, sh0 + 2, 1, 0, p.fence.defaultBlockState());
			for (int x = sh0; x <= sh0 + 2; x++) {
				soft(b, x, 2, 0, Bp.log(p.log, Direction.Axis.X));
			}
		}
		int tx = cx + 3;
		if (tx <= w - 1) {
			b.set(tx, 0, 0, Blocks.CAMPFIRE.defaultBlockState());
			b.set(tx, 1, 0, Blocks.CAULDRON.defaultBlockState());
			soft(b, tx + 1, 1, 0, barrel(Direction.UP));
			soft(b, tx - 1, 1, 0, barrel(Direction.UP));
		}
		// Timber piled along the side.
		int lx = w - 1;
		if (lx > cx + hb + 1) {
			for (int z = 2; z <= Math.min(sz - 1, 7); z++) {
				soft(b, lx, 1, z, Bp.log(p.log, Direction.Axis.Z));
				if (z > 2 && z < Math.min(sz - 1, 7)) {
					soft(b, lx, 2, z, Bp.log(p.log, Direction.Axis.Z));
				}
			}
		}
		if (!shed && cx - hb - 2 >= 0) {
			for (int z = 2; z <= Math.min(sz - 1, 5); z++) {
				soft(b, 0, 1, z, p.planks.defaultBlockState());
				soft(b, 0, 2, z, Bp.slab(woodSlab(p), false));
			}
			soft(b, 0, 1, 1, Blocks.CRAFTING_TABLE.defaultBlockState());
		}
		// A lamp post at the head of the slipway and mooring posts in the water.
		int lp = cx - hb - 2;
		if (lp >= 0 && sz - 1 >= 0 && !b.has(lp, 1, sz - 1)) {
			b.set(lp, 1, sz - 1, p.fence.defaultBlockState());
			b.set(lp, 2, sz - 1, p.fence.defaultBlockState());
			b.set(lp, 3, sz - 1, Bp.lantern(false));
		}
		int mz = sz + 2;
		if (mz <= d - 1) {
			for (int s = -1; s <= 1; s += 2) {
				int x = cx + s * (hb + 3);
				if (in(b, x, mz)) {
					post(b, x, mz, p.log, 1);
				}
			}
		}
	}

	private static int railY(int z, int sz) {
		return z < sz ? 0 : Math.max(-4, -1 - (z - sz) / 2);
	}
}
