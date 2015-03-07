package net.anfoya.cluster;

import net.anfoya.cluster.LockManager;
import net.anfoya.cluster.StatusManager;

import org.jgroups.JChannel;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LockManagerTest {

	private static StatusManager statusMgr;

	@BeforeClass
	public static void init() throws Exception {
		statusMgr = new StatusManager("Movie test", new JChannel());
	}

	@Test
	public void lockTest() throws Exception {
		final LockManager lockMgr = new LockManager("lockTest", statusMgr);

		Assert.assertFalse(lockMgr.isLocked());

		lockMgr.lock();
		Assert.assertTrue(lockMgr.isLocked());
	}

	@Test
	public void unlockTest() throws Exception {
		final LockManager lockMgr = new LockManager("unlockTest", statusMgr);

		Assert.assertFalse(lockMgr.isLocked());

		lockMgr.lock();
		Assert.assertTrue(lockMgr.isLocked());

		lockMgr.unlock();
		Assert.assertFalse(lockMgr.isLocked());
	}
}
