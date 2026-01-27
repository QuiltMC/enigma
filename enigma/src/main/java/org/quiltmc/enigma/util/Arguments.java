package org.quiltmc.enigma.util;

@SuppressWarnings("unused")
public final class Arguments {
	private Arguments() {
		throw new UnsupportedOperationException();
	}

	public static int requireNonNegative(int argument, String name) {
		if (argument < 0) {
			throw new IllegalArgumentException("%s (%s) must not be negative!".formatted(name, argument));
		} else {
			return argument;
		}
	}

	public static int requireNonPositive(int argument, String name) {
		if (argument > 0) {
			throw new IllegalArgumentException("%s (%s) must not be positive!".formatted(name, argument));
		} else {
			return argument;
		}
	}

	public static int requirePositive(int argument, String name) {
		if (argument <= 0) {
			throw new IllegalArgumentException("%s (%s) must be positive!".formatted(name, argument));
		} else {
			return argument;
		}
	}

	public static int requireNegative(int argument, String name) {
		if (argument >= 0) {
			throw new IllegalArgumentException("%s (%s) must be negative!".formatted(name, argument));
		} else {
			return argument;
		}
	}

	/**
	 * @return the passed {@code greater} if it is not less than the passed {@code lesser}
	 *
	 * @throws IllegalArgumentException if the passed {@code greater} is less than the passed {@code lesser}
	 */
	public static int requireNotLess(int greater, String greaterName, int lesser, String lesserName) {
		if (greater < lesser) {
			throw new IllegalArgumentException(
				"%s (%s) must not be less than %s (%s)!"
					.formatted(greaterName, greater, lesserName, lesser)
			);
		} else {
			return greater;
		}
	}
}
