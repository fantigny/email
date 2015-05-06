package net.anfoya.java.util.concurrent;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ThreadPool {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPool.class);

	// singleton
	private final static ThreadPool THREAD_POOL = new ThreadPool();
	public static ThreadPool getInstance() {
		return THREAD_POOL;
	}

	private final ExecutorService delegateHigh;
	private final ExecutorService delegateLow;

	private ThreadPool() {
		delegateHigh = Executors.newCachedThreadPool();
		delegateLow = Executors.newFixedThreadPool(5);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				shutdown();
			}
		}));
		LOGGER.info("started!");
	}

	public void shutdown() {
		shutdown(delegateHigh);
		shutdown(delegateLow);
	}

	private void shutdown(final ExecutorService service) {
		if (!service.isShutdown()) {
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					if (!service.isTerminated()) {
						service.shutdownNow();
						LOGGER.info("stop forced.");
					}
				}
			}, 10000);
			service.shutdown();
		}
		LOGGER.info("stopped.");
	}

	public Future<?> submit(final Runnable runnable) {
		return delegateHigh.submit(runnable);
	}

	public <T> Future<T> submit(final Callable<T> callable) {
		return delegateHigh.submit(callable);
	}

	public Future<?> submitLow(final Runnable runnable) {
		return delegateLow.submit(runnable);
	}

	public <T> Future<T> submitLow(final Callable<T> callable) {
		return delegateLow.submit(callable);
	}
}
