package net.anfoya.mail.gmail;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;

public class LabelService {
	private static final Logger LOGGER = LoggerFactory.getLogger(LabelService.class);

	private final Map<String, Label> idLabels = new ConcurrentHashMap<String, Label>();
	private final Gmail gmail;
	private final String user;
	
	public LabelService(final Gmail gmail, final String user) {
		this.gmail = gmail;
		this.user = user;
	}

	protected Collection<Label> getAll() throws LabelException {		
		if (idLabels.isEmpty()) {
			try {
				for(final Label l: gmail.users().labels().list(user).execute().getLabels()) {
					idLabels.put(l.getId(), l);
				}
			} catch (final IOException e) {
				throw new LabelException("getting labels", e);
			}
			LOGGER.debug("get labels: {}", idLabels.values());
		}
		return idLabels.values();
	}

	protected Label get(final String id) throws LabelException {
		getAll();
		return idLabels.get(id);
	}

	protected Label rename(Label label, final String name) throws LabelException {
		try {
			String newName = label.getName();
			if (newName.contains("/")) {
				newName = newName.substring(0, newName.lastIndexOf("/"));
			} else {
				newName = "";
			}
			newName += name;
			label.setName(newName);
			label = gmail.users().labels().update(user, label.getId(), label).execute();
			idLabels.put(label.getId(), label);			
			return label;
		} catch (final IOException e) {
			throw new LabelException("renaming " + label.getName() + " to " + name, e);
		}
	}

	protected Label add(Label label) throws LabelException {
		try {
			label = gmail.users().labels().create(user, label).execute();
			idLabels.put(label.getId(), label);
			return label;
		} catch (final IOException e) {
			throw new LabelException("adding " + label.getName(), e);
		}
	}

	protected void remove(final Label label) throws LabelException {
		try {
			gmail.users().labels().delete(user, label.getId());
			idLabels.remove(label.getId());
		} catch (final IOException e) {
			throw new LabelException("removing " + label.getName(), e);
		}
	}

}
