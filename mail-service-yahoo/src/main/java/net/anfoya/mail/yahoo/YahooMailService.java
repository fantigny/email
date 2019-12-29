package net.anfoya.mail.yahoo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.util.Callback;
import net.anfoya.mail.model.SimpleContact;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleSection;
import net.anfoya.mail.model.SimpleTag;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.yahoo.model.YahooTag;
import net.anfoya.mail.yahoo.model.YahooThread;
import net.anfoya.mail.yahoo.service.AuthenticationService;
import net.anfoya.tag.model.SpecialTag;

public class YahooMailService
implements MailService<SimpleSection, SimpleTag, SimpleThread, SimpleMessage, SimpleContact> {
	private static final String USER = "me";

	private final ReadOnlyBooleanWrapper connected;

	private final AuthenticationService authService;
	private Store yahooMail;

	private SimpleContact contact;

	public YahooMailService(String appName) {
		authService = new AuthenticationService(appName, USER);
		connected = new ReadOnlyBooleanWrapper(true);
	}

	@Override
	public void authenticate() {
		authService.authenticate();
	}

	@Override
	public ReadOnlyBooleanProperty connected() {
		return connected.getReadOnlyProperty();
	}

	@Override
	public void reconnect() throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setOnAuth(Runnable callback) {
		authService.setOnAuth(() -> {
			yahooMail = authService.getYahooMail();
			contact = authService.getContact();

			callback.run();
		});
	}

	@Override
	public void setOnAuthFailed(Runnable callback) {
		authService.setOnAuthFailed(callback);
	}

	@Override
	public MailException getAuthException() {
		return authService.getException();
	}

	@Override
	public void signout() {
		authService.signout();
	}

	@Override
	public void startListening() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stopListening() {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearCache() {
		// TODO Auto-generated method stub

	}

	@Override
	public void addOnUpdateMessage(Runnable callback) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addOnNewMessage(Callback<Set<SimpleThread>, Void> callback) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<SimpleSection> getHiddenSections() throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SimpleThread getThread(String id) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<SimpleThread> findThreads(Set<SimpleTag> includes, Set<SimpleTag> excludes, String pattern, int pageMax)
			throws MailException {

		final Set<SimpleThread> threads = new HashSet<>();
		try {
			String tag = includes.iterator().next().getName().toLowerCase();
			Folder folder = yahooMail.getFolder(tag);
			if (folder.isOpen()) {
				for (Message message : folder.getMessages()) {
					threads.add(new YahooThread(message));
				}
			}
		} catch (MessagingException e) {
			throw new YahooException("", e);
		}
		return threads;
	}

	@Override
	public void addTagForThreads(SimpleTag tag, Set<SimpleThread> threads) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTagForThreads(SimpleTag tag, Set<SimpleThread> thread) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public void archive(Set<SimpleThread> threads) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public void trash(Set<SimpleThread> threads) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public SimpleMessage getMessage(String id) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(SimpleMessage message) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public SimpleMessage createDraft(SimpleMessage message) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SimpleMessage getDraft(String id) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void send(SimpleMessage draft) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public void save(SimpleMessage draft) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public SimpleContact getContact() {
		return contact;
	}

	@Override
	public Set<SimpleContact> getContacts() throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void persistBytes(String id, byte[] bytes) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public byte[] readBytes(String id) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<SimpleSection> getSections() throws MailException {
		final Set<SimpleSection> sections = new HashSet<>();
		try {
			Folder[] folders = yahooMail.getPersonalNamespaces();
			for(Folder f: folders) {
				sections.add(new SimpleSection(f.getFullName()));
			}
		} catch (MessagingException e) {
			throw new YahooException("", e);
		}
		return sections;
	}

	@Override
	public long getCountForSection(SimpleSection section, Set<SimpleTag> includes, Set<SimpleTag> excludes,
			String itemPattern) throws MailException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public SimpleSection addSection(String name) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(SimpleSection Section) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public SimpleSection rename(SimpleSection Section, String name) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void hide(SimpleSection Section) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public void show(SimpleSection Section) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public SimpleTag findTag(String name) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SimpleTag getTag(String id) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<SimpleTag> getTags(SimpleSection section) throws MailException {
		return Collections.singleton(new SimpleTag("Inbox", "Inbox", "Inbox", true));
	}

	@Override
	public Set<SimpleTag> getTags(String pattern) throws MailException {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

	@Override
	public long getCountForTags(Set<SimpleTag> includes, Set<SimpleTag> excludes, String pattern) throws MailException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Set<SimpleTag> getHiddenTags() throws MailException {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

	@Override
	public SimpleTag getSpecialTag(SpecialTag specialTag) {
		String name = specialTag.toString();
		return new YahooTag(name, name, null, false);
	}

	@Override
	public SimpleTag addTag(String name) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(SimpleTag tag) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public SimpleTag rename(SimpleTag tag, String name) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void hide(SimpleTag tag) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public void show(SimpleTag tag) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public SimpleTag moveToSection(SimpleTag tag, SimpleSection section) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addOnUpdateTagOrSection(Runnable callback) {
		// TODO Auto-generated method stub

	}

}
