package com.stasdoto.medieval.earth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;

/**
 * Decoded tiles in memory: the most recently used ones are kept, each tile is built only once even when several world
 * generation threads ask for it together, and every thread keeps its last few tiles at hand without locking.
 */
public final class TileCache<T> {
	private final int capacity;
	private final LongFunction<T> loader;
	private final LinkedHashMap<Long, T> lru;
	private final ConcurrentHashMap<Long, CompletableFuture<T>> loading = new ConcurrentHashMap<>();
	private final ThreadLocal<long[]> localKeys = ThreadLocal.withInitial(() -> new long[]{Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE});
	private final ThreadLocal<Object[]> localVals = ThreadLocal.withInitial(() -> new Object[4]);
	private volatile int generation;
	private final ThreadLocal<int[]> localGen = ThreadLocal.withInitial(() -> new int[1]);

	public TileCache(int capacity, LongFunction<T> loader) {
		this.capacity = capacity;
		this.loader = loader;
		this.lru = new LinkedHashMap<>(capacity * 2, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<Long, T> e) {
				return size() > TileCache.this.capacity;
			}
		};
	}

	public static long key(int z, int x, int y) {
		return ((long) z << 58) | ((long) x << 29) | (long) y;
	}

	@SuppressWarnings("unchecked")
	public T get(long key) {
		long[] ks = localKeys.get();
		Object[] vs = localVals.get();
		int[] g = localGen.get();
		if (g[0] != generation) {
			g[0] = generation;
			java.util.Arrays.fill(ks, Long.MIN_VALUE);
			java.util.Arrays.fill(vs, null);
		}
		for (int i = 0; i < ks.length; i++) {
			if (ks[i] == key) {
				return (T) vs[i];
			}
		}
		T v = getShared(key);
		System.arraycopy(ks, 0, ks, 1, ks.length - 1);
		System.arraycopy(vs, 0, vs, 1, vs.length - 1);
		ks[0] = key;
		vs[0] = v;
		return v;
	}

	private T getShared(long key) {
		synchronized (lru) {
			T v = lru.get(key);
			if (v != null) {
				return v;
			}
		}
		CompletableFuture<T> mine = new CompletableFuture<>();
		CompletableFuture<T> f = loading.putIfAbsent(key, mine);
		if (f == null) {
			try {
				T v = loader.apply(key);
				synchronized (lru) {
					lru.put(key, v);
				}
				mine.complete(v);
				return v;
			} catch (RuntimeException e) {
				mine.completeExceptionally(e);
				throw e;
			} finally {
				loading.remove(key);
			}
		}
		return f.join();
	}

	/** Forgets everything (e.g. when a world is closed). */
	public void clear() {
		synchronized (lru) {
			lru.clear();
		}
		generation++;
	}
}
