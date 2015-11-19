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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.util.Callback;

public final class ThreadPool {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPool.class);

	private static class NamedThreadFactory implements ThreadFactory {
	    private final ThreadGroup group;
	    private final AtomicInteger threadNumber = new AtomicInteger(1);
	    private final String namePrefix;
	    private final int priority;

	    protected NamedThreadFactory(final String name, final int priority) {
	        final SecurityManager s = System.getSecurityManager();
	        group = s != null? s.getThreadGroup(): Thread.currentThread().getThreadGroup();
	        namePrefix = name + "-";
	        this.priority = priority;
	    }

	    @Override
		public Thread newThread(final Runnable r) {
	        final Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(true);
            t.setPriority(priority);
	        return t;
	    }
	}
	// singleton
	private final static ThreadPool THREAD_POOL = new ThreadPool();
	public static ThreadPool getInstance() {
		return THREAD_POOL;
	}

	private final ExecutorService delegateHigh;
	private final ExecutorService delegateLow;

	private final Map<Future<?>, String> futureDesc;
	private final Set<Callback<Map<Future<?>, String>, Void>> changeCallbacks;

	private ThreadPool() {
		futureDesc = new ConcurrentHashMap<Future<?>, String>();
		changeCallbacks = new HashSet<Callback<Map<Future<?>,String>,Void>>();
//		delegateHigh = Executors.newCachedThreadPool(new NamedThreadFactory("high", Thread.NORM_PRIORITY));
//		delegateLow = Executors.newCachedThreadPool(new NamedThreadFactory("low", Thread.MIN_PRIORITY));
		delegateHigh = Executors.newFixedThreadPool(30, new NamedThreadFactory("norm-prority-threadpool", Thread.NORM_PRIORITY));
		delegateLow = Executors.newFixedThreadPool(20, new NamedThreadFactory(" min-prority-threadpool", Thread.MIN_PRIORITY));

		final Timer timer = new Timer("threadpool-cleanup-timer", true);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				cleanupFutures();
			}
		}, 1000, 1000);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			shutdown(delegateHigh);
			shutdown(delegateLow);
		}));

		LOGGER.info("singleton is created");
	}

	private void shutdown(final ExecutorService service) {
		if (!service.isShutdown()) {
			service.shutdown();
			LOGGER.info("shutdown {}", service);
		} else if (!service.isTerminated()) {
			service.shutdownNow();
			LOGGER.info("shutdown now {}", service);
		}
	}

	public void submitHigh(final Runnable runnable, final String description) {
		submit(delegateHigh, runnable, description);
	}

	public void submitLow(final Runnable runnable, final String description) {
		submit(delegateLow, runnable, description);
	}

	public <T> Future<T> submitHigh(final Callable<T> callable, final String description) {
		return submit(delegateHigh, callable, description);
	}

	public <T> Future<T> submitLow(final Callable<T> callable, final String description) {
		return submit(delegateLow, callable, description);
	}

	public void setOnChange(final Callback<Map<Future<?>, String>, Void> callback) {
		changeCallbacks.add(callback);
		callback.call(futureDesc);
	}

	private void submit(final ExecutorService delegate, final Runnable runnable, final String description) {
		addFuture(delegate.submit(runnable), description);
	}

	private <T> Future<T> submit(final ExecutorService delegate, final Callable<T> callable, final String description) {
		return addFuture(delegate.submit(callable), description);
	}

	private <T> Future<T> addFuture(final Future<T> future, final String description) {
		futureDesc.put(future, description);
		changeCallbacks.forEach(c -> c.call(futureDesc));
		return future;
	}

	private void cleanupFutures() {
		boolean changed = false;
		for(final Iterator<Future<?>> i = futureDesc.keySet().iterator(); i.hasNext();) {
			if (i.next().isDone()) {
				i.remove();
				changed = true;
			}
		}
		if (changed) {
			changeCallbacks.forEach(c -> c.call(futureDesc));
		}
	}
}
