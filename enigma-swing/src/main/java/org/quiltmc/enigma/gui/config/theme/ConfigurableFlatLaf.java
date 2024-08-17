package org.quiltmc.enigma.gui.config.theme;

import com.formdev.flatlaf.FlatPropertiesLaf;
import org.quiltmc.config.api.values.TrackedValue;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class ConfigurableFlatLaf extends FlatPropertiesLaf {
	private static final Map<String, Function<Theme.LookAndFeelColors, TrackedValue<Theme.SerializableColor>>>
		COLOR_GETTERS_BY_KEY = ThemeUtil.createColorGettersByKey(
			Stream.of("@foreground"),
			Stream.of("@background"),

			Stream.of("@accentBaseColor"),

			Stream.of("activeCaption"),
			Stream.of("inactiveCaption"),

			Stream.of("Component.error.focusedBorderColor"),
			Stream.of("Component.warning.focusedBorderColor")
		);

	private static String colorPropertyValueOf(Theme.SerializableColor color) {
		return "#" + color.getRepresentation();
	}

	protected ConfigurableFlatLaf(String name, Theme.LookAndFeelColors colors) {
		super(name, new Properties(((int) colors.stream().count()) + 1));

		this.setColorProperties(colors);
		this.getProperties().setProperty("@baseTheme", this.getBase().value);
	}

	protected abstract Base getBase();

	private void setColorProperties(Theme.LookAndFeelColors colors) {
		COLOR_GETTERS_BY_KEY.forEach((key, colorGetter) ->
			this.getProperties().setProperty(
				key,
				colorPropertyValueOf(colorGetter.apply(colors).value())
			)
		);
	}

	/**
	 * These correspond to the allowed {@code <baseTheme>}s listed in the javadoc for {@link FlatPropertiesLaf}
	 */
	protected enum Base {
		LIGHT("light"),
		DARK("dark"),
		DARCULA("darcula"),
		INTELLIJ("intellij");

		public final String value;

		Base(String value) {
			this.value = value;
		}
	}
}
