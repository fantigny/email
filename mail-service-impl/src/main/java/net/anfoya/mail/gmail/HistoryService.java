package net.anfoya.mail.gmail;

import java.io.IOException;
import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

public class HistoryService {
	private static final Logger LOGGER = LoggerFactory.getLogger(HistoryService.class);

	private final Gmail gmail;
	private final String user;

	private BigInteger historyId;

	public HistoryService(final Gmail gmail, final String user) {
		this.gmail = gmail;
		this.user = user;

		historyId = null;
	}

	public boolean hasUpdate() throws HistoryException {
		final long start = System.currentTimeMillis();
		try {
			final boolean hasUpdate;
			if (historyId == null) {
				final ListMessagesResponse response = gmail.users().messages().list(user).setMaxResults(1L).execute();
				final String messageId = response.getMessages().iterator().next().getId();
				final Message message = gmail.users().messages().get(user, messageId).execute();
				historyId = message.getHistoryId();
				hasUpdate = true;
			} else {
				final BigInteger previous = historyId;
				final ListHistoryResponse response = gmail.users().history().list(user).setMaxResults(1L).setStartHistoryId(historyId).execute();
				if (response.getHistory() != null) {
					historyId = response.getHistoryId();
				}
				hasUpdate = !historyId.equals(previous);
			}
			return hasUpdate;
		} catch (final IOException e) {
			throw new HistoryException("getting history id", e);
		} finally {
			LOGGER.debug("got history id: {} ({}ms)", historyId, System.currentTimeMillis()-start);
		}
	}
}
