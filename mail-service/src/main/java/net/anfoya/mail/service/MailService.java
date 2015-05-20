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

	public Object login(String mailId) throws MailException;
	public void logout();
	public boolean hasUpdate() throws MailException;

	public T getTag(String id) throws MailException;
	public T findTag(String name) throws MailException;
	public void addTagForThreads(T tag, Set<H> threads) throws MailException;
	public void removeTagForThread(T tag, H thread) throws MailException;

	public Set<H> getThreads(Set<T> includes, Set<T> excludes, String pattern) throws MailException;
	public void archive(Set<H> threads) throws MailException;
	public void trash(Set<H> threads) throws MailException;

	public M getMessage(String id) throws MailException;

	public M createDraft() throws MailException;
	public void remove(M message) throws MailException;
}
