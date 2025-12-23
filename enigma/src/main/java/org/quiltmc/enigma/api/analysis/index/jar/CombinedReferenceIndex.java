package org.quiltmc.enigma.api.analysis.index.jar;

import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.CombinedCollection;

import java.util.Collection;

/**
 * <b>Note:</b> does <em>not</em> currently index main jar references to library types and members.
 */
final class CombinedReferenceIndex implements ReferenceIndex {
	private final ReferenceIndex mainIndex;
	private final ReferenceIndex libIndex;

	CombinedReferenceIndex(ReferenceIndex mainIndex, ReferenceIndex libIndex) {
		this.mainIndex = mainIndex;
		this.libIndex = libIndex;
	}

	@Override
	public Collection<MethodEntry> getMethodsReferencedBy(MethodEntry entry) {
		return new CombinedCollection<>(
			this.mainIndex.getMethodsReferencedBy(entry),
			this.libIndex.getMethodsReferencedBy(entry)
		);
	}

	@Override
	public Collection<FieldEntry> getFieldsReferencedBy(MethodEntry entry) {
		return new CombinedCollection<>(
			this.mainIndex.getFieldsReferencedBy(entry),
			this.libIndex.getFieldsReferencedBy(entry)
		);
	}

	@Override
	public Collection<EntryReference<FieldEntry, MethodDefEntry>> getReferencesToField(FieldEntry entry) {
		return new CombinedCollection<>(
			this.mainIndex.getReferencesToField(entry),
			this.libIndex.getReferencesToField(entry)
		);
	}

	@Override
	public Collection<EntryReference<ClassEntry, MethodDefEntry>> getReferencesToClass(ClassEntry entry) {
		return new CombinedCollection<>(
			this.mainIndex.getReferencesToClass(entry),
			this.libIndex.getReferencesToClass(entry)
		);
	}

	@Override
	public Collection<EntryReference<MethodEntry, MethodDefEntry>> getReferencesToMethod(MethodEntry entry) {
		return new CombinedCollection<>(
			this.mainIndex.getReferencesToMethod(entry),
			this.libIndex.getReferencesToMethod(entry)
		);
	}

	@Override
	public Collection<EntryReference<ClassEntry, FieldDefEntry>> getFieldTypeReferencesToClass(ClassEntry entry) {
		return new CombinedCollection<>(
			this.mainIndex.getFieldTypeReferencesToClass(entry),
			this.libIndex.getFieldTypeReferencesToClass(entry)
		);
	}

	@Override
	public Collection<EntryReference<ClassEntry, MethodDefEntry>> getMethodTypeReferencesToClass(ClassEntry entry) {
		return new CombinedCollection<>(
			this.mainIndex.getMethodTypeReferencesToClass(entry),
			this.libIndex.getMethodTypeReferencesToClass(entry)
		);
	}
}
