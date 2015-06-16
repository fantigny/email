package net.anfoya.mail.gmail;

import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.Tag;
import net.anfoya.tag.service.TagException;

import org.junit.Test;

public class GmailTest {

	@Test
	public void login() throws MailException {
		final GmailService service = new GmailService();
		service.connect("test");
	}

	@Test
	public void getTags() throws MailException, TagException {
		final GmailService service = new GmailService();
		service.connect("test");
		for(final Tag t: service.getTags()) {
			System.out.println(t);
		}
	}
}
