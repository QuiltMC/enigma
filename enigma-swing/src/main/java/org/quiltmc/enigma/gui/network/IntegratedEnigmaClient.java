package org.quiltmc.enigma.gui.network;

import org.quiltmc.enigma.network.ClientPacketHandler;
import org.quiltmc.enigma.network.EnigmaClient;

import javax.swing.SwingUtilities;

public class IntegratedEnigmaClient extends EnigmaClient {
	public static boolean LOG_PACKETS = false;

	public IntegratedEnigmaClient(ClientPacketHandler handler, String ip, int port) {
		super(handler, ip, port);
		this.logPackets = LOG_PACKETS;
	}

	@Override
	protected void runOnThread(Runnable task) {
		SwingUtilities.invokeLater(task);
	}
}
