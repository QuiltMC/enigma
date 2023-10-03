package org.quiltmc.enigma.stats;

import org.quiltmc.enigma.util.I18n;

public enum StatType {
	CLASSES("type.classes"),
	METHODS("type.methods"),
	FIELDS("type.fields"),
	PARAMETERS("type.parameters");

	private final String translationKey;

	StatType(String translationKey) {
		this.translationKey = translationKey;
	}

	public String getTranslationKey() {
		return this.translationKey;
	}

	public String getName() {
		return I18n.translate(this.getTranslationKey());
	}
}
