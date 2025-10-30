package org.quiltmc.enigma.input.tooltip;

public class ConvergentInheritance {
	abstract static class Named {
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
