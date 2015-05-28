package net.anfoya.mail.gmail;

import net.anfoya.mail.service.MailException;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagException;

import org.junit.Test;

public class GmailTest {

	@Test
	public void login() throws MailException {
		final GmailService service = new GmailService();
		service.login("test");
		service.logout();
	}

	@Test
	public void getTags() throws MailException, TagException {
		final GmailService service = new GmailService();
		service.login("test");
		for(final SimpleTag t: service.getTags()) {
			System.out.println(t);
		}
		service.logout();
	}
}
