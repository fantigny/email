package net.anfoya.java.net.filtered.easylist.cache;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Element<E> implements Serializable {
	private final E value;
	private int count = 1;
	public Element(final E value) {
		this.value = value;
	}
	public E getValue() {
		count++;
		return value;
	}
	public int getCount() {
		return count;
	}
	public void divideCount(final double divisor) {
		count /= divisor;
	}
}