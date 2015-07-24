package net.anfoya.mail.service;

import java.util.Set;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.util.Callback;
import net.anfoya.tag.service.TagService;

public interface MailService<
		S extends Section
		, T extends Tag
		, H extends Thread
		, M extends Message
		, C extends Contact>
		extends TagService<S, T> {

	public void connect(String appName) throws MailException;
	public void reconnect();
	public void disconnect();
	public ReadOnlyBooleanProperty disconnected();

	public void clearCache();

	public void addOnUpdateMessage(Callback<Void, Void> callback);
	public void addOnUnreadMessage(Callback<Set<H>, Void> callback);

	public T getTag(String id) throws MailException;
	public T getSpecialTag(SpecialTag specialTag) throws MailException;
	public T findTag(String name) throws MailException;
	public void addTagForThreads(T tag, Set<H> threads) throws MailException;
	public void removeTagForThreads(T tag, Set<H> thread) throws MailException;

	public Set<H> findThreads(Set<T> includes, Set<T> excludes, String pattern, int pageMax) throws MailException;
	public void archive(Set<H> threads) throws MailException;
	public void trash(Set<H> threads) throws MailException;

	public M getMessage(String id) throws MailException;
	public void remove(M message) throws MailException;

	public M createDraft(M message) throws MailException;
	public M getDraft(String id) throws MailException;
	public void send(M draft) throws MailException;
	public void save(M draft) throws MailException;

	public C getContact();
	public Set<C> getContacts() throws MailException;
}
