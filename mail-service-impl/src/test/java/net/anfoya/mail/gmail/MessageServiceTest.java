package net.anfoya.mail.gmail;

import net.anfoya.mail.service.MailException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;

public class MessageServiceTest {
	private static final String TEST_SUBJECT = "mail_service_test";

	private MessageService service;
	private String messageId;

	@Before public void init() throws MailException, ThreadException {
		final Gmail gmail = new GmailService().login();
		service = new MessageService(gmail, "me");
		messageId = new ThreadService(gmail, "me").find("subject:" + TEST_SUBJECT).iterator().next().getMessages().get(0).getId();
	}

	@Test public void get() throws MessageException {
		final byte[] raw = service.getRaw(messageId);
		Assert.assertNotNull(raw);
	}

	@Test public void createDelete() throws MessageException {
		final Draft draft = service.createDraft();
		Assert.assertNotNull(draft);

		//cleanup
		service.deleteDraft(draft.getId());
	}
}
