package com.stasdoto.medieval.realm;

import java.util.Random;

/**
 * One day of a settlement's life: work brings goods, people eat, smiths forge, taxes are paid, the mood rises and
 * falls, the people grow or leave, works are built, and now and then something happens (a fire, a good harvest, a
 * plague, wolves, a fair, pilgrims).
 *
 * The year has 4 seasons of {@link #SEASON} days: spring sowing, summer, the autumn harvest, winter.
 */
public final class Economy {
	private Economy() {
	}

	public static final int SEASON = 12;

	public static int season(long day) {
		return (int) ((day / SEASON) % 4);
	}

	public static String seasonName(long day) {
		return new String[]{"весна", "лето", "осень", "зима"}[season(day)];
	}

	/** Lets {@code days} days pass. Returns lines worth telling the lord. */
	public static void advance(Town t, long today) {
		int guard = 0;
		while (t.day < today && guard++ < 400) {
			t.day++;
			day(t);
		}
		if (t.day < today) {
			t.day = today;
		}
	}

	private static void day(Town t) {
		Random r = new Random(t.id * 31 + t.day * 7919);
		int season = season(t.day);
		double crop = switch (season) {
			case 0 -> 0.4;
			case 1 -> 0.9;
			case 2 -> 2.2;
			default -> 0.1;
		};
		int p = t.population;
		// Work.
		double toolsBonus = Math.min(1.25, 0.85 + t.get(Res.TOOLS) / Math.max(1, t.workers()) * 0.8);
		t.add(Res.FOOD, t.jobs.get(Job.FARMERS) * 1.7 * t.fertility * crop * toolsBonus);
		t.add(Res.FOOD, t.jobs.get(Job.FISHERS) * 1.8 * t.water * (season == 3 ? 0.4 : 1.0));
		t.add(Res.FOOD, t.jobs.get(Job.HUNTERS) * 0.9);
		t.add(Res.FOOD, t.jobs.get(Job.HERDERS) * 1.3);
		t.add(Res.HIDES, t.jobs.get(Job.HUNTERS) * 0.3 + t.jobs.get(Job.HERDERS) * 0.15);
		t.add(Res.FURS, t.jobs.get(Job.HUNTERS) * 0.25 * t.north);
		t.add(Res.HONEY, t.jobs.get(Job.HUNTERS) * 0.15 * (season == 1 ? 1.5 : 0.5));
		t.add(Res.HORSES, t.jobs.get(Job.HERDERS) * 0.01);
		t.add(Res.WOOD, t.jobs.get(Job.WOODCUTTERS) * 3.5 * Math.min(1.5, t.forest) * toolsBonus);
		t.add(Res.STONE, t.jobs.get(Job.QUARRYMEN) * 2.5 * toolsBonus);
		t.add(Res.IRON, t.jobs.get(Job.MINERS) * 0.7 * t.ore * toolsBonus);
		t.add(Res.SALT, t.coast * t.jobs.get(Job.FISHERS) * 0.3);
		// Weavers make cloth from flax and wool.
		t.add(Res.CLOTH, t.jobs.get(Job.WEAVERS) * 0.8);
		// Smiths: tools first when short, then weapons, then armour.
		for (int i = 0; i < t.jobs.get(Job.SMITHS); i++) {
			if (t.get(Res.IRON) < 1 || t.get(Res.WOOD) < 1) {
				break;
			}
			t.add(Res.WOOD, -1);
			Res out;
			if (t.get(Res.TOOLS) < t.workers() * 0.3) {
				t.add(Res.IRON, -0.5);
				out = Res.TOOLS;
			} else if (t.get(Res.WEAPONS) < t.jobs.get(Job.WARRIORS) * 2 + 10 || r.nextBoolean()) {
				t.add(Res.IRON, -0.8);
				out = Res.WEAPONS;
			} else {
				if (t.get(Res.IRON) < 2) {
					break;
				}
				t.add(Res.IRON, -2);
				out = Res.ARMOR;
				t.add(out, 0.25);
				continue;
			}
			t.add(out, 0.5);
		}
		// Tools wear out.
		t.add(Res.TOOLS, -t.workers() * 0.002);
		// Eating: everybody needs food; in winter wood for the stoves too.
		double eat = p * 0.5;
		double food = t.get(Res.FOOD);
		boolean hungry = food < eat;
		t.add(Res.FOOD, -eat);
		t.add(Res.FOOD, -t.get(Res.FOOD) * 0.002); // spoilage
		// Firewood, fences and repairs; in winter much more for the stoves.
		t.add(Res.WOOD, -p * (season == 3 ? 0.12 : 0.04));
		t.add(Res.CLOTH, -p * 0.003);
		// Taxes to the lord's treasury, the merchants' trade.
		double income = p * 0.08 * t.taxPercent / 10.0 + t.jobs.get(Job.MERCHANTS) * 0.6;
		t.treasury += income;
		// Mood.
		double target = 70 - t.taxPercent * 1.2;
		if (hungry) {
			target -= 35;
		}
		if (t.get(Res.FOOD) > p * 20) {
			target += 8;
		}
		if (t.jobs.get(Job.WARRIORS) < p * 0.02) {
			target -= 6; // people feel unsafe
		}
		if (season == 3 && t.get(Res.WOOD) < p * 0.5) {
			target -= 12;
		}
		t.mood += (target - t.mood) * 0.08;
		t.mood = Math.max(0, Math.min(100, t.mood));
		// People: births, deaths, newcomers and leavers.
		if (hungry) {
			int lost = Math.max(1, p / 60);
			t.population = Math.max(5, p - lost);
			if (r.nextInt(3) == 0) {
				t.log("голод: " + lost + " человек умерли или ушли");
			}
		} else if (t.mood > 55 && t.get(Res.FOOD) > p * 6 && r.nextDouble() < 0.25) {
			int born = Math.max(1, (int) (p * 0.004 + (t.mood - 55) * 0.02));
			t.population += born;
		} else if (t.mood < 30 && r.nextDouble() < 0.3) {
			int left = Math.max(1, p / 80);
			t.population = Math.max(5, p - left);
			t.log("недовольные ушли в другие края: " + left);
		}
		// New hands go to the fields; people lost leave the fields first.
		int idle = t.idle();
		if (idle > 0) {
			t.jobs.put(Job.FARMERS, t.jobs.get(Job.FARMERS) + idle);
		}
		int over = -(t.workers() - sumJobs(t));
		if (over > 0) {
			int f = Math.min(over, t.jobs.get(Job.FARMERS));
			t.jobs.put(Job.FARMERS, t.jobs.get(Job.FARMERS) - f);
		}
		// Building works.
		int builders = t.jobs.get(Job.BUILDERS);
		for (Town.Project pr : t.projects) {
			if (pr.progress >= pr.work || builders <= 0) {
				continue;
			}
			// Materials are taken as the work goes.
			double share = Math.min(1.0, builders * 1.0 / Math.max(1, pr.work - pr.progress));
			boolean enough = true;
			for (var e : pr.needs.entrySet()) {
				double need = e.getValue() * builders / pr.work;
				if (t.get(e.getKey()) < need) {
					enough = false;
				}
			}
			if (!enough) {
				if (r.nextInt(6) == 0) {
					t.log("стройка «" + Buildings.title(pr.kind) + "» стоит: не хватает материалов");
				}
				continue;
			}
			for (var e : pr.needs.entrySet()) {
				t.add(e.getKey(), -e.getValue() * builders / pr.work);
			}
			pr.progress = Math.min(pr.work, pr.progress + builders * share + builders * (1 - share));
			if (pr.progress >= pr.work) {
				t.log("достроено: " + Buildings.title(pr.kind));
			}
			break;
		}
		// Events.
		double ev = r.nextDouble();
		if (ev < 0.004) {
			double burnt = t.get(Res.WOOD) * 0.3;
			t.add(Res.WOOD, -burnt);
			t.add(Res.FOOD, -t.get(Res.FOOD) * 0.1);
			t.mood -= 8;
			t.log("пожар! сгорели амбары и часть леса на дворах");
		} else if (ev < 0.006) {
			int dead = Math.max(1, p / 15);
			t.population = Math.max(5, p - dead);
			t.mood -= 15;
			t.log("мор: умерло " + dead + " человек");
		} else if (ev < 0.012 && season == 2) {
			t.add(Res.FOOD, p * 4.0);
			t.mood += 5;
			t.log("богатый урожай");
		} else if (ev < 0.016 && season == 3) {
			t.add(Res.HIDES, -t.get(Res.HIDES) * 0.2);
			t.add(Res.HORSES, -t.get(Res.HORSES) * 0.05);
			t.log("волки зарезали скот");
		} else if (ev < 0.020) {
			t.treasury += 20 + p * 0.3;
			t.mood += 3;
			t.log("ярмарка: съехались купцы, казна пополнилась");
		} else if (ev < 0.023 && t.population > 40) {
			int come = 3 + r.nextInt(8);
			t.population += come;
			t.log("пришли новые поселенцы: " + come + " человек");
		}
		t.mood = Math.max(0, Math.min(100, t.mood));
	}

	static int sumJobs(Town t) {
		int s = 0;
		for (Job j : Job.values()) {
			s += t.jobs.get(j);
		}
		return s;
	}

	/** How much of a good a settlement wants to keep. */
	public static double want(Town t, Res r) {
		return switch (r) {
			case FOOD -> t.population * 15.0;
			case WOOD -> t.population * 2.0;
			case WEAPONS, ARMOR -> t.jobs.get(Job.WARRIORS) * 2.0 + 5;
			case HORSES -> t.population * 0.05 + 2;
			default -> t.population * 0.3 + 5;
		};
	}

	/**
	 * A day of trade between two settlements: their merchants carry what one has too much of to the other, which lacks
	 * it, and are paid at the buyer's price. Returns a line for the chronicle, or null.
	 */
	public static String trade(Town a, Town b) {
		int merchants = a.jobs.get(Job.MERCHANTS) + b.jobs.get(Job.MERCHANTS) + 2;
		String note = null;
		for (Res r : Res.values()) {
			for (int dir = 0; dir < 2; dir++) {
				Town from = dir == 0 ? a : b, to = dir == 0 ? b : a;
				double extra = from.get(r) - want(from, r) * 1.3;
				double lack = want(to, r) * 0.7 - to.get(r);
				if (extra <= 0 || lack <= 0) {
					continue;
				}
				double amount = Math.min(Math.min(extra, lack), merchants * (r == Res.FOOD || r == Res.WOOD ? 20 : 3));
				double pay = amount * price(to, r);
				if (to.treasury < pay * 0.5) {
					continue;
				}
				from.add(r, -amount);
				to.add(r, amount);
				to.treasury -= pay * 0.5;
				from.treasury += pay * 0.5;
				if (note == null && amount >= 5) {
					note = "купцы из " + from.name + " привезли в " + to.name + ": " + r.title + " (" + Math.round(amount) + ")";
				}
			}
		}
		return note;
	}

	/** The local price of a good (in silver), dearer when scarce. */
	public static double price(Town t, Res r) {
		double want = switch (r) {
			case FOOD -> t.population * 15.0;
			case WOOD -> t.population * 2.0;
			case WEAPONS, ARMOR -> t.jobs.get(Job.WARRIORS) * 2.0 + 5;
			case HORSES -> t.population * 0.05 + 2;
			default -> t.population * 0.3 + 5;
		};
		double k = Math.sqrt((want + 1) / (t.get(r) + 1));
		return r.price * Math.max(0.4, Math.min(3.0, k));
	}
}
