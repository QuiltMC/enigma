package org.quiltmc.enigma.network.packet;

import org.quiltmc.enigma.network.ClientPacketHandler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserListS2CPacket implements Packet<ClientPacketHandler> {
	private final List<String> users;

	public UserListS2CPacket(List<String> users) {
		this.users = users;
	}

	public UserListS2CPacket(DataInput input) throws IOException {
		int len = input.readUnsignedShort();
		this.users = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			this.users.add(PacketHelper.readString(input));
		}
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeShort(this.users.size());
		for (String user : this.users) {
			PacketHelper.writeString(output, user);
		}
	}

	@Override
	public void handle(ClientPacketHandler handler) {
		handler.updateUserList(this.users);
	}
}
