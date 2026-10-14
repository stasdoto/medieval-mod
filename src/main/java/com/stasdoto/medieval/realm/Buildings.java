package com.stasdoto.medieval.realm;

import java.util.EnumMap;

import com.stasdoto.medieval.settle.BKind;

/** What a lord can order to be built, and what it takes. */
public final class Buildings {
	private Buildings() {
	}

	public record Order(String key, BKind kind, String title, int work, int wood, int stone, int iron, String effect) {
	}

	public static final Order[] ALL = {
			new Order("homestead", BKind.HOMESTEAD, "двор с избой", 60, 60, 0, 1, "+6 жителей могут поселиться"),
			new Order("house", BKind.HOUSE, "городской дом", 80, 50, 20, 1, "+5 жителей"),
			new Order("smithy", BKind.SMITHY, "кузница", 120, 40, 40, 6, "больше оружия, орудий и доспехов"),
			new Order("granary", BKind.GRANARY, "амбар", 60, 50, 5, 0, "меньше портится зерно"),
			new Order("tavern", BKind.TAVERN, "корчма", 140, 80, 30, 2, "веселее люди, больше купцов"),
			new Order("church", BKind.CHURCH_SMALL, "церковь (или мечеть)", 300, 120, 120, 4, "лучше настроение"),
			new Order("hall", BKind.HALL, "хоромы (палаты)", 500, 250, 150, 10, "здесь ваш двор, больше налогов"),
			new Order("keep", BKind.KEEP, "башня-донжон", 600, 120, 400, 20, "крепче оборона"),
			new Order("well", BKind.WELL, "колодец", 30, 5, 20, 0, "люди здоровее"),
	};

	public static Order find(String key) {
		for (Order o : ALL) {
			if (o.key.equals(key)) {
				return o;
			}
		}
		return null;
	}

	public static String title(String key) {
		Order o = find(key);
		return o == null ? key : o.title;
	}

	public static Town.Project project(Order o) {
		Town.Project p = new Town.Project();
		p.kind = o.key;
		p.work = o.work;
		p.needs = new EnumMap<>(Res.class);
		p.needs.put(Res.WOOD, (double) o.wood);
		p.needs.put(Res.STONE, (double) o.stone);
		p.needs.put(Res.IRON, (double) o.iron);
		return p;
	}
}
