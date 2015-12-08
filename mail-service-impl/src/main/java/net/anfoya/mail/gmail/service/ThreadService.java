package net.anfoya.mail.gmail.service;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

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

import net.anfoya.java.cache.FileSerieSerializedMap;
import net.anfoya.mail.gmail.cache.CacheData;
import net.anfoya.mail.gmail.cache.CacheException;
import net.anfoya.mail.gmail.model.GmailThread;

public class ThreadService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadService.class);
	private static final String FILE_PREFIX = System.getProperty("java.io.tmpdir") + File.separatorChar + "fsm-cache-id-threads-";
	private static final Long MAX_LIST_RESULTS = Long.valueOf(100);

	private final Gmail gmail;
	private final String user;

	private final Map<String, CacheData<Thread>> idThreads;

	public ThreadService(final Gmail gmail, final String user) {
		this.gmail = gmail;
		this.user = user;

		idThreads = new FileSerieSerializedMap<String, CacheData<Thread>>(FILE_PREFIX + user, 50);
	}

	public Set<Thread> get(final Set<String> ids, boolean cached, Integer nextPage) throws ThreadException {
		final long start = System.currentTimeMillis();
		final Set<Thread> threads = new HashSet<Thread>();

		if (ids.isEmpty()) {
			return threads;
		}

		final Set<String> notCachedIds;
		if (!cached) {
			notCachedIds = ids;
		} else {
			notCachedIds = new HashSet<String>();
			for(final String id: ids) {
				Thread thread = null;
				if (idThreads.containsKey(id)) {
					try {
						thread = idThreads.get(id).getData();
					} catch (final Exception e) {
						LOGGER.error("get from cache {}", id, e);
						idThreads.remove(id);
						thread = null;
					}
				}
				if (thread == null) {
					notCachedIds.add(id);
				} else {
					threads.add(thread);
				}
			}
		}

		threads.addAll(load(notCachedIds));

		if (nextPage != null) {
			threads.add(nextPageThread(nextPage));
		}

		LOGGER.debug("load threads in {}ms ({})", System.currentTimeMillis()-start, ids);
		return threads;
	}

	public Set<Thread> find(final String query, final int pageMax) throws ThreadException {
		if (query.isEmpty()) {
			throw new ThreadException("empty query not allowed");
		}

		final long start = System.currentTimeMillis();
		final Set<String> ids = new LinkedHashSet<String>();
		try {
			ListThreadsResponse threadResponse;
			threadResponse = gmail.users().threads().list(user)
					.setFields("nextPageToken,threads(historyId,id)")
					.setQ(query.toString())
					.setMaxResults(MAX_LIST_RESULTS)
					.execute();
			int page = 0;
			while (threadResponse.getThreads() != null) {
				for(final Thread t : threadResponse.getThreads()) {
					final String id = t.getId();
					ids.add(id);

					boolean removeFromCache = true;
					try {
						removeFromCache = idThreads.containsKey(id) && !idThreads.get(id).getData().getHistoryId().equals(t.getHistoryId());
					} catch (final CacheException e) {
						removeFromCache = true;
					}
					if (removeFromCache) {
						idThreads.remove(id);
					}
				}
				page++;
				if (threadResponse.getNextPageToken() != null && page < pageMax) {
					threadResponse = gmail.users().threads().list(user)
							.setFields("nextPageToken,threads(historyId,id)")
							.setQ(query.toString())
							.setPageToken(threadResponse.getNextPageToken())
							.setMaxResults(MAX_LIST_RESULTS)
							.execute();
				} else {
					break;
				}
			}
			return get(ids, true, threadResponse.getNextPageToken() == null? null: page);
		} catch (final IOException e) {
			throw new ThreadException("getting threads for query " + query, e);
		} finally {
			LOGGER.debug("get threads in {}ms for query {}", System.currentTimeMillis()-start, query);
		}
	}

	private static Thread nextPageThread(final Integer nextPage) {
		final Thread thread = new Thread();
		thread.setId(GmailThread.PAGE_TOKEN_ID);
		thread.setHistoryId(BigInteger.valueOf(Long.valueOf(nextPage)));
		thread.setMessages(new ArrayList<Message>());

		return thread;
	}

	public int count(final String query) throws ThreadException {
		final Long COUNT_MAX = Long.valueOf(1000);
		if (query.isEmpty()) {
			throw new ThreadException("empty query forbidden");
		}
		final long start = System.currentTimeMillis();
		try {
			int count = 0;
			ListThreadsResponse response = gmail.users().threads().list(user)
					.setFields("nextPageToken,threads(id)")
					.setMaxResults(COUNT_MAX)
					.setQ(query.toString())
					.execute();
			while(response.getThreads() != null && count < COUNT_MAX) {
				count += response.getThreads().size();
				if (response.getNextPageToken() != null) {
					final String pageToken = response.getNextPageToken();
					response = gmail.users().threads().list(user)
							.setFields("nextPageToken,threads(id)")
							.setQ(query.toString())
							.setPageToken(pageToken)
							.execute();
				} else {
					break;
				}
			}
			return count;
		} catch (final IOException e) {
			throw new ThreadException("count threads for query " + query, e);
		} finally {
			LOGGER.debug("count threads in {}ms for query {}", System.currentTimeMillis()-start, query);
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
					LOGGER.error("{} labels {} for threads {}", add? "add": "del", labelIds, threadIds, e.getMessage());
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
			LOGGER.debug("update<{}> labels for threads in {}ms, label ids {}, thread ids {}", add? "add": "del", System.currentTimeMillis()-start, labelIds, threadIds);
		}
	}

	public void trash(final Set<String> ids) throws ThreadException {
		final long start = System.currentTimeMillis();
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
					LOGGER.error("trash thread", e.getMessage());
					latch.countDown();
				}
			};
			for(final String id: ids) {
				gmail.users().threads().trash(user, id).queue(batch, callback);
			}
			batch.execute();
			latch.await();
		} catch (IOException | InterruptedException e) {
			throw new ThreadException("trash threads " + ids, e);
		} finally {
			LOGGER.debug("trash threads in {}ms, thread ids {}", System.currentTimeMillis()-start, ids);
		}
	}

	public void clearCache() {
		idThreads.clear();
	}

	private Set<Thread> load(Set<String> ids) throws ThreadException {
		final Set<Thread> threads = new HashSet<Thread>();
		if (ids.isEmpty()) {
			return threads;
		}

		try {
			final CountDownLatch latch = new CountDownLatch(ids.size());
			final BatchRequest batch = gmail.batch();
			final JsonBatchCallback<Thread> callback = new JsonBatchCallback<Thread>() {
				@Override
				public void onSuccess(final Thread t, final HttpHeaders responseHeaders) {
					threads.add(t);
					idThreads.put(t.getId(), new CacheData<Thread>(t));
					latch.countDown();
				}
				@Override
				public void onFailure(final GoogleJsonError e, final HttpHeaders responseHeaders) {
					LOGGER.error("load thread", e.getMessage());
					latch.countDown();
				}
			};
			for(final String id: ids) {
				gmail.users().threads().get(user, id)
					.setFields("historyId,id,messages(id,internalDate,labelIds,payload)")
					.queue(batch, callback);
			}
			batch.execute();
			latch.await();
		} catch (final Exception e) {
			throw new ThreadException("load threads " + ids, e);
		}

		return threads;
	}
}
