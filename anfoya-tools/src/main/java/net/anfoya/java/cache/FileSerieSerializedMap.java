package net.anfoya.java.cache;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.anfoya.java.io.SerializedFile;

public class FileSerieSerializedMap<K extends Serializable, V extends Serializable> implements Map<K, V>{
	private static final Logger LOGGER = LoggerFactory.getLogger(FileSerieSerializedMap.class);

	private final int threshold;
	private final String dicoFilename;
	private final String filenamePattern;
	private final Map<K, V> delegate;
	private final List<K> dico;
	private final List<Boolean> loaded;
	private final List<Boolean> saved;
	private boolean saving;

	public FileSerieSerializedMap(final String filepathPrefix, final int fileCreationThreshold) {
		threshold = fileCreationThreshold;
		filenamePattern = filepathPrefix + "-%s";
		delegate = new HashMap<>();

		dicoFilename = String.format(filenamePattern, "dico");
		List<K> dico;
		try {
			dico = new SerializedFile<List<K>>(dicoFilename).load();
		} catch (ClassNotFoundException | IOException e) {
			dico = new ArrayList<>();
		}
		this.dico = dico;

		loaded = new ArrayList<>();
		saved = new ArrayList<>();
		for(int i=0, n=dico.size(); i<n; i++) {
			loaded.add(false);
			saved.add(true);
		}

		saving = false;

		Runtime.getRuntime().addShutdownHook(new Thread(() -> save()));
	}

	private synchronized void save() {
		if (saving) {
			return;
		}
		saving = true;
		LOGGER.info("saving...");

		try {
			new SerializedFile<List<K>>(dicoFilename).save(dico);

			for(int i=0, n=saved.size(); i<n; i++) {
				if (!saved.get(i)) {
					final Map<K, V> map = new HashMap<>();
					final int fileIndex = i / threshold;
					final int start = threshold * fileIndex;
					final int end = Math.min(start + threshold, saved.size());
					for(int j=start; j<end; j++) {
						final K k = dico.get(j);
						map.put(k, delegate.get(k));
						saved.set(j, true);
					}
					final String filename = String.format(filenamePattern, "" + fileIndex);
					new SerializedFile<Map<K, V>>(filename).save(map);
				}
			}
		} catch (final IOException e) {
			LOGGER.error("saving", e);
		} finally {
			saving = false;
		}
	}

	@Override
	public synchronized int size() {
		return dico.size();
	}

	@Override
	public synchronized boolean isEmpty() {
		return dico.isEmpty();
	}

	@Override
	public synchronized boolean containsKey(final Object key) {
		return dico.contains(key);
	}

	@Override
	public synchronized V get(final Object key) {
		final int keyIndex = dico.indexOf(key);
		if (keyIndex != -1 && !loaded.get(keyIndex)) {
			final String filename = String.format(filenamePattern, "" + keyIndex / threshold);
			try {
				LOGGER.debug("loading {}", filename);
				final Map<K, V> map = new SerializedFile<Map<K, V>>(filename).load();
				delegate.putAll(map);
				// mark all objects in this file as loaded and saved
				for(final K k: map.keySet()) {
					final int kIndex = dico.indexOf(k);
					if (kIndex == -1) {
						LOGGER.error("missing in dico: {}", k);
					} else {
						loaded.set(kIndex, true);
						saved.set(kIndex, true);
					}
				}
			} catch (ClassNotFoundException | IOException e) {
				LOGGER.error("loading {}", filename);
			}
		}

		return delegate.get(key);
	}

	@Override
	public synchronized V put(final K key, final V value) {
		final V previous = delegate.put(key, value);
		if (previous == null) {
			dico.add(key);
			loaded.add(true);
			saved.add(false);
		} else {
			final int index = dico.indexOf(key);
			loaded.set(index, true);
			saved.set(index, false);
		}

		return previous;
	}

	@Override
	public synchronized V remove(final Object key) {
		final V previous = delegate.remove(key);
		if (dico.contains(key)) {
			final int index = dico.indexOf(key);
			final int lastIndex = dico.size() - 1;
			if (index == lastIndex) {
				dico.remove(index);
				loaded.remove(index);
				saved.remove(index);
			} else {
				dico.set(index, dico.remove(lastIndex));
				loaded.set(index, loaded.remove(lastIndex) || true);
				saved.set(index, saved.remove(lastIndex) && false);
			}
		}

		return previous;
	}

	@Override
	public synchronized void putAll(final Map<? extends K, ? extends V> m) {
		for(final Entry<? extends K, ? extends V> entry: m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public synchronized void clear() {
		LOGGER.info("clearing...");

		delegate.clear();
		dico.clear();
		loaded.clear();
		saved.clear();

		new SerializedFile<List<K>>(dicoFilename).clear();
		int fileIndex = 0;
		for(;;) {
			final String filename = String.format(filenamePattern, "" + fileIndex);
			final File file = new SerializedFile<Map<K, V>>(filename);
			if (file.exists()) {
				LOGGER.info("deleting {}", filename);
				file.delete();
			} else {
				break;
			}
			fileIndex++;
		}
	}

	@Override
	public synchronized Set<K> keySet() {
		return Collections.unmodifiableSet(new HashSet<>(dico));
	}

	@Override
	public synchronized boolean containsValue(final Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}
}
