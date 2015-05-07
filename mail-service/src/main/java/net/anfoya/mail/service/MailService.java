package net.anfoya.mail.service;

//aflaml12

import java.util.Set;

import javax.mail.internet.MimeMessage;
import javax.security.auth.login.LoginException;

import net.anfoya.mail.model.SimpleThread;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagService;
import net.anfoya.tag.service.TagServiceException;

public interface MailService<S extends SimpleSection, T extends SimpleTag, H extends SimpleThread> extends TagService<S, T> {

	public void login(String id, String pwd) throws LoginException;
	public void logout();
	public Set<H> getThreads(Set<T> availableTags, Set<T> includes, Set<T> excludes, String pattern) throws MailServiceException;

	public T getTag(String id) throws TagServiceException;
	public H getThread(String id) throws MailServiceException;
	public MimeMessage getMessage(String id) throws MailServiceException;

	public void addTag(T tag, Set<H> threads) throws MailServiceException;
	public void remTag(T tag, H thread) throws MailServiceException;
	public T findTag(String name) throws TagServiceException;
}
