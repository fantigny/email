package net.anfoya.mail.gmail;

import net.anfoya.mail.gmail.service.MessageException;
import net.anfoya.mail.gmail.service.MessageService;
import net.anfoya.mail.gmail.service.ThreadException;
import net.anfoya.mail.gmail.service.ThreadService;
import net.anfoya.mail.service.MailException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;

public class MessageServiceTest {
	private static final String TEST_SUBJECT = "mail_service_test";

	private MessageService service;
	private String messageId;

	@Before public void init() throws MailException, ThreadException {
		final Gmail gmail = new GmailService().login("test");
		service = new MessageService(gmail, "me");
		messageId = new ThreadService(gmail, "me").find("subject:" + TEST_SUBJECT).iterator().next().getMessages().get(0).getId();
	}

	@Test public void get() throws MessageException {
		final Message message = service.getMessage(messageId);
		Assert.assertNotNull(message);
	}

	@Test public void createDelete() throws MessageException {
		final Draft draft = service.createDraft();
		Assert.assertNotNull(draft);

		//cleanup
		service.removeDraft(draft.getId());
	}
}
