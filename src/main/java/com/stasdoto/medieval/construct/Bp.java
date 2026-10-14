package com.stasdoto.medieval.construct;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * A building drawn in its own little grid: x across the front (0..w-1), z from the back (0) to the front (d-1, facing the
 * street, "south"), y up from the floor (0 = floor level; negative = foundations). Placed into the world turned to face
 * its street.
 */
public final class Bp {
	public final int w, d;
	public final Random r;
	final Map<Long, BlockState> blocks = new HashMap<>();
	/** Lowest y used (for foundations). */
	int minY = 0, maxY = 0;

	public Bp(int w, int d, long seed) {
		this.w = w;
		this.d = d;
		this.r = new Random(seed);
	}

	static long key(int x, int y, int z) {
		return ((long) (x + 512) << 40) | ((long) (y + 2048) << 20) | (z + 512);
	}

	static int kx(long k) {
		return (int) (k >>> 40) - 512;
	}

	static int ky(long k) {
		return (int) (k >>> 20 & 0xFFFFF) - 2048;
	}

	static int kz(long k) {
		return (int) (k & 0xFFFFF) - 512;
	}

	public void set(int x, int y, int z, BlockState s) {
		blocks.put(key(x, y, z), s);
		minY = Math.min(minY, y);
		maxY = Math.max(maxY, y);
	}

	public void set(int x, int y, int z, Block b) {
		set(x, y, z, b.defaultBlockState());
	}

	public BlockState get(int x, int y, int z) {
		return blocks.get(key(x, y, z));
	}

	public boolean has(int x, int y, int z) {
		return blocks.containsKey(key(x, y, z));
	}

	public void air(int x, int y, int z) {
		set(x, y, z, Blocks.AIR.defaultBlockState());
	}

	public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
					set(x, y, z, s);
				}
			}
		}
	}

	public void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
		fill(x0, y0, z0, x1, y1, z1, b.defaultBlockState());
	}

	/** Only where nothing is set yet. */
	public void soft(int x, int y, int z, BlockState s) {
		if (!has(x, y, z)) {
			set(x, y, z, s);
		}
	}

	/** Hollow box walls (no floor/ceiling). */
	public void walls(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
		for (int y = y0; y <= y1; y++) {
			for (int x = x0; x <= x1; x++) {
				set(x, y, z0, s);
				set(x, y, z1, s);
			}
			for (int z = z0; z <= z1; z++) {
				set(x0, y, z, s);
				set(x1, y, z, s);
			}
		}
	}

	// ---- block state helpers (directions as seen in the building's own grid; SOUTH = front) ----

	public static BlockState stairs(Block b, Direction facing, boolean top) {
		return b.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
				.setValue(BlockStateProperties.HALF, top ? Half.TOP : Half.BOTTOM);
	}

	public static BlockState slab(Block b, boolean top) {
		return b.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}

	public static BlockState log(Block b, Direction.Axis axis) {
		return b.defaultBlockState().setValue(BlockStateProperties.AXIS, axis);
	}

	public static BlockState facing(Block b, Direction f) {
		return b.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, f);
	}

	public static BlockState trapdoor(Block b, Direction f, boolean top, boolean open) {
		return b.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, f)
				.setValue(BlockStateProperties.HALF, top ? Half.TOP : Half.BOTTOM).setValue(BlockStateProperties.OPEN, open);
	}

	public static BlockState lantern(boolean hanging) {
		return Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, hanging);
	}

	/** A door in the wall at (x, y, z), opening towards {@code out}. */
	public void door(Block b, int x, int y, int z, Direction out) {
		BlockState lower = b.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, out.getOpposite())
				.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
				.setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT).setValue(BlockStateProperties.OPEN, false);
		set(x, y, z, lower);
		set(x, y + 1, z, lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
	}

	public void bed(Block b, int x, int y, int z, Direction head) {
		BlockState foot = b.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, head)
				.setValue(BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT);
		set(x, y, z, foot);
		set(x + head.getStepX(), y, z + head.getStepZ(), foot.setValue(BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
	}

	public static BlockState wallTorch(Direction f) {
		return Blocks.WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, f);
	}

	/**
	 * A gable roof over x0..x1 × z0..z1 starting at height y, ridge running along X (alongX) or Z, with an overhang of
	 * {@code over} blocks. Gable ends are filled with {@code gable} (null = leave open).
	 */
	public void gableRoof(int x0, int x1, int z0, int z1, int y, boolean alongX, int over, Block stair, Block slab, BlockState full, BlockState gable) {
		if (alongX) {
			int lo = z0 - over, hi = z1 + over;
			int layer = 0;
			while (lo <= hi) {
				int yy = y + layer;
				for (int x = x0 - over; x <= x1 + over; x++) {
					if (lo == hi) {
						set(x, yy, lo, slab != null ? slab(slab, false) : full);
					} else {
						set(x, yy, lo, stairs(stair, Direction.SOUTH, false));
						set(x, yy, hi, stairs(stair, Direction.NORTH, false));
						if (layer > 0) {
							set(x, yy - 1, lo, full);
							set(x, yy - 1, hi, full);
						}
					}
				}
				if (gable != null) {
					for (int z = lo + 1; z <= hi - 1; z++) {
						if (z >= z0 && z <= z1) {
							set(x0, yy, z, gable);
							set(x1, yy, z, gable);
						}
					}
				}
				lo++;
				hi--;
				layer++;
			}
		} else {
			int lo = x0 - over, hi = x1 + over;
			int layer = 0;
			while (lo <= hi) {
				int yy = y + layer;
				for (int z = z0 - over; z <= z1 + over; z++) {
					if (lo == hi) {
						set(lo, yy, z, slab != null ? slab(slab, false) : full);
					} else {
						set(lo, yy, z, stairs(stair, Direction.EAST, false));
						set(hi, yy, z, stairs(stair, Direction.WEST, false));
						if (layer > 0) {
							set(lo, yy - 1, z, full);
							set(hi, yy - 1, z, full);
						}
					}
				}
				if (gable != null) {
					for (int x = lo + 1; x <= hi - 1; x++) {
						if (x >= x0 && x <= x1) {
							set(x, yy, z0, gable);
							set(x, yy, z1, gable);
						}
					}
				}
				lo++;
				hi--;
				layer++;
			}
		}
	}

	/** A four-sided pyramid roof (towers, tents). */
	public void pyramidRoof(int x0, int x1, int z0, int z1, int y, Block stair, BlockState full, BlockState tip) {
		int layer = 0;
		while (x0 <= x1 && z0 <= z1) {
			int yy = y + layer;
			if (x0 == x1 || z0 == z1) {
				for (int x = x0; x <= x1; x++) {
					for (int z = z0; z <= z1; z++) {
						set(x, yy, z, full);
					}
				}
				if (tip != null) {
					set((x0 + x1) / 2, yy + 1, (z0 + z1) / 2, tip);
				}
				return;
			}
			for (int x = x0; x <= x1; x++) {
				set(x, yy, z0, stairs(stair, Direction.SOUTH, false));
				set(x, yy, z1, stairs(stair, Direction.NORTH, false));
			}
			for (int z = z0 + 1; z < z1; z++) {
				set(x0, yy, z, stairs(stair, Direction.EAST, false));
				set(x1, yy, z, stairs(stair, Direction.WEST, false));
			}
			if (layer > 0) {
				for (int x = x0; x <= x1; x++) {
					set(x, yy - 1, z0, full);
					set(x, yy - 1, z1, full);
				}
				for (int z = z0; z <= z1; z++) {
					set(x0, yy - 1, z, full);
					set(x1, yy - 1, z, full);
				}
			}
			x0++;
			x1--;
			z0++;
			z1--;
			layer++;
		}
		if (tip != null) {
			set((x0 + x1) / 2, y + layer, (z0 + z1) / 2, tip);
		}
	}

	/** A round dome of radius {@code rad} centred at (cx, y, cz) (y = its base). */
	public void dome(double cx, int y, double cz, double rad, BlockState shell, BlockState tip) {
		int ri = (int) Math.ceil(rad);
		for (int dy = 0; dy <= ri; dy++) {
			for (int x = (int) Math.floor(cx - ri); x <= (int) Math.ceil(cx + ri); x++) {
				for (int z = (int) Math.floor(cz - ri); z <= (int) Math.ceil(cz + ri); z++) {
					double dd = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz) + dy * dy * 1.0);
					if (dd <= rad && dd > rad - 1.3) {
						set(x, y + dy, z, shell);
					}
				}
			}
		}
		if (tip != null) {
			set((int) Math.round(cx), y + ri + 1, (int) Math.round(cz), tip);
		}
	}

	/** An onion/helmet dome (Rus): a bulb narrowing to a spire. */
	public void helmet(int cx, int y, int cz, int rad, BlockState shell, BlockState spire) {
		double[] prof = rad <= 1 ? new double[]{1.2, 1.4, 1.0, 0.5} : new double[]{rad * 0.8, rad * 1.0, rad * 1.0, rad * 0.85, rad * 0.6, rad * 0.3};
		for (int i = 0; i < prof.length; i++) {
			double rr = prof[i];
			int ri = (int) Math.ceil(rr);
			for (int x = -ri; x <= ri; x++) {
				for (int z = -ri; z <= ri; z++) {
					if (x * x + z * z <= rr * rr + 0.3) {
						set(cx + x, y + i, cz + z, shell);
					}
				}
			}
		}
		set(cx, y + prof.length, cz, spire);
		set(cx, y + prof.length + 1, cz, spire);
	}

	public void cross(int x, int y, int z) {
		// An Orthodox / Latin cross from fences and trapdoors is fiddly; a slim gilded post with a crossbar.
		set(x, y, z, Blocks.IRON_CHAIN.defaultBlockState());
		set(x, y + 1, z, Blocks.IRON_CHAIN.defaultBlockState());
		set(x, y + 2, z, Blocks.IRON_CHAIN.defaultBlockState());
		set(x - 1, y + 1, z, trapdoor(Blocks.IRON_TRAPDOOR, Direction.EAST, false, true));
		set(x + 1, y + 1, z, trapdoor(Blocks.IRON_TRAPDOOR, Direction.WEST, false, true));
	}
}
