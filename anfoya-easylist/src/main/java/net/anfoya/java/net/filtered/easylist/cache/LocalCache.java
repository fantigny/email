package net.anfoya.java.net.filtered.easylist.cache;

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

	private static final int COUNT_MAX = 100;
	private static final double COUNT_DIVIDOR = 10.0;

	private static final double CHUNK_PERCENT = 10.0;

	private final String name;
	private final int limit;
	private final int chunkSize;

	private final Map<K, Element<V>> delegate;
	private final SerializedFile<Map<K, Element<V>>> file;

	private AtomicBoolean cleaning;

	public LocalCache(final String name, final int limit) {
		this.name = "<" + name + ">";
		this.limit = limit;
		this.chunkSize = (int) (limit * CHUNK_PERCENT / 100.0);

		this.delegate = new ConcurrentHashMap<K, Element<V>>(limit);
		this.file = new SerializedFile<Map<K, Element<V>>>(System.getProperty("java.io.tmpdir") + "/" + name + ".bin");

	}

	public V put(final K k, final V v) {
		final V previous;
		synchronized(delegate) {
			final Element<V> e = delegate.putIfAbsent(k, new Element<V>(v));
			previous = e == null? null: e.getValue();
		}
		if (previous == null) {
			clean();
		}
		return previous;
	}

	public void load() {
		try {
			delegate.putAll(file.load());
			LOGGER.info("{} {} URLs loaded", name, delegate.size());
		} catch (ClassNotFoundException | IOException e) {
			clear();
			LOGGER.warn("loading {} \"{}\"", name, e.getMessage());
		}
	}

	public void save() {
		try {
			LOGGER.info("{} saving {} URLs", name, delegate.size());
			file.save(delegate);
		} catch (final IOException e) {
			LOGGER.warn("saving {}", name, e);
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
		int maxCount = 0;
		while(delegate.size() > 0 && removed < chunkSize) {
			threshold++;
			for (final Entry<K, Element<V>> entry: delegate.entrySet()) {
				final int count = entry.getValue().getCount();
				if (count < threshold) {
					delegate.remove(entry.getKey());
					removed++;
				}
				if (count > maxCount) {
					maxCount = count;
				}
			}
			LOGGER.info("{} cleaned {} elements (threshold {})", removed, threshold);
		}
		if (maxCount > COUNT_MAX) {
			LOGGER.info("{} reducing count", name);
			for(final Element<V> e: delegate.values()) {
				synchronized(delegate) {
					e.divideCount(COUNT_DIVIDOR);
				}
			}
		}
	}

	public boolean isOlder(final int field, final int value) {
		return file.isOlder(field, value);
	}

	public V get(final K key) {
		final Element<V> e = delegate.get(key);
		return e == null? null: e.getValue();
	}

	public void clear() {
		delegate.clear();
	}
}
