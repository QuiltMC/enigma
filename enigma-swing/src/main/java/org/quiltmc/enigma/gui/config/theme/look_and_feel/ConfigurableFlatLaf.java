package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import com.formdev.flatlaf.FlatPropertiesLaf;
import org.quiltmc.enigma.gui.config.theme.properties.ConfigurableLafThemeProperties;
import org.quiltmc.enigma.gui.config.theme.properties.ConfigurableLafThemeProperties.LookAndFeelColors;

import java.util.Map;
import java.util.Properties;

public abstract class ConfigurableFlatLaf extends FlatPropertiesLaf {
	private static final Map<String, HexColorStringGetter>
			COLOR_GETTERS_BY_KEY = Map.of(
					"@foreground", HexColorStringGetter.of(ConfigurableLafThemeProperties.LookAndFeelColors::getForeground),
					"@background", HexColorStringGetter.of(ConfigurableLafThemeProperties.LookAndFeelColors::getBackground),

					"@accentBaseColor", HexColorStringGetter.of(ConfigurableLafThemeProperties.LookAndFeelColors::getAccentBaseColor),

					"activeCaption", HexColorStringGetter.of(ConfigurableLafThemeProperties.LookAndFeelColors::getActiveCaption),
					"inactiveCaption", HexColorStringGetter.of(ConfigurableLafThemeProperties.LookAndFeelColors::getInactiveCaption),

					"Component.error.focusedBorderColor", HexColorStringGetter.of(ConfigurableLafThemeProperties.LookAndFeelColors::getErrorBorder),
					"Component.warning.focusedBorderColor", HexColorStringGetter.of(ConfigurableLafThemeProperties.LookAndFeelColors::getWarningBorder)
			);

	private static Properties createProperties(LookAndFeelColors colors, Base base) {
		final Properties properties = new Properties(((int) colors.stream().count()) + 1);

		properties.setProperty("@baseTheme", base.value);

		COLOR_GETTERS_BY_KEY.forEach((key, colorGetter) ->
				properties.setProperty(key, colorGetter.apply(colors))
		);

		return properties;
	}

	protected ConfigurableFlatLaf(String name, LookAndFeelColors colors, Base base) {
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
		ConfigurableFlatLaf construct(LookAndFeelColors colors);
	}
}
