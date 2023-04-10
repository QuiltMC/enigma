package cuchaz.enigma;

import com.google.common.collect.ImmutableListMultimap;
import cuchaz.enigma.api.service.EnigmaService;
import cuchaz.enigma.api.service.EnigmaServiceType;

import java.util.List;

public final class EnigmaServices {
	private final ImmutableListMultimap<EnigmaServiceType<?>, RegisteredService<? extends EnigmaService>> services;

	EnigmaServices(ImmutableListMultimap<EnigmaServiceType<?>, RegisteredService<? extends EnigmaService>> services) {
		this.services = services;
	}

	public <T extends EnigmaService> List<T> get(EnigmaServiceType<T> type) {
		List<RegisteredService<T>> withIds = this.getWithIds(type);
		return withIds.stream().map(RegisteredService::service).toList();
	}

	@SuppressWarnings("unchecked")
	public <T extends EnigmaService> List<RegisteredService<T>> getWithIds(EnigmaServiceType<T> type) {
		return (List<RegisteredService<T>>) (Object) this.services.get(type);
	}

	public record RegisteredService<T extends EnigmaService>(String id, T service) {
	}
}
