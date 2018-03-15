package net.anfoya.cluster;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.util.Callback;
import net.anfoya.java.util.concurrent.ThreadPool.PoolPriority;
import net.anfoya.java.util.concurrent.ThreadPool;

public class StatusManager extends ReceiverAdapter {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatusManager.class);

	private final JChannel channel;

	private final Map<String, Status> state;
	private final Future<Boolean> initFuture;

	private final Set<Callback<Status, Void>> receivedCallbacks;

	public StatusManager(final String clusterName, final JChannel channel) {
		this.channel = channel;
		this.state = new ConcurrentHashMap<String, Status>();
		this.receivedCallbacks = new LinkedHashSet<Callback<Status,Void>>();

		this.channel.setReceiver(this);
		this.channel.setDiscardOwnMessages(true);

		// lazy initialization for faster startup
		initFuture = ThreadPool.getDefault().submit(PoolPriority.MAX, "initialize cluster", new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				// connect and fetch state
				channel.connect(clusterName, null, 1000);
				return Boolean.TRUE;
			}
		});
	}

	@Override
	public void receive(final Message message) {
		final Object object = message.getObject();
		if (object instanceof Status) {
			final Status status = (Status) object;
		    state.put(status.getType(), status);
		    LOGGER.info("status received {}", status.toString());

		    for(final Callback<Status, Void> callback: receivedCallbacks) {
		    	callback.call(status);
		    }
		}
	}

	@Override
	public void getState(final OutputStream output) throws Exception {
        Util.objectToStream(state, new DataOutputStream(output));
	    LOGGER.info("state sent {}", state.toString());
	}

	@Override
	public void setState(final InputStream input) throws Exception {
		final Object object = Util.objectFromStream(new DataInputStream(input));
		if (object instanceof Map) {
			@SuppressWarnings("unchecked")
			final Map<String, Status> state = (Map<String, Status>) object;
	    	this.state.clear();
	    	this.state.putAll(state);
		    LOGGER.info("state received {}", state);
		}
	}

	@Override
	public void viewAccepted(final View view) {
		LOGGER.info("cluster update {}", view);
	}

	public void shutdown() {
		channel.close();
	}

	public void addOnReceived(final Callback<Status, Void> callback) {
		this.receivedCallbacks.add(callback);
	}

	public Status getStatus(final String type) {
		if (!initialized()) {
			return null;
		}
		return state.get(type);
	}

	public boolean exists(final String type) {
		if (!initialized()) {
			return false;
		}
		return state.keySet().contains(type);
	}

	public boolean setStatus(final Status status) {
		if (!initialized()) {
			return false;
		}
		try {
			state.put(status.getType(), status);
			LOGGER.debug("sending status {}", status);
			channel.send(new Message(null, status));
			return true;
		} catch (final Exception e) {
		    LOGGER.error("sending status {}", status.toString(), e);
		    return false;
		}
	}

	private boolean initialized() {
		try {
			return initFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			return false;
		}
	}
}
