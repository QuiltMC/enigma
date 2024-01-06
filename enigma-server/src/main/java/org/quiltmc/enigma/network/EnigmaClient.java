package org.quiltmc.enigma.network;

import org.quiltmc.enigma.network.packet.Packet;
import org.quiltmc.enigma.network.packet.PacketRegistry;
import org.tinylog.Logger;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public abstract class EnigmaClient {
	protected boolean logPackets = false;

	private final ClientPacketHandler handler;

	private final String ip;
	private final int port;
	private Socket socket;
	private DataOutput output;

	public EnigmaClient(ClientPacketHandler handler, String ip, int port) {
		this.handler = handler;
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

					Packet<ClientPacketHandler> packet = PacketRegistry.readS2CPacket(packetId, input);
					if (packet == null) {
						throw new IOException("Received invalid packet id " + packetId);
					}

					if (this.logPackets) {
						Logger.info("Received packet {} (id {})", packet, packetId);
					}

					this.runOnThread(() -> packet.handle(this.handler));
				}
			} catch (IOException e) {
				this.handler.disconnectIfConnected(e.toString());
			}
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

			if (this.logPackets) {
				Logger.info("Sent packet {}", packet);
			}
		} catch (IOException e) {
			this.handler.disconnectIfConnected(e.toString());
		}
	}

	protected abstract void runOnThread(Runnable task);
}
