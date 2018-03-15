package net.anfoya.java.util.concurrent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.anfoya.java.util.VoidCallback;

public final class ObservableExecutorService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObservableExecutorService.class);
	private static final long CLEANUP_PERIOD_MS = 250;

	private final ExecutorService service;

	private final Map<Future<?>, String> futureDescriptions;
	private final Set<VoidCallback<Map<Future<?>, String>>> changeCallbacks;

	public ObservableExecutorService(String name, ExecutorService service) {
		this.service = service;

		futureDescriptions = new ConcurrentHashMap<Future<?>, String>();
		changeCallbacks = new HashSet<VoidCallback<Map<Future<?>, String>>>();

		new Timer(String.format("tp-%s-cleanup", name), true).schedule(
			new TimerTask() { @Override public void run() { cleanupFutures(); } }
			, CLEANUP_PERIOD_MS, CLEANUP_PERIOD_MS);

//		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//			if (!service.isShutdown()) {
//				service.shutdown();
//				LOGGER.info("shutdown {}", service);
//			} else if (!service.isTerminated()) {
//				service.shutdownNow();
//				LOGGER.info("shutdown now {}", service);
//			}
//		}));

		LOGGER.info("created thread pool {}", name);
	}

	public void addOnChange(final VoidCallback<Map<Future<?>, String>> callback) {
		changeCallbacks.add(callback);
		callback.call(futureDescriptions);
	}

	public void submit(final Runnable runnable, final String description) {
		addFuture(service.submit(runnable), description);
	}

	public <T> Future<T> submit(final Callable<T> callable, final String description) {
		return addFuture(service.submit(callable), description);
	}

	private <T> Future<T> addFuture(final Future<T> future, final String description) {
		futureDescriptions.put(future, description);
		changeCallbacks.forEach(c -> c.call(futureDescriptions));

		return future;
	}

	private void cleanupFutures() {
		boolean cleaned = false;
		for(final Iterator<Future<?>> i = futureDescriptions.keySet().iterator(); i.hasNext();) {
			if (i.next().isDone()) {
				i.remove();
				cleaned = true;
			}
		}
		if (cleaned) {
			changeCallbacks.forEach(c -> c.call(futureDescriptions));
		}
	}

	public boolean isRunning() {
		return !futureDescriptions.isEmpty();
	}
}
