package com.stasdoto.medieval.world;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import com.stasdoto.medieval.earth.Climate;
import com.stasdoto.medieval.earth.Cover;
import com.stasdoto.medieval.earth.Noise;
import com.stasdoto.medieval.earth.Projection;
import com.stasdoto.medieval.earth.Terrain;

/**
 * The world generator of the real Earth at 1:1: real heights, seas, rivers, lakes, forests and fields (see
 * {@link Terrain}), turned into blocks. No vanilla villages or other modern-looking structures - the settlements of the
 * 11th century are built by the mod itself.
 */
public class EarthChunkGenerator extends ChunkGenerator {
	public static final MapCodec<EarthChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)).apply(i, i.stable(EarthChunkGenerator::new)));

	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static final BlockState STONE = Blocks.STONE.defaultBlockState();
	private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
	private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
	private static final BlockState WATER = Blocks.WATER.defaultBlockState();
	private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
	private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
	private static final BlockState COARSE = Blocks.COARSE_DIRT.defaultBlockState();
	private static final BlockState PODZOL = Blocks.PODZOL.defaultBlockState();
	private static final BlockState MUD = Blocks.MUD.defaultBlockState();
	private static final BlockState SAND = Blocks.SAND.defaultBlockState();
	private static final BlockState RED_SAND = Blocks.RED_SAND.defaultBlockState();
	private static final BlockState SANDSTONE = Blocks.SANDSTONE.defaultBlockState();
	private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
	private static final BlockState CLAY = Blocks.CLAY.defaultBlockState();
	private static final BlockState SNOW = Blocks.SNOW_BLOCK.defaultBlockState();
	private static final BlockState PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState();
	private static final BlockState ANDESITE = Blocks.ANDESITE.defaultBlockState();
	private static final BlockState TUFF = Blocks.TUFF.defaultBlockState();
	private static final BlockState CALCITE = Blocks.CALCITE.defaultBlockState();
	private static final BlockState TERRACOTTA = Blocks.TERRACOTTA.defaultBlockState();
	private static final BlockState ORANGE_TERRACOTTA = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
	private static final BlockState MOSS = Blocks.MOSS_BLOCK.defaultBlockState();
	private static final BlockState ROOTED = Blocks.ROOTED_DIRT.defaultBlockState();

	public EarthChunkGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> sets, RandomState randomState, long seed) {
		return ChunkGeneratorStructureState.createForFlat(randomState, seed, getOrigin(randomState), biomeSource, Stream.empty());
	}

	@Override
	public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState, StructureManager structures,
			BiomeManager biomeManager, WorldGenRegion region, Set<Holder<Biome>> biomes) {
		int x0 = chunk.getPos().getMinBlockX();
		int z0 = chunk.getPos().getMinBlockZ();
		// Columns of the chunk plus one around, for the steepness.
		Terrain.Col[] cols = new Terrain.Col[18 * 18];
		for (int j = 0; j < 18; j++) {
			for (int i = 0; i < 18; i++) {
				cols[j * 18 + i] = Terrain.column(x0 + i - 1, z0 + j - 1);
			}
		}
		Heightmap floor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		int minY = chunk.getMinY();
		int maxY = minY + chunk.getHeight() - 1;
		BlockState[] column = new BlockState[16];
		for (int lz = 0; lz < 16; lz++) {
			for (int lx = 0; lx < 16; lx++) {
				Terrain.Col c = cols[(lz + 1) * 18 + lx + 1];
				int x = x0 + lx, z = z0 + lz;
				double slope = 0;
				for (int d = 0; d < 4; d++) {
					Terrain.Col n = cols[(lz + 1 + (d == 2 ? 1 : d == 3 ? -1 : 0)) * 18 + lx + 1 + (d == 0 ? 1 : d == 1 ? -1 : 0)];
					slope = Math.max(slope, Math.abs(n.ground - c.ground));
				}
				int top = Math.min(maxY - 2, c.top());
				int layers = surface(c, x, z, slope, column);
				int deep = -16 + (int) (Noise.at(x / 23.0, z / 23.0, 3) * 4);
				int bedrockTop = minY + (int) (Noise.rand(x, z, 9) * 4);
				for (int y = minY; y <= top; y++) {
					BlockState s;
					int fromTop = top - y;
					if (y <= bedrockTop) {
						s = BEDROCK;
					} else if (fromTop < layers) {
						s = column[fromTop];
					} else {
						s = y < deep ? DEEPSLATE : STONE;
					}
					set(chunk, lx, y, lz, s);
				}
				floor.update(lx, top, lz, column[0]);
				int wTop = c.waterTop == Terrain.NO_WATER ? top : Math.min(maxY - 2, c.waterTop);
				if (c.waterTop != Terrain.NO_WATER) {
					for (int y = top + 1; y <= wTop; y++) {
						set(chunk, lx, y, lz, WATER);
					}
					surface.update(lx, wTop, lz, WATER);
				} else {
					surface.update(lx, top, lz, column[0]);
					// Puddles in marshes.
					if ((c.cover == Cover.WETLAND || c.cover == Cover.MUD) && slope < 0.6 && Noise.at(x / 7.0, z / 7.0, 21) > 0.25) {
						set(chunk, lx, top, lz, WATER);
					}
				}
			}
		}
		return CompletableFuture.completedFuture(chunk);
	}

	private static void set(ChunkAccess chunk, int lx, int y, int lz, BlockState s) {
		LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
		section.setBlockState(lx, y & 15, lz, s, false);
	}

	/**
	 * The top layers of a column, from the surface down, into {@code out}; returns how many there are (the rest is stone).
	 */
	static int surface(Terrain.Col c, int x, int z, double slope, BlockState[] out) {
		double r = Noise.at(x / 5.0, z / 5.0, 31);
		double r2 = Noise.at(x / 17.0, z / 17.0, 32);
		int depth = 3 + (int) (Noise.rand(x, z, 33) * 2);
		BlockState top;
		BlockState under = DIRT;
		if (c.waterTop != Terrain.NO_WATER) {
			double d = c.waterTop + 1 - c.ground;
			if (c.waterKind == Terrain.W_SEA) {
				top = d < 6 ? SAND : r > 0.3 ? GRAVEL : r < -0.4 ? CLAY : SAND;
				under = SAND;
			} else if (c.waterKind == Terrain.W_RIVER) {
				top = r > 0.15 ? GRAVEL : r < -0.35 ? CLAY : SAND;
				under = r2 > 0 ? GRAVEL : SAND;
			} else {
				top = r > 0.35 ? CLAY : r < -0.3 ? SAND : MUD;
				under = r2 > 0 ? CLAY : DIRT;
			}
			out[0] = top;
			for (int i = 1; i < depth; i++) {
				out[i] = under;
			}
			return depth;
		}
		double snow = Climate.snowLine(c.lat);
		boolean desert = c.zone == Climate.DESERT || c.zone == Climate.COLD_DESERT;
		if (c.meters > snow + r * 60 && slope < 2.2) {
			out[0] = SNOW;
			out[1] = SNOW;
			out[2] = c.cover == Cover.ICE ? PACKED_ICE : STONE;
			return 3;
		}
		if (c.cover == Cover.ICE) {
			for (int i = 0; i < 6; i++) {
				out[i] = i == 0 ? SNOW : PACKED_ICE;
			}
			return 6;
		}
		if (slope >= 1.7 || c.cover == Cover.ROCK && slope > 0.6) {
			BlockState rock = desert ? (r > 0 ? TERRACOTTA : ORANGE_TERRACOTTA) : r > 0.45 ? ANDESITE : r < -0.5 ? TUFF : r2 > 0.6 ? CALCITE : STONE;
			out[0] = rock;
			return 1;
		}
		if (c.cover == Cover.SCREE || c.cover == Cover.ROCK) {
			out[0] = r > 0 ? GRAVEL : STONE;
			out[1] = GRAVEL;
			return 2;
		}
		if (c.cover == Cover.SAND || c.cover == Cover.BEACH || desert) {
			BlockState sand = desert && r2 > 0.55 ? RED_SAND : SAND;
			for (int i = 0; i < depth + 2; i++) {
				out[i] = i < depth ? sand : SANDSTONE;
			}
			return depth + 2;
		}
		if (c.cover == Cover.WETLAND || c.cover == Cover.MUD) {
			out[0] = r > 0.1 ? MUD : r < -0.4 ? MOSS : GRASS;
			for (int i = 1; i < depth; i++) {
				out[i] = i < 2 ? MUD : CLAY;
			}
			return depth;
		}
		if (slope >= 1.1) {
			out[0] = r > 0.2 ? STONE : r < -0.3 ? GRAVEL : COARSE;
			out[1] = DIRT;
			return 2;
		}
		top = GRASS;
		boolean wood = BiomePick.wooded(c, x, z);
		if (c.zone == Climate.BOREAL && wood && r > 0.0) {
			top = r > 0.4 ? PODZOL : COARSE;
		} else if (wood && r < -0.55) {
			top = ROOTED;
		} else if ((c.zone == Climate.STEPPE || c.zone == Climate.MEDITERRANEAN) && r > 0.45) {
			top = COARSE;
		} else if (slope > 0.8 && r2 > 0.4) {
			top = COARSE;
		}
		out[0] = top;
		for (int i = 1; i < depth; i++) {
			out[i] = DIRT;
		}
		return depth;
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
	}

	@Override
	public int getGenDepth() {
		return Projection.HEIGHT;
	}

	@Override
	public int getSeaLevel() {
		return Projection.SEA_LEVEL;
	}

	@Override
	public int getMinY() {
		return Projection.MIN_Y;
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
		Terrain.Col c = Terrain.column(x, z);
		int top = c.top();
		if (c.waterTop != Terrain.NO_WATER && type != Heightmap.Types.OCEAN_FLOOR && type != Heightmap.Types.OCEAN_FLOOR_WG) {
			top = Math.max(top, c.waterTop);
		}
		return top + 1;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
		Terrain.Col c = Terrain.column(x, z);
		int minY = level.getMinY();
		BlockState[] states = new BlockState[level.getHeight()];
		int top = c.top();
		int wTop = c.waterTop == Terrain.NO_WATER ? top : c.waterTop;
		for (int i = 0; i < states.length; i++) {
			int y = minY + i;
			states[i] = y <= top ? STONE : y <= wTop ? WATER : AIR;
		}
		return new NoiseColumn(minY, states);
	}

	@Override
	public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos, SamplerContext context) {
		Terrain.Col c = Terrain.column(pos.getX(), pos.getZ());
		info.add(String.format("Earth: %.5f, %.5f  %.0f m  %s  %.1f °C", c.lat, c.lon, c.meters, Cover.name(c.cover), c.temp));
	}
}
