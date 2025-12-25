package org.quiltmc.enigma.network;

import org.quiltmc.enigma.api.translation.mapping.EntryChange;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.network.packet.Packet;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DummyClientPacketHandler implements ClientPacketHandler {
	TestEnigmaClient client;
	CountDownLatch disconnectFromServerLatch = new CountDownLatch(1);

	@Override
	public void openMappings(EntryTree<EntryMapping> mappings) {
	}

	@Override
	public boolean applyChangeFromServer(EntryChange<?> change) {
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void disconnectIfConnected(String reason) {
		if (this.client != null) {
			this.client.disconnect();
		}

		if (this.disconnectFromServerLatch != null) {
			this.disconnectFromServerLatch.countDown();
		}
	}

	@Override
	public void sendPacket(Packet<ServerPacketHandler> packet) {
		if (this.client != null) {
			this.client.sendPacket(packet);
		}
	}

	@Override
	public void addMessage(ServerMessage message) {
	}

	@Override
	public void updateUserList(List<String> users) {
	}
}
