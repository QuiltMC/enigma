package org.quiltmc.enigma.api.service;

import org.quiltmc.enigma.api.translation.representation.entry.Entry;

/**
 * An obfuscation test service allows a plugin to override the deobfuscation status of an entry.
 * <br>
 * Obfuscation test services are not active by default, and need to be specified in the {@link org.quiltmc.enigma.api.EnigmaProfile profile}.
 */
public interface ObfuscationTestService extends EnigmaService {
	EnigmaServiceType<ObfuscationTestService> TYPE = EnigmaServiceType.create("obfuscation_test");

	boolean testDeobfuscated(Entry<?> entry);
}
