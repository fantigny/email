package net.anfoya.mail.gmail.service;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

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
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Thread;

public class GmailApiQuickstart {

  // Check https://developers.google.com/gmail/api/auth/scopes for all available scopes
  private static final String SCOPE = "https://www.googleapis.com/auth/gmail.readonly";
  private static final String APP_NAME = "Gmail API Quickstart";
  // Email address of the user, or "me" can be used to represent the currently authorized user.
  private static final String USER = "me";
  // Path to the client_secret.json file downloaded from the Developer Console
  private static final String CLIENT_SECRET_PATH = "client_secret.json";

  private static GoogleClientSecrets clientSecrets;

  public static void main (final String [] args) throws IOException {
    final HttpTransport httpTransport = new NetHttpTransport();
    final JsonFactory jsonFactory = new JacksonFactory();

    clientSecrets = GoogleClientSecrets.load(jsonFactory, new FileReader(CLIENT_SECRET_PATH));

    // Allow user to authorize via url.
    final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        httpTransport, jsonFactory, clientSecrets, Arrays.asList(SCOPE))
        .setAccessType("online")
        .setApprovalPrompt("auto").build();

    final String url = flow.newAuthorizationUrl().setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI)
        .build();
    System.out.println("Please open the following URL in your browser then type"
                       + " the authorization code:\n" + url);

    // Read code entered by user.
    final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    final String code = br.readLine();

    // Generate Credential using retrieved code.
    final GoogleTokenResponse response = flow.newTokenRequest(code)
        .setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI).execute();
    final GoogleCredential credential = new GoogleCredential()
        .setFromTokenResponse(response);

    // Create a new authorized Gmail API client
    final Gmail service = new Gmail.Builder(httpTransport, jsonFactory, credential)
        .setApplicationName(APP_NAME).build();

    // Retrieve a page of Threads; max of 100 by default.
    final ListThreadsResponse threadsResponse = service.users().threads().list(USER).execute();
    final List<Thread> threads = threadsResponse.getThreads();

    // Print ID of each Thread.
    for (final Thread thread : threads) {
      System.out.println("Thread ID: " + thread.getId());
    }
  }

}