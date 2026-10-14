package com.stasdoto.medieval.earth;

/** What covers the ground at a place (from OpenStreetMap). */
public final class Cover {
	private Cover() {
	}

	public static final byte NONE = 0, WOOD = 1, GRASS = 2, FARMLAND = 3, WETLAND = 4, SAND = 5, ROCK = 6, ICE = 7, SCRUB = 8,
			BEACH = 9, SCREE = 10, MUD = 11, MODERN = 12, FLOODPLAIN = 13, HEATH = 14, MEADOW = 15;

	public static String name(int c) {
		return switch (c) {
			case WOOD -> "лес";
			case GRASS -> "луг";
			case FARMLAND -> "поля";
			case WETLAND -> "болото";
			case SAND -> "пески";
			case ROCK -> "скалы";
			case ICE -> "ледник";
			case SCRUB -> "кустарник";
			case BEACH -> "пляж";
			case SCREE -> "осыпь";
			case MUD -> "ил";
			case MODERN -> "место нынешнего города";
			case FLOODPLAIN -> "пойма";
			case HEATH -> "пустошь";
			case MEADOW -> "луг";
			default -> "-";
		};
	}
}
