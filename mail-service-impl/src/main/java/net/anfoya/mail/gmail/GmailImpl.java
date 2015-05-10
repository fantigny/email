package net.anfoya.mail.gmail;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.anfoya.java.io.JsonFile;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.gmail.model.GmailThread;
import net.anfoya.mail.model.SimpleMessage;
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
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.ModifyThreadRequest;
import com.google.api.services.gmail.model.Thread;

public class GmailImpl implements MailService<GmailSection, GmailTag, GmailThread, SimpleMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(GmailImpl.class);

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

	private Gmail gmail;
	private LabelService labelService;
	private MessageService messageService;
	private ThreadService threadService;

	public GmailImpl() {
		httpTransport = new NetHttpTransport();
		jsonFactory = new JacksonFactory();
	}

	@Override
	public void login(final String id, final String pwd) throws GMailException {
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
		
		labelService = new LabelService(gmail, USER);
		messageService = new MessageService(gmail, USER);
		threadService = new ThreadService(gmail, USER);
	}

	@Override
	public void logout() {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<GmailThread> getThreads(final Set<GmailTag> availableTags, final Set<GmailTag> includes, final Set<GmailTag> excludes, final String pattern) throws GMailException {
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
	
			for(final Thread t: threadService.find(query.toString())) {
				threads.add(new GmailThread(t));
			}
			
			return threads;
		} catch (final ThreadException e) {
			throw new GMailException("login", e);
		}
	}

	@Override
	public GmailThread getThread(final String id) throws GMailException {
		try {
			return new GmailThread(threadService.get(id));
		} catch (final ThreadException e) {
			throw new GMailException("loading thread " + id, e);
		}
	}

	@Override
	public SimpleMessage getMessage(final String id) throws GMailException {
		try {
			LOGGER.debug("get message for id: {}", id);
		    return new SimpleMessage(id, messageService.getRaw(id));
		} catch (final MessageException e) {
			throw new GMailException("loading message id: " + id, e);
		}
	}

	@Override
	public Set<GmailSection> getSections() throws GMailException {
		try {
			final Set<GmailSection> sectionTemp = new TreeSet<GmailSection>();
			final Collection<Label> labels = labelService.getAll();
			for(final Label l: labels) {
				final GmailSection section = new GmailSection(l);
				if (!section.isHidden()) {
					sectionTemp.add(section);
				}
			}
			for(final Iterator<GmailSection> i=sectionTemp.iterator(); i.hasNext();) {
				final String s = i.next().getName() + "/";
				boolean section = false;
				for(final Label l: labels) {
					if (l.getName().contains(s)) {
						section = true;
						break;
					}
				}
				if (!section) {
					i.remove();
				}
			}
	
			final Set<GmailSection> sections = new TreeSet<GmailSection>();
			sections.add(GmailSection.GMAIL_SYSTEM);
			sections.add(GmailSection.NO_SECTION);
			sections.addAll(sectionTemp);
	
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
	public Set<GmailTag> getTags(final GmailSection section, final String tagPattern) throws GMailException {
		try {
			final Set<GmailTag> tags = new TreeSet<GmailTag>();
			final String pattern = tagPattern.trim().toLowerCase();
			final Collection<Label> labels = labelService.getAll();
			for(final Label l:labels) {
				final GmailTag tag = new GmailTag(l);
				if (!tag.isHidden()
						&& (section == null
								|| section.equals(GmailSection.GMAIL_SYSTEM) && l.getType().equals("system")
								|| section.getId().equals(GmailSection.NO_SECTION.getId()) && !l.getName().contains("/") && !l.getType().equals("system")
								|| l.getName().startsWith(section.getName()+"/") && !l.getName().substring(section.getName().length()+1, l.getName().length()).contains("/"))
						&& tag.getName().toLowerCase().contains(pattern)) {
					tags.add(tag);
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
	public GmailTag moveToSection(final GmailTag tag, final GmailSection section) throws GMailException {
		try {
			Label label = labelService.get(tag.getId());
			label.setName(section.getName() + "/" + tag.getName());
			label.setMessageListVisibility("show");
			label.setLabelListVisibility("labelShow");
			label = gmail.users().labels().update(USER, label.getId(), label).execute();
			return new GmailTag(label);
		} catch (final IOException | LabelException e) {
			throw new GMailException("moving " + tag.getName() + " to " + section.getName(), e);
		}
	}

	@Override
	public int getCountForTags(final Set<GmailTag> includes, final Set<GmailTag> excludes, final String mailPattern) throws GMailException {
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

		int count = 0;
		try {
			ListThreadsResponse response = gmail.users().threads().list(USER).setQ(query.toString()).execute();
			while (response.getThreads() != null) {
				count += response.getThreads().size();
				if (response.getNextPageToken() != null) {
					final String pageToken = response.getNextPageToken();
					response = gmail.users().threads().list(USER).setQ(query.toString()).setPageToken(pageToken).execute();
				} else {
					break;
				}
			}
		} catch (final IOException e) {
			throw new GMailException("counting threads for " + includes.toString(), e);
		}

		LOGGER.debug("count for tag includes({}) excludes({}) mailPattern({}): {} == query({})", includes, excludes, mailPattern, count, query);

		return count;
	}

	@Override
	public int getCountForSection(final GmailSection section
			, final Set<GmailTag> includes, final Set<GmailTag> excludes
			, final String namePattern, final String tagPattern) throws GMailException {

		final StringBuilder query = new StringBuilder();

		boolean first = true;
		for (final GmailTag t: getTags(section, tagPattern)) {
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

		int count = 0;
		try {
			ListThreadsResponse resp = gmail.users().threads().list(USER).setQ(query.toString()).execute();
			while (resp.getThreads() != null) {
				count += resp.getThreads().size();
				if (resp.getNextPageToken() != null) {
					final String pageToken = resp.getNextPageToken();
					resp = gmail.users().threads().list(USER).setQ(query.toString()).setPageToken(pageToken).execute();
				} else {
					break;
				}
			}
		} catch (final IOException e) {
			throw new GMailException("counting threads for " + section.getPath(), e);
		}

		LOGGER.debug("count for section({}) includes({}) excludes({}) tagPattern(\"{}\"): {} == query({})", section, includes, excludes, tagPattern, count, query);
		return count;
	}

	@Override
	public void addForThread(final GmailTag tag, final Set<GmailThread> threads) throws GMailException {
		for(final GmailThread t: threads) {
			try {
				@SuppressWarnings("serial")
				final ModifyThreadRequest request = new ModifyThreadRequest().setAddLabelIds(new ArrayList<String>() {{ add(tag.getId()); }});
				gmail.users().threads().modify(USER, t.getId(), request).execute();
			} catch (final IOException e) {
				throw new GMailException("adding tag", e);
			}
		}
	}

	@Override
	public void removeForThread(final GmailTag tag, final GmailThread thread) throws GMailException {
		try {
			@SuppressWarnings("serial")
			final ModifyThreadRequest request = new ModifyThreadRequest().setRemoveLabelIds(new ArrayList<String>() {{ add(tag.getId()); }});
			gmail.users().threads().modify(USER, thread.getId(), request).execute();
		} catch (final IOException e) {
			throw new GMailException("adding tag", e);
		}
	}

	@Override
	public GmailTag findTag(final String name) throws GMailException {
		try {
			for(final Label l: labelService.getAll()) {
				if (l.getName().equalsIgnoreCase(name)) {
					return new GmailTag(l);
				}
			}
		} catch (final LabelException e) {
			throw new GMailException("finf tag " + name, e);
		}
		return null;
	}

	@Override
	public GmailSection rename(GmailSection section, final String name) throws GMailException {
		final Set<GmailTag> tags = getTags(section, "");
		Label label;
		try {
			label = labelService.get(section.getId());
			label = labelService.rename(label, name);
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
			return new GmailTag(labelService.rename(labelService.get(tag.getId()), name));
		} catch (final LabelException e) {
			throw new GMailException("rename tag " + tag.getName(), e);
		}
	}

	@Override
	public GmailSection addSection(final String name) throws GMailException {

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GmailTag createTag(final String name) throws GMailException {
		try {
			Label label = new Label();
			label.setMessageListVisibility("show");
			label.setLabelListVisibility("labelShow");
			label.setName(name);
			label = labelService.add(label);
			return new GmailTag(label);
		} catch (final LabelException e) {
			throw new GMailException("adding " + name, e);
		}
	}

	@Override
	public void remove(final GmailSection section) throws GMailException {
		try {
			gmail.users().labels().delete(USER, section.getId());
		} catch (final IOException e) {
			throw new GMailException("remove section " + section.getName(), e);
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
			@SuppressWarnings("serial")
			final List<String> inboxId = new ArrayList<String>() {{ add(findTag("INBOX").getId());}};
			final ModifyThreadRequest request = new ModifyThreadRequest().setRemoveLabelIds(inboxId);
			for(final GmailThread t: threads) {
				gmail.users().threads().modify(USER, t.getId(), request).execute();
			}
		} catch (final IOException | GMailException e) {
			throw new GMailException("trashing threads " + threads, e);
		}
	}

	@Override
	public void delete(final Set<GmailThread> threads) throws GMailException {
		try {
			for(final GmailThread t: threads) {
				gmail.users().threads().trash(USER, t.getId()).execute();
			}
		} catch (final IOException e) {
			throw new GMailException("trashing threads " + threads, e);
		}
	}
}
