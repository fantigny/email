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

import net.anfoya.java.util.VoidCallback;

public final class ThreadPool2 {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPool2.class);

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

	private final ExecutorService delegate;

	private final Set<VoidCallback<Map<Future<?>, String>>> changeCallbacks;
	private final Map<Future<?>, String> futureDesc;

	public ThreadPool2(final String name, final int priority) {
		delegate = Executors.newFixedThreadPool(20, new NamedThreadFactory(" min-prority-threadpool", priority));

		changeCallbacks = new HashSet<VoidCallback<Map<Future<?>, String>>>();
		futureDesc = new ConcurrentHashMap<Future<?>, String>();

		new Timer("threadpool-cleanup-timer", true).schedule(new TimerTask() {
			@Override
			public void run() {
				cleanupFutures();
			}
		}, 1000, 1000);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));

		LOGGER.info("singleton is created");
	}

	private void shutdown() {
		if (!delegate.isShutdown()) {
			delegate.shutdown();
			LOGGER.info("shutdown {}", delegate);
		} else if (!delegate.isTerminated()) {
			delegate.shutdownNow();
			LOGGER.info("shutdown now {}", delegate);
		}
	}

	public void submit(final Runnable runnable, final String description) {
		submit(delegate, runnable, description);
	}

	public <T> Future<T> submit(final Callable<T> callable, final String description) {
		return submit(delegate, callable, description);
	}

	public void setOnChange(final VoidCallback<Map<Future<?>, String>> callback) {
		changeCallbacks.add(callback);
		callback.call(futureDesc);
	}

	public void wait(final int ms, final String description) {
		final String desc = "waiting for " + description;
		submit(() -> {
			try {
				Thread.sleep(ms);
			} catch (final Exception e) {
				LOGGER.error(desc, e);
			}
		}, desc);
	}

	private void submit(final ExecutorService delegate, final Runnable runnable, final String description) {
		addFuture(delegate, delegate.submit(runnable), description);
	}

	private <T> Future<T> submit(final ExecutorService delegate, final Callable<T> callable, final String description) {
		return addFuture(delegate, delegate.submit(callable), description);
	}

	private <T> Future<T> addFuture(final ExecutorService delegate, final Future<T> future, final String description) {
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
