package cuchaz.enigma.analysis;

import cuchaz.enigma.translation.representation.entry.ClassEntry;

public interface ReferenceTargetType {
	Kind getKind();

	static None none() {
		return None.NONE;
	}

	static Uninitialized uninitialized() {
		return Uninitialized.UNINITIALIZED;
	}

	static ClassType classType(ClassEntry name) {
		return new ClassType(name);
	}

	enum Kind {
		NONE,
		UNINITIALIZED,
		CLASS_TYPE
	}

	class None implements ReferenceTargetType {
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

	class Uninitialized implements ReferenceTargetType {
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

	class ClassType implements ReferenceTargetType {
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
