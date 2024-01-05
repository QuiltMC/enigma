package org.quiltmc.enigma.network.packet;

import org.quiltmc.enigma.network.ClientPacketHandler;
import org.quiltmc.enigma.api.translation.mapping.EntryChange;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record EntryChangeS2CPacket(int syncId, EntryChange<?> change) implements Packet<ClientPacketHandler> {
	public EntryChangeS2CPacket(DataInput input) throws IOException {
		this(input.readUnsignedShort(), PacketHelper.readEntryChange(input));
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeShort(this.syncId);
		PacketHelper.writeEntryChange(output, this.change);
	}

	@Override
	public void handle(ClientPacketHandler handler) {
		if (handler.applyChangeFromServer(this.change)) {
			handler.sendPacket(new ConfirmChangeC2SPacket(this.syncId));
		}
	}
}
