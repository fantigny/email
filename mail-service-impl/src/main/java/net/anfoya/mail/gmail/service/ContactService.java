package net.anfoya.mail.gmail.service;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import net.anfoya.java.util.concurrent.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gdata.client.Query;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.data.extensions.Email;


public class ContactService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ContactService.class);

	private final ContactsService gcontact;
	private final String user;

	private final Map<String, ContactEntry> nameContacts;

	private Future<Map<String, ContactEntry>> future;

	public ContactService(final ContactsService gcontact, final String user) {
		this.gcontact = gcontact;
		this.user = user;
		nameContacts = new TreeMap<String, ContactEntry>();
	}

	public ContactService init() {
		future = ThreadPool.getInstance().submitLow(() -> {
			final Map<String, ContactEntry> nameContacts = new LinkedHashMap<String, ContactEntry>();
			final Query query = new Query(new URL("https://www.google.com/m8/feeds/contacts/" + user + "/full"));
			query.setMaxResults(10000);
			final List<ContactEntry> entries = gcontact.query(query, ContactFeed.class).getEntries();
			for(final ContactEntry c: entries) {
				for(final Email e: c.getEmailAddresses()) {
					nameContacts.put(e.getAddress(), c);
				}
			}

			LOGGER.info("loaded {} contacts", nameContacts.size());
			return nameContacts;
		});
		return this;
	}

	public synchronized Map<String, ContactEntry> getAll() throws ContactException {
		if (nameContacts.isEmpty()) {
			try {
				nameContacts.putAll(future.get());
			} catch (InterruptedException | ExecutionException e) {
				throw new ContactException("getting contacts", e);
			}
		}
		return nameContacts;
	}

	public Set<String> getAllAddresses() throws ContactException {
		return getAll().keySet();
	}

	public Set<ContactEntry> get(final String pattern) throws ContactException {
		return new LinkedHashSet<ContactEntry>(getAll().entrySet().stream()
				.filter(p -> p.getKey().toLowerCase().contains(pattern))
				.collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue())).values());
	}
}
