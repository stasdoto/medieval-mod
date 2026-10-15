package com.stasdoto.medieval.construct;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Big buildings drawn as blueprints, front (+z) = the main entrance side. Built once and cached by {@link Landmarks}.
 * Everything is drawn at its real size (1 block = 1 m) with walkable interiors.
 */
final class Monuments {
	private Monuments() {
	}

	/** Blueprint size {w, d} of a monument. */
	static int[] size(String id) {
		return switch (id) {
			case "hagia_sophia" -> new int[]{77, 142};
			case "kyiv_sofia" -> new int[]{57, 52};
			case "kyiv_golden_gate" -> new int[]{48, 32};
			case "cordoba_mezquita" -> new int[]{128, 182};
			case "dome_of_rock" -> new int[]{100, 100};
			case "pantheon" -> new int[]{62, 88};
			default -> new int[]{16, 16};
		};
	}

	/** Big buildings drawn as blueprints, front (+z) = the main entrance side. Built once and cached by Landmarks. */
	static Bp build(String id) {
		int[] s = size(id);
		Bp b = new Bp(s[0], s[1], 1050L * 31 + id.hashCode());
		switch (id) {
			case "hagia_sophia" -> hagiaSophia(b);
			case "kyiv_sofia" -> kyivSofia(b);
			case "kyiv_golden_gate" -> goldenGate(b);
			case "cordoba_mezquita" -> mezquita(b);
			case "dome_of_rock" -> domeOfRock(b);
			case "pantheon" -> pantheon(b);
			default -> {
			}
		}
		return b;
	}

	// ---- helpers ----

	interface Mat {
		BlockState at(int x, int y, int z);
	}

	interface Keep {
		boolean test(int x, int z);
	}

	private static BlockState st(Block b) {
		return b.defaultBlockState();
	}

	private static BlockState dyed(DyeColor c) {
		return Blocks.DYED_TERRACOTTA.pick(c).defaultBlockState();
	}

	private static BlockState glazed(DyeColor c) {
		return Blocks.GLAZED_TERRACOTTA.pick(c).defaultBlockState();
	}

	private static BlockState pillar() {
		return Bp.log(Blocks.QUARTZ_PILLAR, Direction.Axis.Y);
	}

	private static BlockState candle() {
		return Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.LIT, true).setValue(BlockStateProperties.CANDLES, 3);
	}

	private static BlockState arch(Block stair, Direction towardColumn) {
		return Bp.stairs(stair, towardColumn, true);
	}

	/** A repeatable random number in [0, 1) for a position. */
	private static double h(int x, int y, int z, int s) {
		long v = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ s * 0x27D4EB2F165667C5L;
		v ^= v >>> 31;
		v *= 0x7FB5D329728EA185L;
		v ^= v >>> 27;
		v *= 0x81DADEF4BC2DD44DL;
		v ^= v >>> 33;
		return (v >>> 11) * 0x1.0p-53;
	}

	/** Gold mosaic: mostly ochre tesserae with gold glinting. */
	private static BlockState gold(int x, int y, int z) {
		return h(x, y, z, 1) < 0.22 ? st(Blocks.GOLD_BLOCK) : dyed(DyeColor.YELLOW);
	}

	/** Frescoes: painted plaster in a few colours. */
	private static BlockState fresco(int x, int y, int z) {
		double r = h(x, y, z, 2);
		DyeColor c = r < 0.3 ? DyeColor.WHITE : r < 0.5 ? DyeColor.ORANGE : r < 0.65 ? DyeColor.LIGHT_BLUE : r < 0.8 ? DyeColor.YELLOW
				: r < 0.9 ? DyeColor.BROWN : DyeColor.RED;
		return dyed(c);
	}

	/** Byzantine brick with stone bands. */
	private static BlockState byz(int x, int y, int z) {
		if (Math.floorMod(y, 6) == 0) {
			return st(Blocks.STONE_BRICKS);
		}
		return h(x, y, z, 3) < 0.06 ? st(Blocks.MUD_BRICKS) : st(Blocks.BRICKS);
	}

	/** Kyivan opus mixtum: thin brick with rows of rough pink granite and quartzite. */
	private static BlockState mixtum(int x, int y, int z) {
		if (Math.floorMod(y, 4) == 0) {
			return h(x, y, z, 4) < 0.55 ? st(Blocks.GRANITE) : st(Blocks.COBBLESTONE);
		}
		return st(Blocks.BRICKS);
	}

	/** A spherical shell around (cx, y0, cz): the skin from r to r + thick, an inner (mosaic) layer innerThick deep inside r. */
	private static void sphere(Bp b, double cx, int y0, double cz, double r, double thick, double yScale, Mat outer, Mat inner, double innerThick, Keep keep) {
		double rr = r + thick;
		int top = y0 + (int) Math.ceil(rr * yScale);
		for (int y = y0; y <= top; y++) {
			double dy = (y - y0) / yScale;
			for (int x = (int) Math.floor(cx - rr) - 1; x <= (int) Math.ceil(cx + rr); x++) {
				for (int z = (int) Math.floor(cz - rr) - 1; z <= (int) Math.ceil(cz + rr); z++) {
					if (keep != null && !keep.test(x, z)) {
						continue;
					}
					double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
					double d = Math.sqrt(dx * dx + dz * dz + dy * dy);
					if (d >= r && d < rr) {
						b.set(x, y, z, outer.at(x, y, z));
					} else if (inner != null && d >= r - innerThick && d < r) {
						b.set(x, y, z, inner.at(x, y, z));
					}
				}
			}
		}
	}

	/** A straight flight of stairs going up towards dir, w wide (to the right-hand +x/+z side), clearing headroom. */
	private static void flight(Bp b, int x, int y, int z, Direction dir, int n, int w, Block stair) {
		int sx = dir.getStepX(), sz = dir.getStepZ();
		for (int i = 0; i < n; i++) {
			for (int k = 0; k < w; k++) {
				int xx = x + sx * i + (sx == 0 ? k : 0), zz = z + sz * i + (sz == 0 ? k : 0);
				b.set(xx, y + i, zz, Bp.stairs(stair, dir, false));
				for (int yy = y + i + 1; yy <= y + i + 3; yy++) {
					b.air(xx, yy, zz);
				}
			}
		}
	}

	// =====================================================================================================================
	// Hagia Sophia (Constantinople)
	// =====================================================================================================================

	/**
	 * Hagia Sophia as Justinian left it, front (+z) = the west entrance: the atrium with its fountain, the outer and inner
	 * narthex, the nave under the great dome (31 m, 40 windows round its base) on pendentives and four great arches, the
	 * east and west half-domes, aisles and galleries, the apse with the altar under its ciborium. No minarets in 1050.
	 * x runs across (north to south), z from the apse (0, east) to the atrium (west).
	 */
	private static void hagiaSophia(Bp b) {
		final double cx = 38.5, cz = 43.5;
		BlockState smooth = st(Blocks.SMOOTH_STONE), glass = st(Blocks.GLASS_PANE), andesite = st(Blocks.POLISHED_ANDESITE);
		Mat mosaic = Monuments::gold;
		Mat lead = (x, y, z) -> smooth;
		// Marble floor with bands of green marble ("rivers") and the porphyry Omphalion.
		for (int x = 2; x <= 74; x++) {
			for (int z = 3; z <= 89; z++) {
				BlockState f = Math.floorMod(z, 11) == 0 ? andesite : st(Blocks.POLISHED_DIORITE);
				if (x >= 45 && x <= 48 && z >= 47 && z <= 50) {
					f = x >= 46 && x <= 47 && z >= 48 && z <= 49 ? st(Blocks.GRANITE) : st(Blocks.POLISHED_GRANITE);
				}
				b.set(x, 0, z, f);
				b.set(x, -1, z, Blocks.STONE_BRICKS);
			}
		}
		// Outer walls of the church and the inner narthex.
		for (int y = 1; y <= 25; y++) {
			for (int z = 8; z <= 84; z++) {
				for (int x : new int[]{2, 3, 73, 74}) {
					b.set(x, y, z, byz(x, y, z));
				}
			}
			for (int x = 2; x <= 74; x++) {
				for (int z : new int[]{8, 9, 77, 78, 84}) {
					b.set(x, y, z, byz(x, y, z));
				}
			}
		}
		// Windows of the aisles and the galleries.
		for (int z = 12; z <= 74; z += 4) {
			for (int y = 4; y <= 8; y++) {
				for (int x : new int[]{2, 3, 73, 74}) {
					b.set(x, y, z, glass);
				}
			}
			for (int y = 15; y <= 21; y++) {
				for (int x : new int[]{2, 3, 73, 74}) {
					b.set(x, y, z, glass);
				}
			}
		}
		for (int x : new int[]{6, 10, 14, 62, 66, 70}) {
			for (int y = 4; y <= 8; y++) {
				b.set(x, y, 8, glass);
				b.set(x, y, 9, glass);
			}
			for (int y = 15; y <= 21; y++) {
				b.set(x, y, 8, glass);
				b.set(x, y, 9, glass);
			}
		}
		// The apse, with its windows and a semi-dome (conch) of gold mosaic with the Theotokos.
		for (int x = 28; x <= 49; x++) {
			for (int z = 2; z <= 12; z++) {
				double d = Math.hypot(x + 0.5 - cx, z + 0.5 - 12.5);
				if (z + 0.5 > 12.5) {
					continue;
				}
				if (d < 7.5) {
					for (int y = 1; y <= 17; y++) {
						b.air(x, y, z);
					}
					b.set(x, 0, z, st(Blocks.POLISHED_DIORITE));
				} else if (d < 9.0) {
					double ang = Math.toDegrees(Math.atan2(z + 0.5 - 12.5, x + 0.5 - cx));
					boolean win = false;
					for (double a : new double[]{-90, -135, -45}) {
						if (Math.abs(ang - a) * Math.PI / 180 * d < 0.8) {
							win = true;
						}
					}
					for (int y = 1; y <= 17; y++) {
						b.set(x, y, z, win && (y >= 5 && y <= 9 || y >= 11 && y <= 15) ? glass : byz(x, y, z));
					}
				}
			}
		}
		sphere(b, cx, 17, 12.5, 7.5, 1.3, 1.0, lead, (x, y, z) -> Math.abs(x + 0.5 - cx) < 2 && y >= 18 && y <= 22 ? dyed(DyeColor.BLUE) : gold(x, y, z), 1.1,
				(x, z) -> z + 0.5 < 12.5);
		// Synthronon (stepped seats of the clergy) round the apse, the altar under its ciborium.
		for (int x = 28; x <= 49; x++) {
			for (int z = 3; z <= 12; z++) {
				double d = Math.hypot(x + 0.5 - cx, z + 0.5 - 12.5);
				if (z + 0.5 < 12.5 && d >= 6 && d < 7.5) {
					b.set(x, 1, z, Blocks.POLISHED_DIORITE);
					if (d >= 6.8) {
						b.set(x, 2, z, Blocks.POLISHED_DIORITE);
					}
				}
			}
		}
		for (int x = 37; x <= 39; x++) {
			b.set(x, 1, 13, Blocks.GOLD_BLOCK);
			b.set(x, 2, 13, candle());
		}
		for (int[] c : new int[][]{{35, 11}, {41, 11}, {35, 15}, {41, 15}}) {
			for (int y = 1; y <= 5; y++) {
				b.set(c[0], y, c[1], pillar());
			}
		}
		b.fill(35, 6, 11, 41, 6, 15, st(Blocks.CHISELED_QUARTZ_BLOCK));
		b.fill(36, 7, 12, 40, 7, 14, st(Blocks.QUARTZ_BLOCK));
		b.set(38, 8, 13, Blocks.GOLD_BLOCK);
		// The four great piers.
		int[][] piers = {{17, 22, 22, 27}, {54, 59, 22, 27}, {17, 22, 59, 64}, {54, 59, 59, 64}};
		for (int[] p : piers) {
			for (int x = p[0]; x <= p[1]; x++) {
				for (int z = p[2]; z <= p[3]; z++) {
					for (int y = 1; y <= 39; y++) {
						b.set(x, y, z, byz(x, y, z));
					}
					b.set(x, 40, z, smooth);
				}
			}
		}
		// Galleries over the aisles and the narthex: floor at 12, lead roof at 25, columns in the aisles.
		for (int x = 4; x <= 72; x++) {
			for (int z = 10; z <= 83; z++) {
				if (z >= 77 && z <= 78) {
					continue;
				}
				if (hsNave(x, z) || pierAt(piers, x, z) || (x == 22 || x == 54) && z >= 28 && z <= 58) {
					continue;
				}
				b.set(x, 12, z, andesite);
				b.set(x, 25, z, smooth);
				if ((x == 12 || x == 64) && z <= 74 && Math.floorMod(z - 13, 6) == 0) {
					for (int y = 1; y <= 11; y++) {
						b.set(x, y, z, pillar());
					}
					for (int y = 13; y <= 24; y++) {
						b.set(x, y, z, pillar());
					}
				}
				if (Math.floorMod(x, 8) == 4 && Math.floorMod(z, 8) == 4) {
					b.set(x, 11, z, Bp.lantern(true));
					b.set(x, 24, z, Bp.lantern(true));
				}
			}
		}
		for (int x = 2; x <= 74; x++) {
			for (int z : new int[]{8, 9, 77, 78, 84}) {
				b.set(x, 25, z, smooth);
			}
		}
		for (int z = 8; z <= 84; z++) {
			for (int x : new int[]{2, 3, 73, 74}) {
				b.set(x, 25, z, smooth);
			}
		}
		// The north and south colonnades and, above them, the tympanum walls under the great arches.
		int[] colsG = {30, 35, 40, 46, 51, 56};
		for (int px : new int[]{22, 54}) {
			for (int z = 28; z <= 58; z++) {
				boolean col = contains(colsG, z);
				boolean gcol = Math.floorMod(z - 28, 3) == 1;
				if (col) {
					for (int y = 1; y <= 7; y++) {
						b.set(px, y, z, pillar());
					}
					b.set(px, 8, z, Blocks.CHISELED_QUARTZ_BLOCK);
				} else if (contains(colsG, z - 1) || z == 28) {
					b.set(px, 8, z, arch(Blocks.QUARTZ_STAIRS, Direction.NORTH));
				} else if (contains(colsG, z + 1) || z == 58) {
					b.set(px, 8, z, arch(Blocks.QUARTZ_STAIRS, Direction.SOUTH));
				}
				for (int y = 9; y <= 11; y++) {
					b.set(px, y, z, Blocks.CALCITE);
				}
				b.set(px, 12, z, andesite);
				if (gcol) {
					for (int y = 13; y <= 20; y++) {
						b.set(px, y, z, pillar());
					}
					b.set(px, 21, z, Blocks.CHISELED_QUARTZ_BLOCK);
				} else {
					b.set(px, 21, z, arch(Blocks.QUARTZ_STAIRS, Math.floorMod(z - 28, 3) == 2 ? Direction.NORTH : Direction.SOUTH));
				}
				double dz = z + 0.5 - cz;
				double yA = 24 + Math.sqrt(Math.max(0, 15.5 * 15.5 - dz * dz));
				for (int y = 22; y <= 40; y++) {
					boolean win = y >= 27 && y <= 30 && z >= 31 && z <= 55 && Math.floorMod(z - 31, 4) == 0
							|| y >= 33 && y <= 36 && z >= 37 && z <= 49 && Math.floorMod(z - 37, 4) == 0;
					BlockState s = y > yA && y <= yA + 1.5 ? st(Blocks.BRICKS) : byz(px, y, z);
					b.set(px, y, z, win ? glass : s);
				}
			}
		}
		// The east and west great arches with the wall above them up to the base of the dome.
		for (int x = 23; x <= 53; x++) {
			double dx = x + 0.5 - cx;
			double yA = 24 + Math.sqrt(Math.max(0, 15.5 * 15.5 - dx * dx));
			for (int z : new int[]{27, 59}) {
				for (int y = (int) Math.floor(yA - 1.5); y <= 40; y++) {
					if (y >= 1) {
						b.set(x, y, z, y <= yA ? st(Blocks.BRICKS) : byz(x, y, z));
					}
				}
			}
		}
		// Pendentives: the spherical triangles between the arches, covered in mosaic.
		for (int x = 23; x <= 53; x++) {
			for (int z = 28; z <= 58; z++) {
				double d = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
				for (int y = 24; y <= 39; y++) {
					double thr = Math.sqrt(Math.max(0, 21.92 * 21.92 - (y - 24) * (y - 24)));
					if (d > thr) {
						b.set(x, y, z, d < thr + 1.3 ? gold(x, y, z) : st(Blocks.BRICKS));
					}
				}
			}
		}
		// The great dome: lead outside, gold inside, forty windows round its base with buttresses between them.
		final double R = 15.5;
		for (int y = 40; y <= 57; y++) {
			int dy = y - 40;
			for (int x = 20; x <= 57; x++) {
				for (int z = 25; z <= 62; z++) {
					double dx = x + 0.5 - cx, dz = z + 0.5 - cz;
					double d = Math.sqrt(dx * dx + dz * dz + dy * dy);
					double ang = Math.atan2(dz, dx) / (2 * Math.PI) * 40;
					double fr = ang - Math.floor(ang);
					boolean win = dy <= 3 && fr > 0.3 && fr < 0.7;
					if (d >= R && d < R + 1.3) {
						b.set(x, y, z, win ? glass : smooth);
					} else if (d >= R - 1.1 && d < R) {
						BlockState in = dy >= 12 && dy <= 13 ? dyed(DyeColor.LIGHT_BLUE) : gold(x, y, z);
						b.set(x, y, z, win ? glass : in);
					} else if (dy <= 3 && d >= R + 1.3 && d < R + 2.6 && !win) {
						b.set(x, y, z, byz(x, y, z));
					} else if (dy == 0 && d >= R + 1.3 && x >= 22 && x <= 54 && z >= 27 && z <= 59) {
						b.set(x, y, z, smooth);
					}
				}
			}
		}
		b.cross(38, 57, 43);
		// The east and west half-domes, and the arcades under their rims.
		sphere(b, cx, 24, 28.0, 15.5, 1.3, 1.0, lead, mosaic, 1.1, (x, z) -> z + 0.5 < 28.0);
		sphere(b, cx, 24, 59.0, 15.5, 1.3, 1.0, lead, mosaic, 1.1, (x, z) -> z + 0.5 > 59.0);
		for (int x = 20; x <= 57; x++) {
			for (int z = 10; z <= 76; z++) {
				boolean east = z < 28, west = z > 58;
				if (!east && !west) {
					continue;
				}
				double c0 = east ? 28.0 : 59.0;
				double d = Math.hypot(x + 0.5 - cx, z + 0.5 - c0);
				if (d < 15.5 || d >= 16.8) {
					continue;
				}
				if (east && Math.abs(x + 0.5 - cx) < 7.5 || pierAt(piers, x, z)) {
					continue; // open into the apse; the piers stay whole
				}
				double pos = Math.atan2(z + 0.5 - c0, x + 0.5 - cx) * 16;
				for (int y = 1; y <= 24; y++) {
					boolean lower = y <= 8, upper = y >= 13 && y <= 20;
					double f4 = pos / 4 - Math.floor(pos / 4), f3 = pos / 3 - Math.floor(pos / 3);
					BlockState s;
					if (lower) {
						s = f4 > 0.3 ? Blocks.AIR.defaultBlockState() : pillar();
					} else if (upper) {
						s = f3 > 0.35 ? Blocks.AIR.defaultBlockState() : pillar();
					} else {
						s = st(Blocks.CALCITE);
					}
					b.set(x, y, z, s);
				}
			}
		}
		// Chandeliers hanging on long chains round the nave.
		for (int k = 0; k < 8; k++) {
			double a = k * Math.PI / 4;
			int x = (int) Math.floor(cx + 11 * Math.cos(a)), z = (int) Math.floor(cz + 11 * Math.sin(a));
			for (int y = 10; y <= 48; y++) {
				b.set(x, y, z, Blocks.IRON_CHAIN);
			}
			b.set(x, 9, z, Bp.lantern(true));
		}
		// The templon screen, the solea and the ambo.
		for (int x = 30; x <= 47; x++) {
			if (x >= 37 && x <= 40) {
				continue;
			}
			b.set(x, 1, 21, Blocks.POLISHED_DIORITE);
			if (x % 2 == 0) {
				b.set(x, 2, 21, pillar());
				b.set(x, 3, 21, pillar());
				b.set(x, 4, 21, pillar());
			} else {
				b.set(x, 2, 21, candle());
			}
			b.set(x, 5, 21, Blocks.CHISELED_QUARTZ_BLOCK);
		}
		for (int x = 37; x <= 40; x++) {
			b.set(x, 5, 21, Blocks.GOLD_BLOCK);
		}
		for (int z = 22; z <= 41; z++) {
			b.set(38, 1, z, Bp.slab(Blocks.POLISHED_DIORITE_SLAB, false));
			b.set(39, 1, z, Bp.slab(Blocks.POLISHED_DIORITE_SLAB, false));
		}
		for (int x = 36; x <= 41; x++) {
			for (int z = 44; z <= 48; z++) {
				for (int y = 1; y <= 3; y++) {
					b.set(x, y, z, Blocks.CALCITE);
				}
				if (x == 36 || x == 41 || z == 44 || z == 48) {
					b.set(x, 4, z, (x == 36 || x == 41) && (z == 44 || z == 48) ? st(Blocks.GOLD_BLOCK) : Bp.slab(Blocks.POLISHED_DIORITE_SLAB, false));
				}
			}
		}
		for (int x = 38; x <= 39; x++) {
			b.air(x, 4, 44);
			b.air(x, 4, 48);
			b.set(x, 3, 43, Bp.stairs(Blocks.QUARTZ_STAIRS, Direction.SOUTH, false));
			b.set(x, 2, 42, Bp.stairs(Blocks.QUARTZ_STAIRS, Direction.SOUTH, false));
			b.set(x, 1, 41, Bp.stairs(Blocks.QUARTZ_STAIRS, Direction.SOUTH, false));
			b.set(x, 3, 49, Bp.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
			b.set(x, 2, 50, Bp.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
			b.set(x, 1, 51, Bp.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
		}
		b.set(38, 4, 46, Blocks.LECTERN);
		// Doors: the Imperial Door in the middle, four more, through both narthexes.
		int[][] doors = {{12, 14}, {24, 26}, {50, 52}, {62, 64}};
		for (int[] d : doors) {
			for (int x = d[0]; x <= d[1]; x++) {
				for (int y = 1; y <= 6; y++) {
					b.air(x, y, 77);
					b.air(x, y, 78);
					b.air(x, y, 84);
					b.air(x, y, 89);
				}
			}
		}
		for (int x = 36; x <= 41; x++) {
			for (int y = 1; y <= 9; y++) {
				b.air(x, y, 77);
				b.air(x, y, 78);
			}
			for (int y = 1; y <= 7; y++) {
				b.air(x, y, 84);
				b.air(x, y, 89);
			}
			b.set(x, 10, 78, x == 38 || x == 39 ? dyed(DyeColor.BLUE) : st(Blocks.GOLD_BLOCK));
			b.set(x, 11, 78, gold(x, 11, 78));
		}
		for (int x = 30; x <= 47; x++) {
			for (int y = 13; y <= 18; y++) {
				b.air(x, y, 77);
				b.air(x, y, 78);
			}
		}
		// The stair up to the galleries at the north end of the narthex.
		flight(b, 5, 1, 79, Direction.EAST, 11, 2, Blocks.STONE_BRICK_STAIRS);
		// The outer narthex (exonarthex).
		for (int y = 1; y <= 12; y++) {
			for (int x = 4; x <= 72; x++) {
				b.set(x, y, 89, byz(x, y, 89));
			}
			for (int z = 85; z <= 89; z++) {
				b.set(4, y, z, byz(4, y, z));
				b.set(72, y, z, byz(72, y, z));
			}
		}
		for (int[] d : doors) {
			for (int x = d[0]; x <= d[1]; x++) {
				for (int y = 1; y <= 6; y++) {
					b.air(x, y, 89);
				}
			}
		}
		for (int x = 36; x <= 41; x++) {
			for (int y = 1; y <= 7; y++) {
				b.air(x, y, 89);
			}
		}
		b.fill(4, 13, 85, 72, 13, 89, smooth);
		// The atrium with porticoes on three sides and the phiale (fountain) in the middle.
		for (int x = 12; x <= 64; x++) {
			for (int z = 90; z <= 139; z++) {
				b.set(x, 0, z, (x + z) % 2 == 0 ? andesite : st(Blocks.POLISHED_DIORITE));
				b.set(x, -1, z, Blocks.STONE_BRICKS);
				boolean wall = x == 12 || x == 64 || z == 139;
				if (wall) {
					for (int y = 1; y <= 9; y++) {
						b.set(x, y, z, byz(x, y, z));
					}
					continue;
				}
				boolean portico = x <= 18 || x >= 58 || z >= 133;
				boolean colonnade = (x == 18 || x == 58) && z <= 133 || z == 133 && x >= 18 && x <= 58;
				if (colonnade) {
					int c = x == 18 || x == 58 ? z : x;
					if (Math.floorMod(c, 3) == 0) {
						for (int y = 1; y <= 7; y++) {
							b.set(x, y, z, pillar());
						}
					}
					b.set(x, 8, z, Blocks.CALCITE);
				}
				if (portico) {
					b.set(x, 9, z, Bp.slab(Blocks.SMOOTH_STONE_SLAB, false));
				}
			}
		}
		for (int x = 35; x <= 41; x++) {
			for (int y = 1; y <= 6; y++) {
				b.air(x, y, 139);
			}
		}
		for (int x = 32; x <= 45; x++) {
			for (int z = 105; z <= 118; z++) {
				double d = Math.hypot(x + 0.5 - 38.5, z + 0.5 - 111.5);
				if (d < 0.8) {
					b.set(x, 1, z, pillar());
					b.set(x, 2, z, pillar());
					b.set(x, 3, z, Blocks.CHISELED_QUARTZ_BLOCK);
				} else if (d < 3.5) {
					b.set(x, 1, z, Blocks.WATER);
				} else if (d < 4.5) {
					b.set(x, 1, z, Blocks.POLISHED_DIORITE);
				}
			}
		}
	}

	private static boolean contains(int[] a, int v) {
		for (int x : a) {
			if (x == v) {
				return true;
			}
		}
		return false;
	}

	private static boolean pierAt(int[][] piers, int x, int z) {
		for (int[] p : piers) {
			if (x >= p[0] && x <= p[1] && z >= p[2] && z <= p[3]) {
				return true;
			}
		}
		return false;
	}

	/** The open space of Hagia Sophia's nave: the central square, the half-dome bays (with their rims) and the apse. */
	private static boolean hsNave(int x, int z) {
		if (x >= 23 && x <= 53 && z >= 28 && z <= 58) {
			return true;
		}
		if (z < 28 && Math.hypot(x + 0.5 - 38.5, z + 0.5 - 28.0) < 16.8) {
			return true;
		}
		if (z > 58 && Math.hypot(x + 0.5 - 38.5, z + 0.5 - 59.0) < 16.8) {
			return true;
		}
		return z <= 12 && Math.hypot(x + 0.5 - 38.5, z + 0.5 - 12.5) < 9.0;
	}

	// =====================================================================================================================
	// St Sophia of Kyiv
	// =====================================================================================================================

	/**
	 * St Sophia of Kyiv as built by Yaroslav (1037), front (+z) = west: a cross-domed church of five naves and five apses,
	 * thirteen domes rising in a pyramid round the central one, open one-storey galleries round three sides and two stair
	 * towers at the west; walls of thin brick with rows of rough stone; inside the gold Oranta in the apse, frescoes and the
	 * choirs (polati) where the prince stood. x runs across (north to south), z from the apses (east, 0) to the west.
	 */
	private static void kyivSofia(Bp b) {
		BlockState smooth = st(Blocks.SMOOTH_STONE), glass = st(Blocks.GLASS_PANE), calcite = st(Blocks.CALCITE);
		final int side = 13, arm = 16; // tops of the side walls and of the cross arms
		// Floors.
		for (int x = 2; x <= 54; x++) {
			for (int z = 2; z <= 51; z++) {
				b.set(x, -1, z, Blocks.STONE_BRICKS);
			}
		}
		for (int x = 3; x <= 53; x++) {
			for (int z = 3; z <= 46; z++) {
				boolean core = x >= 13 && x <= 43 && z >= 12 && z <= 37;
				b.set(x, 0, z, core ? ((x + z) % 3 == 0 ? st(Blocks.POLISHED_GRANITE) : smooth) : st(Blocks.BRICKS));
			}
		}
		// Core walls: the cross arms run higher up to their semicircular gables (zakomaras).
		for (int x = 11; x <= 45; x++) {
			for (int z = 10; z <= 39; z++) {
				boolean wall = x <= 12 || x >= 44 || z <= 11 || z >= 38;
				if (!wall) {
					continue;
				}
				boolean inArm = x >= 25 && x <= 31 || z >= 23 && z <= 29;
				int top = inArm ? arm : side;
				boolean inner = x == 12 || x == 44 || z == 11 || z == 38;
				for (int y = 1; y <= top; y++) {
					b.set(x, y, z, inner && y >= 2 && y <= 7 ? fresco(x, y, z) : mixtum(x, y, z));
				}
				if (inArm) {
					double off = x >= 25 && x <= 31 && (z <= 11 || z >= 38) ? x + 0.5 - 28.5 : z + 0.5 - 26.5;
					for (int y = arm + 1; y <= arm + 4; y++) {
						if ((y - arm) * (y - arm) + off * off <= 3.7 * 3.7) {
							b.set(x, y, z, mixtum(x, y, z));
						}
					}
				}
				b.set(x, inArm ? arm + 1 : side + 1, z, smooth);
			}
		}
		// Windows in the core walls.
		for (int i = 14; i <= 42; i += 4) {
			for (int y = 9; y <= 11; y++) {
				b.set(i, y, 10, glass);
				b.set(i, y, 11, glass);
				b.set(i, y, 38, glass);
				b.set(i, y, 39, glass);
			}
		}
		for (int i = 14; i <= 37; i += 4) {
			for (int y = 9; y <= 11; y++) {
				b.set(11, y, i, glass);
				b.set(12, y, i, glass);
				b.set(44, y, i, glass);
				b.set(45, y, i, glass);
			}
		}
		// Five apses at the east; the Oranta on the gold of the main apse.
		int[][] apses = {{28, 50, 12}, {21, 32, 9}, {35, 32, 9}, {15, 24, 7}, {41, 24, 7}}; // x, radius*10, height
		for (int[] a : apses) {
			double acx = a[0] + 0.5, r = a[1] / 10.0;
			int ah = a[2];
			boolean main = a[0] == 28;
			for (int x = (int) Math.floor(acx - r - 2); x <= (int) Math.ceil(acx + r + 2); x++) {
				for (int z = 3; z <= 11; z++) {
					if (z + 0.5 > 12.0) {
						continue;
					}
					double d = Math.hypot(x + 0.5 - acx, z + 0.5 - 12.0);
					if (d < r) {
						for (int y = 1; y <= ah; y++) {
							b.air(x, y, z);
						}
						b.set(x, 0, z, Blocks.POLISHED_GRANITE);
					} else if (d < r + 1.5) {
						for (int y = 1; y <= ah; y++) {
							BlockState s = mixtum(x, y, z);
							if (d < r + 0.6 && y >= 2) {
								if (main) {
									boolean figure = Math.abs(x + 0.5 - acx) < 1.6 && y >= 4 && y <= 10
											|| Math.abs(Math.abs(x + 0.5 - acx) - 2.2) < 0.7 && y >= 9 && y <= 10;
									s = figure ? glazed(DyeColor.BLUE) : y == 11 && Math.abs(x + 0.5 - acx) < 1 ? glazed(DyeColor.WHITE) : gold(x, y, z);
								} else {
									s = fresco(x, y, z);
								}
							}
							b.set(x, y, z, s);
						}
						if (main && Math.abs(x + 0.5 - acx) < 0.8 && d >= r + 0.6) {
							b.set(x, 6, z, glass);
							b.set(x, 7, z, glass);
						}
					}
				}
			}
			Mat conch = main ? Monuments::gold : Monuments::fresco;
			sphere(b, acx, ah, 12.0, r, 1.3, 1.0, (x, y, z) -> smooth, conch, 1.0, (x, z) -> z + 0.5 < 12.0);
		}
		// Altar, templon, candles.
		b.set(28, 1, 9, Blocks.POLISHED_DIORITE);
		b.set(28, 2, 9, candle());
		for (int x = 19; x <= 37; x++) {
			if (x >= 27 && x <= 29) {
				continue;
			}
			b.set(x, 1, 13, Blocks.POLISHED_DIORITE);
			if (x % 2 == 0) {
				b.set(x, 2, 13, pillar());
				b.set(x, 3, 13, pillar());
				b.set(x, 4, 13, Blocks.CHISELED_QUARTZ_BLOCK);
			} else {
				b.set(x, 2, 13, candle());
			}
		}
		// Piers, arcades and choirs (polati) at 8, ceilings over the sides (13) and the cross arms (16).
		int[] lines = {17, 18, 23, 24, 32, 33, 38, 39};
		int[] rows = {15, 16, 21, 22, 30, 31, 34, 35};
		for (int x = 13; x <= 43; x++) {
			for (int z = 12; z <= 37; z++) {
				boolean lx = contains(lines, x), rz = contains(rows, z);
				boolean armX = x >= 25 && x <= 31, armZ = z >= 23 && z <= 29;
				boolean crossing = armX && armZ;
				boolean choir = (x <= 22 || x >= 34) && !armZ || z >= 32;
				if (choir && !(lx && rz)) {
					b.set(x, 8, z, st(Blocks.SPRUCE_PLANKS));
				}
				boolean nextToArm = x == 23 || x == 24 || x == 32 || x == 33 || z == 21 || z == 22 || z == 30 || z == 31;
				if (lx && rz) {
					int top = nextToArm ? arm : side;
					for (int y = 1; y <= top; y++) {
						b.set(x, y, z, y >= 2 && y <= 6 && h(x, y, z, 9) < 0.5 ? fresco(x, y, z) : calcite);
					}
				} else if ((lx || rz) && !(lx && armZ) && !(rz && armX)) {
					// Walls over the arcades between the piers; higher where they flank the cross arms.
					boolean tall = lx && (x == 23 || x == 24 || x == 32 || x == 33) || rz && (z == 21 || z == 22 || z == 30 || z == 31);
					int top = tall ? arm : side;
					for (int y = 9; y <= top; y++) {
						boolean look = y >= 10 && y <= 11 && (x + z) % 3 == 0;
						b.set(x, y, z, look ? Blocks.AIR.defaultBlockState() : calcite);
					}
				}
				if (!crossing) {
					b.set(x, armX || armZ ? arm : side, z, smooth);
				}
				if (!armX && !armZ) {
					b.set(x, side + 1, z, smooth);
				} else if (!crossing) {
					b.set(x, arm + 1, z, smooth);
				}
			}
		}
		// The way up to the choirs in the north-west corner.
		flight(b, 13, 1, 37, Direction.NORTH, 8, 2, Blocks.SPRUCE_STAIRS);
		// The central drum with twelve windows and the gilded helmet dome; gold mosaic under it (the Pantocrator).
		for (int x = 22; x <= 34; x++) {
			for (int z = 20; z <= 32; z++) {
				double d = Math.hypot(x - 28, z - 26);
				if (d > 3.5 && d <= 4.5) {
					double a = Math.atan2(z - 26, x - 28) / (2 * Math.PI) * 12;
					double fr = a - Math.floor(a);
					for (int y = arm; y <= arm + 7; y++) {
						boolean win = y >= arm + 2 && y <= arm + 5 && fr > 0.35 && fr < 0.65;
						b.set(x, y, z, win ? glass : mixtum(x, y, z));
					}
					b.set(x, arm + 8, z, Bp.slab(Blocks.SMOOTH_STONE_SLAB, false));
				} else if (d <= 3.5 && d > 2.6) {
					b.set(x, arm + 7, z, gold(x, arm + 7, z));
				}
			}
		}
		b.helmet(28, arm + 8, 26, 4, st(Blocks.GOLD_BLOCK), st(Blocks.IRON_CHAIN));
		for (int x = 25; x <= 31; x++) {
			for (int z = 23; z <= 29; z++) {
				double d = Math.hypot(x - 28, z - 26);
				if (d <= 3.3) {
					b.set(x, arm + 8, z, d < 1 ? dyed(DyeColor.BLUE) : gold(x, arm + 8, z));
				}
			}
		}
		b.cross(28, arm + 16, 26);
		for (int y = 9; y <= arm + 6; y++) {
			b.set(28, y, 26, Blocks.IRON_CHAIN);
		}
		b.set(28, 8, 26, Bp.lantern(true));
		// Four middle domes and eight small ones, all on drums, in a pyramid round the centre.
		int[][] mids = {{20, 18}, {36, 18}, {20, 33}, {36, 33}};
		for (int[] m : mids) {
			kyivDome(b, m[0], m[1], 2.5, (m[0] >= 25 && m[0] <= 31 || m[1] >= 23 && m[1] <= 29 ? arm : side) + 2, 4, 2);
		}
		int[][] smalls = {{14, 13}, {42, 13}, {14, 22}, {42, 22}, {14, 33}, {42, 33}, {28, 14}, {28, 36}};
		for (int[] m : smalls) {
			kyivDome(b, m[0], m[1], 1.6, (m[0] >= 25 && m[0] <= 31 || m[1] >= 23 && m[1] <= 29 ? arm : side) + 2, 3, 1);
		}
		// Doors into the church.
		for (int y = 1; y <= 5; y++) {
			for (int x = 27; x <= 29; x++) {
				b.air(x, y, 38);
				b.air(x, y, 39);
			}
			for (int z = 25; z <= 27; z++) {
				b.air(11, y, z);
				b.air(12, y, z);
				b.air(44, y, z);
				b.air(45, y, z);
			}
		}
		for (int y = 1; y <= 3; y++) {
			for (int x : new int[]{20, 36}) {
				b.air(x, y, 38);
				b.air(x, y, 39);
			}
		}
		// Open galleries round the north, south and west, arcaded towards the outside.
		for (int x = 3; x <= 53; x++) {
			for (int z = 12; z <= 46; z++) {
				boolean north = x <= 10 && z <= 46, south = x >= 46 && z <= 46, west = z >= 40;
				if (!(north || south || west) || x >= 11 && x <= 45 && z <= 39) {
					continue;
				}
				boolean outer = x == 3 || x == 53 || z == 46;
				boolean end = z == 12;
				if (outer || end) {
					int c = outer && z == 46 ? x : z;
					boolean pierCell = Math.floorMod(c, 4) == 0 || end;
					for (int y = 1; y <= 7; y++) {
						boolean open = !pierCell && y <= 5 || !pierCell && y == 6 && Math.floorMod(c, 4) == 2;
						b.set(x, y, z, open ? Blocks.AIR.defaultBlockState() : mixtum(x, y, z));
					}
				}
				boolean nearCore = x == 10 || x == 46 || z == 40;
				b.set(x, 8, z, smooth);
				if (nearCore) {
					b.set(x, 9, z, Bp.slab(Blocks.SMOOTH_STONE_SLAB, false));
				}
				if (!outer && !end && Math.floorMod(x + z, 9) == 0) {
					b.set(x, 7, z, Bp.lantern(true));
				}
			}
		}
		// Two stair towers at the west corners with spiral stairs up to a lookout.
		for (int t = 0; t < 2; t++) {
			int ox = t == 0 ? 2 : 48;
			for (int x = ox; x <= ox + 6; x++) {
				for (int z = 45; z <= 51; z++) {
					boolean wall = x == ox || x == ox + 6 || z == 45 || z == 51;
					for (int y = 1; y <= 17; y++) {
						if (wall) {
							b.set(x, y, z, mixtum(x, y, z));
						} else {
							b.air(x, y, z);
						}
					}
					b.set(x, 0, z, st(Blocks.BRICKS));
					b.set(x, 17, z, smooth);
					if (wall && (x + z) % 2 == 0) {
						b.set(x, 18, z, mixtum(x, 18, z));
					}
				}
			}
			int px = ox + 3, pz = 48;
			int[][] ring = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
			Direction[] dirs = {Direction.EAST, Direction.EAST, Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.WEST, Direction.NORTH, Direction.NORTH};
			for (int i = 0; i < 16; i++) {
				int[] c = ring[i % 8];
				b.set(px + c[0], 1 + i, pz + c[1], Bp.stairs(Blocks.STONE_BRICK_STAIRS, dirs[i % 8], false));
				b.set(px, 1 + i, pz, Blocks.STONE_BRICKS);
			}
			int[] last = ring[15 % 8];
			b.air(px + last[0], 17, pz + last[1]);
			b.air(px, 1, 45);
			b.air(px, 2, 45);
		}
	}

	/** A Rus helmet dome on a drum with windows; the drum stands on the roof at y0. */
	private static void kyivDome(Bp b, int cx, int cz, double r, int y0, int drumH, int helm) {
		BlockState glass = st(Blocks.GLASS_PANE);
		int ri = (int) Math.ceil(r);
		for (int x = cx - ri; x <= cx + ri; x++) {
			for (int z = cz - ri; z <= cz + ri; z++) {
				double d = Math.hypot(x - cx, z - cz);
				if (d > r) {
					continue;
				}
				for (int y = y0 - 1; y < y0 + drumH; y++) {
					boolean win = y == y0 + 1 && d > r - 1 && (x == cx || z == cz);
					b.set(x, y, z, d > r - 1 ? (win ? glass : mixtum(x, y, z)) : Blocks.AIR.defaultBlockState());
				}
			}
		}
		b.helmet(cx, y0 + drumH, cz, helm, st(Blocks.SMOOTH_STONE), st(Blocks.IRON_CHAIN));
	}

	// =====================================================================================================================
	// The Golden Gate of Kyiv
	// =====================================================================================================================

	/**
	 * The Golden Gate of Yaroslav's Kyiv, front (+z) = west, outside the town: a stone and brick gate tower with a 7.5 m
	 * arch, battlements, the gilded gate leaves, the little Church of the Annunciation with a gilded dome on top, and stubs
	 * of the earth rampart with its timber wall on both sides. A stair on the town side leads up to the church.
	 */
	private static void goldenGate(Bp b) {
		BlockState smooth = st(Blocks.SMOOTH_STONE);
		// The tower (x 14..33, z 2..27) with the passage through it (x 20..27).
		for (int x = 14; x <= 33; x++) {
			for (int z = 2; z <= 27; z++) {
				boolean passage = x >= 20 && x <= 27;
				double ax = Math.abs(x + 0.5 - 24.0);
				b.set(x, 0, z, passage ? st(Blocks.COBBLESTONE) : st(Blocks.STONE_BRICKS));
				b.set(x, -1, z, Blocks.STONE_BRICKS);
				for (int y = 1; y <= 12; y++) {
					boolean open = passage && (y <= 8 || y == 9 && ax < 3.2 || y == 10 && ax < 2.2);
					b.set(x, y, z, open ? Blocks.AIR.defaultBlockState() : mixtum(x, y, z));
				}
				boolean edge = x == 14 || x == 33 || z == 2 || z == 27;
				if (edge && (x + z) % 2 == 0) {
					b.set(x, 13, z, mixtum(x, 13, z));
				}
			}
		}
		// The gilded gate leaves standing open against the passage walls.
		for (int z = 22; z <= 25; z++) {
			for (int y = 1; y <= 8; y++) {
				BlockState leaf = y == 4 ? st(Blocks.GOLD_BLOCK) : st(Blocks.DARK_OAK_PLANKS);
				b.set(20, y, z, leaf);
				b.set(27, y, z, leaf);
			}
		}
		// The Church of the Annunciation on the top of the tower.
		for (int x = 18; x <= 29; x++) {
			for (int z = 7; z <= 20; z++) {
				boolean wall = x == 18 || x == 29 || z == 7 || z == 20;
				b.set(x, 12, z, smooth);
				for (int y = 13; y <= 20; y++) {
					if (wall) {
						b.set(x, y, z, mixtum(x, y, z));
					} else {
						b.air(x, y, z);
					}
				}
				b.set(x, 21, z, Bp.slab(Blocks.SMOOTH_STONE_SLAB, false));
			}
		}
		for (int x = 19; x <= 28; x++) {
			for (int z = 3; z <= 6; z++) {
				double d = Math.hypot(x + 0.5 - 24.0, z + 0.5 - 7.0);
				if (d < 3.0) {
					for (int y = 13; y <= 18; y++) {
						b.air(x, y, z);
					}
					b.set(x, 12, z, smooth);
				} else if (d < 4.2) {
					for (int y = 13; y <= 18; y++) {
						b.set(x, y, z, mixtum(x, y, z));
					}
					b.set(x, 19, z, Bp.slab(Blocks.SMOOTH_STONE_SLAB, false));
				}
			}
		}
		for (int x = 22; x <= 25; x++) {
			for (int y = 13; y <= 17; y++) {
				b.air(x, y, 7);
			}
		}
		for (int y = 13; y <= 14; y++) {
			b.air(23, y, 20);
			b.air(24, y, 20);
		}
		for (int z : new int[]{10, 14, 17}) {
			b.set(18, 17, z, st(Blocks.GLASS_PANE));
			b.set(29, 17, z, st(Blocks.GLASS_PANE));
		}
		b.set(23, 13, 5, Blocks.POLISHED_DIORITE);
		b.set(24, 13, 5, Blocks.POLISHED_DIORITE);
		b.set(23, 14, 5, candle());
		for (int x = 21; x <= 26; x++) {
			b.set(x, 13, 6, Blocks.SPRUCE_PLANKS);
			b.set(x, 14, 6, x % 2 == 0 ? st(Blocks.GOLD_BLOCK) : dyed(DyeColor.BLUE));
		}
		b.set(23, 20, 13, Bp.lantern(true));
		// Drum and gilded dome.
		for (int x = 20; x <= 26; x++) {
			for (int z = 10; z <= 16; z++) {
				double d = Math.hypot(x - 23, z - 13);
				if (d <= 2.6) {
					for (int y = 21; y <= 24; y++) {
						b.set(x, y, z, d > 1.6 ? (y == 22 && (x == 23 || z == 13) ? st(Blocks.GLASS_PANE) : mixtum(x, y, z)) : Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
		b.helmet(23, 25, 13, 2, st(Blocks.GOLD_BLOCK), st(Blocks.IRON_CHAIN));
		b.cross(23, 33, 13);
		// The stair on the town side (east) up to the top of the tower.
		for (int i = 0; i < 12; i++) {
			int x = 2 + i;
			for (int z = 0; z <= 1; z++) {
				b.set(x, 1 + i, z, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false));
				for (int y = 1; y <= i; y++) {
					b.set(x, y, z, mixtum(x, y, z));
				}
			}
		}
		for (int x = 14; x <= 15; x++) {
			for (int z = 0; z <= 1; z++) {
				for (int y = 1; y <= 12; y++) {
					b.set(x, y, z, mixtum(x, y, z));
				}
			}
			b.air(x, 13, 2);
		}
		// Stubs of the town rampart on both sides: an earth bank with a timber wall (gorodni) on the crest.
		for (int x = 0; x <= 47; x++) {
			if (x >= 14 && x <= 33) {
				continue;
			}
			for (int z = 3; z <= 26; z++) {
				double u = (z + 0.5 - 15.0) / 11.5;
				int hh = (int) Math.round(10 * (1 - u * u));
				for (int y = 1; y <= hh; y++) {
					b.set(x, y, z, y == hh ? st(Blocks.GRASS_BLOCK) : st(Blocks.DIRT));
				}
				if (Math.abs(z + 0.5 - 15.0) <= 1.5 && hh >= 9) {
					for (int y = hh + 1; y <= hh + 4; y++) {
						b.set(x, y, z, Bp.log(Blocks.SPRUCE_LOG, (y + x) % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z));
					}
					b.set(x, hh + 5, z, Bp.slab(Blocks.SPRUCE_SLAB, false));
				}
			}
		}
		// The ditch in front (west), crossed by a causeway at the gate.
		for (int x = 0; x <= 47; x++) {
			if (x >= 19 && x <= 28) {
				continue;
			}
			for (int z = 28; z <= 31; z++) {
				b.set(x, 0, z, Blocks.AIR.defaultBlockState());
				b.set(x, -1, z, Blocks.AIR.defaultBlockState());
				b.set(x, -2, z, z == 28 || z == 31 ? st(Blocks.GRASS_BLOCK) : st(Blocks.MUD));
				if (z == 28 || z == 31) {
					b.set(x, -2, z, Blocks.AIR.defaultBlockState());
					b.set(x, -1, z, st(Blocks.GRASS_BLOCK));
				}
			}
		}
	}

	// =====================================================================================================================
	// The Great Mosque of Córdoba
	// =====================================================================================================================

	/**
	 * The Great Mosque of Córdoba after al-Mansur's enlargement (987), front (+z) = north: the courtyard of trees with its
	 * channels, fountains and porticoes and the minaret of Abd al-Rahman III; the prayer hall of 19 aisles with its forest of
	 * columns carrying two tiers of arches of red brick and white stone, a gable roof over every aisle, and on the qibla
	 * wall (south, z 4..5) the mihrab of al-Hakam II with its gold and glazed decoration and the domed maqsura in front of it.
	 */
	private static void mezquita(Bp b) {
		final int W = 128, zQ = 4, zH = 121, zN = 177;
		BlockState sand = st(Blocks.SMOOTH_SANDSTONE), brick = st(Blocks.BRICKS), planks = st(Blocks.DARK_OAK_PLANKS);
		int[] line = new int[18];
		for (int i = 1; i <= 18; i++) {
			line[i - 1] = 1 + (int) Math.round(i * 126.0 / 19);
		}
		// The mihrab aisle: al-Hakam II's, west of the middle since al-Mansur enlarged the hall on the east (local x = 0).
		final int mX = (line[12] + line[13]) / 2;
		// Floors.
		for (int x = 0; x < W; x++) {
			for (int z = 0; z <= zN; z++) {
				b.set(x, -1, z, Blocks.STONE_BRICKS);
				boolean hall = z >= zQ && z <= zH;
				b.set(x, 0, z, hall ? (Math.floorMod(x + 2 * z, 7) == 0 ? sand : st(Blocks.TERRACOTTA))
						: (Math.floorMod(x + z, 5) == 0 ? brick : sand));
			}
		}
		// Outer walls with crenellations.
		for (int z = zQ; z <= zN; z++) {
			for (int x : new int[]{0, 1, W - 2, W - 1}) {
				for (int y = 1; y <= 11; y++) {
					b.set(x, y, z, Math.floorMod(y, 4) == 0 ? brick : sand);
				}
				if ((x == 0 || x == W - 1) && Math.floorMod(z, 3) == 0) {
					b.set(x, 12, z, Blocks.CUT_SANDSTONE);
				}
			}
		}
		for (int x = 0; x < W; x++) {
			for (int z : new int[]{zQ, zQ + 1, zN - 1, zN}) {
				for (int y = 1; y <= 11; y++) {
					b.set(x, y, z, Math.floorMod(y, 4) == 0 ? brick : sand);
				}
				if ((z == zQ || z == zN) && Math.floorMod(x, 3) == 0) {
					b.set(x, 12, z, Blocks.CUT_SANDSTONE);
				}
			}
		}
		// Doorways in the side walls, with red and white arches over them.
		for (int zc : new int[]{20, 45, 70, 95, 140, 160}) {
			for (int x : new int[]{0, 1, W - 2, W - 1}) {
				for (int z = zc - 1; z <= zc + 1; z++) {
					for (int y = 1; y <= 5; y++) {
						b.air(x, y, z);
					}
					b.set(x, 6, z, Math.floorMod(z, 2) == 0 ? brick : sand);
					b.set(x, 7, z, Math.floorMod(z, 2) == 0 ? sand : brick);
				}
				b.air(x, 6, zc);
				b.set(x, 6, zc - 2, Blocks.CHISELED_SANDSTONE);
				b.set(x, 6, zc + 2, Blocks.CHISELED_SANDSTONE);
			}
		}
		// The prayer hall: lines of columns with double arches of red brick and white stone voussoirs.
		Block[] shafts = {Blocks.POLISHED_GRANITE, Blocks.POLISHED_DIORITE, Blocks.POLISHED_ANDESITE, Blocks.CALCITE, Blocks.POLISHED_GRANITE};
		for (int li = 0; li < line.length; li++) {
			int x = line[li];
			for (int z = zQ + 2; z <= zH - 2; z++) {
				int k = Math.floorMod(z - (zQ + 3), 3);
				boolean col = k == 0;
				boolean red = Math.floorMod(z + li, 2) == 0;
				if (col) {
					BlockState shaft = st(shafts[(int) (h(x, 0, z, 6) * shafts.length)]);
					for (int y = 1; y <= 3; y++) {
						b.set(x, y, z, shaft);
					}
					b.set(x, 4, z, Blocks.CHISELED_SANDSTONE);
					for (int y = 5; y <= 8; y++) {
						b.set(x, y, z, sand);
					}
					b.set(x, 9, z, Blocks.CHISELED_SANDSTONE);
					b.set(x, 10, z, red ? brick : sand);
				} else {
					Direction toCol = k == 1 ? Direction.NORTH : Direction.SOUTH;
					Block stair = red ? Blocks.BRICK_STAIRS : Blocks.SMOOTH_SANDSTONE_STAIRS;
					b.set(x, 5, z, arch(stair, toCol));
					b.set(x, 6, z, red ? brick : sand);
					b.set(x, 9, z, arch(stair, toCol));
					b.set(x, 10, z, red ? brick : sand);
				}
			}
		}
		// The wooden ceiling, hanging lamps, and a gable roof over each aisle.
		int[] edges = new int[line.length + 2];
		edges[0] = 1;
		System.arraycopy(line, 0, edges, 1, line.length);
		edges[edges.length - 1] = W - 2;
		for (int x = 2; x <= W - 3; x++) {
			for (int z = zQ + 2; z <= zH - 2; z++) {
				b.set(x, 11, z, planks);
			}
		}
		for (int a = 0; a + 1 < edges.length; a++) {
			int x0 = edges[a], x1 = edges[a + 1];
			int mid = (x0 + x1) / 2;
			b.gableRoof(x0, x1, zQ, zH, 12, false, 0, Blocks.BRICK_STAIRS, Blocks.BRICK_SLAB, brick, sand);
			for (int z = zQ + 6; z <= zH - 4; z += 9) {
				b.set(mid, 10, z, Blocks.IRON_CHAIN);
				b.set(mid, 9, z, Bp.lantern(true));
			}
		}
		// The courtyard side of the hall: one great arch into every aisle.
		for (int x = 2; x <= W - 3; x++) {
			for (int z = zH - 1; z <= zH; z++) {
				for (int y = 1; y <= 11; y++) {
					b.set(x, y, z, Math.floorMod(y, 4) == 0 ? brick : sand);
				}
			}
		}
		for (int a = 0; a + 1 < edges.length; a++) {
			int x0 = edges[a] + 1, x1 = edges[a + 1] - 1;
			double c = (x0 + x1 + 1) / 2.0;
			for (int x = x0; x <= x1; x++) {
				double ax = Math.abs(x + 0.5 - c);
				for (int z = zH - 1; z <= zH; z++) {
					for (int y = 1; y <= 7; y++) {
						if (y <= 5 && ax < 2.6 || y == 6 && ax < 2.0 || y == 7 && ax < 1.0) {
							b.air(x, y, z);
						} else if (y >= 6 && ax < 3.4) {
							b.set(x, y, z, Math.floorMod(x, 2) == 0 ? brick : sand);
						}
					}
				}
			}
		}
		// The maqsura before the mihrab: a screen of lobed arches, carpets, and three ribbed domes rising over the roof.
		for (int x = mX - 10; x <= mX + 10; x++) {
			for (int z = zQ + 2; z <= zQ + 11; z++) {
				b.set(x, 0, z, Blocks.POLISHED_DIORITE);
				if (b.get(x, 1, z) == null) {
					b.set(x, 1, z, Blocks.CARPET.pick(Math.floorMod(z, 2) == 0 ? DyeColor.RED : DyeColor.GREEN));
				}
			}
			int zs = zQ + 12;
			boolean col = Math.floorMod(x - mX, 2) == 0;
			b.set(x, 1, zs, col ? pillar() : Blocks.AIR.defaultBlockState());
			b.set(x, 2, zs, col ? pillar() : Blocks.AIR.defaultBlockState());
			b.set(x, 3, zs, col ? pillar() : Bp.stairs(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.EAST, true));
			b.set(x, 4, zs, Math.floorMod(x, 2) == 0 ? st(Blocks.GOLD_BLOCK) : st(Blocks.CHISELED_SANDSTONE));
			b.set(x, 5, zs, Math.floorMod(x, 2) == 0 ? brick : sand);
		}
		for (int y = 1; y <= 3; y++) {
			b.air(mX, y, zQ + 12);
			b.air(mX + 1, y, zQ + 12);
			b.air(mX - 1, y, zQ + 12);
		}
		for (int di = 11; di <= 13; di++) {
			double dcx = (line[di] + line[di + 1]) / 2.0 + 0.5, dcz = zQ + 7.5;
			for (int x = (int) Math.floor(dcx) - 5; x <= (int) Math.floor(dcx) + 5; x++) {
				for (int z = zQ + 3; z <= zQ + 12; z++) {
					double d = Math.hypot(x + 0.5 - dcx, z + 0.5 - dcz);
					if (d >= 3.5) {
						continue;
					}
					for (int y = 11; y <= 16; y++) {
						b.air(x, y, z);
					}
					if (d >= 2.6) {
						for (int y = 12; y <= 15; y++) {
							boolean win = y == 14 && (Math.abs(x + 0.5 - dcx) < 0.6 || Math.abs(z + 0.5 - dcz) < 0.6);
							b.set(x, y, z, win ? st(Blocks.IRON_BARS) : sand);
						}
					}
				}
			}
			sphere(b, dcx, 16, dcz, 2.6, 1.0, 1.0, (x, y, z) -> sand, Monuments::gold, 0.9, null);
		}
		// The mihrab: a horseshoe arch framed by gold and glazed panels, opening into its small octagonal room.
		for (int x = mX - 6; x <= mX + 6; x++) {
			for (int y = 1; y <= 10; y++) {
				double ax = Math.abs(x + 0.5 - (mX + 0.5));
				boolean open = ax < 1.6 && y <= 4 || ax < 1.0 && y == 5;
				BlockState s;
				if (open) {
					s = Blocks.AIR.defaultBlockState();
				} else if (ax < 2.6 && y <= 6) {
					s = Math.floorMod(x + y, 2) == 0 ? st(Blocks.GOLD_BLOCK) : glazed(DyeColor.GREEN);
				} else if (y == 9) {
					s = st(Blocks.GOLD_BLOCK);
				} else if (ax < 4.6 && y <= 8) {
					s = h(x, y, zQ + 1, 7) < 0.5 ? glazed(DyeColor.BLUE) : glazed(DyeColor.CYAN);
				} else {
					s = y <= 8 && (x == mX - 6 || x == mX + 6) ? glazed(DyeColor.RED) : sand;
				}
				b.set(x, y, zQ + 1, s);
				if (open) {
					b.air(x, y, zQ);
				}
			}
		}
		for (int x = mX - 2; x <= mX + 2; x++) {
			for (int z = 0; z <= zQ - 1; z++) {
				boolean wall = x == mX - 2 || x == mX + 2 || z == 0;
				for (int y = 1; y <= 6; y++) {
					b.set(x, y, z, wall ? (y <= 4 ? gold(x, y, z) : sand) : Blocks.AIR.defaultBlockState());
				}
				b.set(x, 0, z, Blocks.POLISHED_DIORITE);
				b.set(x, 7, z, Bp.slab(Blocks.SMOOTH_SANDSTONE_SLAB, false));
			}
		}
		b.set(mX, 6, 2, Bp.lantern(true));
		// The minbar beside the mihrab.
		for (int i = 0; i < 3; i++) {
			b.set(mX + 3, 1 + i, zQ + 5 - i, Bp.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
			for (int y = 1; y <= i; y++) {
				b.set(mX + 3, y, zQ + 5 - i, Blocks.DARK_OAK_PLANKS);
			}
		}
		b.fill(mX + 3, 1, zQ + 2, mX + 3, 3, zQ + 2, planks);
		b.set(mX + 2, 6, zQ + 2, Blocks.DARK_OAK_SLAB);
		b.set(mX + 4, 6, zQ + 2, Blocks.DARK_OAK_SLAB);
		b.set(mX + 2, 4, zQ + 2, Blocks.DARK_OAK_FENCE);
		b.set(mX + 4, 4, zQ + 2, Blocks.DARK_OAK_FENCE);
		b.set(mX + 2, 5, zQ + 2, Blocks.DARK_OAK_FENCE);
		b.set(mX + 4, 5, zQ + 2, Blocks.DARK_OAK_FENCE);
		b.set(mX + 3, 6, zQ + 2, Blocks.DARK_OAK_PLANKS);
		// The courtyard: porticoes on three sides, rows of trees in line with the arcades, water channels and fountains.
		for (int x = 2; x <= W - 3; x++) {
			for (int z = zH + 1; z <= zN - 2; z++) {
				boolean portico = x <= 8 || x >= W - 9 || z >= zN - 8;
				boolean colonnade = (x == 8 || x == W - 9) && z <= zN - 8 || z == zN - 8 && x >= 8 && x <= W - 9;
				if (colonnade) {
					int c = x == 8 || x == W - 9 ? z : x;
					if (Math.floorMod(c, 3) == 0) {
						for (int y = 1; y <= 5; y++) {
							b.set(x, y, z, Math.floorMod(c, 6) == 0 ? st(Blocks.POLISHED_GRANITE) : st(Blocks.POLISHED_DIORITE));
						}
					} else {
						b.set(x, 6, z, arch(Math.floorMod(c, 2) == 0 ? Blocks.BRICK_STAIRS : Blocks.SMOOTH_SANDSTONE_STAIRS,
								x == 8 || x == W - 9 ? (Math.floorMod(c, 3) == 1 ? Direction.NORTH : Direction.SOUTH)
										: (Math.floorMod(c, 3) == 1 ? Direction.WEST : Direction.EAST)));
					}
					b.set(x, 7, z, Math.floorMod(c, 2) == 0 ? brick : sand);
				}
				if (portico) {
					b.set(x, 8, z, brick);
					b.set(x, 9, z, Bp.slab(Blocks.BRICK_SLAB, false));
				}
			}
		}
		for (int li = 0; li < line.length; li += 2) {
			int x = line[li];
			if (x <= 9 || x >= W - 10) {
				continue;
			}
			for (int z = zH + 4; z <= zN - 11; z++) {
				b.set(x + 1, 0, z, Blocks.WATER);
			}
			for (int z = zH + 6; z <= zN - 13; z += 6) {
				orangeTree(b, x, z);
			}
		}
		for (int fx : new int[]{41, 81}) {
			for (int x = fx - 2; x <= fx + 2; x++) {
				for (int z = 145; z <= 149; z++) {
					boolean rim = x == fx - 2 || x == fx + 2 || z == 145 || z == 149;
					b.set(x, 1, z, rim ? st(Blocks.POLISHED_DIORITE) : st(Blocks.WATER));
				}
			}
		}
		// The minaret of Abd al-Rahman III on the north side of the courtyard.
		final int m0 = 60, m1 = 67, n0 = zN - 7;
		for (int x = m0; x <= m1; x++) {
			for (int z = n0; z <= zN; z++) {
				boolean wall = x == m0 || x == m1 || z == n0 || z == zN;
				for (int y = 1; y <= 34; y++) {
					if (wall) {
						b.set(x, y, z, Math.floorMod(y, 5) == 0 ? brick : sand);
					} else {
						b.air(x, y, z);
					}
				}
				b.set(x, 0, z, sand);
			}
		}
		for (int y = 1; y <= 35; y++) {
			b.set(m0 + 4, y, n0 + 1, Bp.facing(Blocks.LADDER, Direction.SOUTH));
		}
		b.air(m0 + 2, 1, n0);
		b.air(m0 + 2, 2, n0);
		b.air(m0 + 3, 1, n0);
		b.air(m0 + 3, 2, n0);
		for (int yw : new int[]{12, 20, 28}) {
			for (int y = yw; y <= yw + 2; y++) {
				for (int o : new int[]{2, 5}) {
					b.air(m0 + o, y, n0);
					b.air(m0 + o, y, zN);
					b.air(m0, y, n0 + o);
					b.air(m1, y, n0 + o);
				}
			}
		}
		for (int x = m0 - 1; x <= m1 + 1; x++) {
			for (int z = n0 - 1; z <= zN + 1; z++) {
				if (x == m0 + 4 && z == n0 + 1) {
					continue;
				}
				b.set(x, 35, z, sand);
				boolean rim = x == m0 - 1 || x == m1 + 1 || z == n0 - 1 || z == zN + 1;
				if (rim && Math.floorMod(x + z, 2) == 0) {
					b.set(x, 36, z, Blocks.CUT_SANDSTONE);
				}
			}
		}
		for (int x = m0 + 2; x <= m0 + 5; x++) {
			for (int z = n0 + 2; z <= n0 + 5; z++) {
				for (int y = 36; y <= 42; y++) {
					boolean win = y >= 38 && y <= 40 && (x == m0 + 3 || z == n0 + 3);
					b.set(x, y, z, win ? Blocks.AIR.defaultBlockState() : sand);
				}
			}
		}
		sphere(b, m0 + 4.0, 43, n0 + 4.0, 1.4, 1.0, 1.0, (x, y, z) -> sand, null, 0, null);
		b.set(m0 + 3, 45, n0 + 3, Blocks.GOLD_BLOCK);
		b.set(m0 + 3, 46, n0 + 3, Blocks.IRON_CHAIN);
		b.set(m0 + 3, 47, n0 + 3, Blocks.GOLD_BLOCK);
		b.set(m0 + 3, 48, n0 + 3, Blocks.IRON_CHAIN);
		b.set(m0 + 3, 49, n0 + 3, Blocks.GOLD_BLOCK);
		// The north gate of the courtyard.
		for (int x = 40; x <= 42; x++) {
			for (int y = 1; y <= 6; y++) {
				b.air(x, y, zN);
				b.air(x, y, zN - 1);
			}
		}
	}

	private static void orangeTree(Bp b, int x, int z) {
		BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
		b.set(x, 0, z, Blocks.DIRT);
		for (int y = 1; y <= 2; y++) {
			b.set(x, y, z, Bp.log(Blocks.OAK_LOG, Direction.Axis.Y));
		}
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 3; dy <= 5; dy++) {
					double d = Math.sqrt(dx * dx + dz * dz + (dy - 4) * (dy - 4) * 1.5);
					if (d <= 2.3 && b.get(x + dx, dy, z + dz) == null) {
						b.set(x + dx, dy, z + dz, leaves);
					}
				}
			}
		}
		b.set(x, 3, z, Bp.log(Blocks.OAK_LOG, Direction.Axis.Y));
	}

	// =====================================================================================================================
	// The Dome of the Rock (Jerusalem)
	// =====================================================================================================================

	/**
	 * The Dome of the Rock on its raised platform: the octagon (20.6 m a side), white marble below and blue and green
	 * glazed tiles above with grilled windows, four doors with porches, the inner octagonal arcade and the circular arcade
	 * round the Rock with its cave, the drum and the golden dome; the little Dome of the Chain to the east.
	 * Centre at (50, 50); drawn symmetrical (front +z = south).
	 */
	private static void domeOfRock(Bp b) {
		final double c = 50.0, A = 24.8;
		final int P = 3; // platform top
		BlockState sand = st(Blocks.SMOOTH_SANDSTONE), calcite = st(Blocks.CALCITE), smooth = st(Blocks.SMOOTH_STONE);
		// The paved area and the raised upper platform with its stairways and arcades (qanatir).
		for (int x = 0; x < 100; x++) {
			for (int z = 0; z < 100; z++) {
				double dx = x + 0.5 - c, dz = z + 0.5 - c;
				double m = Math.max(Math.abs(dx), Math.abs(dz));
				b.set(x, 0, z, Math.floorMod(x, 8) == 0 || Math.floorMod(z, 8) == 0 ? st(Blocks.CUT_SANDSTONE) : sand);
				b.set(x, -1, z, Blocks.STONE_BRICKS);
				if (m < 38) {
					for (int y = 1; y <= P; y++) {
						b.set(x, y, z, y == P ? (Math.floorMod(x + z, 2) == 0 ? sand : st(Blocks.SANDSTONE)) : st(Blocks.SANDSTONE));
					}
				}
			}
		}
		// Stairways: north, south and west in the middle, east off to the side; an arcade at the top of each.
		int[][] stairs = {{0, 1, 0}, {0, -1, 0}, {-1, 0, 0}, {1, 0, 20}}; // direction x, z, offset along the side
		for (int[] s : stairs) {
			for (int w = -5; w <= 5; w++) {
				for (int i = 0; i < 3; i++) {
					int along = s[2] + w;
					int dOut = 35 + i; // the stairs cut into the platform edge
					int x = s[0] == 0 ? 50 + along : cell(s[0], dOut);
					int z = s[1] == 0 ? 50 + along : cell(s[1], dOut);
					Direction up = s[0] > 0 ? Direction.WEST : s[0] < 0 ? Direction.EAST : s[1] > 0 ? Direction.NORTH : Direction.SOUTH;
					for (int y = P - i + 1; y <= P; y++) {
						b.air(x, y, z);
					}
					b.set(x, P - i, z, Bp.stairs(Blocks.SANDSTONE_STAIRS, up, false));
				}
				int along = s[2] + w;
				int x = s[0] == 0 ? 50 + along : cell(s[0], 33);
				int z = s[1] == 0 ? 50 + along : cell(s[1], 33);
				boolean col = Math.floorMod(w + 5, 3) == 0;
				for (int y = P + 1; y <= P + 5; y++) {
					if (col) {
						b.set(x, y, z, pillar());
					}
				}
				if (!col) {
					Direction toCol = s[0] == 0 ? (Math.floorMod(w + 5, 3) == 1 ? Direction.WEST : Direction.EAST)
							: (Math.floorMod(w + 5, 3) == 1 ? Direction.NORTH : Direction.SOUTH);
					b.set(x, P + 6, z, arch(Blocks.SMOOTH_SANDSTONE_STAIRS, toCol));
				} else {
					b.set(x, P + 6, z, calcite);
				}
				b.set(x, P + 7, z, calcite);
			}
		}
		// The octagon.
		for (int x = 20; x <= 80; x++) {
			for (int z = 20; z <= 80; z++) {
				double dx = x + 0.5 - c, dz = z + 0.5 - c;
				double r = Math.hypot(dx, dz);
				double phi = Math.atan2(dz, dx);
				int sector = (int) Math.round(phi / (Math.PI / 4));
				double th = sector * Math.PI / 4;
				double along = -dx * Math.sin(th) + dz * Math.cos(th);
				boolean inOuter = oct(dx, dz, A);
				if (!inOuter) {
					continue;
				}
				b.set(x, P, z, Math.floorMod(x + z, 2) == 0 ? st(Blocks.POLISHED_DIORITE) : calcite);
				if (!oct(dx, dz, A - 1.6)) {
					// The outer wall.
					boolean door = sector % 2 == 0 && Math.abs(along) < 2;
					for (int y = P + 1; y <= P + 13; y++) {
						int hgt = y - P;
						BlockState s;
						if (door && hgt <= 6) {
							s = Blocks.AIR.defaultBlockState();
						} else if (hgt <= 6) {
							s = Math.floorMod(hgt, 3) == 0 ? st(Blocks.POLISHED_DIORITE) : calcite;
						} else if (hgt == 13) {
							s = Math.floorMod((int) Math.floor(along), 2) == 0 ? st(Blocks.GOLD_BLOCK) : glazed(DyeColor.LIGHT_BLUE);
						} else {
							double fr = (along + 10.25) / 2.93;
							fr -= Math.floor(fr);
							boolean win = hgt >= 8 && hgt <= 10 && Math.abs(along) < 9.5 && fr > 0.3 && fr < 0.7;
							double rr = h(x, y, z, 8);
							s = win ? st(Blocks.IRON_BARS) : rr < 0.4 ? glazed(DyeColor.BLUE) : rr < 0.7 ? glazed(DyeColor.CYAN) : glazed(DyeColor.LIGHT_BLUE);
						}
						b.set(x, y, z, s);
					}
					b.set(x, P + 14, z, calcite);
					continue;
				}
				// The inner octagonal arcade: piers at the corners, two columns a side.
				if (oct(dx, dz, 17.6) && !oct(dx, dz, 16.6)) {
					double aa = Math.abs(along);
					boolean pier = aa > 5.5;
					boolean colm = aa >= 1.8 && aa < 2.8;
					for (int y = P + 1; y <= P + 10; y++) {
						int hgt = y - P;
						BlockState s;
						if (hgt >= 7) {
							s = hgt == 7 ? calcite : gold(x, y, z);
						} else if (pier) {
							s = Math.floorMod(hgt, 3) == 0 ? st(Blocks.POLISHED_DIORITE) : calcite;
						} else if (colm) {
							s = hgt == 6 ? st(Blocks.CHISELED_QUARTZ_BLOCK) : pillar();
						} else {
							s = Blocks.AIR.defaultBlockState();
						}
						b.set(x, y, z, s);
					}
				}
				// The circular arcade round the Rock: four piers, twelve columns; the drum rises from it.
				if (r >= 10.3 && r < 11.5) {
					boolean pier = false, colm = false;
					for (int k = 0; k < 4; k++) {
						if (Math.abs(Math.IEEEremainder(phi - (Math.PI / 4 + k * Math.PI / 2), 2 * Math.PI)) * r < 1.8) {
							pier = true;
						}
						for (double o : new double[]{-0.42, 0, 0.42}) {
							if (Math.abs(Math.IEEEremainder(phi - (k * Math.PI / 2 + o), 2 * Math.PI)) * r < 0.6) {
								colm = true;
							}
						}
					}
					for (int y = P + 1; y <= P + 21; y++) {
						int hgt = y - P;
						BlockState s;
						if (hgt <= 6) {
							s = pier ? calcite : colm ? (hgt == 6 ? st(Blocks.CHISELED_QUARTZ_BLOCK) : pillar()) : Blocks.AIR.defaultBlockState();
						} else if (hgt == 7) {
							s = calcite;
						} else {
							double a16 = phi / (2 * Math.PI) * 16;
							double fr = a16 - Math.floor(a16);
							boolean win = hgt >= 14 && hgt <= 17 && fr > 0.35 && fr < 0.65;
							s = win ? Blocks.STAINED_GLASS_PANE.pick(Math.floorMod((int) Math.floor(a16), 2) == 0 ? DyeColor.YELLOW : DyeColor.BLUE).defaultBlockState()
									: r < 10.9 ? gold(x, y, z) : glazed(h(x, y, z, 9) < 0.5 ? DyeColor.BLUE : DyeColor.CYAN);
						}
						b.set(x, y, z, s);
					}
				}
				// Painted wooden ceilings over the ambulatories and the low lead roof above them.
				if (r >= 11.5) {
					b.set(x, P + 11, z, Blocks.DARK_OAK_PLANKS);
					int ry = P + 13 + (int) Math.floor((A - 1.6 - octR(dx, dz)) / 4.5);
					b.set(x, ry, z, smooth);
					if (h(x, 0, z, 10) < 0.25 && !b.has(x, P + 1, z)) {
						b.set(x, P + 1, z, Blocks.CARPET.pick(DyeColor.RED));
					}
				}
			}
		}
		// Hanging lamps in the ambulatories.
		for (int k = 0; k < 16; k++) {
			double a = k * Math.PI / 8;
			for (double rr : new double[]{14, 21}) {
				int x = (int) Math.floor(c + rr * Math.cos(a)), z = (int) Math.floor(c + rr * Math.sin(a));
				b.set(x, P + 10, z, Bp.lantern(true));
			}
		}
		// Porches before the four doors.
		for (int k = 0; k < 4; k++) {
			int sx = k == 0 ? 1 : k == 1 ? -1 : 0, sz = k == 2 ? 1 : k == 3 ? -1 : 0;
			for (int along = -4; along <= 4; along++) {
				for (int out = 0; out < 3; out++) {
					int d = 25 + out;
					int x = sx == 0 ? 50 + along : cell(sx, d);
					int z = sz == 0 ? 50 + along : cell(sz, d);
					boolean corner = Math.abs(along) == 4 && out == 2;
					for (int y = P + 1; y <= P + 7; y++) {
						if (corner) {
							b.set(x, y, z, pillar());
						}
					}
					b.set(x, P + 8, z, Bp.slab(Blocks.SMOOTH_STONE_SLAB, false));
					b.set(x, P, z, st(Blocks.POLISHED_DIORITE));
				}
			}
		}
		// The drum's top and the golden dome, a little pointed, with a crescent finial.
		sphere(b, c, P + 22, c, 10.0, 1.2, 1.15, (x, y, z) -> st(Blocks.GOLD_BLOCK), (x, y, z) -> h(x, y, z, 11) < 0.3 ? dyed(DyeColor.GREEN) : gold(x, y, z), 1.0, null);
		int top = P + 22 + (int) Math.ceil(11.2 * 1.15);
		b.set(49, top + 1, 49, Blocks.GOLD_BLOCK);
		b.set(49, top + 2, 49, Blocks.IRON_CHAIN);
		b.set(49, top + 3, 49, Blocks.GOLD_BLOCK);
		for (int y = P + 5; y <= top - 2; y++) {
			b.set(49, y, 49, Blocks.IRON_CHAIN);
		}
		b.set(49, P + 4, 49, Bp.lantern(true));
		// The Rock, and the cave beneath it reached by steps.
		for (int x = 40; x <= 60; x++) {
			for (int z = 40; z <= 60; z++) {
				double dx = (x + 0.5 - c) / 7.0, dz = (z + 0.5 - (c - 1)) / 5.5;
				double hh = 2.8 * (1 - dx * dx - dz * dz) + (h(x, 0, z, 12) - 0.5) * 0.8;
				int n = (int) Math.round(hh);
				for (int y = P + 1; y <= P + n; y++) {
					double rr = h(x, y, z, 13);
					b.set(x, y, z, rr < 0.5 ? st(Blocks.STONE) : rr < 0.8 ? st(Blocks.ANDESITE) : st(Blocks.DIORITE));
				}
			}
		}
		for (int x = 48; x <= 52; x++) {
			for (int z = 48; z <= 52; z++) {
				for (int y = 1; y <= P; y++) {
					b.air(x, y, z);
				}
				b.set(x, 0, z, Blocks.STONE);
			}
		}
		for (int i = 0; i < 3; i++) {
			int x = 53 + i;
			for (int z = 50; z <= 51; z++) {
				b.set(x, i + 1, z, Bp.stairs(Blocks.STONE_STAIRS, Direction.EAST, false));
				for (int y = i + 2; y <= i + 4; y++) {
					b.air(x, y, z);
				}
				for (int y = 0; y <= i; y++) {
					b.set(x, y, z, Blocks.STONE);
				}
			}
		}
		b.set(50, 1, 48, Bp.lantern(false));
		// The Dome of the Chain, east of the Dome of the Rock.
		double kx = c + 33, kz = c;
		for (int x = (int) kx - 6; x <= (int) kx + 6; x++) {
			for (int z = (int) kz - 6; z <= (int) kz + 6; z++) {
				double dx = x + 0.5 - kx, dz = z + 0.5 - kz;
				double r = Math.hypot(dx, dz);
				double phi = Math.atan2(dz, dx);
				if (r < 5.2) {
					b.set(x, P, z, st(Blocks.POLISHED_DIORITE));
					b.set(x, P + 6, z, smooth);
				}
				boolean outerCol = false, innerCol = false;
				for (int k = 0; k < 11; k++) {
					if (Math.abs(Math.IEEEremainder(phi - k * 2 * Math.PI / 11, 2 * Math.PI)) * r < 0.6 && r >= 4.2 && r < 5.0) {
						outerCol = true;
					}
				}
				for (int k = 0; k < 6; k++) {
					if (Math.abs(Math.IEEEremainder(phi - k * Math.PI / 3, 2 * Math.PI)) * r < 0.6 && r >= 1.6 && r < 2.4) {
						innerCol = true;
					}
				}
				if (outerCol || innerCol) {
					for (int y = P + 1; y <= P + 5; y++) {
						b.set(x, y, z, pillar());
					}
				}
				if (r >= 1.8 && r < 2.8) {
					b.set(x, P + 7, z, calcite);
				}
			}
		}
		sphere(b, kx, P + 8, kz, 2.2, 1.0, 1.0, (x, y, z) -> smooth, null, 0, null);
	}

	/** The cell k blocks out from the middle of the 100-wide Dome of the Rock plan, towards +s or -s (mirrored exactly). */
	private static int cell(int s, int k) {
		return s > 0 ? 50 + k : 49 - k;
	}

	/** Inside a regular octagon (flats facing the four directions) of apothem a. */
	private static boolean oct(double dx, double dz, double a) {
		double ax = Math.abs(dx), az = Math.abs(dz);
		return ax <= a && az <= a && ax + az <= a * 1.41421356;
	}

	/** The "octagonal radius" of a point: the apothem of the octagon through it. */
	private static double octR(double dx, double dz) {
		double ax = Math.abs(dx), az = Math.abs(dz);
		return Math.max(Math.max(ax, az), (ax + az) / 1.41421356);
	}

	// =====================================================================================================================
	// The Pantheon (Rome)
	// =====================================================================================================================

	/**
	 * The Pantheon, in 1050 the church of Santa Maria ad Martyres, front (+z) = the portico on the north: the rotunda
	 * (43.3 m across and high) with its niches and aediculae, the coffered dome with the 9 m oculus, lead on the outside;
	 * the transitional block with the bronze doors; the portico of sixteen granite columns under a low pediment.
	 */
	private static void pantheon(Bp b) {
		final double c = 31.0, Ri = 21.65, Ro = 27.65;
		final int yc = 22; // the dome springs here (centre of the inner sphere)
		BlockState smooth = st(Blocks.SMOOTH_STONE), calcite = st(Blocks.CALCITE), tuff = st(Blocks.TUFF);
		// The square in front and around: old paving.
		for (int x = 0; x < 62; x++) {
			for (int z = 0; z < 88; z++) {
				b.set(x, -1, z, Blocks.STONE_BRICKS);
				double rr = h(x, 0, z, 14);
				b.set(x, 0, z, rr < 0.5 ? st(Blocks.COBBLESTONE) : rr < 0.8 ? st(Blocks.STONE_BRICKS) : st(Blocks.GRAVEL));
			}
		}
		// The rotunda: floor, the 6 m thick wall with its niches, the dome.
		for (int x = 0; x < 62; x++) {
			for (int z = 0; z < 62; z++) {
				double dx = x + 0.5 - c, dz = z + 0.5 - c;
				double r = Math.hypot(dx, dz);
				if (r < Ri) {
					boolean circle = Math.floorMod(x / 3 + z / 3, 2) == 0;
					b.set(x, 0, z, circle ? st(Blocks.POLISHED_GRANITE) : st(Blocks.POLISHED_DIORITE));
				}
				if (r < Ri || r > Ro + 0.8) {
					continue;
				}
				double phi = Math.atan2(dz, dx);
				// Which niche: 8 directions; +z (phi = 90°) is the entrance, -z the main apse.
				int k = (int) Math.round(phi / (Math.PI / 4));
				double off = Math.abs(Math.IEEEremainder(phi - k * Math.PI / 4, 2 * Math.PI)) * Ri;
				int kk = Math.floorMod(k, 8);
				boolean entrance = kk == 2, apse = kk == 6;
				boolean semi = kk == 0 || kk == 4;
				double depth = apse ? 5.5 : 4.5;
				int nh = apse ? 15 : 12;
				for (int y = 1; y <= 29; y++) {
					BlockState s;
					if (r > Ro) {
						// Cornices round the outside.
						if (y == 10 || y == 20 || y == 29) {
							b.set(x, y, z, Bp.slab(Blocks.SMOOTH_STONE_SLAB, true));
						}
						continue;
					}
					boolean niche = !entrance && r < Ri + depth && off < 4.45 && (y <= nh || y == nh + 1 && off < (semi || apse ? 3.0 : 4.45));
					if (niche) {
						s = Blocks.AIR.defaultBlockState();
					} else if (r < Ri + 1) {
						if (y <= 13) {
							s = y == 13 ? st(Blocks.CHISELED_QUARTZ_BLOCK) : Math.floorMod(y, 6) == 0 ? st(Blocks.POLISHED_GRANITE) : st(Blocks.POLISHED_DIORITE);
						} else if (y <= 21) {
							double a32 = phi / (2 * Math.PI) * 32;
							s = y >= 15 && y <= 19 && a32 - Math.floor(a32) > 0.3 && a32 - Math.floor(a32) < 0.7 ? st(Blocks.POLISHED_GRANITE) : calcite;
						} else {
							s = calcite;
						}
					} else if (r > Ro - 1) {
						s = Math.floorMod(y, 10) == 0 ? smooth : st(Blocks.BRICKS);
					} else {
						s = tuff;
					}
					b.set(x, y, z, s);
				}
				// Columns in front of the niches.
				if (!entrance && !apse && r >= Ri && r < Ri + 1 && Math.abs(off - 2.2) < 0.6) {
					for (int y = 1; y <= 11; y++) {
						b.set(x, y, z, pillar());
					}
					b.set(x, 12, z, Blocks.CHISELED_QUARTZ_BLOCK);
				}
			}
		}
		// Aediculae (little shrines) between the niches, with candles.
		for (int k = 0; k < 8; k++) {
			double a = (k + 0.5) * Math.PI / 4;
			if (Math.abs(Math.IEEEremainder(a - Math.PI / 2, 2 * Math.PI)) < 0.5) {
				continue;
			}
			int x = (int) Math.floor(c + (Ri - 0.8) * Math.cos(a)), z = (int) Math.floor(c + (Ri - 0.8) * Math.sin(a));
			for (int y = 1; y <= 4; y++) {
				b.set(x, y, z, y == 4 ? st(Blocks.CHISELED_QUARTZ_BLOCK) : st(Blocks.POLISHED_GRANITE));
			}
			b.set(x, 5, z, candle());
		}
		// The altar in the main apse, with an icon of the Virgin.
		b.set(30, 1, 6, Blocks.POLISHED_GRANITE);
		b.set(31, 1, 6, Blocks.POLISHED_GRANITE);
		b.set(30, 2, 6, candle());
		b.set(31, 2, 6, candle());
		b.set(30, 4, 3, Blocks.GOLD_BLOCK);
		b.set(31, 4, 3, dyed(DyeColor.BLUE));
		b.set(31, 5, 3, Blocks.GOLD_BLOCK);
		b.set(30, 5, 3, Blocks.GOLD_BLOCK);
		// The dome: coffers inside, step-rings low on the outside, lead above, the oculus open to the sky.
		for (int y = yc; y <= yc + 24; y++) {
			double ri = y - yc < Ri ? Math.sqrt(Ri * Ri - (y - yc) * (y - yc)) : 0;
			double so = 27.15 * 27.15 - (y - (yc - 4)) * (y - (yc - 4));
			double outerSphere = so > 0 ? Math.sqrt(so) : 0;
			double step = y >= 30 && y <= 31 ? 26.2 : y >= 32 && y <= 33 ? 25.4 : y >= 34 && y <= 35 ? 24.6 : 0;
			double ro = y <= 29 ? Ro : Math.max(outerSphere, step);
			if (ro <= 4.5) {
				continue;
			}
			for (int x = 0; x < 62; x++) {
				for (int z = 0; z < 62; z++) {
					double dx = x + 0.5 - c, dz = z + 0.5 - c;
					double r = Math.hypot(dx, dz);
					if (r < ri || r > ro || r < 4.5) {
						continue;
					}
					BlockState s;
					if (r < ri + 1.2 && ri > 0) {
						double el = Math.toDegrees(Math.atan2(y - yc, r));
						double ring = (el - 8) / 11;
						double az = (Math.atan2(dz, dx) / (2 * Math.PI) + 1) * 28;
						double fa = az - Math.floor(az), fr = ring - Math.floor(ring);
						boolean coffer = ring >= 0 && ring < 5 && fr > 0.18 && fr < 0.82 && fa > 0.15 && fa < 0.85;
						s = coffer ? Blocks.AIR.defaultBlockState() : st(Blocks.STONE);
					} else if (r > ro - 1.2) {
						s = y <= 29 ? (Math.floorMod(y, 10) == 0 ? smooth : st(Blocks.BRICKS)) : smooth;
					} else {
						s = tuff;
					}
					b.set(x, y, z, s);
				}
			}
		}
		// The bronze ring round the oculus.
		for (int x = 20; x <= 42; x++) {
			for (int z = 20; z <= 42; z++) {
				double r = Math.hypot(x + 0.5 - c, z + 0.5 - c);
				if (r >= 4.5 && r < 5.6) {
					for (int y = yc + 20; y <= yc + 24; y++) {
						if (b.has(x, y, z) && !b.has(x, y + 1, z)) {
							b.set(x, y, z, Blocks.IRON_BLOCK);
						}
					}
				}
			}
		}
		// The entrance passage from the portico into the rotunda, a step down inside.
		for (int x = 28; x <= 33; x++) {
			for (int z = 50; z <= 65; z++) {
				for (int y = 1; y <= 13; y++) {
					b.air(x, y, z);
				}
				if (z >= 56) {
					b.set(x, 1, z, Blocks.POLISHED_DIORITE);
					b.set(x, 2, z, Blocks.POLISHED_DIORITE);
					for (int y = 3; y <= 13; y++) {
						b.air(x, y, z);
					}
				} else if (z == 55) {
					b.set(x, 1, z, Blocks.POLISHED_DIORITE);
					b.set(x, 2, z, Bp.stairs(Blocks.POLISHED_DIORITE_STAIRS, Direction.SOUTH, false));
				} else if (z == 54) {
					b.set(x, 1, z, Bp.stairs(Blocks.POLISHED_DIORITE_STAIRS, Direction.SOUTH, false));
				} else {
					b.set(x, 0, z, Blocks.POLISHED_DIORITE);
				}
			}
		}
		// The transitional block between rotunda and portico, with its own pediment high up.
		for (int x = 13; x <= 48; x++) {
			for (int z = 56; z <= 65; z++) {
				boolean passage = x >= 28 && x <= 33;
				for (int y = 1; y <= 30; y++) {
					if (passage && y <= 13) {
						continue;
					}
					b.set(x, y, z, z == 65 || x == 13 || x == 48 ? (Math.floorMod(y, 10) == 0 ? smooth : st(Blocks.BRICKS)) : tuff);
				}
				double hp = (18 - Math.abs(x + 0.5 - c)) * 0.4;
				for (int y = 31; y <= 30 + (int) hp; y++) {
					b.set(x, y, z, z == 65 ? calcite : smooth);
				}
			}
		}
		for (int x : new int[]{17, 18, 19, 42, 43, 44}) {
			for (int y = 3; y <= 10; y++) {
				b.air(x, y, 65);
			}
			b.set(x, 2, 65, Blocks.CALCITE);
		}
		// The portico: raised floor, sixteen columns, entablature, the low gable roof and the pediment.
		for (int x = 14; x <= 47; x++) {
			for (int z = 66; z <= 81; z++) {
				b.set(x, 1, z, Blocks.POLISHED_DIORITE);
				b.set(x, 2, z, (x + z) % 2 == 0 ? st(Blocks.POLISHED_DIORITE) : st(Blocks.POLISHED_ANDESITE));
			}
			b.set(x, 2, 82, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
			b.set(x, 1, 82, Blocks.STONE_BRICKS);
			b.set(x, 1, 83, Bp.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
		}
		int[] front = {15, 19, 24, 28, 32, 36, 41, 45};
		int[] inner = {15, 24, 36, 45};
		for (int fx : front) {
			pantheonColumn(b, fx, 79, st(Blocks.POLISHED_ANDESITE));
		}
		for (int fx : inner) {
			pantheonColumn(b, fx, 74, st(Blocks.POLISHED_GRANITE));
			pantheonColumn(b, fx, 69, st(Blocks.POLISHED_GRANITE));
		}
		for (int x = 14; x <= 47; x++) {
			for (int z = 66; z <= 81; z++) {
				boolean edge = x <= 15 || x >= 46 || z >= 79;
				if (edge) {
					b.set(x, 16, z, calcite);
					b.set(x, 17, z, calcite);
				}
				b.set(x, 18, z, smooth);
				double hr = (17.5 - Math.abs(x + 0.5 - c)) * 0.42;
				int full = (int) Math.floor(hr);
				for (int y = 19; y < 19 + full; y++) {
					b.set(x, y, z, z == 81 ? calcite : tuff);
				}
				if (hr - full >= 0.5) {
					b.set(x, 19 + full, z, Bp.slab(Blocks.SMOOTH_STONE_SLAB, false));
				} else if (full > 0) {
					b.set(x, 18 + full, z, smooth);
				}
			}
			double hr = (17.5 - Math.abs(x + 0.5 - c)) * 0.42;
			b.set(x, 19 + (int) Math.floor(hr), 82, Bp.slab(Blocks.SMOOTH_STONE_SLAB, false));
		}
		for (int x = 30; x <= 31; x++) {
			b.set(x, 14, 72, Bp.lantern(true));
			for (int y = 15; y <= 17; y++) {
				b.set(x, y, 72, Blocks.IRON_CHAIN);
			}
		}
	}

	/** One of the portico's granite columns: 2×2, base, shaft and a white Corinthian capital. */
	private static void pantheonColumn(Bp b, int x, int z, BlockState shaft) {
		for (int dx = 0; dx <= 1; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				b.set(x + dx, 3, z + dz, Blocks.CALCITE);
				for (int y = 4; y <= 13; y++) {
					b.set(x + dx, y, z + dz, shaft);
				}
				b.set(x + dx, 14, z + dz, Blocks.CHISELED_QUARTZ_BLOCK);
				b.set(x + dx, 15, z + dz, Blocks.QUARTZ_BLOCK);
			}
		}
	}
}
