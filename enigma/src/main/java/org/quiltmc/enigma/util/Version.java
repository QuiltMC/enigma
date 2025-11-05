package org.quiltmc.enigma.util;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.Comparator;

public record Version(int major, int minor, int patch) implements Comparable<Version> {
	public static final Comparator<Version> MAJOR_COMPARATOR = Comparator
			.comparing(Version::major, Integer::compareTo);
	public static final Comparator<Version> MINOR_COMPARATOR = Comparator
			.comparing(Version::minor, Integer::compareTo);
	public static final Comparator<Version> PATCH_COMPARATOR = Comparator
			.comparing(Version::patch, Integer::compareTo);

	public static final Comparator<Version> MAJOR_MINOR_COMPARATOR = MAJOR_COMPARATOR
			.thenComparing(MINOR_COMPARATOR);

	public static final Comparator<Version> COMPLETE_COMPARATOR = MAJOR_MINOR_COMPARATOR
			.thenComparing(PATCH_COMPARATOR);

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
		return COMPLETE_COMPARATOR.compare(this, other);
	}
}
