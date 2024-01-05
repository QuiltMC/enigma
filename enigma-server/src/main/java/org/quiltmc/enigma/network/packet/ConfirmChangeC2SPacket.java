package org.quiltmc.enigma.network.packet;

import org.quiltmc.enigma.network.ServerPacketHandler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record ConfirmChangeC2SPacket(int syncId) implements Packet<ServerPacketHandler> {
	@Deprecated
	ConfirmChangeC2SPacket() {
		this(-1);
	}

	public ConfirmChangeC2SPacket(DataInput input) throws IOException {
		this(input.readUnsignedShort());
	}

	@Override
	public void read(DataInput input) throws IOException {
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
