package org.quiltmc.enigma.api.service;

import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.source.Decompiler;
import org.quiltmc.enigma.api.source.SourceSettings;

public interface DecompilerService extends EnigmaService {
	EnigmaServiceType<DecompilerService> TYPE = new EnigmaServiceType<>("decompiler", false);

	Decompiler create(ClassProvider classProvider, SourceSettings settings);
}
