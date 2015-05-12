package net.anfoya.mail.gmail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import net.anfoya.mail.gmail.model.GmailThread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.gmail.Gmail;
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
			throw new ThreadException("loading thread for id " + id, e);
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

	private Set<Thread> get(final Set<String> ids, final DataType dataType) throws ThreadException {
		final long start = System.currentTimeMillis();
		try {
			final Set<Thread> threads = new LinkedHashSet<Thread>();
			final CountDownLatch latch = new CountDownLatch(ids.size());
			final BatchRequest batchRequest = gmail.batch();
			final JsonBatchCallback<Thread> callback = new JsonBatchCallback<Thread>() {
				@Override
				public void onSuccess(final Thread t, final HttpHeaders responseHeaders) throws IOException {
					threads.add(t);
					idThreads.put(t.getId(), t);
					latch.countDown();
				}
				@Override
				public void onFailure(final GoogleJsonError e, final HttpHeaders responseHeaders) throws IOException {
					LOGGER.error("get<{}> thread {} {}", dataType, e.getMessage());
					latch.countDown();
				}
			};
			for(final String id: ids) {
				gmail.users().threads().get(user, id).setFormat(dataType.toString()).queue(batchRequest, callback);
			}
			batchRequest.execute();
			latch.await();
			return threads;
		} catch (IOException | InterruptedException e) {
			throw new ThreadException("get<" + dataType.toString() + "> threads for ids " + ids, e);
		} finally {
			LOGGER.debug("get<{}> thread for ids {} ({}ms)", dataType, ids, System.currentTimeMillis()-start);
		}
	}

	public Set<Thread> find(final String query) throws ThreadException {
		if (query.isEmpty()) {
			throw new ThreadException("query must not be empty");
		}

		final long start = System.currentTimeMillis();
		final Set<String> ids = new LinkedHashSet<String>();
		final Set<String> toLoadIds = new LinkedHashSet<String>();
		try {
			ListThreadsResponse threadResponse = gmail.users().threads().list(user).setQ(query.toString()).execute();
			while (threadResponse.getThreads() != null) {
				for(final Thread t : threadResponse.getThreads()) {
					final String id = t.getId();
					final Thread cached = idThreads.get(id);
					if (cached == null || !cached.getHistoryId().equals(t.getHistoryId())) {
						toLoadIds.add(id);
					}
					ids.add(id);
				}
				if (threadResponse.getNextPageToken() != null) {
					final String pageToken = threadResponse.getNextPageToken();
					threadResponse = gmail.users().threads().list(user).setQ(query.toString()).setPageToken(pageToken).execute();
				} else {
					break;
				}
			}
			if (!toLoadIds.isEmpty()) {
				get(toLoadIds, DataType.metadata);
			}
			final Set<Thread> threads = new LinkedHashSet<Thread>();
			for(final String id: ids) {
				threads.add(idThreads.get(id));
			}
			return threads;
		} catch (final IOException e) {
			throw new ThreadException("loading threads for query " + query, e);
		} finally {
			LOGGER.debug("get threads for query {} ({}=ms)", query, System.currentTimeMillis()-start);
		}
	}

	public int count(final String query) throws ThreadException {
		if (query.isEmpty()) {
			throw new ThreadException("query must not be empty");
		}
		final long start = System.currentTimeMillis();
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
			throw new ThreadException("counting threads for query " + query, e);
		} finally {
			LOGGER.debug("count threads for query {} ({}=ms)", query, System.currentTimeMillis()-start);
		}
	}

	public void updateLabels(final Set<String> threadIds, final Set<String> labelIds, final boolean add) throws ThreadException {
		final long start = System.currentTimeMillis();
		try {
			final CountDownLatch latch = new CountDownLatch(threadIds.size());
			final BatchRequest batchRequest = gmail.batch();
			final JsonBatchCallback<Thread> callback = new JsonBatchCallback<Thread>() {
				@Override
				public void onSuccess(final Thread t, final HttpHeaders responseHeaders) throws IOException {
					latch.countDown();
				}
				@Override
				public void onFailure(final GoogleJsonError e, final HttpHeaders responseHeaders) throws IOException {
					LOGGER.error("update<{}> labels {} to threads {}\n{}", add? "add": "del", labelIds, threadIds, e.getMessage());
					latch.countDown();
				}
			};
			final ModifyThreadRequest request = new ModifyThreadRequest().setAddLabelIds(new ArrayList<String>(labelIds));
			for(final String id: threadIds) {
				gmail.users().threads().modify(user, id, request).queue(batchRequest, callback);
			}
			batchRequest.execute();
			latch.await();
		} catch (final IOException | InterruptedException e) {
			throw new ThreadException("update<{" + (add? "add": "del") + "}> labels " + labelIds + " to threads " + threadIds, e);
		} finally {
			LOGGER.debug("update<{}> labels {} to threads {} ({}ms)", add? "add": "del", labelIds, threadIds, System.currentTimeMillis()-start);
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
				throw new ThreadException("archiving thread id " + t.getId(), e);
			}
		}
	}

	public void trash(final Set<GmailThread> threads) throws ThreadException {
		for(final GmailThread t: threads) {
			try {
				gmail.users().threads().trash(user, t.getId()).execute();
			} catch (final IOException e) {
				throw new ThreadException("trashing thread id " + t.getId(), e);
			}
		}
	}
}
