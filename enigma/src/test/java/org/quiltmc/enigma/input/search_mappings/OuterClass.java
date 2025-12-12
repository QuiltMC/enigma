package org.quiltmc.enigma.input.search_mappings;

public class OuterClass {
	public static class Inner {
		public OtherReturnInterface get() {
			return new OtherReturnInterface() {
				@Override
				public char get(int n) {
					return 0;
				}
			};
		}
	}
}
