package org.quiltmc.enigma.api.stats;

import org.quiltmc.enigma.util.I18n;

import java.awt.*;

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

	public Color getColor() {
		return switch (this) {
			case CLASSES -> new Color(0xDE3E80);
			case FIELDS -> new Color(0x8080FF);
			case METHODS -> new Color(0x2196F3);
			case PARAMETERS -> new Color(0x36BF20);
		};
	}
}
