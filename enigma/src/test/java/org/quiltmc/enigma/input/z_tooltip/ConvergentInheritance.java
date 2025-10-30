package org.quiltmc.enigma.input.z_tooltip;

public class ConvergentInheritance {
	static abstract class Named {
		public abstract void setName(String name);
	}

	interface Nameable {
		void setName(String name);
	}

	static class Implementer extends Named implements Nameable {
		private String name;

		@Override
		public void setName(String name) {
			this.name = name;
		}
	}
}
