package net.anfoya.java.util.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.anfoya.java.util.VoidCallback;

public final class ThreadPool {
	public enum ThreadPriority { MIN, REG, MAX };

	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPool.class);
	private static final Map<ThreadPriority, ObservableExecutorService> priorityPools = new HashMap<ThreadPriority, ObservableExecutorService>();

	private static ThreadPool THREAD_POOL = null;
	// initialize
	public static void setDefault(ThreadPool threadPool) {
		if (THREAD_POOL != null) {
			throw new IllegalStateException("default thread pool already defined");
		}
	}

	// singleton
	public static ThreadPool getDefault() {
		if (THREAD_POOL == null) {
			THREAD_POOL = new ThreadPool(new ObservableExecutorService(new NamedExecutorService("min", Executors.newCachedThreadPool(new NamedThreadFactory("min", Thread.MIN_PRIORITY))))
					, new ObservableExecutorService(new NamedExecutorService("reg", Executors.newCachedThreadPool(new NamedThreadFactory("reg", Thread.NORM_PRIORITY))))
					, new ObservableExecutorService(new NamedExecutorService("max", Executors.newCachedThreadPool(new NamedThreadFactory("max", Thread.MAX_PRIORITY)))));
		}
		return THREAD_POOL;
	}

	public ThreadPool(ObservableExecutorService min, ObservableExecutorService reg, ObservableExecutorService max) {
		priorityPools.put(ThreadPriority.MIN, min);
		priorityPools.put(ThreadPriority.REG, reg);
		priorityPools.put(ThreadPriority.MAX, max);
	}

	public void submit(final ThreadPriority priority, final String description, final Runnable runnable) {
		priorityPools.get(priority).submit(runnable, description);
	}

	public <T> Future<T> submit(final ThreadPriority priority, final String description, final Callable<T> callable) {
		return priorityPools.get(priority).submit(callable, description);
	}

	public void setOnChange(final ThreadPriority priority, final VoidCallback<Map<Future<?>, String>> callback) {
		priorityPools.get(priority).setOnChange(callback);
	}

	public void wait(final int ms, final String description) {
		final String desc = "waiting for " + description;
		submit(ThreadPriority.MIN, desc, () -> {
			try {
				Thread.sleep(ms);
			} catch (final Exception e) {
				LOGGER.error(desc, e);
			}
		});
	}
}
