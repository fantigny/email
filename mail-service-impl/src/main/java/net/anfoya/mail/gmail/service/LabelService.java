package net.anfoya.mail.gmail.service;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;

import net.anfoya.java.io.SerializedFile;
import net.anfoya.java.util.system.ShutdownHook;
import net.anfoya.mail.gmail.cache.CacheData;
import net.anfoya.mail.gmail.cache.CacheException;
import net.anfoya.mail.gmail.model.GmailTag;

public class LabelService {
	private static final Logger LOGGER = LoggerFactory.getLogger(LabelService.class);
	private static final String FILE_PREFIX = System.getProperty("java.io.tmpdir") + File.separatorChar + "fsm-cache-id-labels-";

	private final Map<String, Label> idLabels;
	private final Gmail gmail;
	private final String user;

	public LabelService(final Gmail gmail, final String user) {
		this.gmail = gmail;
		this.user = user;

		idLabels = new ConcurrentHashMap<>();
		try {
			for(final Entry<String, CacheData<Label>> entry: new SerializedFile<Map<String, CacheData<Label>>>(FILE_PREFIX + user).load().entrySet()) {
				idLabels.put(entry.getKey(), entry.getValue().getData());
			}
		} catch (ClassNotFoundException | IOException | CacheException e) {
			idLabels.clear();
		}

		new ShutdownHook(() -> {
			LOGGER.info("saving...");
			new SerializedFile<Map<String, CacheData<Label>>>(FILE_PREFIX + user)
				.save(idLabels.entrySet().stream().collect(Collectors.toMap(
					e -> e.getKey(),
					e -> new CacheData<>(e.getValue()))));
		});
	}

	public Set<Label> getAll() throws LabelException {
		if (idLabels.isEmpty()) {
			try {
				for(final Label l: gmail.users().labels().list(user).execute().getLabels()) {
					idLabels.put(l.getId(), l);
				}
			} catch (final IOException e) {
				throw new LabelException("get labels", e);
			}
			LOGGER.debug("all labels: {}", idLabels.values());
		}
		return new HashSet<>(idLabels.values());
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
			throw new LabelException("rename (id: " + labelId + ") to \"" + name + "\"", e);
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
			throw new LabelException("add \"" + name + "\"", e);
		}
	}

	public void remove(final String labelId) throws LabelException {
		try {
			idLabels.remove(labelId);
			gmail.users().labels().delete(user, labelId).execute();
		} catch (final IOException e) {
			throw new LabelException("remove \"" + labelId + "\"", e);
		}
	}

	public void hide(final String labelId) throws LabelException {
		if (!idLabels.containsKey(labelId)) {
			return;
		}

		try {
			final Label label = idLabels.get(labelId);
			label.setLabelListVisibility("labelHide");
			if (!"system".equals(label.getType())) {
				label.setMessageListVisibility("hide");
			}
			gmail.users().labels().update(user, labelId, label).execute();
		} catch (final IOException e) {
			throw new LabelException("hide \"" + labelId + "\"", e);
		}
	}

	public void show(final String labelId) throws LabelException {
		if (!idLabels.containsKey(labelId)) {
			return;
		}

		try {
			final Label label = idLabels.get(labelId);
			label.setLabelListVisibility("labelShow");
			if (!"system".equals(label.getType())) {
				label.setMessageListVisibility("show");
			}
			gmail.users().labels().update(user, labelId, label).execute();
		} catch (final IOException e) {
			throw new LabelException("show \"" + labelId + "\"", e);
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
		new SerializedFile<Map<String, CacheData<Label>>>(FILE_PREFIX + user).clear();
	}

	public void clean(com.google.api.services.gmail.model.Thread t) {
		// clean labels
		t.getMessages()
			.stream()
			.filter(m -> m.getLabelIds() != null)
			.forEach(m -> {
				m.setLabelIds(m.getLabelIds()
						.stream()
						.filter(id -> {
							try {
								return !GmailTag.isHidden(get(id));
							} catch (final LabelException e) {
								return false;
							}
						})
						.collect(Collectors.toList()));
			});
	}
}
