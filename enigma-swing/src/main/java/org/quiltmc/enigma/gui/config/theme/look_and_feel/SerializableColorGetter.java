package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.config.theme.Theme;

import java.util.function.Function;

@FunctionalInterface
public interface SerializableColorGetter extends Function<Theme.LookAndFeelColors, Theme.SerializableColor> {
	static SerializableColorGetter of(TrackedSerializableColorGetter getter) {
		return (SerializableColorGetter) getter.andThen(TrackedValue::value);
	}
}
