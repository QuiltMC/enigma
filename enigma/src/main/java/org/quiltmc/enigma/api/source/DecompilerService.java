package org.quiltmc.enigma.api.source;

import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.service.EnigmaService;
import org.quiltmc.enigma.api.service.EnigmaServiceType;

public interface DecompilerService extends EnigmaService {
	EnigmaServiceType<DecompilerService> TYPE = EnigmaServiceType.create("decompiler");

	Decompiler create(ClassProvider classProvider, SourceSettings settings);
}
