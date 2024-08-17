package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.config.theme.Theme;
import org.quiltmc.enigma.gui.config.theme.ThemeUtil;

import javax.swing.plaf.metal.MetalLookAndFeel;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class ConfigurableMetalOceanLookAndFeel extends MetalLookAndFeel {
	private static final Map<String, Function<Theme.LookAndFeelColors, TrackedValue<Theme.SerializableColor>>>
			COLOR_GETTERS_BY_KEY = ThemeUtil.createColorGettersByKey(
				// ColorUIResource CONTROL_TEXT_COLOR =  new PrintColorUIResource(0x333333, Color.BLACK)
				Stream.of(
					"DesktopIcon.foreground",
					"EditorPane.caretForeground",
					"Menu.selectionForeground",
					"CheckBoxMenuItem.selectionForeground",
					"CheckBox.foreground",
					"FormattedTextField.caretForeground",
					"TextField.caretForeground",
					"OptionPane.messageForeground",
					"Label.foreground",
					"inactiveCaptionText",
					"PasswordField.selectionForeground",
					"CheckBoxMenuItem.acceleratorSelectionForeground",
					"PasswordField.foreground",
					"TextPane.foreground",
					"Viewport.foreground",
					"TabbedPane.foreground",
					"RadioButton.foreground",
					"infoText",
					"MenuItem.foreground",
					"Tree.textForeground",
					"TableHeader.foreground",
					"ScrollPane.foreground",
					"TextArea.caretForeground",
					"textHighlightText",
					"controlText",
					"ComboBox.foreground",
					"MenuItem.selectionForeground",
					"TextArea.selectionForeground",
					"MenuItem.acceleratorSelectionForeground",
					"Panel.foreground",
					"ToolTip.foreground",
					"EditorPane.selectionForeground",
					"List.foreground",
					"activeCaptionText",
					"Tree.foreground",
					"TextField.foreground",
					"FormattedTextField.selectionForeground",
					"textText",
					"ColorChooser.foreground",
					"Button.foreground",
					"Table.focusCellForeground",
					"PasswordField.caretForeground",
					"TextPane.selectionForeground",
					"menuText",
					"TextField.selectionForeground",
					"List.selectionForeground",
					"RadioButtonMenuItem.acceleratorSelectionForeground",
					"ComboBox.selectionForeground",
					"Tree.selectionForeground",
					"TextPane.caretForeground",
					"Table.selectionForeground",
					"ToggleButton.foreground",
					"windowText",
					"CheckBoxMenuItem.foreground",
					"FormattedTextField.foreground",
					"TitledBorder.titleColor",
					"Table.dropLineShortColor",
					"EditorPane.foreground",
					"InternalFrame.activeTitleForeground",
					"TextArea.foreground",
					"PopupMenu.foreground",
					"OptionPane.foreground",
					"RadioButtonMenuItem.selectionForeground",
					"Menu.acceleratorSelectionForeground",
					"Menu.foreground",
					"ToolBar.foreground",
					"RadioButtonMenuItem.foreground",
					"MenuBar.foreground",
					"Table.foreground",
					"InternalFrame.inactiveTitleForeground"
				),
				// ColorUIResource white = new ColorUIResource( 255, 255, 255 )
				Stream.of(
					"TextArea.background",
					"Table.background",
					"Slider.highlight",
					"RadioButton.highlight",
					"text",
					"MenuBar.highlight",
					"ScrollBar.highlight",
					"ToolBar.light",
					"PasswordField.background",
					"TextPane.background",
					"ToggleButton.highlight",
					"Tree.textBackground",
					"InternalFrame.borderLight",
					"desktop",
					"Desktop.background",
					"SplitPane.highlight",
					"Tree.background",
					"TextField.background",
					"Button.light",
					"InternalFrame.borderHighlight",
					"ToggleButton.light",
					"TextField.light",
					"List.background",
					"window",
					"TabbedPane.selectHighlight",
					"TextField.highlight",
					"Table.focusCellBackground",
					"controlLtHighlight",
					"controlHighlight",
					"Separator.background",
					"Button.highlight",
					"ComboBox.buttonHighlight",
					"Separator.highlight",
					"RadioButton.light",
					"TabbedPane.highlight",
					"FormattedTextField.background",
					"ToolBar.highlight",
					"EditorPane.background"
				),

				Stream.of(),

				// ColorUIResource primary3 = new ColorUIResource(204, 204, 255)
				Stream.of(
					"activeCaption",
					"PasswordField.inactiveForeground",
					"ComboBox.disabledForeground",
					"textInactiveText",
					"ToolBar.shadow",
					"controlShadow",
					"inactiveCaptionBorder",
					"EditorPane.inactiveForeground",
					"RadioButton.select",
					"Separator.shadow",
					"TextField.shadow",
					"PasswordField.selectionBackground",
					"TabbedPane.shadow",
					"TabbedPane.background",
					"InternalFrame.borderShadow",
					"ComboBox.buttonShadow",
					"Label.disabledShadow",
					"TextArea.inactiveForeground",
					"Tree.hash",
					"Button.shadow",
					"TextArea.selectionBackground",
					"TextPane.selectionBackground",
					"ToolTip.background",
					"EditorPane.selectionBackground",
					"TextPane.inactiveForeground",
					"SplitPane.shadow",
					"TextField.selectionBackground",
					"TextField.inactiveForeground",
					"FormattedTextField.selectionBackground",
					"ScrollBar.thumbHighlight",
					"MenuBar.shadow",
					"ToggleButton.shadow",
					"Tree.line",
					"FormattedTextField.inactiveForeground",
					"Button.select",
					"ScrollBar.shadow",
					"textHighlight",
					"Table.sortIconColor",
					"List.selectionBackground",
					"Tree.selectionBackground",
					"Table.selectionBackground",
					"info",
					"InternalFrame.activeTitleBackground",
					"ToolBar.floatingForeground",
					"Checkbox.select",
					"ToggleButton.select",
					"Slider.shadow",
					"RadioButton.shadow"
				),
				// OceanTheme
				// ColorUIResource SECONDARY3 = new ColorUIResource(0xEEEEEE)
				Stream.of(
					"ToolBar.floatingBackground",
					"DesktopIcon.background",
					"TabbedPane.unselectedBackground",
					"InternalFrame.borderColor",
					"ComboBox.disabledBackground",
					"MenuBar.background",
					"menu",
					"InternalFrame.inactiveTitleBackground",
					"PasswordField.inactiveBackground",
					"RadioButton.background",
					"inactiveCaption",
					"CheckBox.background",
					"ProgressBar.background",
					"Label.background",
					"Spinner.foreground",
					"Spinner.background",
					"Viewport.background",
					"MenuItem.background",
					"ScrollBar.track",
					"control",
					"TabbedPane.light",
					"TableHeader.background",
					"ScrollPane.background",
					"ColorChooser.swatchesDefaultRecentColor",
					"ComboBox.background",
					"Button.background",
					"Panel.background",
					"ToolBar.dockingBackground",
					"TextField.inactiveBackground",
					"ColorChooser.background",
					"ComboBox.buttonBackground",
					"scrollbar",
					"SplitPane.background",
					"FormattedTextField.inactiveBackground",
					"CheckBoxMenuItem.background",
					"ProgressBar.selectionForeground",
					"Slider.background",
					"ScrollBar.foreground",
					"windowBorder",
					"ToggleButton.background",
					"PopupMenu.background",
					"ScrollBar.background",
					"OptionPane.background",
					"ToolTip.backgroundInactive",
					"Menu.background",
					"ToolBar.background",
					"RadioButtonMenuItem.background"
				),

				Stream.of(),
				Stream.of()
			);

	static {
		// ColorUIResource primary2 = new ColorUIResource(153,153, 204)
		// @accentBase2Color = lighten(saturate(@accentBaseColor,10%),6%)
		Stream.of(
				"RadioButtonMenuItem.selectionBackground",
				"ProgressBar.foreground",
				"Menu.selectionBackground",
				"CheckBoxMenuItem.selectionBackground",
				"Button.focus",
				"ToggleButton.focus",
				"activeCaptionBorder",
				"ScrollBar.thumb",
				"MenuItem.selectionBackground",
				"RadioButton.focus",
				"Slider.foreground",
				"CheckBox.focus",
				"Slider.focus",
				"ComboBox.selectionBackground"
		);

		// OceanTheme
		// ColorUIResource PRIMARY1 = new ColorUIResource(0x6382BF)
		// @accentCheckmarkColor = if(@accentColor, @accentColor, tint(@accentBase2Color,20%))
		// @accentBase2Color = lighten(saturate(@accentBaseColor,10%),6%)
		Stream.of(
				"TabbedPane.focus",
				"MenuItem.acceleratorForeground",
				"List.dropLineColor",
				"Menu.acceleratorForeground",
				"Tree.dropLineColor",
				"Table.dropLineColor",
				"ScrollBar.thumbShadow",
				"Separator.foreground",
				"ToolBar.dockingForeground",
				"TabbedPane.borderHightlightColor",
				"Tree.selectionBorderColor",
				"ProgressBar.selectionBackground",
				"RadioButtonMenuItem.acceleratorForeground",
				"CheckBoxMenuItem.acceleratorForeground"
		);

		// ColorUIResource INACTIVE_CONTROL_TEXT_COLOR = new ColorUIResource(0x999999)
		// textInactiveText = @disabledForeground
		// @disabledForeground = tint(@foreground,55%)
		Stream.of(
			"RadioButton.disabledText", // putDefaults( defaults, defaults.getColor( "textInactiveText" )
			"Button.toolBarBorderBackground",
			"Label.disabledForeground",
			"CheckBoxMenuItem.disabledForeground",
			"CheckBox.disabledText",
			"ToggleButton.disabledText",
			"RadioButtonMenuItem.disabledForeground",
			"Menu.disabledForeground",
			"MenuItem.disabledForeground",
			"Button.disabledText"
		);

		// OceanTheme
		// ColorUIResource OCEAN_DROP = new ColorUIResource(0xD2E9FF)
		// @dropCellBackground = lighten(List.selectionBackground,10%,lazy)
		// "List.selectionBackground", textHighlight,
		// textHighlight = @selectionBackground
		// @selectionBackground = @accentSelectionBackground
		// @accentSelectionBackground = if(@accentColor, @accentColor, @accentBaseColor)
		Stream.of(
			"Table.dropCellBackground",//@dropCellBackground
			"Tree.dropCellBackground",//@dropCellBackground
			"List.dropCellBackground"//@dropCellBackground
		);

		// OceanTheme
		// ColorUIResource SECONDARY1 = new ColorUIResource(0x7A8A99)
		// controlShadow = $Component.borderColor
		// Component.borderColor = shade(@background,20%)
		Stream.of(
			"ScrollBar.thumbDarkShadow",//$controlDkShadow
			"ScrollBar.trackHighlight",//$controlDkShadow
			"InternalFrame.borderDarkShadow",//$controlDkShadow
			"TabbedPane.darkShadow",
			"RadioButton.darkShadow",
			"ToolTip.foregroundInactive",
			"Table.gridColor",//darken($Table.background,8%)
			"controlDkShadow",//darken($controlShadow,15%)
			"TextField.darkShadow",
			"Button.darkShadow",
			"ComboBox.buttonDarkShadow",//$controlDkShadow
			"SplitPane.darkShadow",
			"ToggleButton.darkShadow",
			"ScrollBar.darkShadow",
			"ToolBar.darkShadow"
		);

		// OceanTheme
		// Color cccccc = new ColorUIResource(0xCCCCCC);
		// Separator.foreground = shade(@background,15%)
		Stream.of(
			"ToolBar.borderColor",
			"Button.disabledToolBarBorderBackground",
			"MenuBar.borderColor"//$Separator.foreground
		);

		// OceanTheme
		// Color c8ddf2 = new ColorUIResource(0xC8DDF2)
		// Component.borderColor = shade(@background,20%)
		//
		//TableHeader.background = @componentBackground
		//@componentBackground = lighten(@background,5%)
		Stream.of(
			"TabbedPane.contentAreaColor",//$Component.borderColor
			"TabbedPane.selected",
			"TableHeader.focusCellBackground",//$TableHeader.background
			"SplitPane.dividerFocusColor"
		);

		// Color darkGray = new Color(64, 64, 64);
		// Component.borderColor = shade(@background,20%)
		Stream.of(
			"SplitPaneDivider.draggingColor"//$Component.borderColor
		);

		// Color black = new Color(0, 0, 0)
		// @disabledForeground = tint(@foreground,55%)
		Stream.of(
			"Slider.tickColor"//@disabledForeground
		);
	}

	// new ColorUIResource(51, 102, 51)
	// "OptionPane.questionDialog.border.background"

	// OceanTheme
	// Color dadada = new ColorUIResource(0xDADADA)
	// "TabbedPane.tabAreaBackground"

	// new ColorUIResource(0, 51, 0)
	// "OptionPane.questionDialog.titlePane.foreground"

	// new ColorUIResource(102, 153, 102)
	// "OptionPane.questionDialog.titlePane.shadow"

	// new ColorUIResource(153, 204, 153)
	// "OptionPane.questionDialog.titlePane.background"

	// new ColorUIResource(102, 51, 0)
	// "OptionPane.warningDialog.titlePane.foreground"

	// new ColorUIResource(255, 153, 153)
	// "OptionPane.errorDialog.titlePane.background"

	// new ColorUIResource(255, 204, 153)
	// "OptionPane.warningDialog.titlePane.background"

	// new ColorUIResource(153, 51, 51)
	// "OptionPane.errorDialog.border.background"

	// new ColorUIResource(204, 153, 102)
	// "OptionPane.warningDialog.titlePane.shadow"

	// new ColorUIResource(153, 102, 51)
	// "OptionPane.warningDialog.border.background"

	// new ColorUIResource(204, 102, 102)
	// "OptionPane.errorDialog.titlePane.shadow"

	// new ColorUIResource(51, 0, 0)
	// "OptionPane.errorDialog.titlePane.foreground"

	// OceanTheme
	// new ColorUIResource(0xD2E2EF)
	// "Slider.altTrackColor"
}
