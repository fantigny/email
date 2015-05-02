package net.anfoya.mail.gmail;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.anfoya.java.io.JsonFile;
import net.anfoya.mail.model.Thread;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceException;
import net.anfoya.tag.model.Tag;

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

public class GmailImpl implements MailService {

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

	private Gmail delegate = null;

	private List<com.google.api.services.gmail.model.Thread> lastThreads = null;

	public GmailImpl() {
		httpTransport = new NetHttpTransport();
		jsonFactory = new JacksonFactory();
	}

	@Override
	public void login(final String id, final String pwd) throws MailServiceException {
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
			refreshTokenFile.save(refreshToken);
		} catch (final URISyntaxException | IOException e) {
			throw new MailServiceException("", e);
		}
	}

	@Override
	public void logout() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Tag> getTags() throws MailServiceException {
		try {
			final List<Tag> tags = new ArrayList<Tag>();
			for(final Label l: delegate.users().labels().list(USER).execute().getLabels()) {
				final String name = l.getName();
				if (!name.startsWith("CATEGORY_")) {
					tags.add(new Tag(l.getId(), l.getName(), "Main"));
				}
			}
			return tags;
		} catch (final IOException e) {
			throw new MailServiceException("getting tags", e);
		}
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
			final List<String> mailIds = new ArrayList<String>();
			if (t.getMessages() != null) {
				for(final Message m: t.getMessages()) {
					mailIds.add(m.getId());
				}
			}
			threads.add(new Thread(t.getId(), t.getSnippet(), mailIds));
		}

		return threads;
	}

	@Override
	public String getMail(final String mailId) {
		String mail = "";
		if (lastThreads != null) {
			for(final com.google.api.services.gmail.model.Thread t: lastThreads) {
				for(final Message m: t.getMessages()) {
					if (m.getId().equals(mailId)) {
						mail = m.getPayload().getParts().get(0).getBody().getData();
					}
				}
			}
		}
		return mail;
	}
}
