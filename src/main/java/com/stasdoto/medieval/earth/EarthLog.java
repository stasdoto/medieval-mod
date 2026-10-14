package com.stasdoto.medieval.earth;

import java.util.function.Consumer;

/** Logging for the plain-Java earth code (wired to the mod logger in the game, to stderr in the preview tool). */
public final class EarthLog {
	private EarthLog() {
	}

	public static volatile Consumer<String> info = s -> System.out.println("[medieval] " + s);
	public static volatile Consumer<String> warn = s -> System.err.println("[medieval] WARN " + s);
}
