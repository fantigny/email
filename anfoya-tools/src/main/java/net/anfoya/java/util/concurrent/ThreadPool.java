package net.anfoya.java.util.concurrent;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.anfoya.java.util.VoidCallback;

public final class ThreadPool {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPool.class);

	// singleton
	private final static ThreadPool THREAD_POOL = new ThreadPool();
	public static ThreadPool getThreadPool() {
		return THREAD_POOL;
	}

	private final ObservableThreadPool delegateMax;
	private final ObservableThreadPool delegateMin;

	private ThreadPool() {
		delegateMax = new ObservableThreadPool("high", Thread.MAX_PRIORITY);
		delegateMin = new ObservableThreadPool("norm", Thread.MIN_PRIORITY);
	}

	public void submitHigh(final Runnable runnable, final String description) {
		delegateMax.submit(runnable, description);
	}

	public void submitLow(final Runnable runnable, final String description) {
		delegateMin.submit(runnable, description);
	}

	public <T> Future<T> submitHigh(final Callable<T> callable, final String description) {
		return delegateMax.submit(callable, description);
	}

	public <T> Future<T> submitLow(final Callable<T> callable, final String description) {
		return delegateMin.submit(callable, description);
	}

	public void setOnChange(final VoidCallback<Map<Future<?>, String>> callback) {
		delegateMin.setOnChange(callback);
	}

	public void setOnHighRunning(final VoidCallback<Boolean> callback) {
		delegateMax.setOnChange(s -> callback.call(!s.isEmpty()));
	}

	public void wait(final int ms, final String description) {
		final String desc = "waiting for " + description;
		submitLow(() -> {
			try {
				Thread.sleep(ms);
			} catch (final Exception e) {
				LOGGER.error(desc, e);
			}
		}, desc);
	}
}
