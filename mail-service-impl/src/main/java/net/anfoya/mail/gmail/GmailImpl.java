package net.anfoya.mail.gmail;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.security.auth.login.LoginException;

import net.anfoya.java.io.JsonFile;
import net.anfoya.java.io.SerializedFile;
import net.anfoya.mail.model.Message;
import net.anfoya.mail.model.Thread;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceException;
import net.anfoya.tag.model.Section;
import net.anfoya.tag.model.Tag;
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

public class GmailImpl implements MailService {
	private static final Logger LOGGER = LoggerFactory.getLogger(GmailImpl.class);

	// Check https://developers.google.com/gmail/api/auth/scopes for all
	// available scopes
	private static final String SCOPE = "https://www.googleapis.com/auth/gmail.readonly";
	private static final String APP_NAME = "AGARAM";
	// Email address of the user, or "me" can be used to represent the currently
	// authorized user.
	private static final String USER = "me";
	// Path to the client_secret.json file downloaded from the Developer Console
	private static final String CLIENT_SECRET_PATH = "client_secret.json";

	private final HttpTransport httpTransport;
	private final JsonFactory jsonFactory;
	private final Map<Section, Set<Tag>> sectionTags;
	private final SerializedFile<Map<Section, Set<Tag>>> sectionTagsFile;

	private Gmail delegate = null;

	private Set<Label> labels = null;

	public GmailImpl() {
		httpTransport = new NetHttpTransport();
		jsonFactory = new JacksonFactory();

		sectionTagsFile = new SerializedFile<Map<Section,Set<Tag>>>("sectionTags.json");

		Map<Section, Set<Tag>> sectionTags;
		try {
			sectionTags = sectionTagsFile.load();
		} catch (ClassNotFoundException | IOException e) {
			sectionTags = new ConcurrentHashMap<Section, Set<Tag>>();
		}
		this.sectionTags = sectionTags;
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
	public List<Thread> getThreads(final List<Tag> tags) throws MailServiceException {
		final List<Thread> threads = new ArrayList<Thread>();
		if (tags.isEmpty()) {
			return threads;
		}
		final List<String> labelIds = new ArrayList<String>();
		for(final Tag t: tags) {
			labelIds.add(t.getId());
		}
		List<com.google.api.services.gmail.model.Thread> lastThreads;
		try {
			final ListThreadsResponse response = delegate.users().threads().list(USER).setLabelIds(labelIds).execute();
			if (response.getThreads() == null) {
				lastThreads = new ArrayList<com.google.api.services.gmail.model.Thread>();
			} else {
				lastThreads = response.getThreads();
			}
		} catch (final IOException e) {
			throw new MailServiceException("loading threads for: " + tags.toString(), e);
		}
		for(final com.google.api.services.gmail.model.Thread t : lastThreads) {
			threads.add(new Thread(t.getId(), t.getSnippet()));
		}

		return threads;
	}

	@Override
	public List<String> getMessageIds(final String threadId) {
		final List<String> ids = new ArrayList<String>();
		try {
			final com.google.api.services.gmail.model.Thread thread = delegate.users().threads().get(USER, threadId).execute();
			for(final com.google.api.services.gmail.model.Message m: thread.getMessages()) {
				ids.add(m.getId());
			}
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ids;
	}

	@Override
	public Message getMessage(final String id) {
		try {
			final com.google.api.services.gmail.model.Message message = delegate.users().messages().get(USER, id).execute();
			return new Message(id, message.getSnippet());
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Set<Section> getSections() throws TagServiceException {
		final Set<Section> sections = new TreeSet<Section>();
		for(final Label l: getLabels()) {
			final Section section = buildSection(l);
			if (section != null) {
				sections.add(section);
			}
		}
		sections.add(Section.NO_SECTION);

		LOGGER.info("sections: {}", sections);
		return sections;
	}

	@Override
	public Set<Tag> getTags(final Section section, final String tagPattern) throws TagServiceException {
		final Set<Tag> tags = new TreeSet<Tag>();
		for(final Label l: getLabels()) {
			final Tag tag = buildTag(l);
			if (tag != null
					&& (section == null
						|| section == Section.NO_SECTION && !l.getName().contains("/")
						|| l.getName().contains(section.getName()))
					&& tag.getName().contains(tagPattern)) {
				tags.add(tag);
			}
		}

		LOGGER.info("tags for section({}) tagPattern({}): {}", section, tagPattern, tags);
		return tags;
	}

	@Override
	public Set<Tag> getTags() throws TagServiceException {
		return getTags(null, "");
	}

	@Override
	public void addToSection(final Section section, final Tag tag) {
	}

	@Override
	public int getTagCount(final Set<Tag> includes, final Set<Tag> excludes, final String mailPattern) throws TagServiceException {
		if (includes.isEmpty()) {
			return 0;
		}
		final List<String> includeIds = new ArrayList<String>();
		for(final Tag t: includes) {
			includeIds.add(t.getId());
		}

		int count = 0;
		try {
			final ListThreadsResponse response = delegate.users().threads().list(USER).setLabelIds(includeIds).execute();
			if (response.getThreads() != null) {
				count = response.getThreads().size();
			}
		} catch (final IOException e) {
			throw new TagServiceException("counting threads for " + includes.toString(), e);
		}

		LOGGER.info("count for includes({}) excludes({}) mailPattern({}): {}"
				, includes, excludes, mailPattern, count);

		return count;
	}

	@Override
	public int getSectionCount(final Section section
			, final Set<Tag> includes, final Set<Tag> excludes
			, final String namePattern, final String tagPattern) throws TagServiceException {
		final String sectionName;
		if (Section.NO_SECTION.equals(section)) {
			sectionName = "";
		} else {
			sectionName = section.getName().replaceAll("/", "-") + "-";
		}
		final StringBuilder sectionLabels = new StringBuilder();
		for(final Tag t: getTags(section, tagPattern)) {
			if (sectionLabels.length() > 0) {
				sectionLabels.append(" OR ");
			}
			sectionLabels.append("label:").append(sectionName).append(t.getName());
		}
		int count = 0;
		try {
			final ListThreadsResponse response = delegate.users().threads().list(USER).setQ(sectionLabels.toString()).execute();
			if (response.getThreads() != null) {
				count = response.getThreads().size();
			}
		} catch (final IOException e) {
			throw new TagServiceException("counting threads for " + includes.toString(), e);
		}

		LOGGER.info("count for section({}) includes({}) excludes({}) tagPattern({}): {}"
				, section, includes, excludes, tagPattern, count);
		return count;
	}

	@Override
	public Section addSection(final String sectionName) {
		final Section section = new Section(sectionName);
		sectionTags.put(section, new CopyOnWriteArraySet<Tag>());
		return section;
	}

	private Tag buildTag(final Label l) {
		if ("labelHide".equals(l.getLabelListVisibility())) {
			return null;
		}

		String name = l.getName();
		if (name.contains("/") && name.length() > 1) {
			name = name.substring(name.lastIndexOf("/")+1);
		}
		return new Tag(l.getId(), name);
	}

	private Section buildSection(final Label l) {
		if ("labelHide".equals(l.getLabelListVisibility())
				|| !l.getName().contains("/")) {
			return null;
		}

		String name = l.getName();
		name = name.substring(0, name.lastIndexOf("/"));

		return new Section(l.getId(), name);
	}

	private Set<Label> getLabels() throws TagServiceException {
		if (labels == null) {
			try {
				labels = new LinkedHashSet<Label>(delegate.users().labels().list(USER).execute().getLabels());
			} catch (final IOException e) {
				throw new TagServiceException("getting labels", e);
			}
		}
		return labels;
	}
}
