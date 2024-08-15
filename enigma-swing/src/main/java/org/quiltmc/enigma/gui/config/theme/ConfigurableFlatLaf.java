package org.quiltmc.enigma.gui.config.theme;

import com.formdev.flatlaf.FlatPropertiesLaf;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueKey;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

public abstract class ConfigurableFlatLaf extends FlatPropertiesLaf {
	private static final Map<String, Function<Theme.LookAndFeelColors, TrackedValue<Theme.SerializableColor>>>
		COLOR_GETTERS_BY_KEY =
		Map.of(
			"@foreground", Theme.LookAndFeelColors::getForeground,
			 "@background", Theme.LookAndFeelColors::getBackground,

			"@accentBaseColor", Theme.LookAndFeelColors::getAccentBaseColor,

			"activeCaption", Theme.LookAndFeelColors::getActiveCaption,
			"inactiveCaption", Theme.LookAndFeelColors::getInactiveCaption,

			"Component.error.focusedBorderColor", Theme.LookAndFeelColors::getErrorBorder,
			"Component.warning.focusedBorderColor", Theme.LookAndFeelColors::getWarningBorder
		);

	protected ConfigurableFlatLaf(String name, Theme.LookAndFeelColors colors) {
		super(name, new Properties(((int) colors.stream().count()) + 1));

		this.setColorProperties(colors);
		this.getProperties().setProperty("@baseTheme", this.getBase());
	}

	/**
	 * @return one of the allowed {@code <baseTheme>}s listed in the javadoc for {@link FlatPropertiesLaf}
	 */
	protected abstract String getBase();

	private void setColorProperties(Theme.LookAndFeelColors colors) {
		COLOR_GETTERS_BY_KEY.forEach((key, colorGetter) ->
			this.getProperties().setProperty(
				key,
				"#" + colorGetter.apply(colors).value().getRepresentation()
			)
		);
	}
}
