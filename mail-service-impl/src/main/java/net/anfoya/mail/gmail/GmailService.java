package net.anfoya.mail.gmail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.anfoya.mail.gmail.model.GmailMessage;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.gmail.model.GmailThread;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;

public class GmailService implements MailService<GmailSection, GmailTag, GmailThread, GmailMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(GmailService.class);
    private static final String REFRESH_TOKEN = "-refresh-token";

	private static final String APP_NAME = "AGARAM";
	private static final String CLIENT_SECRET_PATH = "client_secret.json";
	private static final String SCOPE = "https://mail.google.com/";
	private static final String USER = "me";

	private final HttpTransport httpTransport;
	private final JsonFactory jsonFactory;

	private LabelService labelService;
	private MessageService messageService;
	private ThreadService threadService;
	private HistoryService historyService;

	public GmailService() {
		httpTransport = new NetHttpTransport();
		jsonFactory = new JacksonFactory();
	}

	@Override
	public Gmail login(final String mailId) throws GMailException {
		Gmail gmail;
		try {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(CLIENT_SECRET_PATH)));
			final GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, reader);

			// read refresh token
			final String refreshTokenName = mailId + REFRESH_TOKEN;
		    final Preferences prefs = Preferences.userNodeForPackage(GmailService.class);
			final String refreshToken = prefs.get(refreshTokenName, null);

			// Generate Credential using retrieved code.
			final GoogleCredential credential = new GoogleCredential.Builder()
					.setClientSecrets(clientSecrets)
					.setJsonFactory(jsonFactory)
					.setTransport(httpTransport)
					.build();
			if (refreshToken == null || refreshToken.isEmpty()) {
				// Allow user to authorize via url.
				final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow
						.Builder(httpTransport, jsonFactory, clientSecrets, Arrays.asList(SCOPE))
						.setAccessType("offline")
						.setApprovalPrompt("auto").build();
				final String url = flow.newAuthorizationUrl().setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI).build();
				System.out.println("Please open the following URL in your browser then type the authorization code:\n" + url);
				// Read code entered by user.
				final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				final String code = br.readLine();
				final GoogleTokenResponse response = flow.newTokenRequest(code)
						.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI)
						.execute();
				credential.setFromTokenResponse(response);
			} else {
				credential.setRefreshToken(refreshToken);
			}

			// Create a new authorized Gmail API client
			gmail = new Gmail.Builder(httpTransport, jsonFactory, credential).setApplicationName(APP_NAME).build();

			// save refresh token
			prefs.put(refreshTokenName, credential.getRefreshToken());
			prefs.flush();
		} catch (final IOException | BackingStoreException e) {
			throw new GMailException("login", e);
		}

		historyService = new HistoryService(gmail, USER);
		labelService = new LabelService(gmail, USER);
		messageService = new MessageService(gmail, USER);
		threadService = new ThreadService(gmail, USER);

		return gmail;
	}

	@Override
	public void logout() {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<GmailThread> getThreads(final Set<GmailTag> includes, final Set<GmailTag> excludes, final String pattern) throws GMailException {
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

			final String unreadId = labelService.find("UNREAD").getId();
			for(final Thread t: threadService.find(query.toString())) {
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
				threads.add(new GmailThread(t, unreadId));
			}

			return threads;
		} catch (final ThreadException | LabelException | IOException e) {
			throw new GMailException("login", e);
		}
	}

	@Override
	public GmailMessage getMessage(final String id) throws GMailException {
		try {
		    return new GmailMessage(id, messageService.getRaw(id), false);
		} catch (final MessageException e) {
			throw new GMailException("loading message id: " + id, e);
		}
	}

	@Override
	public GmailMessage createDraft() throws GMailException {
		try {
			final Draft draft = messageService.createDraft();
			return new GmailMessage(draft.getId(), Base64.getUrlDecoder().decode(draft.getMessage().getRaw()), true);
		} catch (final MessageException e) {
			throw new GMailException("creating draft", e);
		}
	}

	@Override
	public Set<GmailSection> getSections() throws GMailException {
		try {
			final Collection<Label> labels = labelService.getAll();
			final Set<GmailSection> sectionTmp = new TreeSet<GmailSection>();
			for(final Label label: labels) {
				final GmailSection section = new GmailSection(label);
				if (!section.isHidden()) {
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
			sections.add(GmailSection.TO_HIDE);
			sections.add(GmailSection.TO_SORT);
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
			final Set<GmailTag> tags = new TreeSet<GmailTag>();
			final String pattern = tagPattern.trim().toLowerCase();
			final Collection<Label> labels = labelService.getAll();
			if (GmailSection.SYSTEM.equals(section)) {
				for(final Label l:labels) {
					if (!GmailTag.isHidden(l) && GmailTag.isSystem(l)) {
						final String name = l.getName();
						l.setName(name.charAt(0) + name.substring(1).toLowerCase());
						tags.add(new GmailTag(l));
					}
				}
			} else if (GmailSection.TO_HIDE.equals(section)) {
				for(final Label label:labels) {
					final String name = label.getName();
					if (!GmailTag.isHidden(label) && !GmailTag.isSystem(label) && !name.contains("/")) {
						for(final Label l: labels) {
							final String n = l.getName();
							if (!n.equals(name)
									&& n.contains(name + "/")
									&& name.indexOf("/") == name.lastIndexOf("/")) {
								tags.add(new GmailTag(label));
								break;
							}
						}
					}
				}
			} else if (GmailSection.TO_SORT.equals(section)) {
				for(final Label label:labels) {
					final String name = label.getName();
					if (!GmailTag.isHidden(label) && !GmailTag.isSystem(label) && !name.contains("/")) {
						boolean isToSort = true;
						for(final Label l: labels) {
							final String n = l.getName();
							if (!n.equals(name)
									&& n.contains(name + "/")
									&& name.indexOf("/") == name.lastIndexOf("/")) {
								isToSort = false;
								break;
							}
						}
						if (isToSort) {
							tags.add(new GmailTag(label));
						}
					}
				}
			} else {
				for(final Label label:labels) {
					final String name = label.getName();
					if (!GmailTag.isHidden(label) && !GmailTag.isSystem(label)) {
						final String tagName = GmailTag.getName(label);
						if (tagName.toLowerCase().contains(pattern)
								&& name.indexOf("/") == name.lastIndexOf("/")
								&& section == null || name.startsWith(section.getName() + "/")) {
							tags.add(new GmailTag(label));
						}
					}
				}
			}

			LOGGER.debug("tags for section({}) tagPattern({}): {}", section == null? "": section.getPath(), tagPattern, tags);
			return tags;
		} catch (final LabelException e) {
			throw new GMailException("getting tags for section " + section.getName() + " and pattern \" + tagPattern + \"", e);
		}
	}

	@Override
	public GmailTag moveToSection(final GmailTag tag, final GmailSection section) throws GMailException {
		try {
			final String name = section.getName() + "/" + tag.getName();
			final Label label = labelService.get(tag.getId());
			return new GmailTag(labelService.rename(label, name));
		} catch (final LabelException e) {
			throw new GMailException("moving " + tag.getName() + " to " + section.getName(), e);
		}
	}

	@Override
	public int getCountForTags(final Set<GmailTag> includes, final Set<GmailTag> excludes, final String pattern) throws GMailException {
		try {
			if (includes.isEmpty()) { //TODO && excludes.isEmpty() && pattern.isEmpty()) {
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
			if (tags.isEmpty()) {
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
	public void removeTagForThread(final GmailTag tag, final GmailThread thread) throws GMailException {
		try {
			final Set<String> labelIds = new HashSet<String>();
			labelIds.add(tag.getId());
			final Set<String> threadIds = new HashSet<String>();
			threadIds.add(thread.getId());
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
			label = labelService.get(section.getId());
			String newName = label.getName();
			if (newName.contains("/")) {
				newName = newName.substring(0, newName.lastIndexOf("/"));
			} else {
				newName = "";
			}
			newName += name;

			label = labelService.rename(label, newName);
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
			final Label label = labelService.get(tag.getId());
			String newName = label.getName();
			if (newName.contains("/")) {
				newName = newName.substring(0, newName.lastIndexOf("/") + 1);
			} else {
				newName = "";
			}
			newName += name;
			return new GmailTag(labelService.rename(label, newName));
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
		try {
			labelService.remove(labelService.get(section.getId()));
		} catch (final LabelException e) {
			throw new GMailException("remove section \"" + section.getName() + "\"", e);
		}
	}

	@Override
	public void remove(final GmailTag tag) throws GMailException {
		try {
			labelService.remove(labelService.get(tag.getId()));
		} catch (final LabelException e) {
			throw new GMailException("remove tag " + tag.getName(), e);
		}
	}

	@Override
	public void archive(final Set<GmailThread> threads) throws GMailException {
		try {
			final Set<String> threadIds = new HashSet<String>();
			for(final String id: threadIds) {
				threadIds.add(id);
			}
			final Set<String> labelIds = new HashSet<String>();
			labelIds.add("INBOX");
			threadService.update(threadIds, labelIds, false);
		} catch (final ThreadException e) {
			throw new GMailException("archiving threads " + threads, e);
		}
	}

	@Override
	public void trash(final Set<GmailThread> threads) throws GMailException {
		try {
			threadService.trash(threads);
		} catch (final ThreadException e) {
			throw new GMailException("trashing threads " + threads, e);
		}
	}

	@Override
	public boolean hasUpdate() throws GMailException {
		try {
			return historyService.hasUpdate();
		} catch (final HistoryException e) {
			throw new GMailException("checking for updates", e);
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
}
