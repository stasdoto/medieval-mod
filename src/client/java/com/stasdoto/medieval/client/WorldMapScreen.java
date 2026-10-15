package com.stasdoto.medieval.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.stasdoto.medieval.construct.Landmarks;
import com.stasdoto.medieval.earth.Projection;
import com.stasdoto.medieval.history.Places;

/**
 * The big map (key M): the Old World of 1050 with its towns and wonders and you on it. Drag to move, wheel to zoom,
 * click a town or any point to go there (needs cheats/creative).
 */
public class WorldMapScreen extends Screen {
	static final Identifier MAP = Identifier.fromNamespaceAndPath("medieval", "textures/gui/worldmap.png");
	static final double LON0 = -26, LON1 = 92, LAT0 = 4, LAT1 = 72;
	static final int TW = 2048, TH = 1731;
	/** Map pixels per screen pixel... kept as: screen px per map px. */
	double scale = 0, cx, cy;
	boolean dragged;

	public WorldMapScreen() {
		super(Component.literal("Карта мира 1050"));
	}

	static double mapX(double lon) {
		return (lon - LON0) / (LON1 - LON0) * TW;
	}

	static double mapY(double lat) {
		return (LAT1 - lat) / (LAT1 - LAT0) * TH;
	}

	@Override
	protected void init() {
		if (scale == 0) {
			var p = Minecraft.getInstance().player;
			double lat = p == null ? 50.45 : Projection.lat(p.getZ()), lon = p == null ? 30.52 : Projection.lon(p.getX());
			cx = mapX(lon);
			cy = mapY(lat);
			scale = Math.max(width / (double) TW, height / (double) TH) * 3;
		}
	}

	int sx(double mx) {
		return (int) Math.round(width / 2.0 + (mx - cx) * scale);
	}

	int sy(double my) {
		return (int) Math.round(height / 2.0 + (my - cy) * scale);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float pt) {
		g.fill(0, 0, width, height, 0xFF6F8487);
		int x0 = sx(0), y0 = sy(0), x1 = sx(TW), y1 = sy(TH);
		g.blit(MAP, x0, y0, x1, y1, 0f, 1f, 0f, 1f);
		var font = Minecraft.getInstance().font;
		String hover = null;
		for (Places.Place p : Places.ALL) {
			int x = sx(mapX(p.lon())), y = sy(mapY(p.lat()));
			int r = 1 + p.size() / 2;
			g.fill(x - r - 1, y - r - 1, x + r + 2, y + r + 2, 0xFF1E140A);
			g.fill(x - r, y - r, x + r + 1, y + r + 1, p.size() >= 4 ? 0xFFC0392B : 0xFFE8D9A8);
			if (p.size() >= 5 || p.size() >= 4 && scale > 0.6 || scale > 1.4) {
				g.text(font, p.name(), x + r + 3, y - 4, 0xFF20160C, false);
			}
			if (Math.abs(mouseX - x) <= r + 3 && Math.abs(mouseY - y) <= r + 3) {
				hover = p.name() + " — " + p.realm() + ", ~" + p.people() + " жит.";
			}
		}
		for (Landmarks.Mark m : Landmarks.all()) {
			if (m.id().startsWith("giza_") && !m.id().equals("giza_khufu")) {
				continue;
			}
			int x = sx(mapX(m.lon())), y = sy(mapY(m.lat()));
			g.fill(x - 3, y - 3, x + 4, y + 4, 0xFF5A3D00);
			g.fill(x - 2, y - 2, x + 3, y + 3, 0xFFFFC94A);
			if (Math.abs(mouseX - x) <= 5 && Math.abs(mouseY - y) <= 5) {
				hover = "Чудо света: " + m.name();
			}
		}
		var pl = Minecraft.getInstance().player;
		if (pl != null) {
			int x = sx(mapX(Projection.lon(pl.getX()))), y = sy(mapY(Projection.lat(pl.getZ())));
			g.fill(x - 4, y - 4, x + 5, y + 5, 0xFFFFFFFF);
			g.fill(x - 3, y - 3, x + 4, y + 4, 0xFF1F4E9C);
		}
		double lat = LAT1 - (cy + (mouseY - height / 2.0) / scale) / TH * (LAT1 - LAT0);
		double lon = LON0 + (cx + (mouseX - width / 2.0) / scale) / TW * (LON1 - LON0);
		g.fill(0, 0, width, 22, 0xC0201810);
		g.text(font, "Карта мира 1050 · колесо — масштаб, тянуть — двигать, клик — телепорт · M или Esc — закрыть", 6, 3, 0xFFF0E6D2, false);
		g.text(font, String.format("%.3f° с.ш., %.3f° в.д. · блок X %d, Z %d", lat, lon, Math.round(Projection.x(lon)), Math.round(Projection.z(lat))), 6, 12, 0xFFD8C8A0, false);
		if (hover != null) {
			g.text(font, hover, mouseX + 10, mouseY - 4, 0xFFFFFFFF, true);
		}
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		cx -= dx / scale;
		cy -= dy / scale;
		dragged = true;
		return true;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		double before = scale;
		scale = Math.max(0.2, Math.min(12, scale * (v > 0 ? 1.25 : 0.8)));
		// Keep the point under the mouse in place.
		cx += (mx - width / 2.0) / before - (mx - width / 2.0) / scale;
		cy += (my - height / 2.0) / before - (my - height / 2.0) / scale;
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
		dragged = false;
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		if (!dragged && e.button() == 0) {
			double mx = e.x(), my = e.y();
			String target = null;
			for (Places.Place p : Places.ALL) {
				if (Math.abs(mx - sx(mapX(p.lon()))) <= 5 && Math.abs(my - sy(mapY(p.lat()))) <= 5) {
					target = p.id();
				}
			}
			for (Landmarks.Mark m : Landmarks.all()) {
				if (Math.abs(mx - sx(mapX(m.lon()))) <= 5 && Math.abs(my - sy(mapY(m.lat()))) <= 5) {
					target = m.id();
				}
			}
			if (target == null) {
				double lat = LAT1 - (cy + (my - height / 2.0) / scale) / TH * (LAT1 - LAT0);
				double lon = LON0 + (cx + (mx - width / 2.0) / scale) / TW * (LON1 - LON0);
				target = String.format(java.util.Locale.ROOT, "%.5f %.5f", lat, lon);
			}
			var pl = Minecraft.getInstance().player;
			if (pl != null) {
				pl.connection.sendCommand("medieval go " + target);
			}
			onClose();
		}
		dragged = false;
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
