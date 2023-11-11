package org.quiltmc.enigma.network.packet;

import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.network.ClientPacketHandler;
import org.quiltmc.enigma.network.EnigmaServer;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class SyncMappingsS2CPacket implements Packet<ClientPacketHandler> {
	private EntryTree<EntryMapping> mappings;

	SyncMappingsS2CPacket() {
	}

	public SyncMappingsS2CPacket(EntryTree<EntryMapping> mappings) {
		this.mappings = mappings;
	}

	@Override
	public void read(DataInput input) throws IOException {
		this.mappings = new HashEntryTree<>();
		int size = input.readInt();
		for (int i = 0; i < size; i++) {
			this.readEntryTreeNode(input, null);
		}
	}

	private void readEntryTreeNode(DataInput input, Entry<?> parent) throws IOException {
		Entry<?> entry = PacketHelper.readEntry(input, parent, false);
		String name = PacketHelper.readString(input);
		String javadoc = PacketHelper.readString(input);
		TokenType tokenType = TokenType.values()[input.readUnsignedShort()];
		String sourcePluginId = PacketHelper.readString(input);

		EntryMapping mapping = new EntryMapping(!name.isEmpty() ? name : null, !javadoc.isEmpty() ? javadoc : null, tokenType, !sourcePluginId.isEmpty() ? sourcePluginId : null);
		this.mappings.insert(entry, mapping);
		int size = input.readUnsignedShort();
		for (int i = 0; i < size; i++) {
			this.readEntryTreeNode(input, entry);
		}
	}

	@Override
	public void write(DataOutput output) throws IOException {
		List<EntryTreeNode<EntryMapping>> roots = this.mappings.getRootNodes().toList();
		output.writeInt(roots.size());
		for (EntryTreeNode<EntryMapping> node : roots) {
			writeEntryTreeNode(output, node);
		}
	}

	private static void writeEntryTreeNode(DataOutput output, EntryTreeNode<EntryMapping> node) throws IOException {
		PacketHelper.writeEntry(output, node.getEntry(), false);
		EntryMapping value = node.getValue();
		if (value == null) value = EntryMapping.DEFAULT;

		PacketHelper.writeString(output, value.targetName() != null ? value.targetName() : "");
		PacketHelper.writeString(output, value.javadoc() != null ? value.javadoc() : "");
		output.writeShort(value.tokenType().ordinal());
		PacketHelper.writeString(output, value.sourcePluginId() != null ? value.sourcePluginId() : "");

		Collection<? extends EntryTreeNode<EntryMapping>> children = node.getChildNodes();
		output.writeShort(children.size());
		for (EntryTreeNode<EntryMapping> child : children) {
			writeEntryTreeNode(output, child);
		}
	}

	@Override
	public void handle(ClientPacketHandler controller) {
		controller.openMappings(this.mappings);
		controller.sendPacket(new ConfirmChangeC2SPacket(EnigmaServer.DUMMY_SYNC_ID));
	}
}
