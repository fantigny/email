package net.anfoya.java.net.url.filter;


public interface RuleSet {
	void load();
	void setWithException(boolean withException);
	boolean isWithException();
	boolean matchesException(String url);
	boolean matchesExclusion(String url);
}
