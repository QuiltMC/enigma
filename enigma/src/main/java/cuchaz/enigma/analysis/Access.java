package cuchaz.enigma.analysis;

import cuchaz.enigma.translation.representation.AccessFlags;

import java.lang.reflect.Modifier;

public enum Access {
	PUBLIC, PROTECTED, PACKAGE, PRIVATE;

	public static Access get(AccessFlags flags) {
		return get(flags.getFlags());
	}

	public static Access get(int modifiers) {
		boolean isPublic = Modifier.isPublic(modifiers);
		boolean isProtected = Modifier.isProtected(modifiers);
		boolean isPrivate = Modifier.isPrivate(modifiers);

		if (isPublic && !isProtected && !isPrivate) {
			return PUBLIC;
		} else if (!isPublic && isProtected && !isPrivate) {
			return PROTECTED;
		} else if (!isPublic && !isProtected && isPrivate) {
			return PRIVATE;
		} else if (!isPublic && !isProtected && !isPrivate) {
			return PACKAGE;
		}
		// assume public by default
		return PUBLIC;
	}
}
