package com.stasdoto.medieval.settle;

import com.stasdoto.medieval.earth.Climate;
import com.stasdoto.medieval.earth.Regions;
import com.stasdoto.medieval.history.Culture;
import com.stasdoto.medieval.history.Places;

/** Which people live at a place around 1050 (and so how they build). */
public final class CultureMap {
	private CultureMap() {
	}

	public static Culture at(double lat, double lon, int zone) {
		// A few broad strokes that the nearest-town rule gets wrong.
		if (lat > 51.3 && lat < 55.5 && lon > -10.7 && lon < -5.4) {
			return Culture.CELTIC; // Ireland
		}
		if (lat > 56 && lon > -7.5 && lon < -2 ) {
			return Culture.CELTIC; // Scottish highlands
		}
		if (lat > 59.5 && lon > 21 && lon < 35 && !(lat < 61 && lon > 29.5 && lon < 33.5)) {
			return Culture.BALT_FINN; // Finland and Karelia (Ladoga is Rus)
		}
		if (lat > 53.5 && lat < 58 && lon > 20 && lon < 27.5 && !(lat > 55.2 && lon > 26.5)) {
			return Culture.BALT_FINN; // Prussians, Lithuanians, Latvians, Estonians
		}
		if (lat > 57.5 && lat < 59.5 && lon > 21.5 && lon < 28) {
			return Culture.BALT_FINN; // Estonia
		}
		if (zone == Climate.STEPPE && Regions.in(lat, lon, Regions.STEPPE) && lon > 28 && lon < 90) {
			return Culture.STEPPE;
		}
		Places.Place best = null;
		double bd = Double.MAX_VALUE;
		double k = Math.cos(Math.toRadians(lat));
		for (Places.Place p : Places.ALL) {
			double dx = (p.lon() - lon) * k, dy = p.lat() - lat;
			// Bigger towns pull their culture further.
			double d = Math.sqrt(dx * dx + dy * dy) / (0.7 + 0.15 * p.size());
			if (d < bd) {
				bd = d;
				best = p;
			}
		}
		return best == null ? Culture.LATIN : best.culture();
	}
}
