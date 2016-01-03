package net.anfoya.java.util.concurrent;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ObservableExecutors {
	private static class NamedThreadFactory implements ThreadFactory {
	    private final String name;
	    private final int priority;

	    private final ThreadGroup group;
	    private final AtomicInteger threadNumber = new AtomicInteger(1);

	    private NamedThreadFactory(final String name, final int priority) {
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

	public static ObservableExecutorService newCachedThreadPool(String name, int priority) {
		return new ObservableExecutorService(name, Executors.newCachedThreadPool(new NamedThreadFactory(name, priority)));
	}
}
