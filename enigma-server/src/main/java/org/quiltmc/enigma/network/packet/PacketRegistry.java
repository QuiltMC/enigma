package org.quiltmc.enigma.network.packet;

import org.quiltmc.enigma.network.ClientPacketHandler;
import org.quiltmc.enigma.network.ServerPacketHandler;

import java.io.DataInput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PacketRegistry {
	private static final Map<Class<? extends Packet<ServerPacketHandler>>, Integer> C2S_PACKET_IDS = new HashMap<>();
	private static final Map<Integer, DataFunction<? extends Packet<ServerPacketHandler>>> C2S_PACKET_READERS = new HashMap<>();
	private static final Map<Class<? extends Packet<ClientPacketHandler>>, Integer> S2C_PACKET_IDS = new HashMap<>();
	private static final Map<Integer, DataFunction<? extends Packet<ClientPacketHandler>>> S2C_PACKET_READERS = new HashMap<>();

	private static <T extends Packet<ServerPacketHandler>> void registerC2S(int id, Class<T> clazz, DataFunction<T> reader) {
		C2S_PACKET_IDS.put(clazz, id);
		C2S_PACKET_READERS.put(id, reader);
	}

	private static <T extends Packet<ClientPacketHandler>> void registerS2C(int id, Class<T> clazz, DataFunction<T> reader) {
		S2C_PACKET_IDS.put(clazz, id);
		S2C_PACKET_READERS.put(id, reader);
	}

	static {
		registerC2S(0, LoginC2SPacket.class, LoginC2SPacket::new);
		registerC2S(1, ConfirmChangeC2SPacket.class, ConfirmChangeC2SPacket::new);
		registerC2S(6, MessageC2SPacket.class, MessageC2SPacket::new);
		registerC2S(7, EntryChangeC2SPacket.class, EntryChangeC2SPacket::new);

		registerS2C(0, KickS2CPacket.class, KickS2CPacket::new);
		registerS2C(1, SyncMappingsS2CPacket.class, SyncMappingsS2CPacket::new);
		registerS2C(6, MessageS2CPacket.class, MessageS2CPacket::new);
		registerS2C(7, UserListS2CPacket.class, UserListS2CPacket::new);
		registerS2C(8, EntryChangeS2CPacket.class, EntryChangeS2CPacket::new);
	}

	public static int getC2SId(Packet<ServerPacketHandler> packet) {
		return C2S_PACKET_IDS.get(packet.getClass());
	}

	public static Packet<ServerPacketHandler> readC2SPacket(int id, DataInput input) throws IOException {
		DataFunction<? extends Packet<ServerPacketHandler>> reader = C2S_PACKET_READERS.get(id);
		return reader == null ? null : reader.apply(input);
	}

	public static int getS2CId(Packet<ClientPacketHandler> packet) {
		return S2C_PACKET_IDS.get(packet.getClass());
	}

	public static Packet<ClientPacketHandler> readS2CPacket(int id, DataInput input) throws IOException {
		DataFunction<? extends Packet<ClientPacketHandler>> reader = S2C_PACKET_READERS.get(id);
		return reader == null ? null : reader.apply(input);
	}

	@FunctionalInterface
	private interface DataFunction<R> {
		R apply(DataInput input) throws IOException;
	}
}
