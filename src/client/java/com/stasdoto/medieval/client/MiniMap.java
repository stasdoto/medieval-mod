package com.stasdoto.medieval.client;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import com.stasdoto.medieval.earth.Projection;

/** A small map of the land around you in the top right corner, drawn from the world itself, north up, with the place name. */
public final class MiniMap {
	private MiniMap() {
	}

	static final int N = 128;
	static final Identifier ID = Identifier.fromNamespaceAndPath("medieval", "minimap");
	static NativeImage img;
	static DynamicTexture tex;
	static long last;
	public static boolean visible = true;

	static void update(Minecraft mc) {
		var lvl = mc.level;
		var p = mc.player;
		if (lvl == null || p == null) {
			return;
		}
		if (img == null) {
			img = new NativeImage(N, N, true);
			tex = new DynamicTexture(() -> "medieval minimap", img);
			mc.getTextureManager().register(ID, tex);
		}
		int px = p.getBlockX(), pz = p.getBlockZ();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int step = 2;
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				int x = px + (i - N / 2) * step, z = pz + (j - N / 2) * step;
				int y = lvl.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
				pos.set(x, y, z);
				var st = lvl.getBlockState(pos);
				MapColor mcol = st.getMapColor(lvl, pos);
				int north = lvl.getHeight(Heightmap.Types.WORLD_SURFACE, x, z - step);
				MapColor.Brightness b = north > y + 1 ? MapColor.Brightness.LOW : north < y + 1 ? MapColor.Brightness.HIGH : MapColor.Brightness.NORMAL;
				int c = mcol == MapColor.NONE ? 0xFF404040 : mcol.calculateARGBColor(b);
				img.setPixel(i, j, c | 0xFF000000);
			}
		}
		tex.upload();
	}

	public static void draw(GuiGraphicsExtractor g, DeltaTracker dt) {
		Minecraft mc = Minecraft.getInstance();
		if (!visible || mc.player == null || mc.options.hideGui) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - last > 1000) {
			last = now;
			try {
				update(mc);
			} catch (RuntimeException e) {
				return;
			}
		}
		if (tex == null) {
			return;
		}
		int size = 96, x0 = g.guiWidth() - size - 6, y0 = 6;
		g.fill(x0 - 2, y0 - 2, x0 + size + 2, y0 + size + 2, 0xFF2A1E12);
		g.blit(ID, x0, y0, x0 + size, y0 + size, 0f, 1f, 0f, 1f);
		int c = size / 2;
		g.fill(x0 + c - 2, y0 + c - 2, x0 + c + 2, y0 + c + 2, 0xFFFFFFFF);
		g.fill(x0 + c - 1, y0 + c - 1, x0 + c + 1, y0 + c + 1, 0xFFC0392B);
		g.text(mc.font, "С", x0 + c - 2, y0 + 1, 0xFFFFFFFF, true);
		var p = mc.player;
		String where = String.format("%.3f° %.3f°", Projection.lat(p.getZ()), Projection.lon(p.getX()));
		g.text(mc.font, where, x0, y0 + size + 4, 0xFFF0E6D2, true);
	}
}
