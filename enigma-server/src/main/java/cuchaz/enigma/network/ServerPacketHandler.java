package cuchaz.enigma.network;

import java.net.Socket;

public record ServerPacketHandler(Socket client, EnigmaServer server) {
}
