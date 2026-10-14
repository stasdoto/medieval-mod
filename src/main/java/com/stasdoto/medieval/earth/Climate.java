package com.stasdoto.medieval.earth;

/**
 * A rough climate of the 11th century (close to today's, a little warmer - the Medieval Warm Period): the mean yearly
 * temperature from latitude, the Atlantic and Mediterranean warmth and the height, and the climate zone.
 */
public final class Climate {
	private Climate() {
	}

	public static final int POLAR = 0, TUNDRA = 1, BOREAL = 2, TEMPERATE = 3, MEDITERRANEAN = 4, SUBTROPICAL = 5, TROPICAL = 6,
			STEPPE = 7, DESERT = 8, COLD_DESERT = 9;

	private static final double[] LAT = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90};
	private static final double[] TEMP = {26, 26, 24, 19, 13, 7.5, 2, -6, -16, -25};

	/** Mean yearly temperature (°C) at sea level. */
	public static double seaTemp(double lat, double lon) {
		double a = Math.abs(lat);
		int i = Math.min(LAT.length - 2, (int) (a / 10));
		double t = TEMP[i] + (TEMP[i + 1] - TEMP[i]) * (a - LAT[i]) / 10.0;
		// The Gulf Stream warms north-western Europe.
		if (lat > 40 && lat < 72 && lon > -25 && lon < 30) {
			double west = smooth(0, 30, lon) * smooth(46, 40, lat) * smooth(66, 72, lat);
			t += 4.5 * west;
		}
		// The Mediterranean sea keeps its shores mild.
		if (lat > 29 && lat < 46 && lon > -10 && lon < 42) {
			t += 2.0 * smooth(42, 46, lat) * smooth(-6, -10, lon) * smooth(36, 42, lon);
		}
		// Deserts are hot.
		if (Regions.in(lat, lon, Regions.DESERT) && a < 40) {
			t += 3;
		}
		// Medieval Warm Period.
		t += 0.4;
		return t;
	}

	/** Mean yearly temperature at a height. */
	public static double temp(double lat, double lon, double meters) {
		return seaTemp(lat, lon) - Math.max(0, meters) * 0.0065;
	}

	public static int zone(double lat, double lon, double temp) {
		int r = Regions.kind(lat, lon);
		if (r == Regions.DESERT) {
			return temp > 12 ? DESERT : COLD_DESERT;
		}
		if (r == Regions.ARID) {
			return temp > 12 ? DESERT : COLD_DESERT;
		}
		if (temp < -11) {
			return POLAR;
		}
		if (r == Regions.TUNDRA || temp < -3.5) {
			return TUNDRA;
		}
		if (r == Regions.STEPPE) {
			return STEPPE;
		}
		if (temp < 3) {
			return BOREAL;
		}
		if (lat > 29 && lat < 46 && lon > -10 && lon < 42 && temp >= 13) {
			return MEDITERRANEAN;
		}
		if (temp < 13.5) {
			return TEMPERATE;
		}
		if (temp < 21 || Math.abs(lat) > 24) {
			return SUBTROPICAL;
		}
		return TROPICAL;
	}

	/** Height (m) above which snow lies all year. */
	public static double snowLine(double lat) {
		double a = Math.abs(lat);
		return a < 25 ? 4800 : Math.max(300, 4800 - 85 * (a - 25));
	}

	/** Height (m) above which no trees grow. */
	public static double treeLine(double lat) {
		return Math.max(150, snowLine(lat) - 900);
	}

	/** 1 at a (and beyond it), 0 at b (and beyond it), smooth in between. */
	static double smooth(double a, double b, double x) {
		double t = (x - a) / (b - a);
		t = Math.max(0, Math.min(1, t));
		t = t * t * (3 - 2 * t);
		return 1 - t;
	}
}
