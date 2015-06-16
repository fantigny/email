package net.anfoya.mail.gmail.service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.anfoya.java.io.SerializedFile;
import net.anfoya.mail.gmail.cache.CacheData;
import net.anfoya.mail.gmail.cache.CacheException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;

public class LabelService {
	private static final Logger LOGGER = LoggerFactory.getLogger(LabelService.class);
	private static final String FILE_PREFIX = System.getProperty("java.io.tmpdir") + "/fsm-cache-id-labels-";

	private final Map<String, Label> idLabels;
	private final Gmail gmail;
	private final String user;

	public LabelService(final Gmail gmail, final String user) {
		this.gmail = gmail;
		this.user = user;

		idLabels = new ConcurrentHashMap<String, Label>();
		try {
			for(final Entry<String, CacheData<Label>> entry: new SerializedFile<Map<String, CacheData<Label>>>(FILE_PREFIX + user).load().entrySet()) {
				idLabels.put(entry.getKey(), entry.getValue().getData());
			}
		} catch (ClassNotFoundException | IOException | CacheException e) {
			idLabels.clear();
		}
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final Map<String, CacheData<Label>> map = new HashMap<String, CacheData<Label>>();
					for(final Entry<String, Label> entry: idLabels.entrySet()) {
						map.put(entry.getKey(), new CacheData<Label>(entry.getValue()));
					}
					new SerializedFile<Map<String, CacheData<Label>>>(FILE_PREFIX + user).save(map);
				} catch (final IOException e) {
					LOGGER.error("saving history id", e);
				}
			}
		}));
	}

	public Collection<Label> getAll() throws LabelException {
		if (idLabels.isEmpty()) {
			try {
				for(final Label l: gmail.users().labels().list(user).execute().getLabels()) {
					idLabels.put(l.getId(), l);
				}
			} catch (final IOException e) {
				throw new LabelException("getting labels", e);
			}
			LOGGER.debug("all labels: {}", idLabels.values());
		}
		return idLabels.values();
	}

	public Label get(final String id) throws LabelException {
		getAll();
		return idLabels.get(id);
	}

	public Label rename(final String labelId, final String name) throws LabelException {
		try {
			final Label label = get(labelId);
			label.setName(name);
			label.setMessageListVisibility("show");
			label.setLabelListVisibility("labelShow");
			label.setType("user");
			gmail.users().labels().update(user, labelId, label).execute();
			return label;
		} catch (final IOException e) {
			throw new LabelException("renaming label (id: " + labelId + ") to \"" + name + "\"", e);
		}
	}

	public Label add(final String name) throws LabelException {
		try {
			final Label label = new Label();
			label.setMessageListVisibility("show");
			label.setLabelListVisibility("labelShow");
			label.setType("user");
			label.setName(name);
			final Label newLabel = gmail.users().labels().create(user, label).execute();
			label.setId(newLabel.getId());
			idLabels.put(label.getId(), label);
			return label;
		} catch (final IOException e) {
			throw new LabelException("adding \"" + name + "\"", e);
		}
	}

	public void remove(final String labelId) throws LabelException {
		try {
			idLabels.remove(labelId);
			gmail.users().labels().delete(user, labelId).execute();
		} catch (final IOException e) {
			throw new LabelException("removing \"" + labelId + "\"", e);
		}
	}

	public Label find(final String name) throws LabelException {
		for(final Label l: getAll()) {
			if (l.getName().equalsIgnoreCase(name)) {
				return l;
			}
		}
		return null;
	}

	public void clearCache() {
		idLabels.clear();
	}
}
