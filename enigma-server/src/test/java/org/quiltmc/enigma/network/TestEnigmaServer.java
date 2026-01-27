package org.quiltmc.enigma.network;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class TestEnigmaServer extends EnigmaServer {
	@NonNull
	private CountDownLatch connectionLatch = new CountDownLatch(0);
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
	Socket acceptClient() throws IOException {
		final Socket socket = super.acceptClient();

		this.connectionLatch.countDown();

		return socket;
	}

	public void queueConnectionWait() {
		Assertions.assertEquals(0, this.connectionLatch.getCount());
		this.connectionLatch = new CountDownLatch(1);
	}

	public boolean awaitNextConnection(int timeout, TimeUnit unit) throws InterruptedException {
		return this.connectionLatch.await(timeout, unit);
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
