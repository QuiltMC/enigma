package org.quiltmc.enigma.api.event;

import org.quiltmc.enigma.api.class_handle.ClassHandle;
import org.quiltmc.enigma.api.class_handle.ClassHandleError;
import org.quiltmc.enigma.source.DecompiledClassSource;
import org.quiltmc.enigma.api.source.Source;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.util.Result;

public interface ClassHandleListener {
	default void onDeobfRefChanged(ClassHandle h, ClassEntry deobfRef) {
	}

	default void onUncommentedSourceChanged(ClassHandle h, Result<Source, ClassHandleError> res) {
	}

	default void onDocsChanged(ClassHandle h, Result<Source, ClassHandleError> res) {
	}

	default void onMappedSourceChanged(ClassHandle h, Result<DecompiledClassSource, ClassHandleError> res) {
	}

	default void onInvalidate(ClassHandle h, InvalidationType t) {
	}

	default void onDeleted(ClassHandle h) {
	}

	enum InvalidationType {
		FULL,
		JAVADOC,
		MAPPINGS,
	}
}
