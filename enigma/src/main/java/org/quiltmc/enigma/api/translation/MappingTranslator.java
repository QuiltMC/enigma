package org.quiltmc.enigma.api.translation;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.translation.mapping.EntryMap;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;

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
