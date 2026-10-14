package com.stasdoto.medieval.earth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * A small PNG reader (8-bit greyscale / RGB / RGBA / palette, not interlaced) - enough for the elevation tiles, and free of
 * the desktop graphics classes, which are not always present in a game runtime.
 */
public final class Png {
	public final int width;
	public final int height;
	/** Packed 0xRRGGBB per pixel, row by row. */
	public final int[] rgb;

	private Png(int width, int height, int[] rgb) {
		this.width = width;
		this.height = height;
		this.rgb = rgb;
	}

	public static Png decode(byte[] data) throws IOException {
		ByteBuffer b = ByteBuffer.wrap(data);
		if (data.length < 8 || b.getLong() != 0x89504E470D0A1A0AL) {
			throw new IOException("not a PNG");
		}
		int w = 0, h = 0, depth = 0, type = 0, interlace = 0;
		int[] palette = null;
		ByteArrayOutputStream idat = new ByteArrayOutputStream(data.length);
		while (b.remaining() >= 12) {
			int len = b.getInt();
			int t = b.getInt();
			if (len < 0 || len > b.remaining() - 4) {
				throw new IOException("bad chunk");
			}
			int start = b.position();
			switch (t) {
				case 0x49484452 -> { // IHDR
					w = b.getInt();
					h = b.getInt();
					depth = b.get() & 0xFF;
					type = b.get() & 0xFF;
					b.get();
					b.get();
					interlace = b.get() & 0xFF;
				}
				case 0x504C5445 -> { // PLTE
					palette = new int[len / 3];
					for (int i = 0; i < palette.length; i++) {
						palette[i] = ((data[start + i * 3] & 0xFF) << 16) | ((data[start + i * 3 + 1] & 0xFF) << 8) | (data[start + i * 3 + 2] & 0xFF);
					}
				}
				case 0x49444154 -> idat.write(data, start, len); // IDAT
				default -> {
				}
			}
			b.position(start + len + 4);
			if (t == 0x49454E44) { // IEND
				break;
			}
		}
		if (depth != 8 || interlace != 0) {
			throw new IOException("unsupported PNG (depth " + depth + ", interlace " + interlace + ")");
		}
		int channels = switch (type) {
			case 0 -> 1;
			case 2 -> 3;
			case 3 -> 1;
			case 4 -> 2;
			case 6 -> 4;
			default -> throw new IOException("unsupported colour type " + type);
		};
		int stride = w * channels;
		byte[] raw = new byte[(stride + 1) * h];
		Inflater inf = new Inflater();
		inf.setInput(idat.toByteArray());
		try {
			int off = 0;
			while (off < raw.length && !inf.finished()) {
				int n = inf.inflate(raw, off, raw.length - off);
				if (n == 0 && (inf.needsInput() || inf.needsDictionary())) {
					break;
				}
				off += n;
			}
		} catch (DataFormatException e) {
			throw new IOException(e);
		} finally {
			inf.end();
		}
		byte[] cur = new byte[stride];
		byte[] prev = new byte[stride];
		int[] out = new int[w * h];
		for (int y = 0; y < h; y++) {
			int base = y * (stride + 1);
			int filter = raw[base] & 0xFF;
			for (int i = 0; i < stride; i++) {
				int x = raw[base + 1 + i] & 0xFF;
				int a = i >= channels ? cur[i - channels] & 0xFF : 0;
				int up = prev[i] & 0xFF;
				int c = i >= channels ? prev[i - channels] & 0xFF : 0;
				int v = switch (filter) {
					case 1 -> x + a;
					case 2 -> x + up;
					case 3 -> x + ((a + up) >> 1);
					case 4 -> x + paeth(a, up, c);
					default -> x;
				};
				cur[i] = (byte) v;
			}
			for (int px = 0; px < w; px++) {
				int i = px * channels;
				int r, g, bl;
				switch (type) {
					case 0, 4 -> r = g = bl = cur[i] & 0xFF;
					case 3 -> {
						int p = palette == null ? 0 : palette[cur[i] & 0xFF];
						r = p >> 16 & 0xFF;
						g = p >> 8 & 0xFF;
						bl = p & 0xFF;
					}
					default -> {
						r = cur[i] & 0xFF;
						g = cur[i + 1] & 0xFF;
						bl = cur[i + 2] & 0xFF;
					}
				}
				out[y * w + px] = r << 16 | g << 8 | bl;
			}
			byte[] t = prev;
			prev = cur;
			cur = t;
		}
		return new Png(w, h, out);
	}

	private static int paeth(int a, int b, int c) {
		int p = a + b - c;
		int pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
		if (pa <= pb && pa <= pc) {
			return a;
		}
		return pb <= pc ? b : c;
	}
}
