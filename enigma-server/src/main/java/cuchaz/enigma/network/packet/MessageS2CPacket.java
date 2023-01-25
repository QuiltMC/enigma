package cuchaz.enigma.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import cuchaz.enigma.network.ClientPacketHandler;
import cuchaz.enigma.network.Message;

public class MessageS2CPacket implements Packet<ClientPacketHandler> {
	private Message message;

	MessageS2CPacket() {
	}

	public MessageS2CPacket(Message message) {
		this.message = message;
	}

	@Override
	public void read(DataInput input) throws IOException {
		this.message = Message.read(input);
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
