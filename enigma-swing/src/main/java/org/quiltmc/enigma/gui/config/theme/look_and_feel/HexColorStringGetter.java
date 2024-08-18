package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import org.quiltmc.enigma.gui.config.theme.Theme;

import java.util.function.Function;

@FunctionalInterface
public interface HexColorStringGetter extends Function<Theme.LookAndFeelColors, String> {
	static HexColorStringGetter of(TrackedSerializableColorGetter getter) {
		return of(SerializableColorGetter.of(getter));
	}

	static HexColorStringGetter of(SerializableColorGetter getter) {
		return of(getter.andThen(HexColorStringGetter::colorPropertyValueOf));
	}

	static HexColorStringGetter of(Function<Theme.LookAndFeelColors, String> getter) {
		return getter::apply;
	}

	static String colorPropertyValueOf(Theme.SerializableColor color) {
		return prependHash(color.getRepresentation());
	}

	static String prependHash(String hexColor) {
		return "#" + hexColor;
	}
}
