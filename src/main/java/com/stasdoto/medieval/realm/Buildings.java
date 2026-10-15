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
			new Order("pottery", BKind.POTTERY, "гончарня", 90, 40, 20, 0, "горшки и кувшины на торг"),
			new Order("bakery", BKind.BAKERY, "пекарня", 80, 40, 25, 0, "хлеб из муки, сытнее люди"),
			new Order("brewery", BKind.BREWERY, "пивоварня (медоварня)", 100, 50, 10, 1, "пиво и мёд, веселее люди"),
			new Order("weaver", BKind.WEAVER, "ткацкая", 90, 45, 0, 0, "сукно и полотно"),
			new Order("carpenter", BKind.CARPENTER, "плотницкий двор", 80, 70, 0, 2, "быстрее стройки"),
			new Order("stable", BKind.STABLE, "конюшня", 110, 80, 10, 1, "кони для дружины и купцов"),
			new Order("warehouse", BKind.WAREHOUSE, "торговый склад", 150, 90, 40, 2, "больше купцов, запасы целее"),
			new Order("bath", BKind.BATHHOUSE, "баня (хаммам)", 90, 50, 30, 1, "меньше болезней"),
			new Order("barracks", BKind.BARRACKS, "дружинная изба (казарма)", 200, 100, 60, 6, "больше воинов в гарнизоне"),
			new Order("tower", BKind.TOWER, "сторожевая башня", 220, 40, 160, 4, "раньше видно набеги"),
			new Order("apiary", BKind.APIARY, "пасека", 40, 20, 0, 0, "мёд и воск"),
			new Order("garden", BKind.GARDEN, "сад", 50, 15, 10, 0, "плоды и красота"),
			new Order("caravanserai", BKind.CARAVANSERAI, "гостиный двор (караван-сарай)", 400, 160, 200, 6, "приходят дальние караваны"),
			new Order("motte", BKind.MOTTE, "замок на холме (мотт)", 700, 300, 100, 15, "крепость: стойко держит набеги"),
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
