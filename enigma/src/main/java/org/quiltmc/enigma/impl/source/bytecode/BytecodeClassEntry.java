package org.quiltmc.enigma.impl.source.bytecode;

import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class BytecodeClassEntry extends ClassEntry {
	BytecodeClassEntry(String className) {
		super(className);
	}

	BytecodeClassEntry(@Nullable ClassEntry parent, String className) {
		super(parent, className);
	}

	@Override
	public String getSourceRemapName() {
		return this.getFullName();
	}

	@Override
	public TranslateResult<? extends ClassEntry> extendedTranslate(Translator translator, @Nonnull EntryMapping mapping) {
		var result = super.extendedTranslate(translator, mapping);
		return result.map(e -> new BytecodeClassEntry(e.getParent(), e.getName()));
	}
}
