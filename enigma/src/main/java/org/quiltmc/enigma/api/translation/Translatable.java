package org.quiltmc.enigma.api.translation;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.translation.mapping.EntryMap;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;

public interface Translatable {
	TranslateResult<? extends Translatable> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings);

	@Nullable
	default Translatable translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		TranslateResult<? extends Translatable> res = this.extendedTranslate(translator, resolver, mappings);
		return res == null ? null : res.getValue();
	}
}
