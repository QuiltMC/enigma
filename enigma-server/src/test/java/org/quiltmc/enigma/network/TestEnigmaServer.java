package org.quiltmc.enigma.network;

import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;

import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class TestEnigmaServer extends EnigmaServer {
	private final Map<Socket, CountDownLatch> changeConfirmationLatches = new ConcurrentHashMap<>();

	public TestEnigmaServer(byte[] jarChecksum, char[] password, EntryRemapper remapper, int port) {
		super(jarChecksum, password, remapper, port);
	}

	@Override
	protected void runOnThread(Runnable task) {
		task.run();
	}

	@Override
	public void confirmChange(Socket client, int syncId) {
		super.confirmChange(client, syncId);

		var latch = this.changeConfirmationLatches.get(client);
		latch.countDown();
	}

	public CountDownLatch waitChangeConfirmation(Socket client, int count) {
		var latch = new CountDownLatch(count);
		this.changeConfirmationLatches.put(client, latch);
		return latch;
	}
}
