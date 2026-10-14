package com.stasdoto.medieval.earth;

/** Smooth 2D gradient noise (Perlin), deterministic, about -1..1. */
public final class Noise {
	private Noise() {
	}

	private static int hash(int x, int y, int seed) {
		int h = x * 0x27D4EB2D ^ y * 0x165667B1 ^ seed * 0x9E3779B9;
		h ^= h >>> 15;
		h *= 0x85EBCA6B;
		h ^= h >>> 13;
		h *= 0xC2B2AE35;
		h ^= h >>> 16;
		return h;
	}

	private static double grad(int h, double dx, double dy) {
		return switch (h & 7) {
			case 0 -> dx + dy;
			case 1 -> dx - dy;
			case 2 -> -dx + dy;
			case 3 -> -dx - dy;
			case 4 -> dx;
			case 5 -> -dx;
			case 6 -> dy;
			default -> -dy;
		};
	}

	private static double fade(double t) {
		return t * t * t * (t * (t * 6 - 15) + 10);
	}

	public static double at(double x, double y, int seed) {
		int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
		double fx = x - ix, fy = y - iy;
		double u = fade(fx), v = fade(fy);
		double a = grad(hash(ix, iy, seed), fx, fy);
		double b = grad(hash(ix + 1, iy, seed), fx - 1, fy);
		double c = grad(hash(ix, iy + 1, seed), fx, fy - 1);
		double d = grad(hash(ix + 1, iy + 1, seed), fx - 1, fy - 1);
		double ab = a + (b - a) * u;
		double cd = c + (d - c) * u;
		return (ab + (cd - ab) * v) * 0.7;
	}

	/** Several octaves, roughly -1..1. */
	public static double fbm(double x, double y, int seed, int octaves) {
		double sum = 0, amp = 1, norm = 0;
		for (int o = 0; o < octaves; o++) {
			sum += at(x, y, seed + o * 131) * amp;
			norm += amp;
			amp *= 0.5;
			x *= 2.03;
			y *= 2.03;
		}
		return sum / norm;
	}

	/** A random value 0..1 for a block. */
	public static double rand(int x, int y, int seed) {
		return (hash(x, y, seed) >>> 8) / (double) (1 << 24);
	}
}
