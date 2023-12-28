package org.quiltmc.enigma.api.service;

import org.quiltmc.enigma.api.translation.representation.entry.Entry;

public interface ObfuscationTestService extends EnigmaService {
	EnigmaServiceType<ObfuscationTestService> TYPE = EnigmaServiceType.create("obfuscation_test");

	boolean testDeobfuscated(Entry<?> entry);
}
