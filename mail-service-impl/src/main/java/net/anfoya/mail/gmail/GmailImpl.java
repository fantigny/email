package net.anfoya.mail.gmail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.security.auth.login.LoginException;

import net.anfoya.java.io.JsonFile;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.gmail.model.GmailThread;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceException;
import net.anfoya.tag.service.TagServiceException;

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
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyThreadRequest;
import com.google.api.services.gmail.model.Thread;

public class GmailImpl implements MailService<GmailSection, GmailTag, GmailThread> {
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

	private Gmail delegate = null;

	private final Map<String, Label> idLabels = new ConcurrentHashMap<String, Label>();

	public GmailImpl() {
		httpTransport = new NetHttpTransport();
		jsonFactory = new JacksonFactory();
	}

	@Override
	public void login(final String id, final String pwd) throws LoginException {
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
			delegate = new Gmail.Builder(httpTransport, jsonFactory, credential).setApplicationName(APP_NAME).build();

			// save refresh token
			refreshTokenFile.save(credential.getRefreshToken());
		} catch (final URISyntaxException | IOException e) {
			throw new LoginException("");
		}
	}

	@Override
	public void logout() {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<GmailThread> getThreads(final Set<GmailTag> availableTags, final Set<GmailTag> includes, final Set<GmailTag> excludes, final String pattern) throws MailServiceException {
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

		LOGGER.debug("get thread ids for tags({}) include({}) excludes({}) tagPattern(\"\") == query({})", availableTags, includes, excludes, pattern, query);
		final Set<String> threadIds = new LinkedHashSet<String>();
		try {
			ListThreadsResponse threadResponse = delegate.users().threads().list(USER).setQ(query.toString()).execute();
			while (threadResponse.getThreads() != null) {
				for(final Thread t : threadResponse.getThreads()) {
					threadIds.add(t.getId());
				}
				if (threadResponse.getNextPageToken() != null) {
					final String pageToken = threadResponse.getNextPageToken();
					LOGGER.debug("get thread ids for tags({}) include({}) excludes({}) tagPattern(\"\") == {}", availableTags, includes, excludes, pattern, query);
					threadResponse = delegate.users().threads().list(USER).setQ(query.toString()).setPageToken(pageToken).execute();
				} else {
					break;
				}
			}
		} catch (final IOException e) {
			throw new MailServiceException("loading thread ids for: " + includes.toString(), e);
		}

		final Set<Future<GmailThread>> futures = new LinkedHashSet<Future<GmailThread>>();
		try {
			ListThreadsResponse threadResponse = delegate.users().threads().list(USER).setQ(query.toString()).execute();
			while (threadResponse.getThreads() != null) {
				for(final Thread t : threadResponse.getThreads()) {
					final Callable<GmailThread> c = new Callable<GmailThread>() {
						@Override
						public GmailThread call() throws Exception {
							return getThread(t.getId());
						}
					};
					futures.add(ThreadPool.getInstance().submit(c));
				}
				if (threadResponse.getNextPageToken() != null) {
					final String pageToken = threadResponse.getNextPageToken();
					threadResponse = delegate.users().threads().list(USER).setQ(query.toString()).setPageToken(pageToken).execute();
				} else {
					break;
				}
			}
		} catch (final IOException e) {
			throw new MailServiceException("loading threads for: " + includes.toString(), e);
		}

		try {
			for(final Future<GmailThread> f: futures) {
				threads.add(f.get());
			}
		} catch(final CancellationException | InterruptedException | ExecutionException e) {
			throw new MailServiceException("loading threads for: " + includes.toString(), e);
		}

		return threads;
	}

	@Override
	public GmailThread getThread(final String id) throws MailServiceException {
		try {
			LOGGER.debug("get thread for id: {}", id);
			return new GmailThread(delegate.users().threads().get(USER, id).setFormat("metadata") .execute());
		} catch (final IOException e) {
			throw new MailServiceException("loading thread " + id, e);
		}
	}

	@Override
	public MimeMessage getMessage(final String id) throws MailServiceException {
		try {
			LOGGER.debug("get message for id: {}", id);
			final Message message = delegate.users().messages().get(USER, id).setFormat("raw").execute();
			final byte[] emailBytes = Base64.getUrlDecoder().decode(message.getRaw());
			final Properties props = new Properties();
		    final Session session = Session.getDefaultInstance(props, null);
		    return new MimeMessage(session, new ByteArrayInputStream(emailBytes));
		} catch (final IOException | MessagingException e) {
			throw new MailServiceException("loading message id: " + id, e);
		}
	}

	@Override
	public Set<GmailSection> getSections() throws TagServiceException {
		final Set<GmailSection> sectionTemp = new TreeSet<GmailSection>();
		for(final Label l: getLabels().values()) {
			final GmailSection section = new GmailSection(l);
			if (!section.isHidden()) {
				sectionTemp.add(section);
			}
		}
		for(final Iterator<GmailSection> i=sectionTemp.iterator(); i.hasNext();) {
			final String s = i.next().getName() + "/";
			boolean section = false;
			for(final Label l: getLabels().values()) {
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
	}

	@Override
	public GmailTag getTag(final String id) throws TagServiceException {
		return new GmailTag(getLabels().get(id));
	}

	@Override
	public Set<GmailTag> getTags(final GmailSection section, final String tagPattern) throws TagServiceException {
		final Set<GmailTag> tags = new TreeSet<GmailTag>();
		final String pattern = tagPattern.trim().toLowerCase();
		for(final Label l: getLabels().values()) {
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
	}

	@Override
	public Set<GmailTag> getTags() throws TagServiceException {
		return getTags(null, "");
	}

	@Override
	public void moveToSection(final GmailSection section, final GmailTag tag) throws TagServiceException {
		Label label = getLabels().get(tag.getId());
		label.setName(section.getName() + "/" + tag.getName());
		label.setMessageListVisibility("show");
		label.setLabelListVisibility("labelShow");
		try {
			label = delegate.users().labels().update(USER, label.getId(), label).execute();
		} catch (final IOException e) {
			throw new TagServiceException("moving " + tag.getName(), e);
		} finally {
			idLabels.clear();
		}
	}

	@Override
	public int getCountForTags(final Set<GmailTag> includes, final Set<GmailTag> excludes, final String mailPattern) throws TagServiceException {
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
			ListThreadsResponse response = delegate.users().threads().list(USER).setQ(query.toString()).execute();
			while (response.getThreads() != null) {
				count += response.getThreads().size();
				if (response.getNextPageToken() != null) {
					final String pageToken = response.getNextPageToken();
					response = delegate.users().threads().list(USER).setQ(query.toString()).setPageToken(pageToken).execute();
				} else {
					break;
				}
			}
		} catch (final IOException e) {
			throw new TagServiceException("counting threads for " + includes.toString(), e);
		}

		LOGGER.debug("count for tag includes({}) excludes({}) mailPattern({}): {} == query({})", includes, excludes, mailPattern, count, query);

		return count;
	}

	@Override
	public int getCountForSection(final GmailSection section
			, final Set<GmailTag> includes, final Set<GmailTag> excludes
			, final String namePattern, final String tagPattern) throws TagServiceException {

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
			ListThreadsResponse resp = delegate.users().threads().list(USER).setQ(query.toString()).execute();
			while (resp.getThreads() != null) {
				count += resp.getThreads().size();
				if (resp.getNextPageToken() != null) {
					final String pageToken = resp.getNextPageToken();
					resp = delegate.users().threads().list(USER).setQ(query.toString()).setPageToken(pageToken).execute();
				} else {
					break;
				}
			}
		} catch (final IOException e) {
			throw new TagServiceException("counting threads for " + section.getPath(), e);
		}

		LOGGER.debug("count for section({}) includes({}) excludes({}) tagPattern(\"{}\"): {} == query({})", section, includes, excludes, tagPattern, count, query);
		return count;
	}

	private Map<String, Label> getLabels() throws TagServiceException {
		if (idLabels.isEmpty()) {
			try {
				for(final Label l: delegate.users().labels().list(USER).execute().getLabels()) {
					idLabels.put(l.getId(), l);
				}
			} catch (final IOException e) {
				throw new TagServiceException("getting labels", e);
			}
			LOGGER.debug("get labels: {}", idLabels.values());
		}
		return idLabels;
	}

	@Override
	public void addTag(final GmailTag tag, final Set<GmailThread> threads) throws MailServiceException {
		for(final GmailThread t: threads) {
			try {
				@SuppressWarnings("serial")
				final ModifyThreadRequest request = new ModifyThreadRequest().setAddLabelIds(new ArrayList<String>() {{ add(tag.getId()); }});
				delegate.users().threads().modify(USER, t.getId(), request).execute();
			} catch (final IOException e) {
				throw new MailServiceException("adding tag", e);
			}
		}
	}

	@Override
	public void remTag(final GmailTag tag, final GmailThread thread) throws MailServiceException {
		try {
			@SuppressWarnings("serial")
			final ModifyThreadRequest request = new ModifyThreadRequest().setRemoveLabelIds(new ArrayList<String>() {{ add(tag.getId()); }});
			delegate.users().threads().modify(USER, thread.getId(), request).execute();
		} catch (final IOException e) {
			throw new MailServiceException("adding tag", e);
		} finally {
			idLabels.clear();
		}
	}

	@Override
	public GmailTag findTag(final String name) throws TagServiceException {
		for(final Label l: getLabels().values()) {
			if (l.getName().equalsIgnoreCase(name)) {
				return new GmailTag(l);
			}
		}
		return null;
	}

	@Override
	public void rename(final GmailSection section, final String name) throws TagServiceException {
		final Set<GmailTag> tags = getTags(section, "");
		Label label = getLabels().get(section.getId());
		try {
			label = rename(label, name);
		} catch (final IOException e) {
			throw new TagServiceException("rename section " + label.getName(), e);
		}

		// move tags to new section
		final GmailSection newSection = new GmailSection(label);
		for(final GmailTag t: tags) {
			moveToSection(newSection, t);
		}
	}

	private Label rename(Label label, final String name) throws IOException {
		try {
			String newName = label.getName();
			if (newName.contains("/")) {
				newName = newName.substring(0, newName.lastIndexOf("/"));
			} else {
				newName = "";
			}
			newName += name;
			label.setName(newName);
			label = delegate.users().labels().update(USER, label.getId(), label).execute();

			return label;
		} finally {
			idLabels.clear();
		}
	}

	@Override
	public void rename(final GmailTag tag, final String name) throws TagServiceException {
		try {
			rename(getLabels().get(tag.getId()), name);
		} catch (final IOException e) {
			throw new TagServiceException("rename tag " + tag.getName(), e);
		}
	}

	@Override
	public GmailSection addSection(final String name) throws TagServiceException {

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GmailTag createTag(final String name) throws TagServiceException {

		Label label = new Label();
		label.setMessageListVisibility("show");
		label.setLabelListVisibility("labelShow");
		label.setName(name);
		try {
			label = delegate.users().labels().create(USER, label).execute();
		} catch (final IOException e) {
			throw new TagServiceException("adding " + name, e);
		} finally {
			idLabels.clear();
		}

		return new GmailTag(label);
	}

	@Override
	public void remove(final GmailSection section) throws TagServiceException {
		try {
			delegate.users().labels().delete(USER, section.getId());
		} catch (final IOException e) {
			throw new TagServiceException("remove section " + section.getName(), e);
		} finally {
			idLabels.clear();
		}
	}

	@Override
	public void remove(final GmailTag tag) throws TagServiceException {
		try {
			delegate.users().labels().delete(USER, tag.getId());
		} catch (final IOException e) {
			throw new TagServiceException("remove tag " + tag.getName(), e);
		} finally {
			idLabels.clear();
		}
	}

	@Override
	public void archive(final Set<GmailThread> threads) throws MailServiceException {
		try {
			@SuppressWarnings("serial")
			final List<String> inboxId = new ArrayList<String>() {{ add(findTag("INBOX").getId());}};
			final ModifyThreadRequest request = new ModifyThreadRequest().setRemoveLabelIds(inboxId);
			for(final GmailThread t: threads) {
				delegate.users().threads().modify(USER, t.getId(), request).execute();
			}
		} catch (final IOException | TagServiceException e) {
			throw new MailServiceException("trashing threads " + threads, e);
		}
	}

	@Override
	public void delete(final Set<GmailThread> threads) throws MailServiceException {
		try {
			for(final GmailThread t: threads) {
				delegate.users().threads().trash(USER, t.getId()).execute();
			}
		} catch (final IOException e) {
			throw new MailServiceException("trashing threads " + threads, e);
		}
	}
}
