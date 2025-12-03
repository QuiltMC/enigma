package org.quiltmc.enigma.api.analysis.index.jar;

import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.Collection;

public interface ReferenceIndex extends JarIndexer {
	Collection<MethodEntry> getMethodsReferencedBy(MethodEntry entry);

	Collection<FieldEntry> getFieldsReferencedBy(MethodEntry entry);

	Collection<EntryReference<FieldEntry, MethodDefEntry>> getReferencesToField(FieldEntry entry);

	Collection<EntryReference<ClassEntry, MethodDefEntry>> getReferencesToClass(ClassEntry entry);

	Collection<EntryReference<MethodEntry, MethodDefEntry>> getReferencesToMethod(MethodEntry entry);

	Collection<EntryReference<ClassEntry, FieldDefEntry>> getFieldTypeReferencesToClass(ClassEntry entry);

	Collection<EntryReference<ClassEntry, MethodDefEntry>> getMethodTypeReferencesToClass(ClassEntry entry);

	@Override
	default Class<? extends JarIndexer> getType() {
		return ReferenceIndex.class;
	}

	@Override
	default String getTranslationKey() {
		return "progress.jar.indexing.process.references";
	}
}
