package org.quiltmc.enigma.api.service;

public interface EnigmaServiceFactory<T extends EnigmaService> {
	T create(EnigmaServiceContext<T> ctx);
}
