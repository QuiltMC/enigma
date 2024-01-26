package org.quiltmc.enigma.network.packet.c2s;

import org.quiltmc.enigma.network.ServerPacketHandler;
import org.quiltmc.enigma.network.packet.Packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record ConfirmChangeC2SPacket(int syncId) implements Packet<ServerPacketHandler> {
	public ConfirmChangeC2SPacket(DataInput input) throws IOException {
		this(input.readUnsignedShort());
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
