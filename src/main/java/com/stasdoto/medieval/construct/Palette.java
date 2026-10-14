package com.stasdoto.medieval.construct;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.stasdoto.medieval.history.Culture;

/** The building materials of a people (and some variety from house to house). */
public final class Palette {
	public Block log = Blocks.SPRUCE_LOG;
	public Block strippedLog = Blocks.STRIPPED_SPRUCE_LOG;
	public Block planks = Blocks.SPRUCE_PLANKS;
	public Block stairs = Blocks.SPRUCE_STAIRS;
	public Block slab = Blocks.SPRUCE_SLAB;
	public Block fence = Blocks.SPRUCE_FENCE;
	public Block gate = Blocks.SPRUCE_FENCE_GATE;
	public Block door = Blocks.SPRUCE_DOOR;
	public Block trapdoor = Blocks.SPRUCE_TRAPDOOR;
	public Block roofStairs = Blocks.SPRUCE_STAIRS;
	public Block roofSlab = Blocks.SPRUCE_SLAB;
	public BlockState roofFull = Blocks.SPRUCE_PLANKS.defaultBlockState();
	public BlockState stone = Blocks.COBBLESTONE.defaultBlockState();
	public BlockState stone2 = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
	public BlockState plaster = Blocks.CALCITE.defaultBlockState();
	public BlockState floor = Blocks.SPRUCE_PLANKS.defaultBlockState();
	public Block bed = Blocks.BED.pick(DyeColor.BROWN);
	public boolean thatch;

	public static Palette of(Culture c, java.util.Random r) {
		Palette p = new Palette();
		switch (c) {
			case RUS, BALT_FINN -> {
				if (r.nextInt(3) == 0) {
					p.log = Blocks.OAK_LOG;
					p.strippedLog = Blocks.STRIPPED_OAK_LOG;
				}
				p.bed = Blocks.BED.pick(r.nextBoolean() ? DyeColor.RED : DyeColor.BROWN);
			}
			case WEST_SLAV, MAGYAR -> {
				p.log = Blocks.OAK_LOG;
				p.strippedLog = Blocks.STRIPPED_OAK_LOG;
				p.planks = Blocks.OAK_PLANKS;
				p.stairs = Blocks.OAK_STAIRS;
				p.slab = Blocks.OAK_SLAB;
				p.fence = Blocks.OAK_FENCE;
				p.gate = Blocks.OAK_FENCE_GATE;
				p.door = Blocks.OAK_DOOR;
				p.trapdoor = Blocks.OAK_TRAPDOOR;
				p.thatch = true;
				p.roofFull = Blocks.HAY_BLOCK.defaultBlockState();
				p.plaster = r.nextBoolean() ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.CALCITE.defaultBlockState();
				p.floor = Blocks.PACKED_MUD.defaultBlockState();
			}
			case LATIN, CELTIC -> {
				p.log = Blocks.DARK_OAK_LOG;
				p.strippedLog = Blocks.STRIPPED_DARK_OAK_LOG;
				p.planks = Blocks.OAK_PLANKS;
				p.stairs = Blocks.OAK_STAIRS;
				p.slab = Blocks.OAK_SLAB;
				p.fence = Blocks.OAK_FENCE;
				p.gate = Blocks.OAK_FENCE_GATE;
				p.door = r.nextBoolean() ? Blocks.SPRUCE_DOOR : Blocks.DARK_OAK_DOOR;
				p.trapdoor = Blocks.SPRUCE_TRAPDOOR;
				int roof = r.nextInt(3);
				if (roof == 0) {
					p.roofStairs = Blocks.DEEPSLATE_TILE_STAIRS;
					p.roofSlab = Blocks.DEEPSLATE_TILE_SLAB;
					p.roofFull = Blocks.DEEPSLATE_TILES.defaultBlockState();
				} else if (roof == 1) {
					p.roofStairs = Blocks.DARK_OAK_STAIRS;
					p.roofSlab = Blocks.DARK_OAK_SLAB;
					p.roofFull = Blocks.DARK_OAK_PLANKS.defaultBlockState();
				} else {
					p.roofStairs = Blocks.SPRUCE_STAIRS;
					p.roofSlab = Blocks.SPRUCE_SLAB;
					p.roofFull = Blocks.SPRUCE_PLANKS.defaultBlockState();
				}
				p.stone = r.nextBoolean() ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
				p.stone2 = r.nextBoolean() ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
				int infill = r.nextInt(3);
				p.plaster = infill == 0 ? Blocks.CALCITE.defaultBlockState() : infill == 1 ? Blocks.MUD_BRICKS.defaultBlockState()
						: Blocks.PACKED_MUD.defaultBlockState();
				p.floor = Blocks.OAK_PLANKS.defaultBlockState();
				p.bed = Blocks.BED.pick(r.nextBoolean() ? DyeColor.GREEN : DyeColor.BLUE);
			}
			case NORSE -> {
				p.log = Blocks.DARK_OAK_LOG;
				p.strippedLog = Blocks.STRIPPED_DARK_OAK_LOG;
				p.roofStairs = Blocks.DARK_OAK_STAIRS;
				p.roofSlab = Blocks.DARK_OAK_SLAB;
				p.roofFull = Blocks.DARK_OAK_PLANKS.defaultBlockState();
			}
			case BYZANTINE, CAUCASUS -> {
				p.log = Blocks.OAK_LOG;
				p.planks = Blocks.OAK_PLANKS;
				p.stairs = Blocks.OAK_STAIRS;
				p.slab = Blocks.OAK_SLAB;
				p.fence = Blocks.OAK_FENCE;
				p.gate = Blocks.OAK_FENCE_GATE;
				p.door = Blocks.OAK_DOOR;
				p.trapdoor = Blocks.OAK_TRAPDOOR;
				p.roofStairs = Blocks.BRICK_STAIRS;
				p.roofSlab = Blocks.BRICK_SLAB;
				p.roofFull = Blocks.BRICKS.defaultBlockState();
				p.stone = c == Culture.CAUCASUS ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.BRICKS.defaultBlockState();
				p.stone2 = c == Culture.CAUCASUS ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
				p.plaster = r.nextBoolean() ? Blocks.CALCITE.defaultBlockState() : Blocks.SMOOTH_SANDSTONE.defaultBlockState();
				p.floor = Blocks.POLISHED_ANDESITE.defaultBlockState();
				p.bed = Blocks.BED.pick(DyeColor.PURPLE);
			}
			case ANDALUSI, ARAB, PERSIAN, STEPPE -> {
				p.log = Blocks.JUNGLE_LOG;
				p.planks = Blocks.JUNGLE_PLANKS;
				p.stairs = Blocks.SMOOTH_SANDSTONE_STAIRS;
				p.slab = Blocks.SMOOTH_SANDSTONE_SLAB;
				p.fence = Blocks.JUNGLE_FENCE;
				p.gate = Blocks.JUNGLE_FENCE_GATE;
				p.door = Blocks.JUNGLE_DOOR;
				p.trapdoor = Blocks.JUNGLE_TRAPDOOR;
				p.roofStairs = Blocks.SMOOTH_SANDSTONE_STAIRS;
				p.roofSlab = Blocks.SMOOTH_SANDSTONE_SLAB;
				p.roofFull = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
				int k = r.nextInt(3);
				p.stone = c == Culture.ANDALUSI ? Blocks.CALCITE.defaultBlockState() : k == 0 ? Blocks.MUD_BRICKS.defaultBlockState()
						: k == 1 ? Blocks.SMOOTH_SANDSTONE.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState();
				p.stone2 = Blocks.CUT_SANDSTONE.defaultBlockState();
				p.plaster = c == Culture.ANDALUSI ? Blocks.CALCITE.defaultBlockState() : Blocks.SMOOTH_SANDSTONE.defaultBlockState();
				p.floor = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
				p.bed = Blocks.BED.pick(DyeColor.ORANGE);
			}
		}
		return p;
	}
}
