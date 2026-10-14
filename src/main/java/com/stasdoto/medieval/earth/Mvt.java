package com.stasdoto.medieval.earth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** A reader for Mapbox Vector Tiles (the format of the OpenStreetMap tiles): layers of features with tags and shapes. */
public final class Mvt {
	public static final int POINT = 1, LINE = 2, POLYGON = 3;

	public static final class Feature {
		public int type;
		public final Map<String, Object> tags = new HashMap<>();
		/** Each part is x0,y0,x1,y1,... in tile units (0..extent). For polygons every part is a ring. */
		public final List<int[]> parts = new ArrayList<>();

		public String str(String key) {
			Object o = tags.get(key);
			return o == null ? null : o.toString();
		}
	}

	public static final class Layer {
		public String name;
		public int extent = 4096;
		public final List<Feature> features = new ArrayList<>();
	}

	private Mvt() {
	}

	public static Map<String, Layer> decode(byte[] data) throws IOException {
		if (data.length > 2 && (data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B) {
			try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
				data = in.readAllBytes();
			}
		}
		Map<String, Layer> layers = new HashMap<>();
		Reader r = new Reader(data, 0, data.length);
		while (r.more()) {
			int tag = (int) r.varint();
			if (tag >>> 3 == 3 && (tag & 7) == 2) {
				int len = (int) r.varint();
				Layer l = layer(new Reader(data, r.pos, r.pos + len));
				r.pos += len;
				if (l.name != null) {
					layers.put(l.name, l);
				}
			} else {
				r.skip(tag & 7);
			}
		}
		return layers;
	}

	private static Layer layer(Reader r) {
		Layer l = new Layer();
		List<String> keys = new ArrayList<>();
		List<Object> values = new ArrayList<>();
		List<int[]> rawFeatures = new ArrayList<>(); // start,end pairs
		while (r.more()) {
			int tag = (int) r.varint();
			int field = tag >>> 3;
			int wire = tag & 7;
			if (field == 1 && wire == 2) {
				l.name = r.string();
			} else if (field == 2 && wire == 2) {
				int len = (int) r.varint();
				rawFeatures.add(new int[]{r.pos, r.pos + len});
				r.pos += len;
			} else if (field == 3 && wire == 2) {
				keys.add(r.string());
			} else if (field == 4 && wire == 2) {
				int len = (int) r.varint();
				values.add(value(new Reader(r.buf, r.pos, r.pos + len)));
				r.pos += len;
			} else if (field == 5 && wire == 0) {
				l.extent = (int) r.varint();
			} else {
				r.skip(wire);
			}
		}
		for (int[] se : rawFeatures) {
			l.features.add(feature(new Reader(r.buf, se[0], se[1]), keys, values));
		}
		return l;
	}

	private static Object value(Reader r) {
		Object v = null;
		while (r.more()) {
			int tag = (int) r.varint();
			switch (tag >>> 3) {
				case 1 -> v = r.string();
				case 2 -> v = Float.intBitsToFloat(r.fixed32());
				case 3 -> v = Double.longBitsToDouble(r.fixed64());
				case 4, 5 -> v = r.varint();
				case 6 -> {
					long z = r.varint();
					v = (z >>> 1) ^ -(z & 1);
				}
				case 7 -> v = r.varint() != 0;
				default -> r.skip(tag & 7);
			}
		}
		return v;
	}

	private static Feature feature(Reader r, List<String> keys, List<Object> values) {
		Feature f = new Feature();
		int[] geom = null;
		while (r.more()) {
			int tag = (int) r.varint();
			int field = tag >>> 3;
			int wire = tag & 7;
			if (field == 2 && wire == 2) {
				int len = (int) r.varint();
				int stop = r.pos + len;
				while (r.pos < stop) {
					int k = (int) r.varint();
					int v = (int) r.varint();
					if (k < keys.size() && v < values.size()) {
						f.tags.put(keys.get(k), values.get(v));
					}
				}
			} else if (field == 3 && wire == 0) {
				f.type = (int) r.varint();
			} else if (field == 4 && wire == 2) {
				int len = (int) r.varint();
				int stop = r.pos + len;
				int[] tmp = new int[len];
				int n = 0;
				while (r.pos < stop) {
					tmp[n++] = (int) r.varint();
				}
				geom = java.util.Arrays.copyOf(tmp, n);
			} else {
				r.skip(wire);
			}
		}
		if (geom != null) {
			parts(f, geom);
		}
		return f;
	}

	private static void parts(Feature f, int[] g) {
		int x = 0, y = 0;
		int i = 0;
		int[] cur = null;
		int n = 0;
		while (i < g.length) {
			int cmd = g[i] & 7;
			int count = g[i] >>> 3;
			i++;
			if (cmd == 1 || cmd == 2) {
				for (int c = 0; c < count && i + 1 < g.length; c++) {
					int dx = g[i], dy = g[i + 1];
					i += 2;
					x += (dx >>> 1) ^ -(dx & 1);
					y += (dy >>> 1) ^ -(dy & 1);
					if (cmd == 1) {
						if (cur != null && n >= 2) {
							f.parts.add(java.util.Arrays.copyOf(cur, n));
						}
						cur = new int[16];
						n = 0;
					}
					if (cur == null) {
						cur = new int[16];
						n = 0;
					}
					if (n + 2 > cur.length) {
						cur = java.util.Arrays.copyOf(cur, cur.length * 2);
					}
					cur[n++] = x;
					cur[n++] = y;
				}
			} else if (cmd == 7) {
				// ClosePath: rings are closed implicitly by the rasteriser.
			} else {
				break;
			}
		}
		if (cur != null && n >= 2) {
			f.parts.add(java.util.Arrays.copyOf(cur, n));
		}
	}

	private static final class Reader {
		final byte[] buf;
		int pos;
		final int end;

		Reader(byte[] buf, int pos, int end) {
			this.buf = buf;
			this.pos = pos;
			this.end = end;
		}

		boolean more() {
			return pos < end;
		}

		long varint() {
			long v = 0;
			int shift = 0;
			while (pos < end) {
				int b = buf[pos++] & 0xFF;
				v |= (long) (b & 0x7F) << shift;
				if (b < 0x80) {
					break;
				}
				shift += 7;
			}
			return v;
		}

		int fixed32() {
			int v = (buf[pos] & 0xFF) | (buf[pos + 1] & 0xFF) << 8 | (buf[pos + 2] & 0xFF) << 16 | (buf[pos + 3] & 0xFF) << 24;
			pos += 4;
			return v;
		}

		long fixed64() {
			long lo = fixed32() & 0xFFFFFFFFL;
			long hi = fixed32() & 0xFFFFFFFFL;
			return lo | hi << 32;
		}

		String string() {
			int len = (int) varint();
			String s = new String(buf, pos, len, StandardCharsets.UTF_8);
			pos += len;
			return s;
		}

		void skip(int wire) {
			switch (wire) {
				case 0 -> varint();
				case 1 -> pos += 8;
				case 2 -> {
					int len = (int) varint();
					pos += len;
				}
				case 5 -> pos += 4;
				default -> pos = end;
			}
		}
	}
}
