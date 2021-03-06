package net.anfoya.mail.gmail.service;

import java.io.IOException;

import javax.mail.MessagingException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;

import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.model.GmailMessage;
import net.anfoya.mail.service.MailException;

public class MessageServiceTest {
	private static final String TEST_SUBJECT = "mail_service_test";

	private MessageService service;
	private String messageId;

	@Before public void init() throws MailException, ThreadException {
		final GmailService gmail = new GmailService("testApp");
		gmail.authenticate();
		service = gmail.getMessageService();
		messageId = gmail.getThreadService().find("subject:" + TEST_SUBJECT, 1).iterator().next().getMessages().get(0).getId();
	}

	@Test public void get() throws MessageException {
		final Message message = service.getMessage(messageId);
		Assert.assertNotNull(message);
	}

	@Test public void draft() throws MessageException {
		final Draft draft = service.createDraft();
		Assert.assertNotNull(draft);

		//cleanup
		service.removeDraft(draft.getId());
	}

	@Test public void raw() throws MessageException, MessagingException, IOException {
		final Message message = service.getMessage(messageId);
		final GmailMessage gmailMessage = new GmailMessage(message);
		Assert.assertEquals(message.getRaw(), gmailMessage.getRaw());
	}
}
