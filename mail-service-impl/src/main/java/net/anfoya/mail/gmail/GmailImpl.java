package net.anfoya.mail.gmail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.security.auth.login.LoginException;

import net.anfoya.java.io.JsonFile;
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

	private final Map<String, Label> idLabels = new HashMap<String, Label>();

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
	public Set<GmailThread> getThreads(final Set<GmailTag> availableTags, final Set<GmailTag> includes, final Set<GmailTag> excludes) throws MailServiceException {
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

		try {
			ListThreadsResponse threadResponse = delegate.users().threads().list(USER).setQ(query.toString()).execute();
			while (threadResponse.getThreads() != null) {
				for(final Thread t : threadResponse.getThreads()) {
					final Thread thread = delegate.users().threads().get(USER, t.getId()).setFormat("metadata") .execute();
					if (thread != null) {
						threads.add(new GmailThread(thread));
					}
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

		return threads;
	}

	@Override
	public MimeMessage getMessage(final String id) throws MailServiceException {
		try {
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
		final Set<GmailSection> sections = new TreeSet<GmailSection>();
		for(final Label l: getLabels().values()) {
			final GmailSection section = new GmailSection(l);
			if (!section.isHidden()) {
				sections.add(section);
			}
		}

		for(final Iterator<GmailSection> i=sections.iterator(); i.hasNext();) {
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

		sections.add(GmailSection.NO_SECTION);

		LOGGER.info("sections: {}", sections);
		return sections;
	}

	@Override
	public Set<GmailTag> getTags(final GmailSection section, final String tagPattern) throws TagServiceException {
		final Set<GmailTag> tags = new TreeSet<GmailTag>();
		for(final Label l: getLabels().values()) {
			final GmailTag tag = new GmailTag(l);
			if (!tag.isHidden()
					&& (section == null
							|| section.getId().equals(GmailSection.NO_SECTION.getId()) && !l.getName().contains("/")
							|| l.getName().startsWith(section.getName()+"/") && !l.getName().substring(section.getName().length()+1, l.getName().length()).contains("/"))
					&& tag.getName().contains(tagPattern)) {
				tags.add(tag);
			}
		}

		LOGGER.info("tags for section({}) tagPattern({}): {}", section == null? "": section.getPath(), tagPattern, tags);
		return tags;
	}

	@Override
	public Set<GmailTag> getTags() throws TagServiceException {
		return getTags(null, "");
	}

	@Override
	public GmailSection addSection(final String sectionName) throws TagServiceException {
		Label label = new Label();
		label.setMessageListVisibility("show");
		label.setLabelListVisibility("labelShow");
		label.setName(sectionName);
		try {
			label = delegate.users().labels().create(USER, label).execute();
		} catch (final IOException e) {
			throw new TagServiceException("adding " + sectionName, e);
		}

		idLabels.put(label.getId(), label);
		return new GmailSection(label);
	}

	@Override
	public void moveToSection(final GmailSection section
			, final GmailTag tag) throws TagServiceException {
		Label label = getLabels().get(tag.getId());
		label.setName(section.getName() + "/" + tag.getName());
		try {
			label = delegate.users().labels().update(USER, label.getId(), label).execute();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		idLabels.put(label.getId(), label);
	}

	@Override
	public int getCountForTags(final Set<GmailTag> includes, final Set<GmailTag> excludes, final String mailPattern) throws TagServiceException {
		if (includes.isEmpty()) {
			return 0;
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

		LOGGER.debug("count for tag includes({}) excludes({}) mailPattern({}): {}", includes, excludes, mailPattern, count);
		LOGGER.debug("query({})", query);

		return count;
	}

	@Override
	public int getCountForSection(final GmailSection section
			, final Set<GmailTag> includes, final Set<GmailTag> excludes
			, final String namePattern, final String tagPattern) throws TagServiceException {

		final StringBuilder query = new StringBuilder();
		boolean multiple;

		// label in section
		query.append("(");
		multiple = false;
		for (final GmailTag t: getTags(section, tagPattern)) {
			if (!includes.contains(t) && !excludes.contains(t)) {
				if (multiple) {
					query.append(" OR ");
				}
				query.append("label:").append(t.getPath().replaceAll("/", "-").replaceAll(" ", "-"));
				multiple = true;
			}
		}
		query.append(")");

		// includes
		if (includes.size() > 0) {
			query.append(" AND (");
			multiple = false;
			for (final GmailTag t: includes) {
				if (multiple) {
					query.append(" AND ");
				}
				query.append("label:").append(t.getPath().replaceAll("/", "-").replaceAll(" ", "-"));
				multiple = true;
			}
			query.append(")");
		}

		// includes
		if (excludes.size() > 0) {
			query.append(" AND (");
			multiple = false;
			for (final GmailTag t: excludes) {
				if (multiple) {
					query.append(" AND ");
				}
				query.append("label:").append(t.getPath().replaceAll("/", "-").replaceAll(" ", "-"));
				multiple = true;
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

		LOGGER.info("count for section({}) includes({}) excludes({}) tagPattern(\"{}\"): {}", section, includes, excludes, tagPattern, count);
		LOGGER.info("query({})", query);
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
		}
		return idLabels;
	}
}
