package com.stasdoto.medieval.construct;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import com.stasdoto.medieval.earth.Climate;
import com.stasdoto.medieval.earth.Cover;
import com.stasdoto.medieval.earth.Noise;
import com.stasdoto.medieval.earth.Terrain;

/**
 * The little things of the land, all over the Earth: patchy ground (moss, podzol, coarse earth, gravel, worn paths),
 * boulders and stones, fallen trunks and stumps, mushrooms, ferns, berry bushes, flower meadows in drifts, and trees of
 * many shapes - spreading oaks with boughs, slender birches, spruce cones, tall pines, columns of poplar and cypress,
 * willows by the water, olives and dead snags.
 */
public final class Nature {
	private Nature() {
	}

	public interface World {
		BlockState get(int x, int y, int z);

		void set(int x, int y, int z, BlockState s);

		Terrain.Col col(int x, int z);

		boolean in(int x, int z);
	}

	private static BlockState leaves(Block b) {
		return b.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
	}

	private static boolean soil(BlockState s) {
		return s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.PODZOL) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.DIRT) || s.is(Blocks.MOSS_BLOCK)
				|| s.is(Blocks.ROOTED_DIRT);
	}

	private static boolean free(BlockState s) {
		return s.isAir() || s.canBeReplaced();
	}

	/** Decorates one chunk x0..x1 × z0..z1. */
	public static void decorate(World w, int x0, int z0, int x1, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				Terrain.Col c = w.col(x, z);
				if (c.waterTop != Terrain.NO_WATER || c.cover == Cover.ICE) {
					continue;
				}
				int t = c.top();
				BlockState g = w.get(x, t, z);
				if (!soil(g) || !free(w.get(x, t + 1, z))) {
					continue;
				}
				boolean wood = c.cover == Cover.WOOD;
				boolean dry = c.zone == Climate.DESERT || c.zone == Climate.STEPPE || c.zone == Climate.MEDITERRANEAN || c.zone == Climate.COLD_DESERT;
				boolean cold = c.zone == Climate.BOREAL || c.zone == Climate.TUNDRA;
				boolean nearWater = c.shore > -10 && c.shore < 0;
				// Patchy ground.
				double p = Noise.fbm(x / 9.0, z / 9.0, 401, 2);
				double q = Noise.rand(x, z, 402);
				if (g.is(Blocks.GRASS_BLOCK)) {
					if (wood && p > 0.28) {
						w.set(x, t, z, q < 0.5 ? Blocks.PODZOL.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState());
					} else if (p < -0.42) {
						w.set(x, t, z, dry ? Blocks.COARSE_DIRT.defaultBlockState() : q < 0.3 ? Blocks.ROOTED_DIRT.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
					} else if (p > 0.45 && !wood && q < 0.25) {
						w.set(x, t, z, Blocks.GRAVEL.defaultBlockState());
					}
				}
				double r = Noise.rand(x, z, 403);
				// Trees standing alone in the open land and more under the forest edge.
				double treeChance = wood ? 0.012 : dry ? 0.0015 : 0.0035;
				if (r < treeChance) {
					tree(w, x, t + 1, z, pickTree(c, wood, dry, cold, nearWater, x, z), x * 31 + z * 17);
					continue;
				}
				r -= treeChance;
				if (r < 0.0012) {
					boulder(w, x, t, z, (int) (q * 1000));
					continue;
				}
				r -= 0.0012;
				if (r < 0.004) {
					// A loose stone lying in the grass.
					Block[] st = {Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.ANDESITE, Blocks.STONE, Blocks.TUFF};
					w.set(x, t + 1, z, Bp.slab(q < 0.5 ? Blocks.COBBLESTONE_SLAB : Blocks.MOSSY_COBBLESTONE_SLAB, false));
					if (q > 0.85) {
						w.set(x, t + 1, z, st[(int) (q * 37) % st.length].defaultBlockState());
					}
					continue;
				}
				r -= 0.004;
				if (wood && r < 0.0015) {
					fallenLog(w, x, t + 1, z, cold ? Blocks.SPRUCE_LOG : q < 0.5 ? Blocks.OAK_LOG : Blocks.BIRCH_LOG, q < 0.5);
					continue;
				}
				r -= 0.0015;
				if (r < 0.0012) {
					// A stump.
					w.set(x, t + 1, z, Bp.log(cold ? Blocks.SPRUCE_LOG : Blocks.OAK_LOG, Direction.Axis.Y));
					if (q < 0.5) {
						w.set(x, t + 2, z, Blocks.MOSS_CARPET.defaultBlockState());
					}
					continue;
				}
				// Ground plants in drifts, so that meadows have patches of flowers and woods have fern beds.
				double drift = Noise.fbm(x / 23.0, z / 23.0, 404, 2);
				BlockState plant = null;
				if (wood) {
					if (q < 0.09) {
						plant = drift > 0.1 ? Blocks.FERN.defaultBlockState() : Blocks.LEAF_LITTER.defaultBlockState();
					} else if (q < 0.1) {
						plant = q < 0.095 ? Blocks.BROWN_MUSHROOM.defaultBlockState() : Blocks.RED_MUSHROOM.defaultBlockState();
					} else if (q < 0.11 && !cold) {
						plant = Blocks.SWEET_BERRY_BUSH.defaultBlockState();
					} else if (q < 0.125 && drift > 0.2) {
						tall(w, x, t + 1, z, Blocks.LARGE_FERN);
						continue;
					} else if (q < 0.14) {
						plant = Blocks.BUSH.defaultBlockState();
					}
				} else if (dry) {
					if (q < 0.12) {
						plant = q < 0.06 ? Blocks.SHORT_DRY_GRASS.defaultBlockState() : Blocks.DEAD_BUSH.defaultBlockState();
					} else if (q < 0.15) {
						plant = Blocks.TALL_DRY_GRASS.defaultBlockState();
					}
				} else {
					if (drift > 0.32 && q < 0.22) {
						Block[] fl = {Blocks.POPPY, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.DANDELION, Blocks.AZURE_BLUET, Blocks.WILDFLOWERS};
						plant = fl[Math.floorMod((int) (drift * 21) + x / 40 + z / 40, fl.length)].defaultBlockState();
					} else if (q < 0.05) {
						tall(w, x, t + 1, z, Blocks.TALL_GRASS);
						continue;
					} else if (q < 0.065) {
						plant = Blocks.BUSH.defaultBlockState();
					} else if (q < 0.075 && nearWater) {
						for (int k = 1; k <= 2; k++) {
							w.set(x, t + k, z, Blocks.SUGAR_CANE.defaultBlockState());
						}
						continue;
					} else if (q < 0.078) {
						plant = Blocks.FIREFLY_BUSH.defaultBlockState();
					}
				}
				if (plant != null && w.get(x, t + 1, z).isAir()) {
					w.set(x, t + 1, z, plant);
				}
			}
		}
	}

	private static void tall(World w, int x, int y, int z, Block b) {
		if (!w.get(x, y, z).isAir() || !w.get(x, y + 1, z).isAir()) {
			return;
		}
		w.set(x, y, z, b.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
		w.set(x, y + 1, z, b.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
	}

	public enum Tree {
		OAK, BIRCH, SPRUCE, PINE, POPLAR, AUTUMN, WILLOW, CYPRESS, OLIVE, SNAG
	}

	static Tree pickTree(Terrain.Col c, boolean wood, boolean dry, boolean cold, boolean water, int x, int z) {
		double v = Noise.rand(x, z, 405);
		if (water && v < 0.5 && !cold) {
			return Tree.WILLOW;
		}
		if (v > 0.97) {
			return Tree.SNAG;
		}
		if (dry || c.zone == Climate.SUBTROPICAL) {
			return v < 0.45 ? Tree.OLIVE : v < 0.8 ? Tree.CYPRESS : Tree.PINE;
		}
		if (cold) {
			return v < 0.55 ? Tree.SPRUCE : v < 0.8 ? Tree.PINE : Tree.BIRCH;
		}
		if (v < 0.4) {
			return Tree.OAK;
		}
		if (v < 0.62) {
			return Tree.BIRCH;
		}
		if (v < 0.72) {
			return Tree.SPRUCE;
		}
		if (v < 0.8) {
			return Tree.PINE;
		}
		if (v < 0.9) {
			return Tree.POPLAR;
		}
		return Tree.AUTUMN;
	}

	private static void leaf(World w, int x, int y, int z, BlockState lv) {
		if (w.in(x, z) && w.get(x, y, z).isAir()) {
			w.set(x, y, z, lv);
		}
	}

	/** An irregular ball of leaves. */
	private static void crown(World w, int cx, int cy, int cz, double rx, double ry, BlockState lv, int seed) {
		int ix = (int) Math.ceil(rx), iy = (int) Math.ceil(ry);
		for (int dx = -ix; dx <= ix; dx++) {
			for (int dy = -iy; dy <= iy; dy++) {
				for (int dz = -ix; dz <= ix; dz++) {
					double d = (dx * dx + dz * dz) / (rx * rx) + dy * dy / (ry * ry);
					if (d <= 1 - Noise.rand(cx + dx, cz + dz + dy * 7, seed) * 0.35) {
						leaf(w, cx + dx, cy + dy, cz + dz, lv);
					}
				}
			}
		}
	}

	private static void trunk(World w, int x, int y, int z, int h, Block log) {
		for (int i = 0; i < h; i++) {
			if (w.in(x, z)) {
				w.set(x, y + i, z, Bp.log(log, Direction.Axis.Y));
			}
		}
	}

	/** A tree of the given kind growing at (x, y, z) - y is the first block above the ground. */
	public static void tree(World w, int x, int y, int z, Tree kind, int seed) {
		int s = Math.abs(seed);
		int a = s % 7, b = (s / 7) % 5;
		switch (kind) {
			case OAK -> {
				int h = 4 + a % 3;
				BlockState lv = leaves(b == 0 ? Blocks.DARK_OAK_LEAVES : Blocks.OAK_LEAVES);
				Block log = b == 0 ? Blocks.DARK_OAK_LOG : Blocks.OAK_LOG;
				trunk(w, x, y, z, h + 1, log);
				// Two or three boughs leaning out, each with its own clump of leaves.
				int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
				for (int k = 0; k < 2 + b % 2; k++) {
					int[] d = dirs[(a + k * 3) % 4];
					int by = y + h - 2 + k % 2;
					for (int i = 1; i <= 2; i++) {
						if (w.in(x + d[0] * i, z + d[1] * i)) {
							w.set(x + d[0] * i, by + i - 1, z + d[1] * i, Bp.log(log, d[0] != 0 ? Direction.Axis.X : Direction.Axis.Z));
						}
					}
					crown(w, x + d[0] * 3, by + 2, z + d[1] * 3, 2.3, 1.6, lv, seed + k);
				}
				crown(w, x, y + h + 1, z, 3.2, 2.2, lv, seed);
			}
			case BIRCH -> {
				int h = 6 + a % 3;
				trunk(w, x, y, z, h, Blocks.BIRCH_LOG);
				crown(w, x, y + h - 1, z, 2.0, 3.0, leaves(Blocks.BIRCH_LEAVES), seed);
			}
			case SPRUCE -> {
				int h = 9 + a % 5;
				BlockState lv = leaves(Blocks.SPRUCE_LEAVES);
				trunk(w, x, y, z, h, Blocks.SPRUCE_LOG);
				for (int i = 2; i <= h; i++) {
					double rad = (h - i) * 0.32 + (i % 2 == 0 ? 0.6 : 0);
					int ri = (int) Math.round(rad);
					for (int dx = -ri; dx <= ri; dx++) {
						for (int dz = -ri; dz <= ri; dz++) {
							if (dx * dx + dz * dz <= rad * rad + 0.5) {
								leaf(w, x + dx, y + i, z + dz, lv);
							}
						}
					}
				}
				leaf(w, x, y + h, z, lv);
				leaf(w, x, y + h + 1, z, lv);
			}
			case PINE -> {
				int h = 10 + a % 4;
				trunk(w, x, y, z, h, Blocks.SPRUCE_LOG);
				crown(w, x, y + h, z, 2.6, 1.3, leaves(Blocks.SPRUCE_LEAVES), seed);
				crown(w, x + (b % 3) - 1, y + h - 2, z + (a % 3) - 1, 1.6, 1.0, leaves(Blocks.SPRUCE_LEAVES), seed + 1);
			}
			case POPLAR, AUTUMN, CYPRESS -> {
				int h = (kind == Tree.CYPRESS ? 7 : 10) + a % 4;
				BlockState lv = leaves(kind == Tree.CYPRESS ? Blocks.SPRUCE_LEAVES
						: kind == Tree.AUTUMN ? (b % 3 == 0 ? Blocks.RED_POPLAR_LEAVES : b % 3 == 1 ? Blocks.ORANGE_POPLAR_LEAVES : Blocks.YELLOW_POPLAR_LEAVES)
								: Blocks.OAK_LEAVES);
				trunk(w, x, y, z, h, kind == Tree.CYPRESS ? Blocks.SPRUCE_LOG : Blocks.POPLAR_LOG);
				for (int i = 2; i <= h + 1; i++) {
					double rad = i < h - 1 ? 1.2 : 0.6;
					for (int dx = -1; dx <= 1; dx++) {
						for (int dz = -1; dz <= 1; dz++) {
							if (dx * dx + dz * dz <= rad * rad) {
								leaf(w, x + dx, y + i, z + dz, lv);
							}
						}
					}
				}
			}
			case WILLOW -> {
				int h = 4 + a % 2;
				BlockState lv = leaves(Blocks.MANGROVE_LEAVES);
				trunk(w, x, y, z, h, Blocks.OAK_LOG);
				crown(w, x, y + h, z, 3.4, 1.5, lv, seed);
				// Hanging curtains of leaves.
				for (int dx = -3; dx <= 3; dx++) {
					for (int dz = -3; dz <= 3; dz++) {
						int r2 = dx * dx + dz * dz;
						if (r2 >= 6 && r2 <= 11 && Noise.rand(x + dx, z + dz, seed) < 0.6) {
							int len = 1 + (int) (Noise.rand(x + dz, z + dx, seed + 3) * 3);
							for (int k = 0; k < len; k++) {
								leaf(w, x + dx, y + h - 1 - k, z + dz, lv);
							}
						}
					}
				}
			}
			case OLIVE -> {
				// A short gnarled trunk leaning one way, a low grey-green crown.
				int h = 3;
				int ox = (a % 3) - 1, oz = (b % 3) - 1;
				if (w.in(x, z)) {
					w.set(x, y, z, Bp.log(Blocks.JUNGLE_LOG, Direction.Axis.Y));
				}
				if (w.in(x + ox, z + oz)) {
					w.set(x + ox, y + 1, z + oz, Bp.log(Blocks.JUNGLE_LOG, Direction.Axis.Y));
					w.set(x + ox, y + 2, z + oz, Bp.log(Blocks.JUNGLE_LOG, Direction.Axis.Y));
				}
				crown(w, x + ox, y + h, z + oz, 2.6, 1.4, leaves(Blocks.AZALEA_LEAVES), seed);
			}
			case SNAG -> {
				int h = 5 + a % 4;
				trunk(w, x, y, z, h, Blocks.STRIPPED_OAK_LOG);
				if (w.in(x + 1, z)) {
					w.set(x + 1, y + h - 2, z, Bp.log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.X));
				}
				if (w.in(x, z - 1)) {
					w.set(x, y + h - 3, z - 1, Bp.log(Blocks.STRIPPED_OAK_LOG, Direction.Axis.Z));
				}
			}
		}
	}

	/** A boulder: a lumpy pile of stone, moss on top, sometimes half sunk. */
	private static void boulder(World w, int x, int t, int z, int seed) {
		int size = 1 + seed % 3;
		Block[] st = {Blocks.STONE, Blocks.ANDESITE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.TUFF, Blocks.GRANITE};
		for (int dx = -size; dx <= size; dx++) {
			for (int dz = -size; dz <= size; dz++) {
				if (!w.in(x + dx, z + dz)) {
					continue;
				}
				double d = Math.sqrt(dx * dx + dz * dz) + Noise.rand(x + dx, z + dz, seed) * 0.8;
				int hh = (int) Math.round(size + 0.5 - d);
				if (hh < 0) {
					continue;
				}
				int gt = w.col(x + dx, z + dz).top();
				for (int k = 0; k <= hh; k++) {
					Block b = st[(int) (Noise.rand(x + dx, z + dz + k, seed + 9) * st.length)];
					w.set(x + dx, gt + k, z + dz, b.defaultBlockState());
				}
				if (Noise.rand(x + dx, z + dz, seed + 5) < 0.5) {
					BlockState above = w.get(x + dx, gt + hh + 1, z + dz);
					if (above.isAir()) {
						w.set(x + dx, gt + hh + 1, z + dz, Blocks.MOSS_CARPET.defaultBlockState());
					}
				}
			}
		}
	}

	private static void fallenLog(World w, int x, int y, int z, Block log, boolean alongX) {
		int len = 3 + Math.abs(x * 7 + z * 3) % 3;
		for (int i = 0; i < len; i++) {
			int xx = alongX ? x + i : x, zz = alongX ? z : z + i;
			if (!w.in(xx, zz) || !w.get(xx, y, zz).isAir() && !w.get(xx, y, zz).canBeReplaced()) {
				return;
			}
			w.set(xx, y, zz, Bp.log(log, alongX ? Direction.Axis.X : Direction.Axis.Z));
			if (i % 2 == 1) {
				w.set(xx, y + 1, zz, Blocks.MOSS_CARPET.defaultBlockState());
			}
		}
	}
}
