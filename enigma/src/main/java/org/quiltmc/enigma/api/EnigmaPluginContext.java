package org.quiltmc.enigma.api;

import org.quiltmc.enigma.api.service.EnigmaService;
import org.quiltmc.enigma.api.service.EnigmaServiceFactory;
import org.quiltmc.enigma.api.service.EnigmaServiceType;

public interface EnigmaPluginContext {
	<T extends EnigmaService> void registerService(String id, EnigmaServiceType<T> serviceType, EnigmaServiceFactory<T> factory);
}
