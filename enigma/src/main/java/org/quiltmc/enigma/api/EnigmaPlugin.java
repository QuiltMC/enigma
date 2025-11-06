package org.quiltmc.enigma.api;

import org.quiltmc.enigma.util.Version;
import org.tinylog.Logger;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

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
	 * {@linkplain Enigma#MAJOR_VERSION major} and {@linkplain Enigma#MINOR_VERSION minor} parts of the Enigma version
	 * this plugin was compiled against.
	 *
	 * <p> If {@code false} is returned, an exception will be thrown during initialization. To make the error more
	 * readable, a plugin may override {@link #getName()} to return a name clearly identifying itself.
	 *
	 * <p> Custom implementations of this method may use {@link Version#compareTo(Version)} or any of {@link Version}'s
	 * static {@link Comparator}s to easily match various version ranges.
	 *
	 * @param enigmaVersion the Enigma version
	 */
	default boolean supportsEnigmaVersion(@Nonnull Version enigmaVersion) {
		final EnigmaVersionMarked versionAnnotation = this.getClass().getAnnotation(EnigmaVersionMarked.class);
		if (versionAnnotation == null) {
			Logger.error(
					"""
					Unable to determine compatibility of plugin: {}
					\tPlugin has no {} annotation and does not override supportsEnigmaVersion.
					\tIt was likely built with a pre-2.7.x version of Enigma and may cause issues.\
					""",
					this.getName(), EnigmaVersionMarked.class.getSimpleName()
			);

			return true;
		} else {
			{ // DEBUG
				System.out.println("versionAnnotation: " + versionAnnotation);
				System.out.println("enigmaVersion: " + enigmaVersion);
			}

			return versionAnnotation.major() == enigmaVersion.major()
				&& versionAnnotation.minor() == enigmaVersion.minor();
		}
	}

	/**
	 * Gets the name of the plugin; used for error reporting.
	 *
	 * <p> The default implementation simply returns {@link Object#toString()}.
	 *
	 * @return the name of this plugin
	 */
	default String getName() {
		return this.toString();
	}
}
