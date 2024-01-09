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
import org.quiltmc.enigma.util.Utils;

import java.io.IOException;
import java.nio.file.Path;
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

		Assertions.assertEquals(1, server.getClients().size());
		Assertions.assertEquals(1, server.getUnapprovedClients().size());

		client.sendPacket(new LoginC2SPacket(checksum, PASSWORD.toCharArray(), "alice"));
		var confirmed = server.waitChangeConfirmation(server.getClients().get(0), 1)
				.await(3, TimeUnit.SECONDS);

		Assertions.assertTrue(confirmed, "Timed out waiting for the change confirmation");
	}
}
