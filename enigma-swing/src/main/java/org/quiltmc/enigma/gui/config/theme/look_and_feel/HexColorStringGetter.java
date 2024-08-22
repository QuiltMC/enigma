package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import org.quiltmc.enigma.gui.config.theme.properties.ConfigurableLafThemeProperties;
import org.quiltmc.enigma.gui.config.theme.properties.ThemeProperties;

import java.util.function.Function;

@FunctionalInterface
public interface HexColorStringGetter extends Function<ConfigurableLafThemeProperties.LookAndFeelColors, String> {
	static HexColorStringGetter of(TrackedSerializableColorGetter getter) {
		return of(SerializableColorGetter.of(getter));
	}

	static HexColorStringGetter of(SerializableColorGetter getter) {
		return of(getter.andThen(HexColorStringGetter::colorPropertyValueOf));
	}

	static HexColorStringGetter of(Function<ConfigurableLafThemeProperties.LookAndFeelColors, String> getter) {
		return getter::apply;
	}

	static String colorPropertyValueOf(ThemeProperties.SerializableColor color) {
		return prependHash(color.getRepresentation());
	}

	static String prependHash(String hexColor) {
		return "#" + hexColor;
	}
}
