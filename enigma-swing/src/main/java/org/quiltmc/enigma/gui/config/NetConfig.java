package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.network.EnigmaServer;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public final class NetConfig extends ReflectiveConfig {
	@Comment("Your username for multiplayer mapping. Defaults to the system username.")
	public final TrackedValue<String> username = this.value(System.getProperty("user.name", "user"));
	public final TrackedValue<String> password = this.value("");
	public final TrackedValue<String> remoteAddress = this.value("");
	public final TrackedValue<String> serverPassword = this.value("");
	@Comment("The network port of this server. Interesting fact! The default was decided pretty much at random in the Fabric discord: https://discordapp.com/channels/507304429255393322/566418023372816394/700292322918793347 (server: https://fabricmc.net/discuss/). You can still blame 2xsaiko if it conflicts with anything.")
	public final TrackedValue<Integer> serverPort = this.value(EnigmaServer.DEFAULT_PORT);
}
