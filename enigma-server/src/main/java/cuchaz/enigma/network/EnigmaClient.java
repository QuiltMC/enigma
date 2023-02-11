package cuchaz.enigma.network;

import cuchaz.enigma.network.packet.Packet;
import cuchaz.enigma.network.packet.PacketRegistry;
import org.tinylog.Logger;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class EnigmaClient {
	private final ClientPacketHandler controller;

	private final String ip;
	private final int port;
	private Socket socket;
	private DataOutput output;

	public EnigmaClient(ClientPacketHandler controller, String ip, int port) {
		this.controller = controller;
		this.ip = ip;
		this.port = port;
	}

	public void connect() throws IOException {
		this.socket = new Socket(this.ip, this.port);
		this.output = new DataOutputStream(this.socket.getOutputStream());
		Thread thread = new Thread(() -> {
			try {
				DataInput input = new DataInputStream(this.socket.getInputStream());
				while (true) {
					int packetId;
					try {
						packetId = input.readUnsignedByte();
					} catch (EOFException | SocketException e) {
						break;
					}
					Packet<ClientPacketHandler> packet = PacketRegistry.createS2CPacket(packetId);
					if (packet == null) {
						throw new IOException("Received invalid packet id " + packetId);
					}
					packet.read(input);
					SwingUtilities.invokeLater(() -> packet.handle(this.controller));
				}
			} catch (IOException e) {
				this.controller.disconnectIfConnected(e.toString());
				return;
			}
			this.controller.disconnectIfConnected("Disconnected");
		});
		thread.setName("Client I/O thread");
		thread.setDaemon(true);
		thread.start();
	}

	public synchronized void disconnect() {
		if (this.socket != null && !this.socket.isClosed()) {
			try {
				this.socket.close();
			} catch (IOException e) {
				Logger.error(e, "Failed to close socket!");
			}
		}
	}


	public void sendPacket(Packet<ServerPacketHandler> packet) {
		try {
			this.output.writeByte(PacketRegistry.getC2SId(packet));
			packet.write(this.output);
		} catch (IOException e) {
			this.controller.disconnectIfConnected(e.toString());
		}
	}
}
