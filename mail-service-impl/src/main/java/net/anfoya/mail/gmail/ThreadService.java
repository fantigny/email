package net.anfoya.mail.gmail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.mail.gmail.model.GmailThread;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.ModifyThreadRequest;
import com.google.api.services.gmail.model.Thread;

public class ThreadService {
	private final Map<String, Thread> idThreads = new ConcurrentHashMap<String, Thread>();
	private final Gmail gmail;
	private final String user;
	
	public ThreadService(final Gmail gmail, final String user) {
		this.gmail = gmail;
		this.user = user;
	}
	
	public Thread get(final String id) throws ThreadException {
		try {
			final Thread thread = idThreads.get(id);
			if (thread == null || !thread.getId().equals(getMinimal(id).getHistoryId())) {
				idThreads.put(id, gmail.users().threads().get(user, id).setFormat("metadata").execute());
			}
			return idThreads.get(id);
		} catch (final IOException e) {
			throw new ThreadException("loading thread for id: " + id, e);
		}
	}

	private Thread getMinimal(final String id) throws IOException {
		return gmail.users().threads().get(user, id).setFormat("minimal").execute();
	}

	public Set<Thread> find(final String query) throws ThreadException {
		final Set<String> threadIds = new LinkedHashSet<String>();
		try {
			ListThreadsResponse threadResponse = gmail.users().threads().list(user).setQ(query.toString()).execute();
			while (threadResponse.getThreads() != null) {
				for(final Thread t : threadResponse.getThreads()) {
					threadIds.add(t.getId());
				}
				if (threadResponse.getNextPageToken() != null) {
					final String pageToken = threadResponse.getNextPageToken();
					threadResponse = gmail.users().threads().list(user).setQ(query.toString()).setPageToken(pageToken).execute();
				} else {
					break;
				}
			}
		} catch (final IOException e) {
			throw new ThreadException("loading threads for query: " + query, e);
		}
		final Set<Future<Thread>> futures = new LinkedHashSet<Future<Thread>>();
		try {
			ListThreadsResponse threadResponse = gmail.users().threads().list(user).setQ(query.toString()).execute();
			while (threadResponse.getThreads() != null) {
				for(final Thread t : threadResponse.getThreads()) {
					futures.add(ThreadPool.getInstance().submit(() -> {
						return get(t.getId());
					}));
				}
				if (threadResponse.getNextPageToken() != null) {
					final String pageToken = threadResponse.getNextPageToken();
					threadResponse = gmail.users().threads().list(user).setQ(query.toString()).setPageToken(pageToken).execute();
				} else {
					break;
				}
			}
		} catch (final IOException e) {
			throw new ThreadException("loading threads for query: " + query, e);
		}

		try {
			final Map<String, Thread> idThreads = new HashMap<String, Thread>();
			for(final Future<Thread> f: futures) {
				final Thread thread = f.get();
				idThreads.put(thread.getId(), thread);
			}
			final Set<Thread> threads = new LinkedHashSet<Thread>();
			for(final String id: threadIds) {
				threads.add(idThreads.get(id));
			}
			return threads;
		} catch(final CancellationException | InterruptedException | ExecutionException e) {
			throw new ThreadException("loading threads for query: " + query, e);
		}
	}

	public int count(final String query) throws ThreadException {
		try {
			int count = 0;
			ListThreadsResponse response = gmail.users().threads().list(user).setQ(query.toString()).execute();
			while (response.getThreads() != null) {
				count += response.getThreads().size();
				if (response.getNextPageToken() != null) {
					final String pageToken = response.getNextPageToken();
					response = gmail.users().threads().list(user).setQ(query.toString()).setPageToken(pageToken).execute();
				} else {
					break;
				}
			}
			return count;
		} catch (final IOException e) {
			throw new ThreadException("counting threads for query: " + query, e);
		}
	}

	public void addLabel(final String threadId, final String labelId) throws ThreadException {
		try {
			@SuppressWarnings("serial")
			final ModifyThreadRequest request = new ModifyThreadRequest().setAddLabelIds(new ArrayList<String>() {{ add(labelId); }});
			gmail.users().threads().modify(user, threadId, request).execute();
		} catch (final IOException e) {
			throw new ThreadException("adding label id: " + labelId + " to thread id: " + threadId, e);
		}
	}

	public void remLabel(final String threadId, final String labelId) throws ThreadException {
		try {
			@SuppressWarnings("serial")
			final ModifyThreadRequest request = new ModifyThreadRequest().setRemoveLabelIds(new ArrayList<String>() {{ add(labelId); }});
			gmail.users().threads().modify(user, threadId, request).execute();
		} catch (final IOException e) {
			throw new ThreadException("removing label id: " + labelId + " to thread id: " + threadId, e);
		}
	}

	public void archive(final Set<GmailThread> threads) throws ThreadException {
		@SuppressWarnings("serial")
		final List<String> inboxId = new ArrayList<String>() {{ add(find("INBOX").iterator().next().getId());}};
		final ModifyThreadRequest request = new ModifyThreadRequest().setRemoveLabelIds(inboxId);
		for(final GmailThread t: threads) {
			try {
				gmail.users().threads().modify(user, t.getId(), request).execute();
			} catch (final IOException e) {
				throw new ThreadException("archiving thread id: " + t.getId(), e);
			}
		}
	}

	public void trash(final Set<GmailThread> threads) throws ThreadException {
		for(final GmailThread t: threads) {
			try {
				gmail.users().threads().trash(user, t.getId()).execute();
			} catch (final IOException e) {
				throw new ThreadException("trashing thread id: " + t.getId(), e);
			}
		}
	}
}
