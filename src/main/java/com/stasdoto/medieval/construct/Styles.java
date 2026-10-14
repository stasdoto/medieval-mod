package com.stasdoto.medieval.construct;

import java.util.Random;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;

import com.stasdoto.medieval.history.Culture;
import com.stasdoto.medieval.settle.BKind;
import com.stasdoto.medieval.settle.Site;

/** Chooses and draws the building for a plot, in the way of the people living there. */
public final class Styles {
	private Styles() {
	}

	public static Bp build(BKind kind, Site site, long seed) {
		Random r = new Random(seed);
		Culture c = site.culture;
		Palette p = Palette.of(c, r);
		Bp b = new Bp(kind.w, kind.d, seed);
		switch (kind) {
			case HOMESTEAD -> homestead(b, p, c, r);
			case HOUSE, HOUSE_RICH -> townHouse(b, p, c, kind == BKind.HOUSE_RICH);
			case SMITHY -> Civic.smithy(b, p, c == Culture.LATIN || c == Culture.BYZANTINE || c == Culture.CAUCASUS || c == Culture.ARAB
					|| c == Culture.ANDALUSI || c == Culture.PERSIAN);
			case TAVERN -> Civic.tavern(b, p, c);
			case GRANARY -> Civic.granary(b, p);
			case STALL -> Civic.stall(b, r);
			case WELL -> Civic.well(b, p);
			case CELLS -> Civic.cells(b, p);
			case YURT -> Houses.yurt(b, 4, 4, r.nextInt(4) == 0 ? DyeColor.BROWN : DyeColor.WHITE);
			case CHURCH_SMALL -> {
				switch (c) {
					case RUS, BALT_FINN -> Holy.woodenChurch(b, p);
					case NORSE -> Holy.stave(b, p);
					case BYZANTINE, CAUCASUS -> Holy.crossDomed(b, p, false, false);
					default -> Holy.romanesque(b, p, false);
				}
			}
			case CHURCH -> {
				switch (c) {
					case RUS, BALT_FINN -> {
						if (site.kind.urban()) {
							Holy.crossDomed(b, p, false, true);
						} else {
							Holy.woodenChurch(b, p);
						}
					}
					case NORSE -> Holy.stave(b, p);
					case BYZANTINE, CAUCASUS -> Holy.crossDomed(b, p, false, false);
					default -> Holy.romanesque(b, p, false);
				}
			}
			case CATHEDRAL -> {
				switch (c) {
					case RUS, BALT_FINN -> Holy.crossDomed(b, p, true, true);
					case BYZANTINE, CAUCASUS -> Holy.crossDomed(b, p, true, false);
					default -> Holy.romanesque(b, p, true);
				}
			}
			case MOSQUE -> Holy.mosque(b, p, c);
			case HALL -> {
				switch (c) {
					case RUS, BALT_FINN, WEST_SLAV, MAGYAR -> Civic.terem(b, p);
					case NORSE, CELTIC -> Houses.longhouse(b, p, 3, 2, b.w - 4, b.d - 3);
					case ANDALUSI, ARAB, PERSIAN, STEPPE -> Civic.palace(b, p);
					default -> Civic.stoneHall(b, p);
				}
			}
			case KEEP -> Civic.keep(b, p, c == Culture.LATIN || c == Culture.BYZANTINE || c == Culture.CAUCASUS || c == Culture.CELTIC
					|| site.kind.ordinal() >= Site.Kind.CITY.ordinal());
		}
		return b;
	}

	/** A village yard: house, barn, garden, woodpile, fence and gate. */
	private static void homestead(Bp b, Palette p, Culture c, Random r) {
		int W = b.w, D = b.d;
		switch (c) {
			case RUS, BALT_FINN -> {
				int hw = 6 + r.nextInt(2), hd = 7 + r.nextInt(2);
				Houses.izba(b, p, 1, D - 2 - hd, hw, D - 3, 4, false);
				Houses.barn(b, p, W - 7, 1, W - 2, 6);
				Houses.garden(b, 1, 1, W - 9, D - hd - 5, r);
				Houses.woodpile(b, p, W - 6, D - 6, 4);
				Houses.fenceRing(b, p, 0, 0, W - 1, D - 1, W - 4);
			}
			case WEST_SLAV, MAGYAR -> {
				Houses.izba(b, p, 1, D - 10, 7, D - 3, 3, false);
				Houses.barn(b, p, W - 7, 1, W - 2, 6);
				Houses.garden(b, 1, 1, W - 9, D - 13, r);
				Houses.fenceRing(b, p, 0, 0, W - 1, D - 1, W - 4);
			}
			case LATIN -> {
				Houses.timberHouse(b, p, 1, D - 10, 8, D - 4, false);
				Houses.barn(b, p, W - 7, 1, W - 2, 7);
				Houses.garden(b, 1, 1, W - 9, D - 14, r);
				Houses.woodpile(b, p, W - 6, D - 5, 3);
				Houses.fenceRing(b, p, 0, 0, W - 1, D - 1, W - 4);
			}
			case CELTIC -> {
				Houses.roundhouse(b, p, 6, D - 8, 4);
				Houses.garden(b, 1, 1, W - 3, 7, r);
				Houses.fenceRing(b, p, 0, 0, W - 1, D - 1, W - 4);
			}
			case NORSE -> {
				Houses.longhouse(b, p, 2, 2, 8, D - 3);
				Houses.barn(b, p, W - 6, 1, W - 2, 6);
				Houses.fenceRing(b, p, 0, 0, W - 1, D - 1, W - 3);
			}
			case BYZANTINE, CAUCASUS -> {
				Houses.stoneHouse(b, p, 1, D - 10, 9, D - 3, 1);
				Houses.garden(b, 1, 1, W - 3, D - 13, r);
				for (int x = 0; x < W; x++) {
					b.soft(x, 1, 0, p.stone);
					b.soft(x, 1, D - 1, x == W - 4 ? Blocks.AIR.defaultBlockState() : p.stone);
				}
				for (int z = 0; z < D; z++) {
					b.soft(0, 1, z, p.stone);
					b.soft(W - 1, 1, z, p.stone);
				}
			}
			case ANDALUSI, ARAB, PERSIAN -> {
				Houses.courtyardHouse(b, p, 1, D - 14, W - 2, D - 2, false);
				Houses.garden(b, 1, 1, W - 3, D - 17, r);
				for (int x = 0; x < W; x++) {
					b.soft(x, 1, 0, p.stone);
					b.soft(x, 2, 0, p.stone);
				}
				for (int z = 0; z < D - 14; z++) {
					b.soft(0, 1, z, p.stone);
					b.soft(W - 1, 1, z, p.stone);
				}
			}
			case STEPPE -> Houses.yurt(b, W / 2, D / 2, DyeColor.WHITE);
		}
		// Yard surface where nothing else is.
		for (int x = 0; x < W; x++) {
			for (int z = 0; z < D; z++) {
				if (!b.has(x, 0, z)) {
					b.set(x, 0, z, r.nextInt(5) == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
				}
			}
		}
		// A path from the gate to the door.
		for (int z = D - 1; z >= D - 3; z--) {
			b.set(W - 4, 0, z, Blocks.DIRT_PATH.defaultBlockState());
		}
	}

	/** A house in a town. */
	private static void townHouse(Bp b, Palette p, Culture c, boolean rich) {
		int W = b.w, D = b.d;
		switch (c) {
			case RUS, BALT_FINN -> {
				Houses.izba(b, p, 1, 2, W - 2, D - 2, rich ? 5 : 4, true);
				Houses.fenceRing(b, p, 0, 0, W - 1, 1, -1);
			}
			case WEST_SLAV, MAGYAR -> Houses.izba(b, p, 1, 2, W - 2, D - 2, 4, true);
			case LATIN, CELTIC -> Houses.timberHouse(b, p, 1, 2, W - 2, D - 3, rich);
			case NORSE -> Houses.longhouse(b, p, 1, 0, W - 2, D - 2);
			case BYZANTINE, CAUCASUS -> Houses.stoneHouse(b, p, 1, 1, W - 2, D - 2, rich ? 3 : 2);
			case ANDALUSI, ARAB, PERSIAN, STEPPE -> Houses.courtyardHouse(b, p, 0, 0, W - 1, D - 1, rich);
		}
		boolean south = c == Culture.ANDALUSI || c == Culture.ARAB || c == Culture.PERSIAN || c == Culture.BYZANTINE;
		for (int x = 0; x < W; x++) {
			for (int z = 0; z < D; z++) {
				if (!b.has(x, 0, z)) {
					b.set(x, 0, z, south ? (b.r.nextInt(3) == 0 ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState())
							: b.r.nextInt(3) == 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
				}
			}
		}
		// A tree in some yards of the South: a cypress-like column or a fruit tree.
		if (south && b.r.nextInt(3) == 0) {
			int tx = 0, tz = 0;
			if (!b.has(tx, 1, tz)) {
				b.set(tx, 0, tz, Blocks.GRASS_BLOCK.defaultBlockState());
				for (int y = 1; y <= 5; y++) {
					b.set(tx, y, tz, y <= 2 ? Bp.log(Blocks.SPRUCE_LOG, net.minecraft.core.Direction.Axis.Y) : Blocks.SPRUCE_LEAVES.defaultBlockState());
				}
			}
		}
		if (!b.has(W / 2, 1, D - 1) && c != Culture.ANDALUSI && c != Culture.ARAB && c != Culture.PERSIAN) {
			b.set(W - 1, 1, D - 1, Bp.lantern(false));
		}
	}
}
