package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import com.formdev.flatlaf.FlatPropertiesLaf;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.config.theme.Theme;
import org.quiltmc.enigma.gui.config.theme.ThemeUtil;

import java.util.Map;
import java.util.Properties;
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

	private static Properties createProperties(Theme.LookAndFeelColors colors, Base base) {
		final Properties properties = new Properties(((int) colors.stream().count()) + 1);

		properties.setProperty("@baseTheme", base.value);

		COLOR_GETTERS_BY_KEY.forEach((key, colorGetter) ->
			properties.setProperty(
				key,
				colorPropertyValueOf(colorGetter.apply(colors).value())
			)
		);

		return properties;
	}

	protected ConfigurableFlatLaf(String name, Theme.LookAndFeelColors colors, Base base) {
		super(name, createProperties(colors, base));
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
