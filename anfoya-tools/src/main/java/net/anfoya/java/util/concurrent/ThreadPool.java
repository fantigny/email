package net.anfoya.java.util.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import net.anfoya.java.util.VoidCallback;

public class ThreadPool {
	public enum PoolPriority { MIN, REG, MAX };

	private static final Map<PoolPriority, ObservableExecutorService> priorityPools = new HashMap<PoolPriority, ObservableExecutorService>();

	private static ThreadPool THREAD_POOL = null;

	// initialize
	public static void setDefault(ObservableExecutorService min, ObservableExecutorService reg, ObservableExecutorService max) {
		if (THREAD_POOL != null) {
			throw new IllegalStateException("already initialized");
		}

		THREAD_POOL = new ThreadPool(min, reg, max);
	}

	// singleton
	public static ThreadPool getDefault() {
		if (THREAD_POOL == null) {
			THREAD_POOL = new ThreadPool(ObservableExecutors.newCachedThreadPool("min", Thread.MIN_PRIORITY)
					, ObservableExecutors.newCachedThreadPool("reg", Thread.NORM_PRIORITY)
					, ObservableExecutors.newCachedThreadPool("max", Thread.MAX_PRIORITY));
		}

		return THREAD_POOL;
	}

	private ThreadPool(ObservableExecutorService min, ObservableExecutorService reg, ObservableExecutorService max) {
		priorityPools.put(PoolPriority.MIN, min);
		priorityPools.put(PoolPriority.REG, reg);
		priorityPools.put(PoolPriority.MAX, max);
	}

	public void submit(final PoolPriority priority, final String description, final Runnable runnable) {
		priorityPools.get(priority).submit(runnable, description);
	}

	public <T> Future<T> submit(final PoolPriority priority, final String description, final Callable<T> callable) {
		return priorityPools.get(priority).submit(callable, description);
	}

	public void setOnChange(final PoolPriority priority, final VoidCallback<Map<Future<?>, String>> callback) {
		priorityPools.get(priority).setOnChange(callback);
	}

	public void mustRun(final String description, final Runnable runnable) {
		final Thread thread = new Thread(runnable, description);
		thread.setDaemon(false);
		thread.start();
	}
}
