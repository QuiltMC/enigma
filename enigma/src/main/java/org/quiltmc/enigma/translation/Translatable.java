package org.quiltmc.enigma.translation;

import org.quiltmc.enigma.translation.mapping.EntryMap;
import org.quiltmc.enigma.translation.mapping.EntryMapping;
import org.quiltmc.enigma.translation.mapping.EntryResolver;

import javax.annotation.Nullable;

public interface Translatable {
	TranslateResult<? extends Translatable> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings);

	@Nullable
	default Translatable translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		TranslateResult<? extends Translatable> res = this.extendedTranslate(translator, resolver, mappings);
		return res == null ? null : res.getValue();
	}
}
