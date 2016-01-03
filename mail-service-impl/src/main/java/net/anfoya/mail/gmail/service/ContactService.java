package net.anfoya.mail.gmail.service;

import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gdata.client.Query;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;


//TODO: refresh contacts
public class ContactService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ContactService.class);

	private final ContactsService gcontact;
	private final String user;
	private final Set<ContactEntry> contacts;

	private Future<List<ContactEntry>> future;

	public ContactService(final ContactsService gcontact, final String user) {
		this.gcontact = gcontact;
		this.user = user;

		contacts = new LinkedHashSet<ContactEntry>();
	}

	public ContactService init() {
		future = ThreadPool.getThreadPool().submitLow(() -> {
			final Query query = new Query(new URL("https://www.google.com/m8/feeds/contacts/" + user + "/full"));
			query.setMaxResults(10000);
			return gcontact.query(query, ContactFeed.class).getEntries();
		}, "getting contacts");
		return this;
	}

	public synchronized Set<ContactEntry> getAll() throws ContactException {
		if (contacts.isEmpty()) {
			try {
				contacts.addAll(future.get());
				LOGGER.info("loaded {} contacts", contacts.size());
			} catch (InterruptedException | ExecutionException e) {
				throw new ContactException("getting contacts", e);
			}
		}
		return contacts;
	}

	public void clearCache() {
		contacts.clear();
	}
}
