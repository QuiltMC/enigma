package org.quiltmc.enigma.network;

import org.quiltmc.enigma.network.packet.Packet;

import java.net.Socket;

public record ServerPacketHandler(Socket client, EnigmaServer server) {
	public boolean isClientApproved() {
		return this.server.isClientApproved(this.client);
	}

	public void sendPacket(Packet<ClientPacketHandler> packet) {
		this.server.sendPacket(this.client, packet);
	}
}
