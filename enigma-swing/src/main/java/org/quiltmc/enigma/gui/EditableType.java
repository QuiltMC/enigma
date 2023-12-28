package org.quiltmc.enigma.gui;

import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;

public enum EditableType {
	CLASS,
	METHOD,
	FIELD,
	PARAMETER,
	LOCAL_VARIABLE,
	JAVADOC;

	@Nullable
	public static EditableType fromEntry(Entry<?> entry) {
		// TODO get rid of this with Entry rework
		EditableType type = null;

		if (entry instanceof ClassEntry) {
			type = EditableType.CLASS;
		} else if (entry instanceof MethodEntry me) {
			if (me.isConstructor()) {
				// treat constructors as classes because renaming one renames
				// the class
				type = EditableType.CLASS;
			} else {
				type = EditableType.METHOD;
			}
		} else if (entry instanceof FieldEntry) {
			type = EditableType.FIELD;
		} else if (entry instanceof LocalVariableEntry lve) {
			if (lve.isArgument()) {
				type = EditableType.PARAMETER;
			} else {
				type = EditableType.LOCAL_VARIABLE;
			}
		}

		return type;
	}
}
