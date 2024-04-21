package org.quiltmc.enigma.api.service;

import org.quiltmc.enigma.api.translation.representation.entry.Entry;

public interface ObfuscationTestService extends EnigmaService {
	EnigmaServiceType<ObfuscationTestService> TYPE = new EnigmaServiceType<>("obfuscation_test", false);

	boolean testDeobfuscated(Entry<?> entry);
}
