package net.anfoya.javafx.application;

import javafx.stage.Stage;

public class PlatformHelper {

	private static final boolean HEADLESS;
	static {
		boolean headless = false;
		try {
			new Stage();
		} catch (final ExceptionInInitializerError e) {
			headless = true;
		}
		HEADLESS = headless;
	}
	public static boolean isHeadless() {
		return HEADLESS;
	}
}
