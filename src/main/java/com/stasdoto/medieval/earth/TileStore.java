package com.stasdoto.medieval.earth;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Downloads map tiles ({z}/{x}/{y}) once and keeps them on disk, so a place seen once works offline afterwards.
 * Safe to call from many world-generation threads at the same time; at most {@link #PARALLEL} downloads run at once.
 */
public final class TileStore {
	public static final int PARALLEL = 8;
	private static final byte[] NONE = new byte[0];
	private static volatile HttpClient http;
	private static final Semaphore SLOTS = new Semaphore(PARALLEL);
	public static final AtomicLong DOWNLOADED = new AtomicLong();
	public static final AtomicLong FAILED = new AtomicLong();
	/** False when the game is told to stay offline: only tiles already on disk are used. */
	public static volatile boolean online = true;

	private final String name;
	private final Path dir;
	private final String ext;
	private volatile String urlTemplate;

	public TileStore(String name, Path root, String ext, String urlTemplate) {
		this.name = name;
		this.dir = root.resolve(name);
		this.ext = ext;
		this.urlTemplate = urlTemplate;
	}

	public void setUrlTemplate(String t) {
		this.urlTemplate = t;
	}

	public String urlTemplate() {
		return urlTemplate;
	}

	public Path file(int z, int x, int y) {
		return dir.resolve(z + "/" + x + "/" + y + "." + ext);
	}

	public boolean cached(int z, int x, int y) {
		Path f = file(z, x, y);
		return Files.exists(f) || Files.exists(Path.of(f + ".none"));
	}

	/**
	 * The tile's bytes, an empty array when the server has no such tile, or null when it could not be fetched (offline, or
	 * the server kept failing).
	 */
	/**
	 * Downloads run on their own threads: waiting for the network inside a world-generation worker would let that worker
	 * pick up other chunk tasks meanwhile (work stealing), which can end up waiting for the very tile it is fetching.
	 */
	private static final java.util.concurrent.ExecutorService NET = java.util.concurrent.Executors.newFixedThreadPool(PARALLEL, r -> {
		Thread t = new Thread(r, "medieval-download");
		t.setDaemon(true);
		return t;
	});

	public byte[] get(int z, int x, int y) {
		Path f = file(z, x, y);
		try {
			if (Files.exists(f)) {
				return Files.readAllBytes(f);
			}
			if (Files.exists(Path.of(f + ".none"))) {
				return NONE;
			}
		} catch (IOException e) {
			EarthLog.warn.accept("cannot read " + f + ": " + e);
		}
		if (!online || urlTemplate == null) {
			return null;
		}
		java.util.concurrent.FutureTask<byte[]> task = new java.util.concurrent.FutureTask<>(() -> fetch(z, x, y));
		NET.execute(task);
		try {
			// FutureTask.get parks the thread plainly (no work stealing).
			return task.get(150, java.util.concurrent.TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
			EarthLog.warn.accept(name + " tile " + z + "/" + x + "/" + y + ": " + e);
			FAILED.incrementAndGet();
			return null;
		}
	}

	private byte[] fetch(int z, int x, int y) {
		Path f = file(z, x, y);
		try {
			if (Files.exists(f)) {
				return Files.readAllBytes(f);
			}
			if (Files.exists(Path.of(f + ".none"))) {
				return NONE;
			}
		} catch (IOException e) {
			EarthLog.warn.accept("cannot read " + f + ": " + e);
		}
		if (!online || urlTemplate == null) {
			return null;
		}
		String url = urlTemplate.replace("{z}", Integer.toString(z)).replace("{x}", Integer.toString(x)).replace("{y}", Integer.toString(y));
		for (int attempt = 0; attempt < 4; attempt++) {
			try {
				SLOTS.acquire();
				HttpResponse<byte[]> r;
				try {
					r = client().send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
							.header("User-Agent", "StasdotoMedieval/0.1 (Minecraft mod)").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
				} finally {
					SLOTS.release();
				}
				if (r.statusCode() == 404 || r.statusCode() == 204) {
					save(Path.of(f + ".none"), NONE);
					return NONE;
				}
				if (r.statusCode() == 200) {
					byte[] body = r.body();
					save(f, body);
					DOWNLOADED.incrementAndGet();
					return body;
				}
				EarthLog.warn.accept(name + " tile " + z + "/" + x + "/" + y + ": HTTP " + r.statusCode());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			} catch (IOException e) {
				if (attempt == 3) {
					EarthLog.warn.accept(name + " tile " + z + "/" + x + "/" + y + ": " + e);
				}
			}
			try {
				Thread.sleep(500L << attempt);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}
		FAILED.incrementAndGet();
		return null;
	}

	/** A small text/JSON document (not cached). */
	public static String fetchText(String url) throws IOException, InterruptedException {
		HttpResponse<String> r = client().send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
				.header("User-Agent", "StasdotoMedieval/0.1 (Minecraft mod)").GET().build(), HttpResponse.BodyHandlers.ofString());
		if (r.statusCode() != 200) {
			throw new IOException("HTTP " + r.statusCode());
		}
		return r.body();
	}

	private static void save(Path f, byte[] body) {
		try {
			Files.createDirectories(f.getParent());
			Path tmp = f.resolveSibling(f.getFileName() + "." + Thread.currentThread().threadId() + ".part");
			Files.write(tmp, body);
			Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			EarthLog.warn.accept("cannot save " + f + ": " + e);
		}
	}

	private static HttpClient client() {
		HttpClient c = http;
		if (c == null) {
			synchronized (TileStore.class) {
				c = http;
				if (c == null) {
					c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
					http = c;
				}
			}
		}
		return c;
	}
}
