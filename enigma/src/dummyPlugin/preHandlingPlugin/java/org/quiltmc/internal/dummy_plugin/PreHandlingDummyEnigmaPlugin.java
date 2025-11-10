package org.quiltmc.internal.dummy_plugin;

import org.quiltmc.enigma.api.EnigmaPluginContext;
import org.quiltmc.enigma.api.EnigmaPlugin;

/**
 * A dummy plugin to be compiled against a (copied) pre-error-handling version of Enigma and run against
 * a post-error-handling version.
 */
public class PreHandlingDummyEnigmaPlugin implements EnigmaPlugin {
	@Override
	public void init(EnigmaPluginContext ctx) {
		throw new UnsupportedOperationException(PreHandlingDummyEnigmaPlugin.class.getSimpleName() + " initialized!");
	}
}
