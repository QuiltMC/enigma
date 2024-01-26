package org.quiltmc.enigma.gui.network;

import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.network.EnigmaServer;

import javax.swing.SwingUtilities;

public class IntegratedEnigmaServer extends EnigmaServer {
	public IntegratedEnigmaServer(byte[] jarChecksum, char[] password, EntryRemapper mappings, int port) {
		super(jarChecksum, password, mappings, port);
	}

	@Override
	protected void runOnThread(Runnable task) {
		SwingUtilities.invokeLater(task);
	}
}
