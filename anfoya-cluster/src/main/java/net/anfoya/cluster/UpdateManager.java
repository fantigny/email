package net.anfoya.cluster;

import javafx.util.Callback;

import org.jgroups.ReceiverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateManager extends ReceiverAdapter {
	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateManager.class);
	private static final Status UPDATE_STATUS = new Status("Update Manager", "UPDATE");

	private final StatusManager statusMgr;

	public UpdateManager(final StatusManager statusMgr) {
		this.statusMgr = statusMgr;
	}

	public void addOnUpdate(final Callback<Status, Void> callback) {
		this.statusMgr.addOnReceived(new Callback<Status, Void>() {
			@Override
			public Void call(final Status status) {
				if (UPDATE_STATUS.getType().equals(status.getType())
						&& UPDATE_STATUS.getValue().equals(status.getValue())) {
					callback.call(status);
				}
				return null;
			}
		});
	}

	public void updatePerformed() {
		LOGGER.info("broadcasting update {}", UPDATE_STATUS.toString());
		statusMgr.setStatus(UPDATE_STATUS);
	}
}
