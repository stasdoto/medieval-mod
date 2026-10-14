package com.stasdoto.medieval.world;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import com.stasdoto.medieval.earth.Dem;
import com.stasdoto.medieval.earth.Projection;
import com.stasdoto.medieval.earth.Terrain;

/** Biomes of the real Earth: each place gets the biome that matches its climate, height and what grows there. */
public class EarthBiomeSource extends BiomeSource {
	public static final MapCodec<EarthBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegistryOps.<Biome, EarthBiomeSource>retrieveGetter(Registries.BIOME)).apply(i, i.stable(EarthBiomeSource::new)));

	private final HolderGetter<Biome> biomes;
	private final Map<ResourceKey<Biome>, Holder<Biome>> holders = new IdentityHashMap<>();

	public EarthBiomeSource(HolderGetter<Biome> biomes) {
		this.biomes = biomes;
		for (ResourceKey<Biome> k : BiomePick.ALL) {
			holders.put(k, biomes.getOrThrow(k));
		}
	}

	@Override
	protected MapCodec<? extends BiomeSource> codec() {
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		return Arrays.stream(BiomePick.ALL).map(holders::get);
	}

	public Holder<Biome> holder(ResourceKey<Biome> key) {
		return holders.get(key);
	}

	/** The biome of a column (block coordinates). */
	public Holder<Biome> at(int x, int z) {
		Terrain.Col c = Terrain.column(x, z);
		double slope = 0;
		if (c.waterTop == Terrain.NO_WATER) {
			double a = Projection.y(Dem.meters(Projection.lat(z + 0.5), Projection.lon(x + 4.5)));
			double b = Projection.y(Dem.meters(Projection.lat(z + 4.5), Projection.lon(x + 0.5)));
			double y0 = Projection.y(c.meters);
			slope = Math.max(Math.abs(a - y0), Math.abs(b - y0)) / 4.0;
		}
		return holders.get(BiomePick.pick(c, x, z, slope));
	}

	@Override
	public BiomeResolver createResolver(Climate.Sampler sampler) {
		// Biomes do not change with height: remember the 16 columns of the chunk being filled.
		long[] keys = new long[64];
		Arrays.fill(keys, Long.MIN_VALUE);
		@SuppressWarnings("unchecked")
		Holder<Biome>[] vals = new Holder[64];
		return (qx, qy, qz) -> {
			long k = ((long) qx << 32) ^ (qz & 0xFFFFFFFFL);
			int slot = (int) ((qx * 31 + qz) & 63);
			synchronized (keys) {
				if (keys[slot] == k) {
					return vals[slot];
				}
			}
			Holder<Biome> h = at(qx * 4 + 2, qz * 4 + 2);
			synchronized (keys) {
				keys[slot] = k;
				vals[slot] = h;
			}
			return h;
		};
	}
}
