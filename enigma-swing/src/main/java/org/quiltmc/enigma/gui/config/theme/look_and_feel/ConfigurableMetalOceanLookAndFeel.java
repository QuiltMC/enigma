package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import org.quiltmc.enigma.gui.config.theme.Theme;
import org.quiltmc.enigma.gui.config.theme.Theme.LookAndFeelColors;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigurableMetalOceanLookAndFeel extends MetalLookAndFeel {
	private static Stream<ColorGetterForKeys> streamColorGettersForKeys() {
		return Stream.of(
				// ColorUIResource CONTROL_TEXT_COLOR =  new PrintColorUIResource(0x333333, Color.BLACK)
				new ColorGetterForKeys(
					ColorGetters::getForeground,
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
				new ColorGetterForKeys(
					ColorGetters::getBackground,
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

				// ColorUIResource primary3 = new ColorUIResource(204, 204, 255)
				new ColorGetterForKeys(
					ColorGetters::getActiveCaption,
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
				new ColorGetterForKeys(
					ColorGetters::getInactiveCaption,
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

				// ColorUIResource primary2 = new ColorUIResource(153,153, 204)
				new ColorGetterForKeys(
					ColorGetters::getAccentBase2,
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
				),

				// OceanTheme
				// ColorUIResource PRIMARY1 = new ColorUIResource(0x6382BF)
				new ColorGetterForKeys(
					ColorGetters::getAccentCheckmark,
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
				),

				// ColorUIResource INACTIVE_CONTROL_TEXT_COLOR = new ColorUIResource(0x999999)
				new ColorGetterForKeys(
					ColorGetters::getTextInactiveText,
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
				),

				// OceanTheme
				// ColorUIResource OCEAN_DROP = new ColorUIResource(0xD2E9FF)
				new ColorGetterForKeys(
					ColorGetters::getDropCellBackground,
					"Table.dropCellBackground",//@dropCellBackground
					"Tree.dropCellBackground",//@dropCellBackground
					"List.dropCellBackground"//@dropCellBackground
				),

				// OceanTheme
				// ColorUIResource SECONDARY1 = new ColorUIResource(0x7A8A99)
				new ColorGetterForKeys(
					ColorGetters::getControlShadow,
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
				),

				// OceanTheme
				// Color cccccc = new ColorUIResource(0xCCCCCC)
				new ColorGetterForKeys(
					ColorGetters::getSeparatorForeground,
					"ToolBar.borderColor",
					"Button.disabledToolBarBorderBackground",
					"MenuBar.borderColor"//$Separator.foreground
				),

				// OceanTheme
				// Color c8ddf2 = new ColorUIResource(0xC8DDF2)
				new ColorGetterForKeys(
					ColorGetters::getTableHeaderBackground,
					"TabbedPane.contentAreaColor",//$Component.borderColor
					"TabbedPane.selected",
					"TableHeader.focusCellBackground",//$TableHeader.background
					"SplitPane.dividerFocusColor"
				),

				// Color darkGray = new Color(64, 64, 64)
				new ColorGetterForKeys(
					ColorGetters::getComponentBorder,
					"SplitPaneDivider.draggingColor"//$Component.borderColor
				),

				// Color black = new Color(0, 0, 0)
				new ColorGetterForKeys(
					ColorGetters::getDisabledForeground,
					"Slider.tickColor"//@disabledForeground
				),

				// new ColorUIResource(51, 102, 51)
				new ColorGetterForKeys(
					ColorGetters::getOptionPaneQuestionDialogBorderBackground,
					"OptionPane.questionDialog.border.background"
				),

				// OceanTheme
				// Color dadada = new ColorUIResource(0xDADADA)
				new ColorGetterForKeys(
					ColorGetters::getTabbedPaneTabAreaBackground,
					"TabbedPane.tabAreaBackground"
				),

				// new ColorUIResource(0, 51, 0)
				new ColorGetterForKeys(
					ColorGetters::getOptionPaneQuestionDialogTitlePaneForeground,
					"OptionPane.questionDialog.titlePane.foreground"
				),

				// new ColorUIResource(102, 153, 102)
				new ColorGetterForKeys(
					ColorGetters::getOptionPaneQuestionDialogTitlePaneShadow,
					"OptionPane.questionDialog.titlePane.shadow"
				),

				// new ColorUIResource(153, 204, 153)
				new ColorGetterForKeys(
					ColorGetters::getOptionPaneQuestionDialogTitlePaneBackground,
					"OptionPane.questionDialog.titlePane.background"
				),

				// new ColorUIResource(102, 51, 0)
				new ColorGetterForKeys(
					ColorGetters::getOptionPaneWarningDialogTitlePaneForeground,
					"OptionPane.warningDialog.titlePane.foreground"
				),

				// new ColorUIResource(255, 153, 153)
				new ColorGetterForKeys(
					ColorGetters::getOptionPaneErrorDialogTitlePaneBackground,
					"OptionPane.errorDialog.titlePane.background"
				),

				// new ColorUIResource(255, 204, 153)
				new ColorGetterForKeys(
					ColorGetters::getOptionPaneWarningDialogTitlePaneBackground,
					"OptionPane.warningDialog.titlePane.background"
				),

				// new ColorUIResource(153, 51, 51)
				new ColorGetterForKeys(
					ColorGetters::getOptionPaneErrorDialogBorderBackground,
					"OptionPane.errorDialog.border.background"
				),

				// new ColorUIResource(204, 153, 102)
				new ColorGetterForKeys(
					ColorGetters::getOptionPaneWarningDialogTitlePaneShadow,
					"OptionPane.warningDialog.titlePane.shadow"
				),

				// new ColorUIResource(153, 102, 51)
				new ColorGetterForKeys(
					ColorGetters::getOptionPaneWarningDialogBorderBackground,
					"OptionPane.warningDialog.border.background"
				),

				// new ColorUIResource(204, 102, 102)
				new ColorGetterForKeys(
					ColorGetters::getOptionPaneErrorDialogTitlePaneShadow,
					"OptionPane.errorDialog.titlePane.shadow"
				),

				// new ColorUIResource(51, 0, 0)
				new ColorGetterForKeys(
					ColorGetters::getOptionPaneErrorDialogTitlePaneForeground,
					"OptionPane.errorDialog.titlePane.foreground"
				),

				// OceanTheme
				// new ColorUIResource(0xD2E2EF)
				new ColorGetterForKeys(
					ColorGetters::getSliderAltTrackColor,
					"Slider.altTrackColor"
				)
		);
	}

	private static Map<String, String> createColorDefaults(LookAndFeelColors colors) {
		return streamColorGettersForKeys()
			.map(colorGetterForKeys ->
				Map.entry(
						colorGetterForKeys,
						ColorPropertyValueHolder.of(colorGetterForKeys.getter(), colors)
				)
			)
			.flatMap(entry ->
					entry.getKey().streamKeys()
							.map(key -> Map.entry(key, entry.getValue().get()))
			)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private final LookAndFeelColors colors;

	public ConfigurableMetalOceanLookAndFeel(LookAndFeelColors colors) {
		this.colors = colors;
	}

	@Override
	protected void initSystemColorDefaults(UIDefaults table) {
		super.initSystemColorDefaults(table);

		table.putAll(createColorDefaults(colors));
	}

	private record ColorGetterForKeys(
			SerializableColorGetter getter,
			String... keys
	) {
		public Stream<String> streamKeys() {
			return Arrays.stream(keys);
		}
	}

	private static final class ColorGetters {
		private static Theme.SerializableColor get(
				TrackedSerializableColorGetter getter, LookAndFeelColors colors
		) {
			return SerializableColorGetter.of(getter).apply(colors);
		}

		public static Theme.SerializableColor getForeground(LookAndFeelColors colors) {
			return get(LookAndFeelColors::getForeground, colors);
		}

		public static Theme.SerializableColor getBackground(LookAndFeelColors colors) {
			return get(LookAndFeelColors::getForeground, colors);
		}

		public static Theme.SerializableColor getActiveCaption(LookAndFeelColors colors) {
			return get(LookAndFeelColors::getActiveCaption, colors);
		}

		public static Theme.SerializableColor getInactiveCaption(LookAndFeelColors colors) {
			return get(LookAndFeelColors::getInactiveCaption, colors);
		}

		public static Theme.SerializableColor getAccentBase2(LookAndFeelColors colors) {
			final Theme.SerializableColor accentBaseColor = get(LookAndFeelColors::getAccentBaseColor, colors);

			// @accentBase2Color = lighten(saturate(@accentBaseColor,10%),6%)

			return accentBaseColor;
		}

		public static Theme.SerializableColor getAccentCheckmark(LookAndFeelColors colors) {
			final Theme.SerializableColor accentBaseColor = get(LookAndFeelColors::getAccentBaseColor, colors);

			// @accentCheckmarkColor = if(@accentColor, @accentColor, tint(@accentBase2Color,20%))
			// @accentBase2Color = lighten(saturate(@accentBaseColor,10%),6%)

			return accentBaseColor;
		}

		public static Theme.SerializableColor getTextInactiveText(LookAndFeelColors colors) {
			final Theme.SerializableColor foreground = get(LookAndFeelColors::getForeground, colors);

			// textInactiveText = @disabledForeground
			// @disabledForeground = tint(@foreground,55%)

			return foreground;
		}

		public static Theme.SerializableColor getDropCellBackground(LookAndFeelColors colors) {
			final Theme.SerializableColor accentBaseColor = get(LookAndFeelColors::getAccentBaseColor, colors);

			// @dropCellBackground = lighten(List.selectionBackground,10%,lazy)
			// "List.selectionBackground", textHighlight,
			// textHighlight = @selectionBackground
			// @selectionBackground = @accentSelectionBackground
			// @accentSelectionBackground = if(@accentColor, @accentColor, @accentBaseColor)

			return accentBaseColor;
		}

		public static Theme.SerializableColor getControlShadow(LookAndFeelColors colors) {
			final Theme.SerializableColor background = get(LookAndFeelColors::getBackground, colors);

			// controlShadow = $Component.borderColor
			// Component.borderColor = shade(@background,20%)

			return background;
		}

		public static Theme.SerializableColor getSeparatorForeground(LookAndFeelColors colors) {
			final Theme.SerializableColor background = get(LookAndFeelColors::getBackground, colors);

			// Separator.foreground = shade(@background,15%)

			return background;
		}

		public static Theme.SerializableColor getTableHeaderBackground(LookAndFeelColors colors) {
			final Theme.SerializableColor background = get(LookAndFeelColors::getBackground, colors);

			// TableHeader.background = @componentBackground
			// @componentBackground = lighten(@background,5%)

			return background;
		}

		public static Theme.SerializableColor getComponentBorder(LookAndFeelColors colors) {
			final Theme.SerializableColor background = get(LookAndFeelColors::getBackground, colors);

			// Component.borderColor = shade(@background,20%)

			return background;
		}

		public static Theme.SerializableColor getDisabledForeground(LookAndFeelColors colors) {
			final Theme.SerializableColor foreground = get(LookAndFeelColors::getForeground, colors);

			// @disabledForeground = tint(@foreground,55%)

			return foreground;
		}

		public static Theme.SerializableColor getOptionPaneQuestionDialogBorderBackground(
				LookAndFeelColors colors
		) {
			final Theme.SerializableColor __ = get(LookAndFeelColors::getForeground, colors);

			// ???

			return __;
		}

		public static Theme.SerializableColor getTabbedPaneTabAreaBackground(
				LookAndFeelColors colors
		) {
			final Theme.SerializableColor __ = get(LookAndFeelColors::getForeground, colors);

			// ???

			return __;
		}

		public static Theme.SerializableColor getOptionPaneQuestionDialogTitlePaneForeground(
				LookAndFeelColors colors
		) {
			final Theme.SerializableColor __ = get(LookAndFeelColors::getForeground, colors);

			// ???

			return __;
		}

		public static Theme.SerializableColor getOptionPaneQuestionDialogTitlePaneShadow(
				LookAndFeelColors colors
		) {
			final Theme.SerializableColor __ = get(LookAndFeelColors::getForeground, colors);

			// ???

			return __;
		}

		public static Theme.SerializableColor getOptionPaneQuestionDialogTitlePaneBackground(
				LookAndFeelColors colors
		) {
			final Theme.SerializableColor __ = get(LookAndFeelColors::getForeground, colors);

			// ???

			return __;
		}

		public static Theme.SerializableColor getOptionPaneWarningDialogTitlePaneForeground(
				LookAndFeelColors colors
		) {
			final Theme.SerializableColor __ = get(LookAndFeelColors::getForeground, colors);

			// ???

			return __;
		}

		public static Theme.SerializableColor getOptionPaneErrorDialogTitlePaneBackground(
				LookAndFeelColors colors
		) {
			final Theme.SerializableColor __ = get(LookAndFeelColors::getForeground, colors);

			// ???

			return __;
		}

		public static Theme.SerializableColor getOptionPaneWarningDialogTitlePaneBackground(
				LookAndFeelColors colors
		) {
			final Theme.SerializableColor __ = get(LookAndFeelColors::getForeground, colors);

			// ???

			return __;
		}

		public static Theme.SerializableColor getOptionPaneErrorDialogBorderBackground(
				LookAndFeelColors colors
		) {
			final Theme.SerializableColor __ = get(LookAndFeelColors::getForeground, colors);

			// ???

			return __;
		}

		public static Theme.SerializableColor getOptionPaneWarningDialogTitlePaneShadow(
				LookAndFeelColors colors
		) {
			final Theme.SerializableColor __ = get(LookAndFeelColors::getForeground, colors);

			// ???

			return __;
		}

		public static Theme.SerializableColor getOptionPaneWarningDialogBorderBackground(
				LookAndFeelColors colors
		) {
			final Theme.SerializableColor __ = get(LookAndFeelColors::getForeground, colors);

			// ???

			return __;
		}

		public static Theme.SerializableColor getOptionPaneErrorDialogTitlePaneShadow(
				LookAndFeelColors colors
		) {
			final Theme.SerializableColor __ = get(LookAndFeelColors::getForeground, colors);

			// ???

			return __;
		}

		public static Theme.SerializableColor getOptionPaneErrorDialogTitlePaneForeground(
				LookAndFeelColors colors
		) {
			final Theme.SerializableColor __ = get(LookAndFeelColors::getForeground, colors);

			// ???

			return __;
		}

		public static Theme.SerializableColor getSliderAltTrackColor(LookAndFeelColors colors) {
			final Theme.SerializableColor __ = get(LookAndFeelColors::getForeground, colors);

			// ???

			return __;
		}
	}

	private static class ColorPropertyValueHolder {
		public static ColorPropertyValueHolder of(
			SerializableColorGetter getter,
			LookAndFeelColors colors
		) {
			return new ColorPropertyValueHolder(getter, colors);
		}

		@Nullable
		private String value;
		private final HexColorStringGetter getter;
		private final LookAndFeelColors colors;

		private ColorPropertyValueHolder(SerializableColorGetter getter, LookAndFeelColors colors) {
			this.getter = HexColorStringGetter.of(getter);
			this.colors = colors;
		}

		public String get() {
			if (value == null) {
				value = getter.apply(colors);
			}

			return value;
		}
	}
}
