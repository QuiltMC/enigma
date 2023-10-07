package org.quiltmc.enigma.network;

import org.quiltmc.enigma.api.translation.mapping.EntryChange;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.network.packet.Packet;

import java.util.List;

public interface ClientPacketHandler {
	void openMappings(EntryTree<EntryMapping> mappings);

	boolean applyChangeFromServer(EntryChange<?> change);

	void disconnectIfConnected(String reason);

	void sendPacket(Packet<ServerPacketHandler> packet);

	void addMessage(ServerMessage message);

	void updateUserList(List<String> users);
}
