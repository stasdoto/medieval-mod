package com.stasdoto.medieval.realm;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.stasdoto.medieval.Medieval;
import com.stasdoto.medieval.settle.Site;

/** The settlements that have been visited, with their state, saved with the world. */
public final class Towns extends SavedData {
	public static final Codec<Towns> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.optionalFieldOf("json", "[]").forGetter(Towns::toJson)).apply(i, Towns::new));
	public static final SavedDataType<Towns> TYPE = new SavedDataType<>(Medieval.id("towns"), Towns::new, CODEC, null);

	public final Map<Long, Town> towns = new HashMap<>();

	public Towns() {
	}

	private Towns(String json) {
		try {
			for (JsonElement e : JsonParser.parseString(json).getAsJsonArray()) {
				Town t = read(e.getAsJsonObject());
				towns.put(t.id, t);
			}
		} catch (RuntimeException ex) {
			Medieval.LOGGER.error("[medieval] cannot read towns", ex);
		}
	}

	public static Towns get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public static long today(MinecraftServer server) {
		return server.overworld().getOverworldClockTime() / 24000L;
	}

	/** The town's state, made when first needed and brought up to today. */
	public Town town(MinecraftServer server, Site s) {
		long today = today(server);
		Town t = towns.get(s.id);
		if (t == null) {
			t = Town.of(s, today);
			towns.put(s.id, t);
		}
		Economy.advance(t, today);
		setDirty();
		return t;
	}

	private String toJson() {
		JsonArray arr = new JsonArray();
		for (Town t : towns.values()) {
			arr.add(write(t));
		}
		return arr.toString();
	}

	private static JsonObject write(Town t) {
		JsonObject o = new JsonObject();
		o.addProperty("id", t.id);
		o.addProperty("name", t.name);
		o.addProperty("x", t.x);
		o.addProperty("z", t.z);
		o.addProperty("kind", t.kind);
		o.addProperty("culture", t.culture);
		o.addProperty("population", t.population);
		o.addProperty("owner", t.owner);
		o.addProperty("ownerName", t.ownerName);
		o.addProperty("tax", t.taxPercent);
		o.addProperty("treasury", t.treasury);
		o.addProperty("mood", t.mood);
		o.addProperty("day", t.day);
		o.addProperty("fertility", t.fertility);
		o.addProperty("forest", t.forest);
		o.addProperty("water", t.water);
		o.addProperty("ore", t.ore);
		o.addProperty("coast", t.coast);
		o.addProperty("north", t.north);
		JsonObject st = new JsonObject();
		t.stock.forEach((k, v) -> st.addProperty(k.name(), v));
		o.add("stock", st);
		JsonObject jb = new JsonObject();
		t.jobs.forEach((k, v) -> jb.addProperty(k.name(), v));
		o.add("jobs", jb);
		JsonArray pr = new JsonArray();
		for (Town.Project p : t.projects) {
			JsonObject po = new JsonObject();
			po.addProperty("kind", p.kind);
			po.addProperty("progress", p.progress);
			po.addProperty("work", p.work);
			po.addProperty("placed", p.placed);
			JsonObject nd = new JsonObject();
			p.needs.forEach((k, v) -> nd.addProperty(k.name(), v));
			po.add("needs", nd);
			pr.add(po);
		}
		o.add("projects", pr);
		JsonArray ch = new JsonArray();
		t.chronicle.forEach(ch::add);
		o.add("chronicle", ch);
		return o;
	}

	private static Town read(JsonObject o) {
		Town t = new Town();
		t.id = o.get("id").getAsLong();
		t.name = o.get("name").getAsString();
		t.x = o.get("x").getAsInt();
		t.z = o.get("z").getAsInt();
		t.kind = o.get("kind").getAsString();
		t.culture = o.get("culture").getAsString();
		t.population = o.get("population").getAsInt();
		t.owner = o.get("owner").getAsString();
		t.ownerName = o.get("ownerName").getAsString();
		t.taxPercent = o.get("tax").getAsInt();
		t.treasury = o.get("treasury").getAsDouble();
		t.mood = o.get("mood").getAsDouble();
		t.day = o.get("day").getAsLong();
		t.fertility = o.get("fertility").getAsDouble();
		t.forest = o.get("forest").getAsDouble();
		t.water = o.get("water").getAsDouble();
		t.ore = o.get("ore").getAsDouble();
		t.coast = o.get("coast").getAsDouble();
		t.north = o.get("north").getAsDouble();
		for (var e : o.getAsJsonObject("stock").entrySet()) {
			try {
				t.stock.put(Res.valueOf(e.getKey()), e.getValue().getAsDouble());
			} catch (IllegalArgumentException ignored) {
			}
		}
		for (var e : o.getAsJsonObject("jobs").entrySet()) {
			try {
				t.jobs.put(Job.valueOf(e.getKey()), e.getValue().getAsInt());
			} catch (IllegalArgumentException ignored) {
			}
		}
		for (JsonElement pe : o.getAsJsonArray("projects")) {
			JsonObject po = pe.getAsJsonObject();
			Town.Project p = new Town.Project();
			p.kind = po.get("kind").getAsString();
			p.progress = po.get("progress").getAsDouble();
			p.work = po.get("work").getAsDouble();
			p.placed = po.get("placed").getAsBoolean();
			for (var e : po.getAsJsonObject("needs").entrySet()) {
				p.needs.put(Res.valueOf(e.getKey()), e.getValue().getAsDouble());
			}
			t.projects.add(p);
		}
		for (JsonElement c : o.getAsJsonArray("chronicle")) {
			t.chronicle.add(c.getAsString());
		}
		return t;
	}
}
