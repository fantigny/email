package net.anfoya.tools;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ThreadPool {
	// singleton
	private final static ThreadPool THREAD_POOL = new ThreadPool();
	public static ThreadPool getInstance() {
		return THREAD_POOL;
	}

	private final ExecutorService delegate;

	private ThreadPool() {
		delegate = Executors.newCachedThreadPool();
	}

	public void shutdown() {
		delegate.shutdown();
    }

	public Future<?> submit(final Runnable runnable) {
		return delegate.submit(runnable);
	}

	public <T> Future<T> submit(final Callable<T> callable) {
		return delegate.submit(callable);
	}
}
