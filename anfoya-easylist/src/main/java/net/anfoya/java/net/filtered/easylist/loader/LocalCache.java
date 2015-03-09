package net.anfoya.java.net.filtered.easylist.loader;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import net.anfoya.java.io.SerializedFile;
import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalCache<E> implements Set<E> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LocalCache.class);

	private final Map<E, Integer> map;
	private final Set<E> delegate;

	private final SerializedFile<Map<E, Integer>> file;

	private final int limit;
	private final int chunk;

	private boolean cleaning;

	public LocalCache(final String name, final int limit) {
		this.map = new ConcurrentHashMap<E, Integer>(limit);
		this.delegate = map.keySet();

		this.file = new SerializedFile<Map<E,Integer>>(System.getProperty("java.io.tmpdir") + "/" + name + ".bin");

		this.limit = limit;
		this.chunk = (int) (limit / 10.0);
	}

	@Override
	public boolean add(final E k) {
		boolean add;
		synchronized(map) {
			add = null == map.put(k, map.getOrDefault(k, 0) + 1);
		}
		if (add) {
			clean();
		}
		return add;
	}

	@Override
	public boolean contains(final Object o) {
		return map.keySet().contains(o);
	}

	public void load() {
		try {
			map.putAll(file.load());
			LOGGER.info("loaded {} URLs", size());
		} catch (ClassNotFoundException | IOException e) {
			clear();
			LOGGER.warn("loading cache", file.getName(), e);
		}
	}

	public void save() {
		try {
			LOGGER.info("saving {} URLs", size());
			file.save(map);
		} catch (final IOException e) {
			LOGGER.warn("saving cache {}", file.getName(), e);
		}
	}

	private void clean() {
		if (size() > limit) {
			ThreadPool.getInstance().submit(new Runnable() {
				@Override
				public void run() {
					try {
						synchronized(this) {
							if (cleaning) {
								return;
							} else {
								cleaning = true;
							}
						}
						cleanAsync();
					} finally {
						cleaning = false;
					}
				}
			});
		}
	}

	private void cleanAsync() {
		int threshold = 0;
		int removed = 0;
		int max_count = 0;
		while(map.size() > 0 && removed < chunk) {
			threshold++;
			LOGGER.info("cleaning \"{}\" with threshold {}", file.getName(), threshold);
			for (final E key : map.keySet()) {
				final int count = map.get(key);
				if (count > max_count) {
					max_count = count;
				}
				if (map.get(key) < threshold) {
					map.remove(key);
					removed++;
				}
			}
		}
		if (max_count > 100) {
			LOGGER.info("reducing count for \"{}\"", file.getName());
			for(final E key: map.keySet()) {
				synchronized(map) {
					map.put(key, Math.min(1, map.get(key) / 10));
				}
			}
		}
	}

	@Override
	public void forEach(final Consumer<? super E> action) {
		delegate.forEach(action);
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
	public Iterator<E> iterator() {
		return delegate.iterator();
	}

	@Override
	public Object[] toArray() {
		return delegate.toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		return delegate.toArray(a);
	}

	@Override
	public boolean remove(final Object o) {
		return delegate.remove(o);
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		return delegate.containsAll(c);
	}

	@Override
	public boolean addAll(final Collection<? extends E> c) {
		return delegate.addAll(c);
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		return delegate.retainAll(c);
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		return delegate.removeAll(c);
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public boolean equals(final Object o) {
		return delegate.equals(o);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public Spliterator<E> spliterator() {
		return delegate.spliterator();
	}

	@Override
	public boolean removeIf(final Predicate<? super E> filter) {
		return delegate.removeIf(filter);
	}

	@Override
	public Stream<E> stream() {
		return delegate.stream();
	}

	@Override
	public Stream<E> parallelStream() {
		return delegate.parallelStream();
	}
}
