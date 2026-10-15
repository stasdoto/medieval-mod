package com.stasdoto.medieval.settle;

/**
 * Kinds of buildings and yards. Sizes are the default plot in its own orientation: {@code w} along the street, {@code d}
 * away from it (the front faces the street). Many plots get other sizes from the planner (see {@link Layout.Lot#w}).
 */
public enum BKind {
	HOMESTEAD(17, 23),
	HOUSE(11, 13),
	HOUSE_RICH(15, 17),
	SMITHY(11, 11),
	TAVERN(13, 15),
	GRANARY(9, 9),
	CHURCH_SMALL(11, 17),
	CHURCH(17, 25),
	CATHEDRAL(31, 37),
	MOSQUE(23, 27),
	HALL(21, 27),
	KEEP(15, 15),
	STALL(5, 5),
	WELL(5, 5),
	YURT(9, 9),
	CELLS(9, 19),
	// ---- 0.3: crafts and trades
	/** A shed, store or small workshop in a back yard. */
	SHED(7, 7),
	POTTERY(11, 13),
	TANNERY(13, 13),
	BAKERY(9, 11),
	BREWERY(11, 13),
	WEAVER(9, 11),
	CARPENTER(11, 13),
	BLOOMERY(11, 11),
	KILN(9, 9),
	CHARCOAL(13, 13),
	APIARY(9, 9),
	STABLE(11, 15),
	WAREHOUSE(13, 17),
	BATHHOUSE(9, 9),
	CARAVANSERAI(25, 25),
	BARRACKS(13, 17),
	TOWER(7, 7),
	SHRINE(3, 3),
	CEMETERY(13, 13),
	GARDEN(13, 13),
	// ---- by the water: the front (+z) faces the water, floor y=0 is one block above the water surface
	WATERMILL(11, 13),
	NORIA(9, 9),
	WINDMILL(9, 9),
	FISHERY(9, 11),
	PIER(7, 15),
	BOATYARD(13, 19),
	// ---- castles
	MOTTE(29, 29),
	LONGHOUSE(11, 31);

	public final int w, d;

	BKind(int w, int d) {
		this.w = w;
		this.d = d;
	}

	public boolean big() {
		return switch (this) {
			case CATHEDRAL, MOSQUE, HALL, CHURCH, KEEP, CARAVANSERAI, MOTTE, LONGHOUSE, WAREHOUSE, BARRACKS -> true;
			default -> false;
		};
	}

	/** Buildings that stand on the shore with their front over the water. */
	public boolean shore() {
		return switch (this) {
			case WATERMILL, NORIA, FISHERY, PIER, BOATYARD -> true;
			default -> false;
		};
	}

	/** Homes of families (they count towards a settlement's size). */
	public boolean home() {
		return this == HOMESTEAD || this == HOUSE || this == HOUSE_RICH || this == YURT || this == LONGHOUSE;
	}

	/** Workshops and trades: their people work at home. */
	public boolean craft() {
		return switch (this) {
			case SMITHY, POTTERY, TANNERY, BAKERY, BREWERY, WEAVER, CARPENTER, BLOOMERY, KILN, CHARCOAL, APIARY, WATERMILL, NORIA, WINDMILL,
					FISHERY, BOATYARD -> true;
			default -> false;
		};
	}
}
