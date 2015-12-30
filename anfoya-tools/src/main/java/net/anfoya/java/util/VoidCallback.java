package net.anfoya.java.util;

@FunctionalInterface
public interface VoidCallback<P> {
	public void call(P p);
}
