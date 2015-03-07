package net.anfoya.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(LockManager.class);

	public enum LockState { LOCKED, UNLOCKED };

	private final Status myLock;
	StatusManager statusMgr;

	public LockManager(final String lockName, final StatusManager statusMgr) {
		this.myLock = new Status(lockName, LockState.UNLOCKED.toString());
		this.statusMgr = statusMgr;

		if (!statusMgr.exists(myLock.getType())) {
			statusMgr.setStatus(myLock);
		}
	}

	public boolean lock() {
		boolean locked = false;
		if (isLockedByMe()) {
			locked = true;
		} else if (!isLocked()) {
		    final Status lock = myLock.copyWithValue(LockState.LOCKED.toString());
			LOGGER.info("locking {}", lock.toString());
			locked = statusMgr.setStatus(lock);
		}
		return locked;
	}

	public boolean unlock() {
		boolean unlocked = false;
		if (isLockedByMe()) {
			final Status lock = myLock.copyWithValue(LockState.UNLOCKED.toString());
		    LOGGER.info("unlocking {}", lock.toString());
		    unlocked = statusMgr.setStatus(lock);
		}
		return unlocked;
	}

	protected boolean isLocked() {
		boolean locked = true;
		if (statusMgr.exists(myLock.getType())) {
			final Status lock = statusMgr.getStatus(myLock.getType());
			locked = LockState.LOCKED.toString().equals(lock.getValue());
		}
		return locked;
	}

	protected boolean isLockedByMe() {
		boolean lockedByMe = true;
		if (statusMgr.exists(myLock.getType())) {
			final Status lock = statusMgr.getStatus(myLock.getType());
			lockedByMe = LockState.LOCKED.toString().equals(lock.getValue())
					&& myLock.getUsername().equals(lock.getUsername());
		}
		return lockedByMe;
	}
}
