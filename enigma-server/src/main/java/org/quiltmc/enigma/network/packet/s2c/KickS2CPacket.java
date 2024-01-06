package org.quiltmc.enigma.network.packet.s2c;

import org.quiltmc.enigma.network.ClientPacketHandler;
import org.quiltmc.enigma.network.packet.Packet;
import org.quiltmc.enigma.network.packet.PacketHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record KickS2CPacket(String reason) implements Packet<ClientPacketHandler> {
	public KickS2CPacket(DataInput input) throws IOException {
		this(PacketHelper.readString(input));
	}

	@Override
	public void write(DataOutput output) throws IOException {
		PacketHelper.writeString(output, this.reason);
	}

	@Override
	public void handle(ClientPacketHandler handler) {
		handler.disconnectIfConnected(this.reason);
	}
}
