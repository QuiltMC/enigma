package org.quiltmc.enigma.network;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.network.packet.c2s.LoginC2SPacket;
import org.quiltmc.enigma.network.packet.c2s.MessageC2SPacket;
import org.quiltmc.enigma.util.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NetworkTest {
	private static final Path JAR = TestUtil.obfJar("complete");
	private static final String PASSWORD = "foobar";
	private static byte[] checksum;
	private static TestEnigmaServer server;
	private static EntryRemapper remapper;

	@BeforeAll
	public static void startServer() throws IOException {
		Enigma enigma = Enigma.create();
		EnigmaProject project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.none());

		checksum = Utils.zipSha1(JAR);
		remapper = project.getRemapper();
		server = new TestEnigmaServer(checksum, PASSWORD.toCharArray(), remapper, 0);

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

	@Test
	public void testLogin() throws IOException, InterruptedException {
		var handler = new DummyClientPacketHandler();
		var client = connectClient(handler);
		handler.client = client;

		Assertions.assertFalse(server.getClients().isEmpty());
		Assertions.assertFalse(server.getUnapprovedClients().isEmpty());

		client.sendPacket(new LoginC2SPacket(checksum, PASSWORD.toCharArray(), "alice"));
		var confirmed = server.waitChangeConfirmation(server.getClients().get(0), 1)
				.await(3, TimeUnit.SECONDS);

		Assertions.assertNotEquals(0, handler.disconnectFromServerLatch.getCount(), "The client was disconnected by the server");
		Assertions.assertTrue(confirmed, "Timed out waiting for the change confirmation");
		client.disconnect();
	}

	@Test
	public void testInvalidUsername() throws IOException, InterruptedException {
		var handler = new DummyClientPacketHandler();
		var client = connectClient(handler);
		handler.client = client;

		client.sendPacket(new LoginC2SPacket(checksum, PASSWORD.toCharArray(), "<span style=\"color: lavender\">eve</span>"));
		var disconnected = handler.disconnectFromServerLatch.await(3, TimeUnit.SECONDS);

		Assertions.assertTrue(disconnected, "Timed out waiting for the server to kick the client");
		client.disconnect();
	}

	@Test
	public void testWrongPassword() throws IOException, InterruptedException {
		var handler = new DummyClientPacketHandler();
		var client = connectClient(handler);
		handler.client = client;

		client.sendPacket(new LoginC2SPacket(checksum, "password".toCharArray(), "eve"));
		var disconnected = handler.disconnectFromServerLatch.await(3, TimeUnit.SECONDS);

		Assertions.assertTrue(disconnected, "Timed out waiting for the server to kick the client");
		client.disconnect();
	}

	@Test
	public void testTakenUsername() throws IOException, InterruptedException {
		var packet = new LoginC2SPacket(checksum, PASSWORD.toCharArray(), "alice");

		var handler = new DummyClientPacketHandler();
		var client = connectClient(handler);
		handler.client = client;
		client.sendPacket(packet);

		var handler2 = new DummyClientPacketHandler();
		var client2 = connectClient(handler2);
		handler2.client = client2;

		client2.sendPacket(packet);
		var disconnected = handler2.disconnectFromServerLatch.await(3, TimeUnit.SECONDS);

		Assertions.assertTrue(disconnected, "Timed out waiting for the server to kick the client");
		client.disconnect();
		client2.disconnect();
	}

	@Test
	public void testWrongChecksum() throws IOException, InterruptedException {
		var handler = new DummyClientPacketHandler();
		var client = connectClient(handler);
		handler.client = client;

		handler.disconnectFromServerLatch = new CountDownLatch(1);
		client.sendPacket(new LoginC2SPacket(new byte[EnigmaServer.CHECKSUM_SIZE], PASSWORD.toCharArray(), "eve"));
		var disconnected = handler.disconnectFromServerLatch.await(3, TimeUnit.SECONDS);

		Assertions.assertTrue(disconnected, "Timed out waiting for the server to kick the client");
		client.disconnect();
	}

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
