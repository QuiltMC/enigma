package org.quiltmc.enigma.network;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.network.packet.c2s.LoginC2SPacket;
import org.quiltmc.enigma.network.packet.c2s.MessageC2SPacket;
import org.quiltmc.enigma.network.packet.s2c.KickS2CPacket;
import org.quiltmc.enigma.util.Utils;

import java.io.IOException;
import java.net.BindException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <b>Warning</b>: Using {@link RepeatedTest @RepeatedTest} on tests that wait on
 * {@link DummyClientPacketHandler#disconnectFromServerLatch} eventually causes
 * {@link BindException}: {@code Address already in use: connect}.
 *
 * <p> It seems like all outbound sockets get exhausted and have a timeout before they become available.
 *
 * <p> On my (supersaiyansubtlety's) PC, the first exception comes after ~20,000 combined repetitions within ~2 minutes,
 * after which exceptions are frequently thrown until ~2 minutes have passed without any repetitions.
 *
 * <p> Each {@link DummyClientPacketHandler#disconnectFromServerLatch}-waiting test succeeded with 1,000 repetitions.
 *
 * <p> Repetitions are set to {@value #DEFAULT_REPETITIONS} to reasonably test flakiness while avoiding the socket cap.
 */
public class NetworkTest {
	private static final Path JAR = TestUtil.obfJar("complete");
	private static final String PASSWORD = "foobar";
	private static final int DEFAULT_REPETITIONS = 100;

	private static byte[] checksum;
	private static TestEnigmaServer server;

	@BeforeAll
	public static void startServer() throws IOException {
		Enigma enigma = Enigma.create();
		EnigmaProject project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());

		checksum = Utils.zipSha1(JAR);
		server = new TestEnigmaServer(checksum, PASSWORD.toCharArray(), project.getRemapper(), 0);

		server.start();
	}

	@AfterAll
	public static void stopServer() {
		server.stop();
	}

	private static TestEnigmaClient connectClient(ClientPacketHandler handler) throws IOException {
		var client = new TestEnigmaClient(handler, "127.0.0.1", server.getActualPort());
		client.connect();

		return client;
	}

	private static void awaitNextConnection() throws InterruptedException {
		Assertions.assertTrue(server.awaitNextConnection(3, TimeUnit.SECONDS), "Client did not connect");
	}

	/**
	 * <b>Never</b> directly {@linkplain EnigmaClient#disconnect() disconnect} clients after tests; <b>always</b>
	 * either manually {@link EnigmaServer#kick(Socket, String) kick} them (like this method does) or wait for them to
	 * be disconnected using {@link DummyClientPacketHandler#disconnectFromServerLatch}.
	 *
	 * <p> Directly {@linkplain EnigmaClient#disconnect() disconnecting} a client creates a race condition:
	 * The {@code "disconnect.disconnected"} {@link EnigmaServer#kick(Socket, String) kick} call in
	 * {@link EnigmaServer}'s client threads sends a {@link KickS2CPacket} that can be received by
	 * <em>other</em> test's clients.
	 */
	static void kickAfterTest(Socket clientSocket) {
		server.kick(clientSocket, "test complete");
	}

	@RepeatedTest(DEFAULT_REPETITIONS)
	public void testLogin() throws IOException, InterruptedException {
		final var handler = new DummyClientPacketHandler();

		server.queueConnectionWait();

		final TestEnigmaClient client = connectClient(handler);
		handler.client = client;

		awaitNextConnection();

		Assertions.assertNotEquals(0, handler.disconnectFromServerLatch.getCount(), "The client was disconnected by the server");

		final Socket clientSocket = server.getClients().get(0);
		final CountDownLatch changeConfirmation = server.waitChangeConfirmation(clientSocket, 1);
		client.sendPacket(new LoginC2SPacket(checksum, PASSWORD.toCharArray(), "alice"));
		final boolean confirmed = changeConfirmation.await(3, TimeUnit.SECONDS);

		Assertions.assertTrue(confirmed, "Timed out waiting for the change confirmation");

		kickAfterTest(clientSocket);
	}

	@RepeatedTest(DEFAULT_REPETITIONS)
	public void testInvalidUsername() throws IOException, InterruptedException {
		var handler = new DummyClientPacketHandler();
		var client = connectClient(handler);
		handler.client = client;

		client.sendPacket(new LoginC2SPacket(checksum, PASSWORD.toCharArray(), "<span style=\"color: lavender\">eve</span>"));
		boolean disconnected = handler.disconnectFromServerLatch.await(3, TimeUnit.SECONDS);

		Assertions.assertTrue(disconnected, "Timed out waiting for the server to kick the client");
	}

	@RepeatedTest(DEFAULT_REPETITIONS)
	public void testWrongPassword() throws IOException, InterruptedException {
		var handler = new DummyClientPacketHandler();
		var client = connectClient(handler);
		handler.client = client;

		client.sendPacket(new LoginC2SPacket(checksum, "password".toCharArray(), "eve"));
		var disconnected = handler.disconnectFromServerLatch.await(3, TimeUnit.SECONDS);

		Assertions.assertTrue(disconnected, "Timed out waiting for the server to kick the client");
	}

	// FIXME this test is flaky when run from workflows/build.yml
	@Test
	public void testTakenUsername() throws IOException, InterruptedException {
		final var packet = new LoginC2SPacket(checksum, PASSWORD.toCharArray(), "alice");

		final var handler = new DummyClientPacketHandler();

		server.queueConnectionWait();

		final TestEnigmaClient client1 = connectClient(handler);
		handler.client = client1;
		client1.sendPacket(packet);

		awaitNextConnection();

		final var handler2 = new DummyClientPacketHandler();

		server.queueConnectionWait();

		final TestEnigmaClient client2 = connectClient(handler2);
		handler2.client = client2;

		awaitNextConnection();

		client2.sendPacket(packet);
		final boolean disconnected = handler2.disconnectFromServerLatch.await(3, TimeUnit.SECONDS);

		Assertions.assertTrue(disconnected, "Timed out waiting for the server to kick the client");

		for (final Socket clientSocket : List.copyOf(server.getClients())) {
			kickAfterTest(clientSocket);
		}
	}

	@RepeatedTest(DEFAULT_REPETITIONS)
	public void testWrongChecksum() throws IOException, InterruptedException {
		var handler = new DummyClientPacketHandler();
		var client = connectClient(handler);
		handler.client = client;

		handler.disconnectFromServerLatch = new CountDownLatch(1);
		client.sendPacket(new LoginC2SPacket(new byte[EnigmaServer.CHECKSUM_SIZE], PASSWORD.toCharArray(), "eve"));
		var disconnected = handler.disconnectFromServerLatch.await(3, TimeUnit.SECONDS);

		Assertions.assertTrue(disconnected, "Timed out waiting for the server to kick the client");
	}

	// no repeats because successes take at least 3 seconds
	@Test
	public void testUnapprovedMessage() throws IOException, InterruptedException {
		var handler = new DummyClientPacketHandler();
		var client = connectClient(handler);
		handler.client = client;

		client.sendPacket(new MessageC2SPacket("I am in your (walls) server :3"));
		server.sendMessageLatch = new CountDownLatch(1);
		var sent = client.packetSentLatch.await(1, TimeUnit.SECONDS);
		Assertions.assertTrue(sent, "Failed to send packet");

		var handled = server.sendMessageLatch.await(2, TimeUnit.SECONDS);
		Assertions.assertFalse(handled, "The server handled an unapproved message!");
	}
}
