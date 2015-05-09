package net.anfoya.mail.service;

import java.util.Set;

import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;
import net.anfoya.tag.service.TagService;

public interface MailService<S extends SimpleSection
		, T extends SimpleTag
		, H extends SimpleThread
		, M extends SimpleMessage> extends TagService<S, T> {

	public void login(String id, String pwd) throws MailException;
	public void logout();

	public T getTag(String id) throws MailException;
	public T findTag(String name) throws MailException;
	public void addTag(T tag, Set<H> threads) throws MailException;
	public void remTag(T tag, H thread) throws MailException;
	
	public H getThread(String id) throws MailException;
	public Set<H> getThreads(Set<T> availableTags, Set<T> includes, Set<T> excludes, String pattern) throws MailException;
	public void archive(Set<H> threads) throws MailException;
	public void delete(Set<H> threads) throws MailException;

	public M getMessage(String id) throws MailException;
}
