package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.config.theme.ThemeCreator;

import java.util.function.Function;

@FunctionalInterface
public interface SerializableColorGetter extends Function<ThemeCreator.LookAndFeelColors, ThemeCreator.SerializableColor> {
	static SerializableColorGetter of(TrackedSerializableColorGetter getter) {
		return of(getter.andThen(TrackedValue::value));
	}

	static SerializableColorGetter of(Function<ThemeCreator.LookAndFeelColors, ThemeCreator.SerializableColor> getter) {
		return getter::apply;
	}
}
