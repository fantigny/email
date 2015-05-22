package net.anfoya.mail.gmail.service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import net.anfoya.java.cache.FileSerieSerializedMap;
import net.anfoya.mail.gmail.cache.CacheData;
import net.anfoya.mail.gmail.cache.CacheException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyThreadRequest;
import com.google.api.services.gmail.model.Thread;

public class ThreadService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadService.class);
	private static final String FILE_PREFIX = System.getProperty("java.io.tmpdir") + "fsm-cache-id-threads-";
	private static final Long MAX_LIST_RESULTS = Long.valueOf(100);

	public static final String PAGE_TOKEN_ID = "no-id-page-token";

	private final Gmail gmail;
	private final String user;

	private final Map<String, CacheData<Thread>> idThreads;

	public ThreadService(final Gmail gmail, final String user) {
		this.gmail = gmail;
		this.user = user;

		idThreads = new FileSerieSerializedMap<String, CacheData<Thread>>(FILE_PREFIX + user, 200);
	}

	public Set<Thread> find(final String query, final int pageMax) throws ThreadException {
		if (query.isEmpty()) {
			throw new ThreadException("empty query not allowed");
		}

		final long start = System.currentTimeMillis();
		final Set<String> ids = new LinkedHashSet<String>();
		final Set<String> toLoadIds = new LinkedHashSet<String>();
		try {
			ListThreadsResponse threadResponse;
			threadResponse = gmail.users().threads().list(user).setQ(query.toString()).setMaxResults(MAX_LIST_RESULTS).execute();
			int page = 0;
			while (threadResponse.getThreads() != null) {
				for(final Thread t : threadResponse.getThreads()) {
					final String id = t.getId();
					BigInteger historyId;
					try {
						historyId = idThreads.get(id).getData().getHistoryId();
					} catch (final Exception e) {
						historyId = null;
					}
					if (!t.getHistoryId().equals(historyId)) {
						toLoadIds.add(id);
					}
					ids.add(id);
				}
				page++;
				if (threadResponse.getNextPageToken() != null && page < pageMax) {
					threadResponse = gmail.users().threads().list(user).setQ(query.toString()).setPageToken(threadResponse.getNextPageToken()).setMaxResults(MAX_LIST_RESULTS).execute();
				} else {
					break;
				}
			}
			load(toLoadIds);
			final Set<Thread> threads = new LinkedHashSet<Thread>();
			for(final String id: ids) {
				if (!idThreads.containsKey(id)) {
					LOGGER.error("missing from cache thread id: {}", id);
					continue;
				}
				threads.add(idThreads.get(id).getData());
			}
			if (threadResponse.getNextPageToken() != null) {
				// special thread for next page
				threads.add(nextPageThread(page));
			}
			return threads;
		} catch (final IOException | CacheException e) {
			throw new ThreadException("getting threads for query " + query, e);
		} finally {
			LOGGER.debug("get threads for query {} ({}=ms)", query, System.currentTimeMillis()-start);
		}
	}

	private static Thread nextPageThread(final int nextPage) {
		final Thread thread = new Thread();
		thread.setId(PAGE_TOKEN_ID);
		thread.setHistoryId(BigInteger.valueOf(nextPage));
		thread.setMessages(new ArrayList<Message>());

		return thread;
	}

	public int count(final String query) throws ThreadException {
		if (query.isEmpty()) {
			throw new ThreadException("empty query forbidden");
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

	public void update(final Set<String> threadIds, final Set<String> labelIds, final boolean add) throws ThreadException {
		final long start = System.currentTimeMillis();
		try {
			final CountDownLatch latch = new CountDownLatch(threadIds.size());
			final BatchRequest batch = gmail.batch();
			final JsonBatchCallback<Thread> callback = new JsonBatchCallback<Thread>() {
				@Override
				public void onSuccess(final Thread t, final HttpHeaders responseHeaders) throws IOException {
					idThreads.remove(t.getId());
					latch.countDown();
				}
				@Override
				public void onFailure(final GoogleJsonError e, final HttpHeaders responseHeaders) throws IOException {
					LOGGER.error("update<{}> labels {} for threads {}\n{}", add? "add": "del", labelIds, threadIds, e.getMessage());
					latch.countDown();
				}
			};
			final ModifyThreadRequest request;
			if (add) {
				request = new ModifyThreadRequest().setAddLabelIds(new ArrayList<String>(labelIds));
			} else {
				request = new ModifyThreadRequest().setRemoveLabelIds(new ArrayList<String>(labelIds));
			}
			for(final String id: threadIds) {
				gmail.users().threads().modify(user, id, request).queue(batch, callback);
			}
			batch.execute();
			latch.await();
		} catch (final IOException | InterruptedException e) {
			throw new ThreadException("update<{" + (add? "add": "del") + "}> labels " + labelIds + " for threads " + threadIds, e);
		} finally {
			LOGGER.debug("update<{}> labels {} for threads {} ({}ms)", add? "add": "del", labelIds, threadIds, System.currentTimeMillis()-start);
		}
	}

	public void trash(final Set<String> ids) throws ThreadException {
		try {
			final CountDownLatch latch = new CountDownLatch(ids.size());
			final BatchRequest batch = gmail.batch();
			final JsonBatchCallback<Thread> callback = new JsonBatchCallback<Thread>() {
				@Override
				public void onSuccess(final Thread t, final HttpHeaders responseHeaders) throws IOException {
					latch.countDown();
				}
				@Override
				public void onFailure(final GoogleJsonError e, final HttpHeaders responseHeaders) throws IOException {
					LOGGER.error("trashing thread", e.getMessage());
					latch.countDown();
				}
			};
			for(final String id: ids) {
				gmail.users().threads().trash(user, id).queue(batch, callback);
			}
			batch.execute();
			latch.await();
		} catch (IOException | InterruptedException e) {
			throw new ThreadException("trashing for ids " + ids, e);
		}
	}

	private void load(final Set<String> ids) throws ThreadException {
		if (ids.isEmpty()) {
			return;
		}
		final long start = System.currentTimeMillis();
		try{
			final Set<Thread> threads = new LinkedHashSet<Thread>();
			final CountDownLatch latch = new CountDownLatch(ids.size());
			final BatchRequest batch = gmail.batch();
			final JsonBatchCallback<Thread> callback = new JsonBatchCallback<Thread>() {
				@Override
				public void onSuccess(final Thread t, final HttpHeaders responseHeaders) throws IOException {
					threads.add(t);
					idThreads.put(t.getId(), new CacheData<Thread>(t));
					latch.countDown();
				}
				@Override
				public void onFailure(final GoogleJsonError e, final HttpHeaders responseHeaders) throws IOException {
					LOGGER.error("loading thread {}", e.getMessage());
					latch.countDown();
				}
			};
			for(final String id: ids) {
				gmail.users().threads().get(user, id).setFormat("metadata").queue(batch, callback);
			}
			batch.execute();
			latch.await();
		} catch (IOException | InterruptedException e) {
			throw new ThreadException("loading for ids " + ids, e);
		} finally {
			LOGGER.debug("load for ids {} ({}ms)", ids, System.currentTimeMillis()-start);
		}
	}
}
