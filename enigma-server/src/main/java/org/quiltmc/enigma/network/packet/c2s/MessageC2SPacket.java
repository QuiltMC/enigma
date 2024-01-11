package org.quiltmc.enigma.network.packet.c2s;

import org.quiltmc.enigma.network.ServerMessage;
import org.quiltmc.enigma.network.ServerPacketHandler;
import org.quiltmc.enigma.network.packet.Packet;
import org.quiltmc.enigma.network.packet.PacketHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record MessageC2SPacket(String message) implements Packet<ServerPacketHandler> {
	public MessageC2SPacket(DataInput input) throws IOException {
		this(PacketHelper.readString(input));
	}

	@Override
	public void write(DataOutput output) throws IOException {
		PacketHelper.writeString(output, this.message);
	}

	@Override
	public void handle(ServerPacketHandler handler) {
		if (!handler.isClientApproved()) {
			return;
		}

		String trimmedMessage = this.message.trim();
		if (!trimmedMessage.isEmpty()) {
			handler.server().sendMessage(ServerMessage.chat(handler.server().getUsername(handler.client()), trimmedMessage));
		}
	}
}
