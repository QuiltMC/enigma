package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.values.ComplexConfigValue;
import org.quiltmc.config.api.values.ConfigSerializableObject;

public record BoundedNumber<N extends Number & Comparable<N>>(N value, N min, N max)
		implements ConfigSerializableObject<N> {
	public BoundedNumber(N value, N min, N max) {
		if (min.compareTo(max) >= 0) {
			throw new IllegalArgumentException("min must be less than max!");
		}

		this.min = min;
		this.max = max;
		if (this.min.compareTo(value) > 0) {
			this.value = min;
		} else if (this.max.compareTo(value) < 0) {
			this.value = max;
		} else {
			this.value = value;
		}
	}

	@Override
	public BoundedNumber<N> convertFrom(N representation) {
		return new BoundedNumber<>(representation, this.min, this.max);
	}

	@Override
	public N getRepresentation() {
		return this.value;
	}

	@Override
	public ComplexConfigValue copy() {
		return this;
	}

	@Override
	public String toString() {
		return "%s (min: %s, max: %s)".formatted(this.value, this.min, this.max);
	}
}
