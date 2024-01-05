package org.quiltmc.enigma.network.packet;

import org.quiltmc.enigma.network.ServerPacketHandler;
import org.quiltmc.enigma.network.ServerMessage;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record MessageC2SPacket(String message) implements Packet<ServerPacketHandler> {
	@Deprecated
	MessageC2SPacket() {
		this("foo");
	}

	public MessageC2SPacket(String message) {
		this.message = message;
	}

	public MessageC2SPacket(DataInput input) throws IOException {
		this(PacketHelper.readString(input));
	}

	@Override
	public void read(DataInput input) throws IOException {
	}

	@Override
	public void write(DataOutput output) throws IOException {
		PacketHelper.writeString(output, this.message);
	}

	@Override
	public void handle(ServerPacketHandler handler) {
		String trimmedMessage = this.message.trim();
		if (!trimmedMessage.isEmpty()) {
			handler.server().sendMessage(ServerMessage.chat(handler.server().getUsername(handler.client()), trimmedMessage));
		}
	}
}
