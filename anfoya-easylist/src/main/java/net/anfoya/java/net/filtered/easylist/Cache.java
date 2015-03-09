package net.anfoya.java.net.filtered.easylist;

import java.io.IOException;
import java.util.ArrayList;

import net.anfoya.java.io.SerializedFile;

@SuppressWarnings("serial")
public class Cache<K> extends ArrayList<K> {
	private final String name;
	private final int limit;
	private final int chunk;
	private boolean cleaning;
	public Cache(final String name, final int limit) {
		super();
		this.name = name;
		this.limit = limit;
		this.chunk = limit * 2 / 100;
	}

	public void load() {
		try {
			addAll(new SerializedFile<Cache<K>>(System.getProperty("java.io.tmpdir") + "/" + name + ".bin").load());
		} catch (ClassNotFoundException | IOException e) {
			clear();
		}
	}

	@Override
	public boolean add(final K e) {
		final boolean add;
		synchronized (this) {
			add = super.add(e);
		}
		if (add && size() > limit) {
			removeRange(0, chunk);
		}
		return add;
	}

	public void save() {
		try {
			new SerializedFile<Cache<K>>(System.getProperty("java.io.tmpdir") + "/" + name + ".bin").save(this);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
