package net.anfoya.mail.service;

import java.util.Set;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.util.Callback;
import net.anfoya.mail.model.Contact;
import net.anfoya.mail.model.Message;
import net.anfoya.mail.model.Section;
import net.anfoya.mail.model.Tag;
import net.anfoya.mail.model.Thread;
import net.anfoya.tag.model.SpecialSection;
import net.anfoya.tag.model.SpecialTag;
import net.anfoya.tag.service.TagService;

public interface MailService<
S extends Section
, T extends Tag
, H extends Thread
, M extends Message
, C extends Contact>
extends TagService<S, T> {

	ReadOnlyBooleanProperty connected();
	void reconnect() throws MailException;

	void authenticate();
	void setOnAuth(Runnable callback);
	void setOnAuthFailed(Runnable callback);
	MailException getAuthException();

	void signout();

	void startListening();
	void stopListening();

	void clearCache();

	void addOnUpdateMessage(final Runnable callback);
	void addOnNewMessage(Callback<Set<H>, Void> callback);

	Set<S> getHiddenSections() throws MailException;

	H getThread(String id) throws MailException;
	Set<H> findThreads(Set<T> includes, Set<T> excludes, String pattern, int pageMax) throws MailException;
	void addTagForThreads(T tag, Set<H> threads) throws MailException;
	void removeTagForThreads(T tag, Set<H> thread) throws MailException;

	void archive(Set<H> threads) throws MailException;
	void trash(Set<H> threads) throws MailException;

	M getMessage(String id) throws MailException;
	void remove(M message) throws MailException;

	M createDraft(M message) throws MailException;
	M getDraft(String id) throws MailException;
	void send(M draft) throws MailException;
	void save(M draft) throws MailException;

	C getContact();
	Set<C> getContacts() throws MailException;

	void persistBytes(String id, byte[] bytes) throws MailException;
	byte[] readBytes(String id) throws MailException;

	@Override S getSpecialSection(SpecialSection section);
	@Override long getCountForSection(S section, Set<T> includes, Set<T> excludes, String itemPattern) throws MailException;

	@Override S addSection(String name) throws MailException;
	@Override void remove(S Section) throws MailException;
	@Override S rename(S Section, String name) throws MailException;
	@Override void hide(S Section) throws MailException;
	@Override void show(S Section) throws MailException;

	@Override T findTag(String name) throws MailException;
	@Override T getTag(String id) throws MailException;
	@Override T getSpecialTag(SpecialTag specialTag);

	@Override Set<T> getTags(String pattern) throws MailException;
	@Override Set<T> getTags(S section) throws MailException;
	@Override long getCountForTags(Set<T> includes, Set<T> excludes, String pattern) throws MailException;
	@Override Set<T> getHiddenTags() throws MailException;

	@Override T addTag(String name) throws MailException;
	@Override void remove(T tag) throws MailException;
	@Override T rename(T tag, String name) throws MailException;
	@Override void hide(T tag) throws MailException;
	@Override void show(T tag) throws MailException;

	@Override T moveToSection(T tag, S section) throws MailException;

	@Override void addOnUpdateTagOrSection(Runnable callback);
}
