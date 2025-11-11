package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.config.theme.properties.ThemeProperties;
import org.quiltmc.enigma.gui.config.theme.properties.composite.LookAndFeelProperties;

import java.util.function.Function;

@FunctionalInterface
public interface SerializableColorGetter extends Function<LookAndFeelProperties.Colors, ThemeProperties.SerializableColor> {
	static SerializableColorGetter of(TrackedSerializableColorGetter getter) {
		return of(getter.andThen(TrackedValue::value));
	}

	static SerializableColorGetter of(Function<LookAndFeelProperties.Colors, ThemeProperties.SerializableColor> getter) {
		return getter::apply;
	}
}
