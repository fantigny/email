package net.anfoya.mail.gmail.service;

import java.util.ArrayList;

import net.anfoya.mail.gmail.GmailImpl;
import net.anfoya.mail.model.Tag;
import net.anfoya.mail.model.Thread;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceException;

import org.junit.Test;

public class GmailTest {

	@Test
	public void login() throws MailServiceException {
		final MailService service = new GmailImpl();
		service.login("", "");
		service.logout();
	}

	@Test
	public void getHeaders() throws MailServiceException {
		final MailService service = new GmailImpl();
		service.login("", "");
		for(final Thread t: service.getThreads(new ArrayList<Tag>())) {
			System.out.println(t.getId());
		}
		service.logout();
	}

	@Test
	public void getTags() throws MailServiceException {
		final MailService service = new GmailImpl();
		service.login("", "");
		for(final Tag t: service.getTags()) {
			System.out.println(t);
		}
		service.logout();
	}
}
