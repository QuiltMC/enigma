package org.quiltmc.enigma.network;

import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

public class TestEnigmaServer extends EnigmaServer {
	private final Map<Socket, CountDownLatch> changeConfirmationLatches = new ConcurrentHashMap<>();
	private final BlockingQueue<Runnable> tasks = new LinkedBlockingDeque<>();

	CountDownLatch sendMessageLatch;

	public TestEnigmaServer(byte[] jarChecksum, char[] password, EntryRemapper remapper, int port) {
		super(jarChecksum, password, remapper, port);
	}

	@Override
	public void start() throws IOException {
		super.start();

		var tasksThread = new Thread(() -> {
			while (true) {
				try {
					this.tasks.take().run();
				} catch (InterruptedException e) {
					break;
				}
			}
		});
		tasksThread.setName("Test server tasks");
		tasksThread.setDaemon(true);
		tasksThread.start();
	}

	@Override
	protected void runOnThread(Runnable task) {
		this.tasks.add(task);
	}

	@Override
	public void confirmChange(Socket client, int syncId) {
		super.confirmChange(client, syncId);

		var latch = this.changeConfirmationLatches.get(client);
		if (latch != null) {
			latch.countDown();
		}
	}

	public CountDownLatch waitChangeConfirmation(Socket client, int count) {
		var latch = new CountDownLatch(count);
		this.changeConfirmationLatches.put(client, latch);
		return latch;
	}

	@Override
	public void sendMessage(ServerMessage message) {
		if (this.sendMessageLatch != null) {
			this.sendMessageLatch.countDown();
		}

		super.sendMessage(message);
	}
}
