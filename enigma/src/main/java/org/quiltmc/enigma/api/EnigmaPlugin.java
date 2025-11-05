package org.quiltmc.enigma.api;

import org.quiltmc.enigma.util.Version;

import javax.annotation.Nonnull;

/**
 * An enigma plugin represents a collection of {@link org.quiltmc.enigma.api.service.EnigmaService services} that perform different functions.
 */
public interface EnigmaPlugin {
	/**
	 * Initializes the plugin, registering all services.
	 */
	void init(EnigmaPluginContext ctx);

	/**
	 * Returns whether this plugin supports the passed {@code enigmaVersion}.
	 *
	 * <p> The default implementation returns {@code true} if and only if the passed {@code enigmaVersion}'s
	 * {@linkplain Version#major() major} and {@linkplain Version#minor() minor} parts match the respective
	 * {@linkplain Enigma#MAJOR_VERSION major} and {@linkplain Enigma#MINOR_VERSION minor} version parts of the Enigma
	 * version this plugin was compiled against.
	 *
	 * @param enigmaVersion the Enigma version
	 */
	default boolean supportsEnigmaVersion(@Nonnull Version enigmaVersion) {
		return Enigma.MAJOR_VERSION == enigmaVersion.major()
			&& Enigma.MINOR_VERSION == enigmaVersion.minor();
	}
}
