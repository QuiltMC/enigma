package org.quiltmc.enigma.network.packet;

import org.quiltmc.enigma.network.ClientPacketHandler;
import org.quiltmc.enigma.network.ServerMessage;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class MessageS2CPacket implements Packet<ClientPacketHandler> {
	private ServerMessage message;

	MessageS2CPacket() {
	}

	public MessageS2CPacket(ServerMessage message) {
		this.message = message;
	}

	@Override
	public void read(DataInput input) throws IOException {
		this.message = ServerMessage.read(input);
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