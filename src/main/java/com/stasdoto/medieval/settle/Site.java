package com.stasdoto.medieval.settle;

import com.stasdoto.medieval.history.Culture;

/** A settlement of the 11th century: where it is, how big, who lives there. */
public final class Site {
	public enum Kind {
		HAMLET("хутор", 4, 8, 70),
		VILLAGE("село", 10, 22, 130),
		BIG_VILLAGE("большое село", 22, 40, 190),
		CAMP("кочевье", 8, 20, 110),
		CASTLE("замок", 6, 12, 120),
		MONASTERY("монастырь", 6, 12, 110),
		TOWN("городок", 40, 90, 330),
		CITY("город", 90, 260, 650),
		GREAT_CITY("большой город", 260, 700, 1000),
		METROPOLIS("столица-мегаполис", 700, 2200, 1700);

		public final String title;
		public final int minHouses, maxHouses;
		/** Radius of the built-up area in blocks. */
		public final int radius;

		Kind(String title, int minHouses, int maxHouses, int radius) {
			this.title = title;
			this.minHouses = minHouses;
			this.maxHouses = maxHouses;
			this.radius = radius;
		}

		public boolean fortified() {
			return ordinal() >= TOWN.ordinal() || this == CASTLE;
		}

		public boolean urban() {
			return ordinal() >= TOWN.ordinal();
		}
	}

	public final long id;
	public final int x, z;
	public final Kind kind;
	public final Culture culture;
	public final String name;
	/** The real town of 1050 this is, or null for a village of the countryside. */
	public final com.stasdoto.medieval.history.Places.Place place;
	public final int radius;
	public final int houses;

	public Site(long id, int x, int z, Kind kind, Culture culture, String name, com.stasdoto.medieval.history.Places.Place place, int radius,
			int houses) {
		this.id = id;
		this.x = x;
		this.z = z;
		this.kind = kind;
		this.culture = culture;
		this.name = name;
		this.place = place;
		this.radius = radius;
		this.houses = houses;
	}

	/** How far the fields reach around. */
	public int fieldRadius() {
		return switch (kind) {
			case HAMLET -> radius + 90;
			case VILLAGE -> radius + 170;
			case BIG_VILLAGE -> radius + 230;
			case CAMP -> radius;
			case CASTLE, MONASTERY -> radius + 120;
			case TOWN -> radius + 260;
			case CITY -> radius + 350;
			case GREAT_CITY, METROPOLIS -> radius + 450;
		};
	}

	public boolean christian() {
		return switch (culture) {
			case RUS, BYZANTINE, LATIN, NORSE, WEST_SLAV, MAGYAR, CAUCASUS, CELTIC -> true;
			default -> false;
		};
	}

	public boolean muslim() {
		return culture == com.stasdoto.medieval.history.Culture.ANDALUSI || culture == com.stasdoto.medieval.history.Culture.ARAB
				|| culture == com.stasdoto.medieval.history.Culture.PERSIAN;
	}

	@Override
	public String toString() {
		return kind.title + " " + name + " (" + culture.title + ") at " + x + " " + z;
	}
}
