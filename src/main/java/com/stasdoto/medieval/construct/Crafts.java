package com.stasdoto.medieval.construct;

import java.util.Random;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import com.stasdoto.medieval.history.Culture;

/**
 * Crafts and trades: sheds, potters, tanners, bakers, brewers, weavers, carpenters, iron smelters, kilns, charcoal
 * burners, bee yards, stables and merchants' warehouses. Each plot is drawn whole, facing the street (+z).
 */
final class Crafts {
	private Crafts() {
	}

	static final BlockState AIR = Blocks.AIR.defaultBlockState();

	/** How a people builds its plain working buildings. */
	enum Style {
		LOG, WATTLE, TIMBER, RUBBLE, NORSE, STONE, MUD, PLAIN
	}

	/** The look of one building: walls, roof, wood and the owner's means. */
	static final class Look {
		Culture c;
		Style s;
		boolean thatch, flat, turf;
		Block rs, rsl;
		BlockState rf, gable;
		Block wslab, wstairs, shelf;
		BlockState path;
		int wealth;
		/** Muslim lands (no beer, flat roofs). */
		boolean south;
		/** Forest peoples of the North and East (mead, log hives). */
		boolean north;
	}

	static Look look(Bp b, Palette p, Culture c) {
		Look k = new Look();
		k.c = c;
		k.rs = p.roofStairs;
		k.rsl = p.roofSlab;
		k.rf = p.roofFull;
		k.gable = p.planks.defaultBlockState();
		k.path = Blocks.DIRT_PATH.defaultBlockState();
		switch (c) {
			case RUS, BALT_FINN -> {
				k.s = Style.LOG;
				k.thatch = b.r.nextInt(4) == 0;
				k.north = true;
			}
			case WEST_SLAV, MAGYAR -> {
				k.s = c == Culture.WEST_SLAV && b.r.nextInt(3) == 0 ? Style.LOG : Style.WATTLE;
				k.thatch = true;
				k.gable = p.plaster;
				k.north = c == Culture.WEST_SLAV;
			}
			case LATIN -> {
				int v = b.r.nextInt(4);
				k.s = v < 2 ? Style.TIMBER : Style.RUBBLE;
				k.gable = v < 2 ? p.plaster : p.stone;
				k.thatch = v == 0 && b.r.nextBoolean();
				if (v == 3) {
					// Italy-like: rubble walls under terracotta tiles.
					k.rs = Blocks.BRICK_STAIRS;
					k.rsl = Blocks.BRICK_SLAB;
					k.rf = Blocks.BRICKS.defaultBlockState();
				}
			}
			case CELTIC -> {
				p.stone = Blocks.COBBLESTONE.defaultBlockState();
				p.stone2 = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
				k.s = Style.RUBBLE;
				k.gable = p.stone;
				k.thatch = true;
			}
			case NORSE -> {
				k.s = Style.NORSE;
				k.turf = b.r.nextInt(3) != 0;
				k.north = true;
			}
			case BYZANTINE, CAUCASUS -> {
				k.s = Style.STONE;
				k.gable = p.stone;
			}
			case ANDALUSI, ARAB, PERSIAN -> {
				k.s = Style.MUD;
				k.flat = true;
				k.south = true;
				k.path = Blocks.PACKED_MUD.defaultBlockState();
			}
			default -> {
				k.s = Style.PLAIN;
				k.thatch = true;
			}
		}
		Block pl = p.planks;
		if (pl == Blocks.OAK_PLANKS) {
			k.wslab = Blocks.OAK_SLAB;
			k.wstairs = Blocks.OAK_STAIRS;
			k.shelf = Blocks.OAK_SHELF;
		} else if (pl == Blocks.JUNGLE_PLANKS) {
			k.wslab = Blocks.JUNGLE_SLAB;
			k.wstairs = Blocks.JUNGLE_STAIRS;
			k.shelf = Blocks.JUNGLE_SHELF;
		} else if (pl == Blocks.DARK_OAK_PLANKS) {
			k.wslab = Blocks.DARK_OAK_SLAB;
			k.wstairs = Blocks.DARK_OAK_STAIRS;
			k.shelf = Blocks.DARK_OAK_SHELF;
		} else {
			k.wslab = Blocks.SPRUCE_SLAB;
			k.wstairs = Blocks.SPRUCE_STAIRS;
			k.shelf = Blocks.SPRUCE_SHELF;
		}
		k.wealth = b.r.nextInt(4) == 0 ? 2 : b.r.nextInt(3) == 0 ? 0 : 1;
		return k;
	}

	// ---------------------------------------------------------------- small helpers

	static BlockState st(Block bl) {
		return bl.defaultBlockState();
	}

	static boolean inside(Bp b, int x, int z) {
		return x >= 0 && z >= 0 && x < b.w && z < b.d;
	}

	static BlockState barrel(Direction f) {
		return Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, f);
	}

	static void cauldron(Bp b, int x, int y, int z) {
		b.set(x, y, z, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
	}

	static BlockState lit(Block furnace, Direction f) {
		return Bp.facing(furnace, f).setValue(BlockStateProperties.LIT, true);
	}

	/** A hanging lantern with a beam above it (only where the beam cell is free). */
	static void hang(Bp b, Palette p, int x, int y, int z) {
		b.soft(x, y + 1, z, Bp.log(p.strippedLog, Direction.Axis.X));
		b.set(x, y, z, Bp.lantern(true));
	}

	static BlockState potted(Random r) {
		return switch (r.nextInt(8)) {
			case 0 -> Blocks.POTTED_POPPY.defaultBlockState();
			case 1 -> Blocks.POTTED_CORNFLOWER.defaultBlockState();
			case 2 -> Blocks.POTTED_DANDELION.defaultBlockState();
			case 3 -> Blocks.POTTED_OXEYE_DAISY.defaultBlockState();
			case 4 -> Blocks.POTTED_FERN.defaultBlockState();
			case 5 -> Blocks.POTTED_AZURE_BLUET.defaultBlockState();
			default -> Blocks.FLOWER_POT.defaultBlockState();
		};
	}

	static BlockState flower(Random r) {
		return switch (r.nextInt(9)) {
			case 0 -> Blocks.POPPY.defaultBlockState();
			case 1 -> Blocks.CORNFLOWER.defaultBlockState();
			case 2 -> Blocks.DANDELION.defaultBlockState();
			case 3 -> Blocks.OXEYE_DAISY.defaultBlockState();
			case 4 -> Blocks.AZURE_BLUET.defaultBlockState();
			case 5 -> Blocks.ALLIUM.defaultBlockState();
			case 6 -> Blocks.WILDFLOWERS.defaultBlockState();
			default -> Blocks.SHORT_GRASS.defaultBlockState();
		};
	}

	/** Yard surface wherever nothing is set yet: mostly {@code a}, one cell in {@code n} is {@code a2}. */
	static void ground(Bp b, BlockState a, BlockState a2, int n) {
		for (int x = 0; x < b.w; x++) {
			for (int z = 0; z < b.d; z++) {
				if (!b.has(x, 0, z)) {
					b.set(x, 0, z, n > 0 && b.r.nextInt(n) == 0 ? a2 : a);
				}
			}
		}
	}

	/** The usual trodden yard of the people. */
	static void yard(Bp b, Look k) {
		if (k.south) {
			ground(b, Blocks.PACKED_MUD.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(), 3);
		} else if (k.s == Style.STONE) {
			ground(b, Blocks.COARSE_DIRT.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), 3);
		} else {
			ground(b, Blocks.COARSE_DIRT.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
		}
	}

	/** A pile of logs over x0..x1 × z0..z1, {@code h} high, the top layer ragged. */
	static void logs(Bp b, Block log, int x0, int z0, int x1, int z1, int h, Direction.Axis ax) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
				for (int y = 1; y <= h; y++) {
					if (y == h && h > 1 && b.r.nextInt(3) == 0) {
						continue;
					}
					b.set(x, y, z, Bp.log(log, ax));
				}
			}
		}
	}

	/** A straight line of fence (only into free cells). */
	static void fenceLine(Bp b, Palette p, int x0, int z0, int x1, int z1) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
				b.soft(x, 1, z, p.fence.defaultBlockState());
			}
		}
	}

	/** A two-wheeled cart: a plank body two blocks long from (x, z), wheels by the first block, a shaft in front. */
	static void cart(Bp b, Palette p, Look k, int x, int z, boolean alongX, BlockState load) {
		int dx = alongX ? 1 : 0, dz = alongX ? 0 : 1;
		for (int i = 0; i < 2; i++) {
			b.set(x + dx * i, 1, z + dz * i, Bp.slab(k.wslab, true));
			if (load != null && (i == 0 || b.r.nextBoolean())) {
				b.set(x + dx * i, 2, z + dz * i, load);
			}
		}
		if (b.r.nextBoolean()) {
			Direction.Axis ax = alongX ? Direction.Axis.Z : Direction.Axis.X;
			b.set(x - dz, 1, z - dx, Bp.log(p.strippedLog, ax));
			b.set(x + dz, 1, z + dx, Bp.log(p.strippedLog, ax));
		} else {
			b.set(x - dz, 1, z - dx, Bp.trapdoor(p.trapdoor, alongX ? Direction.NORTH : Direction.WEST, false, true));
			b.set(x + dz, 1, z + dx, Bp.trapdoor(p.trapdoor, alongX ? Direction.SOUTH : Direction.EAST, false, true));
		}
		b.set(x + dx * 2, 1, z + dz * 2, p.fence.defaultBlockState());
	}

	/** Interior furniture for what is still empty (only for a real room). */
	static void furnish(Bp b, Look k, int x0, int z0, int x1, int z1, Interior.Use use) {
		if (x1 >= x0 && z1 >= z0) {
			Interior.room(b, k.c, x0, z0, x1, z1, 1, use, k.wealth);
		}
	}

	// ---------------------------------------------------------------- walls, roofs, doors

	/** One wall block of the people's style at (x, y, z) on the ring x0..x1 × z0..z1, walls {@code h} high. */
	static BlockState wall(Bp b, Palette p, Look k, int x, int y, int z, int x0, int z0, int x1, int z1, int h) {
		boolean ex = x == x0 || x == x1, ez = z == z0 || z == z1;
		boolean corner = ex && ez;
		Direction.Axis run = ez ? Direction.Axis.X : Direction.Axis.Z;
		boolean post = corner || (ez ? (x - x0) % 3 == 0 : (z - z0) % 3 == 0);
		return switch (k.s) {
			case LOG -> Bp.log(p.log, corner ? (y % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z) : run);
			case WATTLE -> post ? Bp.log(p.log, Direction.Axis.Y) : y == h ? Bp.log(p.strippedLog, run) : p.plaster;
			case TIMBER -> y == 1 ? p.stone : post ? Bp.log(p.log, Direction.Axis.Y) : y == h ? Bp.log(p.strippedLog, run) : p.plaster;
			case RUBBLE -> b.r.nextInt(5) == 0 ? p.stone2 : p.stone;
			case NORSE -> post ? Bp.log(p.log, Direction.Axis.Y) : p.planks.defaultBlockState();
			case STONE -> y % 4 == 0 ? p.stone2 : p.stone;
			case MUD -> p.stone;
			case PLAIN -> corner ? Bp.log(p.log, Direction.Axis.Y) : p.planks.defaultBlockState();
		};
	}

	/** A walled box: foundation, sill, floor ({@code floor}, null = keep) and walls 1..h on the ring x0..x1 × z0..z1. */
	static void box(Bp b, Palette p, Look k, int x0, int z0, int x1, int z1, int h, BlockState floor) {
		BlockState found = k.s == Style.MUD || k.s == Style.STONE ? p.stone2 : Blocks.COBBLESTONE.defaultBlockState();
		b.fill(x0, -3, z0, x1, -1, z1, found);
		if (floor != null && x1 - x0 >= 2 && z1 - z0 >= 2) {
			b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, floor);
		}
		for (int y = 0; y <= h; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					if (x == x0 || x == x1 || z == z0 || z == z1) {
						b.set(x, y, z, y == 0 ? found : wall(b, p, k, x, y, z, x0, z0, x1, z1, h));
					}
				}
			}
		}
	}

	/** Opens one side (z = zf) of a box between its corner posts, rows y0..y1, leaving a post every {@code every}. */
	static void openFront(Bp b, Palette p, int x0, int x1, int zf, int y0, int y1, int every) {
		for (int x = x0; x <= x1; x++) {
			boolean post = x == x0 || x == x1 || (every > 0 && (x - x0) % every == 0);
			for (int y = y0; y <= y1; y++) {
				b.set(x, y, zf, post ? Bp.log(p.log, Direction.Axis.Y) : AIR);
			}
		}
	}

	/** A roof over walls x0..x1 × z0..z1 starting at height y; returns the height of the ridge (top block). */
	static int roof(Bp b, Palette p, Look k, int x0, int z0, int x1, int z1, int y, boolean alongX) {
		if (k.flat) {
			b.fill(x0, y, z0, x1, y, z1, k.rf);
			for (int x = x0; x <= x1; x++) {
				b.set(x, y + 1, z0, Bp.slab(k.rsl, false));
				b.set(x, y + 1, z1, Bp.slab(k.rsl, false));
			}
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y + 1, z, Bp.slab(k.rsl, false));
				b.set(x1, y + 1, z, Bp.slab(k.rsl, false));
			}
			// Beam ends poking out of the street wall (not in whitewashed al-Andalus).
			if (k.c != Culture.ANDALUSI && z1 + 1 < b.d) {
				for (int x = x0 + 1; x < x1; x += 2) {
					b.soft(x, y - 1, z1 + 1, Bp.log(p.log, Direction.Axis.Z));
				}
			}
			return y + 1;
		}
		int half = alongX ? (z1 - z0 + 2) / 2 : (x1 - x0 + 2) / 2;
		if (k.thatch) {
			hay(b, x0, z0, x1, z1, y, alongX, k.gable);
			return y + half;
		}
		b.gableRoof(x0, x1, z0, z1, y, alongX, 1, k.rs, k.rsl, k.rf, k.gable);
		if (k.turf) {
			BlockState moss = Blocks.MOSS_CARPET.defaultBlockState();
			int lo = (alongX ? z0 : x0) - 1, hi = (alongX ? z1 : x1) + 1, layer = 0;
			int a0 = (alongX ? x0 : z0) - 1, a1 = (alongX ? x1 : z1) + 1;
			while (lo <= hi) {
				for (int i = a0; i <= a1; i++) {
					if (alongX) {
						b.set(i, y + layer + 1, lo, moss);
						b.set(i, y + layer + 1, hi, moss);
					} else {
						b.set(lo, y + layer + 1, i, moss);
						b.set(hi, y + layer + 1, i, moss);
					}
				}
				lo++;
				hi--;
				layer++;
			}
		}
		return y + half;
	}

	/** A thatched roof of hay with gable ends of {@code gable}. */
	static void hay(Bp b, int x0, int z0, int x1, int z1, int y, boolean alongX, BlockState gable) {
		BlockState hay = Blocks.HAY_BLOCK.defaultBlockState();
		int lo = (alongX ? z0 : x0) - 1, hi = (alongX ? z1 : x1) + 1, layer = 0;
		while (lo <= hi) {
			int yy = y + layer;
			if (alongX) {
				for (int x = x0 - 1; x <= x1 + 1; x++) {
					b.set(x, yy, lo, hay);
					b.set(x, yy, hi, hay);
				}
				for (int z = lo + 1; z < hi; z++) {
					if (z >= z0 && z <= z1) {
						b.set(x0, yy, z, gable);
						b.set(x1, yy, z, gable);
					}
				}
			} else {
				for (int z = z0 - 1; z <= z1 + 1; z++) {
					b.set(lo, yy, z, hay);
					b.set(hi, yy, z, hay);
				}
				for (int x = lo + 1; x < hi; x++) {
					if (x >= x0 && x <= x1) {
						b.set(x, yy, z0, gable);
						b.set(x, yy, z1, gable);
					}
				}
			}
			lo++;
			hi--;
			layer++;
		}
	}

	/** A door with an arch over it in stone and mud brick, and a trodden step outside. */
	static void door(Bp b, Palette p, Look k, int x, int z, Direction out) {
		b.door(p.door, x, 1, z, out);
		if ((k.s == Style.MUD || k.s == Style.STONE || k.s == Style.RUBBLE) && b.has(x, 3, z)) {
			b.set(x, 3, z, Bp.stairs(k.s == Style.MUD ? Blocks.SMOOTH_SANDSTONE_STAIRS : Blocks.STONE_BRICK_STAIRS, out, true));
		}
		int sx = x + out.getStepX(), sz = z + out.getStepZ();
		if (inside(b, sx, sz) && !b.has(sx, 0, sz)) {
			b.set(sx, 0, sz, k.path);
		}
	}

	/** A pair of doors at x and x + 1 in the street wall z. */
	static void doubleDoor(Bp b, Palette p, int x, int z) {
		b.door(p.door, x, 1, z, Direction.SOUTH);
		b.door(p.door, x + 1, 1, z, Direction.SOUTH);
		BlockState lo = b.get(x + 1, 1, z);
		if (lo != null) {
			BlockState right = lo.setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.RIGHT);
			b.set(x + 1, 1, z, right);
			b.set(x + 1, 2, z, right.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
		}
	}

	/** A window: a lattice in the South, glass for the rich, else an opening with an open shutter outside. */
	static void window(Bp b, Palette p, Look k, int x, int y, int z, Direction out) {
		BlockState w = k.s == Style.MUD ? Blocks.IRON_BARS.defaultBlockState() : k.wealth >= 2 ? Blocks.GLASS_PANE.defaultBlockState() : AIR;
		b.set(x, y, z, w);
		int sx = x + out.getStepX(), sz = z + out.getStepZ();
		if (k.s != Style.MUD && inside(b, sx, sz) && !b.has(sx, y, sz)) {
			b.set(sx, y, sz, Bp.trapdoor(p.trapdoor, out, false, true));
		}
	}

	/** A smoking chimney column from y0 up to y1, smoke on top. */
	static void chimney(Bp b, BlockState s, int x, int z, int y0, int y1) {
		for (int y = y0; y <= y1; y++) {
			b.set(x, y, z, s);
		}
		b.set(x, y1 + 1, z, Blocks.CAMPFIRE.defaultBlockState());
	}

	static Direction inward(int dx, int dz) {
		if (Math.abs(dx) >= Math.abs(dz)) {
			return dx > 0 ? Direction.WEST : Direction.EAST;
		}
		return dz > 0 ? Direction.NORTH : Direction.SOUTH;
	}

	/**
	 * A domed oven or kiln of {@code full} with {@code stair} shoulders: radius 1 is 3×3, radius 2 is 5×5, centred at
	 * (cx, cz), a fire inside, its mouth ({@code mouth}, or null for an open arch) towards {@code out}, smoke on top.
	 */
	static void oven(Bp b, int cx, int cz, int rad, BlockState full, Block stair, BlockState mouth, Direction out) {
		int R = rad >= 2 ? 2 : 1;
		for (int dx = -R; dx <= R; dx++) {
			for (int dz = -R; dz <= R; dz++) {
				int m = Math.max(Math.abs(dx), Math.abs(dz));
				boolean corner = R == 2 && Math.abs(dx) == 2 && Math.abs(dz) == 2;
				if (corner) {
					continue;
				}
				int x = cx + dx, z = cz + dz;
				b.set(x, -1, z, full);
				b.set(x, 0, z, full);
				if (R == 1) {
					if (m == 1) {
						b.set(x, 1, z, full);
						if (dx == 0 || dz == 0) {
							b.set(x, 2, z, Bp.stairs(stair, inward(dx, dz), false));
						}
					}
				} else {
					if (m == 2) {
						b.set(x, 1, z, full);
						b.set(x, 2, z, Bp.stairs(stair, inward(dx, dz), false));
					} else if (m == 1) {
						b.air(x, 1, z);
						b.set(x, 2, z, full);
						if (dx == 0 || dz == 0) {
							b.set(x, 3, z, Bp.stairs(stair, inward(dx, dz), false));
						}
					}
				}
			}
		}
		b.set(cx, 1, cz, Blocks.CAMPFIRE.defaultBlockState());
		if (R == 1) {
			b.set(cx, 2, cz, full);
			b.set(cx, 3, cz, Blocks.CAMPFIRE.defaultBlockState());
		} else {
			b.air(cx, 2, cz);
			b.set(cx, 3, cz, full);
			b.set(cx, 4, cz, Blocks.CAMPFIRE.defaultBlockState());
		}
		int mx = cx + out.getStepX() * R, mz = cz + out.getStepZ() * R;
		b.set(mx, 1, mz, mouth != null ? mouth : AIR);
	}

	/** Materials of an oven or kiln dome for the people (full block, then its stairs). */
	static BlockState ovenFull(Bp b, Look k) {
		if (k.south) {
			return b.r.nextBoolean() ? Blocks.MUD_BRICKS.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState();
		}
		if (k.s == Style.STONE || (k.s == Style.RUBBLE && b.r.nextBoolean())) {
			return Blocks.BRICKS.defaultBlockState();
		}
		return b.r.nextBoolean() ? Blocks.MUD_BRICKS.defaultBlockState() : Blocks.BRICKS.defaultBlockState();
	}

	static Block ovenStair(BlockState full) {
		return full.getBlock() == Blocks.BRICKS ? Blocks.BRICK_STAIRS : Blocks.MUD_BRICK_STAIRS;
	}

	// ---------------------------------------------------------------- 1. shed

	/** A back-yard outbuilding: a woodshed, a little store, a sty or hen house, or a cart shelter. */
	static void shed(Bp b, Palette p, Culture c) {
		Look k = look(b, p, c);
		int W = b.w, D = b.d;
		int x0 = 0, x1 = W - 1, mx = W / 2;
		switch (b.r.nextInt(4)) {
			case 0 -> {
				// Woodshed: open to the yard, split logs stacked with their ends out.
				int z1 = Math.max(2, D - 3);
				box(b, p, k, x0, 0, x1, z1, 3, Blocks.COARSE_DIRT.defaultBlockState());
				openFront(b, p, x0, x1, z1, 1, 2, 0);
				roof(b, p, k, x0, 0, x1, z1, 4, true);
				for (int x = x0 + 1; x <= x1 - 1; x++) {
					for (int z = 1; z <= z1 - 1; z++) {
						int hh = z == z1 - 1 && z1 > 2 ? 2 : 3;
						for (int y = 1; y <= hh; y++) {
							if (y == hh && b.r.nextInt(4) == 0) {
								continue;
							}
							b.set(x, y, z, Bp.log(b.r.nextInt(5) == 0 ? p.strippedLog : p.log, Direction.Axis.Z));
						}
					}
				}
				// Chopping block and loose logs in the yard.
				if (z1 + 2 <= D - 1) {
					b.set(mx, 1, z1 + 2, Bp.log(p.strippedLog, Direction.Axis.Y));
					b.soft(x0 + 1, 1, D - 1, Bp.log(p.log, Direction.Axis.X));
					b.soft(x0 + 2, 1, D - 1, Bp.log(p.log, Direction.Axis.X));
				}
				b.soft(x1, 1, D - 1, Bp.slab(k.wslab, false));
			}
			case 1 -> {
				// A little store: barrels, sacks and a chest.
				int z1 = D - 2;
				box(b, p, k, x0, 0, x1, z1, 3, p.floor);
				roof(b, p, k, x0, 0, x1, z1, 4, true);
				door(b, p, k, mx, z1, Direction.SOUTH);
				for (int x = x0 + 1; x <= x1 - 1; x++) {
					b.set(x, 1, 1, k.south && b.r.nextBoolean() ? st(Blocks.DECORATED_POT) : barrel(Direction.UP));
					if (b.r.nextBoolean()) {
						b.set(x, 2, 1, b.r.nextBoolean() ? barrel(Direction.UP) : Blocks.HAY_BLOCK.defaultBlockState());
					}
				}
				for (int z = 2; z <= z1 - 1; z++) {
					b.set(x0 + 1, 1, z, z % 2 == 0 ? Blocks.HAY_BLOCK.defaultBlockState() : Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
				}
				if (z1 - 1 >= 2) {
					b.set(x1 - 1, 1, 2, Blocks.CHEST.defaultBlockState());
				}
				hang(b, p, mx, 3, Math.max(1, z1 / 2));
			}
			case 2 -> {
				// A sty (or a goat fold in Muslim lands) or a hen house: a low hut and a pen.
				boolean pigs = !k.south && b.r.nextBoolean();
				int hz1 = Math.max(2, D / 2 - 1);
				box(b, p, k, x0, 0, x1, hz1, 2, Blocks.COARSE_DIRT.defaultBlockState());
				roof(b, p, k, x0, 0, x1, hz1, 3, true);
				b.air(mx, 1, hz1);
				b.air(mx, 2, hz1);
				for (int x = x0 + 1; x <= x1 - 1; x++) {
					if (x != mx) {
						b.set(x, 1, 1, Blocks.HAY_BLOCK.defaultBlockState());
					}
				}
				if (!pigs) {
					// Perches.
					b.set(x0 + 1, 2, hz1 - 1 > 1 ? hz1 - 1 : 1, Bp.slab(k.wslab, false));
					b.set(mx, 0, hz1, Bp.slab(k.wslab, false));
				}
				for (int x = x0; x <= x1; x++) {
					for (int z = hz1 + 1; z <= D - 1; z++) {
						b.set(x, 0, z, pigs ? (b.r.nextInt(3) == 0 ? Blocks.COARSE_DIRT : Blocks.MUD).defaultBlockState()
								: (b.r.nextInt(3) == 0 ? Blocks.DIRT : Blocks.COARSE_DIRT).defaultBlockState());
					}
				}
				for (int z = hz1 + 1; z <= D - 1; z++) {
					b.set(x0, 1, z, p.fence.defaultBlockState());
					b.set(x1, 1, z, p.fence.defaultBlockState());
				}
				for (int x = x0; x <= x1; x++) {
					b.set(x, 1, D - 1, x == mx ? Bp.facing(p.gate, Direction.SOUTH) : p.fence.defaultBlockState());
				}
				if (D - 2 > hz1) {
					cauldron(b, x0 + 1, 1, D - 2);
					b.set(x1 - 1, 1, D - 2, pigs ? st(Blocks.COMPOSTER) : Blocks.HAY_BLOCK.defaultBlockState());
				}
			}
			default -> {
				// A cart shelter: open front, a cart under the roof, a spare wheel and a barrel.
				int z1 = D - 2;
				box(b, p, k, x0, 0, x1, z1, 3, Blocks.GRAVEL.defaultBlockState());
				openFront(b, p, x0, x1, z1, 1, 2, 0);
				roof(b, p, k, x0, 0, x1, z1, 4, true);
				BlockState load = switch (b.r.nextInt(3)) {
					case 0 -> Blocks.HAY_BLOCK.defaultBlockState();
					case 1 -> barrel(Direction.UP);
					default -> null;
				};
				cart(b, p, k, mx, 1, false, load);
				b.soft(x1 - 1, 1, 1, Bp.log(p.strippedLog, Direction.Axis.X));
				b.soft(x0 + 1, 1, 1, barrel(Direction.UP));
				if (z1 - 1 > 2) {
					b.soft(x0 + 1, 1, z1 - 1, Bp.trapdoor(p.trapdoor, Direction.EAST, false, true));
				}
			}
		}
		yard(b, k);
	}

	// ---------------------------------------------------------------- 2. pottery

	/** A potter's workshop: wheel, shelves of pots, a clay pit and a domed kiln in the yard. */
	static void pottery(Bp b, Palette p, Culture c) {
		Look k = look(b, p, c);
		int W = b.w, D = b.d, x0 = 0, x1 = W - 1, mx = W / 2;
		// The workshop stands either on the street (yard behind) or at the back (yard to the street).
		boolean front = b.r.nextBoolean();
		int hd = Math.max(5, Math.min(7, D / 2));
		int hz0 = front ? D - hd : 0, hz1 = front ? D - 1 : hd - 1;
		int yz0 = front ? 0 : hd, yz1 = front ? D - hd - 1 : D - 1;
		int h = 3;
		box(b, p, k, x0, hz0, x1, hz1, h, p.floor);
		roof(b, p, k, x0, hz0, x1, hz1, h + 1, true);
		door(b, p, k, mx, hz1, Direction.SOUTH);
		if (front) {
			door(b, p, k, mx, hz0, Direction.NORTH);
		}
		for (int x : new int[]{mx - 2, mx + 2}) {
			if (x > x0 && x < x1) {
				window(b, p, k, x, 2, hz1, Direction.SOUTH);
			}
		}
		int ix0 = x0 + 1, ix1 = x1 - 1, iz0 = hz0 + 1, iz1 = hz1 - 1;
		// Shelves along the back wall, pots, clay and barrels under them.
		for (int x = ix0; x <= ix1; x++) {
			if (front && x == mx) {
				continue;
			}
			b.set(x, 2, iz0, Bp.facing(k.shelf, Direction.SOUTH));
			int t = b.r.nextInt(4);
			b.set(x, 1, iz0, t == 0 ? st(Blocks.DECORATED_POT) : t == 1 ? st(Blocks.CLAY) : t == 2 ? barrel(Direction.UP) : potted(b.r));
		}
		// The wheel by the window with the potter's seat and a bowl of water.
		int wz = (iz0 + iz1 + 1) / 2;
		if (b.r.nextBoolean()) {
			b.set(ix0 + 1, 1, wz, Bp.facing(Blocks.STONECUTTER, Direction.SOUTH));
		} else {
			b.set(ix0 + 1, 1, wz, p.fence.defaultBlockState());
			b.set(ix0 + 1, 2, wz, Bp.slab(Blocks.SMOOTH_STONE_SLAB, false));
		}
		b.set(ix0, 1, wz, Bp.stairs(k.wstairs, Direction.WEST, false));
		if (!b.has(ix0 + 2, 1, wz) && ix0 + 2 != mx) {
			cauldron(b, ix0 + 2, 1, wz);
		}
		b.bed(p.bed, ix1, 1, iz1, Direction.NORTH);
		for (int x = ix0; x <= ix1; x++) {
			if (x != mx && b.r.nextInt(3) == 0) {
				b.soft(x, 1, iz1, st(Blocks.DECORATED_POT));
			}
		}
		hang(b, p, mx, h, (iz0 + iz1) / 2);
		furnish(b, k, ix0, iz0, ix1, iz1, Interior.Use.WORKSHOP);

		// The yard: kiln at the side, clay pit on the other, pots drying in the sun.
		int yd = yz1 - yz0 + 1;
		int R = yd >= 6 && W >= 12 ? 2 : 1;
		int kx = x1 - R, kz = (yz0 + yz1) / 2;
		BlockState of = ovenFull(b, k);
		oven(b, kx, kz, R, of, ovenStair(of), lit(Blocks.FURNACE, Direction.WEST), Direction.WEST);
		if (kz + R + 1 <= yz1) {
			logs(b, p.log, kx - R, kz + R + 1, x1, kz + R + 1, 2, Direction.Axis.X);
		} else if (kz - R - 1 >= yz0) {
			logs(b, p.log, kx - R, kz - R - 1, x1, kz - R - 1, 2, Direction.Axis.X);
		}
		int px0 = x0 + 1, px1 = Math.min(px0 + 2, mx - 1);
		int pz0 = yz0 + 1, pz1 = Math.min(yz1 - 1, pz0 + 2);
		for (int x = px0; x <= px1; x++) {
			for (int z = pz0; z <= pz1; z++) {
				boolean edge = x == px0 || x == px1 || z == pz0 || z == pz1;
				b.set(x, -1, z, st(Blocks.CLAY));
				b.set(x, 0, z, edge ? (b.r.nextInt(3) == 0 ? st(Blocks.MUD) : st(Blocks.CLAY)) : st(Blocks.WATER));
			}
		}
		for (int z = yz0; z <= yz1; z++) {
			b.soft(mx, 0, z, k.path);
		}
		for (int i = 0; i < 3 + W / 3; i++) {
			int x = x0 + b.r.nextInt(W), z = yz0 + b.r.nextInt(Math.max(1, yd));
			if (x != mx && !b.has(x, 0, z) && !b.has(x, 1, z)) {
				b.set(x, 1, z, b.r.nextBoolean() ? st(Blocks.DECORATED_POT) : potted(b.r));
			}
		}
		if (front) {
			fenceLine(b, p, x0, 0, x1, 0);
		} else {
			fenceLine(b, p, x0, D - 1, mx - 1, D - 1);
			fenceLine(b, p, mx + 1, D - 1, x1, D - 1);
			b.soft(mx, 1, D - 1, Bp.facing(p.gate, Direction.SOUTH));
		}
		yard(b, k);
	}

	// ---------------------------------------------------------------- 3. tannery

	/** A tanning yard: sunk vats and tubs, hides drying on racks, bark barrels, a shed with the scraping beam. */
	static void tannery(Bp b, Palette p, Culture c) {
		Look k = look(b, p, c);
		int W = b.w, D = b.d, x0 = 0, x1 = W - 1, mx = W / 2;
		DyeColor[] hides = {DyeColor.BROWN, DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.BROWN, DyeColor.ORANGE};
		int sz1 = Math.max(3, Math.min(5, D / 3));
		box(b, p, k, x0, 0, x1, sz1, 3, Blocks.PACKED_MUD.defaultBlockState());
		roof(b, p, k, x0, 0, x1, sz1, 4, true);
		// The left part of the shed is open to the yard; the right part is the tanner's room.
		int split = Math.max(x0 + 3, Math.min(x1 - 3, W / 2));
		openFront(b, p, x0, split, sz1, 1, 2, 3);
		for (int z = 1; z < sz1; z++) {
			for (int y = 1; y <= 3; y++) {
				b.set(split, y, z, p.planks.defaultBlockState());
			}
		}
		door(b, p, k, (split + x1) / 2, sz1, Direction.SOUTH);
		b.bed(p.bed, x1 - 1, 1, sz1 - 1, Direction.NORTH);
		b.soft(split + 1, 1, 1, st(Blocks.CHEST));
		hang(b, p, (split + x1) / 2, 3, Math.max(1, sz1 / 2));
		furnish(b, k, split + 1, 1, x1 - 1, sz1 - 1, Interior.Use.LIVING);
		// The work shed: scraping beam with a hide over it, stacked hides, bark barrels, a lime vat.
		int bz = Math.min(2, sz1 - 1);
		b.set(x0 + 1, 1, bz, Bp.log(p.strippedLog, Direction.Axis.X));
		b.set(x0 + 1, 2, bz, Blocks.CARPET.pick(DyeColor.BROWN).defaultBlockState());
		if (x0 + 2 < split) {
			b.set(x0 + 2, 1, bz, Bp.stairs(k.wstairs, Direction.WEST, false));
		}
		b.set(x0 + 1, 1, 1, Blocks.WOOL.pick(DyeColor.BROWN).defaultBlockState());
		b.set(x0 + 1, 2, 1, Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
		for (int x = x0 + 2; x < split; x++) {
			b.set(x, 1, 1, barrel(Direction.UP));
		}
		if (split - 1 > x0 + 2) {
			b.soft(split - 1, 1, sz1 - 1, st(Blocks.POWDER_SNOW_CAULDRON));
			b.soft(split - 1, 1, bz, Blocks.GRINDSTONE.defaultBlockState());
		}
		hang(b, p, (x0 + split) / 2, 3, Math.max(1, sz1 / 2));

		// Sunk vats rimmed with planks.
		int vz = sz1 + 2;
		for (int x = x0 + 1; x <= x1 - 1; x++) {
			for (int z = vz - 1; z <= vz + 1; z++) {
				boolean hole = z == vz && (x - x0) % 2 == 0 && x != mx;
				b.set(x, -2, z, st(Blocks.PACKED_MUD));
				b.set(x, -1, z, hole ? st(Blocks.WATER) : st(Blocks.PACKED_MUD));
				b.set(x, 0, z, hole ? st(Blocks.WATER) : p.planks.defaultBlockState());
			}
		}
		int zc = vz + 2;
		// A row of tubs: water, bark barrels and lime.
		if (zc + 1 <= D - 3) {
			int z = zc + 1;
			for (int x = x0 + 1; x <= x1 - 1; x++) {
				if (x == mx) {
					continue;
				}
				switch ((x - x0) % 3) {
					case 0 -> cauldron(b, x, 1, z);
					case 1 -> b.set(x, 1, z, barrel(Direction.UP));
					default -> b.set(x, 1, z, b.r.nextInt(3) == 0 ? st(Blocks.POWDER_SNOW_CAULDRON) : st(Blocks.COMPOSTER));
				}
			}
			zc += 3;
		}
		// Racks of hides.
		for (int z = zc + 1; z <= D - 1; z += 2) {
			if (b.r.nextBoolean()) {
				for (int x = x0 + 1; x <= x1 - 1; x++) {
					if (x != mx) {
						b.set(x, 1, z, p.fence.defaultBlockState());
						b.set(x, 2, z, Blocks.CARPET.pick(hides[b.r.nextInt(hides.length)]).defaultBlockState());
					}
				}
			} else {
				for (int y = 1; y <= 2; y++) {
					b.set(x0 + 1, y, z, p.fence.defaultBlockState());
					b.set(x1 - 1, y, z, p.fence.defaultBlockState());
				}
				for (int x = x0 + 1; x <= x1 - 1; x++) {
					b.set(x, 3, z, Bp.log(p.strippedLog, Direction.Axis.X));
					if (x > x0 + 1 && x < x1 - 1 && x != mx && b.r.nextInt(3) != 0) {
						b.set(x, 2, z, Blocks.WOOL.pick(hides[b.r.nextInt(hides.length)]).defaultBlockState());
					}
				}
			}
		}
		for (int z = sz1 + 1; z <= D - 1; z++) {
			b.soft(mx, 0, z, Blocks.COARSE_DIRT.defaultBlockState());
		}
		ground(b, Blocks.MUD.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(), 2);
	}

	// ---------------------------------------------------------------- 4. bakery

	/** A bakery on the street: a counter window, bread shelves, flour sacks, a big domed oven fired from inside. */
	static void bakery(Bp b, Palette p, Culture c) {
		Look k = look(b, p, c);
		int W = b.w, D = b.d, x0 = 0, x1 = W - 1, mx = W / 2;
		int R = D >= 11 && W >= 8 ? 2 : 1;
		// The oven stands behind the house; its mouth opens through the back wall.
		int hz0 = 2 * R + 2, hz1 = D - 1;
		int h = k.wealth >= 2 ? 4 : 3;
		box(b, p, k, x0, hz0, x1, hz1, h, p.floor);
		roof(b, p, k, x0, hz0, x1, hz1, h + 1, b.r.nextBoolean());
		int ox = x0 + R + 1, oz = hz0 - 1 - R;
		BlockState of = ovenFull(b, k);
		oven(b, ox, oz, R, of, ovenStair(of), lit(Blocks.SMOKER, Direction.SOUTH), Direction.SOUTH);
		b.air(ox, 1, hz0);
		// Firewood behind the house.
		if (x1 - 1 >= ox + R + 2 && hz0 - 2 >= 1) {
			logs(b, p.log, ox + R + 2, 1, x1 - 1, hz0 - 2, 2, Direction.Axis.Z);
		}
		int dx = W >= 8 ? x1 - 2 : x1 - 1;
		door(b, p, k, dx, hz1, Direction.SOUTH);
		int ix0 = x0 + 1, ix1 = x1 - 1, iz0 = hz0 + 1, iz1 = hz1 - 1;
		// The counter window to the street.
		for (int x = ix0; x <= dx - 2; x++) {
			b.set(x, 1, hz1, Bp.slab(k.wslab, true));
			b.air(x, 2, hz1);
		}
		window(b, p, k, x1, 2, (iz0 + iz1) / 2, Direction.EAST);
		// Flour sacks and grain by the back wall, a kneading table, bread shelves on the side wall.
		b.set(ix1, 1, iz0, Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
		b.set(ix1, 2, iz0, Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
		if (ix1 - 1 != ox) {
			b.set(ix1 - 1, 1, iz0, Blocks.HAY_BLOCK.defaultBlockState());
		}
		if (iz0 + 1 < iz1) {
			b.set(ix1, 1, iz0 + 1, Bp.slab(k.wslab, true));
			if (iz0 + 2 < iz1) {
				b.set(ix1, 1, iz0 + 2, Bp.slab(k.wslab, true));
			}
		}
		for (int z = iz0 + 1; z <= iz1; z++) {
			if (ix0 == ox && z == iz0) {
				continue;
			}
			b.set(ix0, 2, z, Bp.facing(k.shelf, Direction.EAST));
			b.set(ix0, 1, z, z % 2 == 0 ? barrel(Direction.UP) : Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
		}
		if (ix0 != ox) {
			b.set(ix0, 1, iz0, Blocks.CRAFTING_TABLE.defaultBlockState());
		}
		hang(b, p, mx, h, (iz0 + iz1) / 2);
		furnish(b, k, ix0, iz0, ix1, iz1, Interior.Use.WORKSHOP);
		yard(b, k);
	}

	// ---------------------------------------------------------------- 5. brewery

	/**
	 * A brewhouse or meadery (a copper over the fire, mash tubs, racks of barrels, a cellar, hops and hives); in Muslim
	 * lands a syrup and oil press with jars, in Byzantium a wine press, in Georgia jars sunk in the floor.
	 */
	static void brewery(Bp b, Palette p, Culture c) {
		Look k = look(b, p, c);
		int W = b.w, D = b.d, x0 = 0, x1 = W - 1, mx = W / 2;
		boolean qvevri = c == Culture.CAUCASUS;
		boolean press = k.south || qvevri || (c == Culture.BYZANTINE && b.r.nextBoolean());
		boolean mead = k.north || c == Culture.STEPPE;
		int hz1 = Math.max(5, D - 4);
		int h = 4;
		box(b, p, k, x0, 0, x1, hz1, h, p.floor);
		int ridge = roof(b, p, k, x0, 0, x1, hz1, h + 1, true);
		doubleDoor(b, p, mx - 1, hz1);
		for (int x : new int[]{x0 + 2, x1 - 2}) {
			if (x < mx - 2 || x > mx + 1) {
				window(b, p, k, x, 2, hz1, Direction.SOUTH);
			}
		}
		int ix0 = x0 + 1, ix1 = x1 - 1, iz0 = 1, iz1 = hz1 - 1;
		// The hearth with the copper, its chimney in the back wall.
		int hx = ix0 + 1;
		b.set(hx, 0, iz0, Blocks.CAMPFIRE.defaultBlockState());
		cauldron(b, hx, 1, iz0);
		chimney(b, k.s == Style.MUD ? p.stone : Blocks.COBBLESTONE.defaultBlockState(), hx, 0, 1, ridge + 1);
		b.set(ix0, 1, iz0, st(Blocks.COMPOSTER));
		// The press (oil, syrup or wine) or the mash tubs.
		int px = ix1 - 2;
		if (press && px > hx + 1) {
			for (int y = 1; y <= 2; y++) {
				b.set(px, y, iz0, Bp.log(p.log, Direction.Axis.Y));
				b.set(px + 2, y, iz0, Bp.log(p.log, Direction.Axis.Y));
			}
			for (int x = px; x <= px + 2; x++) {
				b.set(x, 3, iz0, Bp.log(p.log, Direction.Axis.X));
			}
			b.set(px + 1, 1, iz0, barrel(Direction.UP));
			b.set(px + 1, 2, iz0, Bp.log(p.strippedLog, Direction.Axis.Y));
			for (int x = hx + 1; x < px; x++) {
				b.set(x, 1, iz0, st(Blocks.DECORATED_POT));
			}
		} else {
			for (int x = hx + 1; x <= Math.min(hx + 3, ix1 - 1); x++) {
				b.set(x, 1, iz0, barrel(Direction.UP));
			}
		}
		// Racks of barrels (or rows of jars) along the side walls.
		for (int z = iz0 + 2; z <= iz1 - 1; z++) {
			if (press && !qvevri && c != Culture.BYZANTINE) {
				b.set(ix1, 1, z, st(Blocks.DECORATED_POT));
				b.set(ix0, 1, z, z % 2 == 0 ? st(Blocks.DECORATED_POT) : barrel(Direction.UP));
			} else {
				b.set(ix1, 1, z, barrel(Direction.WEST));
				b.set(ix1, 2, z, barrel(Direction.WEST));
				b.set(ix0, 1, z, barrel(Direction.EAST));
				if (z % 2 == 0) {
					b.set(ix0, 2, z, barrel(Direction.EAST));
				}
			}
		}
		int cz = (iz0 + iz1) / 2 + 1;
		if (qvevri) {
			// Qvevri: great jars sunk in the floor up to their necks.
			for (int x = ix0 + 2; x <= ix1 - 2; x += 2) {
				for (int z = cz - 1; z <= iz1 - 1; z += 2) {
					b.set(x, 0, z, st(Blocks.DECORATED_POT));
					b.set(x, -1, z, Blocks.CLAY.defaultBlockState());
				}
			}
		} else if (cz + 1 <= iz1 - 1 && mx + 2 <= ix1 - 1) {
			// A cellar under the floor, reached through a hatch.
			int cx0 = mx, cx1 = mx + 2;
			b.fill(cx0, -4, cz, cx1, -4, cz + 1, Blocks.COBBLESTONE.defaultBlockState());
			b.fill(cx0, -3, cz, cx1, -1, cz + 1, AIR);
			for (int y = -3; y <= -1; y++) {
				b.set(cx0, y, cz, Bp.facing(Blocks.LADDER, Direction.EAST));
			}
			b.set(cx0, 0, cz, Bp.trapdoor(p.trapdoor, Direction.NORTH, true, false));
			for (int x = cx0 + 1; x <= cx1; x++) {
				b.set(x, -3, cz + 1, barrel(Direction.NORTH));
				b.set(x, -2, cz + 1, barrel(Direction.NORTH));
			}
			b.set(cx1, -3, cz, barrel(Direction.UP));
			b.set(cx0 + 1, -1, cz, Bp.lantern(true));
		}
		hang(b, p, mx, h, Math.max(1, cz - 1));
		furnish(b, k, ix0, iz0, ix1, iz1, Interior.Use.WORKSHOP);

		// The yard to the street: hops, hives, or jars and a cart.
		int z = Math.min(D - 2, hz1 + 2);
		for (int x = x0 + 1; x <= x1 - 1; x += 2) {
			if (x >= mx - 2 && x <= mx + 1) {
				continue;
			}
			if (k.south) {
				b.set(x, 1, z, b.r.nextBoolean() ? st(Blocks.DECORATED_POT) : barrel(Direction.UP));
			} else if (mead && x > mx) {
				b.set(x, 1, z, Bp.log(p.log, Direction.Axis.Y));
				b.set(x, 2, z, Bp.facing(Blocks.BEEHIVE, Direction.SOUTH));
			} else {
				// Hop poles with the bines.
				b.set(x, 1, z, p.fence.defaultBlockState());
				b.set(x, 2, z, p.fence.defaultBlockState());
				b.set(x, 3, z, Blocks.AZALEA_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
			}
		}
		for (int zz = hz1 + 1; zz <= D - 1; zz++) {
			b.soft(mx - 1, 0, zz, k.path);
			b.soft(mx, 0, zz, k.path);
		}
		yard(b, k);
	}

	// ---------------------------------------------------------------- 6. weaver

	/** A weaver's and dyer's house: looms, wool of many colours, dye vats and cloth drying on lines. */
	static void weaver(Bp b, Palette p, Culture c) {
		Look k = look(b, p, c);
		int W = b.w, D = b.d, x0 = 0, x1 = W - 1, mx = W / 2;
		DyeColor[] dyes = {DyeColor.RED, DyeColor.BLUE, DyeColor.YELLOW, DyeColor.GREEN, DyeColor.BROWN, DyeColor.WHITE,
				DyeColor.LIGHT_BLUE, DyeColor.ORANGE,
				c == Culture.BYZANTINE || c == Culture.PERSIAN || c == Culture.ARAB ? DyeColor.PURPLE : DyeColor.BLACK};
		boolean front = b.r.nextBoolean();
		int hd = Math.max(5, Math.min(7, D - 4));
		int hz0 = front ? D - hd : 0, hz1 = front ? D - 1 : hd - 1;
		int yz0 = front ? 0 : hd, yz1 = front ? D - hd - 1 : D - 1;
		int h = 3;
		box(b, p, k, x0, hz0, x1, hz1, h, p.floor);
		roof(b, p, k, x0, hz0, x1, hz1, h + 1, true);
		door(b, p, k, mx, hz1, Direction.SOUTH);
		if (front) {
			door(b, p, k, mx, hz0, Direction.NORTH);
		}
		for (int x : new int[]{mx - 2, mx + 2}) {
			if (x > x0 && x < x1) {
				window(b, p, k, x, 2, hz1, Direction.SOUTH);
			}
		}
		int ix0 = x0 + 1, ix1 = x1 - 1, iz0 = hz0 + 1, iz1 = hz1 - 1;
		// Looms along the back wall with a seat before each.
		for (int x = ix0; x <= ix1 - 2; x += 2) {
			if (front && x == mx) {
				continue;
			}
			b.set(x, 1, iz0, Bp.facing(Blocks.LOOM, Direction.SOUTH));
			if (iz0 + 1 < iz1) {
				b.set(x, 1, iz0 + 1, Bp.stairs(k.wstairs, Direction.SOUTH, false));
			}
		}
		// Bales of dyed wool in the corner, the bed under a shelf of yarn.
		b.set(ix1, 1, iz0, Blocks.WOOL.pick(dyes[b.r.nextInt(dyes.length)]).defaultBlockState());
		b.set(ix1, 2, iz0, Blocks.WOOL.pick(dyes[b.r.nextInt(dyes.length)]).defaultBlockState());
		b.bed(p.bed, ix1, 1, iz1, Direction.NORTH);
		for (int z = iz0 + 1; z <= iz1; z++) {
			b.set(ix1, 2, z, Bp.facing(k.shelf, Direction.WEST));
		}
		hang(b, p, mx, h, (iz0 + iz1) / 2);
		furnish(b, k, ix0, iz0, ix1, iz1, Interior.Use.WORKSHOP);
		for (int x = ix0; x <= ix1; x++) {
			for (int z = iz0; z <= iz1; z++) {
				if (b.r.nextInt(4) == 0) {
					b.soft(x, 1, z, Blocks.CARPET.pick(dyes[b.r.nextInt(dyes.length)]).defaultBlockState());
				}
			}
		}

		// The yard: dye vats (one over a fire), barrels of woad and madder, cloth on lines.
		int yd = yz1 - yz0 + 1;
		int vz = yd >= 3 ? yz0 + 1 : yz0;
		boolean fire = false;
		for (int x = x0 + 1; x <= x1 - 1; x++) {
			if (x == mx) {
				continue;
			}
			if ((x - x0) % 2 == 1) {
				if (!fire) {
					b.set(x, 0, vz, Blocks.CAMPFIRE.defaultBlockState());
					fire = true;
				}
				cauldron(b, x, 1, vz);
			} else if (b.r.nextBoolean()) {
				b.set(x, 1, vz, barrel(Direction.UP));
			}
		}
		for (int z = vz + 2; z <= yz1; z += 2) {
			if (b.r.nextBoolean()) {
				// A line between two posts with cloth hanging from it.
				for (int y = 1; y <= 2; y++) {
					b.set(x0 + 1, y, z, p.fence.defaultBlockState());
					b.set(x1 - 1, y, z, p.fence.defaultBlockState());
				}
				for (int x = x0 + 1; x <= x1 - 1; x++) {
					b.set(x, 3, z, Bp.log(p.strippedLog, Direction.Axis.X));
					if (x > x0 + 1 && x < x1 - 1 && x != mx && b.r.nextInt(4) != 0) {
						b.set(x, 2, z, Blocks.WOOL.pick(dyes[b.r.nextInt(dyes.length)]).defaultBlockState());
					}
				}
			} else {
				// Cloth laid over a low rail.
				for (int x = x0 + 1; x <= x1 - 1; x++) {
					if (x != mx) {
						b.set(x, 1, z, p.fence.defaultBlockState());
						b.set(x, 2, z, Blocks.CARPET.pick(dyes[b.r.nextInt(dyes.length)]).defaultBlockState());
					}
				}
			}
		}
		// A patch of dye plants.
		for (int z = yz0; z <= yz1; z++) {
			if (!b.has(x0, 1, z) && b.r.nextBoolean()) {
				b.set(x0, 0, z, Blocks.GRASS_BLOCK.defaultBlockState());
				b.set(x0, 1, z, b.r.nextBoolean() ? Blocks.CORNFLOWER.defaultBlockState() : Blocks.DANDELION.defaultBlockState());
			}
		}
		for (int z = yz0; z <= yz1; z++) {
			b.soft(mx, 0, z, k.path);
		}
		yard(b, k);
	}

	// ---------------------------------------------------------------- 7. carpenter

	/** A carpenter's, cooper's and wheelwright's yard: an open shed, a sawpit, log piles, planks, barrels and wheels. */
	static void carpenter(Bp b, Palette p, Culture c) {
		Look k = look(b, p, c);
		int W = b.w, D = b.d, x0 = 0, x1 = W - 1, mx = W / 2;
		int sz1 = Math.max(4, Math.min(6, D / 2 - 1));
		box(b, p, k, x0, 0, x1, sz1, 3, Blocks.COARSE_DIRT.defaultBlockState());
		roof(b, p, k, x0, 0, x1, sz1, 4, true);
		boolean room = W >= 10;
		int split = room ? x1 - 4 : x1;
		openFront(b, p, x0, split, sz1, 1, 2, 3);
		if (room) {
			for (int z = 1; z < sz1; z++) {
				for (int y = 1; y <= 3; y++) {
					b.set(split, y, z, p.planks.defaultBlockState());
				}
			}
			b.fill(split + 1, 0, 1, x1 - 1, 0, sz1 - 1, p.floor);
			door(b, p, k, split + 2, sz1, Direction.SOUTH);
			b.bed(p.bed, x1 - 1, 1, sz1 - 1, Direction.NORTH);
			hang(b, p, split + 2, 3, 2);
			furnish(b, k, split + 1, 1, x1 - 1, sz1 - 1, Interior.Use.LIVING);
		}
		// Benches and tools in the shed.
		b.set(x0 + 1, 1, 1, Blocks.CRAFTING_TABLE.defaultBlockState());
		if (x0 + 2 < split) {
			b.set(x0 + 2, 1, 1, Blocks.FLETCHING_TABLE.defaultBlockState());
		}
		if (x0 + 3 < split) {
			b.set(x0 + 3, 1, 1, Blocks.GRINDSTONE.defaultBlockState());
		}
		for (int x = x0 + 1; x < split; x++) {
			if (b.r.nextBoolean()) {
				// Wheels and hoops hanging on the back wall.
				b.set(x, 2, 1, Bp.trapdoor(p.trapdoor, Direction.SOUTH, false, true));
			}
		}
		for (int x = x0 + 4; x < split; x++) {
			int t = b.r.nextInt(4);
			b.set(x, 1, 1, t == 0 ? st(Blocks.COMPOSTER) : t == 1 ? barrel(Direction.UP) : t == 2 ? p.planks.defaultBlockState() : Bp.slab(k.wslab, false));
		}
		for (int y = 1; y <= 2; y++) {
			b.set(x0 + 1, y, sz1 - 1, p.planks.defaultBlockState());
		}
		b.set(x0 + 1, 3, sz1 - 1, Bp.slab(k.wslab, false));
		if (sz1 - 2 > 1) {
			b.set(x0 + 1, 1, sz1 - 2, Bp.log(p.strippedLog, Direction.Axis.X));
		}
		hang(b, p, (x0 + split) / 2, 3, Math.max(1, sz1 / 2));

		// The sawpit with a log laid over it on trestles.
		int pz = sz1 + 2;
		int px0 = x0 + 1, px1 = Math.min(x0 + 4, mx - 2);
		if (pz + 1 <= D - 1 && px1 >= px0) {
			for (int x = px0 - 1; x <= px1 + 1; x++) {
				for (int z = pz - 1; z <= pz + 1; z++) {
					boolean pit = x >= px0 && x <= px1 && z == pz;
					b.set(x, -2, z, st(Blocks.COARSE_DIRT));
					if (pit) {
						b.air(x, -1, z);
						b.air(x, 0, z);
					} else {
						b.set(x, -1, z, st(Blocks.DIRT));
						b.set(x, 0, z, p.planks.defaultBlockState());
					}
				}
				b.set(x, 1, pz, Bp.log(p.log, Direction.Axis.X));
			}
		}
		// Log piles, planks and barrels in the yard.
		if (mx + 2 <= x1 - 1 && sz1 + 3 <= D - 2) {
			logs(b, p.log, mx + 2, sz1 + 2, x1 - 1, sz1 + 3, 2, Direction.Axis.X);
		}
		if (D - 2 > sz1 + 4) {
			for (int y = 1; y <= 2; y++) {
				b.soft(x1 - 1, y, D - 2, p.planks.defaultBlockState());
			}
			b.soft(x1 - 1, 3, D - 2, Bp.slab(k.wslab, false));
			b.soft(x1 - 2, 1, D - 2, barrel(Direction.UP));
			b.soft(x0 + 1, 1, D - 2, Bp.log(p.strippedLog, Direction.Axis.X));
			b.soft(x0 + 2, 1, D - 2, st(Blocks.COMPOSTER));
		}
		for (int i = 0; i < 4 + W / 3; i++) {
			int x = x0 + b.r.nextInt(W), z = sz1 + 1 + b.r.nextInt(Math.max(1, D - sz1 - 1));
			if (x != mx && !b.has(x, 0, z) && !b.has(x, 1, z)) {
				b.set(x, 1, z, Bp.slab(Blocks.BIRCH_SLAB, false));
			}
		}
		for (int z = sz1 + 1; z <= D - 1; z++) {
			b.soft(mx, 0, z, k.path);
		}
		ground(b, Blocks.COARSE_DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), 3);
	}

	// ---------------------------------------------------------------- 8. bloomery

	/** A clay shaft furnace: a 3×3 tower of packed mud tapering up, the tapping hole in front, fire in the top. */
	static void shaftFurnace(Bp b, int fx, int fz, int H, Direction bellows) {
		BlockState body = b.r.nextBoolean() ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.MUD_BRICKS.defaultBlockState();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				int x = fx + dx, z = fz + dz;
				boolean edge = dx == 0 || dz == 0;
				b.set(x, -1, z, body);
				b.set(x, 0, z, body);
				if (dx == 0 && dz == 0) {
					for (int y = 1; y < H; y++) {
						b.air(x, y, z);
					}
					b.set(x, H, z, Blocks.CAMPFIRE.defaultBlockState());
					continue;
				}
				for (int y = 1; y <= 2; y++) {
					b.set(x, y, z, body);
				}
				if (edge) {
					for (int y = 3; y <= H; y++) {
						b.set(x, y, z, body);
					}
				} else {
					b.set(x, 3, z, Bp.slab(Blocks.MUD_BRICK_SLAB, false));
				}
			}
		}
		b.set(fx, 1, fz + 1, lit(Blocks.BLAST_FURNACE, Direction.SOUTH));
		// Bellows: a barrel blowing into the side, a trapdoor lid.
		int bx = fx + bellows.getStepX() * 2, bz = fz + bellows.getStepZ() * 2;
		b.set(bx, 1, bz, barrel(bellows.getOpposite()));
		b.set(bx, 2, bz, Bp.trapdoor(Blocks.SPRUCE_TRAPDOOR, bellows.getOpposite(), false, false));
	}

	/** Iron smelting: one or two shaft furnaces with bellows, charcoal, bog ore and slag, a shed with the anvil. */
	static void bloomery(Bp b, Palette p, Culture c) {
		Look k = look(b, p, c);
		int W = b.w, D = b.d, x0 = 0, x1 = W - 1, mx = W / 2;
		int sz1 = Math.max(3, Math.min(4, D / 3));
		box(b, p, k, x0, 0, x1, sz1, 3, Blocks.COBBLESTONE.defaultBlockState());
		int ridge = roof(b, p, k, x0, 0, x1, sz1, 4, true);
		openFront(b, p, x0, x1, sz1, 1, 2, 3);
		// The smith's hearth with its chimney, anvil, quenching tub, grindstone and chest.
		b.set(x0 + 1, 1, 1, Blocks.CAMPFIRE.defaultBlockState());
		chimney(b, Blocks.COBBLESTONE.defaultBlockState(), x0 + 1, 0, 1, ridge + 1);
		b.set(mx, 1, Math.min(2, sz1 - 1), Bp.facing(Blocks.ANVIL, Direction.EAST));
		cauldron(b, mx + 1, 1, 1);
		b.set(x1 - 1, 1, 1, Blocks.CHEST.defaultBlockState());
		if (x1 - 2 > mx + 1) {
			b.set(x1 - 2, 1, 1, Blocks.GRINDSTONE.defaultBlockState());
		}
		b.set(x0 + 2, 1, 1, Blocks.RAW_IRON_BLOCK.defaultBlockState());
		hang(b, p, mx, 3, Math.max(1, sz1 / 2));
		furnish(b, k, x0 + 1, 1, x1 - 1, sz1 - 1, Interior.Use.WORKSHOP);

		// Furnaces in the yard.
		int yd = D - sz1 - 1;
		int fz = sz1 + 1 + yd / 2;
		if (fz + 1 > D - 1) {
			fz = D - 2;
		}
		int H = 3 + b.r.nextInt(2);
		shaftFurnace(b, x0 + 2, fz, H, Direction.WEST);
		if (W >= 11) {
			shaftFurnace(b, x1 - 2, fz, 3 + b.r.nextInt(2), Direction.EAST);
		}
		// Heaps: charcoal, bog ore, slag.
		int hz = sz1 + 1;
		BlockState coal = Blocks.COAL_BLOCK.defaultBlockState();
		b.soft(mx + 1, 1, hz, coal);
		b.soft(mx + 2, 1, hz, coal);
		b.soft(mx + 1, 1, hz + 1, coal);
		b.soft(mx + 1, 2, hz, coal);
		b.soft(mx - 1, 1, hz, Blocks.RAW_IRON_BLOCK.defaultBlockState());
		b.soft(mx - 2, 1, hz, Blocks.IRON_ORE.defaultBlockState());
		b.soft(mx - 1, 1, hz + 1, Blocks.GRAVEL.defaultBlockState());
		for (int i = 0; i < 6; i++) {
			int x = x0 + b.r.nextInt(W), z = D - 1 - b.r.nextInt(2);
			if (x != mx && !b.has(x, 1, z)) {
				int t = b.r.nextInt(4);
				b.set(x, 1, z, t == 0 ? Blocks.BLACKSTONE.defaultBlockState() : t == 1 ? Bp.log(Blocks.BASALT, Direction.Axis.Y)
						: t == 2 ? Bp.slab(Blocks.BLACKSTONE_SLAB, false) : Blocks.SMOOTH_BASALT.defaultBlockState());
			}
		}
		for (int z = sz1 + 1; z <= D - 1; z++) {
			b.soft(mx, 0, z, k.path);
		}
		ground(b, Blocks.COARSE_DIRT.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), 3);
	}

	// ---------------------------------------------------------------- 9. kiln

	/** A lime or brick kiln: a round tower with a fire mouth and smoke, stacks of bricks or lime stone around. */
	static void kiln(Bp b, Palette p, Culture c) {
		Look k = look(b, p, c);
		int W = b.w, D = b.d, x0 = 0, x1 = W - 1, mx = W / 2;
		boolean lime = !k.south && b.r.nextBoolean();
		BlockState wallA, wallB, charge;
		if (lime) {
			wallA = Blocks.COBBLESTONE.defaultBlockState();
			wallB = b.r.nextBoolean() ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
			charge = Blocks.CALCITE.defaultBlockState();
		} else {
			wallA = k.south ? Blocks.MUD_BRICKS.defaultBlockState() : Blocks.BRICKS.defaultBlockState();
			wallB = k.south ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.MUD_BRICKS.defaultBlockState();
			charge = Blocks.BRICKS.defaultBlockState();
		}
		double rad = W >= 9 && D >= 9 ? 2.5 : 1.8;
		int ro = rad > 2 ? 2 : 1;
		int cx = mx, cz = Math.max(ro + 1, (D - 1) / 2 - 1);
		int H = 4 + b.r.nextInt(2);
		for (int dx = -ro; dx <= ro; dx++) {
			for (int dz = -ro; dz <= ro; dz++) {
				double dd = Math.sqrt(dx * dx + dz * dz);
				if (dd > rad) {
					continue;
				}
				int x = cx + dx, z = cz + dz;
				boolean ring = dd > rad - 1.2;
				b.set(x, -1, z, wallA);
				b.set(x, 0, z, wallA);
				for (int y = 1; y <= H; y++) {
					if (ring) {
						b.set(x, y, z, b.r.nextInt(4) == 0 ? wallB : wallA);
					} else if (y == 1) {
						b.set(x, y, z, dx == 0 && dz == 0 ? Blocks.CAMPFIRE.defaultBlockState() : AIR);
					} else if (y < H) {
						b.set(x, y, z, charge);
					} else {
						b.set(x, y, z, dx == 0 && dz == 0 ? Blocks.CAMPFIRE.defaultBlockState() : AIR);
					}
				}
			}
		}
		if (ro == 1) {
			b.set(cx, H, cz, Blocks.CAMPFIRE.defaultBlockState());
		}
		b.set(cx, 1, cz + ro, lit(Blocks.FURNACE, Direction.SOUTH));
		for (int y = 1; y <= H; y++) {
			b.set(cx + ro + 1, y, cz, Bp.facing(Blocks.LADDER, Direction.EAST));
		}
		// Stacks in front: bricks, or lime stone and the burnt lime.
		int sz0 = cz + ro + 2;
		for (int z = sz0; z <= D - 1; z++) {
			for (int x = x0; x <= x1; x++) {
				if (Math.abs(x - mx) <= 0 || (x - x0) % 3 == 2 || (z - sz0) % 2 == 1 || b.r.nextInt(5) == 0) {
					continue;
				}
				if (lime) {
					b.set(x, 1, z, b.r.nextInt(3) == 0 ? Blocks.DIORITE.defaultBlockState() : Blocks.CALCITE.defaultBlockState());
					if (b.r.nextBoolean()) {
						b.set(x, 2, z, b.r.nextBoolean() ? st(Blocks.POWDER_SNOW_CAULDRON) : Blocks.CALCITE.defaultBlockState());
					}
				} else {
					b.set(x, 1, z, charge);
					b.set(x, 2, z, b.r.nextBoolean() ? charge : Bp.slab(k.south ? Blocks.MUD_BRICK_SLAB : Blocks.BRICK_SLAB, false));
				}
			}
		}
		// Fuel behind, and for bricks a clay pit beside the kiln.
		if (cz - ro - 1 >= 0 && cx - ro - 1 >= x0) {
			logs(b, p.log, x0, 0, cx - ro - 1, Math.max(0, cz - ro - 1), 2, Direction.Axis.X);
		}
		if (!lime && cx + ro + 2 <= x1) {
			for (int x = cx + ro + 2; x <= x1; x++) {
				for (int z = 0; z <= Math.min(1, cz - 1); z++) {
					b.set(x, -1, z, st(Blocks.CLAY));
					b.set(x, 0, z, b.r.nextInt(3) == 0 ? st(Blocks.MUD) : st(Blocks.CLAY));
				}
			}
		}
		for (int z = cz + ro + 1; z <= D - 1; z++) {
			b.soft(mx, 0, z, k.path);
		}
		ground(b, Blocks.COARSE_DIRT.defaultBlockState(), Blocks.GRAVEL.defaultBlockState(), 3);
	}

	// ---------------------------------------------------------------- 10. charcoal

	/** A charcoal mound: earth over stacked logs, smoking at the top and a vent; or an opened one full of charcoal. */
	static void mound(Bp b, Palette p, int cx, int cz, double R, boolean burning) {
		int ri = (int) Math.ceil(R);
		for (int dx = -ri; dx <= ri; dx++) {
			for (int dz = -ri; dz <= ri; dz++) {
				double hd = Math.sqrt(dx * dx + dz * dz);
				if (hd > R) {
					continue;
				}
				int top = (int) Math.round(Math.sqrt(Math.max(0, R * R - hd * hd)));
				if (!burning) {
					top = Math.max(1, top - 1);
				}
				int x = cx + dx, z = cz + dz;
				b.set(x, 0, z, Blocks.COARSE_DIRT.defaultBlockState());
				for (int y = 1; y <= top; y++) {
					boolean skin = y == top || hd > R - 1.2;
					BlockState s;
					if (burning && skin) {
						int t = b.r.nextInt(4);
						s = t == 0 ? Blocks.PODZOL.defaultBlockState() : t == 1 ? Blocks.DIRT.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
					} else if (burning) {
						s = Bp.log(p.log, Direction.Axis.Y);
					} else {
						s = b.r.nextInt(3) == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.COAL_BLOCK.defaultBlockState();
					}
					b.set(x, y, z, s);
				}
			}
		}
		if (burning) {
			int top = (int) Math.round(R);
			b.set(cx, top + 1, cz, Blocks.CAMPFIRE.defaultBlockState());
			if (R >= 2) {
				b.set(cx, 1, cz + 2, Blocks.CAMPFIRE.defaultBlockState());
			}
		}
	}

	/** A charcoal burner's place: one or two mounds, stacked logs, the burner's hut, ash and spilt charcoal. */
	static void charcoal(Bp b, Palette p, Culture c) {
		Look k = look(b, p, c);
		int W = b.w, D = b.d, x0 = 0, x1 = W - 1, mx = W / 2;
		boolean two = W >= 12 && D >= 12;
		double R = W >= 13 && D >= 13 ? 3.0 : 2.3;
		// The hut in the back right corner.
		int hx0 = x1 - 4, hz1 = 3;
		k.wealth = 0;
		box(b, p, k, hx0, 0, x1, hz1, 2, Blocks.PACKED_MUD.defaultBlockState());
		roof(b, p, k, hx0, 0, x1, hz1, 3, true);
		door(b, p, k, x1 - 2, hz1, Direction.SOUTH);
		b.bed(p.bed, x1 - 1, 1, 2, Direction.NORTH);
		b.set(hx0 + 1, 1, 1, Blocks.CHEST.defaultBlockState());
		furnish(b, k, hx0 + 1, 1, x1 - 1, hz1 - 1, Interior.Use.LIVING);
		b.soft(x1 - 3, 1, hz1 + 1, barrel(Direction.UP));
		// Mounds.
		mound(b, p, x0 + 3, two ? D - 4 : D - 3, R, true);
		if (two) {
			mound(b, p, x0 + 3, 3, R - 0.3, b.r.nextBoolean());
		}
		// Stacked logs waiting for the next mound.
		int lz0 = hz1 + 2, lz1 = Math.min(D - 3, lz0 + 3);
		int lx0 = Math.max(x1 - 3, x0 + 6);
		if (lx0 > mx && lx0 <= x1 - 1 && lz1 >= lz0) {
			logs(b, p.log, lx0, lz0, x1 - 1, lz1, 2, Direction.Axis.Z);
		}
		// Ash and spilt charcoal.
		for (int i = 0; i < 6 + W / 2; i++) {
			int x = x0 + b.r.nextInt(W), z = b.r.nextInt(D);
			if (!b.has(x, 0, z) && !b.has(x, 1, z)) {
				int t = b.r.nextInt(4);
				if (t == 0) {
					b.set(x, 0, z, Blocks.GRAVEL.defaultBlockState());
				} else if (t == 1) {
					b.set(x, 1, z, Blocks.COAL_BLOCK.defaultBlockState());
				} else {
					b.set(x, 1, z, Blocks.CARPET.pick(t == 2 ? DyeColor.GRAY : DyeColor.LIGHT_GRAY).defaultBlockState());
				}
			}
		}
		for (int z = hz1 + 1; z <= D - 1; z++) {
			b.soft(mx, 0, z, Blocks.DIRT_PATH.defaultBlockState());
		}
		for (int x = mx; x <= x1 - 2; x++) {
			b.soft(x, 0, hz1 + 1, Blocks.DIRT_PATH.defaultBlockState());
		}
		ground(b, Blocks.COARSE_DIRT.defaultBlockState(), Blocks.PODZOL.defaultBlockState(), 3);
	}

	// ---------------------------------------------------------------- 11. apiary

	/** A bee yard: hives on stumps, a hive bench or hollow-log hives, flowers, a wattle fence and a honey shed. */
	static void apiary(Bp b, Palette p, Culture c) {
		Look k = look(b, p, c);
		int W = b.w, D = b.d, x0 = 0, x1 = W - 1, mx = W / 2;
		boolean wattle = c == Culture.RUS || c == Culture.BALT_FINN || c == Culture.WEST_SLAV || c == Culture.MAGYAR || c == Culture.CELTIC;
		BlockState fence = wattle && b.r.nextBoolean() ? Blocks.MANGROVE_ROOTS.defaultBlockState() : p.fence.defaultBlockState();
		// The honey shed in the back corner.
		int sx1 = Math.max(x0 + 3, Math.min(x0 + 4, x1 - 2)), sz1 = 3;
		box(b, p, k, x0, 0, sx1, sz1, 2, p.floor);
		roof(b, p, k, x0, 0, sx1, sz1, 3, true);
		door(b, p, k, sx1 - 1, sz1, Direction.SOUTH);
		b.set(x0 + 1, 1, 1, barrel(Direction.UP));
		b.set(x0 + 1, 2, 1, Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, 3).setValue(BlockStateProperties.LIT, true));
		b.set(x0 + 1, 1, 2, Blocks.CHEST.defaultBlockState());
		if (sx1 - 1 > x0 + 2) {
			b.set(x0 + 2, 1, 1, Blocks.HONEYCOMB_BLOCK.defaultBlockState());
		}
		furnish(b, k, x0 + 1, 1, sx1 - 1, sz1 - 1, Interior.Use.STORE);
		// Hives.
		int v = b.r.nextInt(3);
		if (v == 2 && !(k.north || wattle) || v == 1 && D < 9) {
			v = 0;
		}
		int hz0 = Math.min(sz1 + 2, D - 2);
		if (v == 1) {
			// A long hive bench under a lean-to roof.
			int z = hz0;
			for (int x = x0 + 1; x <= x1 - 1; x++) {
				if (x == mx) {
					continue;
				}
				b.set(x, 1, z, Bp.slab(k.wslab, true));
				if ((x - x0) % 2 == 1) {
					b.set(x, 2, z, Bp.facing(Blocks.BEEHIVE, Direction.SOUTH));
				}
				b.set(x, 4, z, Bp.slab(k.wslab, false));
				b.set(x, 4, z - 1, Bp.slab(k.wslab, false));
			}
			for (int y = 1; y <= 3; y++) {
				b.set(x0 + 1, y, z - 1, p.fence.defaultBlockState());
				b.set(x1 - 1, y, z - 1, p.fence.defaultBlockState());
			}
			hz0 += 2;
		}
		for (int z = hz0; z <= D - 2; z += 2) {
			for (int x = x0 + 1; x <= x1 - 1; x += 2) {
				if (x == mx || b.has(x, 1, z)) {
					continue;
				}
				if (v == 2) {
					// A hollow-log hive (борть-колода) standing on end.
					b.set(x, 1, z, Bp.log(p.log, Direction.Axis.Y));
					b.set(x, 2, z, Bp.log(p.log, Direction.Axis.Y));
					b.set(x, 3, z, Bp.facing(Blocks.BEE_NEST, Direction.SOUTH));
				} else {
					b.set(x, 1, z, Bp.log(p.strippedLog, Direction.Axis.Y));
					b.set(x, 2, z, Bp.facing(b.r.nextInt(4) == 0 ? Blocks.BEE_NEST : Blocks.BEEHIVE, Direction.SOUTH));
				}
			}
		}
		// Hives beside the shed too.
		for (int x = sx1 + 2; x <= x1 - 1; x += 2) {
			b.soft(x, 1, 1, Bp.log(p.strippedLog, Direction.Axis.Y));
			b.soft(x, 2, 1, Bp.facing(Blocks.BEEHIVE, Direction.SOUTH));
		}
		// Path, flowers, the fence.
		for (int z = sz1 + 1; z <= D - 1; z++) {
			b.set(mx, 0, z, k.path);
		}
		for (int x = x0 + 1; x <= x1 - 1; x++) {
			for (int z = 1; z <= D - 2; z++) {
				if (!b.has(x, 0, z) && !b.has(x, 1, z) && b.r.nextInt(3) != 0) {
					b.set(x, 0, z, Blocks.GRASS_BLOCK.defaultBlockState());
					b.set(x, 1, z, flower(b.r));
				}
			}
		}
		for (int x = x0; x <= x1; x++) {
			b.soft(x, 1, 0, fence);
			b.soft(x, 1, D - 1, x == mx ? Bp.facing(p.gate, Direction.SOUTH) : fence);
		}
		for (int z = 0; z < D; z++) {
			b.soft(x0, 1, z, fence);
			b.soft(x1, 1, z, fence);
		}
		ground(b, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(), 6);
	}

	// ---------------------------------------------------------------- 12. stable

	/** A stable: stalls between fences, hay and water troughs, a hay loft, a horse yard with a cart in front. */
	static void stable(Bp b, Palette p, Culture c) {
		Look k = look(b, p, c);
		int W = b.w, D = b.d, x0 = 0, x1 = W - 1, mx = W / 2;
		int sz1 = Math.max(6, D - 6);
		boolean open = k.south || b.r.nextInt(3) == 0;
		int h = open ? 3 : 5;
		box(b, p, k, x0, 0, x1, sz1, h, Blocks.COARSE_DIRT.defaultBlockState());
		roof(b, p, k, x0, 0, x1, sz1, h + 1, true);
		if (open) {
			openFront(b, p, x0, x1, sz1, 1, 2, 3);
		} else {
			doubleDoor(b, p, mx - 1, sz1);
			for (int x = x0 + 2; x <= x1 - 2; x += 3) {
				if (x < mx - 2 || x > mx + 1) {
					window(b, p, k, x, 2, sz1, Direction.SOUTH);
				}
			}
		}
		// Stalls along the back wall: dividers, hay racks and water.
		int sd = Math.min(3, sz1 - 2);
		for (int x = x0 + 1; x <= x1 - 1; x++) {
			boolean divider = (x - x0) % 3 == 0;
			for (int z = 1; z <= sd; z++) {
				if (divider) {
					b.set(x, 1, z, p.fence.defaultBlockState());
				} else if (b.r.nextInt(4) == 0) {
					b.set(x, 1, z, Blocks.CARPET.pick(DyeColor.YELLOW).defaultBlockState());
				}
			}
			if (!divider) {
				if ((x - x0) % 3 == 1) {
					b.set(x, 1, 1, Blocks.HAY_BLOCK.defaultBlockState());
				} else {
					cauldron(b, x, 1, 1);
				}
			}
		}
		// A second row of stalls along the front wall in a long stable.
		if (sz1 - 1 >= 9) {
			for (int x = x0 + 3; x <= x1 - 1; x += 3) {
				if (Math.abs(x - mx) <= 2) {
					continue;
				}
				for (int z = sz1 - 3; z <= sz1 - 1; z++) {
					b.set(x, 1, z, p.fence.defaultBlockState());
				}
			}
			for (int x = x0 + 1; x <= x1 - 1; x++) {
				if (Math.abs(x - mx) > 2 && (x - x0) % 3 == 1) {
					b.set(x, 1, sz1 - 1, Blocks.HAY_BLOCK.defaultBlockState());
				}
			}
		}
		if (!open) {
			// The hay loft over the stalls, a ladder up, a bed for the groom.
			int lz = Math.min(4, sz1 - 2);
			b.fill(x0 + 1, 4, 1, x1 - 1, 4, lz, p.planks.defaultBlockState());
			for (int x = x0 + 1; x <= x1 - 1; x++) {
				for (int z = 1; z <= lz; z++) {
					if (x > x0 + 2 && b.r.nextInt(3) != 0) {
						b.set(x, 5, z, Blocks.HAY_BLOCK.defaultBlockState());
					}
				}
			}
			for (int y = 1; y <= 5; y++) {
				b.set(x1 - 1, y, lz + 1, Bp.facing(Blocks.LADDER, Direction.WEST));
			}
			if (lz >= 2) {
				b.bed(p.bed, x0 + 1, 5, lz, Direction.NORTH);
			}
			window(b, p, k, mx, 5, 0, Direction.NORTH);
			hang(b, p, mx, 3, Math.min(sz1 - 1, lz + 2));
		} else {
			b.soft(x0 + 1, 1, sz1 - 1, Blocks.HAY_BLOCK.defaultBlockState());
			b.soft(x0 + 1, 2, sz1 - 1, Blocks.HAY_BLOCK.defaultBlockState());
			b.soft(x1 - 1, 1, sz1 - 1, Blocks.CHEST.defaultBlockState());
		}
		b.soft(x1 - 1, 1, sd + 1, Blocks.CHEST.defaultBlockState());
		b.soft(x1 - 2, 1, sd + 1, barrel(Direction.UP));

		// The horse yard to the street.
		for (int z = sz1 + 1; z <= D - 1; z++) {
			b.set(x0, 1, z, p.fence.defaultBlockState());
			b.set(x1, 1, z, p.fence.defaultBlockState());
		}
		for (int x = x0; x <= x1; x++) {
			b.set(x, 1, D - 1, x == mx || x == mx - 1 ? Bp.facing(p.gate, Direction.SOUTH) : p.fence.defaultBlockState());
		}
		if (D - 2 >= sz1 + 2) {
			cauldron(b, x0 + 1, 1, D - 2);
			cauldron(b, x0 + 2, 1, D - 2);
			b.set(x0 + 1, 1, sz1 + 2, Blocks.HAY_BLOCK.defaultBlockState());
		}
		if (W >= 10 && D - 2 >= sz1 + 3) {
			cart(b, p, k, x1 - 3, sz1 + 2, true, b.r.nextBoolean() ? Blocks.HAY_BLOCK.defaultBlockState() : barrel(Direction.UP));
		}
		for (int x = x0 + 1; x <= x1 - 1; x++) {
			for (int z = sz1 + 1; z <= D - 1; z++) {
				if (!b.has(x, 0, z)) {
					int t = b.r.nextInt(5);
					b.set(x, 0, z, t == 0 ? Blocks.MUD.defaultBlockState() : t == 1 ? Blocks.DIRT_PATH.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
				}
			}
		}
		yard(b, k);
	}

	// ---------------------------------------------------------------- 13. warehouse

	/**
	 * A merchant's warehouse or guild store (a Rus gostiny dvor store, a Latin storehouse, a Byzantine horreum, a store
	 * of an Islamic funduq): a big solid building with doors to the street, rows of barrels, chests, bales and jars, a
	 * counter with a scale, an upper floor or loft and a hoist beam over the loft door.
	 */
	static void warehouse(Bp b, Palette p, Culture c) {
		Look k = look(b, p, c);
		int W = b.w, D = b.d, x0 = 0, x1 = W - 1, mx = W / 2;
		boolean rich = k.flat || k.wealth >= 2 || b.r.nextInt(3) == 0;
		boolean jars = k.south || k.s == Style.STONE || (c == Culture.LATIN && k.rs == Blocks.BRICK_STAIRS);
		int z1 = D - 2;
		int h = rich ? 8 : 4;
		box(b, p, k, x0, 0, x1, z1, h, p.floor);
		// Upper floor (or loft) at y = 4, carried on posts.
		b.fill(x0 + 1, 4, 1, x1 - 1, 4, z1 - 1, p.planks.defaultBlockState());
		for (int x : new int[]{x0 + 3, x1 - 3}) {
			for (int z = 4; z <= z1 - 4; z += 4) {
				if (x > x0 + 1 && x < x1 - 1 && Math.abs(x - mx) > 1) {
					for (int y = 1; y <= 3; y++) {
						b.set(x, y, z, Bp.log(p.log, Direction.Axis.Y));
					}
				}
			}
		}
		roof(b, p, k, x0, 0, x1, z1, h + 1, false);
		for (int y = 1; y <= 4; y++) {
			b.set(x1 - 1, y, 1, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}
		// Big doors to the street, windows.
		doubleDoor(b, p, mx - 1, z1);
		if (k.s == Style.MUD || k.s == Style.STONE || k.s == Style.RUBBLE) {
			Block arch = k.s == Style.MUD ? Blocks.SMOOTH_SANDSTONE_STAIRS : Blocks.STONE_BRICK_STAIRS;
			b.set(mx - 1, 3, z1, Bp.stairs(arch, Direction.SOUTH, true));
			b.set(mx, 3, z1, Bp.stairs(arch, Direction.SOUTH, true));
		}
		for (int x = x0 + 2; x <= x1 - 2; x += 3) {
			if (x < mx - 2 || x > mx + 1) {
				window(b, p, k, x, 2, z1, Direction.SOUTH);
				if (rich) {
					window(b, p, k, x, 6, z1, Direction.SOUTH);
				}
			}
		}
		// The loft door with the hoist beam and chain over it.
		b.air(mx, 5, z1);
		b.air(mx, 6, z1);
		int beamY = rich ? h : 7;
		b.set(mx, beamY, z1, Bp.log(p.log, Direction.Axis.Z));
		b.set(mx, beamY, z1 + 1, Bp.log(p.log, Direction.Axis.Z));
		for (int y = 6; y < beamY; y++) {
			b.set(mx, y, z1 + 1, Blocks.IRON_CHAIN.defaultBlockState());
		}
		if (rich) {
			b.set(mx, 4, z1 + 1, Bp.slab(k.wslab, true));
		}

		// Ground floor: barrels along the back wall, bales and chests along the sides, rows of barrels or jars.
		DyeColor[] bales = {DyeColor.WHITE, DyeColor.BROWN, DyeColor.LIGHT_GRAY, DyeColor.WHITE};
		for (int x = x0 + 1; x <= x1 - 2; x++) {
			b.set(x, 1, 1, jars && x % 2 == 0 ? st(Blocks.DECORATED_POT) : barrel(Direction.UP));
			if (b.r.nextBoolean()) {
				b.set(x, 2, 1, barrel(Direction.UP));
			}
		}
		for (int z = 2; z <= z1 - 3; z++) {
			for (int y = 1; y <= 2 + (z % 2); y++) {
				b.set(x0 + 1, y, z, Blocks.WOOL.pick(bales[b.r.nextInt(bales.length)]).defaultBlockState());
			}
			b.set(x1 - 1, 1, z, z % 3 == 0 ? Blocks.CHEST.defaultBlockState() : barrel(Direction.WEST));
			if (z % 3 != 0) {
				b.set(x1 - 1, 2, z, barrel(Direction.WEST));
			}
		}
		for (int x = x0 + 3; x <= x1 - 3; x += 2) {
			if (Math.abs(x - mx) <= 1 || Math.abs(x - mx) == 2 && x > mx) {
				continue;
			}
			for (int z = 3; z <= z1 - 4; z++) {
				if (b.has(x, 1, z)) {
					continue;
				}
				b.set(x, 1, z, jars && b.r.nextBoolean() ? st(Blocks.DECORATED_POT) : barrel(Direction.NORTH));
				if (!jars && b.r.nextBoolean()) {
					b.set(x, 2, z, barrel(Direction.NORTH));
				}
			}
		}
		// The counter with the ledger and a scale.
		int cz = z1 - 2;
		for (int x = x0 + 1; x <= Math.min(x0 + 3, mx - 3); x++) {
			b.set(x, 1, cz, Bp.slab(k.wslab, true));
		}
		if (x0 + 4 <= mx - 2) {
			b.set(x0 + 4, 1, cz, p.fence.defaultBlockState());
			b.set(x0 + 4, 2, cz, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.defaultBlockState());
		}
		b.set(x0 + 1, 1, z1 - 1, Bp.facing(Blocks.LECTERN, Direction.EAST));
		for (int z = 3; z < z1; z += 5) {
			b.set(mx, 3, z, Bp.lantern(true));
		}
		// Upstairs: more bales and chests (and the watchman's bed in the rich store), or a low loft under the roof.
		int uy = 5;
		for (int x = x0 + 1; x <= x1 - 3; x++) {
			b.set(x, uy, 1, Blocks.WOOL.pick(bales[b.r.nextInt(bales.length)]).defaultBlockState());
			if (rich && b.r.nextBoolean()) {
				b.set(x, uy + 1, 1, Blocks.WOOL.pick(bales[b.r.nextInt(bales.length)]).defaultBlockState());
			}
		}
		if (rich) {
			b.bed(p.bed, x0 + 1, uy, z1 - 1, Direction.NORTH);
			b.set(mx, h, (z1 + 1) / 2, Bp.lantern(true));
			Interior.room(b, c, x0 + 1, 1, x1 - 1, z1 - 1, uy, Interior.Use.STORE, k.wealth);
		} else {
			b.set(x0 + 1, uy, z1 - 1, Blocks.CHEST.defaultBlockState());
		}
		furnish(b, k, x0 + 1, 1, x1 - 1, z1 - 1, Interior.Use.STORE);
		// The loading strip by the street.
		for (int x = x0; x <= x1; x++) {
			b.soft(x, 0, D - 1, Math.abs(x - mx) <= 1 ? k.path : Blocks.COARSE_DIRT.defaultBlockState());
		}
		b.soft(x0 + 1, 1, D - 1, barrel(Direction.UP));
		b.soft(x1 - 1, 1, D - 1, Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
		yard(b, k);
	}

}
