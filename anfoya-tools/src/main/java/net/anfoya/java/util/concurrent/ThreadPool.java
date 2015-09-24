package net.anfoya.java.util.concurrent;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ThreadPool {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPool.class);

	private static class NamedThreadFactory implements ThreadFactory {
	    private static final AtomicInteger poolNumber = new AtomicInteger(1);
	    private final ThreadGroup group;
	    private final AtomicInteger threadNumber = new AtomicInteger(1);
	    private final String namePrefix;
	    private final int priority;

	    protected NamedThreadFactory(final String name, final int priority) {
	        final SecurityManager s = System.getSecurityManager();
	        group = s != null? s.getThreadGroup(): Thread.currentThread().getThreadGroup();
	        namePrefix = "pool-" +
	                      poolNumber.getAndIncrement() +
	                     "-thread-" + name + "-";
	        this.priority = priority;
	    }

	    @Override
		public Thread newThread(final Runnable r) {
	        final Thread t = new Thread(group, r,
	                              namePrefix + threadNumber.getAndIncrement(),
	                              0);
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

	private final ObservableMap<Future<?>, String> futureDesc = FXCollections.observableMap(new LinkedHashMap<Future<?>, String>());
	private final Timer timer;

	private ThreadPool() {
//		delegateHigh = Executors.newCachedThreadPool(new NamedThreadFactory("high", Thread.NORM_PRIORITY));
//		delegateLow = Executors.newCachedThreadPool(new NamedThreadFactory("low", Thread.MIN_PRIORITY));
		delegateHigh = Executors.newFixedThreadPool(20, new NamedThreadFactory("high", Thread.NORM_PRIORITY));
		delegateLow = Executors.newFixedThreadPool(10, new NamedThreadFactory("low", Thread.MIN_PRIORITY));

		timer = new Timer("threadpool-cleanup-timer", true);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				cleanupFutures();
			}
		}, 1000, 1000);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));

		LOGGER.info("started!");
	}

	public void shutdown() {
		timer.cancel();
		shutdown(delegateHigh);
		shutdown(delegateLow);
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

	public ObservableMap<Future<?>, String> getFutureDescriptions() {
		return futureDesc;
	}

	private void submit(final ExecutorService delegate, final Runnable runnable, final String description) {
		final Future<?> future = delegate.submit(runnable);
		futureDesc.put(future, description);
	}

	private <T> Future<T> submit(final ExecutorService delegate, final Callable<T> callable, final String description) {
		final Future<T> future = delegate.submit(callable);
		futureDesc.put(future, description);
		return future;
	}

	private void cleanupFutures() {
		final Set<Future<?>> toRemove = new HashSet<Future<?>>();
		for(final Future<?> f: futureDesc.keySet()) {
			if (f!= null && f.isDone()) {
				toRemove.add(f);
			}
		}
		Platform.runLater(() -> {
			for(final Future<?> f: toRemove) {
				futureDesc.remove(f);
			}
		});
	}
}
