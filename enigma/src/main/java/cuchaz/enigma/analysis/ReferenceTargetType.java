package cuchaz.enigma.analysis;

import cuchaz.enigma.translation.representation.entry.ClassEntry;

public abstract class ReferenceTargetType {
	public abstract Kind getKind();

	public static None none() {
		return None.NONE;
	}

	public static Uninitialized uninitialized() {
		return Uninitialized.UNINITIALIZED;
	}

	public static ClassType classType(ClassEntry name) {
		return new ClassType(name);
	}

	public enum Kind {
		NONE,
		UNINITIALIZED,
		CLASS_TYPE
	}

	public static class None extends ReferenceTargetType {
		private static final None NONE = new None();

		@Override
		public Kind getKind() {
			return Kind.NONE;
		}

		@Override
		public String toString() {
			return "(none)";
		}
	}

	public static class Uninitialized extends ReferenceTargetType {
		private static final Uninitialized UNINITIALIZED = new Uninitialized();

		@Override
		public Kind getKind() {
			return Kind.UNINITIALIZED;
		}

		@Override
		public String toString() {
			return "(uninitialized)";
		}
	}

	public static class ClassType extends ReferenceTargetType {
		private final ClassEntry entry;

		private ClassType(ClassEntry entry) {
			this.entry = entry;
		}

		public ClassEntry getEntry() {
			return this.entry;
		}

		@Override
		public Kind getKind() {
			return Kind.CLASS_TYPE;
		}

		@Override
		public String toString() {
			return this.entry.toString();
		}
	}
}
