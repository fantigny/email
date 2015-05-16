package net.anfoya.mail.gmail;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.anfoya.java.io.JsonFile;
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

	// Check https://developers.google.com/gmail/api/auth/scopes for all
	// available scopes
	private static final String SCOPE = "https://mail.google.com/";
	private static final String APP_NAME = "AGARAM";
	// Email address of the user, or "me" can be used to represent the currently
	// authorized user.
	private static final String USER = "me";
	// Path to the client_secret.json file downloaded from the Developer Console
	private static final String CLIENT_SECRET_PATH = "client_secret.json";

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
	public void login(final String id, final String pwd) throws net.anfoya.mail.service.MailException {
		login();
	}

	protected Gmail login() throws GMailException {
		Gmail gmail;
		try {
			final String path = this.getClass().getResource(CLIENT_SECRET_PATH).toURI().getPath();
			final GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, new FileReader(path));
			// read refresh token
			final JsonFile<String> refreshTokenFile = new JsonFile<String>("refreshToken.json");

			String refreshToken = null;
			if (refreshTokenFile.exists()) {
				refreshToken = refreshTokenFile.load(String.class);
			}

			// Generate Credential using retrieved code.
			final GoogleCredential credential = new GoogleCredential
					.Builder()
					.setClientSecrets(clientSecrets)
					.setJsonFactory(jsonFactory).setTransport(httpTransport)
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
			refreshTokenFile.save(credential.getRefreshToken());
		} catch (final URISyntaxException | IOException e) {
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
			if (includes.isEmpty()) {
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

			final String unreadId = labelService.find("UNREAD").getId();
			for(final Thread t: threadService.find(query.toString())) {
				// clean labels
				for(final Message m: t.getMessages()) {
					final List<String> cleaned = new ArrayList<String>();
					for(final String id: m.getLabelIds()) {
						final Label l = labelService.get(id);
						if (l!= null && !GmailTag.isHidden(l)) {
							cleaned.add(id);
						}
					}
					m.setLabelIds(cleaned);
				}
				threads.add(new GmailThread(t, unreadId));
			}

			return threads;
		} catch (final ThreadException | LabelException e) {
			throw new GMailException("login", e);
		}
	}

	@Override
	public GmailMessage getMessage(final String id) throws GMailException {
		try {
		    return new GmailMessage(id, messageService.getRaw(id));
		} catch (final MessageException e) {
			throw new GMailException("loading message id: " + id, e);
		}
	}

	@Override
	public GmailMessage createDraft() throws GMailException {
		try {
			final Draft draft = messageService.createDraft();
			return new GmailMessage(draft.getId(), Base64.getUrlDecoder().decode(draft.getMessage().getRaw()));
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
	public Set<GmailTag> getTags(final GmailSection section, final String tagPattern) throws GMailException {
		try {
			final Set<GmailTag> tags = new TreeSet<GmailTag>();
			final String pattern = tagPattern.trim().toLowerCase();
			final Collection<Label> labels = labelService.getAll();
			if (section.equals(GmailSection.SYSTEM)) {
				for(final Label l:labels) {
					if (!GmailTag.isHidden(l) && GmailTag.isSystem(l)) {
						final String name = l.getName();
						l.setName(name.charAt(0) + name.substring(1).toLowerCase());
						tags.add(new GmailTag(l));
					}
				}
			} else if (section.equals(GmailSection.TO_HIDE)) {
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
			} else if (section.equals(GmailSection.TO_SORT)) {
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
								&& name.startsWith(section.getName() + "/")
								&& name.indexOf("/") == name.lastIndexOf("/")) {
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
	public Set<GmailTag> getTags() throws GMailException {
		return getTags(null, "");
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
	public int getCountForTags(final Set<GmailTag> includes, final Set<GmailTag> excludes, final String mailPattern) throws GMailException {
		try {
			if (includes.isEmpty()) {
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
				if (!first) {
					query.append(" OR ");
				}
				first = false;

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
				newName = newName.substring(0, newName.lastIndexOf("/"));
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
}
