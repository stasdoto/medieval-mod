package com.stasdoto.medieval.settle;

import java.util.ArrayList;
import java.util.List;

/** The plan of one settlement: plots with their buildings, streets, walls, squares and the ring of fields. */
public final class Layout {
	public final Site site;
	public final List<Lot> lots = new ArrayList<>();
	public final List<Road> roads = new ArrayList<>();
	public final List<Wall> walls = new ArrayList<>();
	public final List<Square> squares = new ArrayList<>();
	/** Fields lie between these two distances from the centre. */
	public int fieldInner, fieldOuter;
	public int centerY;
	/** Bounding box of everything built (not the fields). */
	public int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

	public Layout(Site site) {
		this.site = site;
	}

	void grow(int x0, int z0, int x1, int z1) {
		minX = Math.min(minX, x0);
		minZ = Math.min(minZ, z0);
		maxX = Math.max(maxX, x1);
		maxZ = Math.max(maxZ, z1);
	}

	/** A plot: the rectangle x0..x1, z0..z1 (inclusive) in the world; {@code rot} turns the front (0 south, 1 west, 2 north, 3 east). */
	public static final class Lot {
		public final int x0, z0, x1, z1;
		public final int rot;
		public final int floor;
		public final BKind kind;
		public final long seed;
		/** Cached blocks of the building (filled by the builder). */
		public volatile Object built;

		Lot(int x0, int z0, int x1, int z1, int rot, int floor, BKind kind, long seed) {
			this.x0 = x0;
			this.z0 = z0;
			this.x1 = x1;
			this.z1 = z1;
			this.rot = rot;
			this.floor = floor;
			this.kind = kind;
			this.seed = seed;
		}

		public boolean touches(int ax0, int az0, int ax1, int az1, int margin) {
			return x1 + margin >= ax0 && x0 - margin <= ax1 && z1 + margin >= az0 && z0 - margin <= az1;
		}
	}

	public enum Surface {
		DIRT, GRAVEL, LOGS, COBBLE, SAND, PLANKS
	}

	/** A street or track: points x,z every few blocks. */
	public static final class Road {
		public final int[] pts;
		public final int width;
		public final Surface surface;
		public final int minX, minZ, maxX, maxZ;

		Road(int[] pts, int width, Surface surface) {
			this.pts = pts;
			this.width = width;
			this.surface = surface;
			int a = Integer.MAX_VALUE, b = Integer.MAX_VALUE, c = Integer.MIN_VALUE, d = Integer.MIN_VALUE;
			for (int i = 0; i < pts.length; i += 2) {
				a = Math.min(a, pts[i]);
				b = Math.min(b, pts[i + 1]);
				c = Math.max(c, pts[i]);
				d = Math.max(d, pts[i + 1]);
			}
			minX = a - width;
			minZ = b - width;
			maxX = c + width;
			maxZ = d + width;
		}

		/** Distance from a point to the road's centre line. */
		public double dist(double x, double z) {
			double best = Double.MAX_VALUE;
			for (int i = 0; i + 3 < pts.length; i += 2) {
				best = Math.min(best, segDist(x, z, pts[i], pts[i + 1], pts[i + 2], pts[i + 3]));
			}
			return best;
		}
	}

	public enum WallType {
		PALISADE, RAMPART, STONE, MUDBRICK
	}

	/** A ring wall around a centre: its distance from the centre for 64 directions; gates in some directions. */
	public static final class Wall {
		public final int cx, cz;
		public final float[] radius = new float[64];
		public final boolean[] gate = new boolean[64];
		public final WallType type;
		public final int height;
		/** True for the inner castle/citadel. */
		public final boolean citadel;

		Wall(int cx, int cz, WallType type, int height, boolean citadel) {
			this.cx = cx;
			this.cz = cz;
			this.type = type;
			this.height = height;
			this.citadel = citadel;
		}

		public double radiusAt(double angle) {
			double a = (angle / (2 * Math.PI) * 64 + 64) % 64;
			int i = (int) a;
			double f = a - i;
			return radius[i] * (1 - f) + radius[(i + 1) % 64] * f;
		}

		public boolean gateAt(double angle) {
			int i = (int) Math.round((angle / (2 * Math.PI) * 64 + 64)) % 64;
			return gate[i];
		}

		public double maxRadius() {
			double m = 0;
			for (float r : radius) {
				m = Math.max(m, r);
			}
			return m;
		}
	}

	/** A paved open place: a market square or the yard of a castle. */
	public static final class Square {
		public final int x0, z0, x1, z1;
		public final boolean market;

		Square(int x0, int z0, int x1, int z1, boolean market) {
			this.x0 = x0;
			this.z0 = z0;
			this.x1 = x1;
			this.z1 = z1;
			this.market = market;
		}
	}

	static double segDist(double px, double pz, double ax, double az, double bx, double bz) {
		double dx = bx - ax, dz = bz - az;
		double l2 = dx * dx + dz * dz;
		double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / l2));
		double qx = ax + t * dx - px, qz = az + t * dz - pz;
		return Math.sqrt(qx * qx + qz * qz);
	}
}
