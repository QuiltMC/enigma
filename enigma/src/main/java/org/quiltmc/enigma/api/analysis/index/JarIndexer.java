package org.quiltmc.enigma.api.analysis.index;

import org.quiltmc.enigma.analysis.ReferenceTargetType;
import org.quiltmc.enigma.analysis.index.JarIndex;
import org.quiltmc.enigma.api.translation.representation.Lambda;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

public interface JarIndexer {
	default void indexClass(ClassDefEntry classEntry) {
	}

	default void indexField(FieldDefEntry fieldEntry) {
	}

	default void indexMethod(MethodDefEntry methodEntry) {
	}

	default void indexMethodReference(MethodDefEntry callerEntry, MethodEntry referencedEntry, ReferenceTargetType targetType) {
	}

	default void indexFieldReference(MethodDefEntry callerEntry, FieldEntry referencedEntry, ReferenceTargetType targetType) {
	}

	default void indexLambda(MethodDefEntry callerEntry, Lambda lambda, ReferenceTargetType targetType) {
	}

	default void indexEnclosingMethod(ClassDefEntry classEntry, EnclosingMethodData enclosingMethodData) {
	}

	default void processIndex(JarIndex index) {
	}

	default String getTranslationKey() {
		// REMOVE IN 2.0: this is a temporary default impl to avoid api breakage
		return this.getClass().getSimpleName();
	}

	record EnclosingMethodData(String owner, String name, String descriptor) {
		public MethodEntry getMethod() {
			return MethodEntry.parse(this.owner, this.name, this.descriptor);
		}
	}
}
