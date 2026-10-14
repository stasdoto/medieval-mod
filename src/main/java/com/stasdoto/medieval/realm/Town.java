package com.stasdoto.medieval.realm;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.stasdoto.medieval.earth.Climate;
import com.stasdoto.medieval.earth.Cover;
import com.stasdoto.medieval.earth.Terrain;
import com.stasdoto.medieval.history.Culture;
import com.stasdoto.medieval.settle.Site;

/**
 * The living state of one settlement: its people and what they do, its stores, its lord, taxes, mood, garrison and
 * building works. Simulated day by day (also for the days nobody was near).
 */
public final class Town {
	public long id;
	public String name;
	public int x, z;
	public String kind;
	public String culture;
	public int population;
	public final EnumMap<Res, Double> stock = new EnumMap<>(Res.class);
	public final EnumMap<Job, Integer> jobs = new EnumMap<>(Job.class);
	/** The lord: a player's name and id, or empty for a lord of the game's own (a prince, a count, an emir). */
	public String owner = "";
	public String ownerName = "";
	public int taxPercent = 10;
	public double treasury;
	public double mood = 60;
	public long day;
	/** Land around: how fertile the fields, how much forest, water, ore, sea salt. */
	public double fertility = 1, forest = 1, water = 0, ore = 0, coast = 0, north = 0;
	public final List<Project> projects = new ArrayList<>();
	public final List<String> chronicle = new ArrayList<>();
	/** Lots the lord added (they are built into the world when finished): kind name, x, z, rot. */
	public final List<int[]> added = new ArrayList<>();
	public final List<String> addedKinds = new ArrayList<>();

	public static final class Project {
		public String kind;
		public double progress;
		public double work;
		public EnumMap<Res, Double> needs = new EnumMap<>(Res.class);
		public boolean placed;
	}

	public Town() {
		for (Res r : Res.values()) {
			stock.put(r, 0.0);
		}
		for (Job j : Job.values()) {
			jobs.put(j, 0);
		}
	}

	/** A settlement as it is when first visited: people, work and stores fitting its size, people and land. */
	public static Town of(Site s, long today) {
		Town t = new Town();
		t.id = s.id;
		t.name = s.name;
		t.x = s.x;
		t.z = s.z;
		t.kind = s.kind.name();
		t.culture = s.culture.name();
		t.day = today;
		Random r = new Random(s.id);
		t.population = s.place != null ? Math.max(60, s.place.people() / 4) : s.houses * (5 + r.nextInt(3));
		// The land around.
		int fertile = 0, wood = 0, wet = 0, n = 0;
		double maxH = -1e9, minH = 1e9;
		for (int i = -3; i <= 3; i++) {
			for (int j = -3; j <= 3; j++) {
				Terrain.Col c = Terrain.column(s.x + i * s.fieldRadius() / 3, s.z + j * s.fieldRadius() / 3);
				n++;
				if (c.waterTop != Terrain.NO_WATER) {
					wet++;
					if (c.waterKind == Terrain.W_SEA) {
						t.coast = 1;
					}
					continue;
				}
				if (c.cover == Cover.WOOD) {
					wood++;
				}
				if (c.cover == Cover.FARMLAND || c.cover == Cover.GRASS || c.cover == Cover.MEADOW || c.cover == Cover.NONE || c.cover == Cover.MODERN) {
					fertile++;
				}
				maxH = Math.max(maxH, c.meters);
				minH = Math.min(minH, c.meters);
				t.north = Math.max(t.north, c.lat > 56 ? 1 : 0);
				if (c.zone == Climate.DESERT || c.zone == Climate.COLD_DESERT) {
					fertile--;
				}
			}
		}
		t.fertility = Math.max(0.2, 0.5 + fertile / (double) n);
		t.forest = 0.4 + wood / (double) n * 1.6 + 0.3;
		t.water = wet > 0 ? 1 : 0;
		t.ore = maxH - minH > 250 ? 1 : wet > 3 ? 0.4 : 0.15; // mountains, or bog iron by marshes and rivers
		// Work.
		boolean urban = s.kind.urban();
		int p = t.population;
		int working = (int) (p * 0.55);
		t.jobs.put(Job.WARRIORS, Math.max(urban ? 10 : 2, (int) (p * (urban ? 0.06 : 0.03))));
		t.jobs.put(Job.SMITHS, Math.max(1, (int) (working * (urban ? 0.06 : 0.03))));
		t.jobs.put(Job.WEAVERS, (int) (working * (urban ? 0.08 : 0.04)));
		t.jobs.put(Job.MERCHANTS, urban ? (int) (working * 0.07) : 0);
		t.jobs.put(Job.FISHERS, t.water > 0 ? (int) (working * 0.08) : 0);
		t.jobs.put(Job.HUNTERS, (int) (working * (t.north > 0 ? 0.08 : 0.04)));
		t.jobs.put(Job.WOODCUTTERS, (int) (working * 0.08));
		t.jobs.put(Job.QUARRYMEN, urban ? (int) (working * 0.03) : 0);
		t.jobs.put(Job.MINERS, t.ore > 0.3 ? (int) (working * 0.04) : (int) (working * 0.01));
		boolean nomads = s.culture == Culture.STEPPE;
		t.jobs.put(Job.HERDERS, nomads ? (int) (working * 0.7) : (int) (working * 0.06));
		int used = 0;
		for (Job j : Job.values()) {
			used += t.jobs.get(j);
		}
		t.jobs.put(Job.FARMERS, Math.max(0, working - used));
		// Stores for a few weeks.
		t.stock.put(Res.FOOD, p * 1.0 * 30);
		t.stock.put(Res.WOOD, p * 2.0);
		t.stock.put(Res.STONE, urban ? p * 0.5 : 10.0);
		t.stock.put(Res.IRON, urban ? p * 0.05 : 3.0);
		t.stock.put(Res.TOOLS, p * 0.1);
		t.stock.put(Res.WEAPONS, t.jobs.get(Job.WARRIORS) * 1.5);
		t.stock.put(Res.ARMOR, t.jobs.get(Job.WARRIORS) * 0.5);
		t.stock.put(Res.CLOTH, p * 0.2);
		t.stock.put(Res.HIDES, p * 0.1);
		t.stock.put(Res.FURS, t.north > 0 ? p * 0.1 : 0.0);
		t.stock.put(Res.SALT, t.coast > 0 ? p * 0.3 : p * 0.05);
		t.stock.put(Res.HONEY, p * 0.05);
		t.stock.put(Res.HORSES, nomads ? p * 0.6 : urban ? p * 0.02 : p * 0.01);
		t.treasury = urban ? p * 2.0 : p * 0.3;
		return t;
	}

	public double get(Res r) {
		return stock.getOrDefault(r, 0.0);
	}

	public void add(Res r, double v) {
		stock.put(r, Math.max(0, get(r) + v));
	}

	public int idle() {
		int used = 0;
		for (Job j : Job.values()) {
			used += jobs.get(j);
		}
		return Math.max(0, (int) (population * 0.55) - used);
	}

	public int workers() {
		return (int) (population * 0.55);
	}

	public void log(String line) {
		chronicle.add(0, "День " + day + ": " + line);
		while (chronicle.size() > 30) {
			chronicle.remove(chronicle.size() - 1);
		}
	}
}
