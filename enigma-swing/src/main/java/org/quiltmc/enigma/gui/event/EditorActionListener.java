package org.quiltmc.enigma.gui.event;

import org.quiltmc.enigma.analysis.EntryReference;
import org.quiltmc.enigma.classhandle.ClassHandle;
import org.quiltmc.enigma.gui.panel.EditorPanel;
import org.quiltmc.enigma.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.translation.representation.entry.Entry;

public interface EditorActionListener {
	default void onCursorReferenceChanged(EditorPanel editor, EntryReference<Entry<?>, Entry<?>> ref) {
	}

	default void onClassHandleChanged(EditorPanel editor, ClassEntry old, ClassHandle ch) {
	}

	default void onTitleChanged(EditorPanel editor, String title) {
	}
}
