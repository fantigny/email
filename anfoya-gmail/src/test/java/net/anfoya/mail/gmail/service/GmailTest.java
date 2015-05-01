package net.anfoya.mail.gmail.service;

import java.util.HashSet;

import net.anfoya.mail.gmail.GmailImpl;
import net.anfoya.mail.model.Tag;
import net.anfoya.mail.service.MailService;

import org.junit.Test;

public class GmailTest {

	@Test
	public void login() {
		final MailService service = new GmailImpl();
		service.login("", "");
		service.logout();
	}

	@Test
	public void getHeaders() {
		final MailService service = new GmailImpl();
		service.login("", "");
		service.getHeaders(new HashSet<Tag>());
	}
}
