package net.anfoya.tools.net.filtered;

import java.net.URL;

public interface UrlFilter {
	void loadRules();
	boolean matches(URL url);
	void setWithException(boolean withException);
	boolean isWithException();
}
