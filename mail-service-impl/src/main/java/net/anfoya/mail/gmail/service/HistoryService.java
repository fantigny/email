package net.anfoya.mail.gmail.service;

import java.io.File;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.Message;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.util.Duration;
import net.anfoya.java.io.SerializedFile;
import net.anfoya.java.util.VoidCallback;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.java.util.system.ShutdownHook;

public class HistoryService extends TimerTask {
	private static final Logger LOGGER = LoggerFactory.getLogger(HistoryService.class);
	private static final String FILE_PREFIX = System.getProperty("java.io.tmpdir") + File.separatorChar + "fsm-cache-history-id-";

	private final Gmail gmail;
	private final String user;
	private final ReadOnlyBooleanWrapper disconnected;
	private final Set<VoidCallback<Set<Message>>> updateMessageCallBacks;
	private final Set<VoidCallback<Set<Message>>> addedMessageCallBacks;
	private final Set<Runnable> updateLabelCallBacks;

	private Timer timer;
	private BigInteger historyId;

	public HistoryService(final Gmail gmail, final String user) {
		this.gmail = gmail;
		this.user = user;

		disconnected = new ReadOnlyBooleanWrapper();
		disconnected.addListener((ov, o, n) -> {
			if (!n & o) {

			}
		});

		final SerializedFile<BigInteger> file = new SerializedFile<>(FILE_PREFIX + user);
		try {
			historyId = file.load();
		} catch (final Exception e) {
			historyId = null;
		}

		updateMessageCallBacks = new LinkedHashSet<>();
		addedMessageCallBacks = new LinkedHashSet<>();
		updateLabelCallBacks = new LinkedHashSet<>();
		
		new ShutdownHook(() -> {
			LOGGER.info("saving...");
			new SerializedFile<>(FILE_PREFIX + user).save(historyId);
		});
	}

	public void start(final Duration pullPeriod) {
		final long period = (long) pullPeriod.toMillis();
		timer = new Timer("history-pull-timer", true);
		timer.schedule(this, period, period);
	}

	@Override
	public void run() {
		ThreadPool.getDefault().submit(PoolPriority.MIN, "pull updates", () -> {
			try {
				final List<History> updates = getUpdates();
				disconnected.set(false);
				if (updates != null) {
					invokeCallbacks(updates);
				}
			} catch (final HistoryException e) {
				if (e.getCause() instanceof UnknownHostException
						|| e.getCause() instanceof SocketTimeoutException) {
					disconnected.set(true);
					LOGGER.error("connection lost {}", e.getCause().getMessage());
				} else {
					LOGGER.error("pull updates", e);
				}
			}
		});
	}

	public List<History> getUpdates() throws HistoryException {
		final long start = System.currentTimeMillis();
		try {
			if (historyId == null) {
				historyId = gmail.users().getProfile(user).execute().getHistoryId();
				LOGGER.info("new historyId: {}", historyId);
				return null;
			}

			final ListHistoryResponse response = gmail.users().history().list(user).setStartHistoryId(historyId).execute();
			disconnected.set(false);

			final BigInteger previous = historyId;
			historyId = response.getHistoryId();
			if (historyId.equals(previous)) {
				return null;
			}

			LOGGER.info("updated historyId: {}", historyId);
			if (response.getHistory() == null) {
				return new ArrayList<>();
			} else {
				return response.getHistory();
			}
		} catch (final Exception e) {
			historyId = null;
			throw new HistoryException("get history id", e);
		} finally {
			LOGGER.debug("got history id: {} ({}ms)", historyId, System.currentTimeMillis()-start);
		}
	}

	private void invokeCallbacks(final List<History> updates) {
		if (updates.isEmpty()) {
			updateLabelCallBacks.forEach(c -> c.run());
			return;
		}

		final Set<Message> updatedMessages = new LinkedHashSet<>();
		final Set<Message> addedMessages = new LinkedHashSet<>();
		for(final History h: updates) {
			if (h.getLabelsAdded() != null) {
				h.getLabelsAdded().forEach(l -> updatedMessages.add(l.getMessage()));
			}
			if (h.getLabelsRemoved() != null) {
				h.getLabelsRemoved().forEach(l -> updatedMessages.add(l.getMessage()));
			}
			if (h.getMessages() != null) {
				h.getMessages().forEach(m -> updatedMessages.add(m));
			}
			if (h.getMessagesAdded() != null) {
				addedMessages.addAll(h.getMessagesAdded()
						.stream()
						.map(m -> m.getMessage())
						.collect(Collectors.toSet()));
			}
		}

		if (!updatedMessages.isEmpty()) {
			updateMessageCallBacks.forEach(c -> c.call(updatedMessages));
		}
		if (!addedMessages.isEmpty()) {
			addedMessageCallBacks.forEach(c -> c.call(addedMessages));
		}
	}

	public void addOnUpdateMessage(final VoidCallback<Set<Message>> callback) {
		updateMessageCallBacks.add(callback);
	}

	public void addOnAddedMessage(final VoidCallback<Set<Message>> callback) {
		addedMessageCallBacks.add(callback);
	}

	public void addOnUpdateLabel(final Runnable callback) {
		updateLabelCallBacks.add(callback);
	}

	public void clearCache() {
		historyId = null;
		new SerializedFile<BigInteger>(FILE_PREFIX + user).clear();
		updateLabelCallBacks.forEach(c -> c.run());
	}

	public ReadOnlyBooleanProperty disconnected() {
		return disconnected.getReadOnlyProperty();
	}

	public void stop() {
		timer.cancel();
	}
}
