package cuchaz.enigma.network.packet;

import cuchaz.enigma.network.ServerMessage;
import cuchaz.enigma.network.ServerPacketHandler;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.translation.mapping.EntryUtil;
import cuchaz.enigma.utils.validation.ValidationContext;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class EntryChangeC2SPacket implements Packet<ServerPacketHandler> {
	private EntryChange<?> change;

	EntryChangeC2SPacket() {
	}

	public EntryChangeC2SPacket(EntryChange<?> change) {
		this.change = change;
	}

	@Override
	public void read(DataInput input) throws IOException {
		this.change = PacketHelper.readEntryChange(input);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		PacketHelper.writeEntryChange(output, this.change);
	}

	@Override
	public void handle(ServerPacketHandler handler) {
		ValidationContext vc = new ValidationContext(null);

		boolean valid = handler.server().canModifyEntry(handler.client(), this.change.getTarget());

		if (valid) {
			EntryUtil.applyChange(vc, handler.server().getMappings(), this.change);
			valid = vc.canProceed();
		}

		if (!valid) {
			handler.server().sendCorrectMapping(handler.client(), this.change.getTarget(), true);
			return;
		}

		int syncId = handler.server().lockEntry(handler.client(), this.change.getTarget());
		handler.server().sendToAllExcept(handler.client(), new EntryChangeS2CPacket(syncId, this.change));

		if (this.change.getDeobfName().isSet()) {
			handler.server().sendMessage(ServerMessage.rename(handler.server().getUsername(handler.client()), this.change.getTarget(), this.change.getDeobfName().getNewValue()));
		} else if (this.change.getDeobfName().isReset()) {
			handler.server().sendMessage(ServerMessage.removeMapping(handler.server().getUsername(handler.client()), this.change.getTarget()));
		}

		if (!this.change.getJavadoc().isUnchanged()) {
			handler.server().sendMessage(ServerMessage.editDocs(handler.server().getUsername(handler.client()), this.change.getTarget()));
		}
	}
}
