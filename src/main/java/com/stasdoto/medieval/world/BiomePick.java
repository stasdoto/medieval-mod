package com.stasdoto.medieval.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import com.stasdoto.medieval.earth.Climate;
import com.stasdoto.medieval.earth.Cover;
import com.stasdoto.medieval.earth.Noise;
import com.stasdoto.medieval.earth.Terrain;

/**
 * Which Minecraft biome a place of the real Earth becomes (the biome decides trees, grass colour, animals and snow).
 * Woods of 1050 are wider than today's: a good part of today's fields and towns is still forest.
 */
public final class BiomePick {
	private BiomePick() {
	}

	@SuppressWarnings("unchecked")
	public static final ResourceKey<Biome>[] ALL = new ResourceKey[]{Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SNOWY_PLAINS,
			Biomes.ICE_SPIKES, Biomes.DESERT, Biomes.SWAMP, Biomes.MANGROVE_SWAMP, Biomes.FOREST, Biomes.FLOWER_FOREST,
			Biomes.BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.OLD_GROWTH_PINE_TAIGA,
			Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.TAIGA, Biomes.SNOWY_TAIGA, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU,
			Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.JUNGLE, Biomes.SPARSE_JUNGLE,
			Biomes.BADLANDS, Biomes.MEADOW, Biomes.GROVE, Biomes.SNOWY_SLOPES, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS,
			Biomes.STONY_PEAKS, Biomes.RIVER, Biomes.FROZEN_RIVER, Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE,
			Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.OCEAN, Biomes.DEEP_OCEAN,
			Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN};

	/** Is a place forest in 1050: today's woods, plus a share of today's fields and towns. */
	public static boolean wooded(Terrain.Col c, int x, int z) {
		if (c.cover == Cover.WOOD) {
			return true;
		}
		if (c.cover == Cover.FARMLAND || c.cover == Cover.MODERN || c.cover == Cover.NONE || c.cover == Cover.SCRUB) {
			if (c.zone == Climate.STEPPE || c.zone == Climate.DESERT || c.zone == Climate.COLD_DESERT || c.zone == Climate.TUNDRA
					|| c.zone == Climate.POLAR) {
				return false;
			}
			// Today's towns, roads and fields all count the same, so that no trace of modern streets shows in the woods.
			double share = c.zone == Climate.BOREAL ? 0.55 : c.zone == Climate.MEDITERRANEAN ? 0.3 : 0.45;
			double n = Noise.fbm(x / 900.0, z / 900.0, 77, 3) * 0.5 + 0.5;
			return n < share;
		}
		return false;
	}

	/** @param slope the steepness (height change per block) around the place */
	public static ResourceKey<Biome> pick(Terrain.Col c, int x, int z, double slope) {
		double t = c.temp;
		if (c.waterTop != Terrain.NO_WATER) {
			if (c.waterKind == Terrain.W_SEA) {
				boolean deep = c.waterTop - c.ground > 30;
				if (t < 0) {
					return deep ? Biomes.DEEP_FROZEN_OCEAN : Biomes.FROZEN_OCEAN;
				}
				if (t < 9) {
					return deep ? Biomes.DEEP_COLD_OCEAN : Biomes.COLD_OCEAN;
				}
				if (t < 16) {
					return deep ? Biomes.DEEP_OCEAN : Biomes.OCEAN;
				}
				if (t < 22) {
					return deep ? Biomes.DEEP_LUKEWARM_OCEAN : Biomes.LUKEWARM_OCEAN;
				}
				return Biomes.WARM_OCEAN;
			}
			return t < -1 ? Biomes.FROZEN_RIVER : Biomes.RIVER;
		}
		double snow = Climate.snowLine(c.lat), tree = Climate.treeLine(c.lat);
		double m = c.meters;
		// Sea shores.
		if (c.shore > -14 && c.shore < 0 && (c.cover == Cover.BEACH || c.cover == Cover.SAND) && c.top() < 70) {
			return t < 0 ? Biomes.SNOWY_BEACH : Biomes.BEACH;
		}
		// Mountains.
		if (m > snow) {
			return slope > 1.4 ? Biomes.JAGGED_PEAKS : Biomes.FROZEN_PEAKS;
		}
		if (m > tree) {
			if (c.cover == Cover.ROCK || c.cover == Cover.SCREE || slope > 1.2) {
				return t < 0 ? Biomes.SNOWY_SLOPES : Biomes.STONY_PEAKS;
			}
			return t < -2 ? Biomes.SNOWY_SLOPES : Biomes.MEADOW;
		}
		if (m > tree - 250 && (c.cover == Cover.WOOD || wooded(c, x, z))) {
			return t < 0 ? Biomes.SNOWY_TAIGA : Biomes.GROVE;
		}
		switch (c.cover) {
			case Cover.ICE -> {
				return Biomes.SNOWY_PLAINS;
			}
			case Cover.ROCK, Cover.SCREE -> {
				return c.zone == Climate.DESERT ? Biomes.BADLANDS : Biomes.WINDSWEPT_GRAVELLY_HILLS;
			}
			case Cover.WETLAND, Cover.MUD, Cover.FLOODPLAIN -> {
				if (c.zone == Climate.TROPICAL) {
					return Biomes.MANGROVE_SWAMP;
				}
				if (c.zone == Climate.TUNDRA || c.zone == Climate.POLAR) {
					return Biomes.SNOWY_PLAINS;
				}
				return c.cover == Cover.FLOODPLAIN && Noise.at(x / 300.0, z / 300.0, 5) > 0.1 ? Biomes.PLAINS : Biomes.SWAMP;
			}
			case Cover.SAND, Cover.BEACH -> {
				return c.zone == Climate.DESERT || c.zone == Climate.COLD_DESERT || c.zone == Climate.STEPPE ? Biomes.DESERT : Biomes.BEACH;
			}
			default -> {
			}
		}
		boolean wood = wooded(c, x, z);
		double v = Noise.at(x / 700.0, z / 700.0, 91);
		return switch (c.zone) {
			case Climate.POLAR -> v > 0.35 ? Biomes.ICE_SPIKES : Biomes.SNOWY_PLAINS;
			case Climate.TUNDRA -> wood ? Biomes.SNOWY_TAIGA : Biomes.SNOWY_PLAINS;
			case Climate.BOREAL -> wood ? (v > 0.25 ? Biomes.OLD_GROWTH_SPRUCE_TAIGA : v < -0.3 ? Biomes.OLD_GROWTH_PINE_TAIGA : Biomes.TAIGA)
					: t < 0 ? Biomes.SNOWY_PLAINS : Biomes.PLAINS;
			case Climate.TEMPERATE -> {
				if (!wood) {
					yield m > 700 ? Biomes.MEADOW : v > 0.45 ? Biomes.SUNFLOWER_PLAINS : Biomes.PLAINS;
				}
				if (m > 700 || t < 5.5) {
					yield v > 0.1 ? Biomes.TAIGA : Biomes.BIRCH_FOREST;
				}
				yield v > 0.3 ? Biomes.DARK_FOREST : v < -0.35 ? Biomes.BIRCH_FOREST : v < -0.2 ? Biomes.OLD_GROWTH_BIRCH_FOREST
						: v > 0.22 ? Biomes.FLOWER_FOREST : Biomes.FOREST;
			}
			case Climate.MEDITERRANEAN -> wood ? (m > 600 ? Biomes.TAIGA : Biomes.FOREST) : slope > 0.8 ? Biomes.WINDSWEPT_HILLS : Biomes.PLAINS;
			case Climate.SUBTROPICAL -> wood ? Biomes.FOREST : Biomes.SAVANNA;
			case Climate.TROPICAL -> wood ? (v > 0 ? Biomes.JUNGLE : Biomes.SPARSE_JUNGLE) : Biomes.SAVANNA;
			case Climate.STEPPE -> wood ? Biomes.FOREST : v > 0.4 ? Biomes.SUNFLOWER_PLAINS : Biomes.PLAINS;
			case Climate.DESERT -> slope > 1.0 ? Biomes.BADLANDS : Biomes.DESERT;
			case Climate.COLD_DESERT -> slope > 1.0 ? Biomes.WINDSWEPT_GRAVELLY_HILLS : Biomes.DESERT;
			default -> Biomes.PLAINS;
		};
	}
}
