package net.anfoya.mail.yahoo;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.util.Callback;
import net.anfoya.mail.model.SimpleContact;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.yahoo.model.YahooSection;
import net.anfoya.mail.yahoo.model.YahooTag;
import net.anfoya.mail.yahoo.model.YahooThread;
import net.anfoya.mail.yahoo.service.AuthenticationService;
import net.anfoya.tag.model.SpecialSection;
import net.anfoya.tag.model.SpecialTag;

public class YahooMailService
implements MailService<YahooSection, YahooTag, YahooThread, SimpleMessage, SimpleContact> {
	private static final Logger LOGGER = LoggerFactory.getLogger(YahooMailService.class);
	private static final String USER = "me";

	private static final YahooTag[] SYSTEM_TAG_ORDER = {
			YahooTag.INBOX
			, YahooTag.UNREAD
			, YahooTag.STARRED
			, YahooTag.DRAFT
			, YahooTag.SENT
			, YahooTag.ALL
			, YahooTag.SPAM
			, YahooTag.TRASH
	};

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
	public void addOnNewMessage(Callback<Set<YahooThread>, Void> callback) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<YahooSection> getHiddenSections() throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public YahooThread getThread(String id) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<YahooThread> findThreads(Set<YahooTag> includes, Set<YahooTag> excludes, String pattern, int pageMax)
			throws MailException {

		final Set<YahooThread> threads = new HashSet<>();
		try {
			//			String tag = includes.iterator().next().getName();

			Folder folder = yahooMail.getDefaultFolder();
			if (folder.exists()) {
				if (!folder.isOpen()) {
					folder.open(Folder.HOLDS_MESSAGES);
				}
				for (Message message : folder.getMessages()) {
					threads.add(new YahooThread(message));
				}
			}
		} catch (Exception e) {
			throw new YahooException("", e);
		}
		return threads;
	}

	@Override
	public void addTagForThreads(YahooTag tag, Set<YahooThread> threads) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTagForThreads(YahooTag tag, Set<YahooThread> thread) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public void archive(Set<YahooThread> threads) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public void trash(Set<YahooThread> threads) throws MailException {
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
	public YahooSection getSpecialSection(SpecialSection section) {
		switch(section) {
		case SYSTEM: return YahooSection.SYSTEM;
		}
		return null;
	}

	@Override
	public Set<YahooSection> getSections() throws YahooException {
		try {
			final Set<YahooSection> sections = new LinkedHashSet<>();
			sections.add(YahooSection.SYSTEM);
			sections.add(YahooSection.FOLDERS);

			LOGGER.debug("get sections: {}", sections);
			return sections;
		} catch (final Exception e) {
			throw new YahooException("get sections", e);
		}
	}

	@Override
	public long getCountForSection(YahooSection section, Set<YahooTag> includes, Set<YahooTag> excludes,
			String itemPattern) throws YahooException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public YahooSection addSection(String name) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(YahooSection Section) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public YahooSection rename(YahooSection Section, String name) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void hide(YahooSection Section) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public void show(YahooSection Section) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public YahooTag findTag(String name) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public YahooTag getTag(String id) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<YahooTag> getTags(YahooSection section) throws YahooException {
		final Set<YahooTag> tags = new HashSet<>();
		if (YahooSection.SYSTEM.equals(section)) {
			tags.add(YahooTag.INBOX);
			tags.add(YahooTag.UNREAD);
			tags.add(YahooTag.STARRED);
			tags.add(YahooTag.DRAFT);
			tags.add(YahooTag.SENT);
			tags.add(YahooTag.ALL);
			tags.add(YahooTag.SPAM);
			tags.add(YahooTag.TRASH);
		} else {
			try {
				Folder[] perso;
				perso = yahooMail.getUserNamespaces(USER);
				for(final Folder f: perso) {
					tags.add(new YahooTag(f));
				}
			} catch (MessagingException e) {
				throw new YahooException("getting tags for {}", e);
			}
		}
		return sortSystemTags(tags);
	}

	private Set<YahooTag> sortSystemTags(final Set<YahooTag> tags) {
		final Set<YahooTag> alphaTags = new TreeSet<>(tags);
		final Set<YahooTag> sorted = new LinkedHashSet<>();

		for(final YahooTag t: SYSTEM_TAG_ORDER) {
			if (alphaTags.contains(t)) {
				sorted.add(t);
				alphaTags.remove(t);
			}
		}
		sorted.addAll(alphaTags);

		return sorted;
	}

	@Override
	public Set<YahooTag> getTags(String pattern) throws MailException {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

	@Override
	public long getCountForTags(Set<YahooTag> includes, Set<YahooTag> excludes, String pattern) throws MailException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Set<YahooTag> getHiddenTags() throws MailException {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

	@Override
	public YahooTag getSpecialTag(SpecialTag specialTag) {
		switch (specialTag) {
		case ALL:		return YahooTag.ALL;
		case FLAGGED:	return YahooTag.STARRED;
		case INBOX:		return YahooTag.INBOX;
		case SENT:		return YahooTag.SENT;
		case UNREAD:	return YahooTag.UNREAD;
		case SPAM:		return YahooTag.SPAM;
		case TRASH:		return YahooTag.TRASH;
		case DRAFT:		return YahooTag.DRAFT;
		case CHAT:
		case FORUMS:
		case PROMOTIONS:
		case SOCIAL:
		case UPDATES:
		}
		return null;
	}

	@Override
	public YahooTag addTag(String name) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(YahooTag tag) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public YahooTag rename(YahooTag tag, String name) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void hide(YahooTag tag) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public void show(YahooTag tag) throws MailException {
		// TODO Auto-generated method stub

	}

	@Override
	public YahooTag moveToSection(YahooTag tag, YahooSection section) throws MailException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addOnUpdateTagOrSection(Runnable callback) {
		// TODO Auto-generated method stub

	}

}
