package org.quiltmc.enigma.input.similar_class_names;

public class Alphabet {
	public static class Inner {
		public Alpha get() {
			return new Alpha() {
				@Override
				public char get(int n) {
					return 0;
				}
			};
		}
	}
}
