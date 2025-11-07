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
	 * <p> The recommended implementation is
	 * <pre><code>
	 *     return Enigma.MAJOR_VERSION == enigmaVersion.major()
	 *         && Enigma.MINOR_VERSION == enigmaVersion.minor();
	 * </code></pre>
	 * which matches version with the same major and minor parts as the Enigma version the plugin was compiled against,
	 * but allows any patch.
	 *
	 * <p> If {@code false} is returned, an exception will be thrown during initialization. To make the error more
	 * readable, a plugin may override {@link #getName()} to return a name clearly identifying itself.
	 *
	 * <p> Custom implementations of this method may use {@link Version#compareTo(Version)} or any of {@link Version}'s
	 * static {@link Comparator}s to easily match various version ranges.
	 *
	 * @param enigmaVersion the run-time Enigma version
	 */
	boolean supportsEnigmaVersion(@Nonnull Version enigmaVersion);

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
