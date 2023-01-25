package cuchaz.enigma.network.packet;

import cuchaz.enigma.network.ServerPacketHandler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ConfirmChangeC2SPacket implements Packet<ServerPacketHandler> {
	private int syncId;

	ConfirmChangeC2SPacket() {
	}

	public ConfirmChangeC2SPacket(int syncId) {
		this.syncId = syncId;
	}

	@Override
	public void read(DataInput input) throws IOException {
		this.syncId = input.readUnsignedShort();
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeShort(this.syncId);
	}

	@Override
	public void handle(ServerPacketHandler handler) {
		handler.server().confirmChange(handler.client(), this.syncId);
	}
}
