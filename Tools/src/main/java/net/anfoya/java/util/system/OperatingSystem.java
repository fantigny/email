package net.anfoya.java.util.system;


public final class OperatingSystem {
	// singleton
	private final static OperatingSystem OPERATING_SYSTEM = new OperatingSystem();
	public static OperatingSystem getInstance() {
		return OPERATING_SYSTEM;
	}

	public enum Family { WIN, MAC, UNX, UKN }

	private final Family family;

	protected OperatingSystem(String name) {
		name = name.toLowerCase();
		if (name.contains("win")) {
			family = Family.WIN;
		} else if (name.contains("mac")) {
			family = Family.MAC;
		} else if (name.contains("nux") || name.contains("nix")) {
			family = Family.UNX;
		} else {
			family = Family.UKN;
		}
	}

	private OperatingSystem() {
		this(System.getProperty("os.name"));
	}

	public Family getFamily() {
		return family;
	}
}
