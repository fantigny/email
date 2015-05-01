package net.anfoya.mail.gmail.service;

import net.anfoya.mail.gmail.GmailImpl;
import net.anfoya.mail.service.MailService;

import org.junit.Test;

public class GmailTest {

	@Test
	public void login() {
		final MailService service = new GmailImpl();
		service.login("", "");
	}
}
