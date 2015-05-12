package net.anfoya.mail.gmail;

import java.io.IOException;
import java.util.ArrayList;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.ModifyThreadRequest;
import com.google.api.services.gmail.model.Thread;

public class ThreadService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadService.class);

	private enum DataType { minimal, metadata };

	private final Gmail gmail;
	private final String user;

	private final Map<String, Thread> idThreads;

	public ThreadService(final Gmail gmail, final String user) {
		this.gmail = gmail;
		this.user = user;

		idThreads = new ConcurrentHashMap<String, Thread>();
	}

	public Thread get(final String id) throws ThreadException {
		final long start = System.currentTimeMillis();
		try {
			final Thread cached = idThreads.get(id);
			final Thread thread = get(id, DataType.minimal);
			if (cached == null || cached.getHistoryId().equals(thread.getHistoryId())) {
				idThreads.put(id, get(id, DataType.metadata));
			}
			return idThreads.get(id);
		} catch (final IOException e) {
			throw new ThreadException("loading thread for id: " + id, e);
		} finally {
			LOGGER.debug("get thread for id {} ({}ms)", id, System.currentTimeMillis()-start);
		}
	}

	private Thread get(final String id, final DataType dataType) throws IOException {
		final long start = System.currentTimeMillis();
		try {
			return gmail.users().threads().get(user, id).setFormat(dataType.toString()).execute();
		} finally {
			LOGGER.debug("get<{}> thread for id {} ({}ms)", dataType, id, System.currentTimeMillis()-start);
		}
	}

	public Set<Thread> find(final String query) throws ThreadException {
		final long start = System.currentTimeMillis();
		final Set<String> ids = new LinkedHashSet<String>();
		final Set<Future<Thread>> futures = new LinkedHashSet<Future<Thread>>();
		try {
			ListThreadsResponse threadResponse = gmail.users().threads().list(user).setQ(query.toString()).execute();
			while (threadResponse.getThreads() != null) {
				for(final Thread t : threadResponse.getThreads()) {
					ids.add(t.getId());
					final Thread cached = idThreads.get(t.getId());
					if (cached == null || !cached.getHistoryId().equals(t.getHistoryId())) {
						futures.add(ThreadPool.getInstance().submit(() -> {
							return get(t.getId());
						}));
					}
				}
				if (threadResponse.getNextPageToken() != null) {
					final String pageToken = threadResponse.getNextPageToken();
					threadResponse = gmail.users().threads().list(user).setQ(query.toString()).setPageToken(pageToken).execute();
				} else {
					break;
				}
			}
		} catch (final IOException e) {
			throw new ThreadException("loading thread ids for query: " + query, e);
		} finally {
			LOGGER.debug("get thread ids for query [{}] ({}ms)", query, System.currentTimeMillis()-start);
		}
		try {
			for(final Future<Thread> f: futures) {
				final Thread thread = f.get();
				idThreads.put(thread.getId(), thread);
			}
			final Set<Thread> threads = new LinkedHashSet<Thread>();
			for(final String id: ids) {
				threads.add(idThreads.get(id));
			}
			return threads;
		} catch(final CancellationException | InterruptedException | ExecutionException e) {
			throw new ThreadException("loading threads for query: " + query, e);
		} finally {
			LOGGER.debug("get threads for query [{}] ({}ms)", query, System.currentTimeMillis()-start);
		}
	}

	public int count(final String query) throws ThreadException {
		try {
			int count = 0;
			ListThreadsResponse response = gmail.users().threads().list(user).setQ(query.toString()).execute();
			while(response.getThreads() != null) {
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

	public void add(final Thread thread, final Label label) throws ThreadException {
		try {
			@SuppressWarnings("serial")
			final ModifyThreadRequest request = new ModifyThreadRequest().setAddLabelIds(new ArrayList<String>() {{
				add(label.getId());
			}});
			gmail.users().threads().modify(user, thread.getId(), request).execute();
		} catch (final IOException e) {
			throw new ThreadException("adding label \"" + label.getName() + "\" to thread id: " + thread, e);
		}
	}

	public void remove(final Thread thread, final Label label) throws ThreadException {
		try {
			@SuppressWarnings("serial")
			final ModifyThreadRequest request = new ModifyThreadRequest().setRemoveLabelIds(new ArrayList<String>() {{
				add(label.getId());
			}});
			gmail.users().threads().modify(user, thread.getId(), request).execute();
		} catch (final IOException e) {
			throw new ThreadException("removing label \"" + label.getName() + "\" to thread id: " + thread, e);
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
