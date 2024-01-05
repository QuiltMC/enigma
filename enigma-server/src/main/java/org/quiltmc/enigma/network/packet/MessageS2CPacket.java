package org.quiltmc.enigma.network.packet;

import org.quiltmc.enigma.network.ClientPacketHandler;
import org.quiltmc.enigma.network.ServerMessage;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record MessageS2CPacket(ServerMessage message) implements Packet<ClientPacketHandler> {
	@Deprecated
	MessageS2CPacket() {
		this(ServerMessage.chat("foo", "bar"));
	}

	public MessageS2CPacket(DataInput input) throws IOException {
		this(ServerMessage.read(input));
	}

	@Override
	public void read(DataInput input) throws IOException {
	}

	@Override
	public void write(DataOutput output) throws IOException {
		this.message.write(output);
	}

	@Override
	public void handle(ClientPacketHandler handler) {
		handler.addMessage(this.message);
	}
}
