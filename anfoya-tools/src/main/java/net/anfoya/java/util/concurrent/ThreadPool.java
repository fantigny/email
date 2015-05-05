package net.anfoya.java.util.concurrent;

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

	private final ExecutorService delegate;

	private ThreadPool() {
		delegate = Executors.newFixedThreadPool(20);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				shutdown();
			}
		}));
		LOGGER.info("started!");
	}

	public void shutdown() {
		if (!delegate.isShutdown()) {
			delegate.shutdownNow();
		}
		LOGGER.info("stopped.");
	}

	public Future<?> submit(final Runnable runnable) {
		return delegate.submit(runnable);
	}

	public <T> Future<T> submit(final Callable<T> callable) {
		return delegate.submit(callable);
	}
}
