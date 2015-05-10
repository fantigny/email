package net.anfoya.mail.gmail;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.anfoya.java.util.concurrent.ThreadPool;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListThreadsResponse;
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
			throw new ThreadException("loading thread " + id, e);
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
			throw new ThreadException("loading thread ids for: " + query, e);
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
			throw new ThreadException("loading threads for: " + query, e);
		}

		try {
			final Set<Thread> threads = new LinkedHashSet<Thread>();
			for(final Future<Thread> f: futures) {
				threads.add(f.get());
			}
			return threads;
		} catch(final CancellationException | InterruptedException | ExecutionException e) {
			throw new ThreadException("loading threads for: " + query, e);
		}
	}
}
