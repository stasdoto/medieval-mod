package com.stasdoto.medieval.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import com.stasdoto.medieval.Medieval;
import com.stasdoto.medieval.history.Culture;
import com.stasdoto.medieval.people.Role;

/**
 * Paints the people of the 11th century as 64×64 player-style skins: the face (skin tone of the region, eyes, hair,
 * beards) and clothes of their people and work - Rus shirts with embroidered collars and leg wraps, Western tunics with
 * hoods, Norse tunics, Byzantine long robes, Muslim robes with turbans and veils, steppe kaftans with fur hats, monks'
 * black habits, warriors' mail and helmets.
 */
public final class Skins {
	private Skins() {
	}

	private static final Map<Long, Identifier> CACHE = new HashMap<>();
	private static final Map<Identifier, net.minecraft.world.entity.player.PlayerSkin> SKINS = new HashMap<>();

	private static final int[] LIGHT = {0xFFF6DCC8, 0xFFEFC9AE, 0xFFE8BE9F, 0xFFE3B595};
	private static final int[] OLIVE = {0xFFE3B595, 0xFFD5A07E, 0xFFC99272, 0xFFC48A66};
	private static final int[] BROWN = {0xFFC48A66, 0xFFAE7451, 0xFF955F3E};
	private static final int[] HAIR_N = {0xFF33231A, 0xFF5E412B, 0xFF7E5E3E, 0xFFC4A266, 0xFFD8BC7A, 0xFF8A3A1E, 0xFF5E412B};
	private static final int[] HAIR_S = {0xFF17120F, 0xFF17120F, 0xFF33231A, 0xFF33231A, 0xFF5E412B};
	private static final int[] EYES_N = {0xFF3A6CA6, 0xFF4E7A3A, 0xFF7A8590, 0xFF4A2E1A};
	private static final int[] EYES_S = {0xFF4A2E1A, 0xFF2A1A10, 0xFF4A2E1A, 0xFF4E7A3A};

	public static Identifier texture(int look, Culture c, Role role, boolean female) {
		long key = ((long) look << 24) ^ ((long) c.ordinal() << 8) ^ (role.ordinal() << 2) ^ (female ? 1 : 0);
		Identifier id = CACHE.get(key);
		if (id != null) {
			return id;
		}
		NativeImage img = new NativeImage(64, 64, true);
		img.fillRect(0, 0, 64, 64, 0);
		paint(img, look, c, role, female);
		id = Medieval.id("dynamic/person_" + Long.toHexString(key));
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "medieval person", img));
		CACHE.put(key, id);
		return id;
	}

	public static net.minecraft.world.entity.player.PlayerSkin skin(Identifier texture) {
		return SKINS.computeIfAbsent(texture, id -> {
			net.minecraft.core.ClientAsset.ResourceTexture body = new net.minecraft.core.ClientAsset.ResourceTexture(id, id);
			return net.minecraft.world.entity.player.PlayerSkin.insecure(body, null, null, net.minecraft.world.entity.player.PlayerModelType.WIDE);
		});
	}

	// ---- skin layout ----

	private record Face(int x, int y, int w, int h) {
		void fill(NativeImage img, int c) {
			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					img.setPixel(x + i, y + j, c);
				}
			}
		}

		void set(NativeImage img, int i, int j, int c) {
			if (i >= 0 && i < w && j >= 0 && j < h) {
				img.setPixel(x + i, y + j, c);
			}
		}

		void row(NativeImage img, int j, int c) {
			for (int i = 0; i < w; i++) {
				set(img, i, j, c);
			}
		}
	}

	private record Part(int u, int v, int w, int h, int d) {
		Face top() {
			return new Face(u + d, v, w, d);
		}

		Face bottom() {
			return new Face(u + d + w, v, w, d);
		}

		Face right() {
			return new Face(u, v + d, d, h);
		}

		Face front() {
			return new Face(u + d, v + d, w, h);
		}

		Face left() {
			return new Face(u + d + w, v + d, d, h);
		}

		Face back() {
			return new Face(u + d + w + d, v + d, w, h);
		}

		Face[] sides() {
			return new Face[]{right(), front(), left(), back()};
		}

		void fill(NativeImage img, int c) {
			top().fill(img, c);
			bottom().fill(img, c);
			for (Face f : sides()) {
				f.fill(img, c);
			}
		}

		void band(NativeImage img, int from, int to, int c) {
			for (Face f : sides()) {
				for (int j = from; j <= to; j++) {
					f.row(img, j, c);
				}
			}
		}
	}

	private static final Part HEAD = new Part(0, 0, 8, 8, 8);
	private static final Part HAT = new Part(32, 0, 8, 8, 8);
	private static final Part BODY = new Part(16, 16, 8, 12, 4);
	private static final Part JACKET = new Part(16, 32, 8, 12, 4);
	private static final Part RIGHT_ARM = new Part(40, 16, 4, 12, 4);
	private static final Part LEFT_ARM = new Part(32, 48, 4, 12, 4);
	private static final Part RIGHT_LEG = new Part(0, 16, 4, 12, 4);
	private static final Part LEFT_LEG = new Part(16, 48, 4, 12, 4);
	private static final Part[] ARMS = {RIGHT_ARM, LEFT_ARM};
	private static final Part[] LEGS = {RIGHT_LEG, LEFT_LEG};

	static int shade(int argb, float k) {
		int r = Math.min(255, Math.round(((argb >> 16) & 255) * k));
		int g = Math.min(255, Math.round(((argb >> 8) & 255) * k));
		int b = Math.min(255, Math.round((argb & 255) * k));
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	static int mix(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
		return 0xFF000000 | r << 16 | g << 8 | bl;
	}

	/** Undyed and plant-dyed cloth of the time: linen, wool greys and browns, madder red, woad blue, weld yellow. */
	private static final int[] PLAIN = {0xFFE8E0CC, 0xFFD9CFB4, 0xFFB8AC8E, 0xFF8C7D63, 0xFF6E5E48, 0xFF7A7A72};
	private static final int[] DYED = {0xFF8E2A22, 0xFF3C4F7A, 0xFF6B7A3A, 0xFFB08A2E, 0xFF6A2E4A, 0xFF2E5A52};

	private static void paint(NativeImage img, int look, Culture c, Role role, boolean female) {
		Random r = new Random(look * 0x9E3779B97F4A7C15L);
		boolean south = switch (c) {
			case BYZANTINE, ANDALUSI, ARAB, PERSIAN, CAUCASUS, STEPPE -> true;
			default -> false;
		};
		int[] tones = c == Culture.ARAB ? BROWN : south ? OLIVE : LIGHT;
		int skin = tones[r.nextInt(tones.length)];
		int hair = role == Role.MONK || role == Role.PRIEST && r.nextBoolean() ? 0xFF8C8C8C : (south ? HAIR_S : HAIR_N)[r.nextInt((south ? HAIR_S : HAIR_N).length)];
		int eyes = (south ? EYES_S : EYES_N)[r.nextInt(4)];
		for (Part p : new Part[]{HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG}) {
			p.fill(img, skin);
		}
		face(img, skin, hair, eyes, female);
		boolean beard = !female && role != Role.CHILD && (c != Culture.LATIN || r.nextInt(3) == 0) && r.nextInt(4) != 0;
		hair(img, hair, skin, female, beard, role, c, r);

		int cloth = r.nextInt(3) == 0 ? DYED[r.nextInt(DYED.length)] : PLAIN[r.nextInt(PLAIN.length)];
		int cloth2 = PLAIN[r.nextInt(PLAIN.length)];
		int trim = DYED[r.nextInt(DYED.length)];
		int leather = 0xFF5A3E28;
		switch (role) {
			case MONK -> robe(img, 0xFF1E1A1A, 0xFF1E1A1A, 0xFF3A2A1A, true);
			case PRIEST -> {
				if (c == Culture.ANDALUSI || c == Culture.ARAB || c == Culture.PERSIAN) {
					robe(img, 0xFFE8E4DA, 0xFFE8E4DA, 0xFF6E5E48, false);
					turban(img, 0xFFF2F0EA);
				} else {
					robe(img, 0xFF1E1A1A, 0xFFB08A2E, 0xFF3A2A1A, false);
				}
			}
			case WARRIOR -> warrior(img, c, cloth, trim, leather, r);
			case NOBLE -> {
				int rich = DYED[r.nextInt(DYED.length)];
				if (female) {
					dress(img, rich, 0xFFD8B04A, 0xFF5A3E28);
				} else {
					kaftan(img, rich, 0xFFD8B04A, 0xFF3A2A1A, 0xFF5A3E28);
				}
				if (c == Culture.RUS || c == Culture.STEPPE) {
					furHat(img, rich);
				}
			}
			default -> {
				if (female) {
					dress(img, cloth, trim, leather);
				} else {
					tunic(img, cloth, cloth2, trim, leather, c, role);
				}
			}
		}
		// Headwear of the people.
		if (role != Role.WARRIOR && role != Role.MONK && role != Role.PRIEST && role != Role.NOBLE) {
			switch (c) {
				case RUS, BALT_FINN, WEST_SLAV -> {
					if (female && role != Role.CHILD) {
						kerchief(img, r.nextBoolean() ? 0xFFF2EEE4 : trim);
					}
				}
				case LATIN, CELTIC -> {
					if (female && role != Role.CHILD) {
						kerchief(img, 0xFFF2EEE4);
					} else if (r.nextInt(3) == 0 && role != Role.CHILD) {
						hood(img, r.nextBoolean() ? cloth : trim);
					}
				}
				case ANDALUSI, ARAB, PERSIAN -> {
					if (female) {
						veil(img, r.nextBoolean() ? 0xFF2A2A30 : 0xFF6E5E48);
					} else if (role != Role.CHILD) {
						turban(img, r.nextBoolean() ? 0xFFF2F0EA : 0xFF3C4F7A);
					}
				}
				case STEPPE -> furHat(img, 0xFF6E5E48);
				case BYZANTINE, CAUCASUS -> {
					if (female) {
						veil(img, 0xFFE8E0CC);
					}
				}
				default -> {
				}
			}
		}
	}

	private static void face(NativeImage img, int skin, int hair, int eyes, boolean female) {
		Face f = HEAD.front();
		int brow = shade(hair, 0.85f);
		f.set(img, 1, 3, brow);
		f.set(img, 2, 3, brow);
		f.set(img, 5, 3, brow);
		f.set(img, 6, 3, brow);
		f.set(img, 1, 4, 0xFFF2F0EC);
		f.set(img, 2, 4, eyes);
		f.set(img, 5, 4, eyes);
		f.set(img, 6, 4, 0xFFF2F0EC);
		f.set(img, 3, 5, shade(skin, 0.9f));
		f.set(img, 4, 5, shade(skin, 0.88f));
		int lips = mix(shade(skin, 0.72f), 0xFF9A4A44, female ? 0.45f : 0.25f);
		f.set(img, 3, 6, lips);
		f.set(img, 4, 6, lips);
		HEAD.right().set(img, 3, 4, shade(skin, 0.9f));
		HEAD.left().set(img, 4, 4, shade(skin, 0.9f));
		HEAD.bottom().fill(img, shade(skin, 0.85f));
	}

	private static void hair(NativeImage img, int h, int skin, boolean female, boolean beard, Role role, Culture c, Random r) {
		int h2 = shade(h, 1.18f);
		HEAD.top().fill(img, h);
		for (int i = 0; i < 8; i++) {
			if (r.nextInt(4) == 0) {
				HEAD.top().set(img, i, r.nextInt(8), h2);
			}
		}
		int sideRows = female ? 8 : role == Role.MONK ? 2 : c == Culture.NORSE ? 6 : 3;
		for (int j = 0; j < sideRows; j++) {
			for (int i = 0; i < 8; i++) {
				if (!(female && j >= 3 && i >= 5)) {
					HEAD.right().set(img, i, j, h);
					HEAD.left().set(img, 7 - i, j, h);
				}
			}
		}
		for (int j = 0; j < (female ? 8 : sideRows + 1); j++) {
			HEAD.back().row(img, j, h);
		}
		HEAD.front().row(img, 0, h);
		if (female) {
			for (int j = 1; j < 8; j++) {
				HEAD.front().set(img, 0, j, h);
				HEAD.front().set(img, 7, j, h);
			}
		}
		if (role == Role.MONK) {
			// Tonsure.
			for (int i = 2; i < 6; i++) {
				for (int j = 2; j < 6; j++) {
					HEAD.top().set(img, i, j, skin);
				}
			}
		}
		if (beard) {
			Face f = HEAD.front();
			for (int i = 1; i < 7; i++) {
				f.set(img, i, 7, h);
			}
			f.set(img, 1, 6, h);
			f.set(img, 6, 6, h);
			f.set(img, 1, 5, h);
			f.set(img, 6, 5, h);
			f.set(img, 2, 7, h);
			f.set(img, 5, 7, h);
			f.set(img, 2, 6, h);
			f.set(img, 5, 6, h);
			for (int j = 5; j < 8; j++) {
				HEAD.right().set(img, 6, j, h);
				HEAD.right().set(img, 7, j, h);
				HEAD.left().set(img, 0, j, h);
				HEAD.left().set(img, 1, j, h);
			}
			HEAD.bottom().fill(img, h);
		}
	}

	/** A knee-long shirt or tunic with a belt, trousers and leg wraps or hose, shoes. */
	private static void tunic(NativeImage img, int cloth, int trousers, int trim, int leather, Culture c, Role role) {
		BODY.fill(img, cloth);
		for (Part a : ARMS) {
			a.top().fill(img, cloth);
			a.band(img, 0, 9, cloth);
			a.band(img, 9, 9, shade(cloth, 0.8f));
		}
		// The tunic hangs over the thighs.
		for (Part l : LEGS) {
			l.fill(img, trousers);
			l.band(img, 0, 3, cloth);
			l.band(img, 10, 11, leather);
			l.bottom().fill(img, shade(leather, 0.8f));
		}
		BODY.band(img, 7, 7, leather);
		// Embroidered collar and hem (Rus, Slavs), a plain one elsewhere.
		Face f = BODY.front();
		boolean embroider = c == Culture.RUS || c == Culture.WEST_SLAV || c == Culture.BALT_FINN || c == Culture.MAGYAR;
		for (int i = 2; i < 6; i++) {
			f.set(img, i, 0, embroider ? trim : shade(cloth, 0.85f));
		}
		if (embroider) {
			f.set(img, 3, 1, trim);
			f.set(img, 4, 1, trim);
			for (Part l : LEGS) {
				l.band(img, 3, 3, trim);
			}
			// Leg wraps (онучи) bound crosswise.
			for (Part l : LEGS) {
				for (int j = 5; j <= 9; j++) {
					l.band(img, j, j, j % 2 == 0 ? 0xFFE8E0CC : 0xFFB8AC8E);
				}
			}
		}
		if (role == Role.SMITH) {
			// Leather apron.
			for (int j = 2; j < 12; j++) {
				f.row(img, j, 0xFF4A3220);
			}
		}
		if (role == Role.MERCHANT) {
			BODY.band(img, 7, 7, 0xFFD8B04A);
		}
	}

	/** A long dress with a belt and an apron or a cloak. */
	private static void dress(NativeImage img, int cloth, int trim, int leather) {
		BODY.fill(img, cloth);
		for (Part a : ARMS) {
			a.top().fill(img, cloth);
			a.band(img, 0, 10, cloth);
			a.band(img, 10, 10, trim);
		}
		for (Part l : LEGS) {
			l.fill(img, cloth);
			l.band(img, 11, 11, leather);
			l.band(img, 10, 10, trim);
		}
		BODY.band(img, 6, 6, trim);
		Face f = BODY.front();
		for (int i = 2; i < 6; i++) {
			f.set(img, i, 0, trim);
		}
	}

	private static void robe(NativeImage img, int cloth, int stole, int shoes, boolean hoodUp) {
		BODY.fill(img, cloth);
		for (Part a : ARMS) {
			a.top().fill(img, cloth);
			a.band(img, 0, 11, cloth);
		}
		for (Part l : LEGS) {
			l.fill(img, cloth);
			l.band(img, 11, 11, shoes);
		}
		Face f = BODY.front();
		for (int j = 0; j < 12; j++) {
			f.set(img, 3, j, stole);
			f.set(img, 4, j, stole);
		}
		BODY.band(img, 6, 6, 0xFF6E5E48);
		if (hoodUp) {
			hood(img, cloth);
		}
	}

	private static void kaftan(NativeImage img, int cloth, int trim, int trousers, int boots) {
		BODY.fill(img, cloth);
		for (Part a : ARMS) {
			a.top().fill(img, cloth);
			a.band(img, 0, 11, cloth);
			a.band(img, 10, 11, trim);
		}
		for (Part l : LEGS) {
			l.fill(img, trousers);
			l.band(img, 0, 4, cloth);
			l.band(img, 7, 11, boots);
		}
		Face f = BODY.front();
		for (int j = 0; j < 12; j++) {
			f.set(img, 4, j, trim);
		}
		BODY.band(img, 7, 7, 0xFF3A2A1A);
		for (int i = 0; i < 8; i++) {
			f.set(img, i, 0, trim);
		}
	}

	/** Mail shirt, padded coat, a conical helmet with a nasal (or a pointed Rus helmet), a cloak. */
	private static void warrior(NativeImage img, Culture c, int cloth, int trim, int leather, Random r) {
		int mail = 0xFF8A8C90, mail2 = 0xFF6A6C70;
		for (Part p : new Part[]{BODY}) {
			for (Face f : p.sides()) {
				for (int j = 0; j < f.h(); j++) {
					for (int i = 0; i < f.w(); i++) {
						f.set(img, i, j, (i + j) % 2 == 0 ? mail : mail2);
					}
				}
			}
			p.top().fill(img, mail);
		}
		for (Part a : ARMS) {
			for (Face f : a.sides()) {
				for (int j = 0; j < 7; j++) {
					for (int i = 0; i < f.w(); i++) {
						f.set(img, i, j, (i + j) % 2 == 0 ? mail : mail2);
					}
				}
			}
			a.top().fill(img, mail);
		}
		for (Part l : LEGS) {
			l.fill(img, shade(cloth, 0.7f));
			l.band(img, 0, 3, (c == Culture.STEPPE || c == Culture.ARAB) ? cloth : mail);
			l.band(img, 8, 11, leather);
		}
		BODY.band(img, 7, 7, leather);
		// Helmet on the hat layer.
		int steel = 0xFF9A9CA2;
		HAT.top().fill(img, steel);
		for (Face f : HAT.sides()) {
			f.row(img, 0, steel);
			f.row(img, 1, steel);
			f.row(img, 2, shade(steel, 0.85f));
		}
		HAT.front().set(img, 3, 3, steel);
		HAT.front().set(img, 4, 3, steel);
		HAT.front().set(img, 3, 4, steel);
		HAT.front().set(img, 4, 4, steel);
		if (c == Culture.ANDALUSI || c == Culture.ARAB || c == Culture.PERSIAN) {
			for (Face f : HAT.sides()) {
				f.row(img, 2, 0xFFF2F0EA);
			}
		}
		// A cloak on the jacket layer's back for some.
		if (r.nextBoolean()) {
			Face back = JACKET.back();
			for (int j = 0; j < 12; j++) {
				back.row(img, j, trim);
			}
		}
	}

	private static void kerchief(NativeImage img, int c) {
		HAT.top().fill(img, c);
		for (Face f : HAT.sides()) {
			f.row(img, 0, c);
			f.row(img, 1, c);
		}
		HAT.back().fill(img, c);
		HAT.right().row(img, 2, c);
		HAT.left().row(img, 2, c);
	}

	private static void hood(NativeImage img, int c) {
		HAT.top().fill(img, c);
		for (Face f : HAT.sides()) {
			f.row(img, 0, c);
			f.row(img, 1, c);
		}
		HAT.back().fill(img, c);
		for (int j = 0; j < 8; j++) {
			HAT.right().set(img, 0, j, c);
			HAT.right().set(img, 1, j, c);
			HAT.left().set(img, 6, j, c);
			HAT.left().set(img, 7, j, c);
		}
	}

	private static void veil(NativeImage img, int c) {
		HAT.top().fill(img, c);
		HAT.back().fill(img, c);
		HAT.right().fill(img, c);
		HAT.left().fill(img, c);
		HAT.front().row(img, 0, c);
		HAT.front().row(img, 1, c);
		for (int j = 2; j < 8; j++) {
			HAT.front().set(img, 0, j, c);
			HAT.front().set(img, 7, j, c);
		}
	}

	private static void turban(NativeImage img, int c) {
		HAT.top().fill(img, c);
		for (Face f : HAT.sides()) {
			f.row(img, 0, c);
			f.row(img, 1, shade(c, 0.9f));
			f.row(img, 2, c);
		}
	}

	private static void furHat(NativeImage img, int band) {
		int fur = 0xFF4A3828;
		HAT.top().fill(img, band);
		for (Face f : HAT.sides()) {
			f.row(img, 0, band);
			f.row(img, 1, fur);
			f.row(img, 2, fur);
		}
	}
}
