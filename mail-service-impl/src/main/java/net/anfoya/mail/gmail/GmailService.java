package net.anfoya.mail.gmail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.extensions.Email;
import com.sun.mail.util.BASE64DecoderStream;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.util.Callback;
import javafx.util.Duration;
import net.anfoya.mail.gmail.model.GmailContact;
import net.anfoya.mail.gmail.model.GmailMessage;
import net.anfoya.mail.gmail.model.GmailMoreThreads;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.gmail.model.GmailThread;
import net.anfoya.mail.gmail.service.ConnectionService;
import net.anfoya.mail.gmail.service.ContactException;
import net.anfoya.mail.gmail.service.ContactService;
import net.anfoya.mail.gmail.service.HistoryException;
import net.anfoya.mail.gmail.service.HistoryService;
import net.anfoya.mail.gmail.service.LabelException;
import net.anfoya.mail.gmail.service.LabelService;
import net.anfoya.mail.gmail.service.MessageException;
import net.anfoya.mail.gmail.service.MessageService;
import net.anfoya.mail.gmail.service.ThreadException;
import net.anfoya.mail.gmail.service.ThreadService;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Tag;
import net.anfoya.tag.model.SpecialTag;

public class GmailService implements MailService<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact> {
	private static final Logger LOGGER = LoggerFactory.getLogger(GmailService.class);

	private static final String USER = "me";
	private static final String DEFAULT = "default";

	private static final Duration PULL_PERIOD = Duration.seconds(5);

	private static final GmailTag[] SYSTEM_TAG_ORDER = {
			GmailTag.INBOX
			, GmailTag.SOCIAL
			, GmailTag.PROMOTIONS
			, GmailTag.UPDATES
			, GmailTag.FORUMS
			, GmailTag.CHAT
			, GmailTag.UNREAD
			, GmailTag.STARRED
			, GmailTag.DRAFT
			, GmailTag.SENT
			, GmailTag.ALL
			, GmailTag.SPAM
			, GmailTag.TRASH
	};

	private ConnectionService connectionService;
	private LabelService labelService;
	private MessageService messageService;
	private ThreadService threadService;
	private HistoryService historyService;
	private ContactService contactService;

	private String address;

	@Override
	public void connect(final String appName) throws GMailException {
		connectionService = new ConnectionService(appName);
		final boolean connected = connectionService.connect();
		if (!connected) {
			return;
		}

		final ContactsService gcontact = connectionService.getGcontactService();
		final Gmail gmail = connectionService.getGmailService();

		try {
			address = gmail.users().getProfile(USER).execute().getEmailAddress();
		} catch (final IOException e) {
			address = "uknown!";
		}

		contactService = new ContactService(gcontact, DEFAULT).init();

		messageService = new MessageService(gmail, USER);
		threadService = new ThreadService(gmail, USER);
		labelService = new LabelService(gmail, USER);

		historyService = new HistoryService(gmail, USER);
		historyService.addOnUpdateLabel(() -> labelService.clearCache());
		historyService.start(PULL_PERIOD);
	}

	@Override
	public void disconnect() {
	    connectionService.disconnect();
	    historyService.stop();
	    clearCache();
	}

	@Override
	public void reconnect() throws GMailException {
		if (!disconnectedProperty().get()) {
			return;
		}

		try {
			historyService.getUpdates();
		} catch (final HistoryException e) {
			throw new GMailException("get updates to test connection", e);
		}
	}

	@Override
	public ReadOnlyBooleanProperty disconnectedProperty() {
		return historyService.disconnected();
	}

	@Override
	public GmailThread getThread(String id) throws GMailException {
		try {
			final Thread thread = threadService.get(Collections.singleton(id), true, null).iterator().next();
			return new GmailThread(thread);
		} catch (final ThreadException e) {
			throw new GMailException("get thread " + id, e);
		}
	}

	@Override
	public Set<GmailThread> findThreads(final Set<GmailTag> includes, final Set<GmailTag> excludes, final String pattern, final int pageMax) throws GMailException {
		try {
			final Set<GmailThread> threads = new LinkedHashSet<GmailThread>();
			if (includes.isEmpty() && pattern.isEmpty()) { //TODO && excludes.isEmpty()) {
				return threads;
			}
			final StringBuilder query = new StringBuilder("");
			if (includes.size() > 0) {
				final StringBuilder subQuery = new StringBuilder();
				for(final GmailTag t: includes) {
					if (subQuery.length() > 1) {
						subQuery.append(" AND ");
					}
					subQuery.append("label:").append(t.getPath().replaceAll("/", "-").replaceAll(" ", "-"));
				}
				query.append("(").append(subQuery).append(")");
			}
			if (excludes.size() > 0) {
				final StringBuilder subQuery = new StringBuilder();
				for(final GmailTag t: excludes) {
					if (subQuery.length() > 1) {
						subQuery.append(" AND ");
					}
					subQuery.append("-label:").append(t.getPath().replaceAll("/", "-").replaceAll(" ", "-"));
				}
				if (query.length() > 0) {
					query.append(" AND ");
				}
				query.append("(").append(subQuery).append(")");
			}
			if (!pattern.isEmpty()) {
				if (!includes.isEmpty() || !excludes.isEmpty()) {
					query.append(" AND ");
				}
				final String p = pattern.trim().replaceAll(" ", " OR ");
				query.append("(").append(p).append(")");
			}

			for(final Thread t: threadService.find(query.toString(), pageMax)) {
				// clean labels
				if (t.getMessages() == null) {
					LOGGER.error("no message for thread {}", t.toPrettyString());
				} else {
					for(final Message m: t.getMessages()) {
						final List<String> cleaned = new ArrayList<String>();
						if (m.getLabelIds() == null) {
							LOGGER.error("no label for message id: {}", m.getId());
						} else {
							for(final String id: m.getLabelIds()) {
								if (!GmailTag.isHidden(labelService.get(id))) {
									cleaned.add(id);
								}
							}
						}
						m.setLabelIds(cleaned);
					}
				}
				if (GmailThread.PAGE_TOKEN_ID.equals(t.getId())) {
					threads.add(new GmailMoreThreads(pageMax + 1));
				} else {
					threads.add(new GmailThread(t));
				}
			}

			return threads;
		} catch (final ThreadException | LabelException | IOException e) {
			throw new GMailException("find threads for includes=" + includes
					+ " excludes=" + excludes
					+ " pattern=" + pattern
					+ " pageMax=" + pageMax, e);
		}
	}

	@Override
	public GmailMessage getMessage(final String id) throws GMailException {
		try {
		    return new GmailMessage(messageService.getMessage(id));
		} catch (final MessageException | MessagingException e) {
			throw new GMailException("load message id: " + id, e);
		}
	}

	@Override
	public GmailMessage createDraft(final GmailMessage message) throws GMailException {
		try {
			final Draft draft = messageService.createDraft();
			if (message != null) {
				draft.setMessage(messageService.getMessage(message.getId()));
			}
			return new GmailMessage(draft);
		} catch (final MessageException | MessagingException e) {
			throw new GMailException("create draft", e);
		}
	}

	@Override
	public Set<GmailSection> getSections() throws GMailException {
		try {
			final Collection<Label> labels = labelService.getAll();
			final Set<GmailSection> alphaSections = new TreeSet<GmailSection>();
			for(final Label label: labels) {
				if (!GmailSection.isHidden(label)) {
					final GmailSection section = new GmailSection(label);
					final String sectionName = section.getName() + "/";
					for(final Label l: labels) {
						if (l.getName().contains(sectionName)) {
							alphaSections.add(section);
							break;
						}
					}
				}
			}

			final Set<GmailSection> sections = new LinkedHashSet<GmailSection>();
			sections.add(GmailSection.SYSTEM);
			sections.addAll(alphaSections);

			LOGGER.debug("get sections: {}", sections);
			return sections;
		} catch (final LabelException e) {
			throw new GMailException("get sections", e);
		}
	}

	@Override
	public Set<GmailSection> getHiddenSections() throws GMailException {
		try {
			final Collection<Label> labels = labelService.getAll();
			final Set<GmailSection> hiddenSections = new TreeSet<GmailSection>();
			for(final Label label: labels) {
				if (GmailSection.isHidden(label)) {
					final GmailSection section = new GmailSection(label);
					final String sectionName = section.getName() + "/";
					for(final Label l: labels) {
						if (l.getName().contains(sectionName)) {
							hiddenSections.add(section);
							break;
						}
					}
				}
			}

			LOGGER.debug("get hidden sections: {}", hiddenSections);
			return hiddenSections;
		} catch (final LabelException e) {
			throw new GMailException("get hidden sections", e);
		}
	}

	@Override
	public GmailTag getTag(final String id) throws GMailException {
		GmailTag tag = getSpecialTag(id);
		if (tag == null) {
			try {
				tag = new GmailTag(labelService.get(id));
			} catch (final LabelException e) {
				throw new GMailException("get tag " + id, e);
			}
		}
		return tag;
	}

	@Override
	public Set<GmailTag> getTags(final String pattern) throws GMailException {
		final Set<String> patterns = Arrays.asList(pattern.split(" "))
				.stream()
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.map(String::toLowerCase)
				.collect(Collectors.toSet());
		final Set<GmailTag> tags = new TreeSet<GmailTag>();
		Set<Label> labels;
		try {
			labels = labelService.getAll()
					.stream()
					.filter(l -> !GmailTag.isHidden(l))
					.collect(Collectors.toSet());
		} catch (final LabelException e) {
			throw new GMailException("get tags for pattern(s) " + patterns, e);
		}
		for(final Label l: labels) {
			final GmailTag tag = getTag(l.getId());
			final String tagName = tag.getName();
			for(final String p: patterns) {
				if (tagName.contains(p)) {
					tags.add(tag);
					break;
				}
			}
		}
		return tags;
	}

	@Override
	public Set<GmailTag> getHiddenTags() throws GMailException {
		try {
			final Set<GmailTag> tags = new TreeSet<GmailTag>();
			final Collection<Label> labels = labelService.getAll();
			for(final Label l: labels) {
				if (GmailTag.isHidden(l)) {
					if (!hasSubLabel(l, labels)) {
						tags.add(new GmailTag(l));
					}
				}
			}
			return tags;
		} catch (final LabelException e) {
			throw new GMailException("get hidden tags", e);
		}
	}

	@Override
	public Set<GmailTag> getTags(final GmailSection section) throws GMailException {
		try {
			final Set<GmailTag> tags;
			final Collection<Label> labels = labelService.getAll();
			if (GmailSection.SYSTEM.equals(section)) {
				final Set<GmailTag> systemTags = new HashSet<GmailTag>();
				systemTags.add(GmailTag.ALL);
				for(final Label label: labels) {
					final String name = label.getName();
					if (GmailTag.isHidden(label)) {
						continue;
					}
					if (GmailTag.isSystem(label)) {
						// GMail system tags
						GmailTag tag = getSpecialTag(label.getId());
						if (tag == null) {
							tag = new GmailTag(label);
						}
						systemTags.add(tag);
						continue;
					}
					if (!name.contains("/")) {
						// root tags, put them here if no sub-tag
						if (!hasSubLabel(label, labels)) {
							systemTags.add(new GmailTag(label));
						}
					}
				}
				tags = sortSystemTags(systemTags);
			} else {
				tags = new TreeSet<GmailTag>();
				for(final Label label: labels) {
					if (!GmailTag.isHidden(label) && !GmailTag.isSystem(label)) {
						final String name = label.getName();
						if (section != null && name.equals(section.getPath())) {
							tags.add(new GmailTag(label.getId(), Tag.THIS_NAME, label.getName(), true));
						} else {
							final int pos = name.lastIndexOf("/");
							if (pos > 0) {
								if (section == null || section.getName().equals(name.substring(0, pos))) {
									final String sectionName = name + "/";
									boolean isSection = false;
									for(final Label l: labels) {
										if (l.getName().contains(sectionName)) {
											isSection = true;
											break;
										}
									}
									if (!isSection) {
										tags.add(new GmailTag(label));
									}
								}
							}
						}
					}
				}
			}

			LOGGER.debug("tags for section({}) tagPattern({}): {}", section == null? "": section.getPath(), tags);
			return tags;
		} catch (final LabelException e) {
			throw new GMailException("get tags for section " + section.getName(), e);
		}
	}

	private boolean hasSubLabel(final Label label, final Collection<Label> labels) {
		final String name = label.getName();
		for(final Label l: labels) {
			final String n = l.getName();
			if (!n.equals(name)
					&& n.contains(name + "/")
					&& name.indexOf("/") == name.lastIndexOf("/")) {
				return true;
			}
		}
		return false;
	}

	private Set<GmailTag> sortSystemTags(final Set<GmailTag> tags) {
		final Set<GmailTag> alphaTags = new TreeSet<GmailTag>(tags);
		final Set<GmailTag> sorted = new LinkedHashSet<GmailTag>();

		for(final GmailTag t: SYSTEM_TAG_ORDER) {
			if (alphaTags.contains(t)) {
				sorted.add(t);
				alphaTags.remove(t);
			}
		}
		sorted.addAll(alphaTags);

		return sorted;
	}

	@Override
	public GmailTag moveToSection(final GmailTag tag, final GmailSection section) throws GMailException {
		return moveToSection(tag, section.getName());
	}

	private GmailTag moveToSection(final GmailTag tag, final String sectionName) throws GMailException {
		try {
			final String name = sectionName + "/" + tag.getName();
			return new GmailTag(labelService.rename(tag.getId(), name));
		} catch (final LabelException e) {
			throw new GMailException("move " + tag.getName() + " to " + sectionName, e);
		}
	}

	@Override
	public long getCountForTags(final Set<GmailTag> includes, final Set<GmailTag> excludes, final String pattern) throws GMailException {
		try {
			if (includes.isEmpty()){//|| ) { //TODO && excludes.isEmpty() && pattern.isEmpty()) {
				return 0;
			}
			if ((includes.contains(GmailTag.ALL) || includes.contains(GmailTag.SENT))
					&& pattern.isEmpty()) {
				return 0;
			}

			final StringBuilder query = new StringBuilder();
			if (!excludes.isEmpty()) {
				boolean first = true;
				for(final GmailTag t: excludes) {
					if (!first) {
						query.append(" AND ");
					}
					first = false;
					query.append("-label:").append(t.getPath().replaceAll("/", "-").replaceAll(" ", "-"));
				}
			}
			if (!includes.isEmpty()) {
				if (!excludes.isEmpty()) {
					query.append(" AND ");
				}
				boolean first = true;
				for(final GmailTag t: includes) {
					if (!first) {
						query.append(" AND ");
					}
					first = false;
					query.append("label:").append(t.getPath().replaceAll("/", "-").replaceAll(" ", "-"));
				}
			}
			if (!pattern.isEmpty()) {
				if (!includes.isEmpty() || !excludes.isEmpty()) {
					query.append(" AND ");
				}
				final String p = pattern.trim().replaceAll(" ", " OR ");
				query.append("(").append(p).append(")");
			}

			return threadService.count(query.toString());
		} catch (final ThreadException e) {
			throw new GMailException("count threads", e);
		}
	}

	@Override
	public long getCountForSection(final GmailSection section
			, final Set<GmailTag> includes, final Set<GmailTag> excludes
			, final String namePattern) throws GMailException {
		try {
			final Set<GmailTag> tags = getTags(section);
			if (tags.isEmpty() || tags.contains(GmailTag.ALL) || tags.contains(GmailTag.SENT)) {
				return 0;
			}

			final StringBuilder query = new StringBuilder();
			boolean first = true;
			for (final GmailTag t: tags) {
				if (first) {
					first = false;
				} else {
					query.append(" OR ");
				}

				query.append("(");
				if (!excludes.isEmpty()) {
					boolean subFirst = true;
					for(final GmailTag exc: excludes) {
						if (!subFirst) {
							query.append(" AND ");
						}
						subFirst = false;
						query.append("-label:").append(exc.getPath().replaceAll("/", "-").replaceAll(" ", "-"));
					}
				}
				if (!includes.isEmpty()) {
					if (!excludes.isEmpty()) {
						query.append(" AND ");
					}
					boolean subFirst = true;
					for(final GmailTag inc: includes) {
						if (!subFirst) {
							query.append(" AND ");
						}
						subFirst = false;
						query.append("label:").append(inc.getPath().replaceAll("/", "-").replaceAll(" ", "-"));
					}
				}
				if (!includes.contains(t) && !excludes.contains(t)) {
					if (!excludes.isEmpty() || !includes.isEmpty()) {
						query.append(" AND ");
					}
					query.append("label:").append(t.getPath().replaceAll("/", "-").replaceAll(" ", "-"));
				}
				if (!namePattern.isEmpty()) {
					query.append(" AND ");
					final String p = namePattern.trim().replaceAll(" ", " OR ");
					query.append("(").append(p).append(")");
				}
				query.append(")");
			}

			return threadService.count(query.toString());
		} catch (final ThreadException e) {
			throw new GMailException("count threads for " + section.getPath(), e);
		}
	}

	@Override
	public void addTagForThreads(final GmailTag tag, final Set<GmailThread> threads) throws GMailException {
		try {
			final Set<String> threadIds = threads.stream().map(GmailThread::getId).collect(Collectors.toSet());
			final Set<String> labelIds = new HashSet<String>();
			labelIds.add(tag.getId());
			threadService.update(threadIds, labelIds, true);
		} catch (final ThreadException e) {
			throw new GMailException("add tag " + tag.getName(), e);
		}
	}

	@Override
	public void removeTagForThreads(final GmailTag tag, final Set<GmailThread> threads) throws GMailException {
		try {
			final Set<String> threadIds = threads.stream().map(GmailThread::getId).collect(Collectors.toSet());
			final Set<String> labelIds = new HashSet<String>();
			labelIds.add(tag.getId());
			threadService.update(threadIds, labelIds, false);
		} catch (final ThreadException e) {
			throw new GMailException("remove tag " + tag.getName(), e);
		}
	}

	@Override
	public void hide(final GmailTag tag) throws GMailException {
		try {
			labelService.hide(tag.getId());
		} catch (final LabelException e) {
			throw new GMailException("hide tag " + tag.getName(), e);
		}
	}

	@Override
	public void show(final GmailTag tag) throws GMailException {
		try {
			labelService.show(tag.getId());
		} catch (final LabelException e) {
			throw new GMailException("show tag " + tag.getName(), e);
		}
	}

	@Override
	public void hide(final GmailSection section) throws GMailException {
		try {
			labelService.hide(section.getId());
		} catch (final LabelException e) {
			throw new GMailException("hide section " + section.getName(), e);
		}
	}

	@Override
	public void show(final GmailSection section) throws GMailException {
		try {
			labelService.show(section.getId());
		} catch (final LabelException e) {
			throw new GMailException("show section " + section.getName(), e);
		}
	}

	@Override
	public GmailTag findTag(final String name) throws GMailException {
		try {
			return new GmailTag(labelService.find(name));
		} catch (final LabelException e) {
			throw new GMailException("find tag " + name, e);
		}
	}

	@Override
	public GmailSection rename(final GmailSection section, final String name) throws GMailException {
		final GmailSection renamed;
		try {
			String newName = section.getPath();
			if (newName.contains("/")) {
				newName = newName.substring(0, newName.lastIndexOf("/"));
			} else {
				newName = "";
			}
			newName += name;

			renamed = addSection(newName);
			for(final GmailTag t: getTags(section)) {
				moveToSection(t, renamed);
			}
			remove(section);
		} catch (final GMailException e) {
			throw new GMailException("rename section " + section.getName() + " to " + name, e);
		}

		return renamed;
	}

	@Override
	public GmailTag rename(final GmailTag tag, final String name) throws GMailException {
		try {
			String newName = tag.getPath();
			if (newName.contains("/")) {
				newName = newName.substring(0, newName.lastIndexOf("/") + 1);
			} else {
				newName = "";
			}
			newName += name;
			return new GmailTag(labelService.rename(tag.getId(), newName));
		} catch (final LabelException e) {
			throw new GMailException("rename tag \"" + tag.getName() + "\" to \"" + name + "\"", e);
		}
	}

	@Override
	public GmailSection addSection(final String name) throws GMailException {
		try {
			return new GmailSection(labelService.add(name));
		} catch (final LabelException e) {
			throw new GMailException("add section \"" + name + "\"", e);
		}
	}

	@Override
	public GmailTag addTag(final String name) throws GMailException {
		try {
			return new GmailTag(labelService.add(name));
		} catch (final LabelException e) {
			throw new GMailException("add tag \"" + name + "\"", e);
		}
	}

	@Override
	public void remove(final GmailSection section) throws GMailException {
		for(final GmailTag t: getTags(section)) {
			remove(t);
		}
		try {
			labelService.remove(section.getId());
		} catch (final LabelException e) {
			// don't bother if the label really exists
		}
	}

	@Override
	public void remove(final GmailTag tag) throws GMailException {
		try {
			labelService.remove(tag.getId());
		} catch (final LabelException e) {
			throw new GMailException("remove tag " + tag.getName(), e);
		}
	}

	@Override
	public void archive(final Set<GmailThread> threads) throws GMailException {
		try {
			final Set<String> ids = threads.stream().map(GmailThread::getId).collect(Collectors.toSet());
			final Set<String> labelIds = new HashSet<String>();
			labelIds.add(GmailTag.INBOX.getId());
			threadService.update(ids, labelIds, false);
		} catch (final ThreadException e) {
			throw new GMailException("archive threads " + threads, e);
		}
	}

	@Override
	public void trash(final Set<GmailThread> threads) throws GMailException {
		try {
			final Set<String> ids = threads.parallelStream().map(GmailThread::getId).collect(Collectors.toSet());
			threadService.trash(ids);
		} catch (final ThreadException e) {
			throw new GMailException("trash threads " + threads, e);
		}
	}

	@Override
	public void remove(final GmailMessage message) throws MailException {
		try {
			if (message.isDraft()) {
				messageService.removeDraft(message.getId());
			} else {
				messageService.removeMessage(message.getId());
			}
		} catch (final MessageException e) {
			throw new GMailException("remove message " + message.getId(), e);
		}
	}

	@Override
	public void addOnUpdateTagOrSection(final Runnable callback) {
		historyService.addOnUpdateLabel(callback);
	}

	@Override
	public void addOnUpdateMessage(Runnable callback) {
		historyService.addOnUpdateMessage(mSet -> callback.run());
	}

	@Override
	public void addOnNewMessage(final Callback<Set<GmailThread>, Void> callback) {
		historyService.addOnAddedMessage(mSet -> {
			final Set<GmailThread> threads = new LinkedHashSet<GmailThread>();
			mSet.forEach(m -> {
				final Thread t;
				try {
					t = threadService.get(Collections.singleton(m.getThreadId()), false, null).iterator().next();
				} catch (final Exception e) {
					LOGGER.error("load thread id {} for message id {}", m.getThreadId(), m.getId(), e);
					return;
				}
				final GmailThread thread = new GmailThread(t);
				final Set<String> tagIds = thread.getTagIds();
				if (tagIds.contains(GmailTag.UNREAD.getId())
						&& !tagIds.contains(GmailTag.DRAFT.getId())
						&& !tagIds.contains(GmailTag.SPAM.getId())
						&& !tagIds.contains(GmailTag.TRASH.getId())
						&& !tagIds.contains(GmailTag.SENT.getId())) {
					threads.add(thread);
				}
			});
			if (!threads.isEmpty()) {
				callback.call(threads);
			}
		});
	}

	@Override
	public void send(final GmailMessage draft) throws GMailException {
		try {
			messageService.send(draft.getId(), draft.getRaw());
		} catch (final MessageException | IOException | MessagingException e) {
			throw new GMailException("send message", e);
		}
	}

	@Override
	public void save(final GmailMessage draft) throws GMailException {
		try {
			messageService.save(draft.getId(), draft.getRaw());
		} catch (final MessageException | IOException | MessagingException e) {
			throw new GMailException("save message", e);
		}
	}

	@Override
	public synchronized GmailContact getContact() {
		try {
			for(final GmailContact c: getContacts()) {
				if (c.getEmail().equals(address)) {
					return c;
				}
			}
		} catch (final GMailException e) {
			LOGGER.error("load personal contact", e);
		}
		return new GmailContact(address, "");
	}

	@Override
	public Set<GmailContact> getContacts() throws GMailException {
		try {
			final Set<GmailContact> contacts = new LinkedHashSet<GmailContact>();
			for(final ContactEntry c: contactService.getAll()) {
				if (c.getEmailAddresses() != null
						&& c.getName() != null
						&& c.getName().getFullName() != null) {
					final String fullName = c.getName().getFullName().getValue();
					for(final Email e: c.getEmailAddresses()) {
						contacts.add(new GmailContact(e.getAddress(), fullName));
					}
				}
			}
			return contacts;
		} catch (final ContactException e) {
			throw new GMailException("get contacts", e);
		}
	}

	@Override
	public GmailMessage getDraft(final String messageId) throws MailException {
		try {
			final Draft draft = messageService.getDraftForMessage(messageId);
			if (draft == null) {
				return null;
			}
			return new GmailMessage(draft);
		} catch (final MessageException | MessagingException e) {
			throw new GMailException("get draft", e);
		}
	}

	public MessageService getMessageService() {
		return messageService;
	}

	public ThreadService getThreadService() {
		return threadService;
	}

	@Override
	public void clearCache() {
		labelService.clearCache();
		messageService.clearCache();
		threadService.clearCache();
		contactService.clearCache();
		historyService.clearCache();
	}

	@Override
	public GmailTag getSpecialTag(final SpecialTag specialTag) {
		switch (specialTag) {
		case ALL:		return GmailTag.ALL;
		case FLAGGED:	return GmailTag.STARRED;
		case INBOX:		return GmailTag.INBOX;
		case SENT:		return GmailTag.SENT;
		case UNREAD:	return GmailTag.UNREAD;
		case SPAM:		return GmailTag.SPAM;
		case TRASH:		return GmailTag.TRASH;
		case DRAFT:		return GmailTag.DRAFT;
		case SOCIAL:	return GmailTag.SOCIAL;
		case PROMOTIONS:return GmailTag.PROMOTIONS;
		case UPDATES:	return GmailTag.UPDATES;
		case FORUMS:	return GmailTag.FORUMS;
		case CHAT:		return GmailTag.CHAT;
		}
		return null;
	}

	private GmailTag getSpecialTag(final String id) {
		for(final SpecialTag s: SpecialTag.values()) {
			final GmailTag tag = getSpecialTag(s);
			if (tag.getId().equals(id)) {
				return tag;
			}
		}
		return null;
	}

	@Override
	public void persistBytes(String id, byte[] bytes) throws GMailException {
		try {
			final MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
			message.setSubject(id);
			message.setContent(bytes, "application/octet-stream");
			message.saveChanges();
			messageService.insert(id, GmailMessage.toRaw(message));
		} catch (final Exception e) {
			throw new GMailException("persist id", e);
		}
	}

	@Override
	public byte[] readBytes(String id) throws GMailException {
		try {
			final Set<Message> messages = messageService.find(id);
			final String messageId = messages.iterator().next().getId();
			final MimeMessage message = GmailMessage.getMimeMessage(messageService.getMessage(messageId));
			try (final ByteArrayOutputStream bos = new ByteArrayOutputStream();
					BASE64DecoderStream bds = (BASE64DecoderStream) message.getContent()) {
				final byte[] bytes = new byte[128];
				int n = 0;
				while ((n=bds.read(bytes)) > 0) {
					bos.write(bytes, 0, n);
				}
				return bos.toByteArray();
			}
		} catch (final Exception e) {
			throw new GMailException("read " + id, e);
		}
	}
}
