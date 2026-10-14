package com.stasdoto.medieval.construct;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.stasdoto.medieval.history.Culture;

/**
 * Dwellings of each people, drawn facing the street (+z). Every house differs a little: size, materials, windows,
 * porch, chimney and what lies in the yard.
 */
final class Houses {
	private Houses() {
	}

	static final BlockState AIR = Blocks.AIR.defaultBlockState();

	// ---------------------------------------------------------------- Rus: log izba

	/** A log house (сруб): x0..x1, z0..z1 outer, walls {@code h} high, door facing {@code doorSide}. */
	static void izba(Bp b, Palette p, int x0, int z0, int x1, int z1, int h, boolean town) {
		b.fill(x0, -3, z0, x1, -1, z1, Blocks.COBBLESTONE.defaultBlockState());
		b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, p.floor);
		for (int y = 0; y <= h; y++) {
			boolean even = y % 2 == 0;
			for (int x = x0; x <= x1; x++) {
				b.set(x, y, z0, Bp.log(p.log, Direction.Axis.X));
				b.set(x, y, z1, Bp.log(p.log, Direction.Axis.X));
			}
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y, z, Bp.log(p.log, Direction.Axis.Z));
				b.set(x1, y, z, Bp.log(p.log, Direction.Axis.Z));
			}
			// Log ends cross at the corners ("в обло").
			Direction.Axis ax = even ? Direction.Axis.X : Direction.Axis.Z;
			b.set(x0, y, z0, Bp.log(p.log, ax));
			b.set(x1, y, z0, Bp.log(p.log, ax));
			b.set(x0, y, z1, Bp.log(p.log, ax));
			b.set(x1, y, z1, Bp.log(p.log, ax));
		}
		// Gable to the street, ridge running from the street backwards.
		Block rs = p.thatch ? null : p.roofStairs;
		if (rs != null) {
			b.gableRoof(x0, x1, z0, z1, h + 1, false, 1, rs, p.roofSlab, p.roofFull, p.planks.defaultBlockState());
		} else {
			thatch(b, x0, x1, z0, z1, h + 1, false, p);
		}
		// Carved bargeboards and a ridge beam ("охлупень").
		int mid = (x0 + x1) / 2;
		int top = h + 1 + (x1 - x0 + 2) / 2;
		for (int z = z0 - 1; z <= z1 + 1; z++) {
			b.set(mid, top, z, Bp.log(p.log, Direction.Axis.Z));
		}
		// Windows on the street gable with shutters, one on the side.
		int wy = 2;
		for (int x : new int[]{mid - 2, mid + 2}) {
			if (x > x0 && x < x1) {
				b.set(x, wy, z1, town ? Blocks.GLASS_PANE.defaultBlockState() : AIR);
				b.set(x, wy, z1 + 1, Bp.trapdoor(p.trapdoor, Direction.SOUTH, false, true));
			}
		}
		b.set(mid, h + 2, z1, Blocks.GLASS_PANE.defaultBlockState());
		// The door on the yard side (x1) with a porch.
		int dz = z1 - 2;
		b.door(p.door, x1, 1, dz, Direction.EAST);
		b.set(x1 + 1, 0, dz, Bp.slab(p.slab, true));
		b.set(x1 + 1, -1, dz, p.planks.defaultBlockState());
		b.set(x1 + 1, 1, dz - 1, p.fence.defaultBlockState());
		b.set(x1 + 1, 2, dz - 1, p.fence.defaultBlockState());
		b.set(x1 + 1, 1, dz + 1, p.fence.defaultBlockState());
		b.set(x1 + 1, 2, dz + 1, p.fence.defaultBlockState());
		b.set(x1 + 1, 3, dz - 1, Bp.slab(p.slab, false));
		b.set(x1 + 1, 3, dz, Bp.slab(p.slab, false));
		b.set(x1 + 1, 3, dz + 1, Bp.slab(p.slab, false));
		b.air(x1, 3, dz);
		b.set(x1, 3, dz, Bp.log(p.log, Direction.Axis.Z));
		// Inside: the great stove in the corner, beds, bench, table, chest.
		b.fill(x0 + 1, 1, z0 + 1, x0 + 2, 2, z0 + 2, Blocks.MUD_BRICKS.defaultBlockState());
		b.set(x0 + 2, 1, z0 + 2, Bp.facing(Blocks.FURNACE, Direction.SOUTH));
		b.set(x0 + 1, 3, z0 + 1, Blocks.MUD_BRICKS.defaultBlockState());
		b.bed(p.bed, x1 - 1, 1, z0 + 1, Direction.NORTH);
		if (x1 - x0 >= 7) {
			b.bed(p.bed, x1 - 2, 1, z0 + 1, Direction.NORTH);
		}
		b.set(x0 + 1, 1, z1 - 1, Bp.stairs(p.stairs, Direction.WEST, false));
		b.set(x0 + 1, 1, z1 - 2, Bp.stairs(p.stairs, Direction.WEST, false));
		b.set(x0 + 2, 1, z1 - 1, p.fence.defaultBlockState());
		b.set(x0 + 2, 2, z1 - 1, Bp.slab(p.slab, false));
		b.set(mid, 1, z0 + 1, Blocks.CHEST.defaultBlockState());
		b.set(mid + 1, 1, z0 + 1, Blocks.BARREL.defaultBlockState());
		b.set(x0 + 1, 3, z1 - 1, Bp.wallTorch(Direction.EAST));
		// The red corner with an icon lamp.
		b.set(x1 - 1, 3, z1 - 1, Bp.lantern(true));
		b.set(x1 - 1, h, z1 - 1, p.planks.defaultBlockState());
	}

	/** A thatched roof from hay blocks with slab edges. */
	static void thatch(Bp b, int x0, int x1, int z0, int z1, int y, boolean alongX, Palette p) {
		BlockState hay = Blocks.HAY_BLOCK.defaultBlockState();
		if (alongX) {
			int lo = z0 - 1, hi = z1 + 1, layer = 0;
			while (lo <= hi) {
				for (int x = x0 - 1; x <= x1 + 1; x++) {
					b.set(x, y + layer, lo, hay);
					b.set(x, y + layer, hi, hay);
				}
				for (int z = lo + 1; z < hi; z++) {
					if (z >= z0 && z <= z1) {
						b.set(x0, y + layer, z, p.plaster);
						b.set(x1, y + layer, z, p.plaster);
					}
				}
				lo++;
				hi--;
				layer++;
			}
		} else {
			int lo = x0 - 1, hi = x1 + 1, layer = 0;
			while (lo <= hi) {
				for (int z = z0 - 1; z <= z1 + 1; z++) {
					b.set(lo, y + layer, z, hay);
					b.set(hi, y + layer, z, hay);
				}
				for (int x = lo + 1; x < hi; x++) {
					if (x >= x0 && x <= x1) {
						b.set(x, y + layer, z0, p.plaster);
						b.set(x, y + layer, z1, p.plaster);
					}
				}
				lo++;
				hi--;
				layer++;
			}
		}
	}

	/** A plank shed / barn with a hay loft. */
	static void barn(Bp b, Palette p, int x0, int z0, int x1, int z1) {
		b.fill(x0, -2, z0, x1, -1, z1, Blocks.DIRT.defaultBlockState());
		b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, Blocks.COARSE_DIRT.defaultBlockState());
		for (int y = 1; y <= 3; y++) {
			for (int x = x0; x <= x1; x++) {
				b.set(x, y, z0, p.planks.defaultBlockState());
				b.set(x, y, z1, p.planks.defaultBlockState());
			}
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y, z, p.planks.defaultBlockState());
				b.set(x1, y, z, p.planks.defaultBlockState());
			}
			b.set(x0, y, z0, Bp.log(p.log, Direction.Axis.Y));
			b.set(x1, y, z0, Bp.log(p.log, Direction.Axis.Y));
			b.set(x0, y, z1, Bp.log(p.log, Direction.Axis.Y));
			b.set(x1, y, z1, Bp.log(p.log, Direction.Axis.Y));
		}
		if (p.thatch) {
			thatch(b, x0, x1, z0, z1, 4, true, p);
		} else {
			b.gableRoof(x0, x1, z0, z1, 4, true, 1, p.roofStairs, p.roofSlab, p.roofFull, p.planks.defaultBlockState());
		}
		// Wide doors to the yard (front).
		int mx = (x0 + x1) / 2;
		b.air(mx, 1, z1);
		b.air(mx, 2, z1);
		b.air(mx + 1, 1, z1);
		b.air(mx + 1, 2, z1);
		b.set(mx - 1 >= x0 + 1 ? mx - 1 : mx, 1, z0 + 1, Blocks.HAY_BLOCK.defaultBlockState());
		b.set(x0 + 1, 1, z0 + 1, Blocks.HAY_BLOCK.defaultBlockState());
		b.set(x0 + 1, 2, z0 + 1, Blocks.HAY_BLOCK.defaultBlockState());
		b.set(x1 - 1, 1, z0 + 1, Blocks.COMPOSTER.defaultBlockState());
		b.set(x1 - 1, 1, z1 - 1, Blocks.BARREL.defaultBlockState());
	}

	/** Garden beds behind the house. */
	static void garden(Bp b, int x0, int z0, int x1, int z1, java.util.Random r) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if ((x - x0) % 3 == 2) {
					b.set(x, 0, z, Blocks.DIRT_PATH.defaultBlockState());
					continue;
				}
				b.set(x, 0, z, Blocks.FARMLAND.defaultBlockState());
				int kind = ((x - x0) / 3 + r.nextInt(2)) % 4;
				BlockState crop = switch (kind) {
					case 0 -> Blocks.CARROTS.defaultBlockState().setValue(BlockStateProperties.AGE_7, 4 + r.nextInt(4));
					case 1 -> Blocks.BEETROOTS.defaultBlockState().setValue(BlockStateProperties.AGE_3, 2 + r.nextInt(2));
					case 2 -> Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 3 + r.nextInt(5));
					default -> r.nextInt(5) == 0 ? Blocks.PUMPKIN.defaultBlockState() : Blocks.SHORT_GRASS.defaultBlockState();
				};
				b.set(x, 1, z, crop);
			}
		}
	}

	static void fenceRing(Bp b, Palette p, int x0, int z0, int x1, int z1, int gateX) {
		for (int x = x0; x <= x1; x++) {
			b.soft(x, 1, z0, p.fence.defaultBlockState());
			b.soft(x, 1, z1, x == gateX ? Bp.facing(p.gate, Direction.SOUTH) : p.fence.defaultBlockState());
		}
		for (int z = z0; z <= z1; z++) {
			b.soft(x0, 1, z, p.fence.defaultBlockState());
			b.soft(x1, 1, z, p.fence.defaultBlockState());
		}
	}

	static void woodpile(Bp b, Palette p, int x, int z, int len) {
		for (int i = 0; i < len; i++) {
			b.set(x + i, 1, z, Bp.log(p.log, Direction.Axis.Z));
			if (i > 0 && i < len - 1) {
				b.set(x + i, 2, z, Bp.log(p.log, Direction.Axis.Z));
			}
		}
	}

	// ---------------------------------------------------------------- Latin West: stone and timber frame

	/**
	 * A two-storey house: stone ground floor, a jettied timber-framed upper floor with white infill, a steep roof with
	 * its eaves to the street, a stone chimney with smoke, shutters, flower pots and a lantern at the door.
	 */
	static void timberHouse(Bp b, Palette p, int x0, int z0, int x1, int z1, boolean rich) {
		int g = 4; // ground floor height
		int u = rich ? 4 : 3; // upper floor height
		b.fill(x0, -3, z0, x1, -1, z1, p.stone);
		b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, p.floor);
		// Stone ground floor with mossy patches.
		for (int y = 1; y <= g; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					if (x == x0 || x == x1 || z == z0 || z == z1) {
						b.set(x, y, z, b.r.nextInt(6) == 0 ? p.stone2 : p.stone);
					}
				}
			}
		}
		// Upper floor jettied one block out to the street and the back.
		int uz0 = z0 - 1, uz1 = z1 + 1;
		int fy = g + 1;
		for (int x = x0; x <= x1; x++) {
			for (int z = uz0; z <= uz1; z++) {
				b.set(x, fy, z, (x == x0 || x == x1 || z == uz0 || z == uz1) ? Bp.log(p.strippedLog, Direction.Axis.X) : p.floor);
			}
		}
		for (int z = uz0; z <= uz1; z++) {
			b.set(x0, fy, z, Bp.log(p.strippedLog, Direction.Axis.Z));
			b.set(x1, fy, z, Bp.log(p.strippedLog, Direction.Axis.Z));
		}
		// Brackets under the jetty.
		for (int x = x0; x <= x1; x += 2) {
			b.set(x, g, z1 + 1, Bp.stairs(p.stairs, Direction.NORTH, true));
			b.set(x, g, z0 - 1, Bp.stairs(p.stairs, Direction.SOUTH, true));
		}
		int top = fy + u;
		for (int y = fy + 1; y <= top; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = uz0; z <= uz1; z++) {
					if (!(x == x0 || x == x1 || z == uz0 || z == uz1)) {
						continue;
					}
					boolean corner = (x == x0 || x == x1) && (z == uz0 || z == uz1);
					boolean post = corner || ((z == uz0 || z == uz1) && (x - x0) % 3 == 0) || ((x == x0 || x == x1) && (z - uz0) % 3 == 0);
					BlockState s;
					if (y == top) {
						s = Bp.log(p.strippedLog, (z == uz0 || z == uz1) ? Direction.Axis.X : Direction.Axis.Z);
					} else if (post) {
						s = Bp.log(p.log, Direction.Axis.Y);
					} else if (y == fy + 1 + (u - 1) / 2 && b.r.nextInt(4) == 0 && !corner) {
						s = Bp.log(p.strippedLog, (z == uz0 || z == uz1) ? Direction.Axis.X : Direction.Axis.Z);
					} else {
						s = p.plaster;
					}
					b.set(x, y, z, s);
				}
			}
		}
		// Windows: upper floor street side and back, ground floor street side.
		for (int x = x0 + 1; x < x1; x++) {
			if ((x - x0) % 3 == 2 || (x1 - x0 < 6 && x == (x0 + x1) / 2)) {
				b.set(x, fy + 2, uz1, Blocks.GLASS_PANE.defaultBlockState());
				b.set(x, fy + 2, uz0, Blocks.GLASS_PANE.defaultBlockState());
				b.set(x, fy + 1, uz1 + 1, Bp.trapdoor(p.trapdoor, Direction.SOUTH, true, false));
				if (b.r.nextBoolean()) {
					BlockState pot = switch (b.r.nextInt(4)) {
						case 0 -> Blocks.POTTED_POPPY.defaultBlockState();
						case 1 -> Blocks.POTTED_CORNFLOWER.defaultBlockState();
						case 2 -> Blocks.POTTED_DANDELION.defaultBlockState();
						default -> Blocks.POTTED_RED_TULIP.defaultBlockState();
					};
					b.set(x, fy + 2, uz1 + 1, pot);
				}
			}
		}
		int door = (x0 + x1) / 2 + (b.r.nextBoolean() ? -1 : 1);
		for (int x = x0 + 1; x < x1; x++) {
			if (x != door && (x - x0) % 3 == 1) {
				b.set(x, 2, z1, Blocks.GLASS_PANE.defaultBlockState());
				b.set(x, 2, z1 + 1, Bp.trapdoor(p.trapdoor, Direction.SOUTH, false, true));
			}
		}
		b.door(p.door, door, 1, z1, Direction.SOUTH);
		b.set(door, 0, z1 + 1, Blocks.STONE_BRICK_SLAB.defaultBlockState());
		b.set(door + 1, 3, z1 + 1, Bp.lantern(false));
		b.set(door + 1, 2, z1 + 1, p.fence.defaultBlockState());
		// Steep roof, eaves to the street.
		b.gableRoof(x0, x1, uz0, uz1, top + 1, true, 1, p.roofStairs, p.roofSlab, p.roofFull, p.plaster);
		int ridge = top + 1 + (uz1 - uz0 + 2) / 2;
		// A small dormer window in the gable ends.
		b.set(x0, top + 2, (uz0 + uz1) / 2, Blocks.GLASS_PANE.defaultBlockState());
		b.set(x1, top + 2, (uz0 + uz1) / 2, Blocks.GLASS_PANE.defaultBlockState());
		// Chimney on one side with smoke.
		int cx = b.r.nextBoolean() ? x0 + 1 : x1 - 1;
		int cz = uz0 + 1;
		for (int y = 1; y <= ridge + 1; y++) {
			b.set(cx, y, cz, Blocks.COBBLESTONE.defaultBlockState());
		}
		b.set(cx, ridge + 2, cz, Blocks.CAMPFIRE.defaultBlockState());
		b.set(cx, 1, cz + 1, Bp.facing(Blocks.FURNACE, Direction.SOUTH));
		// Inside.
		b.set(x0 + 1, 1, z0 + 1, Blocks.CRAFTING_TABLE.defaultBlockState());
		b.set(x1 - 1, 1, z0 + 1, Blocks.BARREL.defaultBlockState());
		b.set(x1 - 2, 1, z0 + 1, Blocks.CHEST.defaultBlockState());
		b.set(x0 + 2, 1, z1 - 2, p.fence.defaultBlockState());
		b.set(x0 + 2, 2, z1 - 2, Bp.slab(p.slab, false));
		b.set(x0 + 1, 1, z1 - 2, Bp.stairs(p.stairs, Direction.WEST, false));
		b.set(x0 + 3, 1, z1 - 2, Bp.stairs(p.stairs, Direction.EAST, false));
		b.set((x0 + x1) / 2, g, (z0 + z1) / 2, Bp.lantern(true));
		// Ladder up and beds upstairs.
		b.set(x1 - 1, 1, z1 - 1, Bp.facing(Blocks.LADDER, Direction.NORTH));
		for (int y = 2; y <= g; y++) {
			b.set(x1 - 1, y, z1 - 1, Bp.facing(Blocks.LADDER, Direction.NORTH));
		}
		b.air(x1 - 1, fy, z1 - 1);
		b.set(x1 - 1, fy, z1 - 1, Bp.facing(Blocks.LADDER, Direction.NORTH));
		b.bed(p.bed, x0 + 1, fy + 1, z0, Direction.NORTH);
		if (x1 - x0 > 6) {
			b.bed(p.bed, x0 + 3, fy + 1, z0, Direction.NORTH);
		}
		b.set(x1 - 1, fy + 1, uz0 + 1, Blocks.CHEST.defaultBlockState());
		b.set((x0 + x1) / 2, top, (z0 + z1) / 2, Bp.lantern(true));
	}

	// ---------------------------------------------------------------- Norse: longhouse

	static void longhouse(Bp b, Palette p, int x0, int z0, int x1, int z1) {
		b.fill(x0, -2, z0, x1, -1, z1, Blocks.DIRT.defaultBlockState());
		b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, Blocks.PACKED_MUD.defaultBlockState());
		for (int y = 1; y <= 3; y++) {
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y, z, (z - z0) % 3 == 0 ? Bp.log(p.log, Direction.Axis.Y) : p.planks.defaultBlockState());
				b.set(x1, y, z, (z - z0) % 3 == 0 ? Bp.log(p.log, Direction.Axis.Y) : p.planks.defaultBlockState());
			}
			for (int x = x0; x <= x1; x++) {
				b.set(x, y, z0, p.planks.defaultBlockState());
				b.set(x, y, z1, p.planks.defaultBlockState());
			}
		}
		// Turf roof: dark wood with moss on top.
		b.gableRoof(x0, x1, z0, z1, 4, false, 1, p.roofStairs, p.roofSlab, p.roofFull, p.planks.defaultBlockState());
		int half = (x1 - x0 + 2) / 2;
		for (int layer = 0; layer <= half; layer++) {
			for (int z = z0 - 1; z <= z1 + 1; z++) {
				b.set(x0 - 1 + layer, 5 + layer, z, Blocks.MOSS_CARPET.defaultBlockState());
				b.set(x1 + 1 - layer, 5 + layer, z, Blocks.MOSS_CARPET.defaultBlockState());
			}
		}
		// Crossed gable beams.
		int top = 4 + half;
		b.set((x0 + x1) / 2 - 1, top + 1, z1 + 1, p.fence.defaultBlockState());
		b.set((x0 + x1) / 2 + 1, top + 1, z1 + 1, p.fence.defaultBlockState());
		b.door(p.door, (x0 + x1) / 2, 1, z1, Direction.SOUTH);
		// Long hearth down the middle with benches.
		for (int z = z0 + 3; z <= z1 - 3; z += 4) {
			b.set((x0 + x1) / 2, 0, z, Blocks.CAMPFIRE.defaultBlockState());
		}
		for (int z = z0 + 2; z <= z1 - 2; z++) {
			b.set(x0 + 1, 1, z, Bp.stairs(p.stairs, Direction.WEST, false));
			b.set(x1 - 1, 1, z, Bp.stairs(p.stairs, Direction.EAST, false));
		}
		b.bed(p.bed, x0 + 1, 1, z0 + 1, Direction.NORTH);
		b.set(x1 - 1, 1, z0 + 1, Blocks.CHEST.defaultBlockState());
		b.set((x0 + x1) / 2, 3, z0 + 1, Bp.lantern(true));
	}

	// ---------------------------------------------------------------- Byzantium and the Caucasus: brick and stone

	static void stoneHouse(Bp b, Palette p, int x0, int z0, int x1, int z1, int floors) {
		b.fill(x0, -3, z0, x1, -1, z1, p.stone2);
		b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, p.floor);
		int h = floors * 4;
		for (int y = 1; y <= h; y++) {
			BlockState band = y % 4 == 0 ? p.stone2 : p.stone;
			for (int x = x0; x <= x1; x++) {
				b.set(x, y, z0, band);
				b.set(x, y, z1, band);
			}
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y, z, band);
				b.set(x1, y, z, band);
			}
		}
		for (int f = 0; f < floors; f++) {
			int y = f * 4 + 2;
			for (int x = x0 + 2; x < x1 - 1; x += 3) {
				b.set(x, y, z1, Blocks.GLASS_PANE.defaultBlockState());
				b.set(x, y + 1, z1, Bp.stairs(p.stairs == Blocks.SMOOTH_SANDSTONE_STAIRS ? Blocks.SMOOTH_SANDSTONE_STAIRS : Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true));
				b.set(x, y, z0, Blocks.GLASS_PANE.defaultBlockState());
			}
			if (f > 0) {
				b.fill(x0 + 1, f * 4, z0 + 1, x1 - 1, f * 4, z1 - 1, p.floor);
				b.air(x1 - 1, f * 4, z0 + 1);
			}
		}
		for (int y = 1; y <= h; y++) {
			b.set(x1 - 1, y, z0 + 1, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}
		int door = (x0 + x1) / 2;
		b.door(p.door, door, 1, z1, Direction.SOUTH);
		b.set(door, 3, z1, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true));
		// Low tiled roof.
		b.gableRoof(x0, x1, z0, z1, h + 1, true, 1, p.roofStairs, p.roofSlab, p.roofFull, p.stone);
		b.set(x0 + 1, 1, z0 + 1, Blocks.BARREL.defaultBlockState());
		b.set(x0 + 1, 1, z0 + 2, Blocks.DECORATED_POT.defaultBlockState());
		b.set(x0 + 2, 1, z0 + 1, Blocks.CHEST.defaultBlockState());
		b.bed(p.bed, x0 + 1, h - 3, z0 + 1, Direction.NORTH);
		b.set((x0 + x1) / 2, 3, (z0 + z1) / 2, Bp.lantern(true));
	}

	// ---------------------------------------------------------------- Islamic world: courtyard house

	static void courtyardHouse(Bp b, Palette p, int x0, int z0, int x1, int z1, boolean rich) {
		int h = (rich ? 7 : 5) + b.r.nextInt(2) - (b.r.nextInt(4) == 0 ? 1 : 0);
		// Whitewash, ochre plaster or bare mud brick, house by house.
		int tone = b.r.nextInt(4);
		if (tone == 1) {
			p.stone = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
		} else if (tone == 2) {
			p.stone = Blocks.PACKED_MUD.defaultBlockState();
		} else if (tone == 3) {
			p.stone = Blocks.TERRACOTTA.defaultBlockState();
		}
		b.fill(x0, -2, z0, x1, -1, z1, p.stone2);
		b.fill(x0, 0, z0, x1, 0, z1, p.floor);
		for (int y = 1; y <= h; y++) {
			for (int x = x0; x <= x1; x++) {
				b.set(x, y, z0, p.stone);
				b.set(x, y, z1, p.stone);
			}
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y, z, p.stone);
				b.set(x1, y, z, p.stone);
			}
		}
		// Rooms around a courtyard: an inner ring of walls, flat roofs over the rooms.
		int cx0 = x0 + 3, cx1 = x1 - 3, cz0 = z0 + 3, cz1 = z1 - 3;
		for (int y = 1; y <= h - 1; y++) {
			for (int x = cx0; x <= cx1; x++) {
				b.set(x, y, cz0, (x - cx0) % 2 == 1 && y <= 2 ? AIR : p.plaster);
				b.set(x, y, cz1, (x - cx0) % 2 == 1 && y <= 2 ? AIR : p.plaster);
			}
			for (int z = cz0; z <= cz1; z++) {
				b.set(cx0, y, z, p.plaster);
				b.set(cx1, y, z, p.plaster);
			}
		}
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean court = x > cx0 && x < cx1 && z > cz0 && z < cz1;
				if (!court) {
					b.set(x, h, z, p.roofFull);
				}
			}
		}
		// An upper room on the roof (a summer room) for some houses, with a lattice window.
		if (b.r.nextInt(3) == 0 && x1 - x0 >= 8) {
			int ux0 = x0, ux1 = x0 + 4, uz0 = z0, uz1 = z0 + 3;
			for (int y = h + 1; y <= h + 3; y++) {
				for (int x = ux0; x <= ux1; x++) {
					b.set(x, y, uz0, p.stone);
					b.set(x, y, uz1, p.stone);
				}
				for (int z = uz0; z <= uz1; z++) {
					b.set(ux0, y, z, p.stone);
					b.set(ux1, y, z, p.stone);
				}
			}
			b.fill(ux0, h + 4, uz0, ux1, h + 4, uz1, p.roofFull);
			b.set(ux0 + 2, h + 2, uz1, Bp.trapdoor(p.trapdoor, Direction.SOUTH, false, false));
			b.door(p.door, ux1, h + 1, uz0 + 1, Direction.EAST);
		}
		// Parapet.
		for (int x = x0; x <= x1; x++) {
			b.set(x, h + 1, z0, Bp.slab(p.roofSlab, false));
			b.set(x, h + 1, z1, Bp.slab(p.roofSlab, false));
		}
		for (int z = z0; z <= z1; z++) {
			b.set(x0, h + 1, z, Bp.slab(p.roofSlab, false));
			b.set(x1, h + 1, z, Bp.slab(p.roofSlab, false));
		}
		// Courtyard: a fountain/well and a tree.
		int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
		b.set(mx, 0, mz, Blocks.WATER.defaultBlockState());
		b.set(mx + 1, 0, mz, Blocks.CUT_SANDSTONE.defaultBlockState());
		b.set(mx - 1, 0, mz, Blocks.CUT_SANDSTONE.defaultBlockState());
		b.set(mx, 0, mz + 1, Blocks.CUT_SANDSTONE.defaultBlockState());
		b.set(mx, 0, mz - 1, Blocks.CUT_SANDSTONE.defaultBlockState());
		if (cx1 - cx0 > 4) {
			b.set(cx0 + 1, 0, cz0 + 1, Blocks.GRASS_BLOCK.defaultBlockState());
			b.set(cx0 + 1, 1, cz0 + 1, Blocks.FLOWERING_AZALEA.defaultBlockState());
		}
		// Arched entrance in the street wall.
		b.door(p.door, mx, 1, z1, Direction.SOUTH);
		b.set(mx, 3, z1, Bp.stairs(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.SOUTH, true));
		for (int x = x0 + 2; x < x1 - 1; x += 3) {
			if (Math.abs(x - mx) > 1) {
				b.set(x, h - 2, z1, Blocks.IRON_BARS.defaultBlockState());
			}
		}
		b.set(x0 + 1, 1, z0 + 1, Blocks.DECORATED_POT.defaultBlockState());
		b.set(x0 + 2, 1, z0 + 1, Blocks.BARREL.defaultBlockState());
		b.set(x1 - 1, 1, z0 + 1, Blocks.LOOM.defaultBlockState());
		b.bed(p.bed, x1 - 1, 1, z0 + 2, Direction.NORTH);
		b.set(x0 + 1, 1, z1 - 1, Blocks.CARPET.pick(DyeColor.RED).defaultBlockState());
		b.set(x0 + 2, 1, z1 - 1, Blocks.CARPET.pick(DyeColor.RED).defaultBlockState());
		b.set(mx, h - 1, cz0 - 1, Bp.lantern(true));
	}

	// ---------------------------------------------------------------- Steppe: yurt

	static void yurt(Bp b, int cx, int cz, DyeColor felt) {
		double R = 3.6;
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				double dd = Math.sqrt(x * x + z * z);
				if (dd <= R + 0.2) {
					b.set(cx + x, 0, cz + z, Blocks.CARPET.pick(dd < 2 ? DyeColor.RED : DyeColor.BROWN).defaultBlockState());
					b.set(cx + x, -1, cz + z, Blocks.COARSE_DIRT.defaultBlockState());
				}
				if (dd > R - 0.8 && dd <= R + 0.2) {
					b.set(cx + x, 0, cz + z, Blocks.WOOL.pick(felt).defaultBlockState());
					b.set(cx + x, 1, cz + z, Blocks.WOOL.pick(felt).defaultBlockState());
					b.set(cx + x, 2, cz + z, Blocks.WOOL.pick(felt).defaultBlockState());
				}
				if (dd <= R + 0.2 && dd > 2.4) {
					b.set(cx + x, 3, cz + z, Blocks.WOOL_SLAB.pick(felt).defaultBlockState());
				}
				if (dd <= 2.4 && dd > 1.2) {
					b.set(cx + x, 4, cz + z, Blocks.WOOL_SLAB.pick(felt).defaultBlockState());
				}
			}
		}
		b.set(cx, 0, cz, Blocks.CAMPFIRE.defaultBlockState());
		b.door(Blocks.OAK_DOOR, cx, 0, cz + 4, Direction.SOUTH);
		b.air(cx, 2, cz + 4);
		b.set(cx, 2, cz + 4, Blocks.WOOL.pick(felt).defaultBlockState());
		b.set(cx - 2, 0, cz - 2, Blocks.CHEST.defaultBlockState());
	}

	// ---------------------------------------------------------------- Celtic: round house

	static void roundhouse(Bp b, Palette p, int cx, int cz, int rad) {
		for (int x = -rad - 1; x <= rad + 1; x++) {
			for (int z = -rad - 1; z <= rad + 1; z++) {
				double dd = Math.sqrt(x * x + z * z);
				if (dd <= rad + 0.3) {
					b.set(cx + x, 0, cz + z, Blocks.PACKED_MUD.defaultBlockState());
					b.set(cx + x, -1, cz + z, Blocks.COBBLESTONE.defaultBlockState());
				}
				if (dd > rad - 0.7 && dd <= rad + 0.3) {
					for (int y = 1; y <= 3; y++) {
						b.set(cx + x, y, cz + z, b.r.nextInt(5) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
					}
				}
			}
		}
		for (int layer = 0; layer <= rad + 1; layer++) {
			double rr = rad + 1.2 - layer;
			for (int x = -rad - 1; x <= rad + 1; x++) {
				for (int z = -rad - 1; z <= rad + 1; z++) {
					double dd = Math.sqrt(x * x + z * z);
					if (dd <= rr && dd > rr - 1.3) {
						b.set(cx + x, 4 + layer, cz + z, Blocks.HAY_BLOCK.defaultBlockState());
					}
				}
			}
		}
		b.door(p.door, cx, 1, cz + rad, Direction.SOUTH);
		b.set(cx, 0, cz, Blocks.CAMPFIRE.defaultBlockState());
		b.bed(p.bed, cx - rad + 1, 1, cz - 1, Direction.NORTH);
	}
}
