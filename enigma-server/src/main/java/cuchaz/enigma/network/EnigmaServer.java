package cuchaz.enigma.network;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import cuchaz.enigma.network.packet.*;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.Entry;
import org.tinylog.Logger;

public abstract class EnigmaServer {
	// https://discordapp.com/channels/507304429255393322/566418023372816394/700292322918793347
	public static final int DEFAULT_PORT = 34712;
	public static final int PROTOCOL_VERSION = 1;
	public static final int CHECKSUM_SIZE = 20;
	public static final int MAX_PASSWORD_LENGTH = 255; // length is written as a byte in the login packet

	private final int port;
	private ServerSocket socket;
	private final List<Socket> clients = new CopyOnWriteArrayList<>();
	private final Map<Socket, String> usernames = new HashMap<>();
	private final Set<Socket> unapprovedClients = new HashSet<>();

	private final byte[] jarChecksum;
	private final char[] password;

	public static final int DUMMY_SYNC_ID = 0;
	private final EntryRemapper mappings;
	private final Map<Entry<?>, Integer> syncIds = new HashMap<>();
	private final Map<Integer, Entry<?>> inverseSyncIds = new HashMap<>();
	private final Map<Integer, Set<Socket>> clientsNeedingConfirmation = new HashMap<>();
	private int nextSyncId = DUMMY_SYNC_ID + 1;

	private static int nextIoId = 0;

	public EnigmaServer(byte[] jarChecksum, char[] password, EntryRemapper mappings, int port) {
		this.jarChecksum = jarChecksum;
		this.password = password;
		this.mappings = mappings;
		this.port = port;
	}

	public void start() throws IOException {
		this.socket = new ServerSocket(this.port);
		this.log("Server started on " + this.socket.getInetAddress() + ":" + this.port);
		Thread thread = new Thread(() -> {
			try {
				while (!this.socket.isClosed()) {
					this.acceptClient();
				}
			} catch (SocketException e) {
				Logger.info("Server closed");
			} catch (IOException e) {
				Logger.error("Failed to accept client!", e);
			}
		});
		thread.setName("Server client listener");
		thread.setDaemon(true);
		thread.start();
	}

	private void acceptClient() throws IOException {
		Socket client = this.socket.accept();
		this.clients.add(client);
		Thread thread = new Thread(() -> {
			try {
				DataInput input = new DataInputStream(client.getInputStream());
				while (true) {
					int packetId;
					try {
						packetId = input.readUnsignedByte();
					} catch (EOFException | SocketException e) {
						break;
					}
					Packet<ServerPacketHandler> packet = PacketRegistry.createC2SPacket(packetId);
					if (packet == null) {
						throw new IOException("Received invalid packet id " + packetId);
					}
					packet.read(input);
					this.runOnThread(() -> packet.handle(new ServerPacketHandler(client, this)));
				}
			} catch (IOException e) {
				this.kick(client, e.toString());
				Logger.error("Failed to read packet from client!", e);
				return;
			}

			this.kick(client, "disconnect.disconnected");
		});
		thread.setName("Server I/O thread #" + (nextIoId++));
		thread.setDaemon(true);
		thread.start();
	}

	public void stop() {
		this.runOnThread(() -> {
			if (this.socket != null && !this.socket.isClosed()) {
				for (Socket client : this.clients) {
					this.kick(client, "disconnect.server_closed");
				}
				try {
					this.socket.close();
				} catch (IOException e) {
					Logger.error(e, "Failed to close server socket!");
				}
			}
		});
	}

	public void kick(Socket client, String reason) {
		if (!this.clients.remove(client)) {
			return;
		}

		this.sendPacket(client, new KickS2CPacket(reason));

		this.clientsNeedingConfirmation.values().removeIf(list -> {
			list.remove(client);
			return list.isEmpty();
		});
		String username = this.usernames.remove(client);
		try {
			client.close();
		} catch (IOException e) {
			Logger.error("Failed to close server client socket!", e);
		}

		if (username != null) {
			Logger.info("Kicked " + username + " because " + reason);
			this.sendMessage(ServerMessage.disconnect(username));
		}
		this.sendUsernamePacket();
	}

	public boolean isUsernameTaken(String username) {
		return this.usernames.containsValue(username);
	}

	public void setUsername(Socket client, String username) {
		this.usernames.put(client, username);
		this.sendUsernamePacket();
	}

	private void sendUsernamePacket() {
		List<String> usernames = new ArrayList<>(this.usernames.values());
		Collections.sort(usernames);
		this.sendToAll(new UserListS2CPacket(usernames));
	}

	public String getUsername(Socket client) {
		return this.usernames.get(client);
	}

	public void sendPacket(Socket client, Packet<ClientPacketHandler> packet) {
		if (!client.isClosed()) {
			int packetId = PacketRegistry.getS2CId(packet);
			try {
				DataOutput output = new DataOutputStream(client.getOutputStream());
				output.writeByte(packetId);
				packet.write(output);
			} catch (IOException e) {
				if (!(packet instanceof KickS2CPacket)) {
					this.kick(client, e.toString());
					Logger.error("Failed to send packet to client!", e);
				}
			}
		}
	}

	public void sendToAll(Packet<ClientPacketHandler> packet) {
		for (Socket client : this.clients) {
			this.sendPacket(client, packet);
		}
	}

	public void sendToAllExcept(Socket excluded, Packet<ClientPacketHandler> packet) {
		for (Socket client : this.clients) {
			if (client != excluded) {
				this.sendPacket(client, packet);
			}
		}
	}

	public boolean canModifyEntry(Socket client, Entry<?> entry) {
		if (this.unapprovedClients.contains(client)) {
			return false;
		}

		Integer syncId = this.syncIds.get(entry);
		if (syncId == null) {
			return true;
		}
		Set<Socket> clients = this.clientsNeedingConfirmation.get(syncId);
		return clients == null || !clients.contains(client);
	}

	public int lockEntry(Socket exception, Entry<?> entry) {
		int syncId = this.nextSyncId;
		this.nextSyncId++;
		// sync id is sent as an unsigned short, can't have more than 65536
		if (this.nextSyncId == 65536) {
			this.nextSyncId = DUMMY_SYNC_ID + 1;
		}
		Integer oldSyncId = this.syncIds.get(entry);
		if (oldSyncId != null) {
			this.clientsNeedingConfirmation.remove(oldSyncId);
		}
		this.syncIds.put(entry, syncId);
		this.inverseSyncIds.put(syncId, entry);
		Set<Socket> clients = new HashSet<>(this.clients);
		clients.remove(exception);
		this.clientsNeedingConfirmation.put(syncId, clients);
		return syncId;
	}

	public void confirmChange(Socket client, int syncId) {
		if (this.usernames.containsKey(client)) {
			this.unapprovedClients.remove(client);
		}

		Set<Socket> clients = this.clientsNeedingConfirmation.get(syncId);
		if (clients != null) {
			clients.remove(client);
			if (clients.isEmpty()) {
				this.clientsNeedingConfirmation.remove(syncId);
				this.syncIds.remove(this.inverseSyncIds.remove(syncId));
			}
		}
	}

	public void sendCorrectMapping(Socket client, Entry<?> entry, boolean refreshClassTree) {
		EntryMapping oldMapping = this.mappings.getDeobfMapping(entry);
		String oldName = oldMapping.targetName();
		if (oldName == null) {
			this.sendPacket(client, new EntryChangeS2CPacket(DUMMY_SYNC_ID, EntryChange.modify(entry).clearDeobfName()));
		} else {
			this.sendPacket(client, new EntryChangeS2CPacket(0, EntryChange.modify(entry).withDeobfName(oldName)));
		}
	}

	protected abstract void runOnThread(Runnable task);

	public void log(String message) {
		Logger.info("[server] {}", message);
	}

	protected boolean isRunning() {
		return !this.socket.isClosed();
	}

	public byte[] getJarChecksum() {
		return this.jarChecksum;
	}

	public char[] getPassword() {
		return this.password;
	}

	public EntryRemapper getMappings() {
		return this.mappings;
	}

	public void sendMessage(ServerMessage message) {
		Logger.info("[chat] {}", message.translate());
		this.sendToAll(new MessageS2CPacket(message));
	}
}
