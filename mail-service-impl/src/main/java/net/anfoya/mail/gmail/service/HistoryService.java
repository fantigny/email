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
import net.anfoya.javafx.util.ThreadPool;
import net.anfoya.mail.gmail.model.GmailTag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.Message;

public class HistoryService extends TimerTask {
	private static final Logger LOGGER = LoggerFactory.getLogger(HistoryService.class);
	private static final String FILE_PREFIX = System.getProperty("java.io.tmpdir") + File.separatorChar + "fsm-cache-history-id-";

	private final Gmail gmail;
	private final String user;
	private final ReadOnlyBooleanWrapper disconnected;
	private final Set<Callback<Set<Message>, Void>> updateMessageCallBacks;
	private final Set<Callback<Set<Message>, Void>> addedMessageCallBacks;
	private final Set<Callback<Set<String>, Void>> updateLabelCallBacks;

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
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				new SerializedFile<BigInteger>(FILE_PREFIX + user).save(historyId);
			} catch (final IOException e) {
				LOGGER.error("saving history id", e);
			}
		}));

		updateMessageCallBacks = new LinkedHashSet<Callback<Set<Message>, Void>>();
		addedMessageCallBacks = new LinkedHashSet<Callback<Set<Message>, Void>>();
		updateLabelCallBacks = new LinkedHashSet<Callback<Set<String>, Void>>();
	}

	public void start(final Duration pullPeriod) {
		final long period = (long) pullPeriod.toMillis();
		timer = new Timer("history-pull-timer", true);
		timer.schedule(this, period, period);
	}

	@Override
	public void run() {
		ThreadPool.getInstance().submitLow(() -> {
			try {
				final List<History> updates = checkForUpdates();
				invokeCallbacks(updates);
			} catch (final HistoryException e) {
				LOGGER.error("pull updates", e);
			}
		}, "pull updates");
	}

	public List<History> checkForUpdates() throws HistoryException {
		final long start = System.currentTimeMillis();
		try {
			if (historyId == null) {
				historyId = gmail.users().getProfile(user).execute().getHistoryId();
				return null;
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

	private void invokeCallbacks(final List<History> updates) {
		if (updates == null) {
			updateLabelCallBacks.forEach(c -> c.call(null));
			updateMessageCallBacks.forEach(c -> c.call(null));
			return;
		}

		if (updates.isEmpty()) {
			return;
		}

		final Set<String> updatedLabelIds = new LinkedHashSet<String>();
		final Set<Message> updatedMessages = new LinkedHashSet<Message>();
		final Set<Message> addedMessages = new LinkedHashSet<Message>();
		for(final History h: updates) {
			if (h.getLabelsAdded() != null) {
				h.getLabelsAdded().forEach(history -> updatedLabelIds.addAll(history.getLabelIds()));
			}
			if (h.getLabelsRemoved() != null) {
				h.getLabelsRemoved().forEach(history -> updatedLabelIds.addAll(history.getLabelIds()));
			}
			if (h.getMessages() != null) {
				h.getMessages().forEach(m -> updatedMessages.add(m));
				if (h.getMessagesAdded() != null) {
					addedMessages.addAll(h.getMessagesAdded()
							.stream()
							.filter(a -> a.getMessage().getLabelIds() != null
									&& a.getMessage().getLabelIds().contains(GmailTag.UNREAD.getId())
									&& !a.getMessage().getLabelIds().contains(GmailTag.DRAFT.getId())
									&& !a.getMessage().getLabelIds().contains(GmailTag.SPAM.getId())
									&& !a.getMessage().getLabelIds().contains(GmailTag.TRASH.getId())
									&& !a.getMessage().getLabelIds().contains(GmailTag.SENT.getId()))
							.map(HistoryMessageAdded::getMessage)
							.collect(Collectors.toSet()));
				}
			}
		}

		if (!updatedLabelIds.isEmpty()) {
			updateLabelCallBacks.forEach(c -> c.call(updatedLabelIds));
		}
		if (!updatedMessages.isEmpty()) {
			updateMessageCallBacks.forEach(c -> c.call(updatedMessages));
		}
		if (!addedMessages.isEmpty()) {
			addedMessageCallBacks.forEach(c -> c.call(addedMessages));
		}

	}

	public void addOnUpdateMessage(final Callback<Set<Message>, Void> callback) {
		updateMessageCallBacks.add(callback);
	}

	public void addOnAddedMessage(final Callback<Set<Message>, Void> callback) {
		addedMessageCallBacks.add(callback);
	}

	public void addOnUpdateLabel(final Callback<Set<String>, Void> callback) {
		updateLabelCallBacks.add(callback);
	}

	public void clearCache() {
		historyId = null;
	}

	public ReadOnlyBooleanProperty disconnected() {
		return disconnected.getReadOnlyProperty();
	}
}
