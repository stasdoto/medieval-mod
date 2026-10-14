package com.stasdoto.medieval;

import java.nio.file.Path;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stasdoto.medieval.command.MedievalCommands;
import com.stasdoto.medieval.earth.Dem;
import com.stasdoto.medieval.earth.EarthLog;
import com.stasdoto.medieval.earth.OsmTile;
import com.stasdoto.medieval.earth.Regions;
import com.stasdoto.medieval.earth.TileStore;
import com.stasdoto.medieval.world.EarthBiomeSource;
import com.stasdoto.medieval.world.EarthChunkGenerator;

public class Medieval implements ModInitializer {
	public static final String MOD_ID = "medieval";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final String TERRARIUM = "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png";

	@Override
	public void onInitialize() {
		EarthLog.info = s -> LOGGER.info("[medieval] {}", s);
		EarthLog.warn = s -> LOGGER.warn("[medieval] {}", s);
		Path cache = FabricLoader.getInstance().getGameDir().resolve("medieval-cache");
		Dem.init(new TileStore("terrarium", cache, "png", TERRARIUM));
		OsmTile.init(new TileStore("osm", cache, "pbf", null));
		Thread t = new Thread(() -> {
			Regions.load();
			OsmTile.resolveUrl(cache.resolve("osm-url.txt"));
		}, "medieval-map-setup");
		t.setDaemon(true);
		t.start();

		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("earth"), EarthChunkGenerator.CODEC);
		Registry.register(BuiltInRegistries.BIOME_SOURCE, id("earth"), EarthBiomeSource.CODEC);
		MedievalCommands.init();
		LOGGER.info("Stasdoto Medieval loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
