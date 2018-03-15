package net.anfoya.io;

import java.net.MalformedURLException;
import java.net.URL;

import net.anfoya.io.dependant.MacFile;
import net.anfoya.io.dependant.UnxFile;
import net.anfoya.io.dependant.WinFile;
import net.anfoya.java.util.system.OperatingSystem;

public class SmbFileExtFactory {
	private static final OperatingSystem.Family OS_FAMILY = OperatingSystem.getInstance().getFamily();

	private SmbFileExtFactory() {}

	public static SmbFileExt getFile(final URL url) {
		switch(OS_FAMILY) {
		case MAC:
			return new MacFile(url);
		case WIN:
			return new WinFile(url);
		case UNX:
			return new UnxFile(url);
		case UKN:
			return new WinFile(url);
		}
		return null;
	}

	public static SmbFileExt getFile(final String url) throws MalformedURLException {
		switch(OS_FAMILY) {
		case MAC:
			return new MacFile(url);
		case WIN:
			return new WinFile(url);
		case UNX:
			return new UnxFile(url);
		case UKN:
			return new WinFile(url);
		}
		return null;
	}
}
