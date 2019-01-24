package net.anfoya.mail.gmail;

import org.junit.Test;

import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.Tag;
import net.anfoya.tag.service.TagException;

public class GmailTest {

	@Test
	public void login() throws MailException {
		new GmailService("testApp").authenticate();
	}

	@Test
	public void getTags() throws MailException, TagException {
		final GmailService gmail = new GmailService("testApp");
		gmail.authenticate();
		for(final Tag t: gmail.getTags("")) {
			System.out.println(t);
		}
	}

	@Test
	public void clearCache() throws MailException, TagException {
		final GmailService gmail = new GmailService("testApp");
		gmail.authenticate();
		gmail.clearCache();
	}
}
