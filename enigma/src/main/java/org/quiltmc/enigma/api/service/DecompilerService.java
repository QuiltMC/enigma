package org.quiltmc.enigma.api.service;

import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.source.Decompiler;
import org.quiltmc.enigma.api.source.SourceSettings;

/**
 * Decompiler services provide implementations of {@link Decompiler} in order to convert bytecode into human-readable source code.
 * <br>
 * Decompiler services are active by default, and as such do not need to be specified in the {@link org.quiltmc.enigma.api.EnigmaProfile profile}.
 */
public interface DecompilerService extends EnigmaService {
	EnigmaServiceType<DecompilerService> TYPE = new EnigmaServiceType<>("decompiler", true);

	Decompiler create(ClassProvider classProvider, SourceSettings settings);
}
