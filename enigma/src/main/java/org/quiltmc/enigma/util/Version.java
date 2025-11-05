package org.quiltmc.enigma.util;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.Comparator;

public record Version(int major, int minor, int patch) implements Comparable<Version> {
	private static final Comparator<Version> COMPARATOR = Comparator
			.comparing(Version::major, Integer::compareTo)
			.thenComparing(Version::minor, Integer::compareTo)
			.thenComparing(Version::patch, Integer::compareTo);

	private static void validateNonNegative(int value, String name) {
		Preconditions.checkArgument(value >= 0, "%s (%s) must not be negative!".formatted(name, value));
	}

	public Version {
		validateNonNegative(major, "major");
		validateNonNegative(minor, "minor");
		validateNonNegative(patch, "patch");
	}

	@Override
	public int compareTo(@Nonnull Version other) {
		return COMPARATOR.compare(this, other);
	}
}
