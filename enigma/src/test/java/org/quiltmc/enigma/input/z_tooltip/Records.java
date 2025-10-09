package org.quiltmc.enigma.input.z_tooltip;

public record Records() {
	public record WithStaticField(Boolean truth) {
		static final String NON_COMPONENT = "not a component";

		@Override
		public Boolean truth() {
			return new WithStaticField(true).truth;
		}
	}

	public record Implementing(int component) implements Runnable {
		@Override
		public void run() {
			System.out.println(new Implementing().component());
		}

		public Implementing() {
			this(0);
		}

		public Implementing(int component) {
			this.component = component + 1;
		}
	}
}
