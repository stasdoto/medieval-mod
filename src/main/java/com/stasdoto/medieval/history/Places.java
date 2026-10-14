package com.stasdoto.medieval.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Settlements of about 1050 at their real places. {@code size}: 1 village, 2 town, 3 city, 4 great city, 5 metropolis.
 * Population figures are rough estimates of historians.
 */
public final class Places {
	private Places() {
	}

	public record Place(String id, String name, double lat, double lon, String realm, Culture culture, int size, int people, String note) {
	}

	public static final List<Place> ALL = new ArrayList<>();

	private static void p(String id, String name, double lat, double lon, String realm, Culture c, int size, int people, String note) {
		ALL.add(new Place(id, name, lat, lon, realm, c, size, people, note));
	}

	static {
		// Rus
		p("kyiv", "Киев", 50.4535, 30.5143, "Киевская Русь", Culture.RUS, 5, 40000, "престол Ярослава Мудрого, Софийский собор, Золотые ворота");
		p("novgorod", "Новгород", 58.5215, 31.2755, "Киевская Русь", Culture.RUS, 4, 15000, "торг, детинец, Святая София (1050)");
		p("chernihiv", "Чернигов", 51.4930, 31.2980, "Киевская Русь", Culture.RUS, 3, 10000, "Спасский собор");
		p("pereiaslav", "Переяславль", 50.0650, 31.4450, "Киевская Русь", Culture.RUS, 3, 6000, "степная граница");
		p("polotsk", "Полоцк", 55.4860, 28.7720, "Полоцкое княжество", Culture.RUS, 3, 6000, "Софийский собор");
		p("smolensk", "Смоленск", 54.7820, 32.0450, "Киевская Русь", Culture.RUS, 3, 6000, "волок на Днепре");
		p("rostov", "Ростов", 57.1840, 39.4150, "Киевская Русь", Culture.RUS, 2, 4000, "озеро Неро");
		p("suzdal", "Суздаль", 56.4190, 40.4430, "Киевская Русь", Culture.RUS, 2, 3000, "Ополье");
		p("murom", "Муром", 55.5770, 42.0510, "Киевская Русь", Culture.RUS, 2, 3000, "Ока");
		p("volodymyr", "Владимир-Волынский", 50.8480, 24.3240, "Киевская Русь", Culture.RUS, 2, 4000, "Волынь");
		p("turov", "Туров", 52.0680, 27.7350, "Киевская Русь", Culture.RUS, 2, 3000, "Полесье, Припять");
		p("pskov", "Псков", 57.8220, 28.3290, "Киевская Русь", Culture.RUS, 2, 4000, "Кром");
		p("ladoga", "Ладога", 60.0000, 32.3000, "Киевская Русь", Culture.RUS, 2, 2000, "путь из варяг в греки");
		p("lyubech", "Любеч", 51.7000, 30.6600, "Киевская Русь", Culture.RUS, 1, 1000, "замок на Днепре");
		p("vyshhorod", "Вышгород", 50.5880, 30.4880, "Киевская Русь", Culture.RUS, 2, 2000, "княжий город под Киевом");
		p("bilhorod", "Белгород Киевский", 50.3930, 30.2400, "Киевская Русь", Culture.RUS, 2, 2000, "крепость на Ирпене");
		p("tmutarakan", "Тмутаракань", 45.2150, 36.7100, "Киевская Русь", Culture.RUS, 2, 5000, "у Керченского пролива");
		p("ryazan", "Рязань", 54.4340, 40.0700, "Киевская Русь", Culture.RUS, 2, 3000, "Старая Рязань на Оке");
		// Byzantium
		p("constantinople", "Константинополь", 41.0086, 28.9802, "Византия", Culture.BYZANTINE, 5, 300000, "Святая София, Феодосиевы стены");
		p("thessaloniki", "Фессалоники", 40.6401, 22.9444, "Византия", Culture.BYZANTINE, 4, 100000, "второй город империи");
		p("athens", "Афины", 37.9715, 23.7267, "Византия", Culture.BYZANTINE, 2, 10000, "");
		p("nicaea", "Никея", 40.4290, 29.7210, "Византия", Culture.BYZANTINE, 3, 20000, "");
		p("trebizond", "Трапезунд", 41.0050, 39.7270, "Византия", Culture.BYZANTINE, 3, 20000, "");
		p("antioch", "Антиохия", 36.2020, 36.1600, "Византия", Culture.BYZANTINE, 4, 50000, "");
		p("chersonesus", "Херсонес", 44.6110, 33.4930, "Византия", Culture.BYZANTINE, 2, 10000, "Корсунь");
		p("preslav", "Преслав", 43.1600, 26.8200, "Византия", Culture.BYZANTINE, 2, 10000, "бывшая столица Болгарии");
		p("bari", "Бари", 41.1280, 16.8700, "Византия", Culture.BYZANTINE, 3, 20000, "");
		// Latin West
		p("rome", "Рим", 41.8986, 12.4768, "Папская область", Culture.LATIN, 4, 35000, "");
		p("venice", "Венеция", 45.4340, 12.3390, "Венецианская республика", Culture.LATIN, 4, 45000, "");
		p("pisa", "Пиза", 43.7230, 10.3960, "Священная Римская империя", Culture.LATIN, 3, 15000, "");
		p("genoa", "Генуя", 44.4070, 8.9340, "Священная Римская империя", Culture.LATIN, 3, 15000, "");
		p("milan", "Милан", 45.4640, 9.1900, "Священная Римская империя", Culture.LATIN, 4, 40000, "");
		p("palermo", "Палермо", 38.1157, 13.3615, "Сицилийский эмират", Culture.ARAB, 5, 150000, "");
		p("paris", "Париж", 48.8530, 2.3490, "Франция", Culture.LATIN, 3, 20000, "Сите");
		p("rouen", "Руан", 49.4400, 1.0930, "Нормандия", Culture.LATIN, 3, 20000, "столица герцогов Нормандии");
		p("london", "Лондон", 51.5130, -0.0900, "Англия", Culture.LATIN, 4, 20000, "");
		p("winchester", "Винчестер", 51.0610, -1.3130, "Англия", Culture.LATIN, 3, 8000, "");
		p("york", "Йорк", 53.9600, -1.0810, "Англия", Culture.NORSE, 3, 9000, "Йорвик");
		p("cologne", "Кёльн", 50.9410, 6.9580, "Священная Римская империя", Culture.LATIN, 4, 20000, "");
		p("mainz", "Майнц", 49.9990, 8.2730, "Священная Римская империя", Culture.LATIN, 3, 15000, "");
		p("regensburg", "Регенсбург", 49.0190, 12.0970, "Священная Римская империя", Culture.LATIN, 3, 15000, "");
		p("aachen", "Ахен", 50.7750, 6.0840, "Священная Римская империя", Culture.LATIN, 2, 5000, "");
		p("magdeburg", "Магдебург", 52.1250, 11.6350, "Священная Римская империя", Culture.LATIN, 3, 8000, "");
		p("goslar", "Гослар", 51.9060, 10.4290, "Священная Римская империя", Culture.LATIN, 2, 5000, "серебряные рудники");
		p("prague", "Прага", 50.0900, 14.4000, "Чехия", Culture.WEST_SLAV, 3, 10000, "Пражский град, Вышеград");
		p("krakow", "Краков", 50.0540, 19.9360, "Польша", Culture.WEST_SLAV, 3, 6000, "Вавель");
		p("gniezno", "Гнезно", 52.5350, 17.5830, "Польша", Culture.WEST_SLAV, 2, 5000, "");
		p("poznan", "Познань", 52.4120, 16.9360, "Польша", Culture.WEST_SLAV, 2, 5000, "Остров Тумский");
		p("esztergom", "Эстергом", 47.7960, 18.7400, "Венгрия", Culture.MAGYAR, 3, 8000, "");
		p("szekesfehervar", "Секешфехервар", 47.1890, 18.4100, "Венгрия", Culture.MAGYAR, 2, 5000, "");
		p("barcelona", "Барселона", 41.3830, 2.1770, "Барселонское графство", Culture.LATIN, 3, 15000, "");
		p("leon", "Леон", 42.5990, -5.5670, "Леон и Кастилия", Culture.LATIN, 2, 5000, "");
		p("santiago", "Сантьяго-де-Компостела", 42.8800, -8.5450, "Леон и Кастилия", Culture.LATIN, 2, 5000, "паломничество");
		p("dublin", "Дублин", 53.3430, -6.2670, "Дублинское королевство", Culture.NORSE, 2, 4000, "");
		// North
		p("roskilde", "Роскилле", 55.6420, 12.0800, "Дания", Culture.NORSE, 2, 4000, "");
		p("hedeby", "Хедебю", 54.4910, 9.5650, "Дания", Culture.NORSE, 2, 2000, "сожжён в 1050 и 1066");
		p("lund", "Лунд", 55.7050, 13.1930, "Дания", Culture.NORSE, 2, 3000, "");
		p("sigtuna", "Сигтуна", 59.6170, 17.7240, "Швеция", Culture.NORSE, 2, 3000, "");
		p("uppsala", "Уппсала", 59.8980, 17.6340, "Швеция", Culture.NORSE, 2, 2000, "Старая Уппсала, курганы");
		p("nidaros", "Нидарос", 63.4270, 10.3960, "Норвегия", Culture.NORSE, 2, 3000, "Тронхейм");
		p("oslo", "Осло", 59.9080, 10.7680, "Норвегия", Culture.NORSE, 1, 1000, "основан Харальдом Суровым");
		// Islamic world
		p("cordoba", "Кордова", 37.8790, -4.7800, "Тайфа Кордова", Culture.ANDALUSI, 5, 100000, "Мескита");
		p("toledo", "Толедо", 39.8570, -4.0230, "Тайфа Толедо", Culture.ANDALUSI, 4, 37000, "");
		p("seville", "Севилья", 37.3890, -5.9850, "Тайфа Севилья", Culture.ANDALUSI, 4, 50000, "");
		p("zaragoza", "Сарагоса", 41.6560, -0.8770, "Тайфа Сарагоса", Culture.ANDALUSI, 3, 25000, "");
		p("granada", "Гранада", 37.1770, -3.5900, "Тайфа Гранада", Culture.ANDALUSI, 3, 26000, "");
		p("cairo", "Каир", 30.0450, 31.2620, "Фатимидский халифат", Culture.ARAB, 5, 200000, "");
		p("alexandria", "Александрия", 31.2000, 29.9180, "Фатимидский халифат", Culture.ARAB, 4, 60000, "");
		p("jerusalem", "Иерусалим", 31.7780, 35.2350, "Фатимидский халифат", Culture.ARAB, 3, 20000, "");
		p("damascus", "Дамаск", 33.5110, 36.3060, "Фатимидский халифат", Culture.ARAB, 4, 50000, "");
		p("aleppo", "Алеппо", 36.1990, 37.1630, "Мирдасиды", Culture.ARAB, 3, 30000, "");
		p("baghdad", "Багдад", 33.3400, 44.4000, "Аббасидский халифат", Culture.ARAB, 5, 300000, "");
		p("kairouan", "Кайруан", 35.6780, 10.1000, "Зириды", Culture.ARAB, 4, 50000, "");
		p("isfahan", "Исфахан", 32.6570, 51.6770, "Сельджуки", Culture.PERSIAN, 4, 50000, "");
		p("nishapur", "Нишапур", 36.2130, 58.7950, "Сельджуки", Culture.PERSIAN, 4, 100000, "");
		p("bukhara", "Бухара", 39.7750, 64.4180, "Караханиды", Culture.PERSIAN, 4, 50000, "");
		p("samarkand", "Самарканд", 39.6550, 66.9760, "Караханиды", Culture.PERSIAN, 4, 50000, "Афрасиаб");
		p("merv", "Мерв", 37.6650, 62.1900, "Сельджуки", Culture.PERSIAN, 5, 200000, "");
		// Caucasus, Volga, steppe
		p("tbilisi", "Тбилиси", 41.6880, 44.8100, "Тбилисский эмират", Culture.CAUCASUS, 3, 20000, "");
		p("kutaisi", "Кутаиси", 42.2700, 42.7000, "Грузинское царство", Culture.CAUCASUS, 2, 10000, "");
		p("ani", "Ани", 40.5070, 43.5730, "Византия", Culture.CAUCASUS, 4, 50000, "город 1001 церкви");
		p("derbent", "Дербент", 42.0580, 48.2900, "Дербентский эмират", Culture.CAUCASUS, 3, 15000, "");
		p("bolghar", "Болгар", 54.9790, 49.0560, "Волжская Булгария", Culture.ARAB, 3, 15000, "");
		p("bilyar", "Биляр", 54.9910, 50.4010, "Волжская Булгария", Culture.ARAB, 4, 30000, "");
		p("sarkel", "Белая Вежа", 47.7000, 42.2300, "Киевская Русь", Culture.STEPPE, 1, 1500, "бывший Саркел на Дону");
		p("cuman_camp", "Половецкое кочевье", 47.5000, 35.0000, "Половцы", Culture.STEPPE, 1, 800, "");
	}

	/** Finds a place by id or (part of) its name, ignoring case. */
	public static Place find(String q) {
		String s = q.toLowerCase(Locale.ROOT).replace('_', ' ');
		for (Place p : ALL) {
			if (p.id.equals(q) || p.name.toLowerCase(Locale.ROOT).equals(s)) {
				return p;
			}
		}
		for (Place p : ALL) {
			if (p.name.toLowerCase(Locale.ROOT).startsWith(s) || p.id.startsWith(s)) {
				return p;
			}
		}
		return null;
	}
}
