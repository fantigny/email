package net.anfoya.mail.gmail.service;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.util.Callback;
import javafx.util.Duration;
import net.anfoya.java.io.SerializedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

public class HistoryService extends TimerTask {
	private static final Logger LOGGER = LoggerFactory.getLogger(HistoryService.class);
	private static final String FILE_PREFIX = System.getProperty("java.io.tmpdir") + File.separatorChar + "fsm-cache-history-id-";

	private final Gmail gmail;
	private final String user;
	private final ReadOnlyBooleanWrapper disconnected;
	private final Set<Callback<Set<Message>, Void>> onUpdateMessageCallBacks;
	private final Set<Callback<Set<Message>, Void>> onAddedMessageCallBacks;
	private final Set<Callback<Set<String>, Void>> onUpdateLabelCallBacks;

	private Timer timer;
	private BigInteger historyId;

	public HistoryService(final Gmail gmail, final String user) {
		this.gmail = gmail;
		this.user = user;

		disconnected = new ReadOnlyBooleanWrapper(false);

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

		onUpdateMessageCallBacks = new LinkedHashSet<Callback<Set<Message>, Void>>();
		onAddedMessageCallBacks = new LinkedHashSet<Callback<Set<Message>, Void>>();
		onUpdateLabelCallBacks = new LinkedHashSet<Callback<Set<String>, Void>>();
	}

	public void start(final Duration pullPeriod) {
		final long period = (long) pullPeriod.toMillis();
		timer = new Timer(true);
		timer.schedule(this, period, period);
	}

	@Override
	public void run() {
		try {
			final List<History> histories = checkForUpdates();
			if (!histories.isEmpty()) {
				invokeCallbacks(histories);
			}
		} catch (final HistoryException e) {
			LOGGER.error("checking for updates", e);
		}
	}

	public List<History> checkForUpdates() throws HistoryException {
		final long start = System.currentTimeMillis();
		try {
			if (historyId == null) {
				final ListMessagesResponse response = gmail.users().messages().list(user).setMaxResults(1L).execute();
				final String messageId = response.getMessages().iterator().next().getId();
				final Message message = gmail.users().messages().get(user, messageId).execute();
				historyId = message.getHistoryId();
				onUpdateLabelCallBacks.forEach(c -> c.call(null));
				onUpdateMessageCallBacks.forEach(c -> c.call(null));
				return new ArrayList<History>();
			}

			final ListHistoryResponse response = gmail.users().history().list(user).setStartHistoryId(historyId).execute();
			if (disconnected.get()) {
				disconnected.set(false);
			}

			final BigInteger previous = historyId;
			historyId = response.getHistoryId();
			if (historyId.equals(previous) || response.getHistory() == null) {
				return new ArrayList<History>();
			}

			return response.getHistory();
		} catch (final Exception e) {
			historyId = null;
			if (e instanceof ConnectException) {
				if (!disconnected.get()) {
					disconnected.set(true);
				}
				return new ArrayList<History>();
			} else {
				throw new HistoryException("getting history id", e);
			}
		} finally {
			LOGGER.debug("got history id: {} ({}ms)", historyId, System.currentTimeMillis()-start);
		}
	}

	private void invokeCallbacks(final List<History> histories) {
		final Set<String> updatedLabelIds = new LinkedHashSet<String>();
		final Set<Message> updatedMessages = new LinkedHashSet<Message>();
		final Set<Message> addedMessages = new LinkedHashSet<Message>();
		for(final History h: histories) {
			if (h.getLabelsAdded() != null) {
				h.getLabelsAdded().forEach(history -> updatedLabelIds.addAll(history.getLabelIds()));
			}
			if (h.getLabelsRemoved() != null) {
				h.getLabelsRemoved().forEach(history -> updatedLabelIds.addAll(history.getLabelIds()));
			}
			if (h.getMessages() != null) {
				h.getMessages().forEach(m -> updatedMessages.add(m));
				if (h.getMessagesAdded() != null) {
					addedMessages.addAll(h.getMessagesAdded().stream().map(HistoryMessageAdded::getMessage).collect(Collectors.toSet()));
				}
			}
		}

		if (!updatedLabelIds.isEmpty()) {
			onUpdateLabelCallBacks.forEach(c -> c.call(updatedLabelIds));
		}
		if (!updatedMessages.isEmpty()) {
			onUpdateMessageCallBacks.forEach(c -> c.call(updatedMessages));
		}
		if (!addedMessages.isEmpty()) {
			onAddedMessageCallBacks.forEach(c -> c.call(addedMessages));
		}

	}

	public void addOnUpdateMessage(final Callback<Set<Message>, Void> callback) {
		onUpdateMessageCallBacks.add(callback);
	}

	public void addOnAddedMessage(final Callback<Set<Message>, Void> callback) {
		onAddedMessageCallBacks.add(callback);
	}

	public void addOnUpdateLabel(final Callback<Set<String>, Void> callback) {
		onUpdateLabelCallBacks.add(callback);
	}

	public void clearCache() {
		historyId = null;
	}

	public ReadOnlyBooleanProperty disconnected() {
		return disconnected.getReadOnlyProperty();
	}
}
