package net.anfoya.mail.gmail.service;

import java.util.HashSet;

import javax.security.auth.login.LoginException;

import net.anfoya.mail.gmail.GmailImpl;
import net.anfoya.mail.gmail.model.GmailSection;
import net.anfoya.mail.gmail.model.GmailTag;
import net.anfoya.mail.model.Thread;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceException;
import net.anfoya.tag.model.Tag;
import net.anfoya.tag.service.TagServiceException;

import org.junit.Test;

public class GmailTest {

	@Test
	public void login() throws LoginException {
		final MailService<GmailSection, GmailTag> service = new GmailImpl();
		service.login("", "");
		service.logout();
	}

	@Test
	public void getHeaders() throws MailServiceException, LoginException {
		final MailService<GmailSection, GmailTag> service = new GmailImpl();
		service.login("", "");
		for(final Thread t: service.getThreads(new HashSet<GmailTag>(), new HashSet<GmailTag>(), new HashSet<GmailTag>())) {
			System.out.println(t.getId());
		}
		service.logout();
	}

	@Test
	public void getTags() throws TagServiceException, LoginException {
		final MailService<GmailSection, GmailTag> service = new GmailImpl();
		service.login("", "");
		for(final Tag t: service.getTags()) {
			System.out.println(t);
		}
		service.logout();
	}
}
