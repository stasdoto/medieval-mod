package com.stasdoto.medieval.settle;

import java.util.Random;

import com.stasdoto.medieval.history.Culture;

/** Names for the villages that history did not record, in the manner of each people (written in Russian). */
public final class NameGen {
	private NameGen() {
	}

	private static String pick(Random r, String... a) {
		return a[r.nextInt(a.length)];
	}

	public static String name(Culture c, long seed) {
		Random r = new Random(seed * 0x9E3779B97F4A7C15L + 17);
		return switch (c) {
			case RUS, BALT_FINN -> {
				int k = r.nextInt(5);
				if (k == 0) {
					yield pick(r, "Берез", "Дубр", "Липов", "Ольш", "Сосн", "Ольх", "Клен", "Рябин", "Вишн", "Ивн", "Кален")
							+ pick(r, "ово", "ица", "ки", "ня", "ец", "ное", "ичи");
				}
				if (k == 1) {
					yield pick(r, "Вол", "Добр", "Мир", "Свят", "Яр", "Рад", "Влад", "Люб", "Глеб", "Бор", "Стан", "Мил", "Жир", "Дан")
							+ pick(r, "ичи", "овичи", "ино", "ово", "ятичи", "ешичи", "ков");
				}
				if (k == 2) {
					yield pick(r, "Красн", "Бел", "Черн", "Зелен", "Глубок", "Высок", "Тих", "Быстр", "Студен", "Горел")
							+ pick(r, "ое", "ый Брод", "ая Гора", "ый Яр", "ая Поляна", "ый Луг", "ое Озеро");
				}
				if (k == 3) {
					yield pick(r, "Под", "За", "При", "Пере") + pick(r, "горье", "речье", "лесье", "болотье", "дубье", "лужье", "бережье", "яславль");
				}
				yield pick(r, "Городище", "Селище", "Погост", "Займище", "Перевоз", "Мостище", "Слобода", "Хутор", "Выселки", "Бортничи",
						"Рыбаки", "Гончары", "Ковали", "Овчары", "Бондари");
			}
			case LATIN, CELTIC -> pick(r, "Saint-", "Mont", "Ville", "Bel", "Fon", "Roche", "Beau", "Neu", "Alt", "Ober", "Nieder", "Wald",
					"Cler", "Mar", "Brom", "Ash", "Oak", "Thorn", "Wych")
					+ pick(r, "court", "ville", "mont", "bach", "dorf", "heim", "berg", "feld", "ford", "ham", "ton", "wick", "ley", "bury",
					"field", "-sur-l'Eau", "aine", "lac", "bourg");
			case NORSE -> pick(r, "Ulf", "Ask", "Bjørn", "Hal", "Tor", "Sig", "Ran", "Grim", "Hrafn", "Eir", "Frey", "Sten", "Vík", "Hav", "Lund")
					+ pick(r, "by", "stad", "vik", "heim", "sund", "nes", "dal", "fjord", "torp", "holm", "berg");
			case WEST_SLAV -> pick(r, "Bole", "Dobro", "Rado", "Swieto", "Jaro", "Mile", "Kazi", "Wito", "Prze", "Gniew", "Lubo")
					+ pick(r, "sław", "mierz", "wice", "chów", "ów", "ice", "no", "sko", "wo");
			case MAGYAR -> pick(r, "Szent", "Nagy", "Kis", "Fehér", "Fekete", "Vár", "Hegy", "Tó", "Bükk", "Diós", "Almás", "Kőrös")
					+ pick(r, "falva", "háza", "egyháza", "telek", "vár", "szeg", "laka", "kút");
			case BYZANTINE -> pick(r, "Agios ", "Neo", "Kalo", "Paleo", "Mega", "Chryso", "Lefko", "Kastro", "Pyrgo", "Ano ", "Kato ")
					+ pick(r, "polis", "chori", "kastron", "nikos", "mylos", "limani", "pyrgos", "vouni", "kampos", "ktima");
			case ANDALUSI, ARAB -> pick(r, "Al-", "Bab ", "Qal'at ", "Bir ", "Ain ", "Wadi ", "Dar ", "Qasr ", "Ras ", "Tell ", "Kafr ")
					+ pick(r, "Hamra", "Abyad", "Salam", "Nur", "Zahra", "Kabir", "Jadid", "Rumi", "Sharq", "Gharb", "Ma'", "Nakhl", "Zaytun");
			case PERSIAN -> pick(r, "Deh", "Abad", "Mazar", "Khan", "Sar", "Gol", "Shah", "Kuh", "Rud", "Bagh")
					+ pick(r, "-e Now", "abad", "kand", "istan", "-e Bala", "shahr", "gerd", "an", "dasht");
			case CAUCASUS -> pick(r, "Mta", "Tsikhe", "Kvemo ", "Zemo ", "Akhal", "Dzveli ", "Sam", "Tskal", "Gori", "Ust")
					+ pick(r, "-kari", "subani", "kalaki", "tsikhe", "eti", "uri", "ani", "-dzhvari");
			case STEPPE -> pick(r, "Кочевье ", "Вежи ", "Становище ") + pick(r, "Шарукана", "Боняка", "Тугоркана", "Кончака", "Аепы", "Осеня",
					"Белкатгина", "Итларя", "Кытана", "Сугра", "Урусобы", "Алтунопы");
		};
	}
}
