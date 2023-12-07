package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.SerializedName;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.network.EnigmaServer;

public final class NetConfig extends ReflectiveConfig {
	@SerializedName("username")
	public final TrackedValue<String> username = this.value(System.getProperty("user.name", "user"));
	@SerializedName("password")
	public final TrackedValue<String> password = this.value("");
	@SerializedName("remote_address")
	public final TrackedValue<String> remoteAddress = this.value("");
	@SerializedName("server_password")
	public final TrackedValue<String> serverPassword = this.value("");
	@SerializedName("server_port")
	public final TrackedValue<Integer> serverPort = this.value(EnigmaServer.DEFAULT_PORT);
}
