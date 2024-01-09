package org.quiltmc.enigma.network;

import org.quiltmc.enigma.api.translation.mapping.EntryChange;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.network.packet.Packet;

import java.util.List;

public class DummyClientPacketHandler implements ClientPacketHandler {
	TestEnigmaClient client;

	@Override
	public void openMappings(EntryTree<EntryMapping> mappings) {
	}

	@Override
	public boolean applyChangeFromServer(EntryChange<?> change) {
		return true;
	}

	@Override
	public void disconnectIfConnected(String reason) {
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
