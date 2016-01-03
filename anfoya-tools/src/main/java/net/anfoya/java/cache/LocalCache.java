package net.anfoya.java.cache;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.anfoya.java.io.SerializedFile;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.java.util.concurrent.ThreadPool;

public class LocalCache<K, V> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LocalCache.class);
	private static final String NAME_FORMAT = "<%s, %d>";
	private static final String FILENAME_FORMAT = System.getProperty("java.io.tmpdir") + File.separatorChar + "cache_%s.bin";

	private static final double DEFAULT_CHUNK_FACTOR = 0.1; // 10%
	private static final double DEFAULT_COUNT_DIVIDOR = 10;

	private final String name;
	private final int limit;

	private final Map<K, AutoCountElement<V>> map;
	private final SerializedFile<Map<K, AutoCountElement<V>>> file;

	private final AtomicBoolean cleaning;
	private final int chunkSize;
	private final int countMax;
	private final double countDividor;

	public LocalCache(final String name, final int limit) {
		this(name, limit, DEFAULT_CHUNK_FACTOR, DEFAULT_COUNT_DIVIDOR);
	}

	public LocalCache(final String name, final int limit, final double chunkFactor, final double countDividor) {
		this.name = String.format(NAME_FORMAT, name, limit);
		this.limit = limit;

		this.map = new ConcurrentHashMap<K, AutoCountElement<V>>();
		this.file = new SerializedFile<Map<K, AutoCountElement<V>>>(String.format(FILENAME_FORMAT, name));

		this.cleaning = new AtomicBoolean(false);
		this.chunkSize = (int) (limit * chunkFactor) - 1;
		this.countMax = (int) (limit * chunkFactor) - 1;
		this.countDividor = countDividor;
	}

	public V put(final K k, final V v) {
		final AutoCountElement<V> e = map.putIfAbsent(k, new AutoCountElement<V>(v));
		final V previous = e == null? null: e.getValue();
		if (previous == null) {
			clean();
		}
		return previous;
	}

	public void load() {
		try {
			map.putAll(file.load());
			LOGGER.info("{} {} URLs loaded", name, map.size());
		} catch (ClassNotFoundException | IOException e) {
			clear();
			LOGGER.warn("loading {} \"{}\"", name, e.getMessage());
		}
	}

	public void save() {
		try {
			LOGGER.info("{} saving {} URLs", name, map.size());
			file.save(map);
		} catch (final IOException e) {
			LOGGER.warn("saving {}", name, e);
		}
	}

	private void clean() {
		if (map.size() > limit) {
			ThreadPool.getDefault().submit(PoolPriority.MIN, "cleaning cache " + name, () -> {
				if (!cleaning.getAndSet(true)) {
					try {
						cleanAsync();
					} catch(final Exception e) {
						LOGGER.error("{} cleaning", name, e);
					} finally {
						cleaning.set(false);
					}
				}
			});
		}
	}

	private void cleanAsync() {
		int threshold = 0;
		int removed = 0;
		int maxCount = 0;
		while(map.size() > 0 && removed < chunkSize) {
			threshold++;
			for (final Entry<K, AutoCountElement<V>> entry: map.entrySet()) {
				final int count = entry.getValue().getCount();
				if (count > maxCount) {
					maxCount = count;
				}
				if (count < threshold) {
					map.remove(entry.getKey());
					removed++;
				}
				if (removed > chunkSize) {
					break;
				}
			}
		}
		LOGGER.info("{} cleaned (hit < {})", name, threshold);
		if (maxCount > countMax) {
			LOGGER.info("{} reducing count by {}", name, countDividor);
			for(final AutoCountElement<V> e: map.values()) {
				e.divideCount(countDividor);
			}
		}
	}

	public boolean isOlder(final int field, final int value) {
		return file.isOlder(field, value);
	}

	public V get(final K key) {
		final AutoCountElement<V> e = map.get(key);
		return e == null? null: e.getValue();
	}

	public void clear() {
		map.clear();
	}

	public int size() {
		return map.size();
	}
}
