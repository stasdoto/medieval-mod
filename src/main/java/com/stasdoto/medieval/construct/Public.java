package com.stasdoto.medieval.construct;

import static com.stasdoto.medieval.construct.Shore.*;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.stasdoto.medieval.history.Culture;

/**
 * Public buildings and places, drawn facing the street (+z): bath houses, caravanserais and гостиные дворы, barracks,
 * towers, wayside shrines, cemeteries and gardens.
 */
final class Public {
	private Public() {
	}

	private static boolean islamic(Culture c) {
		return c == Culture.ANDALUSI || c == Culture.ARAB || c == Culture.PERSIAN;
	}

	// ================================================================ 1. bath house

	static void bathhouse(Bp b, Palette p, Culture c) {
		switch (c) {
			case RUS, BALT_FINN -> banya(b, p, c);
			case ARAB, ANDALUSI, PERSIAN, BYZANTINE -> hammam(b, p, c);
			default -> washHouse(b, p, c);
		}
	}

	/** Moss on top of a gable roof (ridge along x) whose eaves run from za to zb. */
	private static void turf(Bp b, int xa, int xb, int za, int zb, int y0) {
		for (int l = 0; za + l <= zb - l; l++) {
			for (int x = xa; x <= xb; x++) {
				soft(b, x, y0 + l + 1, za + l, Blocks.MOSS_CARPET.defaultBlockState());
				soft(b, x, y0 + l + 1, zb - l, Blocks.MOSS_CARPET.defaultBlockState());
			}
		}
	}

	/**
	 * A Rus баня (Finnish sauna): a low log house, a steam room with the stone stove (каменка) — either an open hearth
	 * "по-чёрному" whose smoke leaves by a hole, or a stove with a chimney — the high steam bench (полок), water tubs, and
	 * a changing room (предбанник) at the front; birch firewood outside.
	 */
	private static void banya(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d;
		int m = w >= 8 && d >= 8 ? 1 : 0;
		int x0 = m, x1 = w - 1 - m, z0 = m, z1 = d - 1 - m - (d >= 10 ? 1 : 0);
		if (x1 - x0 < 4 || z1 - z0 < 4) {
			return;
		}
		int h = 4;
		b.fill(x0, -2, z0, x1, -1, z1, Blocks.COBBLESTONE.defaultBlockState());
		b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, p.planks.defaultBlockState());
		box(b, p, c, LOG, x0, z0, x1, z1, h);
		b.fill(x0 + 1, h, z0 + 1, x1 - 1, h, z1 - 1, p.planks.defaultBlockState());
		roof(b, p, c, false, x0, x1, z0, z1, h + 1, true, m);
		if (c == Culture.BALT_FINN ? b.r.nextBoolean() : b.r.nextInt(4) == 0) {
			turf(b, x0 - m, x1 + m, z0 - m, z1 + m, h + 1);
		}
		// The partition between the steam room and the changing room.
		int n = z1 - z0 - 1;
		int zp = Math.min(z1 - 2, z0 + 1 + Math.max(2, n / 2));
		for (int y = 0; y < h; y++) {
			for (int x = x0 + 1; x < x1; x++) {
				b.set(x, y, zp, Bp.log(p.log, Direction.Axis.X));
			}
		}
		int dx = x1 - 1, mx = (x0 + x1) / 2;
		b.door(p.door, dx, 1, zp, Direction.SOUTH);
		b.door(p.door, mx, 1, z1, Direction.SOUTH);
		for (int y = 1; y <= 2; y++) {
			b.air(dx, y, zp - 1);
			b.air(dx, y, zp + 1);
			b.air(mx, y, z1 - 1);
		}
		// The stove in the back corner.
		int sx = x0 + 1, sz = z0 + 1;
		if (b.r.nextBoolean()) {
			// По-чёрному: stones heaped over an open fire, the smoke goes out by a hole under the roof.
			b.set(sx, 1, sz, Blocks.COBBLESTONE.defaultBlockState());
			b.set(sx + 1, 1, sz, Blocks.CAMPFIRE.defaultBlockState());
			b.set(sx, 2, sz, Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState());
			b.set(sx + 1, 2, sz, Blocks.COBBLESTONE_WALL.defaultBlockState());
			b.air(sx + 1, h - 1, z0);
		} else {
			b.set(sx, 1, sz, lit(Blocks.FURNACE, Direction.SOUTH));
			b.set(sx + 1, 1, sz, Blocks.COBBLESTONE.defaultBlockState());
			b.set(sx, 2, sz, Blocks.COBBLESTONE.defaultBlockState());
			b.set(sx + 1, 2, sz, Blocks.COBBLESTONE_WALL.defaultBlockState());
			int top = h + 4 + m;
			for (int y = 3; y <= top; y++) {
				b.set(sx, y, sz, Blocks.COBBLESTONE.defaultBlockState());
			}
			b.set(sx, top + 1, sz, Blocks.CAMPFIRE.defaultBlockState());
		}
		// The steam bench: a high shelf against the back wall and a step below it.
		Block slab = woodSlab(p);
		for (int x = x0 + 3; x <= x1 - 2; x++) {
			soft(b, x, 1, z0 + 1, p.planks.defaultBlockState());
			soft(b, x, 2, z0 + 1, Bp.slab(slab, false));
			if (z0 + 2 < zp) {
				soft(b, x, 1, z0 + 2, Bp.slab(slab, true));
			}
		}
		if (zp - 1 > z0 + 1) {
			soft(b, x0 + 1, 1, zp - 1, waterCauldron());
			soft(b, x0 + 2, 1, zp - 1, barrel(Direction.UP));
		}
		b.air(x1, 2, z0 + 1);
		soft(b, dx, h - 1, z0 + 1, Bp.lantern(true));
		// The changing room: a bench, a chest, towels, a jug of kvass.
		Block ws = woodStairs(p);
		for (int z = zp + 1; z <= z1 - 1; z++) {
			soft(b, x0 + 1, 1, z, Bp.stairs(ws, Direction.WEST, false));
		}
		soft(b, x1 - 1, 1, z1 - 1, Blocks.CHEST.defaultBlockState());
		soft(b, x0 + 2, 1, z1 - 1, carpet(DyeColor.WHITE));
		soft(b, mx, h - 1, Math.max(zp + 1, z1 - 1), Bp.lantern(true));
		b.air(x0, 2, z1 - 1);
		furnish(b, c, x0 + 1, zp + 1, x1 - 1, z1 - 1, 1, Interior.Use.STORE, 0);
		// Outside: birch firewood, water barrels, a bench by the door or a little porch.
		BlockState birch = Bp.log(Blocks.BIRCH_LOG, Direction.Axis.X);
		if (z1 + 1 <= d - 1) {
			int fz = d - 1;
			for (int x = x0; x <= Math.min(mx - 2, x0 + 3); x++) {
				soft(b, x, 1, fz, birch);
				if (x > x0 && x < Math.min(mx - 2, x0 + 3)) {
					soft(b, x, 2, fz, birch);
				}
			}
			if (b.r.nextBoolean()) {
				soft(b, mx + 1, 1, z1 + 1, waterCauldron());
				soft(b, mx + 2, 1, z1 + 1, barrel(Direction.UP));
			} else {
				// A porch over the door.
				for (int y = 1; y <= 2; y++) {
					soft(b, mx - 1, y, z1 + 1, p.fence.defaultBlockState());
					soft(b, mx + 1, y, z1 + 1, p.fence.defaultBlockState());
				}
				for (int x = mx - 1; x <= mx + 1; x++) {
					soft(b, x, 3, z1 + 1, Bp.slab(slab, false));
				}
				soft(b, mx + 2, 1, z1 + 1, barrel(Direction.UP));
			}
		}
		if (m == 1) {
			for (int z = z0 + 1; z <= z1 - 1; z++) {
				soft(b, w - 1, 1, z, Bp.log(Blocks.BIRCH_LOG, Direction.Axis.Z));
				if (z > z0 + 1 && z < z1 - 1) {
					soft(b, w - 1, 2, z, Bp.log(Blocks.BIRCH_LOG, Direction.Axis.Z));
				}
			}
		}
	}

	/** A dome like Bp.dome, but some of its shell is glass (the star-shaped light holes of a hammam) and it opens the roof under it. */
	private static void domeLit(Bp b, int cx, int y, int cz, double rad, BlockState shell, BlockState glass, int pattern) {
		int ri = (int) Math.ceil(rad);
		for (int dy = 0; dy <= ri; dy++) {
			for (int x = cx - ri; x <= cx + ri; x++) {
				for (int z = cz - ri; z <= cz + ri; z++) {
					double dd = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz) + dy * dy);
					if (dd <= rad && dd > rad - 1.3) {
						boolean hole = dy >= 1 && Math.floorMod(x * 3 + z * 5 + dy + pattern, 4) == 0;
						put(b, x, y + dy, z, hole ? glass : shell);
					} else if (dd <= rad - 1.3 && dy == 0) {
						put(b, x, y, z, AIR);
					}
				}
			}
		}
		int top = y + (int) Math.floor(rad);
		if (top - 1 >= y) {
			put(b, cx, top - 1, cz, Bp.lantern(true));
		}
	}

	/**
	 * A hammam (and a Byzantine bath): stone or brick, a changing hall at the front, behind it the hot room with a warm
	 * pool, on wide plots also a warm room with the heated platform in the middle; every room under a dome pierced by
	 * coloured glass, the furnace in the back wall with its chimney.
	 */
	private static void hammam(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d;
		int m = w >= 11 && d >= 11 ? 1 : 0;
		int x0 = m, x1 = w - 1 - m, z0 = m, z1 = d - 1 - m;
		if (x1 - x0 < 5 || z1 - z0 < 5) {
			return;
		}
		int h = 4;
		boolean byz = c == Culture.BYZANTINE;
		BlockState wall = p.stone, band = p.stone2;
		Block bench = byz ? Blocks.POLISHED_ANDESITE_STAIRS : c == Culture.ANDALUSI ? Blocks.POLISHED_DIORITE_STAIRS : Blocks.SMOOTH_SANDSTONE_STAIRS;
		BlockState shell = switch (c) {
			case PERSIAN -> b.r.nextBoolean() ? Blocks.DYED_TERRACOTTA.pick(DyeColor.CYAN).defaultBlockState() : p.stone;
			case ANDALUSI -> Blocks.CALCITE.defaultBlockState();
			case BYZANTINE -> b.r.nextBoolean() ? Blocks.BRICKS.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState();
			default -> p.plaster;
		};
		DyeColor gc = c == Culture.ANDALUSI ? DyeColor.YELLOW : c == Culture.PERSIAN ? DyeColor.LIGHT_BLUE
				: b.r.nextBoolean() ? DyeColor.WHITE : DyeColor.YELLOW;
		BlockState glass = Blocks.STAINED_GLASS.pick(gc).defaultBlockState();
		BlockState floorA = p.floor;
		BlockState floorB = byz ? Blocks.POLISHED_DIORITE.defaultBlockState() : c == Culture.ANDALUSI ? Blocks.CALCITE.defaultBlockState()
				: Blocks.SMOOTH_STONE.defaultBlockState();
		b.fill(x0, -2, z0, x1, -1, z1, band);
		for (int x = x0 + 1; x < x1; x++) {
			for (int z = z0 + 1; z < z1; z++) {
				b.set(x, 0, z, (x + z) % 2 == 0 ? floorA : floorB);
			}
		}
		for (int y = 0; y <= h; y++) {
			BlockState s = y == 0 || y == h || (byz && y % 3 == 0) ? band : wall;
			for (int x = x0; x <= x1; x++) {
				b.set(x, y, z0, s);
				b.set(x, y, z1, s);
			}
			for (int z = z0; z <= z1; z++) {
				b.set(x0, y, z, s);
				b.set(x1, y, z, s);
			}
		}
		b.fill(x0, h + 1, z0, x1, h + 1, z1, wall);
		for (int x = x0; x <= x1; x++) {
			b.set(x, h + 2, z0, Bp.slab(p.roofSlab, false));
			b.set(x, h + 2, z1, Bp.slab(p.roofSlab, false));
		}
		for (int z = z0; z <= z1; z++) {
			b.set(x0, h + 2, z, Bp.slab(p.roofSlab, false));
			b.set(x1, h + 2, z, Bp.slab(p.roofSlab, false));
		}
		int zf = Math.max(z0 + 3, z1 - 3);
		for (int y = 1; y <= h; y++) {
			for (int x = x0 + 1; x < x1; x++) {
				b.set(x, y, zf, wall);
			}
		}
		int mx = (x0 + x1) / 2;
		boolean split = x1 - x0 >= 10;
		int xm = mx;
		// Doors: the street door with an arch, the inner doors.
		b.door(p.door, mx, 1, z1, Direction.SOUTH);
		b.set(mx, 3, z1, Bp.stairs(bench, Direction.SOUTH, true));
		if (c == Culture.ANDALUSI) {
			b.set(mx - 1, 3, z1, Blocks.BRICKS.defaultBlockState());
			b.set(mx + 1, 3, z1, Blocks.BRICKS.defaultBlockState());
			b.set(mx, 4, z1, Blocks.BRICKS.defaultBlockState());
		} else if (c == Culture.PERSIAN) {
			for (int x = x0; x <= x1; x += 2) {
				b.set(x, h, z1, Blocks.GLAZED_TERRACOTTA.pick(DyeColor.LIGHT_BLUE).defaultBlockState());
			}
		}
		b.air(mx, 1, z1 - 1);
		b.air(mx, 2, z1 - 1);
		int warmX0 = x0 + 1, warmX1 = xm - 1, hotX0 = split ? xm + 1 : x0 + 1, hotX1 = x1 - 1;
		int rz0 = z0 + 1, rz1 = zf - 1;
		if (split) {
			for (int y = 1; y <= h; y++) {
				for (int z = z0 + 1; z < zf; z++) {
					b.set(xm, y, z, wall);
				}
			}
			int dw = (x0 + xm) / 2, dz = (rz0 + rz1) / 2;
			b.door(p.door, dw, 1, zf, Direction.SOUTH);
			b.door(p.door, xm, 1, dz, Direction.EAST);
			for (int y = 1; y <= 2; y++) {
				b.air(dw, y, zf - 1);
				b.air(dw, y, zf + 1);
				b.air(xm - 1, y, dz);
				b.air(xm + 1, y, dz);
			}
		} else {
			b.door(p.door, mx, 1, zf, Direction.SOUTH);
			for (int y = 1; y <= 2; y++) {
				b.air(mx, y, zf - 1);
				b.air(mx, y, zf + 1);
			}
		}

		// The hot room: the warm pool, stone benches all round, the furnace in the back wall.
		if (hotX1 - hotX0 >= 2 && rz1 - rz0 >= 2) {
			for (int x = hotX0 + 1; x <= hotX1 - 1; x++) {
				for (int z = rz0 + 1; z <= rz1 - 1; z++) {
					if (!b.has(x, 1, z)) {
						b.set(x, 0, z, Blocks.WATER.defaultBlockState());
					}
				}
			}
		} else {
			soft(b, (hotX0 + hotX1) / 2, 1, (rz0 + rz1) / 2, waterCauldron());
		}
		int fx = (hotX0 + hotX1) / 2;
		for (int x = hotX0; x <= hotX1; x++) {
			if (x != fx) {
				soft(b, x, 1, rz0, Bp.stairs(bench, Direction.NORTH, false));
			}
		}
		for (int z = rz0 + 1; z <= rz1; z++) {
			soft(b, hotX0, 1, z, Bp.stairs(bench, Direction.WEST, false));
			soft(b, hotX1, 1, z, Bp.stairs(bench, Direction.EAST, false));
		}
		b.set(fx, 1, z0, lit(Blocks.FURNACE, Direction.SOUTH));
		for (int y = h + 1; y <= h + 4; y++) {
			b.set(fx, y, z0, band);
		}
		b.set(fx, h + 5, z0, Blocks.CAMPFIRE.defaultBlockState());
		int pat = b.r.nextInt(4);
		double hr = Math.min((hotX1 - hotX0) / 2.0 + 1, (rz1 - rz0) / 2.0 + 1) + 0.2;
		domeLit(b, (hotX0 + hotX1) / 2, h + 1, (rz0 + rz1) / 2, hr, shell, glass, pat);

		// The warm room: the heated marble platform in the middle, basins at the walls.
		if (split && warmX1 - warmX0 >= 2) {
			Block plat = byz ? Blocks.POLISHED_DIORITE_SLAB : Blocks.SMOOTH_STONE_SLAB;
			for (int x = warmX0 + 1; x <= warmX1 - 1; x++) {
				for (int z = rz0 + 1; z <= rz1 - 1; z++) {
					soft(b, x, 1, z, Bp.slab(plat, false));
				}
			}
			soft(b, warmX0, 1, rz0, waterCauldron());
			soft(b, warmX1, 1, rz0, waterCauldron());
			for (int x = warmX0 + 1; x < warmX1; x++) {
				soft(b, x, 1, rz0, Bp.stairs(bench, Direction.NORTH, false));
			}
			double wr = Math.min((warmX1 - warmX0) / 2.0 + 1, (rz1 - rz0) / 2.0 + 1) + 0.2;
			domeLit(b, (warmX0 + warmX1) / 2, h + 1, (rz0 + rz1) / 2, wr, shell, glass, pat + 1);
		}

		// The changing hall: benches, carpets, chests and jars, a little basin.
		int hz0 = zf + 1, hz1 = z1 - 1;
		for (int z = hz0; z <= hz1; z++) {
			soft(b, x0 + 1, 1, z, Bp.stairs(bench, Direction.WEST, false));
			soft(b, x1 - 1, 1, z, Bp.stairs(bench, Direction.EAST, false));
		}
		DyeColor rug = c == Culture.PERSIAN ? DyeColor.BLUE : c == Culture.BYZANTINE ? DyeColor.PURPLE : DyeColor.RED;
		for (int x = x0 + 2; x <= x1 - 2; x++) {
			for (int z = hz0; z <= hz1; z++) {
				if (x != mx && (x + z) % 3 != 0) {
					soft(b, x, 1, z, carpet(rug));
				}
			}
		}
		soft(b, x0 + 2, 1, hz1, Blocks.DECORATED_POT.defaultBlockState());
		soft(b, x1 - 2, 1, hz1, Blocks.CHEST.defaultBlockState());
		double fr = Math.min((x1 - x0 - 2) / 2.0 + 1, (hz1 - hz0) / 2.0 + 1) + 0.2;
		domeLit(b, mx, h + 1, (hz0 + hz1) / 2, fr, shell, glass, pat + 2);
		furnish(b, c, x0 + 1, hz0, x1 - 1, hz1, 1, Interior.Use.HALL, 1);
	}

	/** A plain bath house of the West (and the Caucasus, the Slavs, the Magyars, the steppe): a wooden tub, a kettle on the fire. */
	private static void washHouse(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d;
		int m = w >= 8 && d >= 8 ? 1 : 0;
		int x0 = m, x1 = w - 1 - m, z0 = m, z1 = d - 1 - m;
		if (x1 - x0 < 4 || z1 - z0 < 4) {
			return;
		}
		int st = style(c, b.r);
		boolean flat = st == STONE && flatRoof(c, b.r);
		int h = 4;
		b.fill(x0, -2, z0, x1, -1, z1, Blocks.COBBLESTONE.defaultBlockState());
		b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, st == STONE ? p.floor : p.planks.defaultBlockState());
		box(b, p, c, st, x0, z0, x1, z1, h);
		int mz = (z0 + z1) / 2, mx = (x0 + x1) / 2;
		if (!flat) {
			for (int x = x0; x <= x1; x++) {
				b.set(x, h, mz, Bp.log(p.strippedLog, Direction.Axis.X));
			}
		}
		roof(b, p, c, flat, x0, x1, z0, z1, h + 1, true, m);
		b.door(p.door, mx, 1, z1, Direction.SOUTH);
		b.air(mx, 1, z1 - 1);
		b.air(mx, 2, z1 - 1);
		b.air(x0, 2, mz);
		b.air(x1, 2, mz);
		int ix0 = x0 + 1, ix1 = x1 - 1, iz0 = z0 + 1, iz1 = z1 - 1;
		// The tub: a ring of staves holding the water.
		int tx0 = ix0, tx1 = Math.min(ix1 - 1, ix0 + 3), tz0 = iz0, tz1 = Math.min(iz1 - 1, iz0 + 3);
		if (tx1 - tx0 >= 2 && tz1 - tz0 >= 2) {
			for (int x = tx0; x <= tx1; x++) {
				for (int z = tz0; z <= tz1; z++) {
					boolean rim = x == tx0 || x == tx1 || z == tz0 || z == tz1;
					b.set(x, 1, z, rim ? Bp.log(p.strippedLog, Direction.Axis.Y) : Blocks.WATER.defaultBlockState());
				}
			}
		} else {
			soft(b, ix0, 1, iz0, waterCauldron());
		}
		// The kettle over the fire, a stove, benches and towels.
		b.set(ix1, 0, iz0, Blocks.CAMPFIRE.defaultBlockState());
		b.set(ix1, 1, iz0, waterCauldron());
		soft(b, ix1, 1, iz0 + 1, lit(Blocks.FURNACE, Direction.WEST));
		for (int x = ix0; x < mx; x++) {
			soft(b, x, 1, iz1, Bp.stairs(woodStairs(p), Direction.SOUTH, false));
		}
		soft(b, ix1, 1, iz1, barrel(Direction.UP));
		soft(b, ix1 - 1, 1, iz1, carpet(DyeColor.WHITE));
		soft(b, mx, flat ? h : h - 1, mz, Bp.lantern(true));
		furnish(b, c, ix0, iz0, ix1, iz1, 1, Interior.Use.STORE, 1);
		if (m == 1) {
			Houses.woodpile(b, p, 1, d - 1, Math.min(4, mx - 1));
			soft(b, w - 2, 1, d - 1, barrel(Direction.UP));
		}
	}

	// ================================================================ 2. caravanserai

	private static int[] cuts(int s, int e, int step) {
		java.util.List<Integer> l = new java.util.ArrayList<>();
		l.add(s);
		int cur = s;
		while (e - cur >= step + 3) {
			cur += step;
			l.add(cur);
		}
		if (e > s) {
			l.add(e);
		}
		int[] a = new int[l.size()];
		for (int i = 0; i < a.length; i++) {
			a[i] = l.get(i);
		}
		return a;
	}

	private static int[] concat(int[] a, int[] b2) {
		int[] r = new int[a.length + b2.length];
		System.arraycopy(a, 0, r, 0, a.length);
		System.arraycopy(b2, 0, r, a.length, b2.length);
		return r;
	}

	private static boolean contains(int[] a, int v) {
		for (int x : a) {
			if (x == v) {
				return true;
			}
		}
		return false;
	}

	private static boolean pierAt(int a, int lo, int hi) {
		return a == lo || a == hi || (a >= lo && (a - lo) % 3 == 0);
	}

	/**
	 * A caravanserai: a fortified square inn on a caravan road. High blind outer walls with corner towers, one great
	 * gate (an iwan under a tall portal), inside a courtyard with a fountain, all round it arcades and behind them the
	 * merchants' rooms; stables along one side, store rooms with bales and barrels along the other, a prayer room (or
	 * chapel) facing the gate, and flat roofs with parapets to walk on. For the Rus it is a гостиный двор of logs with a
	 * gallery on posts and plank roofs.
	 */
	static void caravanserai(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d;
		int mn = Math.min(w, d);
		if (mn < 15) {
			return;
		}
		boolean wood = c == Culture.RUS || c == Culture.BALT_FINN;
		boolean islam = islamic(c) || c == Culture.STEPPE;
		int rd = mn >= 23 ? 3 : 2;
		int gd = mn >= 29 ? 2 : 1;
		int T = rd + gd + 3;
		int rh = 4;
		int H = wood ? rh : 7;
		int mx = w / 2;
		BlockState floor = wood ? p.planks.defaultBlockState() : p.floor;
		BlockState plaster = wood ? null : p.plaster;
		BlockState pierS = wood ? Bp.log(p.log, Direction.Axis.Y) : islam ? p.stone2 : c == Culture.BYZANTINE ? Blocks.STONE_BRICKS.defaultBlockState() : p.stone;
		Block archStairs = wood ? woodStairs(p) : islam ? Blocks.SMOOTH_SANDSTONE_STAIRS : Blocks.STONE_BRICK_STAIRS;
		Block parapet = islam ? p.roofSlab : c == Culture.BYZANTINE ? Blocks.BRICK_SLAB : Blocks.STONE_BRICK_SLAB;
		int[] pxN = cuts(rd + 1, w - 2 - rd, 4);
		int[] pxS = concat(cuts(rd + 1, mx - 2, 4), cuts(mx + 2, w - 2 - rd, 4));
		int[] pz = cuts(rd + 1, d - 2 - rd, 4);
		boolean stableWest = b.r.nextBoolean();

		// ---- the ranges, ring by ring from the outer wall inwards
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				int k = Math.min(Math.min(x, z), Math.min(w - 1 - x, d - 1 - z));
				boolean ns = z == k || d - 1 - z == k;
				if (k == 0) {
					b.set(x, -2, z, p.stone2);
					b.set(x, -1, z, p.stone2);
					for (int y = 0; y <= H; y++) {
						BlockState s;
						if (wood) {
							s = Bp.log(p.log, ns ? Direction.Axis.X : Direction.Axis.Z);
						} else {
							s = (c == Culture.BYZANTINE && y % 3 == 0) || b.r.nextInt(8) == 0 ? p.stone2 : p.stone;
						}
						b.set(x, y, z, s);
					}
					if (!wood && (x + z) % 2 == 0) {
						b.set(x, H + 1, z, p.stone);
					}
				} else if (k <= rd) {
					boolean corner = (x <= rd || x >= w - 1 - rd) && (z <= rd || z >= d - 1 - rd);
					if (corner) {
						for (int y = 0; y <= rh; y++) {
							b.set(x, y, z, wood ? Bp.log(p.log, Direction.Axis.Y) : p.stone);
						}
					} else {
						boolean nBand = z <= rd, sBand = z >= d - 1 - rd;
						boolean part = nBand ? contains(pxN, x) : sBand ? contains(pxS, x) : contains(pz, z);
						b.set(x, 0, z, floor);
						if (part) {
							for (int y = 1; y <= rh; y++) {
								b.set(x, y, z, wood ? Bp.log(p.log, nBand || sBand ? Direction.Axis.Z : Direction.Axis.X) : plaster);
							}
						}
					}
				} else if (k == rd + 1) {
					for (int y = 0; y <= rh; y++) {
						b.set(x, y, z, wood ? Bp.log(p.log, ns ? Direction.Axis.X : Direction.Axis.Z) : plaster);
					}
				} else if (k < T - 1) {
					b.set(x, 0, z, floor);
				} else if (k == T - 1) {
					b.set(x, 0, z, floor);
					int along = ns ? x : z;
					int hi = ns ? w - T : d - T;
					if (pierAt(along, T - 1, hi)) {
						for (int y = 1; y <= rh; y++) {
							b.set(x, y, z, pierS);
						}
					} else {
						b.set(x, rh, z, wood ? Bp.log(p.strippedLog, ns ? Direction.Axis.X : Direction.Axis.Z) : plaster);
						if (pierAt(along - 1, T - 1, hi)) {
							b.set(x, rh - 1, z, Bp.stairs(archStairs, ns ? Direction.WEST : Direction.NORTH, true));
						} else if (pierAt(along + 1, T - 1, hi)) {
							b.set(x, rh - 1, z, Bp.stairs(archStairs, ns ? Direction.EAST : Direction.SOUTH, true));
						}
					}
				} else {
					// The courtyard.
					BlockState g;
					if (wood) {
						g = b.r.nextInt(3) == 0 ? Blocks.DIRT_PATH.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
					} else {
						g = b.r.nextInt(4) == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState();
					}
					b.set(x, 0, z, g);
				}
				// Roofs over the ranges: flat terraces of stone, or a plank ceiling under the pitched roofs of the Rus.
				if (k <= T - 1 && (wood || k >= 1)) {
					b.set(x, rh + 1, z, wood ? p.planks.defaultBlockState() : p.stone);
				}
				if (!wood && k == T - 1) {
					b.set(x, rh + 2, z, Bp.slab(parapet, false));
				}
			}
		}

		// ---- corner towers
		if (!wood) {
			for (int cxx : new int[]{0, w - 3}) {
				for (int czz : new int[]{0, d - 3}) {
					for (int x = cxx; x <= cxx + 2; x++) {
						for (int z = czz; z <= czz + 2; z++) {
							for (int y = 1; y <= H + 2; y++) {
								b.set(x, y, z, y == H + 2 ? p.stone2 : p.stone);
							}
							if ((x + z) % 2 == 0) {
								b.set(x, H + 3, z, p.stone);
							}
						}
					}
				}
			}
		}

		// ---- the gate
		for (int z = d - T; z < d; z++) {
			for (int x = mx - 1; x <= mx + 1; x++) {
				b.set(x, 0, z, floor);
				for (int y = 1; y <= rh; y++) {
					b.air(x, y, z);
				}
			}
		}
		if (wood) {
			// Plank ceiling done; pitched roofs over the four ranges, then a gate tower over the passage.
			b.gableRoof(0, T - 1, T, d - 1 - T, rh + 2, false, 0, p.roofStairs, p.roofSlab, p.roofFull, p.planks.defaultBlockState());
			b.gableRoof(w - T, w - 1, T, d - 1 - T, rh + 2, false, 0, p.roofStairs, p.roofSlab, p.roofFull, p.planks.defaultBlockState());
			b.gableRoof(0, w - 1, 0, T - 1, rh + 2, true, 0, p.roofStairs, p.roofSlab, p.roofFull, p.planks.defaultBlockState());
			b.gableRoof(0, w - 1, d - T, d - 1, rh + 2, true, 0, p.roofStairs, p.roofSlab, p.roofFull, p.planks.defaultBlockState());
			int gy = rh + 2;
			for (int y = gy; y <= gy + 3; y++) {
				for (int x = mx - 2; x <= mx + 2; x++) {
					for (int z = d - 3; z <= d - 1; z++) {
						boolean edge = x == mx - 2 || x == mx + 2 || z == d - 3 || z == d - 1;
						if (edge) {
							b.set(x, y, z, Bp.log(p.log, z == d - 3 || z == d - 1 ? Direction.Axis.X : Direction.Axis.Z));
						} else {
							b.air(x, y, z);
						}
					}
				}
			}
			b.air(mx, gy + 1, d - 1);
			b.air(mx, gy + 2, d - 1);
			b.pyramidRoof(mx - 2, mx + 2, d - 3, d - 1, gy + 4, p.roofStairs, p.roofFull, Blocks.LIGHTNING_ROD.waxed().unaffected().defaultBlockState());
			b.set(mx, rh + 1, d - 1, p.planks.defaultBlockState());
			b.set(mx, rh, d - 2, Bp.lantern(true));
		} else {
			// The iwan: a tall portal with a pointed arch over a deep recess, the gate at its back.
			int pt = H + 3;
			for (int x = mx - 3; x <= mx + 3; x++) {
				for (int y = 1; y <= pt; y++) {
					b.set(x, y, d - 1, y >= pt - 1 ? p.stone2 : p.stone);
				}
			}
			for (int x = mx - 3; x <= mx + 3; x += 2) {
				b.set(x, pt + 1, d - 1, p.stone2);
			}
			for (int z = d - 2; z <= d - 1; z++) {
				for (int x = mx - 2; x <= mx + 2; x++) {
					b.set(x, 0, z, floor);
					for (int y = 1; y <= H - 1; y++) {
						b.air(x, y, z);
					}
				}
				b.set(mx - 2, H - 1, z, Bp.stairs(archStairs, Direction.WEST, true));
				b.set(mx + 2, H - 1, z, Bp.stairs(archStairs, Direction.EAST, true));
			}
			for (int x = mx - 2; x <= mx + 2; x++) {
				b.set(x, H, d - 2, p.stone);
				for (int y = 1; y <= H - 1; y++) {
					boolean gate = Math.abs(x - mx) <= 1 && y <= rh;
					b.set(x, y, d - 3, gate ? AIR : p.stone);
				}
			}
			for (int y = 1; y <= H - 1; y++) {
				b.set(mx - 3, y, d - 2, p.stone);
				b.set(mx + 3, y, d - 2, p.stone);
			}
			// The frame of the portal by culture: glazed tiles, red and white voussoirs, bands of stone.
			if (c == Culture.PERSIAN) {
				for (int y = 1; y <= pt; y++) {
					BlockState t = Blocks.GLAZED_TERRACOTTA.pick(y % 2 == 0 ? DyeColor.LIGHT_BLUE : DyeColor.BLUE).defaultBlockState();
					b.set(mx - 3, y, d - 1, t);
					b.set(mx + 3, y, d - 1, t);
				}
				for (int x = mx - 2; x <= mx + 2; x++) {
					b.set(x, H, d - 1, Blocks.DYED_TERRACOTTA.pick(DyeColor.CYAN).defaultBlockState());
				}
			} else if (c == Culture.ANDALUSI) {
				for (int x = mx - 2; x <= mx + 2; x++) {
					b.set(x, H, d - 1, x % 2 == 0 ? Blocks.BRICKS.defaultBlockState() : Blocks.CALCITE.defaultBlockState());
				}
			} else {
				for (int x = mx - 3; x <= mx + 3; x++) {
					b.set(x, H, d - 1, p.stone2);
				}
			}
			b.set(mx, H - 1, d - 2, Bp.lantern(true));
			b.set(mx - 2, 1, d - 2, Bp.stairs(archStairs, Direction.WEST, false));
			b.set(mx + 2, 1, d - 2, Bp.stairs(archStairs, Direction.EAST, false));
		}

		// ---- doors, rooms and what is in them
		int prayer = -1;
		for (int i = 0; i + 1 < pxN.length; i++) {
			if (pxN[i] < mx && mx <= pxN[i + 1]) {
				prayer = i;
			}
		}
		// North range: the prayer room facing the gate, rooms for the merchants.
		for (int i = 0; i + 1 < pxN.length; i++) {
			int a0 = pxN[i] + 1, a1 = pxN[i + 1] - 1;
			if (a1 < a0) {
				continue;
			}
			int dxr = (a0 + a1) / 2;
			b.door(p.door, dxr, 1, rd + 1, Direction.SOUTH);
			b.air(dxr, 1, rd);
			b.air(dxr, 2, rd);
			soft(b, dxr, rh, (1 + rd) / 2 + 1, Bp.lantern(true));
			if (i == prayer) {
				prayerRoom(b, p, c, a0, 1, a1, rd, dxr);
			} else {
				if (islam) {
					soft(b, a0, 1, 1, carpet(i % 2 == 0 ? DyeColor.RED : DyeColor.ORANGE));
				}
				furnish(b, c, a0, 1, a1, rd, 1, Interior.Use.SLEEPING, 1);
			}
		}
		// South range, either side of the gate: rooms for the merchants and the gatekeeper.
		for (int i = 0; i + 1 < pxS.length; i++) {
			int a0 = pxS[i] + 1, a1 = pxS[i + 1] - 1;
			if (a1 < a0 || (a0 >= mx - 1 && a1 <= mx + 1)) {
				continue;
			}
			int dxr = (a0 + a1) / 2;
			if (Math.abs(dxr - mx) <= 2) {
				continue;
			}
			int zf = d - 2 - rd;
			b.door(p.door, dxr, 1, zf, Direction.NORTH);
			b.air(dxr, 1, zf + 1);
			b.air(dxr, 2, zf + 1);
			soft(b, dxr, rh, d - 1 - (1 + rd) / 2 - 1, Bp.lantern(true));
			furnish(b, c, a0, d - 1 - rd, a1, d - 2, 1, Interior.Use.SLEEPING, 1);
		}
		// West and east ranges: stables on one side, store rooms on the other.
		for (int side = 0; side < 2; side++) {
			boolean west = side == 0;
			boolean stable = west == stableWest;
			int xf = west ? rd + 1 : w - 2 - rd;
			int xr0 = west ? 1 : w - 1 - rd, xr1 = west ? rd : w - 2;
			int back = west ? 1 : w - 2;
			for (int j = 0; j + 1 < pz.length; j++) {
				int a0 = pz[j] + 1, a1 = pz[j + 1] - 1;
				if (a1 < a0) {
					continue;
				}
				if (stable) {
					// Open stalls: the front wall opened below a lintel, fences between, earth floor, hay and troughs.
					for (int z = a0; z <= a1; z++) {
						b.air(xf, 1, z);
						b.air(xf, 2, z);
						b.set(xf, 0, z, Blocks.COARSE_DIRT.defaultBlockState());
						for (int x = xr0; x <= xr1; x++) {
							b.set(x, 0, z, Blocks.COARSE_DIRT.defaultBlockState());
						}
						soft(b, back, 1, z, z % 2 == 0 ? hay(Direction.Axis.Y) : Blocks.COMPOSTER.defaultBlockState());
					}
					soft(b, back, 2, a0, hay(Direction.Axis.Z));
					soft(b, (xr0 + xr1) / 2, rh, (a0 + a1) / 2, Bp.lantern(true));
				} else {
					int dzr = (a0 + a1) / 2;
					b.door(p.door, xf, 1, dzr, west ? Direction.EAST : Direction.WEST);
					b.air(west ? xf - 1 : xf + 1, 1, dzr);
					b.air(west ? xf - 1 : xf + 1, 2, dzr);
					for (int z = a0; z <= a1; z++) {
						int g = b.r.nextInt(5);
						BlockState s = g == 0 ? hay(Direction.Axis.X) : g == 1 ? wool(b.r.nextBoolean() ? DyeColor.RED : DyeColor.YELLOW)
								: g == 2 && islam ? Blocks.DECORATED_POT.defaultBlockState() : barrel(Direction.UP);
						soft(b, back, 1, z, s);
						if (b.r.nextBoolean()) {
							soft(b, back, 2, z, b.r.nextBoolean() ? hay(Direction.Axis.Z) : barrel(Direction.UP));
						}
					}
					soft(b, (xr0 + xr1) / 2, rh, dzr, Bp.lantern(true));
					furnish(b, c, xr0, a0, xr1, a1, 1, Interior.Use.STORE, 1);
				}
			}
			if (stable) {
				for (int j = 1; j + 1 < pz.length; j++) {
					for (int x = xr0; x <= xr1; x++) {
						b.set(x, 1, pz[j], p.fence.defaultBlockState());
						for (int y = 2; y <= rh; y++) {
							b.air(x, y, pz[j]);
						}
					}
				}
			}
		}

		// ---- the ladder to the roof terrace, lamps in the galleries
		if (!wood) {
			int lx = rd + 2, lz = rd + 2;
			if (!b.has(lx, 1, rd + 1) || b.get(lx, 1, rd + 1).getBlock() != p.door) {
				for (int y = 1; y <= rh + 1; y++) {
					b.set(lx, y, lz, Bp.facing(Blocks.LADDER, Direction.SOUTH));
				}
			}
			// Rugs and jars on the roof.
			for (int i = 0; i < 6; i++) {
				int x = 1 + b.r.nextInt(Math.max(1, w - 2));
				soft(b, x, rh + 2, 1 + b.r.nextInt(Math.max(1, rd)), i % 3 == 0 ? Blocks.DECORATED_POT.defaultBlockState()
						: carpet(i % 2 == 0 ? DyeColor.RED : DyeColor.BLUE));
			}
		}
		int gk = rd + 2;
		for (int a = gk; a <= w - 1 - gk; a++) {
			if ((a - gk) % 5 == 2) {
				soft(b, a, rh, gk, Bp.lantern(true));
				soft(b, a, rh, d - 1 - gk, Bp.lantern(true));
			}
		}
		for (int a = gk + 1; a < d - 1 - gk; a++) {
			if ((a - gk) % 5 == 2) {
				soft(b, gk, rh, a, Bp.lantern(true));
				soft(b, w - 1 - gk, rh, a, Bp.lantern(true));
			}
		}

		// ---- the courtyard: fountain or well, troughs and hitching posts by the stables, bales by the stores, trees
		int cx0 = T, cx1 = w - 1 - T, cz0 = T, cz1 = d - 1 - T;
		int cmx = (cx0 + cx1) / 2, cmz = (cz0 + cz1) / 2;
		for (int z = cz0; z <= cz1; z++) {
			b.set(mx, 0, z, wood ? p.planks.defaultBlockState() : p.floor);
		}
		for (int x = cx0; x <= cx1; x++) {
			b.set(x, 0, cmz, wood ? p.planks.defaultBlockState() : p.floor);
		}
		if (cx1 - cx0 >= 4 && cz1 - cz0 >= 4) {
			if (wood) {
				well(b, p, cmx, cmz);
			} else {
				for (int x = cmx - 2; x <= cmx + 2; x++) {
					for (int z = cmz - 2; z <= cmz + 2; z++) {
						boolean rim = Math.abs(x - cmx) == 2 || Math.abs(z - cmz) == 2;
						b.set(x, -1, z, p.stone2);
						b.set(x, 0, z, rim ? p.stone2 : Blocks.WATER.defaultBlockState());
						if (rim && (x + z) % 2 == 0) {
							b.set(x, 1, z, Bp.slab(parapet, false));
						}
					}
				}
				b.set(cmx, 0, cmz, p.stone2);
				b.set(cmx, 1, cmz, waterCauldron());
			}
		} else {
			soft(b, cmx, 1, cmz, waterCauldron());
		}
		int sxEdge = stableWest ? cx0 : cx1, gxEdge = stableWest ? cx1 : cx0;
		for (int z = cz0 + 1; z < cz1; z += 3) {
			soft(b, sxEdge, 1, z, z % 2 == 0 ? waterCauldron() : p.fence.defaultBlockState());
		}
		for (int z = cz0 + 1; z < cz1; z += 2) {
			if (z == cmz) {
				continue;
			}
			int g = b.r.nextInt(4);
			soft(b, gxEdge, 1, z, g == 0 ? hay(Direction.Axis.Z) : g == 1 ? barrel(Direction.UP)
					: g == 2 ? wool(DyeColor.WHITE) : (islam ? Blocks.DECORATED_POT.defaultBlockState() : Blocks.CHEST.defaultBlockState()));
		}
		for (int[] cc : new int[][]{{cx0, cz0}, {cx1, cz0}, {cx0, cz1}, {cx1, cz1}}) {
			soft(b, cc[0], 1, cc[1], p.fence.defaultBlockState());
			soft(b, cc[0], 2, cc[1], p.fence.defaultBlockState());
			soft(b, cc[0], 3, cc[1], Bp.lantern(false));
		}
		if (cx1 - cx0 >= 10 && cz1 - cz0 >= 10) {
			int[][] spots = {{cx0 + 2, cz0 + 2}, {cx1 - 2, cz0 + 2}};
			for (int[] s : spots) {
				if (wood) {
					tree(b, s[0], s[1], Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, null, 3, 1);
				} else if (islam && b.r.nextBoolean()) {
					tree(b, s[0], s[1], Blocks.JUNGLE_LOG, Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES, 2, 1);
				} else {
					cypress(b, s[0], s[1], 6);
				}
			}
		}
	}

	/** The prayer room of a caravanserai: carpets and the mihrab (Islam), a chapel with a lectern and candles (Christians). */
	private static void prayerRoom(Bp b, Palette p, Culture c, int x0, int z0, int x1, int z1, int nx) {
		if (islamic(c) || c == Culture.STEPPE) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					soft(b, x, 1, z, carpet((x + z) % 2 == 0 ? DyeColor.RED : DyeColor.GREEN));
				}
			}
			BlockState niche = c == Culture.PERSIAN ? Blocks.GLAZED_TERRACOTTA.pick(DyeColor.LIGHT_BLUE).defaultBlockState()
					: Blocks.CHISELED_SANDSTONE.defaultBlockState();
			b.set(nx, 1, z0 - 1, niche);
			b.set(nx, 2, z0 - 1, niche);
			b.set(nx, 3, z0 - 1, Blocks.CUT_SANDSTONE.defaultBlockState());
			soft(b, nx, 3, z0, Bp.lantern(true));
		} else {
			b.set(nx, 1, z0, Blocks.LECTERN.defaultBlockState());
			soft(b, nx - 1, 1, z0, candle(3));
			soft(b, nx + 1, 1, z0, candle(2));
			b.set(nx, 2, z0 - 1, Blocks.GOLD_BLOCK.defaultBlockState());
			for (int x = x0; x <= x1; x++) {
				soft(b, x, 1, z1, carpet(DyeColor.RED));
			}
		}
	}

	/** A small well with a log frame and a little roof. */
	private static void well(Bp b, Palette p, int cx, int cz) {
		for (int x = cx - 1; x <= cx + 1; x++) {
			for (int z = cz - 1; z <= cz + 1; z++) {
				boolean inner = x == cx && z == cz;
				for (int y = -5; y <= 0; y++) {
					put(b, x, y, z, inner ? Blocks.WATER.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
				}
				if (!inner) {
					put(b, x, 1, z, Blocks.COBBLESTONE_WALL.defaultBlockState());
				}
			}
		}
		put(b, cx, 1, cz, AIR);
		put(b, cx - 1, 2, cz, p.fence.defaultBlockState());
		put(b, cx + 1, 2, cz, p.fence.defaultBlockState());
		put(b, cx - 1, 3, cz, p.fence.defaultBlockState());
		put(b, cx + 1, 3, cz, p.fence.defaultBlockState());
		put(b, cx, 3, cz, Bp.log(p.log, Direction.Axis.X));
		put(b, cx, 2, cz, Blocks.IRON_CHAIN.defaultBlockState());
		for (int x = cx - 1; x <= cx + 1; x++) {
			put(b, x, 4, cz - 1, Bp.stairs(p.roofStairs, Direction.SOUTH, false));
			put(b, x, 4, cz + 1, Bp.stairs(p.roofStairs, Direction.NORTH, false));
			put(b, x, 4, cz, p.roofFull);
		}
	}

	// ================================================================ 3. barracks

	/**
	 * A guardhouse / barracks for the garrison: a long building with rows of beds, an armoury at the back, a mess table
	 * and hearth at the front; beside it a fenced yard with training dummies, archery targets, a trough and the flag.
	 */
	static void barracks(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d;
		int bx0 = w >= 11 ? 1 : 0;
		int bwid = clamp(w - 6, 5, 8);
		int bx1 = bx0 + bwid - 1;
		int bz0 = 1, bz1 = d - 2;
		if (bz1 - bz0 < 7 || bx1 > w - 1) {
			return;
		}
		int st = switch (c) {
			case RUS, BALT_FINN -> LOG;
			case NORSE, WEST_SLAV, MAGYAR -> FRAME;
			default -> STONE;
		};
		boolean flat = st == STONE && flatRoof(c, b.r);
		int h = 4;
		b.fill(bx0, -3, bz0, bx1, -1, bz1, Blocks.COBBLESTONE.defaultBlockState());
		int ix0 = bx0 + 1, ix1 = bx1 - 1, iz0 = bz0 + 1, iz1 = bz1 - 1;
		b.fill(ix0, 0, iz0, ix1, 0, iz1, st == STONE ? p.floor : p.planks.defaultBlockState());
		box(b, p, c, st, bx0, bz0, bx1, bz1, h);
		int mx = (bx0 + bx1) / 2;
		// Tie beams with lamps.
		for (int z = iz0 + 2; z < iz1; z += 4) {
			if (!flat) {
				for (int x = bx0; x <= bx1; x++) {
					b.set(x, h, z, Bp.log(p.strippedLog, Direction.Axis.X));
				}
			}
		}
		roof(b, p, c, flat, bx0, bx1, bz0, bz1, h + 1, false, bx0 >= 1 ? 1 : 0);
		// Windows along the long walls, doors to the street and to the yard.
		int zs = bz1 - 3;
		for (int z = bz0 + 2; z < bz1 - 1; z += 3) {
			for (int x : new int[]{bx0, bx1}) {
				if (x == bx1 && Math.abs(z - zs) <= 1) {
					continue;
				}
				b.set(x, 2, z, st == STONE ? Blocks.IRON_BARS.defaultBlockState() : AIR);
			}
		}
		b.door(p.door, mx, 1, bz1, Direction.SOUTH);
		b.door(p.door, bx1, 1, zs, Direction.EAST);
		if (st == STONE) {
			b.set(mx, 3, bz1, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true));
		}
		for (int y = 1; y <= 2; y++) {
			b.air(mx, y, iz1);
			b.air(ix1, y, zs);
		}
		// The armoury along the back wall.
		BlockState[] arms = {grindstone(Direction.SOUTH), Blocks.CHEST.defaultBlockState(), Bp.facing(Blocks.ANVIL, Direction.EAST),
				barrel(Direction.UP), Blocks.CHEST.defaultBlockState(), Blocks.SMITHING_TABLE.defaultBlockState()};
		for (int x = ix0; x <= ix1; x++) {
			BlockState s = arms[(x - ix0) % arms.length];
			soft(b, x, 1, iz0, s);
			if (s.getBlock() == Blocks.CHEST || s.getBlock() == Blocks.BARREL) {
				soft(b, x, 2, iz0, p.fence.defaultBlockState());
			}
		}
		// Rows of beds, with a chest at every third place.
		int bedEnd = iz1 - 4;
		boolean two = ix1 - ix0 + 1 >= 5;
		for (int z = iz0 + 1; z <= bedEnd; z++) {
			boolean chest = (z - iz0) % 3 == 0;
			if (chest) {
				soft(b, ix0, 1, z, Blocks.CHEST.defaultBlockState());
				if (two) {
					soft(b, ix1, 1, z, Blocks.CHEST.defaultBlockState());
				}
				continue;
			}
			if (!b.has(ix0, 1, z) && !b.has(ix0 + 1, 1, z)) {
				b.bed(p.bed, ix0 + 1, 1, z, Direction.WEST);
			}
			if (two && !b.has(ix1, 1, z) && !b.has(ix1 - 1, 1, z)) {
				b.bed(p.bed, ix1 - 1, 1, z, Direction.EAST);
			}
		}
		// The mess: a long table with benches, the hearth, water.
		Block ws = woodStairs(p);
		for (int x = ix0 + 1; x <= ix1 - 1; x++) {
			if (x == mx) {
				continue;
			}
			soft(b, x, 1, iz1 - 2, Bp.slab(woodSlab(p), true));
			soft(b, x, 1, iz1 - 3, Bp.stairs(ws, Direction.NORTH, false));
			soft(b, x, 1, iz1 - 1, Bp.stairs(ws, Direction.SOUTH, false));
		}
		soft(b, ix0, 1, iz1, lit(Blocks.SMOKER, Direction.EAST));
		soft(b, ix0, 1, iz1 - 1, barrel(Direction.UP));
		soft(b, ix1, 1, iz1, waterCauldron());
		for (int z = iz0 + 2; z < iz1; z += 4) {
			soft(b, mx, flat ? h : h - 1, z, Bp.lantern(true));
		}
		furnish(b, c, ix0, iz0, ix1, iz1, 1, Interior.Use.SLEEPING, 1);

		// The lane beside the building and the yard.
		for (int z = 0; z < d; z++) {
			put(b, bx1 + 1, 0, z, Blocks.DIRT_PATH.defaultBlockState());
		}
		int yx0 = bx1 + 2, yx1 = w - 1;
		if (yx1 - yx0 < 2) {
			return;
		}
		for (int x = yx0; x <= yx1; x++) {
			for (int z = 0; z < d; z++) {
				int g = b.r.nextInt(4);
				b.set(x, 0, z, g == 0 ? Blocks.DIRT_PATH.defaultBlockState() : g == 1 ? Blocks.PACKED_MUD.defaultBlockState()
						: Blocks.COARSE_DIRT.defaultBlockState());
			}
		}
		boolean targetsBack = b.r.nextBoolean();
		// Training dummies: a post, a body of straw, a head, arms.
		int dxm = yx0 + 1;
		if (dxm < yx1) {
			for (int z = 3; z <= (targetsBack ? d - 4 : d / 2 + 1); z += 3) {
				put(b, dxm, 1, z, p.fence.defaultBlockState());
				put(b, dxm, 2, z, hay(Direction.Axis.Y));
				put(b, dxm, 3, z, Bp.facing(Blocks.CARVED_PUMPKIN, Direction.EAST));
				put(b, dxm, 2, z - 1, p.fence.defaultBlockState());
				put(b, dxm, 2, z + 1, p.fence.defaultBlockState());
			}
		}
		// Archery targets on straw.
		if (targetsBack) {
			for (int x = yx0 + 3; x <= yx1 - 1; x += 2) {
				put(b, x, 1, 1, hay(Direction.Axis.X));
				put(b, x, 2, 1, Blocks.TARGET.defaultBlockState());
			}
		} else {
			int tx = yx1 - 1;
			for (int z = 2; z <= d / 2; z += 3) {
				if (tx > dxm) {
					put(b, tx, 1, z, hay(Direction.Axis.Z));
					put(b, tx, 2, z, Blocks.TARGET.defaultBlockState());
				}
			}
		}
		// Trough, grindstone, the flag.
		soft(b, yx0, 1, d - 3, waterCauldron());
		soft(b, yx0, 1, d - 5, grindstone(Direction.EAST));
		int fx = yx1 - 1, fz = d - 2;
		if (fx >= yx0) {
			b.set(fx, 0, fz, Blocks.COBBLESTONE.defaultBlockState());
			for (int y = 1; y <= 6; y++) {
				b.set(fx, y, fz, p.fence.defaultBlockState());
			}
			b.set(fx, 7, fz, Blocks.BANNER.pick(flagColour(c, b.r)).defaultBlockState());
		}
		// The fence of the yard with a gate to the street.
		int gx = (yx0 + yx1) / 2;
		for (int z = 0; z < d; z++) {
			soft(b, yx1, 1, z, p.fence.defaultBlockState());
		}
		for (int x = yx0; x <= yx1; x++) {
			soft(b, x, 1, 0, p.fence.defaultBlockState());
			soft(b, x, 1, d - 1, x == gx ? Bp.facing(p.gate, Direction.SOUTH) : p.fence.defaultBlockState());
		}
	}

	private static DyeColor flagColour(Culture c, java.util.Random r) {
		return switch (c) {
			case RUS -> DyeColor.RED;
			case BYZANTINE -> DyeColor.PURPLE;
			case ANDALUSI -> DyeColor.WHITE;
			case ARAB -> r.nextBoolean() ? DyeColor.BLACK : DyeColor.GREEN;
			case PERSIAN -> DyeColor.BLACK;
			case NORSE -> r.nextBoolean() ? DyeColor.RED : DyeColor.BLACK;
			case CAUCASUS -> DyeColor.RED;
			case LATIN -> r.nextBoolean() ? DyeColor.BLUE : DyeColor.YELLOW;
			case STEPPE -> DyeColor.YELLOW;
			default -> DyeColor.WHITE;
		};
	}

	// ================================================================ 4. tower

	/**
	 * A tall tower: a Svan defence tower in the Caucasus, a noble tower house in the Latin West, a round tower at an Irish
	 * monastery, a stone watch tower in the Islamic south and Byzantium, a wooden watch tower on posts elsewhere.
	 */
	static void tower(Bp b, Palette p, Culture c) {
		switch (c) {
			case CAUCASUS -> svan(b, p, c);
			case LATIN -> stoneTower(b, p, c, true);
			case CELTIC -> roundTower(b, p, c);
			case ARAB, ANDALUSI, PERSIAN, BYZANTINE -> stoneTower(b, p, c, false);
			default -> watchTower(b, p, c);
		}
	}

	private static BlockState rubble(java.util.Random r) {
		return switch (r.nextInt(6)) {
			case 0 -> Blocks.STONE.defaultBlockState();
			case 1 -> Blocks.ANDESITE.defaultBlockState();
			case 2 -> Blocks.TUFF.defaultBlockState();
			case 3 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
			default -> Blocks.COBBLESTONE.defaultBlockState();
		};
	}

	/**
	 * A Svan tower: a narrow square shaft of rubble stone 5–6 storeys high with a battered foot, slits for windows, the
	 * entrance on the first floor by a ladder, a corbelled top storey with openings all round and a pyramid of slate.
	 */
	private static void svan(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d, n = Math.min(w, d);
		int ts = Math.min(5, n);
		if (ts < 3) {
			return;
		}
		int x0 = (w - ts) / 2, z0 = (d - ts) / 2, x1 = x0 + ts - 1, z1 = z0 + ts - 1;
		int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
		int H = 21 + b.r.nextInt(4);
		boolean wide = x0 >= 1 && z0 >= 1 && x1 <= w - 2 && z1 <= d - 2;
		int top = wide ? H - 4 : H;
		for (int x = x0 - 1; x <= x1 + 1; x++) {
			for (int z = z0 - 1; z <= z1 + 1; z++) {
				for (int y = -3; y <= -1; y++) {
					put(b, x, y, z, Blocks.COBBLESTONE.defaultBlockState());
				}
			}
		}
		for (int y = 0; y <= top; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
					if (edge) {
						b.set(x, y, z, rubble(b.r));
					} else if (y % 4 == 0) {
						b.set(x, y, z, p.planks.defaultBlockState());
					} else {
						b.air(x, y, z);
					}
				}
			}
		}
		// A battered foot.
		if (wide) {
			for (int x = x0 - 1; x <= x1 + 1; x++) {
				for (int z = z0 - 1; z <= z1 + 1; z++) {
					if (x >= x0 && x <= x1 && z >= z0 && z <= z1) {
						continue;
					}
					Direction inward = z == z0 - 1 ? Direction.SOUTH : z == z1 + 1 ? Direction.NORTH : x == x0 - 1 ? Direction.EAST : Direction.WEST;
					b.set(x, 0, z, Blocks.COBBLESTONE.defaultBlockState());
					b.set(x, 1, z, Blocks.COBBLESTONE.defaultBlockState());
					b.set(x, 2, z, Bp.stairs(Blocks.COBBLESTONE_STAIRS, inward, false));
				}
			}
			// The corbelled top storey, one block wider, with openings on every side.
			for (int x = x0 - 1; x <= x1 + 1; x++) {
				for (int z = z0 - 1; z <= z1 + 1; z++) {
					boolean ring = x == x0 - 1 || x == x1 + 1 || z == z0 - 1 || z == z1 + 1;
					if (ring) {
						Direction inward = z == z0 - 1 ? Direction.SOUTH : z == z1 + 1 ? Direction.NORTH : x == x0 - 1 ? Direction.EAST : Direction.WEST;
						b.set(x, top + 1, z, Bp.stairs(Blocks.COBBLESTONE_STAIRS, inward, true));
						for (int y = top + 2; y <= H; y++) {
							b.set(x, y, z, rubble(b.r));
						}
					} else {
						b.set(x, top + 1, z, p.planks.defaultBlockState());
						for (int y = top + 2; y <= H; y++) {
							b.air(x, y, z);
						}
					}
				}
			}
			for (int y = top + 3; y <= top + 4; y++) {
				b.air(mx, y, z0 - 1);
				b.air(mx, y, z1 + 1);
				b.air(x0 - 1, y, mz);
				b.air(x1 + 1, y, mz);
			}
			b.pyramidRoof(x0 - 1, x1 + 1, z0 - 1, z1 + 1, H + 1, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES.defaultBlockState(),
					Blocks.COBBLED_DEEPSLATE.defaultBlockState());
		} else {
			b.pyramidRoof(x0, x1, z0, z1, H + 1, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES.defaultBlockState(),
					Blocks.COBBLED_DEEPSLATE.defaultBlockState());
		}
		// Ladder inside through all floors (kept off the wall with the slits).
		int lx = mx == x0 + 1 ? x1 - 1 : x0 + 1;
		for (int y = 1; y <= (wide ? top + 1 : top); y++) {
			b.set(lx, y, z0 + 1, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}
		// Slits, one per storey, on changing sides.
		for (int f = 0; f * 4 + 2 < top - 1; f++) {
			int y = f * 4 + 2;
			switch (f % 4) {
				case 0 -> b.air(mx, y, z0);
				case 1 -> b.air(x1, y, mz);
				case 2 -> b.air(x0, y, mz);
				default -> b.air(mx, y, z1);
			}
		}
		// The entrance on the first floor with a ladder up the wall, or at the ground on a small plot.
		if (wide && ts >= 5) {
			b.door(p.door, mx, 5, z1, Direction.SOUTH);
			b.air(mx, 5, z1 - 1);
			b.air(mx, 6, z1 - 1);
			for (int y = 1; y <= 4; y++) {
				b.set(mx, y, z1 + 1, Bp.facing(Blocks.LADDER, Direction.SOUTH));
			}
		} else {
			b.door(p.door, mx, 1, z1, Direction.SOUTH);
			b.air(mx, 1, z1 - 1);
			b.air(mx, 2, z1 - 1);
		}
		// Store below, the living floor above, lamps.
		if (ts >= 5) {
			for (int fy = 0; fy + 4 <= top; fy += 4) {
				soft(b, x1 - 1, fy + 3, z1 - 1, Bp.lantern(true));
			}
			furnish(b, c, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, Interior.Use.STORE, 1);
			furnish(b, c, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 5, Interior.Use.LIVING, 1);
			furnish(b, c, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 9, Interior.Use.SLEEPING, 1);
		}
	}

	/**
	 * A square stone tower: the tower house of an Italian noble family (tall, small windows, crenellated or under a
	 * tiled roof, stone or stone and brick) or, with {@code noble} false, a stone watch tower of the south.
	 */
	private static void stoneTower(Bp b, Palette p, Culture c, boolean noble) {
		int w = b.w, d = b.d, n = Math.min(w, d);
		int ts = Math.min(n, noble ? 7 : 6);
		if (ts < 4) {
			return;
		}
		int x0 = (w - ts) / 2, z0 = (d - ts) / 2, x1 = x0 + ts - 1, z1 = z0 + ts - 1;
		int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
		int H = noble ? 20 + b.r.nextInt(7) : 12 + b.r.nextInt(4);
		int v = b.r.nextInt(3);
		boolean tiles = noble ? v == 1 || b.r.nextInt(4) == 0 : false;
		b.fill(x0, -3, z0, x1, -1, z1, Blocks.COBBLESTONE.defaultBlockState());
		for (int y = 0; y <= H; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
					boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
					if (edge) {
						BlockState s;
						if (!noble) {
							s = b.r.nextInt(6) == 0 || y % 5 == 0 ? p.stone2 : p.stone;
						} else if (v == 1 && y > H / 3) {
							s = corner ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.BRICKS.defaultBlockState();
						} else if (v == 2) {
							s = corner ? Blocks.STONE_BRICKS.defaultBlockState() : b.r.nextInt(5) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
									: Blocks.COBBLESTONE.defaultBlockState();
						} else {
							s = b.r.nextInt(5) == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
						}
						b.set(x, y, z, s);
					} else if (y == 0 || (y % 5 == 0 && y <= H - 3) || y == H) {
						b.set(x, y, z, y == 0 ? Blocks.COBBLESTONE.defaultBlockState() : p.planks.defaultBlockState());
					} else {
						b.air(x, y, z);
					}
				}
			}
		}
		// The ladder through all floors (kept off the wall with the windows).
		int lx = mx == x0 + 1 ? x1 - 1 : x0 + 1;
		for (int y = 1; y <= H; y++) {
			b.set(lx, y, z0 + 1, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}
		// The door with an arch.
		b.door(p.door, mx, 1, z1, Direction.SOUTH);
		Block arch = noble ? Blocks.STONE_BRICK_STAIRS : p.stairs == Blocks.SMOOTH_SANDSTONE_STAIRS ? p.stairs : Blocks.STONE_BRICK_STAIRS;
		b.set(mx, 3, z1, Bp.stairs(arch, Direction.SOUTH, true));
		b.air(mx, 1, z1 - 1);
		b.air(mx, 2, z1 - 1);
		// Windows: slits below, small arched windows above.
		for (int fy = 5; fy <= H - 4; fy += 5) {
			boolean big = noble && fy >= 10;
			BlockState win = big ? Blocks.GLASS_PANE.defaultBlockState() : AIR;
			int[][] spots = {{mx, z1}, {mx, z0}, {x0, mz}, {x1, mz}};
			for (int[] s : spots) {
				b.set(s[0], fy + 2, s[1], win);
				if (big) {
					b.set(s[0], fy + 3, s[1], win);
				}
			}
			if (fy + 5 <= H - 3 || fy + 5 == H) {
				soft(b, x1 - 1, fy + 4, z1 - 1, Bp.lantern(true));
			}
		}
		b.air(mx, 2, z0);
		soft(b, x1 - 1, 4, z1 - 1, Bp.lantern(true));
		// The top: battlements, or a tiled pyramid roof.
		if (tiles) {
			b.pyramidRoof(x0, x1, z0, z1, H + 1, Blocks.BRICK_STAIRS, Blocks.BRICKS.defaultBlockState(), null);
		} else {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
					if (edge && (x + z) % 2 == 0) {
						b.set(x, H + 1, z, noble ? Blocks.STONE_BRICKS.defaultBlockState() : p.stone);
					}
				}
			}
			if (!noble) {
				soft(b, mx, H + 1, mz, p.fence.defaultBlockState());
				soft(b, mx, H + 2, mz, Bp.lantern(false));
			}
		}
		// Rooms: stores below, the hall and chambers above.
		furnish(b, c, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 1, Interior.Use.STORE, noble ? 2 : 1);
		if (H >= 10) {
			furnish(b, c, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 6, noble ? Interior.Use.HALL : Interior.Use.SLEEPING, noble ? 2 : 1);
		}
		if (noble && H >= 18) {
			furnish(b, c, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 11, Interior.Use.SLEEPING, 2);
			furnish(b, c, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 16, Interior.Use.STORE, 2);
		}
	}

	/**
	 * An Irish round tower: a tall slim drum of drystone, the door four blocks up reached by a ladder, wooden floors
	 * inside, a window on each floor facing a different way, four at the top under the bell, and a conical stone cap.
	 */
	private static void roundTower(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d;
		int cx = (w - 1) / 2, cz = (d - 1) / 2;
		int rr = Math.min(Math.min(cx, w - 1 - cx), Math.min(cz, d - 1 - cz));
		if (rr < 2) {
			return;
		}
		double rad = rr + 0.4;
		double inner = rad - 1.2;
		int H = 23 + b.r.nextInt(4);
		for (int x = cx - rr; x <= cx + rr; x++) {
			for (int z = cz - rr; z <= cz + rr; z++) {
				double dd = Math.hypot(x - cx, z - cz);
				if (dd > rad) {
					continue;
				}
				for (int y = -3; y <= -1; y++) {
					b.set(x, y, z, Blocks.COBBLESTONE.defaultBlockState());
				}
				boolean wall = dd > inner;
				for (int y = 0; y <= H; y++) {
					if (wall) {
						b.set(x, y, z, b.r.nextInt(5) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
								: b.r.nextInt(3) == 0 ? Blocks.STONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
					} else if (y <= 2) {
						b.set(x, y, z, Blocks.COBBLESTONE.defaultBlockState());
					} else if ((y - 3) % 5 == 0) {
						b.set(x, y, z, p.planks.defaultBlockState());
					} else {
						b.air(x, y, z);
					}
				}
			}
		}
		// Find the inner cell at the back for the ladder and the wall cell at the front for the door.
		int back = 0;
		while (Math.hypot(0, back - 1) <= inner) {
			back--;
		}
		int front = 0;
		while (Math.hypot(0, front + 1) <= inner) {
			front++;
		}
		front++;
		int topFloor = 3;
		while (topFloor + 5 <= H - 3) {
			topFloor += 5;
		}
		for (int y = 4; y <= topFloor; y++) {
			b.set(cx, y, cz + back, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}
		b.door(p.door, cx, 4, cz + front, Direction.SOUTH);
		b.air(cx, 4, cz + front - 1);
		b.air(cx, 5, cz + front - 1);
		for (int y = 1; y <= 3; y++) {
			put(b, cx, y, cz + front + 1, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}
		// Windows: one per floor, turning round; four under the cap.
		int[][] dirs = {{1, 0}, {0, -1}, {-1, 0}, {0, 1}};
		int f = 0;
		for (int fy = 8; fy < topFloor; fy += 5, f++) {
			int[] dv = dirs[f % 4];
			b.air(cx + dv[0] * rr, fy + 2, cz + dv[1] * rr);
		}
		for (int[] dv : dirs) {
			b.air(cx + dv[0] * rr, topFloor + 2, cz + dv[1] * rr);
			b.air(cx + dv[0] * rr, topFloor + 3, cz + dv[1] * rr);
		}
		if (topFloor + 1 <= H) {
			soft(b, cx, topFloor + 1, cz, Blocks.BELL.defaultBlockState());
		}
		// The treasury of the monks: a chest and a lectern by the door floor, a lamp.
		soft(b, cx + 1, 4, cz, Blocks.CHEST.defaultBlockState());
		soft(b, cx - 1, 4, cz, Blocks.LECTERN.defaultBlockState());
		soft(b, cx, 7, cz, Bp.lantern(true));
		// The conical cap of stone.
		int layer = 0;
		for (double r2 = rad + 0.3; r2 > 0.6; r2 -= 0.75, layer++) {
			for (int x = cx - rr - 1; x <= cx + rr + 1; x++) {
				for (int z = cz - rr - 1; z <= cz + rr + 1; z++) {
					if (Math.hypot(x - cx, z - cz) <= r2) {
						put(b, x, H + 1 + layer, z, b.r.nextInt(4) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
								: Blocks.COBBLESTONE.defaultBlockState());
					}
				}
			}
		}
		put(b, cx, H + 1 + layer, cz, Blocks.COBBLESTONE_WALL.defaultBlockState());
	}

	/** A wooden watch tower: four log posts, cross beams, a ladder, a platform with a railing and a bell under a roof. */
	private static void watchTower(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d, n = Math.min(w, d);
		int s = clamp(n - 2, 3, 5);
		if (n < 3) {
			return;
		}
		int x0 = (w - s) / 2, z0 = (d - s) / 2, x1 = x0 + s - 1, z1 = z0 + s - 1;
		int mx = (x0 + x1) / 2;
		int H = 8 + b.r.nextInt(3);
		int[][] corners = {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}};
		for (int[] cc : corners) {
			b.set(cc[0], -1, cc[1], Blocks.COBBLESTONE.defaultBlockState());
			b.set(cc[0], 0, cc[1], Blocks.COBBLESTONE.defaultBlockState());
			for (int y = 1; y <= H + 2; y++) {
				b.set(cc[0], y, cc[1], Bp.log(p.log, Direction.Axis.Y));
			}
		}
		// Cross beams at two heights.
		for (int y : new int[]{3, H - 3}) {
			if (y < 2) {
				continue;
			}
			for (int x = x0 + 1; x < x1; x++) {
				b.set(x, y, z0, Bp.log(p.strippedLog, Direction.Axis.X));
				b.set(x, y, z1, Bp.log(p.strippedLog, Direction.Axis.X));
			}
			for (int z = z0 + 1; z < z1; z++) {
				b.set(x0, y, z, Bp.log(p.strippedLog, Direction.Axis.Z));
				b.set(x1, y, z, Bp.log(p.strippedLog, Direction.Axis.Z));
			}
		}
		// Platform with a railing.
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
				if (corner) {
					continue;
				}
				b.set(x, H, z, p.planks.defaultBlockState());
				boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
				if (edge) {
					b.set(x, H + 1, z, p.fence.defaultBlockState());
				}
			}
		}
		// The ladder up the front, fixed to the corner post.
		for (int y = 1; y <= H; y++) {
			b.set(x0 + 1, y, z1, Bp.facing(Blocks.LADDER, Direction.EAST));
		}
		b.air(x0 + 1, H + 1, z1);
		// Roof.
		boolean over = x0 >= 1 && z0 >= 1 && x1 <= w - 2 && z1 <= d - 2;
		if (over) {
			b.pyramidRoof(x0 - 1, x1 + 1, z0 - 1, z1 + 1, H + 3, p.roofStairs, p.roofFull, p.fence.defaultBlockState());
		} else {
			b.pyramidRoof(x0, x1, z0, z1, H + 3, p.roofStairs, p.roofFull, p.fence.defaultBlockState());
		}
		// On the platform: the alarm bell, a barrel, a lamp on the railing.
		soft(b, x1 - 1, H + 1, z0 + 1, Blocks.BELL.defaultBlockState());
		soft(b, x0 + 1, H + 1, z0 + 1, barrel(Direction.UP));
		if (mx != x0 + 1) {
			put(b, mx, H + 2, z0, Bp.lantern(false));
		}
		// At the foot: firewood, a water barrel.
		if (x1 + 1 <= w - 1) {
			for (int z = z0; z <= z1 - 1; z++) {
				soft(b, x1 + 1, 1, z, Bp.log(p.log, Direction.Axis.Z));
			}
		}
		soft(b, x0 - 1, 1, z1, barrel(Direction.UP));
	}

	// ================================================================ 5. wayside shrine

	/**
	 * A roadside marker in a 3×3 plot: a wooden голубец (Orthodox cross under a little roof), a Latin cross of wood or
	 * stone, an Irish high cross, a Georgian/Armenian stone stela, a small domed marker or stela in the Islamic lands, a
	 * Norse runestone, a steppe каменная баба on a mound, a sacred stone of the Balts and Finns.
	 */
	static void shrine(Bp b, Palette p, Culture c) {
		int cx = b.w / 2, cz = b.d / 2;
		Block ws = woodStairs(p);
		switch (c) {
			case RUS -> golubets(b, p, cx, cz, ws);
			case BYZANTINE -> {
				if (b.r.nextBoolean()) {
					golubets(b, p, cx, cz, ws);
				} else {
					stoneCross(b, cx, cz, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICK_WALL.defaultBlockState(), false);
				}
			}
			case CAUCASUS -> {
				// A stone stela with a carved cross (khachkar-like) on a stepped base.
				put(b, cx, 1, cz, Blocks.TUFF_BRICKS.defaultBlockState());
				put(b, cx - 1, 1, cz, Bp.stairs(Blocks.TUFF_BRICK_STAIRS, Direction.EAST, false));
				put(b, cx + 1, 1, cz, Bp.stairs(Blocks.TUFF_BRICK_STAIRS, Direction.WEST, false));
				put(b, cx, 2, cz, Blocks.CHISELED_TUFF_BRICKS.defaultBlockState());
				put(b, cx, 3, cz, Blocks.CHISELED_TUFF.defaultBlockState());
				put(b, cx, 4, cz, Bp.slab(Blocks.TUFF_BRICK_SLAB, false));
				put(b, cx, 1, cz + 1, candle(1 + b.r.nextInt(3)));
			}
			case ANDALUSI, ARAB, PERSIAN -> {
				BlockState wall = p.stone, white = p.plaster;
				if (b.r.nextBoolean()) {
					// A tiny domed marker over a cenotaph.
					for (int dx = -1; dx <= 1; dx += 2) {
						for (int dz = -1; dz <= 1; dz += 2) {
							put(b, cx + dx, 1, cz + dz, wall);
							put(b, cx + dx, 2, cz + dz, wall);
						}
					}
					for (int x = cx - 1; x <= cx + 1; x++) {
						for (int z = cz - 1; z <= cz + 1; z++) {
							put(b, x, 3, z, wall);
						}
					}
					put(b, cx, 4, cz, white);
					put(b, cx - 1, 4, cz, Bp.stairs(p.roofStairs, Direction.EAST, false));
					put(b, cx + 1, 4, cz, Bp.stairs(p.roofStairs, Direction.WEST, false));
					put(b, cx, 4, cz - 1, Bp.stairs(p.roofStairs, Direction.SOUTH, false));
					put(b, cx, 4, cz + 1, Bp.stairs(p.roofStairs, Direction.NORTH, false));
					put(b, cx, 5, cz, c == Culture.PERSIAN ? Blocks.DYED_TERRACOTTA.pick(DyeColor.CYAN).defaultBlockState() : Bp.slab(p.roofSlab, false));
					put(b, cx, 1, cz, Bp.slab(Blocks.SMOOTH_SANDSTONE_SLAB, false));
					put(b, cx, 2, cz, Bp.lantern(true));
				} else {
					// A stela with a little cap and flowers.
					put(b, cx, 1, cz, p.stone2);
					put(b, cx, 2, cz, Blocks.CHISELED_SANDSTONE.defaultBlockState());
					put(b, cx, 3, cz, Blocks.SANDSTONE_WALL.defaultBlockState());
					put(b, cx - 1, 1, cz, Blocks.POTTED_RED_TULIP.defaultBlockState());
					put(b, cx + 1, 1, cz, Blocks.DECORATED_POT.defaultBlockState());
				}
			}
			case NORSE -> {
				// A runestone: a rough slab of stone with a carved face and a smooth top.
				put(b, cx, 1, cz, Blocks.STONE.defaultBlockState());
				put(b, cx, 2, cz, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
				put(b, cx, 3, cz, Bp.slab(Blocks.SMOOTH_STONE_SLAB, false));
				if (b.r.nextBoolean()) {
					put(b, cx + 1, 1, cz, Blocks.ANDESITE.defaultBlockState());
					put(b, cx + 1, 2, cz, Bp.slab(Blocks.SMOOTH_STONE_SLAB, false));
				}
				put(b, cx - 1, 1, cz, Blocks.MOSS_CARPET.defaultBlockState());
				put(b, cx, 1, cz + 1, Blocks.MOSS_CARPET.defaultBlockState());
			}
			case STEPPE -> {
				// A каменная баба on a little kurgan.
				for (int x = cx - 1; x <= cx + 1; x++) {
					for (int z = cz - 1; z <= cz + 1; z++) {
						put(b, x, 0, z, Blocks.COARSE_DIRT.defaultBlockState());
					}
				}
				put(b, cx, 1, cz, Blocks.COARSE_DIRT.defaultBlockState());
				put(b, cx, 2, cz, Blocks.POLISHED_ANDESITE.defaultBlockState());
				put(b, cx, 3, cz, Blocks.POLISHED_ANDESITE.defaultBlockState());
				put(b, cx, 4, cz, Blocks.ANDESITE_WALL.defaultBlockState());
				put(b, cx - 1, 1, cz + 1, Blocks.COBBLESTONE.defaultBlockState());
				put(b, cx + 1, 1, cz - 1, Blocks.SHORT_GRASS.defaultBlockState());
			}
			case BALT_FINN -> {
				if (b.r.nextInt(3) == 0) {
					golubets(b, p, cx, cz, ws);
				} else {
					// A sacred stone with offerings.
					put(b, cx, 1, cz, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
					put(b, cx + 1, 1, cz, Blocks.COBBLESTONE.defaultBlockState());
					put(b, cx, 2, cz, Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState());
					put(b, cx - 1, 1, cz, candle(2));
					put(b, cx, 1, cz + 1, Blocks.LILY_OF_THE_VALLEY.defaultBlockState());
				}
			}
			case CELTIC -> stoneCross(b, cx, cz, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState(), true);
			default -> {
				if (b.r.nextBoolean()) {
					// A wooden Latin cross.
					put(b, cx, 1, cz, Bp.log(p.log, Direction.Axis.Y));
					for (int y = 2; y <= 4; y++) {
						put(b, cx, y, cz, p.fence.defaultBlockState());
					}
					put(b, cx - 1, 3, cz, p.fence.defaultBlockState());
					put(b, cx + 1, 3, cz, p.fence.defaultBlockState());
				} else {
					stoneCross(b, cx, cz, Blocks.COBBLESTONE.defaultBlockState(), Blocks.COBBLESTONE_WALL.defaultBlockState(), false);
				}
				put(b, cx, 1, cz + 1, b.r.nextBoolean() ? candle(1) : Blocks.POPPY.defaultBlockState());
			}
		}
	}

	/** An Orthodox wooden cross under a little gable roof (голубец). */
	private static void golubets(Bp b, Palette p, int cx, int cz, Block ws) {
		put(b, cx, 1, cz, Bp.log(p.strippedLog, Direction.Axis.Y));
		for (int y = 2; y <= 4; y++) {
			put(b, cx, y, cz, p.fence.defaultBlockState());
		}
		put(b, cx - 1, 3, cz, p.fence.defaultBlockState());
		put(b, cx + 1, 3, cz, p.fence.defaultBlockState());
		put(b, cx - 1, 5, cz, Bp.stairs(ws, Direction.EAST, false));
		put(b, cx + 1, 5, cz, Bp.stairs(ws, Direction.WEST, false));
		put(b, cx, 5, cz, p.planks.defaultBlockState());
		put(b, cx, 6, cz, Bp.slab(woodSlab(p), false));
		put(b, cx, 1, cz + 1, candle(1));
	}

	/** A stone cross on a base; a high cross with a ring for the Irish. */
	private static void stoneCross(Bp b, int cx, int cz, BlockState base, BlockState shaft, boolean high) {
		put(b, cx, 1, cz, base);
		int top = high ? 5 : 4;
		for (int y = 2; y <= top; y++) {
			put(b, cx, y, cz, shaft);
		}
		int arm = top - 1;
		put(b, cx - 1, arm, cz, shaft);
		put(b, cx + 1, arm, cz, shaft);
		if (high) {
			put(b, cx, top + 1, cz, shaft);
			put(b, cx - 1, arm + 1, cz, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true));
			put(b, cx + 1, arm + 1, cz, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true));
			put(b, cx - 1, arm - 1, cz, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false));
			put(b, cx + 1, arm - 1, cz, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false));
		}
	}

	// ================================================================ 6. cemetery

	/**
	 * A churchyard or cemetery: a low wall or fence with a gate to the street, a path, rows of graves — mounds with
	 * wooden crosses (with a little roof for the Orthodox), stone slabs for the rich, Muslim graves framed in stone with a
	 * headstone, kurgans with stone figures in the steppe — yews, birches or cypresses, candles, and on big plots an
	 * ossuary chapel (or a domed tomb).
	 */
	static void cemetery(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d;
		if (w < 5 || d < 5) {
			return;
		}
		boolean muslim = islamic(c);
		boolean steppe = c == Culture.STEPPE;
		boolean orthodox = c == Culture.RUS || c == Culture.BYZANTINE || c == Culture.CAUCASUS;
		boolean stoneWall = c == Culture.LATIN || c == Culture.CELTIC || c == Culture.BYZANTINE || c == Culture.CAUCASUS || muslim;
		boolean richOk = c == Culture.LATIN || c == Culture.BYZANTINE || c == Culture.CAUCASUS || c == Culture.CELTIC;
		int mx = w / 2;
		if (steppe) {
			kurgans(b, p);
			return;
		}
		boolean chapel = w >= 13 && d >= 13 && ((w >= 15 && d >= 15) || b.r.nextBoolean());
		// The path.
		for (int z = 1; z <= d - 2; z++) {
			if (stoneWall) {
				b.set(mx, -1, z, Blocks.COBBLESTONE.defaultBlockState());
				b.set(mx, 0, z, Blocks.GRAVEL.defaultBlockState());
			} else {
				b.set(mx, 0, z, Blocks.DIRT_PATH.defaultBlockState());
			}
		}
		// The chapel or tomb at the back.
		if (chapel) {
			graveChapel(b, p, c, mx - 2, 1, mx + 2, 5, muslim);
		}
		// Rows of graves either side of the path.
		for (int z = 2; z <= d - 3; z += 3) {
			for (int side = -1; side <= 1; side += 2) {
				for (int hx = mx + 2 * side; side < 0 ? hx - 2 >= 1 : hx + 2 <= w - 2; hx += 3 * side) {
					if (chapel && z <= 6 && Math.abs(hx - mx) <= 4) {
						continue;
					}
					if (b.r.nextInt(5) == 0) {
						continue;
					}
					int m1 = hx + side, m2 = hx + 2 * side;
					if (muslim) {
						muslimGrave(b, p, c, hx, z, m1, m2);
					} else {
						christianGrave(b, p, c, hx, z, m1, m2, orthodox, richOk && b.r.nextInt(4) == 0);
					}
				}
			}
		}
		// Trees: yews or birches in the north, cypresses in the south.
		int[][] spots = {{1, 1}, {w - 2, 1}, {1, d - 3}, {w - 2, d - 3}};
		int trees = 1 + b.r.nextInt(3);
		for (int i = 0; i < trees && i < spots.length; i++) {
			int[] s = spots[i];
			if (b.has(s[0], 1, s[1])) {
				continue;
			}
			if (muslim || c == Culture.BYZANTINE || c == Culture.CAUCASUS) {
				cypress(b, s[0], s[1], 6 + b.r.nextInt(2));
			} else if (c == Culture.RUS || c == Culture.BALT_FINN || b.r.nextBoolean()) {
				tree(b, s[0], s[1], Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, null, 3, 1);
			} else {
				tree(b, s[0], s[1], Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, Blocks.SPRUCE_LEAVES, 2, 2);
			}
		}
		// The enclosure with its gate.
		BlockState fenceS = p.fence.defaultBlockState();
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				boolean edge = x == 0 || x == w - 1 || z == 0 || z == d - 1;
				if (!edge || (z == d - 1 && Math.abs(x - mx) <= 1)) {
					continue;
				}
				if (muslim) {
					b.set(x, 1, z, p.stone);
					b.set(x, 2, z, Bp.slab(p.roofSlab, false));
				} else if (stoneWall) {
					b.set(x, 1, z, b.r.nextInt(4) == 0 ? Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState() : Blocks.COBBLESTONE_WALL.defaultBlockState());
				} else {
					b.set(x, 1, z, fenceS);
				}
			}
		}
		int gz = d - 1;
		if (muslim) {
			for (int y = 1; y <= 3; y++) {
				b.set(mx - 1, y, gz, p.stone2);
				b.set(mx + 1, y, gz, p.stone2);
			}
			b.set(mx, 3, gz, Bp.stairs(p.roofStairs, Direction.SOUTH, true));
			b.set(mx, 4, gz, Bp.slab(p.roofSlab, false));
		} else {
			b.set(mx, 1, gz, Bp.facing(p.gate, Direction.SOUTH));
			BlockState postS = stoneWall ? Blocks.COBBLESTONE.defaultBlockState() : Bp.log(p.log, Direction.Axis.Y);
			for (int y = 1; y <= 3; y++) {
				b.set(mx - 1, y, gz, postS);
				b.set(mx + 1, y, gz, postS);
			}
			if (c == Culture.RUS || c == Culture.BALT_FINN || c == Culture.WEST_SLAV) {
				// A roofed lych gate with a cross.
				Block ws = woodStairs(p);
				b.set(mx - 1, 4, gz, Bp.stairs(ws, Direction.EAST, false));
				b.set(mx + 1, 4, gz, Bp.stairs(ws, Direction.WEST, false));
				b.set(mx, 4, gz, p.planks.defaultBlockState());
				b.set(mx, 5, gz, p.fence.defaultBlockState());
				b.set(mx, 6, gz, p.fence.defaultBlockState());
				b.set(mx - 1, 6, gz, p.fence.defaultBlockState());
				b.set(mx + 1, 6, gz, p.fence.defaultBlockState());
				b.set(mx, 7, gz, p.fence.defaultBlockState());
			} else {
				b.set(mx - 1, 4, gz, Bp.lantern(false));
				b.set(mx + 1, 4, gz, Bp.lantern(false));
			}
		}
	}

	private static void christianGrave(Bp b, Palette p, Culture c, int hx, int z, int m1, int m2, boolean orthodox, boolean rich) {
		if (rich) {
			Block slab = c == Culture.CAUCASUS ? Blocks.TUFF_BRICK_SLAB : c == Culture.BYZANTINE ? Blocks.POLISHED_DIORITE_SLAB : Blocks.STONE_BRICK_SLAB;
			BlockState head = c == Culture.CAUCASUS ? Blocks.CHISELED_TUFF_BRICKS.defaultBlockState() : Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
			BlockState shaft = c == Culture.CAUCASUS ? Blocks.TUFF_BRICK_WALL.defaultBlockState() : Blocks.STONE_BRICK_WALL.defaultBlockState();
			put(b, m1, 1, z, Bp.slab(slab, false));
			put(b, m2, 1, z, Bp.slab(slab, false));
			put(b, hx, 1, z, head);
			put(b, hx, 2, z, shaft);
			put(b, hx, 3, z, shaft);
			put(b, hx, 3, z - 1, shaft);
			put(b, hx, 3, z + 1, shaft);
			return;
		}
		for (int mxx : new int[]{m1, m2}) {
			int k = b.r.nextInt(3);
			put(b, mxx, 0, z, k == 0 ? Blocks.PODZOL.defaultBlockState() : k == 1 ? Blocks.ROOTED_DIRT.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
			int dec = b.r.nextInt(8);
			if (dec == 0) {
				put(b, mxx, 1, z, candle(1 + b.r.nextInt(3)));
			} else if (dec == 1) {
				put(b, mxx, 1, z, Blocks.SHORT_GRASS.defaultBlockState());
			} else if (dec == 2) {
				put(b, mxx, 1, z, b.r.nextBoolean() ? Blocks.POPPY.defaultBlockState() : Blocks.CORNFLOWER.defaultBlockState());
			}
		}
		if (b.r.nextInt(6) == 0) {
			// A poor grave with only a field stone.
			put(b, hx, 1, z, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
			return;
		}
		put(b, hx, 1, z, p.fence.defaultBlockState());
		put(b, hx, 2, z, p.fence.defaultBlockState());
		put(b, hx, 2, z - 1, p.fence.defaultBlockState());
		put(b, hx, 2, z + 1, p.fence.defaultBlockState());
		if (orthodox) {
			put(b, hx, 3, z, Bp.slab(woodSlab(p), false));
		} else {
			put(b, hx, 3, z, p.fence.defaultBlockState());
		}
	}

	private static void muslimGrave(Bp b, Palette p, Culture c, int hx, int z, int m1, int m2) {
		BlockState stone = c == Culture.ANDALUSI ? Blocks.DIORITE_WALL.defaultBlockState() : Blocks.SANDSTONE_WALL.defaultBlockState();
		put(b, m1, 0, z, Blocks.COARSE_DIRT.defaultBlockState());
		put(b, m2, 0, z, Blocks.COARSE_DIRT.defaultBlockState());
		if (b.r.nextInt(4) == 0) {
			// A built tomb.
			put(b, m1, 1, z, p.stone2);
			put(b, m2, 1, z, p.stone2);
			put(b, m1, 2, z, Bp.slab(p.roofSlab, false));
			put(b, m2, 2, z, Bp.slab(p.roofSlab, false));
		} else {
			put(b, m1, 1, z, Bp.slab(p.roofSlab, false));
			put(b, m2, 1, z, Bp.slab(p.roofSlab, false));
		}
		put(b, hx, 1, z, stone);
		put(b, hx, 2, z, stone);
	}

	/** A small ossuary chapel (Christians) or a domed tomb (Muslims). */
	private static void graveChapel(Bp b, Palette p, Culture c, int x0, int z0, int x1, int z1, boolean muslim) {
		int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
		b.fill(x0, -2, z0, x1, -1, z1, Blocks.COBBLESTONE.defaultBlockState());
		if (muslim) {
			b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, p.floor);
			for (int y = 0; y <= 3; y++) {
				for (int x = x0; x <= x1; x++) {
					for (int z = z0; z <= z1; z++) {
						if (x == x0 || x == x1 || z == z0 || z == z1) {
							b.set(x, y, z, y == 0 ? p.stone2 : p.stone);
						}
					}
				}
			}
			b.fill(x0, 4, z0, x1, 4, z1, p.stone);
			BlockState shell = c == Culture.PERSIAN ? Blocks.DYED_TERRACOTTA.pick(DyeColor.CYAN).defaultBlockState() : p.plaster;
			b.dome(mx, 5, mz, 2.3, shell, p.stone2);
			b.air(mx, 1, z1);
			b.air(mx, 2, z1);
			b.set(mx, 3, z1, Bp.stairs(p.roofStairs, Direction.SOUTH, true));
			for (int x = mx - 1; x <= mx + 1; x++) {
				b.set(x, 1, mz - 1, Bp.slab(p.roofSlab, false));
			}
			b.set(mx, 1, mz - 1, p.stone2);
			b.set(mx, 3, mz, Bp.lantern(true));
			soft(b, mx - 1, 1, mz, carpet(DyeColor.GREEN));
			soft(b, mx + 1, 1, mz, carpet(DyeColor.GREEN));
			return;
		}
		int st = c == Culture.RUS || c == Culture.BALT_FINN ? LOG : c == Culture.NORSE ? FRAME : STONE;
		b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, st == STONE ? Blocks.COBBLESTONE.defaultBlockState() : p.planks.defaultBlockState());
		box(b, p, c, st, x0, z0, x1, z1, 3);
		roof(b, p, c, false, x0, x1, z0, z1, 4, false, 0);
		b.door(p.door, mx, 1, z1, Direction.SOUTH);
		b.air(mx, 1, z1 - 1);
		b.air(mx, 2, z1 - 1);
		b.air(x0, 2, mz);
		b.air(x1, 2, mz);
		int ridge = p.thatch ? 4 + (x1 - x0 + 2) / 2 : 4 + (x1 - x0) / 2;
		b.cross(mx, ridge + 1, z1);
		// The ossuary: bones stacked against the back wall, skulls on top, a lectern and candles.
		for (int x = x0 + 1; x <= x1 - 1; x++) {
			b.set(x, 1, z0 + 1, Blocks.BONE_BLOCK.defaultBlockState());
			b.set(x, 2, z0 + 1, b.r.nextBoolean() ? Blocks.BONE_BLOCK.defaultBlockState() : Blocks.SKELETON_SKULL.defaultBlockState());
		}
		soft(b, mx, 1, mz, Blocks.LECTERN.defaultBlockState());
		soft(b, mx - 1, 1, mz, candle(3));
		soft(b, mx + 1, 1, mz, candle(2));
	}

	/** Kurgans of the steppe: earth mounds, some crowned by a stone figure, rings of stones. */
	private static void kurgans(Bp b, Palette p) {
		boolean any = false;
		for (int cx = 2; cx <= b.w - 3; cx += 5) {
			for (int cz = 2; cz <= b.d - 3; cz += 5) {
				boolean last = cx + 5 > b.w - 3 && cz + 5 > b.d - 3;
				if (b.r.nextInt(4) == 0 && !(last && !any)) {
					continue;
				}
				any = true;
				for (int x = cx - 1; x <= cx + 1; x++) {
					for (int z = cz - 1; z <= cz + 1; z++) {
						put(b, x, 0, z, Blocks.COARSE_DIRT.defaultBlockState());
						if (x == cx || z == cz) {
							put(b, x, 1, z, Blocks.GRASS_BLOCK.defaultBlockState());
						}
					}
				}
				put(b, cx, 1, cz, Blocks.COARSE_DIRT.defaultBlockState());
				if (b.r.nextBoolean()) {
					put(b, cx, 2, cz, Blocks.POLISHED_ANDESITE.defaultBlockState());
					put(b, cx, 3, cz, Blocks.POLISHED_ANDESITE.defaultBlockState());
					put(b, cx, 4, cz, Blocks.ANDESITE_WALL.defaultBlockState());
				} else {
					put(b, cx, 2, cz, Blocks.SHORT_GRASS.defaultBlockState());
				}
				for (int[] dv : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
					if (b.r.nextBoolean()) {
						put(b, cx + dv[0], 1, cz + dv[1], Blocks.COBBLESTONE.defaultBlockState());
					}
				}
			}
		}
	}

	// ================================================================ 7. garden

	/**
	 * A garden: an orchard of blossoming fruit trees with beehives and vegetable beds behind a wattle fence in the north
	 * (or a monastery herb garden; a vineyard with buried wine jars in the Caucasus); a chahar bagh in the Islamic lands —
	 * four quarters parted by water channels meeting at a central pool, cypresses, orange trees, flower beds and a
	 * pavilion; a cloister garden with a portico and a fountain in Byzantium.
	 */
	static void garden(Bp b, Palette p, Culture c) {
		if (b.w < 7 || b.d < 7) {
			return;
		}
		switch (c) {
			case ANDALUSI, ARAB, PERSIAN -> chaharBagh(b, p, c);
			case BYZANTINE -> cloister(b, p, c);
			case CAUCASUS -> {
				if (b.r.nextInt(3) > 0) {
					vineyard(b, p, c);
				} else {
					orchard(b, p, c);
				}
			}
			default -> orchard(b, p, c);
		}
	}

	private static BlockState flower(java.util.Random r) {
		return switch (r.nextInt(9)) {
			case 0 -> Blocks.POPPY.defaultBlockState();
			case 1 -> Blocks.RED_TULIP.defaultBlockState();
			case 2 -> Blocks.ORANGE_TULIP.defaultBlockState();
			case 3 -> Blocks.WHITE_TULIP.defaultBlockState();
			case 4 -> Blocks.CORNFLOWER.defaultBlockState();
			case 5 -> Blocks.ALLIUM.defaultBlockState();
			case 6 -> Blocks.OXEYE_DAISY.defaultBlockState();
			case 7 -> Blocks.LILY_OF_THE_VALLEY.defaultBlockState();
			default -> Blocks.AZURE_BLUET.defaultBlockState();
		};
	}

	private static void fruitTree(Bp b, int x, int z) {
		Block blossom = b.r.nextInt(3) == 0 ? Blocks.CHERRY_LEAVES : Blocks.FLOWERING_AZALEA_LEAVES;
		tree(b, x, z, Blocks.OAK_LOG, Blocks.OAK_LEAVES, blossom, 2 + b.r.nextInt(2), b.r.nextInt(3) == 0 ? 2 : 1);
	}

	private static void orchard(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d, mx = w / 2;
		int variant = b.r.nextInt(3);
		boolean logHives = c == Culture.RUS || c == Culture.BALT_FINN || c == Culture.WEST_SLAV;
		// The path from the gate.
		for (int z = 1; z <= d - 2; z++) {
			b.set(mx, 0, z, Blocks.DIRT_PATH.defaultBlockState());
		}
		// Beehives along the back fence.
		for (int x : new int[]{1, 3, w - 4, w - 2}) {
			if (x <= 0 || x >= w - 1 || Math.abs(x - mx) <= 1 || b.r.nextInt(4) == 0) {
				continue;
			}
			b.set(x, 1, 1, Bp.log(logHives ? p.log : Blocks.OAK_LOG, Direction.Axis.Y));
			b.set(x, 2, 1, logHives ? Bp.facing(Blocks.BEE_NEST, Direction.SOUTH) : Bp.facing(Blocks.BEEHIVE, Direction.SOUTH));
		}
		if (variant == 1) {
			// A monastery herb garden: four square beds round a well head, fruit trees in the corners.
			int mz = d / 2;
			for (int x = 1; x <= w - 2; x++) {
				b.set(x, 0, mz, Blocks.DIRT_PATH.defaultBlockState());
			}
			for (int x = 1; x <= w - 2; x++) {
				for (int z = 2; z <= d - 2; z++) {
					if (x == mx || z == mz || b.has(x, 1, z)) {
						continue;
					}
					boolean border = x == 1 || x == w - 2 || z == 2 || z == d - 2 || Math.abs(x - mx) == 1 || Math.abs(z - mz) == 1;
					if (border && (x + z) % 4 == 0) {
						b.set(x, 1, z, Blocks.AZALEA.defaultBlockState());
					} else if (!border) {
						b.set(x, 0, z, Blocks.GRASS_BLOCK.defaultBlockState());
						int k = b.r.nextInt(5);
						if (k < 3) {
							b.set(x, 1, z, flower(b.r));
						} else if (k == 3) {
							b.set(x, 1, z, Blocks.FERN.defaultBlockState());
						}
					}
				}
			}
			b.set(mx, 0, mz, Blocks.COBBLESTONE.defaultBlockState());
			b.set(mx, 1, mz, waterCauldron());
			if (w >= 11 && d >= 11) {
				fruitTree(b, 2, 3);
				fruitTree(b, w - 3, 3);
			}
			soft(b, mx + 1, 1, mz + 2, Bp.stairs(woodStairs(p), Direction.EAST, false));
			soft(b, mx + 1, 1, mz + 3, Bp.stairs(woodStairs(p), Direction.EAST, false));
		} else {
			// An orchard: fruit trees in rows, grass and flowers under them; vegetable beds in front (or a meadow).
			int zb = variant == 0 ? d - 1 - Math.max(3, (d - 2) / 3) : d - 2;
			for (int x = 1; x <= w - 2; x++) {
				for (int z = 2; z < zb; z++) {
					if (x == mx) {
						continue;
					}
					int k = b.r.nextInt(8);
					if (k == 0) {
						soft(b, x, 1, z, flower(b.r));
					} else if (k == 1) {
						soft(b, x, 1, z, Blocks.SHORT_GRASS.defaultBlockState());
					}
				}
			}
			for (int x = 2; x <= w - 3; x += 4) {
				for (int z = 3; z <= zb - 2; z += 4) {
					int tx = Math.abs(x - mx) <= 1 ? x + (x < mx ? -1 : 2) : x;
					if (tx >= 1 && tx <= w - 2 && tx != mx) {
						fruitTree(b, tx, z);
					}
				}
			}
			if (variant == 0 && zb <= d - 3) {
				Houses.garden(b, 1, zb, mx - 1, d - 2, b.r);
				Houses.garden(b, mx + 1, zb, w - 2, d - 2, b.r);
				soft(b, 1, 1, zb - 1, barrel(Direction.UP));
				soft(b, 2, 1, zb - 1, waterCauldron());
			} else {
				// A meadow with a bench and more hives.
				soft(b, mx + 1, 1, d - 3, Bp.stairs(woodStairs(p), Direction.EAST, false));
				soft(b, mx + 1, 1, d - 4, Bp.stairs(woodStairs(p), Direction.EAST, false));
				for (int x = 1; x < mx - 1; x += 2) {
					soft(b, x, 1, d - 3, logHives ? Bp.facing(Blocks.BEE_NEST, Direction.SOUTH) : Bp.facing(Blocks.BEEHIVE, Direction.SOUTH));
				}
			}
			if (w >= 11 && d >= 11 && b.r.nextBoolean()) {
				// A small pond with lily pads.
				int px = w - 3, pz = 1;
				if (!b.has(px, 1, pz) && !b.has(px + 1, 1, pz)) {
					b.set(px, -1, pz, Blocks.DIRT.defaultBlockState());
					b.set(px, 0, pz, Blocks.WATER.defaultBlockState());
					b.set(px, 1, pz, Blocks.LILY_PAD.defaultBlockState());
				}
			}
		}
		Houses.fenceRing(b, p, 0, 0, w - 1, d - 1, mx);
	}

	/** A Georgian vineyard: rows of vines on trellises, wine jars buried in the ground, a press, fruit trees at the front. */
	private static void vineyard(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d, mx = w / 2;
		for (int z = 1; z <= d - 2; z++) {
			b.set(mx, 0, z, Blocks.DIRT_PATH.defaultBlockState());
		}
		int zEnd = d - 4;
		for (int x = 1; x <= w - 2; x += 2) {
			if (Math.abs(x - mx) <= 0) {
				continue;
			}
			for (int z = 3; z <= zEnd; z++) {
				boolean postZ = (z - 3) % 3 == 0 || z == zEnd;
				b.set(x, 0, z, Blocks.COARSE_DIRT.defaultBlockState());
				if (postZ) {
					b.set(x, 1, z, p.fence.defaultBlockState());
				}
				b.set(x, 2, z, leaves(b.r.nextInt(4) == 0 ? Blocks.JUNGLE_LEAVES : Blocks.OAK_LEAVES));
			}
		}
		// The marani at the back: kvevri jars sunk in the ground, a press trough and barrels.
		for (int x = 1; x <= Math.min(w - 2, mx - 2); x++) {
			if (b.r.nextInt(3) > 0) {
				b.set(x, 0, 1, Blocks.DECORATED_POT.defaultBlockState());
			}
		}
		soft(b, mx + 2, 1, 1, Blocks.CAULDRON.defaultBlockState());
		soft(b, mx + 3, 1, 1, barrel(Direction.UP));
		soft(b, mx + 4, 1, 1, barrel(Direction.EAST));
		fruitTree(b, 1, d - 2);
		if (w - 2 > mx + 1) {
			tree(b, w - 2, d - 2, Blocks.JUNGLE_LOG, Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES, 2, 1);
		}
		BlockState wallS = Blocks.COBBLESTONE_WALL.defaultBlockState();
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				boolean edge = x == 0 || x == w - 1 || z == 0 || z == d - 1;
				if (edge && !(z == d - 1 && x == mx)) {
					soft(b, x, 1, z, wallS);
				}
			}
		}
		b.set(mx, 1, d - 1, Bp.facing(p.gate, Direction.SOUTH));
	}

	/**
	 * A chahar bagh: a walled garden of four quarters divided by two water channels in stone rims that meet at a central
	 * pool with a basin; cypresses at the corners of the quarters, orange trees and flower beds in them, a pavilion at
	 * the head of the long channel.
	 */
	private static void chaharBagh(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d;
		int mx = w / 2, mz = d / 2;
		BlockState walk = c == Culture.ANDALUSI ? Blocks.CALCITE.defaultBlockState() : c == Culture.PERSIAN ? Blocks.BRICKS.defaultBlockState() : p.stone2;
		BlockState rimUnder = p.stone2;
		boolean pavilion = d >= 13 && w >= 11;
		int zc0 = pavilion ? 6 : 2;
		// The quarters: grass and flowers.
		for (int x = 1; x <= w - 2; x++) {
			for (int z = 1; z <= d - 2; z++) {
				b.set(x, 0, z, Blocks.GRASS_BLOCK.defaultBlockState());
				if (b.r.nextInt(3) == 0) {
					b.set(x, 1, z, flower(b.r));
				}
			}
		}
		// The long channel and the cross channel with their walks.
		for (int z = 1; z <= d - 2; z++) {
			for (int x = mx - 1; x <= mx + 1; x++) {
				b.air(x, 1, z);
				b.set(x, -1, z, rimUnder);
				b.set(x, 0, z, walk);
			}
			if (z >= zc0 && z <= d - 3) {
				b.set(mx, 0, z, Blocks.WATER.defaultBlockState());
			}
		}
		for (int x = 1; x <= w - 2; x++) {
			for (int z = mz - 1; z <= mz + 1; z++) {
				b.air(x, 1, z);
				b.set(x, -1, z, rimUnder);
				b.set(x, 0, z, walk);
			}
			if (x >= 2 && x <= w - 3) {
				b.set(x, 0, mz, Blocks.WATER.defaultBlockState());
			}
		}
		// The central pool with its basin.
		for (int x = mx - 1; x <= mx + 1; x++) {
			for (int z = mz - 1; z <= mz + 1; z++) {
				b.set(x, 0, z, Blocks.WATER.defaultBlockState());
			}
		}
		for (int x = mx - 2; x <= mx + 2; x++) {
			for (int z = mz - 2; z <= mz + 2; z++) {
				boolean ring = Math.abs(x - mx) == 2 || Math.abs(z - mz) == 2;
				boolean channel = x == mx || z == mz;
				if (ring && !channel) {
					b.set(x, 0, z, walk);
					b.air(x, 1, z);
				}
			}
		}
		b.set(mx, 0, mz, p.stone2);
		b.set(mx, 1, mz, waterCauldron());
		// Cypresses at the inner corners of the quarters, orange trees in them.
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				int qx = mx + sx * 3, qz = mz + sz * 3;
				if (qx >= 1 && qx <= w - 2 && qz >= 1 && qz <= d - 2 && !(pavilion && qz <= 5 && Math.abs(qx - mx) <= 3)) {
					b.set(qx, 1, qz, AIR);
					cypress(b, qx, qz, 6 + b.r.nextInt(2));
				}
				int ox = mx + sx * Math.max(4, (w - 2) / 4 + 1), oz = mz + sz * Math.max(4, (d - 2) / 4 + 1);
				if (ox >= 1 && ox <= w - 2 && oz >= 1 && oz <= d - 2 && Math.abs(ox - mx) >= 3 && Math.abs(oz - mz) >= 3
						&& !(pavilion && oz <= 5)) {
					b.set(ox, 1, oz, AIR);
					tree(b, ox, oz, Blocks.JUNGLE_LOG, Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES, 2, 1);
				}
			}
		}
		// The pavilion at the head of the long channel.
		if (pavilion) {
			int x0 = mx - 2, x1 = mx + 2, z0 = 1, z1 = 5;
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					b.set(x, -1, z, rimUnder);
					b.set(x, 0, z, walk);
					for (int y = 1; y <= 3; y++) {
						boolean column = (x == x0 || x == x1) && (z == z0 || z == z1);
						b.set(x, y, z, column ? p.stone2 : AIR);
					}
					b.set(x, 4, z, p.stone);
				}
			}
			for (int x = x0 + 1; x <= x1 - 1; x++) {
				b.set(x, 3, z1, Bp.stairs(p.stairs, Direction.SOUTH, true));
				b.set(x, 3, z0, Bp.stairs(p.stairs, Direction.NORTH, true));
			}
			for (int z = z0 + 1; z <= z1 - 1; z++) {
				b.set(x0, 3, z, Bp.stairs(p.stairs, Direction.WEST, true));
				b.set(x1, 3, z, Bp.stairs(p.stairs, Direction.EAST, true));
			}
			BlockState shell = c == Culture.PERSIAN ? Blocks.DYED_TERRACOTTA.pick(DyeColor.CYAN).defaultBlockState() : p.plaster;
			b.dome(mx, 5, (z0 + z1) / 2, 2.2, shell, Blocks.GOLD_BLOCK.defaultBlockState());
			b.set(mx, 1, 3, waterCauldron());
			for (int x = x0 + 1; x <= x1 - 1; x++) {
				b.set(x, 1, z0 + 1, Blocks.WOOL_SLAB.pick(x % 2 == 0 ? DyeColor.RED : DyeColor.ORANGE).defaultBlockState());
			}
			b.set(x0 + 1, 1, 3, carpet(DyeColor.RED));
			b.set(x1 - 1, 1, 3, carpet(DyeColor.RED));
			b.set(mx, 3, 3, Bp.lantern(true));
		}
		// Benches along the cross walk.
		for (int x : new int[]{2, w - 3}) {
			put(b, x, 1, mz - 2, Bp.stairs(p.stairs, Direction.NORTH, false));
			put(b, x, 1, mz + 2, Bp.stairs(p.stairs, Direction.SOUTH, false));
		}
		// The wall with its gate.
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				boolean edge = x == 0 || x == w - 1 || z == 0 || z == d - 1;
				if (!edge) {
					continue;
				}
				b.set(x, 0, z, p.stone2);
				boolean gate = z == d - 1 && x == mx;
				b.set(x, 1, z, gate ? AIR : p.stone);
				b.set(x, 2, z, gate ? AIR : p.stone);
				b.set(x, 3, z, Bp.slab(p.roofSlab, false));
			}
		}
		// Lamps on the gate piers.
		b.set(mx - 1, 3, d - 1, p.stone);
		b.set(mx + 1, 3, d - 1, p.stone);
		b.set(mx - 1, 4, d - 1, Bp.lantern(false));
		b.set(mx + 1, 4, d - 1, Bp.lantern(false));
	}

	/** A Byzantine cloister garden: a portico of marble columns on all sides under a tiled roof, beds, cypresses, a fountain. */
	private static void cloister(Bp b, Palette p, Culture c) {
		int w = b.w, d = b.d;
		int mx = w / 2, mz = d / 2;
		BlockState wall = p.stone, band = p.stone2;
		// Outer wall, portico floor, columns and roof.
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				int k = Math.min(Math.min(x, z), Math.min(w - 1 - x, d - 1 - z));
				if (k == 0) {
					b.set(x, -1, z, band);
					for (int y = 0; y <= 4; y++) {
						b.set(x, y, z, y % 3 == 0 ? band : wall);
					}
					b.set(x, 5, z, p.roofFull);
				} else if (k <= 2) {
					b.set(x, 0, z, Blocks.POLISHED_ANDESITE.defaultBlockState());
					if (k == 2) {
						boolean ns = z == 2 || z == d - 3;
						int along = ns ? x : z;
						boolean column = along % 2 == 0 || (x == 2 || x == w - 3) && (z == 2 || z == d - 3);
						for (int y = 1; y <= 3; y++) {
							b.set(x, y, z, column ? Blocks.QUARTZ_PILLAR.defaultBlockState() : AIR);
						}
						b.set(x, 4, z, band);
						Direction out = z == 2 ? Direction.NORTH : z == d - 3 ? Direction.SOUTH : x == 2 ? Direction.WEST : Direction.EAST;
						b.set(x, 5, z, Bp.stairs(p.roofStairs, out, false));
					} else {
						b.set(x, 5, z, p.roofFull);
					}
				} else {
					b.set(x, 0, z, Blocks.GRASS_BLOCK.defaultBlockState());
				}
			}
		}
		// The entrance from the street.
		for (int y = 1; y <= 3; y++) {
			b.air(mx, y, d - 1);
		}
		b.set(mx, 4, d - 1, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true));
		for (int y = 1; y <= 3; y++) {
			b.air(mx, y, d - 3);
		}
		// The court: cross paths, beds, cypresses, a fountain.
		int cx0 = 3, cx1 = w - 4, cz0 = 3, cz1 = d - 4;
		for (int x = cx0; x <= cx1; x++) {
			for (int z = cz0; z <= cz1; z++) {
				if (x == mx || z == mz) {
					b.set(x, 0, z, Blocks.POLISHED_ANDESITE.defaultBlockState());
				} else if (b.r.nextInt(2) == 0) {
					b.set(x, 1, z, flower(b.r));
				}
			}
		}
		if (cx1 - cx0 >= 4 && cz1 - cz0 >= 4) {
			for (int x = mx - 1; x <= mx + 1; x++) {
				for (int z = mz - 1; z <= mz + 1; z++) {
					b.set(x, -1, z, band);
					b.set(x, 0, z, x == mx && z == mz ? band : Blocks.WATER.defaultBlockState());
					b.air(x, 1, z);
				}
			}
			for (int x = mx - 2; x <= mx + 2; x++) {
				for (int z = mz - 2; z <= mz + 2; z++) {
					if (Math.abs(x - mx) == 2 || Math.abs(z - mz) == 2) {
						b.set(x, 0, z, Blocks.POLISHED_DIORITE.defaultBlockState());
						b.air(x, 1, z);
					}
				}
			}
			b.set(mx, 1, mz, waterCauldron());
			if (cx1 - cx0 >= 6 && cz1 - cz0 >= 6) {
				for (int[] s : new int[][]{{cx0, cz0}, {cx1, cz0}, {cx0, cz1}, {cx1, cz1}}) {
					b.set(s[0], 1, s[1], AIR);
					cypress(b, s[0], s[1], 5 + b.r.nextInt(2));
				}
			}
		} else {
			b.set(mx, 1, mz, waterCauldron());
		}
		// Benches and lamps in the portico.
		for (int x = 3; x <= w - 4; x += 3) {
			if (x != mx) {
				soft(b, x, 1, 1, Bp.stairs(Blocks.POLISHED_ANDESITE_STAIRS, Direction.NORTH, false));
			}
		}
		for (int z = 3; z < d - 3; z += 4) {
			soft(b, 1, 4, z, Bp.lantern(true));
			soft(b, w - 2, 4, z, Bp.lantern(true));
		}
	}
}
