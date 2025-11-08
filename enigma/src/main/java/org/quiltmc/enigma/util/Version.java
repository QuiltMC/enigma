package org.quiltmc.enigma.util;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.Comparator;

/**
 * Represents a version with {@link #major}, {@link #minor}, and {@link #patch} number parts.<br>
 * Each part must be non-negative.
 *
 * <p> A version's string representation concatenates its parts and separates them with {@value SEPARATOR}.<br>
 * So {@code 1.2.3} represents a version whose {@link #major}, {@link #minor}, and {@link #patch} parts are
 * {@code 1}, {@code 2}, and {@code 3}, respectively.
 */
public record Version(int major, int minor, int patch) implements Comparable<Version> {
	private static final Comparator<Version> MAJOR_COMPARATOR = Comparator
			.comparing(Version::major, Integer::compareTo);
	private static final Comparator<Version> MINOR_COMPARATOR = Comparator
			.comparing(Version::minor, Integer::compareTo);
	private static final Comparator<Version> PATCH_COMPARATOR = Comparator
			.comparing(Version::patch, Integer::compareTo);

	private static final Comparator<Version> MAJOR_MINOR_COMPARATOR =
			MAJOR_COMPARATOR.thenComparing(MINOR_COMPARATOR);

	private static final Comparator<Version> COMPLETE_COMPARATOR =
			MAJOR_MINOR_COMPARATOR.thenComparing(PATCH_COMPARATOR);

	private static final String SEPARATOR = ".";

	private static void validateNonNegative(int value, String name) {
		Preconditions.checkArgument(value >= 0, "%s (%s) must not be negative!".formatted(name, value));
	}

	/**
	 * Constructs a new Version.
	 *
	 * @param major the major number part
	 * @param minor the minor number part
	 * @param patch the patch number part
	 *
	 * @throws IllegalArgumentException if the passed {@code major}, {@code minor}, or {@code patch} is negative
	 */
	public Version {
		validateNonNegative(major, "major");
		validateNonNegative(minor, "minor");
		validateNonNegative(patch, "patch");
	}

	/**
	 * Compares the {@link #major}, {@link #minor}, and {@link #patch} number parts, in that order.<br>
	 * This means, for example:
	 * <ul>
	 *     <li> {@code 1.8.22} comes before {@code 2.0.0}
	 *     <li> {@code 3.1.7} comes before {@code 3.2.0}
	 *     <li> {@code 7.13.3} comes before {@code 7.13.4}
	 * </ul>
	 *
	 * @return a negative number if this version comes before the passed {@code other} version,<br>
	 *         a positive number if this version comes after the passed {@code other} version,<br>
	 *         or {@code 0} if this version equals the passed {@code other} version
	 */
	@Override
	public int compareTo(@Nonnull Version other) {
		return COMPLETE_COMPARATOR.compare(this, other);
	}

	/**
	 * Compares only the {@link #major} number parts.
	 */
	public int compareMajorTo(@Nonnull Version other) {
		return MAJOR_COMPARATOR.compare(this, other);
	}

	/**
	 * Compares the {@link #major} and {@link #minor} number parts, in that order.
	 */
	public int compareMajorMinorTo(@Nonnull Version other) {
		return MAJOR_MINOR_COMPARATOR.compare(this, other);
	}

	@Override
	public String toString() {
		return this.major + SEPARATOR + this.minor + SEPARATOR + this.patch;
	}
}
