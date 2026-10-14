package com.stasdoto.medieval.settle;

/**
 * Kinds of buildings and yards. Sizes are the plot in its own orientation: {@code w} along the street, {@code d} away
 * from it (the front faces the street).
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
	CELLS(9, 19);

	public final int w, d;

	BKind(int w, int d) {
		this.w = w;
		this.d = d;
	}

	public boolean big() {
		return this == CATHEDRAL || this == MOSQUE || this == HALL || this == CHURCH || this == KEEP;
	}
}
