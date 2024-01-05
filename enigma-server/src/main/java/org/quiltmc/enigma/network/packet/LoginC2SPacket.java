package org.quiltmc.enigma.network.packet;

import org.quiltmc.enigma.network.EnigmaServer;
import org.quiltmc.enigma.network.ServerPacketHandler;
import org.quiltmc.enigma.network.ServerMessage;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class LoginC2SPacket implements Packet<ServerPacketHandler> {
	private final byte[] jarChecksum;
	private final char[] password;
	private final String username;

	public LoginC2SPacket(byte[] jarChecksum, char[] password, String username) {
		this.jarChecksum = jarChecksum;
		this.password = password;
		this.username = username;
	}

	public LoginC2SPacket(DataInput input) throws IOException {
		if (input.readUnsignedShort() != EnigmaServer.PROTOCOL_VERSION) {
			throw new IOException("Mismatching protocol");
		}

		this.jarChecksum = new byte[EnigmaServer.CHECKSUM_SIZE];
		input.readFully(this.jarChecksum);
		this.password = new char[input.readUnsignedByte()];
		for (int i = 0; i < this.password.length; i++) {
			this.password[i] = input.readChar();
		}

		this.username = PacketHelper.readString(input);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeShort(EnigmaServer.PROTOCOL_VERSION);
		output.write(this.jarChecksum);
		output.writeByte(this.password.length);
		for (char c : this.password) {
			output.writeChar(c);
		}

		PacketHelper.writeString(output, this.username);
	}

	@Override
	public void handle(ServerPacketHandler handler) {
		boolean usernameTaken = handler.server().isUsernameTaken(this.username);
		handler.server().setUsername(handler.client(), this.username);
		handler.server().log(this.username + " logged in with IP " + handler.client().getInetAddress().toString() + ":" + handler.client().getPort());

		if (!Arrays.equals(this.password, handler.server().getPassword())) {
			handler.server().kick(handler.client(), "disconnect.wrong_password");
			return;
		}

		if (usernameTaken) {
			handler.server().kick(handler.client(), "disconnect.username_taken");
			return;
		}

		if (!Arrays.equals(this.jarChecksum, handler.server().getJarChecksum())) {
			handler.server().kick(handler.client(), "disconnect.wrong_jar");
			return;
		}

		handler.server().sendPacket(handler.client(), new SyncMappingsS2CPacket(handler.server().getRemapper().getDeobfMappings()));
		handler.server().sendMessage(ServerMessage.connect(this.username));
	}
}
