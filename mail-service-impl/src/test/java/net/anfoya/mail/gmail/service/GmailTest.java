package net.anfoya.mail.gmail.service;

import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.gmail.model.GmailThread;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagException;

import org.junit.Test;

public class GmailTest {

	@Test
	public void login() throws MailException {
		final MailService<GmailSection, GmailTag, GmailThread, SimpleMessage> service = new GmailService();
		service.login("", "");
		service.logout();
	}

	@Test
	public void getTags() throws MailException, TagException {
		final MailService<GmailSection, GmailTag, GmailThread, SimpleMessage> service = new GmailService();
		service.login("", "");
		for(final SimpleTag t: service.getTags()) {
			System.out.println(t);
		}
		service.logout();
	}
}
