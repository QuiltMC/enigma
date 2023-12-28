package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.SerializedName;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.network.EnigmaServer;

public final class NetConfig extends ReflectiveConfig {
	@SerializedName("username")
	@Comment("Your username for multiplayer mapping. Defaults to the system username.")
	public final TrackedValue<String> username = this.value(System.getProperty("user.name", "user"));
	@SerializedName("password")
	public final TrackedValue<String> password = this.value("");
	@SerializedName("remote_address")
	public final TrackedValue<String> remoteAddress = this.value("");
	@SerializedName("server_password")
	public final TrackedValue<String> serverPassword = this.value("");
	@SerializedName("server_port")
	@Comment("The network port of this server. Interesting fact! The default was decided pretty much at random in the Fabric discord: https://discordapp.com/channels/507304429255393322/566418023372816394/700292322918793347 (server: https://fabricmc.net/discuss/). You can still blame 2xsaiko if it conflicts with anything.")
	public final TrackedValue<Integer> serverPort = this.value(EnigmaServer.DEFAULT_PORT);
}
