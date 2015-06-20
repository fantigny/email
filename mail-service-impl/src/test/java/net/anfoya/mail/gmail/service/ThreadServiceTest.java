package net.anfoya.mail.gmail.service;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import net.anfoya.mail.gmail.GmailService;
import net.anfoya.mail.gmail.GmailTestService;
import net.anfoya.mail.service.MailException;

import org.junit.Before;
import org.junit.Test;

import com.google.api.services.gmail.model.Thread;

public class ThreadServiceTest {
	private static final String TEST_SUBJECT = "thread_service_test";

	private ThreadService service;

	@Before public void init() throws MailException {
		final GmailService gmail = new GmailTestService();
		gmail.connect();
		service = gmail.getThreadService();
	}

	@Test public void find() throws ThreadException {
		final Set<Thread> threads = service.find("subject:" + TEST_SUBJECT, 1);
		Assert.assertFalse(threads.isEmpty());
	}

	@Test public void findThread() throws ThreadException {
		final Thread thread = service.find("subject:" + TEST_SUBJECT, 1).iterator().next();
		final Set<String> threadIds = new HashSet<String>();
		threadIds.add(thread.getId());
	}

	@Test public void addRemoveLabel() throws ThreadException {
		final String INBOX = "INBOX";
		final String threadId = service.find("subject:" + TEST_SUBJECT, 1).iterator().next().getId();
		final Set<String> threadIds = new HashSet<String>();
		threadIds.add(threadId);
		final Set<String> labelIds = new HashSet<String>();
		labelIds.add(INBOX);

		// add to INBOX
		service.update(threadIds, labelIds, true);
		Set<Thread> threads = service.find("label:" + INBOX + " AND subject:" + TEST_SUBJECT, 1);
		Assert.assertEquals(1, threads.size());

		// remove from INBOX
		service.update(threadIds, labelIds, false);
		threads = service.find("label:" + INBOX + " AND subject:" + TEST_SUBJECT, 1);
		Assert.assertTrue(threads.isEmpty());
	}

	@Test public void cache() throws ThreadException {
		final String INBOX = "INBOX";
		final String threadId = service.find("subject:" + TEST_SUBJECT, 1).iterator().next().getId();
		final Set<String> threadIds = new HashSet<String>();
		threadIds.add(threadId);
		final Set<String> labelIds = new HashSet<String>();
		labelIds.add(INBOX);

		// get thread
		Thread thread = service.find("subject:" + TEST_SUBJECT, 1).iterator().next();
		// add to INBOX
		BigInteger historyId = thread.getHistoryId();
		service.update(threadIds, labelIds, true);
		thread = service.find("subject:" + TEST_SUBJECT, 1).iterator().next();
		Assert.assertFalse(thread.getHistoryId().equals(historyId));
		// remove from INBOX
		historyId = thread.getHistoryId();
		service.update(threadIds, labelIds, false);
		thread = service.find("subject:" + TEST_SUBJECT, 1).iterator().next();
		Assert.assertFalse(thread.getHistoryId().equals(historyId));
	}
}
