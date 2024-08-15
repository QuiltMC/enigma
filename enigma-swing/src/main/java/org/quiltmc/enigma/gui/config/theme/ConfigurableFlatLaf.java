package org.quiltmc.enigma.gui.config.theme;

import com.formdev.flatlaf.FlatPropertiesLaf;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueKey;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

public abstract class ConfigurableFlatLaf extends FlatPropertiesLaf {
	// private static final Map<String, String> PROPERTIES_BY_COLOR = Map.ofEntries(
	// 	// Map.entry("", "TitlePane.closeHoverBackground"),
	// 	// Map.entry("", "TitlePane.closeHoverForeground"),
	// 	// Map.entry("", "TitlePane.closePressedForeground"),
	//
	// 	Map.entry("background", "@background"),
	// 	Map.entry("foreground", "@foreground"),
	// 	Map.entry("selection_foreground", "@selectionForeground"),
	// 	Map.entry("accent_base_color", "@accentBaseColor"),
	// 	Map.entry("active_caption", "activeCaption"),
	// 	Map.entry("inactive_caption", "inactiveCaption"),
	// 	Map.entry("error_border", "Component.error.focusedBorderColor"),
	// 	Map.entry("warning_border", "Component.warning.focusedBorderColor"),
	// 	Map.entry("border", "Component.custom.borderColor")
	//
	// 	// ,
	// 	// Map.entry("desktop_background", "Desktop.background"),
	// 	// Map.entry("", "InternalFrame.activeTitleBackground"),
	// 	// Map.entry("", "InternalFrame.closeHoverForeground"),
	// 	// Map.entry("", "InternalFrame.closePressedForeground"),
	// 	// Map.entry("", "PasswordField.capsLockIconColor"),
	// 	// Map.entry("", "Popup.dropShadowColor")
	// );

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

	// /**
	//  * Override default colors.
	//  * <p>
	//  * Subclasses whose superclasses implement this method should always build on their parent's implementation.
	//  * <p>
	//  * Never access instance fields from this method, as it's called in the constructor.
	//  */
	// protected abstract Theme.LookAndFeelColors.Builder buildColors(Theme.LookAndFeelColors.Builder colors);

	private void setColorProperties(Theme.LookAndFeelColors colors) {
		COLOR_GETTERS_BY_KEY.forEach((key, colorGetter) ->
			this.getProperties().setProperty(
				key,
				"#" + colorGetter.apply(colors).value().getRepresentation()
			)
		);
	}
}
