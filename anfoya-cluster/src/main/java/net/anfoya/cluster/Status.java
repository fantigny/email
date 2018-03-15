package net.anfoya.cluster;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Status implements Serializable {
	private final String type;
	private final String value;
	private final String username;
	public Status(final String type, final String value) {
		super();
		this.type = type;
		this.value = value;
		this.username = System.getProperty("user.name", "ukn");
	}
	@Override
	public String toString() {
		return "[" + type + ", " + value + ", " + username + "]";
	}
	public Status copyWithValue(final String value) {
		return new Status(type, value);
	}
	public Status copyWithValue(final long value) {
		return new Status(type, Long.toString(value));
	}
	public String getType() {
		return type;
	}
	public String getValue() {
		return value;
	}
	public long getLongValue() {
		return Long.parseLong(value);
	}
	public String getUsername() {
		return username;
	}
}
