package org.quiltmc.enigma.input.parameter_connection;

public class ParameterConnections {
	static final Object O = new Object();

	static Object toStringOf(String param) {
		return new Object() {
			@Override
			public String toString() {
				return param;
			}
		};
	}

	static String getString() {
		return "string";
	}

	static Object weirdToString(String param) {
		final Object[] os = new Object[0];

		return new Object() {
			final String nonSynthetic = getString();

			@Override
			public String toString() {
				return O.toString() + os.toString() + param + this.nonSynthetic;
			}
		};
	}

	static InnerNamedStatic withVisibleParam(String param) {
		return new InnerNamedStatic(getString()) {
			@Override
			public String toString() {
				return this.nonSynthetic + param;
			}
		};
	}

	static int max(int left, int right) {
		class IntGetter {
			final int ignored;

			IntGetter(int ignored) {
				this.ignored = ignored;
			}

			int left() {
				return left;
			}

			int right() {
				return right;
			}
		}

		if (left > right) {
			return new IntGetter(right).left();
		} else {
			return new IntGetter(left).right();
		}
	}

	class InnerNamedInstance { }

	static class InnerNamedStatic {
		final Object nonSynthetic;

		InnerNamedStatic(Object nonSynthetic) {
			this.nonSynthetic = nonSynthetic;
		}
	}
}
