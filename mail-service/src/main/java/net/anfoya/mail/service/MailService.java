package net.anfoya.mail.service;

import java.util.Set;

import javax.mail.internet.MimeMessage;
import javax.security.auth.login.LoginException;

import net.anfoya.mail.model.MessageThread;
import net.anfoya.tag.model.TagSection;
import net.anfoya.tag.model.ThreadTag;
import net.anfoya.tag.service.TagService;

public interface MailService<S extends TagSection, T extends ThreadTag, H extends MessageThread> extends TagService<S, T> {

	public void login(String id, String pwd) throws LoginException;
	public void logout();
	public Set<H> getThreads(Set<T> availableTags, Set<T> includes, Set<T> excludes) throws MailServiceException;
	public MimeMessage getMessage(String id) throws MailServiceException;
}
