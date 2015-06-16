package net.anfoya.mail.gmail.service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javafx.util.Callback;
import net.anfoya.java.io.SerializedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

public class HistoryService extends TimerTask {
	private static final Logger LOGGER = LoggerFactory.getLogger(HistoryService.class);
	private static final String FILE_PREFIX = System.getProperty("java.io.tmpdir") + "/fsm-cache-history-id-";

	private final Gmail gmail;
	private final String user;
	private final Set<Callback<Throwable, Void>> onUpdateCallBacks;
	private final Set<Callback<Throwable, Void>> onLabelUpdateCallBacks;

	private Timer timer;
	private BigInteger historyId;

	private enum UpdateType { NONE, LABEL, UPDATE };

	public HistoryService(final Gmail gmail, final String user) {
		this.gmail = gmail;
		this.user = user;

		try {
			historyId = new SerializedFile<BigInteger>(FILE_PREFIX + user).load();
		} catch (ClassNotFoundException | IOException e) {
			historyId = null;
		}
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					new SerializedFile<BigInteger>(FILE_PREFIX + user).save(historyId);
				} catch (final IOException e) {
					LOGGER.error("saving history id", e);
				}
			}
		}));

		onUpdateCallBacks = new LinkedHashSet<Callback<Throwable, Void>>();
		onLabelUpdateCallBacks = new LinkedHashSet<Callback<Throwable, Void>>();
	}

	public void start(final long period) {
		timer = new Timer(true);
		timer.schedule(this, period, period);
	}

	@Override
	public void run() {
		Set<UpdateType> types = null;
		Throwable exception = null;
		try {
			types = getUpdateTypes();
		} catch (final HistoryException e) {
			exception = e;
		}
		if (exception != null) {
			call(onLabelUpdateCallBacks, exception);
			call(onUpdateCallBacks, exception);
		} else if (types.contains(UpdateType.UPDATE)) {
			if (types.contains(UpdateType.LABEL)) {
				call(onLabelUpdateCallBacks, null);
			}
			call(onUpdateCallBacks, null);
		}
	}

	private void call(final Set<Callback<Throwable, Void>> callBacks, final Throwable exception) {
		for(final Callback<Throwable, Void> c: callBacks) {
			c.call(exception);
		}
	}

	public Set<UpdateType> getUpdateTypes() throws HistoryException {
		final long start = System.currentTimeMillis();
		try {
			final Set<UpdateType> types = new HashSet<>();
			if (historyId == null) {
				final ListMessagesResponse response = gmail.users().messages().list(user).setMaxResults(1L).execute();
				final String messageId = response.getMessages().iterator().next().getId();
				final Message message = gmail.users().messages().get(user, messageId).execute();
				historyId = message.getHistoryId();
				types.add(UpdateType.UPDATE);
			} else {
				final ListHistoryResponse response = gmail.users().history().list(user).setStartHistoryId(historyId).execute();
				final BigInteger previous = historyId;
				historyId = response.getHistoryId();
				if (historyId.equals(previous)) {
					types.add(UpdateType.NONE);
				} else {
					types.add(UpdateType.UPDATE);
					if (response.getHistory() == null) {
						types.add(UpdateType.LABEL);
					} else {
						for(final History h: response.getHistory()) {
							if (h.getLabelsAdded() != null || h.getLabelsRemoved() != null) {
								types.add(UpdateType.LABEL);
								break;
							}
						}
					}
				}
			}
			return types;
		} catch (final Exception e) {
			throw new HistoryException("getting history id", e);
		} finally {
			LOGGER.debug("got history id: {} ({}ms)", historyId, System.currentTimeMillis()-start);
		}
	}

	public void addOnUpdate(final Callback<Throwable, Void> callback) {
		onUpdateCallBacks.add(callback);
	}

	public void addOnLabelUpdate(final Callback<Throwable, Void> callback) {
		onLabelUpdateCallBacks.add(callback);
	}

	public void clearCache() {
		historyId = null;
	}
}
