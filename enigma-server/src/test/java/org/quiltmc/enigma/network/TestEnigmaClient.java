package org.quiltmc.enigma.network;

import org.quiltmc.enigma.network.packet.Packet;

import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class TestEnigmaClient extends EnigmaClient {
	CountDownLatch packetSentLatch = new CountDownLatch(1);

	public TestEnigmaClient(ClientPacketHandler handler, String ip, int port) {
		super(handler, ip, port);
		this.logPackets = true;
	}

	@Override
	protected void runOnThread(Runnable task) {
		task.run();
	}

	@Override
	public void sendPacket(Packet<ServerPacketHandler> packet) {
		super.sendPacket(packet);
		if (this.packetSentLatch != null) {
			this.packetSentLatch.countDown();
		}
	}

	/**
	 * Don't call this at the end of a test; use {@link NetworkTest#kickAfterTest(Socket)} instead.
	 */
	@Override
	@Deprecated
	public synchronized void disconnect() {
		super.disconnect();
	}
}
