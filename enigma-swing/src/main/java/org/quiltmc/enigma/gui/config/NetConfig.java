package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.network.EnigmaServer;

public final class NetConfig extends ReflectiveConfig.Section {
	public final TrackedValue<String> username = this.value(System.getProperty("user.name", "user"));
	public final TrackedValue<String> password = this.value("");
	public final TrackedValue<String> remoteAddress = this.value("");
	public final TrackedValue<String> serverPassword = this.value("");
	public final TrackedValue<Integer> serverPort = this.value(EnigmaServer.DEFAULT_PORT);
}
