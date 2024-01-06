package org.quiltmc.enigma.network.packet.s2c;

import org.quiltmc.enigma.network.ClientPacketHandler;
import org.quiltmc.enigma.network.ServerMessage;
import org.quiltmc.enigma.network.packet.Packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record MessageS2CPacket(ServerMessage message) implements Packet<ClientPacketHandler> {
	public MessageS2CPacket(DataInput input) throws IOException {
		this(ServerMessage.read(input));
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
