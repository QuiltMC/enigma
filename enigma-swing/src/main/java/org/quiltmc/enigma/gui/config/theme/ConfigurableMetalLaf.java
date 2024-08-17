package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.config.api.values.TrackedValue;

import javax.swing.plaf.metal.MetalLookAndFeel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigurableMetalLaf extends MetalLookAndFeel {
	private static final Map<String, Function<Theme.LookAndFeelColors, TrackedValue<Theme.SerializableColor>>>
		COLOR_GETTERS_BY_KEY = ThemeUtil.createColorGettersByKey(
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

			// TODO: finish replacing these with MetalLookAndFeel keys
			Stream.of("@accentBaseColor"),

			Stream.of("activeCaption"),
			Stream.of("inactiveCaption"),

			Stream.of("Component.error.focusedBorderColor"),
			Stream.of("Component.warning.focusedBorderColor")
		);

	public static final Map<String, List<String>> PROPERTIES_BY_COLOR = Stream.of(
		// Map.entry("foreground", List.of(
		// 	"DesktopIcon.foreground",
		// 	"EditorPane.caretForeground",
		// 	"Menu.selectionForeground",
		// 	"CheckBoxMenuItem.selectionForeground",
		// 	"CheckBox.foreground",
		// 	"FormattedTextField.caretForeground",
		// 	"TextField.caretForeground",
		// 	"OptionPane.messageForeground",
		// 	"Label.foreground",
		// 	"inactiveCaptionText",
		// 	"PasswordField.selectionForeground",
		// 	"CheckBoxMenuItem.acceleratorSelectionForeground",
		// 	"PasswordField.foreground",
		// 	"TextPane.foreground",
		// 	"Viewport.foreground",
		// 	"TabbedPane.foreground",
		// 	"RadioButton.foreground",
		// 	"infoText",
		// 	"MenuItem.foreground",
		// 	"Tree.textForeground",
		// 	"TableHeader.foreground",
		// 	"ScrollPane.foreground",
		// 	"TextArea.caretForeground",
		// 	"textHighlightText",
		// 	"controlText",
		// 	"ComboBox.foreground",
		// 	"MenuItem.selectionForeground",
		// 	"TextArea.selectionForeground",
		// 	"MenuItem.acceleratorSelectionForeground",
		// 	"Panel.foreground",
		// 	"ToolTip.foreground",
		// 	"EditorPane.selectionForeground",
		// 	"List.foreground",
		// 	"activeCaptionText",
		// 	"Tree.foreground",
		// 	"TextField.foreground",
		// 	"FormattedTextField.selectionForeground",
		// 	"textText",
		// 	"ColorChooser.foreground",
		// 	"Button.foreground",
		// 	"Table.focusCellForeground",
		// 	"PasswordField.caretForeground",
		// 	"TextPane.selectionForeground",
		// 	"menuText",
		// 	"TextField.selectionForeground",
		// 	"List.selectionForeground",
		// 	"RadioButtonMenuItem.acceleratorSelectionForeground",
		// 	"ComboBox.selectionForeground",
		// 	"Tree.selectionForeground",
		// 	"TextPane.caretForeground",
		// 	"Table.selectionForeground",
		// 	"ToggleButton.foreground",
		// 	"windowText",
		// 	"CheckBoxMenuItem.foreground",
		// 	"FormattedTextField.foreground",
		// 	"TitledBorder.titleColor",
		// 	"Table.dropLineShortColor",
		// 	"EditorPane.foreground",
		// 	"InternalFrame.activeTitleForeground",
		// 	"TextArea.foreground",
		// 	"PopupMenu.foreground",
		// 	"OptionPane.foreground",
		// 	"RadioButtonMenuItem.selectionForeground",
		// 	"Menu.acceleratorSelectionForeground",
		// 	"Menu.foreground",
		// 	"ToolBar.foreground",
		// 	"RadioButtonMenuItem.foreground",
		// 	"MenuBar.foreground",
		// 	"Table.foreground",
		// 	"InternalFrame.inactiveTitleForeground"
		// )),
		Map.entry("foreground_disabled", List.of(
			"RadioButton.disabledText",
			"Button.toolBarBorderBackground",
			"Label.disabledForeground",
			"CheckBoxMenuItem.disabledForeground",
			"CheckBox.disabledText",
			"ToggleButton.disabledText",
			"RadioButtonMenuItem.disabledForeground",
			"Menu.disabledForeground",
			"MenuItem.disabledForeground",
			"Button.disabledText"
		)),
		Map.entry("foreground_line", List.of(
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
		)),

		// Map.entry("background", List.of(
		// 	"TextArea.background",
		// 	"Table.background",
		// 	"Slider.highlight",
		// 	"RadioButton.highlight",
		// 	"text",
		// 	"MenuBar.highlight",
		// 	"ScrollBar.highlight",
		// 	"ToolBar.light",
		// 	"PasswordField.background",
		// 	"TextPane.background",
		// 	"ToggleButton.highlight",
		// 	"Tree.textBackground",
		// 	"InternalFrame.borderLight",
		// 	"desktop",
		// 	"Desktop.background",
		// 	"SplitPane.highlight",
		// 	"Tree.background",
		// 	"TextField.background",
		// 	"Button.light",
		// 	"InternalFrame.borderHighlight",
		// 	"ToggleButton.light",
		// 	"TextField.light",
		// 	"List.background",
		// 	"window",
		// 	"TabbedPane.selectHighlight",
		// 	"TextField.highlight",
		// 	"Table.focusCellBackground",
		// 	"controlLtHighlight",
		// 	"controlHighlight",
		// 	"Separator.background",
		// 	"Button.highlight",
		// 	"ComboBox.buttonHighlight",
		// 	"Separator.highlight",
		// 	"RadioButton.light",
		// 	"TabbedPane.highlight",
		// 	"FormattedTextField.background",
		// 	"ToolBar.highlight",
		// 	"EditorPane.background"
		// )),
		Map.entry("background_disabled", List.of(
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
		)),
		Map.entry("background_drop_cell", List.of(
			"Table.dropCellBackground",
			"Tree.dropCellBackground",
			"List.dropCellBackground"
		)),

		Map.entry("shadow", List.of(
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
		)),
		Map.entry("shadow_dark", List.of(
			"ScrollBar.thumbDarkShadow",
			"ScrollBar.trackHighlight",
			"InternalFrame.borderDarkShadow",
			"TabbedPane.darkShadow",
			"RadioButton.darkShadow",
			"ToolTip.foregroundInactive",
			"Table.gridColor",
			"controlDkShadow",
			"TextField.darkShadow",
			"Button.darkShadow",
			"ComboBox.buttonDarkShadow",
			"SplitPane.darkShadow",
			"ToggleButton.darkShadow",
			"ScrollBar.darkShadow",
			"ToolBar.darkShadow"
		)),

		Map.entry("input_foreground", List.of(
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
		)),
		Map.entry("input_border", List.of(
			"ToolBar.borderColor",
			"Button.disabledToolBarBorderBackground",
			"MenuBar.borderColor"
		)),
		Map.entry("pane_focused", List.of(
			"TabbedPane.contentAreaColor",
			"TabbedPane.selected",
			"TableHeader.focusCellBackground",
			"SplitPane.dividerFocusColor"
		))
		// ,
		// Map.entry("group_2", List.of(
		// 	"OptionPane.questionDialog.border.background"
		// )),
		// Map.entry("group_3", List.of(
		// 	"SplitPaneDivider.draggingColor"
		// )),
		// Map.entry("group_6", List.of(
		// 	"TabbedPane.tabAreaBackground"
		// )),
		// Map.entry("group_8", List.of(
		// 	"OptionPane.questionDialog.titlePane.foreground"
		// )),
		// Map.entry("group_9", List.of(
		// 	"Slider.tickColor"
		// )),
		// Map.entry("group_10", List.of(
		// 	"OptionPane.questionDialog.titlePane.shadow"
		// )),
		// Map.entry("group_11", List.of(
		// 	"OptionPane.questionDialog.titlePane.background"
		// )),
		// Map.entry("group_15", List.of(
		// 	"OptionPane.warningDialog.titlePane.foreground"
		// )),
		// Map.entry("group_16", List.of(
		// 	"OptionPane.errorDialog.titlePane.background"
		// )),
		// Map.entry("group_17", List.of(
		// 	"OptionPane.warningDialog.titlePane.background"
		// )),
		// Map.entry("group_18", List.of(
		// 	"OptionPane.errorDialog.border.background"
		// )),
		// Map.entry("group_19", List.of(
		// 	"OptionPane.warningDialog.titlePane.shadow"
		// )),
		// Map.entry("group_20", List.of(
		// 	"OptionPane.warningDialog.border.background"
		// )),
		// Map.entry("group_21", List.of(
		// 	"OptionPane.errorDialog.titlePane.shadow"
		// )),
		// Map.entry("group_24", List.of(
		// 	"OptionPane.errorDialog.titlePane.foreground"
		// )),
		// Map.entry("group_26", List.of(
		// 	"Slider.altTrackColor"
		// ))
	).collect(Collectors.toMap(
		Map.Entry::getKey,
		Map.Entry::getValue,
		(l, r) -> { throw new IllegalStateException("oops"); },
		LinkedHashMap::new
	));
}
