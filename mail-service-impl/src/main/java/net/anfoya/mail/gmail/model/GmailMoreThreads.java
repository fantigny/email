package net.anfoya.mail.gmail.model;

import java.util.Date;
import java.util.HashSet;

@SuppressWarnings("serial")
public class GmailMoreThreads extends GmailThread {
	public static final GmailThread NEXT_PAGE = new GmailMoreThreads(-1);

	private final int page;

	public GmailMoreThreads(final int page) {
		super(PAGE_TOKEN_ID, "", new HashSet<String>(), new HashSet<String>(), "more results...", new Date());
		this.page = page;
	}

	public int getPage() {
		return page;
	}
}
