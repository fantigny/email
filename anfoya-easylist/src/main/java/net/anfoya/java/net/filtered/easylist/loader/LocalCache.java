package net.anfoya.java.net.filtered.easylist.loader;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.anfoya.java.io.SerializedFile;
import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class LocalCache<K, V> implements Map<K, V>, Serializable {
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

	@Override
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
			LOGGER.info("loaded {} URLs", size());
		} catch (ClassNotFoundException | IOException e) {
			clear();
			LOGGER.warn("loading cache", file.getName(), e);
		}
	}

	public void save() {
		try {
			LOGGER.info("saving {} URLs", size());
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

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(final Object key) {
		return delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(final Object value) {
		return delegate.containsValue(value);
	}

	@Override
	public V get(final Object key) {
		final Element<V> e = delegate.get(key);
		if (e == null) {
			return null;
		} else {
			e.count++;
			return e.v;
		}
	}

	@Override
	public V remove(final Object key) {
		final Element<V> e = delegate.remove(key);
		return e == null? null: e.v;
	}

	@Override
	public void putAll(final Map<? extends K, ? extends V> map) {
		for(final Entry<? extends K, ? extends V> entry: map.entrySet()) {
			delegate.put(entry.getKey(), new Element<V>(entry.getValue()));
		}
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public Set<K> keySet() {
		return delegate.keySet();
	}

	@Override
	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public V getOrDefault(final Object key, final V defaultValue) {
		return delegate.getOrDefault(key, new Element<V>(defaultValue)).v;
	}

	@Override
	public void forEach(final BiConsumer<? super K, ? super V> action) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V putIfAbsent(final K key, final V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(final Object key, final Object value) {
		throw new UnsupportedOperationException();
	}

	public boolean replace(final K key, final LocalCache<K, V>.Element<V> oldValue,
			final LocalCache<K, V>.Element<V> newValue) {
		throw new UnsupportedOperationException();
	}

	public LocalCache<K, V>.Element<V> replace(final K key,
			final LocalCache<K, V>.Element<V> value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V computeIfAbsent(
			final K key,
			final Function<? super K, ? extends V> mappingFunction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V computeIfPresent(
			final K key,
			final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V compute(
			final K key,
			final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException();
	}

	public V merge(
			final K key,
			final LocalCache<K, V>.Element<V> value,
			final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException();
	}
}
