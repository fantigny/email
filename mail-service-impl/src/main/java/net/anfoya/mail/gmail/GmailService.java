package net.anfoya.mail.gmail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.util.Callback;

import javax.mail.MessagingException;

import net.anfoya.mail.gmail.model.GmailContact;
import net.anfoya.mail.gmail.model.GmailMessage;
import net.anfoya.mail.gmail.model.GmailMoreThreads;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.gmail.model.GmailThread;
import net.anfoya.mail.gmail.service.ContactException;
import net.anfoya.mail.gmail.service.ContactService;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.extensions.Email;

public class GmailService implements MailService<GmailSection, GmailTag, GmailThread, GmailMessage, GmailContact> {
	private static final Logger LOGGER = LoggerFactory.getLogger(GmailService.class);

	private static final String USER = "me";
	private static final String DEFAULT = "default";

    private static final String APP_NAME = "AGARAM";
	private static final String CLIENT_SECRET_PATH = "client_secret.json";
    private static final String REFRESH_TOKEN_SUFFIX = "%s-refresh-token";

	private static final long PULL_PERIOD_MS = 1000 * 5;

	private final HttpTransport httpTransport;
	private final JsonFactory jsonFactory;

	private LabelService labelService;
	private MessageService messageService;
	private ThreadService threadService;
	private HistoryService historyService;
	private ContactService contactService;
	private String refreshTokenName;

	private final ReadOnlyBooleanWrapper connected;

	public GmailService() {
		httpTransport = new NetHttpTransport();
		jsonFactory = new JacksonFactory();
		connected = new ReadOnlyBooleanWrapper(false);
	}

	@Override
	public void connect() throws GMailException {
		connect("main");
	}

	public GmailService connect(final String mailId) throws GMailException {
		refreshTokenName = String.format(REFRESH_TOKEN_SUFFIX, mailId);

		Gmail gmail;
		ContactsService gcontact;
		try {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(CLIENT_SECRET_PATH)));
			final GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, reader);
			final GoogleCredential credential = new GoogleCredential.Builder()
					.setClientSecrets(clientSecrets)
					.setJsonFactory(jsonFactory)
					.setTransport(httpTransport)
					.build();

		    final Preferences prefs = Preferences.userNodeForPackage(GmailService.class);
			final String refreshToken = prefs.get(refreshTokenName, null);
			if (refreshToken != null) {
				// Generate Credential using saved token.
				credential.setRefreshToken(refreshToken);
			} else {
				// Generate Credential using login token.
				final TokenResponse tokenResponse = new GmailLogin(mailId, clientSecrets).getTokenResponseCredentials();
				credential.setFromTokenResponse(tokenResponse);
			}
			credential.refreshToken();

			// Create a new authorized Google Contact service
			gcontact = new ContactsService(APP_NAME);
			gcontact.setOAuth2Credentials(credential);

			// Create a new authorized Gmail service
			gmail = new Gmail.Builder(httpTransport, jsonFactory, credential)
					.setApplicationName(APP_NAME)
					.build();

			// save refresh token
			prefs.put(refreshTokenName, credential.getRefreshToken());
			prefs.flush();
		} catch (final IOException | BackingStoreException | InterruptedException e) {
			throw new GMailException("login", e);
		}

		contactService = new ContactService(gcontact, DEFAULT).init();

		messageService = new MessageService(gmail, USER);
		threadService = new ThreadService(gmail, USER);
		labelService = new LabelService(gmail, USER);

		historyService = new HistoryService(gmail, USER);
		historyService.addOnLabelUpdate(t -> {
			if (t == null) {
				labelService.clearCache();
			}
			return null;
		});
		historyService.start(PULL_PERIOD_MS);

		connected.bind(historyService.connected());

		return this;
	}

	@Override
	public void disconnect() {
	    final Preferences prefs = Preferences.userNodeForPackage(GmailService.class);
		prefs.remove(refreshTokenName);
		try {
			prefs.flush();
		} catch (final BackingStoreException e) {
			LOGGER.error("removing refresh token", e);
		}
	}

	@Override
	public void reconnect() {
		if (connected.get()) {
			return;
		}
	}

	@Override
	public ReadOnlyBooleanProperty connected() {
		return connected.getReadOnlyProperty();
	}

	@Override
	public Set<GmailThread> findThreads(final Set<GmailTag> includes, final Set<GmailTag> excludes, final String pattern, final int pageMax) throws GMailException {
		try {
			final Set<GmailThread> threads = new LinkedHashSet<GmailThread>();
			if (includes.isEmpty()) { //TODO && excludes.isEmpty() && pattern.isEmpty()) {
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
							LOGGER.error("no label for message {}", m.toPrettyString());
						} else {
							for(final String id: m.getLabelIds()) {
								final Label l = labelService.get(id);
								if (l!= null && !GmailTag.isHidden(l)) {
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
			throw new GMailException("loading message id: " + id, e);
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
			throw new GMailException("creating draft", e);
		}
	}

	@Override
	public Set<GmailSection> getSections() throws GMailException {
		try {
			final Collection<Label> labels = labelService.getAll();
			final Set<GmailSection> sectionTmp = new TreeSet<GmailSection>();
			for(final Label label: labels) {
				if (!GmailSection.isHidden(label)) {
					final GmailSection section = new GmailSection(label);
					final String sectionName = section.getName() + "/";
					for(final Label l: labels) {
						if (l.getName().contains(sectionName)) {
							sectionTmp.add(section);
							break;
						}
					}
				}
			}

			final Set<GmailSection> sections = new LinkedHashSet<GmailSection>();
			sections.add(GmailSection.SYSTEM);
			sections.addAll(sectionTmp);

			LOGGER.debug("get sections: {}", sections);
			return sections;
		} catch (final LabelException e) {
			throw new GMailException("getting sections", e);
		}
	}

	@Override
	public GmailTag getTag(final String id) throws GMailException {
		try {
			return new GmailTag(labelService.get(id));
		} catch (final LabelException e) {
			throw new GMailException("getting tag " + id, e);
		}
	}

	@Override
	public Set<GmailTag> getTags() throws GMailException {
		return getTags(null, "");
	}

	@Override
	public Set<GmailTag> getTags(final GmailSection section, final String tagPattern) throws GMailException {
		try {
			final Set<GmailTag> tags;
			final String pattern = tagPattern.trim().toLowerCase();
			final Collection<Label> labels = labelService.getAll();
			if (GmailSection.SYSTEM.equals(section)) {
				final Set<GmailTag> alphaTags = new TreeSet<GmailTag>();
				alphaTags.add(GmailTag.ALL_TAG);
				for(final Label label:labels) {
					final String name = label.getName();
					if (!GmailTag.isHidden(label)) {
						if (GmailTag.isSystem(label) && GmailTag.getName(label).toLowerCase().contains(pattern)) {
							// GMail system tags
							alphaTags.add(new GmailTag(label));
						} else if (!name.contains("/")) {
							// root tags, put them here if no sub-tag
							boolean hasSubTag = false;
							for(final Label l: labels) {
								final String n = l.getName();
								if (!n.equals(name)
										&& n.contains(name + "/")
										&& name.indexOf("/") == name.lastIndexOf("/")) {
									hasSubTag = true;
									break;
								}
							}
							if (!hasSubTag) {
								alphaTags.add(new GmailTag(label));
							}
						}
					}
				}
				tags = new LinkedHashSet<GmailTag>();
				for(final GmailTag t: alphaTags) {
					if (t.isSystem()) {
						tags.add(t);
					}
				}
				for(final GmailTag t: alphaTags) {
					if (!t.isSystem()) {
						tags.add(t);
					}
				}
			} else {
				tags = new TreeSet<GmailTag>();
				for(final Label label:labels) {
					if (!GmailTag.isHidden(label) && !GmailTag.isSystem(label)
							&& GmailTag.getName(label).toLowerCase().contains(pattern)) {
						final String name = label.getName();
						if (section != null && name.equals(section.getPath())) {
							tags.add(new GmailTag(label.getId(), Tag.THIS_NAME, label.getName(), false));
						} else if (name.indexOf("/") == name.lastIndexOf("/")
								&& section == null || name.startsWith(section.getName() + "/")) {
							tags.add(new GmailTag(label));
						}
					}
				}
			}

			LOGGER.debug("tags for section({}) tagPattern({}): {}", section == null? "": section.getPath(), tagPattern, tags);
			return tags;
		} catch (final LabelException e) {
			throw new GMailException("getting tags for section " + section.getName() + " and pattern \"" + tagPattern + "\"", e);
		}
	}

	@Override
	public GmailTag moveToSection(final GmailTag tag, final GmailSection section) throws GMailException {
		if (GmailTag.THIS_NAME.equals(tag.getName())) {
			return tag;
		}

		try {
			final String name = section.getName() + "/" + tag.getName();
			return new GmailTag(labelService.rename(tag.getId(), name));
		} catch (final LabelException e) {
			throw new GMailException("moving " + tag.getName() + " to " + section.getName(), e);
		}
	}

	@Override
	public int getCountForTags(final Set<GmailTag> includes, final Set<GmailTag> excludes, final String pattern) throws GMailException {
		try {
			if (includes.isEmpty() || includes.contains(GmailTag.ALL_TAG) || includes.contains(GmailTag.SENT_TAG)) { //TODO && excludes.isEmpty() && pattern.isEmpty()) {
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
			throw new GMailException("counting threads", e);
		}
	}

	@Override
	public int getCountForSection(final GmailSection section
			, final Set<GmailTag> includes, final Set<GmailTag> excludes
			, final String namePattern, final String tagPattern) throws GMailException {
		try {
			final Set<GmailTag> tags = getTags(section, tagPattern);
			if (tags.isEmpty() || tags.contains(GmailTag.ALL_TAG) || tags.contains(GmailTag.SENT_TAG)) {
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
			throw new GMailException("counting threads for " + section.getPath(), e);
		}
	}

	@Override
	public void addTagForThreads(final GmailTag tag, final Set<GmailThread> threads) throws GMailException {
		try {
			final Set<String> labelIds = new HashSet<String>();
			labelIds.add(tag.getId());
			final Set<String> threadIds = new HashSet<String>();
			for(final GmailThread t: threads) {
				threadIds.add(t.getId());
			}
			threadService.update(threadIds, labelIds, true);
		} catch (final ThreadException e) {
			throw new GMailException("adding tag " + tag.getName(), e);
		}
	}

	@Override
	public void removeTagForThreads(final GmailTag tag, final Set<GmailThread> threads) throws GMailException {
		try {
			final Set<String> labelIds = new HashSet<String>();
			labelIds.add(tag.getId());
			final Set<String> threadIds = new HashSet<String>();
			for(final GmailThread t: threads) {
				threadIds.add(t.getId());
			}
			threadService.update(threadIds, labelIds, false);
		} catch (final ThreadException e) {
			throw new GMailException("removing tag " + tag.getName(), e);
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
	public GmailSection rename(GmailSection section, final String name) throws GMailException {
		final Set<GmailTag> tags = getTags(section, "");
		Label label;
		try {
			String newName = section.getPath();
			if (newName.contains("/")) {
				newName = newName.substring(0, newName.lastIndexOf("/"));
			} else {
				newName = "";
			}
			newName += name;

			label = labelService.rename(section.getId(), newName);
		} catch (final LabelException e) {
			throw new GMailException("rename section " + section.getName() + " to " + name, e);
		}
		section = new GmailSection(label);

		// move tags to new section
		for(final GmailTag t: tags) {
			moveToSection(t, section);
		}

		return section;
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
			throw new GMailException("adding section \"" + name + "\"", e);
		}
	}

	@Override
	public GmailTag addTag(final String name) throws GMailException {
		try {
			return new GmailTag(labelService.add(name));
		} catch (final LabelException e) {
			throw new GMailException("adding tag \"" + name + "\"", e);
		}
	}

	@Override
	public void remove(final GmailSection section) throws GMailException {
		for(final GmailTag t: getTags(section, "")) {
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
			final Set<String> ids = new HashSet<String>();
			for(final GmailThread t: threads) {
				ids.add(t.getId());
			}
			final Set<String> labelIds = new HashSet<String>();
			labelIds.add(GmailTag.INBOX_TAG.getId());
			threadService.update(ids, labelIds, false);
		} catch (final ThreadException e) {
			throw new GMailException("archiving threads " + threads, e);
		}
	}

	@Override
	public void trash(final Set<GmailThread> threads) throws GMailException {
		try {
			final Set<String> ids = new HashSet<String>();
			for(final GmailThread t: threads) {
				ids.add(t.getId());
			}
			final Set<String> labelIds = new HashSet<String>();
			labelIds.add("INBOX");
			threadService.trash(ids);
		} catch (final ThreadException e) {
			throw new GMailException("trashing threads " + threads, e);
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
			throw new GMailException("removing message " + message.getId(), e);
		}
	}

	@Override
	public void addOnUpdate(final Callback<Throwable, Void> callback) {
		historyService.addOnUpdate(callback);
	}

	@Override
	public void send(final GmailMessage draft) throws GMailException {
		try {
			messageService.send(draft.getId(), draft.getRaw());
		} catch (final MessageException | IOException | MessagingException e) {
			throw new GMailException("sending message", e);
		}
	}

	@Override
	public void save(final GmailMessage draft) throws GMailException {
		try {
			messageService.save(draft.getId(), draft.getRaw());
		} catch (final MessageException | IOException | MessagingException e) {
			throw new GMailException("saving message", e);
		}
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
			throw new GMailException("getting contacts", e);
		}
	}

	@Override
	public GmailMessage getDraft(final String messageId) throws MailException {
		try {
			return new GmailMessage(messageService.getDraftForMessage(messageId));
		} catch (final MessageException | MessagingException e) {
			throw new GMailException("getting draft", e);
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
		historyService.clearCache();
		contactService.clearCache();
	}
}
