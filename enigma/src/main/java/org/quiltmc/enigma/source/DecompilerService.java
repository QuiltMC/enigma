package org.quiltmc.enigma.source;

import org.quiltmc.enigma.classprovider.ClassProvider;
import org.quiltmc.enigma.api.service.EnigmaService;
import org.quiltmc.enigma.api.service.EnigmaServiceType;

public interface DecompilerService extends EnigmaService {
	EnigmaServiceType<DecompilerService> TYPE = EnigmaServiceType.create("decompiler");

	Decompiler create(ClassProvider classProvider, SourceSettings settings);
}
