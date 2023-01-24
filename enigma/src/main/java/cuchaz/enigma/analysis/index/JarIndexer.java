package cuchaz.enigma.analysis.index;

import cuchaz.enigma.analysis.ReferenceTargetType;
import cuchaz.enigma.translation.representation.Lambda;
import cuchaz.enigma.translation.representation.entry.*;

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

	record EnclosingMethodData(String owner, String name, String descriptor) {
		public MethodEntry getMethod() {
			return MethodEntry.parse(this.owner, this.name, this.descriptor);
		}
	}
}
