package org.quiltmc.enigma.translation;

import org.quiltmc.enigma.translation.mapping.EntryMap;
import org.quiltmc.enigma.translation.mapping.EntryMapping;
import org.quiltmc.enigma.translation.mapping.EntryResolver;

import javax.annotation.Nullable;

public class MappingTranslator implements Translator {
	private final EntryMap<EntryMapping> mappings;
	private final EntryResolver resolver;

	public MappingTranslator(EntryMap<EntryMapping> mappings, EntryResolver resolver) {
		this.mappings = mappings;
		this.resolver = resolver;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Translatable> TranslateResult<T> extendedTranslate(T translatable) {
		if (translatable == null) {
			return null;
		}

		return (TranslateResult<T>) translatable.extendedTranslate(this, this.resolver, this.mappings);
	}
}
