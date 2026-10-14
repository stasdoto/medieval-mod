package com.stasdoto.medieval.construct;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.stasdoto.medieval.history.Culture;

/** Palaces, keeps, workshops, inns, granaries, market stalls, wells and monks' cells. */
final class Civic {
	private Civic() {
	}

	/** A prince's terem (Rus): a log ground floor, a high upper storey with a gallery, a tall roof with a tower. */
	static void terem(Bp b, Palette p) {
		int x0 = 2, x1 = b.w - 3, z0 = 3, z1 = b.d - 4;
		b.fill(x0, -3, z0, x1, -1, z1, Blocks.COBBLESTONE.defaultBlockState());
		for (int floor = 0; floor < 2; floor++) {
			int y0 = floor * 5;
			for (int y = y0; y <= y0 + 4; y++) {
				for (int x = x0; x <= x1; x++) {
					b.set(x, y, z0, Bp.log(p.log, Direction.Axis.X));
					b.set(x, y, z1, Bp.log(p.log, Direction.Axis.X));
				}
				for (int z = z0; z <= z1; z++) {
					b.set(x0, y, z, Bp.log(p.log, Direction.Axis.Z));
					b.set(x1, y, z, Bp.log(p.log, Direction.Axis.Z));
				}
			}
			b.fill(x0 + 1, y0, z0 + 1, x1 - 1, y0, z1 - 1, p.floor);
			for (int x = x0 + 2; x < x1 - 1; x += 3) {
				b.set(x, y0 + 2, z1, Blocks.GLASS_PANE.defaultBlockState());
				b.set(x, y0 + 2, z0, Blocks.GLASS_PANE.defaultBlockState());
				b.set(x, y0 + 3, z1 + 1, Bp.slab(p.slab, true));
			}
		}
		// Gallery around the upper floor.
		for (int x = x0 - 1; x <= x1 + 1; x++) {
			b.set(x, 5, z1 + 1, p.planks.defaultBlockState());
			b.set(x, 6, z1 + 2, p.fence.defaultBlockState());
			b.set(x, 5, z1 + 2, p.planks.defaultBlockState());
		}
		for (int x = x0 - 1; x <= x1 + 1; x += 3) {
			for (int y = 1; y <= 4; y++) {
				b.set(x, y, z1 + 2, Bp.log(p.log, Direction.Axis.Y));
			}
		}
		b.gableRoof(x0, x1, z0, z1, 10, true, 1, p.roofStairs, p.roofSlab, p.roofFull, p.planks.defaultBlockState());
		// The tower (повалуша) with a tent roof at one end.
		int tx = x1 - 3, tz = z0 + 3;
		int top = 10 + (z1 - z0 + 2) / 2;
		for (int y = 10; y <= top + 4; y++) {
			for (int x = tx - 2; x <= tx + 2; x++) {
				for (int z = tz - 2; z <= tz + 2; z++) {
					if (Math.abs(x - tx) == 2 || Math.abs(z - tz) == 2) {
						b.set(x, y, z, Bp.log(p.log, Direction.Axis.Y));
					}
				}
			}
		}
		b.set(tx, top + 2, tz + 2, Blocks.GLASS_PANE.defaultBlockState());
		b.pyramidRoof(tx - 3, tx + 3, tz - 3, tz + 3, top + 5, p.roofStairs, p.roofFull, Blocks.GOLD_BLOCK.defaultBlockState());
		// Grand porch with stairs to the gallery.
		int mx = (x0 + x1) / 2;
		b.door(p.door, mx, 1, z1, Direction.SOUTH);
		for (int i = 0; i < 4; i++) {
			b.set(x0 + 1 + i, 1 + i, z1 + 3, Bp.stairs(p.stairs, Direction.EAST, false));
		}
		// Hall inside: long table, throne.
		for (int x = x0 + 3; x <= x1 - 3; x++) {
			b.set(x, 1, (z0 + z1) / 2, p.fence.defaultBlockState());
			b.set(x, 2, (z0 + z1) / 2, Bp.slab(p.slab, false));
			b.set(x, 1, (z0 + z1) / 2 - 1, Bp.stairs(p.stairs, Direction.SOUTH, false));
			b.set(x, 1, (z0 + z1) / 2 + 1, Bp.stairs(p.stairs, Direction.NORTH, false));
		}
		b.set(mx, 1, z0 + 1, Bp.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
		b.set(mx, 0, z0 + 1, Blocks.GOLD_BLOCK.defaultBlockState());
		for (int x = x0 + 2; x < x1; x += 4) {
			b.set(x, 4, (z0 + z1) / 2, Bp.lantern(true));
		}
		b.bed(Blocks.BED.pick(DyeColor.RED), x0 + 1, 6, z0 + 1, Direction.NORTH);
		b.set(x0 + 2, 6, z0 + 1, Blocks.CHEST.defaultBlockState());
		b.set(x1 - 1, 1, z1 - 1, Bp.facing(Blocks.LADDER, Direction.NORTH));
		for (int y = 2; y <= 5; y++) {
			b.set(x1 - 1, y, z1 - 1, Bp.facing(Blocks.LADDER, Direction.NORTH));
		}
	}

	/** A stone hall of the West or Byzantium: a long two-storey hall with a big roof. */
	static void stoneHall(Bp b, Palette p) {
		Houses.stoneHouse(b, p, 2, 3, b.w - 3, b.d - 4, 2);
		int mx = b.w / 2;
		for (int x = 4; x < b.w - 4; x++) {
			b.set(x, 1, b.d / 2, p.fence.defaultBlockState());
			b.set(x, 2, b.d / 2, Bp.slab(p.slab, false));
		}
		b.set(mx, 1, 4, Bp.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
		b.set(mx - 1, 2, 3, Blocks.BANNER.pick(DyeColor.RED).defaultBlockState());
	}

	/** A palace around a courtyard (Islamic lands). */
	static void palace(Bp b, Palette p) {
		Houses.courtyardHouse(b, p, 1, 1, b.w - 2, b.d - 2, true);
	}

	/**
	 * A keep: a square tower of several storeys with a crenellated top - stone in the West and Byzantium, logs in Rus
	 * and among the Slavs and Magyars.
	 */
	static void keep(Bp b, Palette p, boolean stone) {
		int x0 = 1, x1 = b.w - 2, z0 = 1, z1 = b.d - 2;
		int floors = 4;
		int h = floors * 5;
		BlockState wall = stone ? Blocks.STONE_BRICKS.defaultBlockState() : Bp.log(p.log, Direction.Axis.Y);
		BlockState wall2 = stone ? Blocks.COBBLESTONE.defaultBlockState() : Bp.log(p.log, Direction.Axis.Y);
		b.fill(x0, -5, z0, x1, -1, z1, Blocks.COBBLESTONE.defaultBlockState());
		for (int y = 0; y <= h; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
					if (edge) {
						b.set(x, y, z, b.r.nextInt(6) == 0 ? wall2 : wall);
					} else if (y % 5 == 0) {
						b.set(x, y, z, p.planks.defaultBlockState());
					} else {
						b.air(x, y, z);
					}
				}
			}
		}
		// Arrow slits.
		for (int f = 0; f < floors; f++) {
			int y = f * 5 + 2;
			for (int i = x0 + 3; i < x1 - 2; i += 4) {
				b.set(i, y, z0, Blocks.AIR.defaultBlockState());
				b.set(i, y, z1, Blocks.AIR.defaultBlockState());
				b.set(i, y + 1, z0, Blocks.AIR.defaultBlockState());
				b.set(i, y + 1, z1, Blocks.AIR.defaultBlockState());
			}
			for (int i = z0 + 3; i < z1 - 2; i += 4) {
				b.set(x0, y, i, Blocks.AIR.defaultBlockState());
				b.set(x1, y, i, Blocks.AIR.defaultBlockState());
			}
			b.set((x0 + x1) / 2, f * 5 + 4, (z0 + z1) / 2, Bp.lantern(true));
		}
		// Ladders between floors.
		for (int y = 1; y <= h; y++) {
			b.set(x0 + 1, y, z0 + 1, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}
		// Battlements.
		for (int x = x0 - 1; x <= x1 + 1; x++) {
			for (int z = z0 - 1; z <= z1 + 1; z++) {
				boolean edge = x == x0 - 1 || x == x1 + 1 || z == z0 - 1 || z == z1 + 1;
				if (edge) {
					b.set(x, h, z, wall);
					if ((x + z) % 2 == 0) {
						b.set(x, h + 1, z, wall);
					}
				}
			}
		}
		if (!stone) {
			b.pyramidRoof(x0 - 1, x1 + 1, z0 - 1, z1 + 1, h + 2, p.roofStairs, p.roofFull, Blocks.LIGHTNING_ROD.waxed().unaffected().defaultBlockState());
		} else {
			b.set((x0 + x1) / 2, h + 1, (z0 + z1) / 2, Blocks.OAK_FENCE.defaultBlockState());
			b.set((x0 + x1) / 2, h + 2, (z0 + z1) / 2, Blocks.OAK_FENCE.defaultBlockState());
			b.set((x0 + x1) / 2, h + 3, (z0 + z1) / 2, Blocks.BANNER.pick(DyeColor.RED).defaultBlockState());
		}
		// Raised entrance with wooden stairs.
		int mx = (x0 + x1) / 2;
		b.door(p.door, mx, 6, z1, Direction.SOUTH);
		for (int i = 0; i < 6; i++) {
			b.set(mx - 5 + i, 1 + i, z1 + 1, Bp.stairs(p.stairs, Direction.EAST, false));
		}
		b.set(mx, 5, z1 + 1, p.planks.defaultBlockState());
		b.set(x0 + 2, 6, z1 - 2, Blocks.CHEST.defaultBlockState());
		b.set(x1 - 2, 1, z0 + 2, Blocks.BARREL.defaultBlockState());
		b.set(x1 - 2, 1, z0 + 3, Blocks.BARREL.defaultBlockState());
		b.bed(Blocks.BED.pick(DyeColor.RED), x1 - 3, 11, z0 + 2, Direction.NORTH);
	}

	/** A smithy: an open shed with a hearth, anvil and grindstone, and the smith's tools. */
	static void smithy(Bp b, Palette p, boolean stone) {
		int x0 = 1, x1 = b.w - 2, z0 = 1, z1 = b.d - 2;
		b.fill(x0, -2, z0, x1, -1, z1, Blocks.COBBLESTONE.defaultBlockState());
		b.fill(x0, 0, z0, x1, 0, z1, Blocks.COBBLESTONE.defaultBlockState());
		for (int y = 1; y <= 3; y++) {
			for (int x = x0; x <= x1; x++) {
				b.set(x, y, z0, stone ? p.stone : p.planks.defaultBlockState());
			}
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y, z, stone ? p.stone : p.planks.defaultBlockState());
			}
			b.set(x1, y, z0, Bp.log(p.log, Direction.Axis.Y));
			b.set(x1, y, z1, Bp.log(p.log, Direction.Axis.Y));
			b.set(x0, y, z1, Bp.log(p.log, Direction.Axis.Y));
		}
		if (p.thatch) {
			Houses.thatch(b, x0, x1, z0, z1, 4, true, p);
		} else {
			b.gableRoof(x0, x1, z0, z1, 4, true, 1, p.roofStairs, p.roofSlab, p.roofFull, p.planks.defaultBlockState());
		}
		// Hearth with chimney.
		b.fill(x0 + 1, 1, z0 + 1, x0 + 2, 1, z0 + 2, Blocks.COBBLESTONE.defaultBlockState());
		b.set(x0 + 1, 2, z0 + 1, Blocks.CAMPFIRE.defaultBlockState());
		int ch = 4 + (z1 - z0 + 2) / 2 + 2;
		for (int y = 3; y <= ch; y++) {
			b.set(x0 + 1, y, z0, Blocks.COBBLESTONE.defaultBlockState());
		}
		b.set(x0 + 1, ch + 1, z0, Blocks.CAMPFIRE.defaultBlockState());
		b.set(x0 + 2, 1, z0 + 3, Bp.facing(Blocks.BLAST_FURNACE, Direction.EAST));
		b.set(x0 + 4, 1, z0 + 3, Blocks.ANVIL.defaultBlockState());
		b.set(x1 - 2, 1, z0 + 1, Blocks.GRINDSTONE.defaultBlockState());
		b.set(x1 - 1, 1, z0 + 1, Blocks.SMITHING_TABLE.defaultBlockState());
		b.set(x0 + 1, 1, z1 - 1, Blocks.CHEST.defaultBlockState());
		b.set(x0 + 2, 1, z1 - 1, Blocks.WATER_CAULDRON.defaultBlockState());
		b.set((x0 + x1) / 2, 3, (z0 + z1) / 2, Bp.lantern(true));
	}

	/** An inn: a big house with tables and barrels. */
	static void tavern(Bp b, Palette p, Culture c) {
		switch (c) {
			case LATIN, CELTIC, WEST_SLAV, MAGYAR -> Houses.timberHouse(b, p, 2, 3, b.w - 3, b.d - 3, true);
			case BYZANTINE, CAUCASUS -> Houses.stoneHouse(b, p, 1, 2, b.w - 2, b.d - 2, 2);
			case ANDALUSI, ARAB, PERSIAN, STEPPE -> Houses.courtyardHouse(b, p, 1, 1, b.w - 2, b.d - 2, false);
			default -> Houses.izba(b, p, 2, 3, b.w - 3, b.d - 3, 4, true);
		}
		for (int x = 3; x < b.w - 4; x += 3) {
			b.soft(x, 1, b.d / 2, p.fence.defaultBlockState());
			b.soft(x, 2, b.d / 2, Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
		}
		b.set(3, 1, 4, Blocks.BARREL.defaultBlockState());
		b.set(4, 1, 4, Blocks.BARREL.defaultBlockState());
		b.set(3, 2, 4, Blocks.BARREL.defaultBlockState());
		// Sign: a hanging lantern and a bush over the door.
		b.set(b.w / 2 + 2, 4, b.d - 1, Bp.lantern(false));
	}

	/** A granary on stilts (amбар / horreum). */
	static void granary(Bp b, Palette p) {
		int x0 = 1, x1 = b.w - 2, z0 = 1, z1 = b.d - 2;
		for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
			b.set(c[0], 0, c[1], Blocks.COBBLESTONE.defaultBlockState());
			b.set(c[0], 1, c[1], Bp.log(p.log, Direction.Axis.Y));
			b.set(c[0], -1, c[1], Blocks.COBBLESTONE.defaultBlockState());
		}
		b.fill(x0, 2, z0, x1, 2, z1, p.planks.defaultBlockState());
		for (int y = 3; y <= 5; y++) {
			for (int x = x0; x <= x1; x++) {
				b.set(x, y, z0, Bp.log(p.log, Direction.Axis.X));
				b.set(x, y, z1, Bp.log(p.log, Direction.Axis.X));
			}
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y, z, Bp.log(p.log, Direction.Axis.Z));
				b.set(x1, y, z, Bp.log(p.log, Direction.Axis.Z));
			}
		}
		if (p.thatch) {
			Houses.thatch(b, x0, x1, z0, z1, 6, true, p);
		} else {
			b.gableRoof(x0, x1, z0, z1, 6, true, 1, p.roofStairs, p.roofSlab, p.roofFull, p.planks.defaultBlockState());
		}
		int mx = (x0 + x1) / 2;
		b.door(p.door, mx, 3, z1, Direction.SOUTH);
		b.set(mx, 2, z1 + 1, Bp.slab(p.slab, true));
		b.set(mx, 1, z1 + 2, Bp.stairs(p.stairs, Direction.NORTH, false));
		b.set(x0 + 1, 3, z0 + 1, Blocks.HAY_BLOCK.defaultBlockState());
		b.set(x0 + 2, 3, z0 + 1, Blocks.BARREL.defaultBlockState());
		b.set(x1 - 1, 3, z0 + 1, Blocks.BARREL.defaultBlockState());
		b.set(x1 - 1, 4, z0 + 1, Blocks.HAY_BLOCK.defaultBlockState());
	}

	/** A market stall: four posts, a cloth awning and goods. */
	static void stall(Bp b, java.util.Random r) {
		DyeColor[] cols = {DyeColor.RED, DyeColor.YELLOW, DyeColor.BLUE, DyeColor.GREEN, DyeColor.WHITE, DyeColor.BROWN};
		DyeColor c = cols[r.nextInt(cols.length)];
		for (int[] p : new int[][]{{0, 0}, {4, 0}, {0, 4}, {4, 4}}) {
			b.set(p[0], 1, p[1], Blocks.OAK_FENCE.defaultBlockState());
			b.set(p[0], 2, p[1], Blocks.OAK_FENCE.defaultBlockState());
		}
		for (int x = 0; x <= 4; x++) {
			for (int z = 0; z <= 4; z++) {
				b.set(x, 3, z, Blocks.WOOL_SLAB.pick((x + z) % 2 == 0 ? c : DyeColor.WHITE).defaultBlockState());
			}
		}
		BlockState[] goods = {Blocks.BARREL.defaultBlockState(), Blocks.HAY_BLOCK.defaultBlockState(), Blocks.PUMPKIN.defaultBlockState(),
				Blocks.MELON.defaultBlockState(), Blocks.DECORATED_POT.defaultBlockState(), Blocks.CHEST.defaultBlockState(),
				Blocks.LOOM.defaultBlockState(), Blocks.COMPOSTER.defaultBlockState()};
		for (int x = 1; x <= 3; x++) {
			b.set(x, 1, 1, Bp.slab(Blocks.OAK_SLAB, true));
			if (r.nextBoolean()) {
				b.set(x, 2, 1, goods[r.nextInt(goods.length)]);
			}
		}
		b.set(2, 1, 3, goods[r.nextInt(goods.length)]);
	}

	/** A village well under a little roof. */
	static void well(Bp b, Palette p) {
		for (int x = 0; x <= 4; x++) {
			for (int z = 0; z <= 4; z++) {
				boolean ring = x == 0 || x == 4 || z == 0 || z == 4;
				b.set(x, 0, z, Blocks.COBBLESTONE.defaultBlockState());
				if (x >= 1 && x <= 3 && z >= 1 && z <= 3) {
					boolean inner = x == 2 && z == 2;
					for (int y = -6; y <= 0; y++) {
						b.set(x, y, z, inner ? Blocks.WATER.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
					}
					b.set(x, 1, z, inner ? Blocks.AIR.defaultBlockState() : Blocks.COBBLESTONE_WALL.defaultBlockState());
				}
				if (ring) {
					b.set(x, 0, z, Blocks.GRAVEL.defaultBlockState());
				}
			}
		}
		b.set(1, 2, 2, p.fence.defaultBlockState());
		b.set(3, 2, 2, p.fence.defaultBlockState());
		b.set(1, 3, 2, p.fence.defaultBlockState());
		b.set(3, 3, 2, p.fence.defaultBlockState());
		b.set(2, 3, 2, Bp.log(p.log, Direction.Axis.X));
		b.set(2, 2, 2, Blocks.IRON_CHAIN.defaultBlockState());
		for (int x = 0; x <= 4; x++) {
			b.set(x, 4, 1, Bp.stairs(p.roofStairs, Direction.SOUTH, false));
			b.set(x, 4, 3, Bp.stairs(p.roofStairs, Direction.NORTH, false));
			b.set(x, 4, 2, p.roofFull);
		}
	}

	/** A long row of monks' cells. */
	static void cells(Bp b, Palette p) {
		int x0 = 1, x1 = b.w - 2, z0 = 1, z1 = b.d - 2;
		b.fill(x0, -2, z0, x1, -1, z1, Blocks.COBBLESTONE.defaultBlockState());
		b.fill(x0, 0, z0, x1, 0, z1, p.floor);
		for (int y = 1; y <= 3; y++) {
			for (int x = x0; x <= x1; x++) {
				b.set(x, y, z0, p.stone);
				b.set(x, y, z1, p.stone);
			}
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y, z, p.stone);
				b.set(x1, y, z, p.stone);
				if ((z - z0) % 4 == 0) {
					for (int x = x0; x <= x1; x++) {
						b.set(x, y, z, p.stone);
					}
				}
			}
		}
		b.gableRoof(x0, x1, z0, z1, 4, false, 1, p.roofStairs, p.roofSlab, p.roofFull, p.stone);
		for (int z = z0 + 2; z < z1; z += 4) {
			b.door(p.door, x1, 1, z, Direction.EAST);
			b.bed(Blocks.BED.pick(DyeColor.GRAY), x0 + 1, 1, z - 1, Direction.SOUTH);
			b.set(x0 + 1, 1, z + 1, Blocks.LECTERN.defaultBlockState());
		}
	}
}
