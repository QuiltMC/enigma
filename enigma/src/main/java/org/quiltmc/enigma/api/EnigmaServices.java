package org.quiltmc.enigma.api;

import com.google.common.collect.ImmutableListMultimap;
import org.quiltmc.enigma.api.service.EnigmaService;
import org.quiltmc.enigma.api.service.EnigmaServiceType;

import java.util.List;

public final class EnigmaServices {
	private final ImmutableListMultimap<EnigmaServiceType<?>, EnigmaService> services;

	EnigmaServices(ImmutableListMultimap<EnigmaServiceType<?>, EnigmaService> services) {
		this.services = services;
	}

	@SuppressWarnings("unchecked")
	public <T extends EnigmaService> List<T> get(EnigmaServiceType<T> type) {
		return (List<T>) this.services.get(type);
	}
}
