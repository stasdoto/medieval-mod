package com.stasdoto.medieval.construct;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.stasdoto.medieval.history.Culture;

/** Churches, cathedrals and mosques. The entrance faces the street (+z), the altar or mihrab is at the back. */
final class Holy {
	private Holy() {
	}

	/** A small log church of the Rus countryside: a log nave, an altar room, a steep roof and a little helmet dome. */
	static void woodenChurch(Bp b, Palette p) {
		int x0 = 2, x1 = b.w - 3, z0 = 4, z1 = b.d - 2;
		Houses.izba(b, p, x0, z0, x1, z1, 5, true);
		// Clear the house furniture: a church inside.
		for (int x = x0 + 1; x < x1; x++) {
			for (int z = z0 + 1; z < z1; z++) {
				for (int y = 1; y <= 4; y++) {
					b.air(x, y, z);
				}
			}
		}
		// Altar room at the back.
		int ax0 = x0 + 2, ax1 = x1 - 2;
		for (int y = 0; y <= 3; y++) {
			for (int x = ax0; x <= ax1; x++) {
				b.set(x, y, z0 - 3, Bp.log(p.log, Direction.Axis.X));
			}
			for (int z = z0 - 3; z <= z0; z++) {
				b.set(ax0, y, z, Bp.log(p.log, Direction.Axis.Z));
				b.set(ax1, y, z, Bp.log(p.log, Direction.Axis.Z));
			}
		}
		b.gableRoof(ax0, ax1, z0 - 3, z0, 4, false, 0, p.roofStairs, p.roofSlab, p.roofFull, p.planks.defaultBlockState());
		for (int x = ax0 + 1; x < ax1; x++) {
			for (int y = 1; y <= 3; y++) {
				b.air(x, y, z0);
			}
		}
		b.set((ax0 + ax1) / 2, 1, z0 - 2, Blocks.LECTERN.defaultBlockState());
		b.set((ax0 + ax1) / 2 - 1, 1, z0 - 2, Blocks.CANDLE.defaultBlockState());
		b.set((ax0 + ax1) / 2 + 1, 1, z0 - 2, Blocks.CANDLE.defaultBlockState());
		// Drum and dome over the middle of the roof.
		int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
		int top = 6 + (x1 - x0 + 2) / 2;
		for (int y = top - 1; y <= top + 1; y++) {
			b.set(mx, y, mz, Bp.log(p.log, Direction.Axis.Y));
			b.set(mx - 1, y, mz, Bp.log(p.log, Direction.Axis.Y));
			b.set(mx + 1, y, mz, Bp.log(p.log, Direction.Axis.Y));
			b.set(mx, y, mz - 1, Bp.log(p.log, Direction.Axis.Y));
			b.set(mx, y, mz + 1, Bp.log(p.log, Direction.Axis.Y));
		}
		b.helmet(mx, top + 2, mz, 1, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.IRON_CHAIN.defaultBlockState());
		b.cross(mx, top + 8, mz);
		// Benches are not Orthodox; candles and icons along the walls instead.
		for (int z = z0 + 2; z < z1 - 1; z += 3) {
			b.set(x0 + 1, 2, z, Bp.wallTorch(Direction.EAST));
			b.set(x1 - 1, 2, z, Bp.wallTorch(Direction.WEST));
		}
	}

	/**
	 * A stone church of the cross-in-square plan (Rus and Byzantium): plinth brick with stone bands, three apses at the
	 * back, a central drum with a dome and, for a cathedral, four more domes and galleries.
	 */
	static void crossDomed(Bp b, Palette p, boolean cathedral, boolean rus) {
		BlockState brick = Blocks.BRICKS.defaultBlockState();
		BlockState band = Blocks.STONE_BRICKS.defaultBlockState();
		BlockState plaster = rus && cathedral ? Blocks.CALCITE.defaultBlockState() : brick;
		int x0 = 2, x1 = b.w - 3, z0 = cathedral ? 6 : 5, z1 = b.d - 2;
		int h = cathedral ? 14 : 10;
		b.fill(x0, -4, z0 - 4, x1, -1, z1, Blocks.STONE_BRICKS.defaultBlockState());
		b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, Blocks.POLISHED_ANDESITE.defaultBlockState());
		for (int y = 1; y <= h; y++) {
			BlockState s = y % 3 == 0 ? band : (cathedral && rus ? plaster : brick);
			for (int x = x0; x <= x1; x++) {
				b.set(x, y, z0, s);
				b.set(x, y, z1, s);
			}
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y, z, s);
				b.set(x1, y, z, s);
			}
		}
		// Flat stepped roof over the vaults.
		b.fill(x0, h + 1, z0, x1, h + 1, z1, Blocks.STONE_BRICK_SLAB.defaultBlockState());
		// Tall arched windows.
		for (int x = x0 + 2; x < x1 - 1; x += 3) {
			for (int y = 4; y <= h - 3; y++) {
				b.set(x, y, z1, Blocks.GLASS_PANE.defaultBlockState());
				b.set(x, y, z0, Blocks.GLASS_PANE.defaultBlockState());
			}
			b.set(x, h - 2, z1, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true));
		}
		for (int z = z0 + 2; z < z1 - 1; z += 3) {
			for (int y = 4; y <= h - 3; y++) {
				b.set(x0, y, z, Blocks.GLASS_PANE.defaultBlockState());
				b.set(x1, y, z, Blocks.GLASS_PANE.defaultBlockState());
			}
		}
		// Three apses at the back.
		int mx = (x0 + x1) / 2;
		int[] apses = cathedral ? new int[]{mx - 7, mx, mx + 7} : new int[]{mx - 4, mx, mx + 4};
		for (int ac : apses) {
			double rad = ac == mx ? (cathedral ? 4 : 3) : 2.2;
			for (int x = (int) (ac - rad - 1); x <= ac + rad + 1; x++) {
				for (int z = z0 - 5; z <= z0; z++) {
					double dd = Math.hypot(x - ac, z - z0);
					if (dd <= rad + 0.4 && z < z0) {
						for (int y = 1; y <= h - 4; y++) {
							b.set(x, y, z, dd > rad - 0.6 ? brick : Blocks.AIR.defaultBlockState());
						}
						b.set(x, h - 3, z, Blocks.STONE_BRICK_SLAB.defaultBlockState());
						b.set(x, 0, z, Blocks.POLISHED_ANDESITE.defaultBlockState());
					}
				}
			}
			for (int y = 1; y <= h - 4; y++) {
				b.air(ac, y, z0);
				b.air(ac - 1, y, z0);
				b.air(ac + 1, y, z0);
			}
		}
		// Central drum and dome.
		int mz = (z0 + z1) / 2;
		int dr = cathedral ? 4 : 3;
		int drumTop = h + (cathedral ? 7 : 5);
		BlockState domeShell = cathedral ? Blocks.GOLD_BLOCK.defaultBlockState() : rus ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
				: Blocks.POLISHED_ANDESITE.defaultBlockState();
		for (int y = h + 1; y <= drumTop; y++) {
			for (int x = -dr; x <= dr; x++) {
				for (int z = -dr; z <= dr; z++) {
					double dd = Math.hypot(x, z);
					if (dd <= dr + 0.3 && dd > dr - 0.8) {
						boolean window = y > h + 2 && y < drumTop - 1 && (x == 0 || z == 0);
						b.set(mx + x, y, mz + z, window ? Blocks.GLASS_PANE.defaultBlockState() : y == drumTop ? band : brick);
					}
				}
			}
		}
		if (rus) {
			b.helmet(mx, drumTop + 1, mz, dr, domeShell, Blocks.IRON_CHAIN.defaultBlockState());
			b.cross(mx, drumTop + 9, mz);
		} else {
			b.dome(mx, drumTop + 1, mz, dr + 0.4, domeShell, Blocks.IRON_CHAIN.defaultBlockState());
			b.cross(mx, drumTop + dr + 2, mz);
		}
		// Four small domes on a cathedral.
		if (cathedral) {
			int[][] corners = {{x0 + 4, z0 + 4}, {x1 - 4, z0 + 4}, {x0 + 4, z1 - 4}, {x1 - 4, z1 - 4}};
			for (int[] c : corners) {
				for (int y = h + 1; y <= h + 4; y++) {
					for (int x = -2; x <= 2; x++) {
						for (int z = -2; z <= 2; z++) {
							if (Math.hypot(x, z) <= 2.3 && Math.hypot(x, z) > 1.2) {
								b.set(c[0] + x, y, c[1] + z, brick);
							}
						}
					}
				}
				if (rus) {
					b.helmet(c[0], h + 5, c[1], 2, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Blocks.IRON_CHAIN.defaultBlockState());
				} else {
					b.dome(c[0], h + 5, c[1], 2.4, Blocks.POLISHED_ANDESITE.defaultBlockState(), null);
				}
			}
		}
		// Portal.
		b.door(Blocks.DARK_OAK_DOOR, mx, 1, z1, Direction.SOUTH);
		b.air(mx - 1, 1, z1);
		b.air(mx - 1, 2, z1);
		b.door(Blocks.DARK_OAK_DOOR, mx - 1, 1, z1, Direction.SOUTH);
		b.set(mx, 3, z1, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true));
		b.set(mx - 1, 3, z1, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true));
		// Iconostasis and candles.
		for (int x = x0 + 2; x <= x1 - 2; x++) {
			if (Math.abs(x - mx) > 1) {
				b.set(x, 1, z0 + 2, Blocks.DARK_OAK_PLANKS.defaultBlockState());
				b.set(x, 2, z0 + 2, Blocks.DARK_OAK_PLANKS.defaultBlockState());
				b.set(x, 3, z0 + 2, x % 2 == 0 ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
		}
		b.set(mx, 1, z0 - 2, Blocks.LECTERN.defaultBlockState());
		for (int x = x0 + 3; x < x1 - 2; x += 4) {
			b.set(x, 1, z1 - 3, Blocks.CANDLE.defaultBlockState());
		}
		b.set(mx, h, mz, Bp.lantern(true));
		b.set(mx, drumTop, mz, Bp.lantern(true));
	}

	/** A Romanesque church of the West: a stone nave, a round apse and a square west tower with a pyramid roof. */
	static void romanesque(Bp b, Palette p, boolean big) {
		BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
		BlockState rough = Blocks.COBBLESTONE.defaultBlockState();
		int x0 = 2, x1 = b.w - 3;
		int tower = big ? 7 : 5;
		int z1 = b.d - 2 - tower;
		int z0 = big ? 6 : 4;
		int h = big ? 10 : 7;
		b.fill(x0, -4, z0 - 4, x1, -1, b.d - 2, rough);
		b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1, Blocks.SMOOTH_STONE.defaultBlockState());
		for (int y = 1; y <= h; y++) {
			for (int x = x0; x <= x1; x++) {
				b.set(x, y, z0, b.r.nextInt(7) == 0 ? rough : wall);
			}
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y, z, b.r.nextInt(7) == 0 ? rough : wall);
				b.set(x1, y, z, b.r.nextInt(7) == 0 ? rough : wall);
			}
		}
		for (int z = z0 + 2; z < z1; z += 3) {
			for (int y = 3; y <= h - 2; y++) {
				b.set(x0, y, z, Blocks.GLASS_PANE.defaultBlockState());
				b.set(x1, y, z, Blocks.GLASS_PANE.defaultBlockState());
			}
		}
		b.gableRoof(x0, x1, z0, z1, h + 1, false, 1, p.roofStairs, p.roofSlab, p.roofFull, wall);
		// Round apse.
		int mx = (x0 + x1) / 2;
		double rad = (x1 - x0) / 2.0 - 1.5;
		for (int x = (int) (mx - rad - 1); x <= mx + rad + 1; x++) {
			for (int z = (int) (z0 - rad - 1); z < z0; z++) {
				double dd = Math.hypot(x - mx, z - z0);
				if (dd <= rad + 0.4) {
					for (int y = 1; y <= h - 2; y++) {
						b.set(x, y, z, dd > rad - 0.6 ? wall : Blocks.AIR.defaultBlockState());
					}
					b.set(x, h - 1, z, Bp.slab(p.roofSlab, false));
					b.set(x, 0, z, Blocks.SMOOTH_STONE.defaultBlockState());
				}
			}
		}
		for (int x = mx - (int) rad + 1; x <= mx + (int) rad - 1; x++) {
			for (int y = 1; y <= h - 2; y++) {
				b.air(x, y, z0);
			}
		}
		b.set(mx, 1, z0 - 2, Blocks.LECTERN.defaultBlockState());
		b.set(mx - 1, 1, z0 - 1, Blocks.CANDLE.defaultBlockState());
		b.set(mx + 1, 1, z0 - 1, Blocks.CANDLE.defaultBlockState());
		// Pews.
		for (int z = z0 + 3; z < z1 - 1; z += 2) {
			for (int x = x0 + 2; x < x1 - 1; x++) {
				if (Math.abs(x - mx) > 1) {
					b.set(x, 1, z, Bp.stairs(Blocks.OAK_STAIRS, Direction.SOUTH, false));
				}
			}
		}
		// West tower (at the street).
		int tx0 = mx - tower / 2, tx1 = mx + tower / 2, tz0 = z1, tz1 = z1 + tower - 1;
		int th = h + (big ? 12 : 8);
		for (int y = 0; y <= th; y++) {
			for (int x = tx0; x <= tx1; x++) {
				for (int z = tz0; z <= tz1; z++) {
					boolean edge = x == tx0 || x == tx1 || z == tz0 || z == tz1;
					if (edge) {
						b.set(x, y, z, b.r.nextInt(8) == 0 ? rough : wall);
					} else if (y == 0) {
						b.set(x, y, z, Blocks.SMOOTH_STONE.defaultBlockState());
					} else {
						b.air(x, y, z);
					}
				}
			}
		}
		// Belfry openings.
		for (int y = th - 3; y <= th - 1; y++) {
			b.air(mx, y, tz0);
			b.air(mx, y, tz1);
			b.air(tx0, y, (tz0 + tz1) / 2);
			b.air(tx1, y, (tz0 + tz1) / 2);
		}
		b.set(mx, th - 2, (tz0 + tz1) / 2, Blocks.BELL.defaultBlockState());
		b.set(mx, th - 1, (tz0 + tz1) / 2, Blocks.OAK_PLANKS.defaultBlockState());
		b.pyramidRoof(tx0 - 1, tx1 + 1, tz0 - 1, tz1 + 1, th + 1, p.roofStairs, p.roofFull, Blocks.LIGHTNING_ROD.defaultBlockState());
		// Door through the tower into the nave.
		b.door(Blocks.DARK_OAK_DOOR, mx, 1, tz1, Direction.SOUTH);
		b.set(mx, 3, tz1, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true));
		for (int y = 1; y <= 3; y++) {
			b.air(mx, y, tz0);
		}
		b.set(mx, h - 1, (z0 + z1) / 2, Bp.lantern(true));
	}

	/**
	 * A mosque: a walled courtyard with a fountain, a prayer hall with arcades, a dome over the mihrab and a minaret
	 * (square in the West, round in the East).
	 */
	static void mosque(Bp b, Palette p, Culture c) {
		BlockState wall = p.stone;
		BlockState light = p.plaster;
		int x0 = 1, x1 = b.w - 2, z0 = 1, z1 = b.d - 2;
		int hallEnd = z0 + (z1 - z0) / 2;
		int h = 7;
		b.fill(x0, -2, z0, x1, -1, z1, p.stone2);
		b.fill(x0, 0, z0, x1, 0, z1, p.floor);
		for (int y = 1; y <= h; y++) {
			for (int x = x0; x <= x1; x++) {
				b.set(x, y, z0, wall);
				b.set(x, y, z1, wall);
			}
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y, z, wall);
				b.set(x1, y, z, wall);
			}
		}
		// Prayer hall roof and arcade toward the courtyard.
		b.fill(x0, h, z0, x1, h, hallEnd, light);
		for (int x = x0 + 1; x < x1; x++) {
			for (int y = 1; y < h; y++) {
				boolean pillar = (x - x0) % 3 == 0;
				b.set(x, y, hallEnd, pillar || y >= 5 ? light : Blocks.AIR.defaultBlockState());
			}
			if ((x - x0) % 3 != 0) {
				b.set(x, 4, hallEnd, Bp.stairs(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.SOUTH, true));
			}
		}
		// Carpets in the hall, the mihrab niche in the back wall.
		for (int x = x0 + 1; x < x1; x++) {
			for (int z = z0 + 1; z < hallEnd; z++) {
				b.set(x, 1, z, net.minecraft.world.level.block.Blocks.CARPET.pick((x + z) % 2 == 0 ? net.minecraft.world.item.DyeColor.RED
						: net.minecraft.world.item.DyeColor.GREEN).defaultBlockState());
			}
		}
		int mx = (x0 + x1) / 2;
		b.air(mx, 1, z0);
		b.air(mx, 2, z0);
		b.set(mx, 1, z0 - 1, light);
		b.set(mx, 2, z0 - 1, light);
		b.set(mx, 3, z0, Bp.stairs(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.SOUTH, true));
		b.set(mx, 3, z0 + 1, Bp.lantern(false));
		// Dome.
		int dz = z0 + 4;
		b.dome(mx, h + 1, dz, 3.5, c == Culture.PERSIAN ? Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.CYAN).defaultBlockState()
				: light, Blocks.GOLD_BLOCK.defaultBlockState());
		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) {
				if (Math.hypot(x, z) <= 3.4) {
					b.set(mx + x, h, dz + z, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
				}
			}
		}
		// Courtyard with a fountain.
		int cz = (hallEnd + z1) / 2;
		b.fill(mx - 1, 0, cz - 1, mx + 1, 0, cz + 1, Blocks.WATER.defaultBlockState());
		for (int x = mx - 2; x <= mx + 2; x++) {
			b.set(x, 0, cz - 2, Blocks.CUT_SANDSTONE.defaultBlockState());
			b.set(x, 0, cz + 2, Blocks.CUT_SANDSTONE.defaultBlockState());
		}
		for (int z = cz - 2; z <= cz + 2; z++) {
			b.set(mx - 2, 0, z, Blocks.CUT_SANDSTONE.defaultBlockState());
			b.set(mx + 2, 0, z, Blocks.CUT_SANDSTONE.defaultBlockState());
		}
		// Gate.
		for (int y = 1; y <= 4; y++) {
			b.air(mx, y, z1);
			b.air(mx - 1, y, z1);
		}
		b.set(mx, 5, z1, Bp.stairs(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.SOUTH, true));
		// Minaret at the front corner.
		int mnx = x1 - 2, mnz = z1 - 2;
		int mh = h + 16;
		boolean round = c == Culture.PERSIAN || c == Culture.ARAB;
		for (int y = 1; y <= mh; y++) {
			for (int x = -2; x <= 2; x++) {
				for (int z = -2; z <= 2; z++) {
					boolean edge = round ? Math.hypot(x, z) > 1.2 && Math.hypot(x, z) <= 2.3 : Math.abs(x) == 2 || Math.abs(z) == 2;
					if (edge) {
						b.set(mnx + x, y, mnz + z, y % 6 == 0 ? p.stone2 : wall);
					}
				}
			}
		}
		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) {
				if (round ? Math.hypot(x, z) <= 3.2 : true) {
					b.set(mnx + x, mh + 1, mnz + z, Bp.slab(Blocks.SMOOTH_SANDSTONE_SLAB, false));
				}
			}
		}
		for (int y = mh + 2; y <= mh + 4; y++) {
			b.set(mnx, y, mnz, wall);
		}
		b.dome(mnx, mh + 5, mnz, 1.4, light, Blocks.GOLD_BLOCK.defaultBlockState());
		b.set(mnx, mh + 2, mnz + 1, Bp.lantern(false));
	}

	/** A Norse church: a dark stave building with stacked steep roofs. */
	static void stave(Bp b, Palette p) {
		int x0 = 2, x1 = b.w - 3, z0 = 3, z1 = b.d - 2;
		b.fill(x0, -2, z0, x1, -1, z1, Blocks.COBBLESTONE.defaultBlockState());
		b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, Blocks.DARK_OAK_PLANKS.defaultBlockState());
		for (int y = 1; y <= 5; y++) {
			for (int x = x0; x <= x1; x++) {
				b.set(x, y, z0, Blocks.DARK_OAK_PLANKS.defaultBlockState());
				b.set(x, y, z1, Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y, z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
				b.set(x1, y, z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
		}
		b.gableRoof(x0, x1, z0, z1, 6, false, 1, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_PLANKS.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState());
		int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
		int top = 6 + (x1 - x0 + 2) / 2;
		b.fill(mx - 1, top, mz - 1, mx + 1, top + 3, mz + 1, Blocks.DARK_OAK_PLANKS.defaultBlockState());
		b.pyramidRoof(mx - 2, mx + 2, mz - 2, mz + 2, top + 4, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS.defaultBlockState(), Blocks.LIGHTNING_ROD.defaultBlockState());
		b.door(Blocks.DARK_OAK_DOOR, mx, 1, z1, Direction.SOUTH);
		b.set(mx, 1, z0 + 1, Blocks.LECTERN.defaultBlockState());
		b.set(mx, 4, mz, Bp.lantern(true));
		// Dragon heads on the gables.
		b.set(mx, top + 1, z1 + 2, Blocks.DARK_OAK_FENCE.defaultBlockState());
		b.set(mx, top + 1, z0 - 2, Blocks.DARK_OAK_FENCE.defaultBlockState());
	}
}
