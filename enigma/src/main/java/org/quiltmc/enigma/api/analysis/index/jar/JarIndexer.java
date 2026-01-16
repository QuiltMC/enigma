package org.quiltmc.enigma.api.analysis.index.jar;

import org.quiltmc.enigma.api.analysis.ReferenceTargetType;
import org.quiltmc.enigma.api.translation.representation.Lambda;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
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

	default void indexClassReference(MethodDefEntry callerEntry, ClassEntry referencedEntry, ReferenceTargetType targetType) {
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

	String getTranslationKey();

	// TODO we should probably replace this with a type object in a breaking update
	/**
	 * @return the class used to {@linkplain JarIndex#getIndex(Class) look up} this indexer in a {@link JarIndex}
	 *
	 * @implSpec must return a type to which this indexer is assignable
	 *
	 *  @implNote a {@link JarIndex} can only contain one index of a given type
	 */
	default Class<? extends JarIndexer> getType() {
		return this.getClass();
	}

	record EnclosingMethodData(String owner, String name, String descriptor) {
		public MethodEntry getMethod() {
			return MethodEntry.parse(this.owner, this.name, this.descriptor);
		}
	}
}
