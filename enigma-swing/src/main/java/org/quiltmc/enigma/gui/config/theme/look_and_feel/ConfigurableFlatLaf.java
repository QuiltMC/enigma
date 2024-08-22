package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import com.formdev.flatlaf.FlatPropertiesLaf;
import org.quiltmc.enigma.gui.config.theme.properties.composite.LookAndFeelProperties.Colors;

import java.util.Map;
import java.util.Properties;

public abstract class ConfigurableFlatLaf extends FlatPropertiesLaf {
	private static final Map<String, HexColorStringGetter>
			COLOR_GETTERS_BY_KEY = Map.of(
					"@foreground", HexColorStringGetter.of(Colors::getForeground),
					"@background", HexColorStringGetter.of(Colors::getBackground),

					"@accentBaseColor", HexColorStringGetter.of(Colors::getAccentBaseColor),

					"activeCaption", HexColorStringGetter.of(Colors::getActiveCaption),
					"inactiveCaption", HexColorStringGetter.of(Colors::getInactiveCaption),

					"Component.error.focusedBorderColor", HexColorStringGetter.of(Colors::getErrorBorder),
					"Component.warning.focusedBorderColor", HexColorStringGetter.of(Colors::getWarningBorder)
			);

	private static Properties createProperties(Colors colors, Base base) {
		final Properties properties = new Properties(((int) colors.stream().count()) + 1);

		properties.setProperty("@baseTheme", base.value);

		COLOR_GETTERS_BY_KEY.forEach((key, colorGetter) ->
				properties.setProperty(key, colorGetter.apply(colors))
		);

		return properties;
	}

	protected ConfigurableFlatLaf(String name, Colors colors, Base base) {
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

	@FunctionalInterface
	public interface Constructor {
		ConfigurableFlatLaf construct(Colors colors);
	}
}
