package net.anfoya.java.net.filtered.engine;


public interface RuleSet {
	void load();
	void setWithException(boolean withException);
	boolean isWithException();
	boolean matchesException(String url);
	boolean matchesExclusion(String url);
}
