package cuchaz.enigma.network;

import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.network.packet.Packet;

import java.util.List;

public interface ClientPacketHandler {
	void openMappings(EntryTree<EntryMapping> mappings);

	boolean applyChangeFromServer(EntryChange<?> change);

	void disconnectIfConnected(String reason);

	void sendPacket(Packet<ServerPacketHandler> packet);

	void addMessage(ServerMessage message);

	void updateUserList(List<String> users);
}
