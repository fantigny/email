package net.anfoya.mail.browser.controller.vo;

import java.util.Set;

import net.anfoya.mail.model.Tag;
import net.anfoya.mail.model.Thread;

public class TagForThreadsVo<T extends Tag, H extends Thread> {

	private final T tag;
	private final Set<H> threads;

	public TagForThreadsVo(T tag, Set<H> threads) {
		this.tag = tag;
		this.threads = threads;
	}

	public T getTag() {
		return tag;
	}

	public Set<H> getThreads() {
		return threads;
	}

}
