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

public final class ObservableThreadPool {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObservableThreadPool.class);
	private static final long CLEANUP_PERIOD_MS = 250;

	private static class NamedThreadFactory implements ThreadFactory {
	    private final String name;
	    private final int priority;

	    private final ThreadGroup group;
	    private final AtomicInteger threadNumber = new AtomicInteger(1);

	    protected NamedThreadFactory(final String name, final int priority) {
	    	this.name = name;
	    	this.priority = priority;

	        final SecurityManager s = System.getSecurityManager();
	        group = s != null? s.getThreadGroup(): Thread.currentThread().getThreadGroup();
	    }

	    @Override
		public Thread newThread(final Runnable r) {
	        final Thread t = new Thread(group, r, String.format("tp-%s-%d", name, threadNumber.getAndIncrement()));
            t.setDaemon(true);
            t.setPriority(priority);
	        return t;
	    }
	}

	private final ExecutorService delegate;
	private final Map<Future<?>, String> futureDesc;
	private final Set<VoidCallback<Map<Future<?>, String>>> onChangeCallbacks;

	public ObservableThreadPool(final String name, final int priority) {
		delegate = Executors.newCachedThreadPool(new NamedThreadFactory(name, priority));
		futureDesc = new ConcurrentHashMap<Future<?>, String>();
		onChangeCallbacks = new HashSet<VoidCallback<Map<Future<?>, String>>>();

		new Timer(String.format("tp-%s-cleanup", name), true).schedule(
			new TimerTask() { @Override public void run() { cleanupFutures(); } }
			, CLEANUP_PERIOD_MS, CLEANUP_PERIOD_MS);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));

		LOGGER.info("created thread pool {}", name);
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
		onChangeCallbacks.add(callback);
		callback.call(futureDesc);
	}

	private void submit(final ExecutorService delegate, final Runnable runnable, final String description) {
		addFuture(delegate, delegate.submit(runnable), description);
	}

	private <T> Future<T> submit(final ExecutorService delegate, final Callable<T> callable, final String description) {
		return addFuture(delegate, delegate.submit(callable), description);
	}

	private <T> Future<T> addFuture(final ExecutorService delegate, final Future<T> future, final String description) {
		futureDesc.put(future, description);
		onChangeCallbacks.forEach(c -> c.call(futureDesc));

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
			onChangeCallbacks.forEach(c -> c.call(futureDesc));
		}
	}
}
