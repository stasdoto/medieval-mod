package com.stasdoto.medieval.construct;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;

import com.stasdoto.medieval.Medieval;
import com.stasdoto.medieval.earth.Climate;
import com.stasdoto.medieval.earth.Cover;
import com.stasdoto.medieval.earth.Noise;
import com.stasdoto.medieval.earth.Terrain;
import com.stasdoto.medieval.settle.Layout;
import com.stasdoto.medieval.settle.Planner;
import com.stasdoto.medieval.settle.Site;
import com.stasdoto.medieval.settle.Sites;
import com.stasdoto.medieval.world.EarthChunkGenerator;

/**
 * Builds the settlements of the Earth world while it is generated, one chunk at a time: fields, streets, squares,
 * plots with their buildings, and walls. Placed last in every chunk (after trees), and only in the Earth world.
 */
public record SettlementFeature() implements Feature {
	public static final MapCodec<SettlementFeature> CODEC = MapCodec.unit(SettlementFeature::new);
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ON_PLACE;
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	public static void init() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Medieval.id("settlements"), CODEC);
		Planner.wonders = Landmarks::blocked;
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
				ResourceKey.create(Registries.PLACED_FEATURE, Medieval.id("settlements")));
	}

	@Override
	public MapCodec<SettlementFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		if (!(generator instanceof EarthChunkGenerator)) {
			return false;
		}
		ChunkPos cp = ChunkPos.containing(origin);
		try {
			new Writer(level, cp).run();
		} catch (RuntimeException e) {
			Medieval.LOGGER.error("[medieval] settlement generation failed at {}", cp, e);
		}
		return true;
	}

	/** Builds one plot into a live world (a building the lord ordered): ground, building and its people. */
	public static void buildNow(WorldGenLevel level, Layout l, Layout.Lot lot) {
		Writer w = new Writer(level, lot.x0 - 4, lot.z0 - 4, lot.x1 + 4, lot.z1 + 4);
		w.flags = Block.UPDATE_ALL;
		Bp bp = Styles.build(lot, l.site);
		lot.built = bp;
		w.pad(lot, bp);
		w.place(lot, bp);
		w.people(l, lot);
	}

	private static final class Writer {
		final WorldGenLevel level;
		final int x0, z0, x1, z1;
		final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		final Terrain.Col[] cols;
		int flags = FLAGS;

		Writer(WorldGenLevel level, ChunkPos cp) {
			this(level, cp.getMinBlockX(), cp.getMinBlockZ(), cp.getMinBlockX() + 15, cp.getMinBlockZ() + 15);
		}

		Writer(WorldGenLevel level, int x0, int z0, int x1, int z1) {
			this.level = level;
			this.x0 = x0;
			this.z0 = z0;
			this.x1 = x1;
			this.z1 = z1;
			this.cols = new Terrain.Col[(x1 - x0 + 1) * (z1 - z0 + 1)];
		}

		Terrain.Col col(int x, int z) {
			if (!in(x, z)) {
				return Terrain.column(x, z);
			}
			int i = (z - z0) * (x1 - x0 + 1) + (x - x0);
			Terrain.Col c = cols[i];
			if (c == null) {
				c = Terrain.column(x, z);
				cols[i] = c;
			}
			return c;
		}

		void set(int x, int y, int z, BlockState s) {
			level.setBlock(pos.set(x, y, z), s, flags);
		}

		BlockState get(int x, int y, int z) {
			return level.getBlockState(pos.set(x, y, z));
		}

		boolean in(int x, int z) {
			return x >= x0 && x <= x1 && z >= z0 && z <= z1;
		}

		/** Removes plants, trees and snow above a surface (not buildings). */
		void clearAbove(int x, int top, int z, int height) {
			for (int y = top + 1; y <= top + height; y++) {
				BlockState s = get(x, y, z);
				if (s.isAir()) {
					if (y > top + 3) {
						break;
					}
					continue;
				}
				if (s.canBeReplaced() || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS)) {
					set(x, y, z, AIR);
				} else {
					break;
				}
			}
		}

		void run() {
			int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
			// Country tracks between the settlements.
			for (com.stasdoto.medieval.settle.Links.Link link : com.stasdoto.medieval.settle.Links.near(x0, z0, x1, z1)) {
				roads(java.util.List.of(link.road()));
			}
			List<Site> sites = Sites.near(cx, cz);
			for (Site s : sites) {
				Layout l = Planner.layout(s);
				double d = Math.hypot(cx - s.x, cz - s.z);
				if (d > l.fieldOuter + 24 && (x1 < l.minX - 16 || x0 > l.maxX + 16 || z1 < l.minZ - 16 || z0 > l.maxZ + 16)) {
					boolean wallNear = false;
					for (Layout.Wall w : l.walls) {
						if (Math.hypot(cx - w.cx, cz - w.cz) < w.maxRadius() + 40) {
							wallNear = true;
						}
					}
					if (!wallNear) {
						continue;
					}
				}
				fields(l);
				roads(l);
				squares(l);
				lots(l);
				decor(l);
				walls(l);
			}
			// The wonders of the old world stand over everything else.
			Landmarks.write(this::set, x0, z0, x1, z1);
		}

		// ---- fields ----

		void fields(Layout l) {
			Site s = l.site;
			if (l.fieldOuter <= l.fieldInner) {
				return;
			}
			double ang = (s.id & 0xFFFF) / 65536.0 * Math.PI;
			double ca = Math.cos(ang), sa = Math.sin(ang);
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					double d = Math.hypot(x - s.x, z - s.z);
					double edge = l.fieldOuter * (0.85 + 0.25 * Noise.at(x / 120.0, z / 120.0, (int) s.id));
					if (d < l.fieldInner || d > edge) {
						continue;
					}
					Terrain.Col c = col(x, z);
					if (c.waterTop != Terrain.NO_WATER || c.cover == Cover.WOOD || c.cover == Cover.WETLAND || c.cover == Cover.ROCK
							|| c.cover == Cover.SCREE || c.cover == Cover.ICE || c.cover == Cover.SAND || c.cover == Cover.BEACH) {
						continue;
					}
					if ((c.zone == Climate.DESERT || c.zone == Climate.COLD_DESERT) && c.shore < -200) {
						continue;
					}
					int top = c.top();
					BlockState ground = get(x, top, z);
					if (!ground.is(Blocks.GRASS_BLOCK) && !ground.is(Blocks.DIRT) && !ground.is(Blocks.COARSE_DIRT) && !ground.is(Blocks.PODZOL)
							&& !ground.is(Blocks.ROOTED_DIRT)) {
						continue;
					}
					// Steep ground stays meadow.
					Terrain.Col n = col(Math.min(x1, x + 1), z), e = col(x, Math.min(z1, z + 1));
					if (Math.abs(n.ground - c.ground) > 0.9 || Math.abs(e.ground - c.ground) > 0.9) {
						continue;
					}
					double u = x * ca + z * sa, v = -x * sa + z * ca;
					int strip = (int) Math.floor(u / 9.0);
					double inStrip = u - strip * 9.0;
					int seg = (int) Math.floor(v / 64.0);
					if (inStrip < 1.0) {
						continue; // a grassy balk between the strips
					}
					int kind = (int) (Math.abs((strip * 73856093L) ^ (seg * 19349663L) ^ s.id) % 12);
					BlockState above = get(x, top + 1, z);
					if (!above.isAir() && !above.canBeReplaced()) {
						continue;
					}
					switch (kind) {
						case 0, 1, 2, 3, 4 -> { // rye and wheat
							set(x, top, z, Blocks.FARMLAND.defaultBlockState());
							set(x, top + 1, z, Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, kind < 4 ? 7 : 3 + (x + z) % 3));
						}
						case 5 -> { // barley and oats, younger
							set(x, top, z, Blocks.FARMLAND.defaultBlockState());
							set(x, top + 1, z, Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 5));
						}
						case 6 -> { // turnips and beets
							set(x, top, z, Blocks.FARMLAND.defaultBlockState());
							set(x, top + 1, z, Blocks.BEETROOTS.defaultBlockState().setValue(BlockStateProperties.AGE_3, 3));
						}
						case 7 -> { // ploughed fallow
							set(x, top, z, (x + z) % 3 == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.FARMLAND.defaultBlockState());
							if (!above.isAir()) {
								set(x, top + 1, z, AIR);
							}
						}
						case 8 -> { // a hay meadow with ricks
							if (Noise.rand(x, z, 41) < 0.006) {
								set(x, top + 1, z, Blocks.HAY_BLOCK.defaultBlockState());
							}
						}
						default -> { // pasture and fallow with flowers: leave the grass
						}
					}
				}
			}
		}

		// ---- streets ----

		void roads(Layout l) {
			roads(l.roads);
		}

		void roads(java.util.List<Layout.Road> list) {
			for (Layout.Road r : list) {
				if (r.maxX < x0 || r.minX > x1 || r.maxZ < z0 || r.minZ > z1) {
					continue;
				}
				double half = r.width / 2.0;
				for (int x = x0; x <= x1; x++) {
					for (int z = z0; z <= z1; z++) {
						// Nearest segment and its direction.
						double best = Double.MAX_VALUE;
						double dirX = 1, dirZ = 0;
						int[] p = r.pts;
						for (int i = 0; i + 3 < p.length; i += 2) {
							double dd = segDist(x + 0.5, z + 0.5, p[i], p[i + 1], p[i + 2], p[i + 3]);
							if (dd < best) {
								best = dd;
								dirX = p[i + 2] - p[i];
								dirZ = p[i + 3] - p[i + 1];
							}
						}
						if (best > half) {
							continue;
						}
						Terrain.Col c = col(x, z);
						if (c.waterTop != Terrain.NO_WATER) {
							// A plank bridge over a brook or the edge of the river.
							if (c.waterTop - c.top() <= 3) {
								set(x, c.waterTop + 1, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
							}
							continue;
						}
						int top = c.top();
						BlockState s = switch (r.surface) {
							case DIRT -> best > half - 0.7 && Noise.rand(x, z, 5) < 0.4 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState();
							case GRAVEL -> Noise.rand(x, z, 5) < 0.25 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
							case LOGS -> Bp.log(Blocks.STRIPPED_SPRUCE_LOG, Math.abs(dirX) > Math.abs(dirZ) ? Direction.Axis.Z : Direction.Axis.X);
							case COBBLE -> Noise.rand(x, z, 5) < 0.3 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
							case SAND -> Noise.rand(x, z, 5) < 0.3 ? Blocks.SANDSTONE.defaultBlockState() : Blocks.SMOOTH_SANDSTONE.defaultBlockState();
							case PLANKS -> Blocks.SPRUCE_PLANKS.defaultBlockState();
						};
						BlockState cur = get(x, top, z);
						if (cur.isAir() || cur.is(Blocks.WATER)) {
							continue;
						}
						set(x, top, z, s);
						if (r.surface == Layout.Surface.LOGS || r.surface == Layout.Surface.GRAVEL) {
							set(x, top - 1, z, Blocks.DIRT.defaultBlockState());
						}
						clearAbove(x, top, z, 12);
					}
				}
			}
		}

		static double segDist(double px, double pz, double ax, double az, double bx, double bz) {
			double dx = bx - ax, dz = bz - az;
			double l2 = dx * dx + dz * dz;
			double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / l2));
			double qx = ax + t * dx - px, qz = az + t * dz - pz;
			return Math.sqrt(qx * qx + qz * qz);
		}

		// ---- squares ----

		void squares(Layout l) {
			for (Layout.Square q : l.squares) {
				if (q.x1 < x0 || q.x0 > x1 || q.z1 < z0 || q.z0 > z1) {
					continue;
				}
				int y = Terrain.column((q.x0 + q.x1) / 2, (q.z0 + q.z1) / 2).top();
				for (int x = Math.max(x0, q.x0); x <= Math.min(x1, q.x1); x++) {
					for (int z = Math.max(z0, q.z0); z <= Math.min(z1, q.z1); z++) {
						Terrain.Col c = col(x, z);
						if (c.waterTop != Terrain.NO_WATER) {
							continue;
						}
						int t = c.top();
						for (int yy = y + 1; yy <= Math.max(t, y) + 10; yy++) {
							set(x, yy, z, AIR);
						}
						for (int yy = t; yy < y; yy++) {
							set(x, yy, z, Blocks.DIRT.defaultBlockState());
						}
						BlockState pave = switch (l.site.culture) {
							case RUS, BALT_FINN -> Blocks.SPRUCE_PLANKS.defaultBlockState();
							case ANDALUSI, ARAB, PERSIAN -> Blocks.SMOOTH_SANDSTONE.defaultBlockState();
							case BYZANTINE, CAUCASUS -> (x + z) % 2 == 0 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
							default -> Noise.rand(x, z, 7) < 0.3 ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
						};
						set(x, y, z, pave);
					}
				}
			}
		}

		// ---- plots and buildings ----

		void lots(Layout l) {
			for (Layout.Lot lot : l.lots) {
				if (!lot.touches(x0, z0, x1, z1, 4)) {
					continue;
				}
				Bp bp = (Bp) lot.built;
				if (bp == null) {
					bp = Styles.build(lot, l.site);
					lot.built = bp;
				}
				pad(lot, bp);
				place(lot, bp);
				people(l, lot);
			}
		}

		/** The people of a building appear in front of it, in the chunk that holds that spot. */
		void people(Layout l, Layout.Lot lot) {
			int W = lot.w, D = lot.d;
			int lx = W / 2, lz = D;
			int wx, wz;
			switch (lot.rot) {
				case 1 -> {
					wx = D - 1 - lz;
					wz = lx;
				}
				case 2 -> {
					wx = W - 1 - lx;
					wz = D - 1 - lz;
				}
				case 3 -> {
					wx = lz;
					wz = W - 1 - lx;
				}
				default -> {
					wx = lx;
					wz = lz;
				}
			}
			int x = lot.x0 + wx, z = lot.z0 + wz;
			if (!in(x, z)) {
				return;
			}
			Terrain.Col c = col(x, z);
			int y = Math.max(c.top(), lot.floor) + 1;
			com.stasdoto.medieval.people.People.populate(level, l.site, lot.kind, new BlockPos(x, y, z), lot.seed, l);
		}

		/** Levels the ground of a plot to its floor and blends the edges into the land around. */
		void pad(Layout.Lot lot, Bp bp) {
			int m = 3;
			for (int x = Math.max(x0, lot.x0 - m); x <= Math.min(x1, lot.x1 + m); x++) {
				for (int z = Math.max(z0, lot.z0 - m); z <= Math.min(z1, lot.z1 + m); z++) {
					Terrain.Col c = col(x, z);
					if (c.waterTop != Terrain.NO_WATER) {
						continue;
					}
					int t = c.top();
					int dx = Math.max(0, Math.max(lot.x0 - x, x - lot.x1)), dz = Math.max(0, Math.max(lot.z0 - z, z - lot.z1));
					int out = Math.max(dx, dz);
					int target = out == 0 ? lot.floor : (int) Math.round(lot.floor + (t - lot.floor) * out / (m + 1.0));
					if (out == 0) {
						for (int y = target + 1; y <= Math.max(t, target) + bp.maxY + 4; y++) {
							set(x, y, z, AIR);
						}
					} else if (t > target) {
						for (int y = target + 1; y <= t; y++) {
							set(x, y, z, AIR);
						}
						clearAbove(x, t, z, 14);
					}
					for (int y = t + 1; y < target; y++) {
						set(x, y, z, Blocks.DIRT.defaultBlockState());
					}
					if (out > 0 || t != target) {
						set(x, target, z, Blocks.GRASS_BLOCK.defaultBlockState());
					}
				}
			}
		}

		void place(Layout.Lot lot, Bp bp) {
			Rotation rot = switch (lot.rot) {
				case 1 -> Rotation.CLOCKWISE_90;
				case 2 -> Rotation.CLOCKWISE_180;
				case 3 -> Rotation.COUNTERCLOCKWISE_90;
				default -> Rotation.NONE;
			};
			int W = bp.w, D = bp.d;
			for (Map.Entry<Long, BlockState> e : bp.blocks.entrySet()) {
				long k = e.getKey();
				int lx = Bp.kx(k), ly = Bp.ky(k), lz = Bp.kz(k);
				int wx, wz;
				switch (lot.rot) {
					case 1 -> {
						wx = D - 1 - lz;
						wz = lx;
					}
					case 2 -> {
						wx = W - 1 - lx;
						wz = D - 1 - lz;
					}
					case 3 -> {
						wx = lz;
						wz = W - 1 - lx;
					}
					default -> {
						wx = lx;
						wz = lz;
					}
				}
				int x = lot.x0 + wx, z = lot.z0 + wz;
				if (!in(x, z)) {
					continue;
				}
				BlockState s = e.getValue();
				if (rot != Rotation.NONE) {
					s = s.rotate(rot);
				}
				set(x, lot.floor + ly, z, s);
			}
		}

		// ---- life between the houses: trees, bushes, flowers, haystacks, barrels, carts, woodpiles ----

		void decor(Layout l) {
			Site s = l.site;
			double reach = l.fieldInner + 25;
			boolean south = s.culture == com.stasdoto.medieval.history.Culture.ARAB || s.culture == com.stasdoto.medieval.history.Culture.ANDALUSI
					|| s.culture == com.stasdoto.medieval.history.Culture.PERSIAN || s.culture == com.stasdoto.medieval.history.Culture.BYZANTINE;
			boolean north = s.culture == com.stasdoto.medieval.history.Culture.RUS || s.culture == com.stasdoto.medieval.history.Culture.BALT_FINN
					|| s.culture == com.stasdoto.medieval.history.Culture.NORSE;
			for (int x = x0 + 1; x < x1; x++) {
				for (int z = z0 + 1; z < z1; z++) {
					if (Math.hypot(x - s.x, z - s.z) > reach) {
						continue;
					}
					double k = Noise.rand(x, z, (int) s.id + 77);
					if (k > 0.16) {
						continue;
					}
					Terrain.Col c = col(x, z);
					if (c.waterTop != Terrain.NO_WATER) {
						continue;
					}
					int t = c.top();
					// Only on bare ground, never on streets, floors or yards that were drawn.
					if (!get(x, t, z).is(Blocks.GRASS_BLOCK) || !get(x, t + 1, z).isAir()) {
						continue;
					}
					if (k < 0.006) {
						tree(x, t + 1, z, south, north, k);
					} else if (k < 0.02) {
						set(x, t + 1, z, (south ? Blocks.ACACIA_LEAVES : north ? Blocks.SPRUCE_LEAVES : Blocks.OAK_LEAVES).defaultBlockState()
								.setValue(BlockStateProperties.PERSISTENT, true));
					} else if (k < 0.024) {
						set(x, t + 1, z, Blocks.HAY_BLOCK.defaultBlockState());
					} else if (k < 0.027) {
						set(x, t + 1, z, Blocks.BARREL.defaultBlockState());
					} else if (k < 0.029) {
						set(x, t + 1, z, Bp.log(Blocks.OAK_LOG, (x & 1) == 0 ? Direction.Axis.X : Direction.Axis.Z));
					} else if (k < 0.031) {
						set(x, t + 1, z, Blocks.COMPOSTER.defaultBlockState());
					} else if (k < 0.033 && !south) {
						set(x, t + 1, z, Blocks.OAK_FENCE.defaultBlockState());
						set(x, t + 2, z, Blocks.HAY_BLOCK.defaultBlockState());
						set(x, t + 3, z, Blocks.CARVED_PUMPKIN.defaultBlockState());
					} else if (k < 0.07) {
						BlockState[] fl = {Blocks.POPPY.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(), Blocks.DANDELION.defaultBlockState(),
								Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(), Blocks.BUSH.defaultBlockState()};
						set(x, t + 1, z, fl[(int) (k * 1000) % fl.length]);
					} else {
						set(x, t + 1, z, Blocks.SHORT_GRASS.defaultBlockState());
					}
				}
			}
		}

		void tree(int x, int y, int z, boolean south, boolean north, double k) {
			Block log = south ? Blocks.ACACIA_LOG : north ? (k < 0.003 ? Blocks.BIRCH_LOG : Blocks.SPRUCE_LOG) : (k < 0.003 ? Blocks.BIRCH_LOG : Blocks.OAK_LOG);
			Block leaf = south ? Blocks.ACACIA_LEAVES : log == Blocks.BIRCH_LOG ? Blocks.BIRCH_LEAVES : north ? Blocks.SPRUCE_LEAVES : Blocks.OAK_LEAVES;
			BlockState lv = leaf.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
			int h = 4 + (int) (k * 1000) % 3;
			for (int dy = -2; dy <= 1; dy++) {
				int r = dy == 1 ? 1 : 2;
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (Math.abs(dx) + Math.abs(dz) <= r + 1 && in(x + dx, z + dz) && get(x + dx, y + h + dy, z + dz).isAir()) {
							set(x + dx, y + h + dy, z + dz, lv);
						}
					}
				}
			}
			for (int i = 0; i < h; i++) {
				set(x, y + i, z, log.defaultBlockState());
			}
		}

		// ---- walls ----

		void walls(Layout l) {
			for (Layout.Wall w : l.walls) {
				double maxR = w.maxRadius();
				if (Math.hypot(x0 + 8 - w.cx, z0 + 8 - w.cz) > maxR + 40 || Math.hypot(x0 + 8 - w.cx, z0 + 8 - w.cz) < maxR * 0.6 - 40) {
					continue;
				}
				for (int x = x0; x <= x1; x++) {
					for (int z = z0; z <= z1; z++) {
						wallColumn(l, w, x, z);
					}
				}
			}
		}

		void wallColumn(Layout l, Layout.Wall w, int x, int z) {
			double dx = x + 0.5 - w.cx, dz = z + 0.5 - w.cz;
			double a = Math.atan2(dz, dx);
			double R = w.radiusAt(a);
			double band = Math.hypot(dx, dz) - R;
			double reach = switch (w.type) {
				case RAMPART -> 12;
				case STONE, MUDBRICK -> 5;
				case PALISADE -> 4;
			};
			if (Math.abs(band) > reach) {
				return;
			}
			Terrain.Col c = col(x, z);
			if (c.waterTop != Terrain.NO_WATER) {
				return;
			}
			int t = c.top();
			// Towers every ~36 blocks along the ring; gates where the streets pass.
			double along = a * R;
			double spacing = w.type == Layout.WallType.PALISADE ? 30 : 36;
			double towerPos = Math.round(along / spacing) * spacing;
			double alongOff = along - towerPos;
			boolean gate = w.gateAt(a) && Math.abs(gateOffset(w, a, R)) < 3.0;
			boolean tower = !gate && Math.abs(alongOff) <= 3.5 && band > -4 && band < 3.5 && w.type != Layout.WallType.RAMPART
					|| gateTower(w, a, R, band);
			BlockState stone = switch (w.type) {
				case STONE -> Noise.rand(x, z, (int) (t * 7L)) < 0.2 ? Blocks.COBBLESTONE.defaultBlockState()
						: Noise.rand(x, z, 3) < 0.08 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
				case MUDBRICK -> Noise.rand(x, z, 3) < 0.25 ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.MUD_BRICKS.defaultBlockState();
				default -> Bp.log(Blocks.SPRUCE_LOG, Direction.Axis.Y);
			};
			int H = w.height;
			switch (w.type) {
				case STONE, MUDBRICK -> {
					if (tower) {
						int th = H + 5;
						for (int y = t + 1; y <= t + th; y++) {
							set(x, y, z, stone);
						}
						clearAbove(x, t + th, z, 6);
						if (Math.abs(band) > 2.5 || Math.abs(alongOff) > 2.5) {
							if ((x + z) % 2 == 0) {
								set(x, t + th + 1, z, stone);
							}
						}
						if (Math.abs(alongOff) < 0.6 && band > 3) {
							set(x, t + th - 3, z, AIR);
						}
						return;
					}
					if (band < -1.6 || band > 1.6) {
						if (band > 2.5 && band < 5 && w.type == Layout.WallType.STONE && !w.citadel) {
							// A dry moat in front of the city wall.
							for (int y = t - 2; y <= t; y++) {
								set(x, y, z, AIR);
							}
							set(x, t - 3, z, Blocks.COARSE_DIRT.defaultBlockState());
						}
						return;
					}
					for (int y = t + 1; y <= t + H; y++) {
						if (gate && y <= t + 5) {
							set(x, y, z, AIR);
						} else {
							set(x, y, z, stone);
						}
					}
					clearAbove(x, t + H, z, 8);
					if (gate) {
						set(x, t, z, Blocks.COBBLESTONE.defaultBlockState());
					}
					if (band > 0.6 && ((int) Math.floor(along)) % 2 == 0) {
						set(x, t + H + 1, z, stone);
					}
				}
				case PALISADE -> {
					if (tower) {
						for (int y = t + 1; y <= t + H + 4; y++) {
							boolean edge = Math.abs(band) > 1.6 || Math.abs(alongOff) > 2.4;
							set(x, y, z, edge || y == t + H + 1 ? Bp.log(Blocks.SPRUCE_LOG, Direction.Axis.Y) : AIR);
						}
						set(x, t + H + 5, z, Blocks.SPRUCE_SLAB.defaultBlockState());
						set(x, t + H + 1, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
						return;
					}
					if (Math.abs(band) > 0.55) {
						if (band < -0.55 && band > -1.6) {
							set(x, t + H - 2, z, Bp.slab(Blocks.SPRUCE_SLAB, true));
						}
						return;
					}
					for (int y = t + 1; y <= t + H; y++) {
						set(x, y, z, gate && y <= t + 4 ? AIR : stone);
					}
					if (!gate && ((int) Math.floor(along)) % 2 == 0) {
						set(x, t + H + 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
					}
					clearAbove(x, t + H + 1, z, 8);
				}
				case RAMPART -> {
					// Earth bank with a ditch in front and a log wall of timber cribs (городни) on top.
					if (band > 7.5 && band < 11.5) {
						if (gate) {
							return;
						}
						for (int y = t - 2; y <= t; y++) {
							set(x, y, z, AIR);
						}
						set(x, t - 3, z, Blocks.MUD.defaultBlockState());
						clearAbove(x, t, z, 12);
						return;
					}
					if (band > 7.5) {
						return;
					}
					if (gate) {
						set(x, t, z, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
						for (int y = t + 1; y <= t + 9; y++) {
							boolean beam = y >= t + 6;
							set(x, y, z, beam && Math.abs(band) < 3 ? Bp.log(Blocks.SPRUCE_LOG, Direction.Axis.Y) : AIR);
						}
						return;
					}
					double bh = 6 * (1 - (band / 7.5) * (band / 7.5));
					int top = t + (int) Math.round(bh);
					for (int y = t + 1; y <= top; y++) {
						set(x, y, z, y == top ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState());
					}
					clearAbove(x, top, z, 14);
					if (Math.abs(band) <= 1.5) {
						int wallTop = top + 5;
						for (int y = top + 1; y <= wallTop; y++) {
							Direction.Axis ax = (y % 2 == 0) ? Direction.Axis.X : Direction.Axis.Z;
							set(x, y, z, Math.abs(band) > 0.5 ? Bp.log(Blocks.SPRUCE_LOG, ax) : Blocks.SPRUCE_PLANKS.defaultBlockState());
						}
						set(x, wallTop + 1, z, Bp.slab(Blocks.SPRUCE_SLAB, false));
						if (band > 0.5 && ((int) Math.floor(along)) % 3 != 0) {
							set(x, wallTop + 1, z, Bp.log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
						}
						// Towers (вежи) on the bank.
						if (Math.abs(alongOff) <= 2.5) {
							for (int y = wallTop + 1; y <= wallTop + 5; y++) {
								set(x, y, z, Math.abs(alongOff) > 1.5 || Math.abs(band) > 1.0 ? Bp.log(Blocks.SPRUCE_LOG, Direction.Axis.Y) : AIR);
							}
							set(x, wallTop + 6, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
							set(x, wallTop + 7, z, Blocks.SPRUCE_SLAB.defaultBlockState());
						}
					}
				}
			}
		}

		/** How far (along the wall) a point is from the nearest gate's centre. */
		static double gateOffset(Layout.Wall w, double a, double R) {
			int i = (int) Math.round((a / (2 * Math.PI) * 64 + 64)) % 64;
			double ga = i * 2 * Math.PI / 64;
			double d = a - ga;
			while (d > Math.PI) {
				d -= 2 * Math.PI;
			}
			while (d < -Math.PI) {
				d += 2 * Math.PI;
			}
			return d * R;
		}

		/** The towers either side of a gate (stone and mud walls). */
		static boolean gateTower(Layout.Wall w, double a, double R, double band) {
			if (w.type != Layout.WallType.STONE && w.type != Layout.WallType.MUDBRICK) {
				return false;
			}
			int i = (int) Math.round((a / (2 * Math.PI) * 64 + 64)) % 64;
			for (int k = -1; k <= 1; k++) {
				int j = (i + k + 64) % 64;
				if (!w.gate[j]) {
					continue;
				}
				double ga = j * 2 * Math.PI / 64;
				double d = a - ga;
				while (d > Math.PI) {
					d -= 2 * Math.PI;
				}
				while (d < -Math.PI) {
					d += 2 * Math.PI;
				}
				double off = Math.abs(d * R);
				if (off >= 3 && off <= 7 && band > -4 && band < 4) {
					return true;
				}
			}
			return false;
		}
	}
}
