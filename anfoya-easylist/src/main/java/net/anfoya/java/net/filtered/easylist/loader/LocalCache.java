package net.anfoya.java.net.filtered.easylist.loader;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import net.anfoya.java.io.SerializedFile;
import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class LocalCache<K, V> implements Serializable {
	private static final Logger LOGGER = LoggerFactory.getLogger(LocalCache.class);

	private class Element<E> implements Serializable {
		int count = 1;
		E v;
		public Element(final E e) {
			this.v = e;
		}
	}

	private final Map<K, Element<V>> delegate;

	private final SerializedFile<Map<K, Element<V>>> file;

	private final int limit;
	private final int chunk;

	private AtomicBoolean cleaning;

	public LocalCache(final String name, final int limit) {
		this.delegate = new ConcurrentHashMap<K, Element<V>>(limit);

		this.file = new SerializedFile<Map<K, Element<V>>>(System.getProperty("java.io.tmpdir") + "/" + name + ".bin");

		this.limit = limit;
		this.chunk = (int) (limit / 10.0);
	}

	public V put(final K k, final V v) {
		final V previous;
		synchronized(delegate) {
			final Element<V> e = delegate.putIfAbsent(k, new Element<V>(v));
			if (e != null) {
				e.count++;
				previous = e.v;
			} else {
				previous = null;
			}
		}
		if (previous == null) {
			clean();
		}
		return previous;
	}

	public void load() {
		try {
			delegate.putAll(file.load());
			LOGGER.info("loaded {} URLs", delegate.size());
		} catch (ClassNotFoundException | IOException e) {
			clear();
			LOGGER.warn("loading cache", file.getName(), e);
		}
	}

	public void save() {
		try {
			LOGGER.info("saving {} URLs", delegate.size());
			file.save(delegate);
		} catch (final IOException e) {
			LOGGER.warn("saving cache {}", file.getName(), e);
		}
	}

	private void clean() {
		if (delegate.size() > limit) {
			ThreadPool.getInstance().submit(new Runnable() {
				@Override
				public void run() {
					if (!cleaning.getAndSet(true)) {
						try {
							cleanAsync();
						} finally {
							cleaning.set(false);
						}
					}
				}
			});
		}
	}

	private void cleanAsync() {
		int threshold = 0;
		int removed = 0;
		int max_count = 0;
		while(delegate.size() > 0 && removed < chunk) {
			threshold++;
			LOGGER.info("cleaning \"{}\" with threshold {}", file.getName(), threshold);
			for (final Entry<K, Element<V>> entry: delegate.entrySet()) {
				final int count = entry.getValue().count;
				if (count > max_count) {
					max_count = count;
				}
				if (count < threshold) {
					delegate.remove(entry.getKey());
					removed++;
				}
			}
			LOGGER.info("removed {}", removed);
		}
		if (max_count > 100) {
			LOGGER.info("reducing count for \"{}\"", file.getName());
			for(final Element<V> e: delegate.values()) {
				synchronized(delegate) {
					e.count /= 10;
				}
			}
		}
	}

	public boolean isOlder(final int field, final int value) {
		return file.isOlder(field, value);
	}

	public V get(final K key) {
		final Element<V> e = delegate.get(key);
		if (e == null) {
			return null;
		} else {
			e.count++;
			return e.v;
		}
	}

	public void clear() {
		delegate.clear();
	}
}
