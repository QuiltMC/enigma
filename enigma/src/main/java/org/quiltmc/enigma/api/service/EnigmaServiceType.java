package org.quiltmc.enigma.api.service;

public record EnigmaServiceType<T extends EnigmaService>(
		String key,
		boolean activeByDefault
) {
	public static <T extends EnigmaService> EnigmaServiceType<T> create(String key, boolean activeByDefault) {
		return new EnigmaServiceType<>(key, activeByDefault);
	}

	public static <T extends EnigmaService> EnigmaServiceType<T> create(String key) {
		return new EnigmaServiceType<>(key, false);
	}

	/**
	 * The unique key of this service type.
	 */
	@Override
	public String key() {
		return this.key;
	}

	/**
	 * Whether this service type is active by default.
	 * If {@code true}, this service type will be active without being explicitly enabled in the profile.
	 */
	@Override
	public boolean activeByDefault() {
		return this.activeByDefault;
	}

	@Override
	public String toString() {
		return this.key;
	}

	@Override
	public int hashCode() {
		return this.key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;

		if (obj instanceof EnigmaServiceType<?> serviceType) {
			return serviceType.key.equals(this.key);
		}

		return false;
	}
}
