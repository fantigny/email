package net.anfoya.mail.gmail.model;

import java.util.Collections;
import java.util.Date;

@SuppressWarnings("serial")
public class GmailMoreThreads extends GmailThread {
	private final int page;

	public GmailMoreThreads(final int page) {
		super(PAGE_TOKEN_ID
				, ""
				, Collections.emptySet()
				, ""
				, Collections.emptySet()
				, "more results..."
				, Collections.emptySet()
				, new Date());
		this.page = page;
	}

	public int getPage() {
		return page;
	}
}
