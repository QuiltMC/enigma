package org.quiltmc.enigma.network;

public class TestEnigmaClient extends EnigmaClient {
	public TestEnigmaClient(ClientPacketHandler handler, String ip, int port) {
		super(handler, ip, port);
		this.logPackets = true;
	}

	@Override
	protected void runOnThread(Runnable task) {
		task.run();
	}
}
