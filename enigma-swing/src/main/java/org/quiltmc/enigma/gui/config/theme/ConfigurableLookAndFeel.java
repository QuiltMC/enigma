package org.quiltmc.enigma.gui.config.theme;

import javax.swing.*;
import javax.swing.LookAndFeel;
import javax.swing.plaf.basic.BasicLookAndFeel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigurableLookAndFeel extends BasicLookAndFeel {
	private static final Set<String> CONFIGURABLE_KEYS = Set.of(
		// from BasicLookAndFeel
		"inactiveCaptionText", /* Text color for text in inactive captions (title bars). */
		"inactiveCaptionBorder", /* Border color for inactive caption (title bar) window borders. */
		"window", /* Default color for the interior of windows */
		"windowBorder", /* ??? */
		"windowText", /* ??? */
		"menu", /* Background color for menus */
		"menuText", /* Text color for menus  */
		"text", /* Text background color */
		"textText", /* Text foreground color */
		"textHighlight", /* Text background color when selected */
		"textHighlightText", /* Text color when selected */
		"textInactiveText", /* Text color when disabled */
		"control", /* Default color for controls (buttons, sliders, etc) */
		"controlText", /* Default color for text in controls */
		"controlHighlight", /* Specular highlight (opposite of the shadow) */
		"controlLtHighlight", /* Highlight color for controls */
		"controlShadow", /* Shadow color for controls */
		"controlDkShadow", /* Dark shadow color for controls */
		"scrollbar", /* Scrollbar background (usually the "track") */
		"info", /* ToolTip Background */
		"infoText", /* ToolTip Text */

		// from MetalLookAndFeel initSystemColorDefaults
		"desktop", /* Color of the desktop background */
		"activeCaption", /* Color for captions (title bars) when they are active. */
		"activeCaptionText", /* Text color for text in captions (title bars). */
		"activeCaptionBorder", /* Border color for caption (title bar) window borders. */
		"inactiveCaption", /* Color for captions (title bars) when not active. */

		// from MetalLookAndFeel initComponentDefaults
		// Buttons
		"Button.disabledText",
		"Button.select",
		"Button.focus",

		"CheckBox.disabledText",
		"Checkbox.select",
		"CheckBox.focus",

		"RadioButton.disabledText",
		"RadioButton.select",
		"RadioButton.focus",

		"ToggleButton.select",
		"ToggleButton.disabledText",
		"ToggleButton.focus",

		// ToolTip
		"ToolTip.backgroundInactive",
		"ToolTip.foregroundInactive",

		// Slider Defaults
		"Slider.foreground",
		"Slider.focus",
		"Slider.focusInsets",
		"Slider.focusInputMap",

		// Progress Bar
		"ProgressBar.foreground",
		"ProgressBar.selectionBackground",

		// Combo Box
		"ComboBox.background",
		"ComboBox.foreground",
		"ComboBox.selectionBackground",
		"ComboBox.selectionForeground",

		// Desktop Icon
		"DesktopIcon.foreground",
		"DesktopIcon.background",

		// Titled Border
		"TitledBorder.titleColor",

		// Label
		"Label.foreground",

		// ScrollBar
		"ScrollBar.background",
		"ScrollBar.highlight",
		"ScrollBar.shadow",
		"ScrollBar.darkShadow",
		"ScrollBar.thumb",
		"ScrollBar.thumbShadow",
		"ScrollBar.thumbHighlight",

		// Tabbed Pane
		"TabbedPane.tabAreaBackground",
		"TabbedPane.background",
		"TabbedPane.light",
		"TabbedPane.focus",
		"TabbedPane.selected",
		"TabbedPane.selectHighlight",

		// Table
		"Table.dropLineColor",
		"Table.dropLineShortColor",
		"Table.gridColor",  // grid line color

		// Menu
		"Menu.selectionForeground",
		"Menu.selectionBackground",
		"Menu.disabledForeground",
		"Menu.acceleratorForeground",
		"Menu.acceleratorSelectionForeground",

		// Menu Item
		"MenuItem.selectionForeground",
		"MenuItem.selectionBackground",
		"MenuItem.disabledForeground",
		"MenuItem.acceleratorForeground",
		"MenuItem.acceleratorSelectionForeground",

		// Option Pane Special Dialog Colors, used when MetalRootPaneUI
		// is providing window manipulation widgets.
		"OptionPane.errorDialog.border.background",
		"OptionPane.errorDialog.titlePane.foreground",
		"OptionPane.errorDialog.titlePane.background",
		"OptionPane.errorDialog.titlePane.shadow",
		"OptionPane.questionDialog.border.background",
		"OptionPane.questionDialog.titlePane.foreground",
		"OptionPane.questionDialog.titlePane.background",
		"OptionPane.questionDialog.titlePane.shadow",
		"OptionPane.warningDialog.border.background",
		"OptionPane.warningDialog.titlePane.foreground",
		"OptionPane.warningDialog.titlePane.background",
		"OptionPane.warningDialog.titlePane.shadow",

		// CB & RB Menu Item
		"CheckBoxMenuItem.selectionForeground",
		"CheckBoxMenuItem.selectionBackground",
		"CheckBoxMenuItem.disabledForeground",
		"CheckBoxMenuItem.acceleratorForeground",
		"CheckBoxMenuItem.acceleratorSelectionForeground",

		"RadioButtonMenuItem.selectionForeground",
		"RadioButtonMenuItem.selectionBackground",
		"RadioButtonMenuItem.disabledForeground",
		"RadioButtonMenuItem.acceleratorForeground",
		"RadioButtonMenuItem.acceleratorSelectionForeground",

		// Tree
		"Tree.selectionBorderColor",
		"Tree.line", // horiz lines
		"Tree.hash",  // legs

		// ToolBar
		"ToolBar.background",
		"ToolBar.foreground",
		"ToolBar.dockingBackground",
		"ToolBar.floatingBackground",
		"ToolBar.dockingForeground",
		"ToolBar.floatingForeground"
	);

	private static final Set<Color> TEST_COLORS = Set.of(
		Color.RED,
		Color.ORANGE,
		Color.YELLOW,
		Color.GREEN,
		Color.CYAN,
		Color.BLUE,
		Color.MAGENTA,
		Color.PINK
		,
		Color.LIGHT_GRAY,
		Color.DARK_GRAY,
		Color.BLACK,
		Color.WHITE
	);

	private static final Map<String, List<String>> COLOR_KEY_GROUPS = Map.<String, List<String>>ofEntries(
		Map.entry("group-0", List.of(
			"PasswordField.placeholderForeground",
			"EditorPane.inactiveForeground",
			"RadioButton.disabledText",
			"ComboBox.disabledForeground",
			"InternalFrame.inactiveTitleForeground",
			"FormattedTextField.placeholderForeground",
			"TextField.placeholderForeground",
			"TextPane.inactiveForeground",
			"Label.disabledForeground",
			"MenuItem.disabledForeground",
			"textInactiveText",
			"PasswordField.inactiveForeground",
			"CheckBox.disabledText",
			"Spinner.disabledForeground",
			"TitlePane.inactiveForeground",
			"TabbedPane.disabledForeground",
			"RadioButtonMenuItem.disabledForeground",
			"TabbedPane.closeForeground",
			"TextArea.inactiveForeground",
			"CheckBoxMenuItem.disabledForeground",
			"Menu.disabledForeground",
			"ToggleButton.disabledText",
			"Slider.tickColor",
			"FormattedTextField.inactiveForeground",
			"Button.disabledText",
			"TextField.inactiveForeground"
		)),
		Map.entry("group-1", List.of(
			"Button.toolBarBorderBackground"
		)),
		Map.entry("group-2", List.of(
			"DesktopIcon.foreground",
			"Tree.selectionInactiveForeground",
			"TextField.selectionForeground",
			"MenuItem.foreground",
			"TextField.foreground",
			"Menu.foreground",
			"MenuBar.foreground",
			"PopupMenu.foreground",
			"List.selectionInactiveForeground",
			"PasswordField.revealIconColor",
			"RootPane.foreground",
			"menuText",
			"Menu.acceleratorSelectionForeground",
			"Viewport.foreground",
			"Panel.foreground",
			"TextPane.caretForeground",
			"RadioButton.foreground",
			"Slider.foreground",
			"Tree.foreground",
			"controlText",
			"FormattedTextField.selectionForeground",
			"ScrollPane.foreground",
			"textHighlightText",
			"CheckBoxMenuItem.foreground",
			"windowText",
			"ProgressBar.selectionBackground",
			"TextPane.foreground",
			"TextArea.caretForeground",
			"activeCaptionText",
			"inactiveCaptionText",
			"Button.default.foreground",
			"RadioButtonMenuItem.acceleratorSelectionForeground",
			"PasswordField.caretForeground",
			"TitledBorder.titleColor",
			"TextArea.foreground",
			"Tree.selectionForeground",
			"ColorChooser.foreground",
			"TableHeader.foreground",
			"ToggleButton.foreground",
			"textText",
			"TextField.caretForeground",
			"List.selectionForeground",
			"InternalFrame.activeTitleForeground",
			"RadioButtonMenuItem.selectionForeground",
			"Button.selectedForeground",
			"List.foreground",
			"Menu.selectionForeground",
			"TextArea.selectionForeground",
			"RadioButtonMenuItem.foreground",
			"Table.focusCellForeground",
			"TitlePane.foreground",
			"ToolBar.foreground",
			"ToolTip.foreground",
			"TextPane.selectionForeground",
			"EditorPane.caretForeground",
			"MenuItem.acceleratorSelectionForeground",
			"Table.foreground",
			"PasswordField.selectionForeground",
			"Tree.textForeground",
			"EditorPane.selectionForeground",
			"FormattedTextField.caretForeground",
			"ToggleButton.selectedForeground",
			"Label.foreground",
			"FormattedTextField.foreground",
			"Table.selectionInactiveForeground",
			"Button.foreground",
			"CheckBoxMenuItem.acceleratorSelectionForeground",
			"EditorPane.foreground",
			"MenuItem.selectionForeground",
			"TabbedPane.closeHoverForeground",
			"Spinner.foreground",
			"CheckBox.foreground",
			"windowBorder",
			"OptionPane.foreground",
			"TabbedPane.closePressedForeground",
			"ComboBox.selectionForeground",
			"CheckBoxMenuItem.selectionForeground",
			"PasswordField.foreground",
			"TabbedPane.foreground",
			"ScrollBar.foreground",
			"infoText",
			"Table.selectionForeground",
			"TabbedPane.focus",
			"ComboBox.foreground"
		)),
		Map.entry("group-3", List.of(
			"OptionPane.messageForeground",
			"Table.dropLineShortColor"
		)),
		Map.entry("group-4", List.of(
			"OptionPane.questionDialog.border.background"
		)),
		Map.entry("group-5", List.of(
			"SplitPaneDivider.draggingColor",
			"MenuBar.shadow",
			"SplitPane.shadow",
			"Separator.shadow",
			"TabbedPane.contentAreaColor",
			"ScrollBar.thumbShadow",
			"ComboBox.buttonSeparatorColor",
			"ToolBar.dockingForeground",
			"Label.disabledShadow",
			"Button.shadow",
			"controlShadow",
			"ComboBox.buttonShadow",
			"ToolBar.shadow",
			"ToolBar.floatingForeground",
			"Component.borderColor",
			"Spinner.buttonDisabledSeparatorColor",
			"ComboBox.buttonDisabledSeparatorColor",
			"Spinner.buttonSeparatorColor",
			"RadioButton.shadow",
			"InternalFrame.borderShadow",
			"ToggleButton.shadow",
			"Component.disabledBorderColor",
			"TextField.shadow",
			"Slider.shadow"
		)),
		Map.entry("group-6", List.of(
			"TableHeader.focusCellBackground",
			"Tree.background",
			"ComboBox.background",
			"TextPane.background",
			"CheckBox.icon.background",
			"ComboBox.buttonBackground",
			"TextArea.background",
			"Table.background",
			"Tree.textBackground",
			"TableHeader.background",
			"List.background",
			"Spinner.background",
			"Table.focusCellBackground",
			"PasswordField.background",
			"desktop",
			"text",
			"FormattedTextField.background",
			"CheckBox.icon[filled].checkmarkColor",
			"EditorPane.background",
			"TextField.background",
			"CheckBox.icon.selectedBackground"
		)),
		Map.entry("group-7", List.of(
			"Button.highlight",
			"TabbedPane.highlight",
			"ToolBar.highlight",
			"controlLtHighlight",
			"TitlePane.closePressedForeground",
			"ScrollBar.thumbHighlight",
			"Button.default.background",
			"SplitPane.highlight",
			"MenuBar.highlight",
			"InternalFrame.activeTitleBackground",
			"HelpButton.background",
			"TextField.highlight",
			"Separator.highlight",
			"ToggleButton.highlight",
			"ToggleButton.background",
			"RadioButton.highlight",
			"Button.background",
			"InternalFrame.borderHighlight",
			"InternalFrame.closePressedForeground",
			"TitlePane.closeHoverForeground",
			"Slider.highlight",
			"InternalFrame.closeHoverForeground",
			"ComboBox.buttonHighlight"
		)),
		Map.entry("group-8", List.of(
			"ScrollBar.highlight",
			"Desktop.background",
			"TabbedPane.selectHighlight"
		)),
		Map.entry("group-9", List.of(
			"ToggleButton.light",
			"controlHighlight",
			"InternalFrame.borderLight",
			"TextField.light",
			"ToolBar.light",
			"TabbedPane.light",
			"Button.light",
			"RadioButton.light"
		)),
		Map.entry("group-10", List.of(
			"EditorPane.inactiveBackground",
			"TextField.disabledBackground",
			"TabbedPane.shadow",
			"CheckBox.icon.disabledBackground",
			"TabbedPane.background",
			"ComboBox.disabledBackground",
			"RadioButton.background",
			"menu",
			"InternalFrame.borderColor",
			"EditorPane.disabledBackground",
			"window",
			"RootPane.background",
			"TextPane.inactiveBackground",
			"Viewport.background",
			"Panel.background",
			"ColorChooser.swatchesDefaultRecentColor",
			"Slider.background",
			"ProgressBar.selectionForeground",
			"Separator.background",
			"TextPane.disabledBackground",
			"ToolBar.background",
			"ToolBar.floatingBackground",
			"PasswordField.inactiveBackground",
			"Spinner.disabledBackground",
			"ColorChooser.background",
			"HelpButton.disabledBackground",
			"ToggleButton.disabledBackground",
			"PasswordField.disabledBackground",
			"Button.disabledBackground",
			"TextArea.inactiveBackground",
			"OptionPane.background",
			"FormattedTextField.inactiveBackground",
			"ScrollBar.background",
			"Label.background",
			"TextField.inactiveBackground",
			"control",
			"TextArea.disabledBackground",
			"SplitPane.background",
			"FormattedTextField.disabledBackground",
			"CheckBox.background"
		)),
		Map.entry("group-11", List.of(
			"DesktopIcon.background",
			"TabbedPane.unselectedBackground",
			"ScrollBar.track",
			"ScrollPane.background",
			"scrollbar",
			"ToolTip.backgroundInactive"
		)),
		Map.entry("group-12", List.of(
			"ToolBar.dockingBackground",
			"CheckBoxMenuItem.background",
			"MenuItem.background",
			"MenuBar.background",
			"TabbedPane.buttonHoverBackground",
			"TitlePane.inactiveBackground",
			"ToggleButton.tab.hoverBackground",
			"RadioButtonMenuItem.background",
			"TitlePane.background",
			"TabbedPane.hoverColor",
			"Menu.background",
			"PopupMenu.background"
		)),
		Map.entry("group-13", List.of(
			"InternalFrame.inactiveTitleBackground"
		)),
		Map.entry("group-14", List.of(
			"inactiveCaption",
			"inactiveCaptionBorder"
		)),
		Map.entry("group-15", List.of(
			"ProgressBar.background"
		)),
		Map.entry("group-16", List.of(
			"TabbedPane.tabAreaBackground"
		)),
		Map.entry("group-17", List.of(
			"ToolBar.borderColor",
			"Button.disabledToolBarBorderBackground"
		)),
		Map.entry("group-18", List.of(
			"ToolBar.separatorColor",
			"Separator.foreground",
			"MenuBar.borderColor"
		)),
		Map.entry("group-19", List.of(
			"OptionPane.questionDialog.titlePane.foreground"
		)),
		Map.entry("group-20", List.of(
			"OptionPane.questionDialog.titlePane.shadow"
		)),
		Map.entry("group-21", List.of(
			"OptionPane.questionDialog.titlePane.background"
		)),
		Map.entry("group-22", List.of(
			"RadioButtonMenuItem.acceleratorForeground",
			"TitlePane.embeddedForeground",
			"MenuItem.acceleratorForeground",
			"CheckBoxMenuItem.acceleratorForeground",
			"Menu.acceleratorForeground"
		)),
		Map.entry("group-23", List.of(
			"List.dropLineColor",
			"Tree.dropLineColor",
			"Table.dropLineColor",
			"TabbedPane.borderHightlightColor"
		)),
		Map.entry("group-24", List.of(
			"Table.cellFocusColor",
			"List.cellFocusColor",
			"Tree.selectionBorderColor"
		)),
		Map.entry("group-25", List.of(
			"activeCaptionBorder",
			"activeCaption"
		)),
		Map.entry("group-26", List.of(
			"RadioButton.select",
			"Tree.line",
			"Button.select",
			"ScrollBar.shadow",
			"Checkbox.select",
			"ToggleButton.select"
		)),
		Map.entry("group-27", List.of(
			"ComboBox.selectionBackground",
			"Table.selectionBackground",
			"textHighlight",
			"FormattedTextField.selectionBackground",
			"Menu.selectionBackground",
			"Tree.selectionBackground",
			"TextPane.selectionBackground",
			"List.selectionBackground",
			"RadioButtonMenuItem.selectionBackground",
			"TextArea.selectionBackground",
			"MenuItem.selectionBackground",
			"CheckBoxMenuItem.selectionBackground",
			"PasswordField.selectionBackground",
			"EditorPane.selectionBackground",
			"Component.accentColor",
			"TextField.selectionBackground"
		)),
		Map.entry("group-28", List.of(
			"TableHeader.hoverBackground",
			"Tree.hash"
		)),
		Map.entry("group-29", List.of(
			"ComboBox.buttonEditableBackground",
			"ToolTip.background",
			"info",
			"Spinner.buttonBackground"
		)),
		Map.entry("group-30", List.of(
			"Tree.icon.closedColor",
			"Tree.icon.leafColor",
			"SplitPaneDivider.gripColor",
			"ToolBar.gripColor",
			"Tree.icon.expandedColor",
			"Table.sortIconColor",
			"Tree.icon.collapsedColor",
			"Tree.icon.openColor"
		)),
		Map.entry("group-31", List.of(
			"ComboBox.buttonDarkShadow",
			"ToolBar.darkShadow",
			"ScrollBar.thumbDarkShadow",
			"ToggleButton.darkShadow",
			"ScrollBar.trackHighlight",
			"Button.darkShadow",
			"SplitPane.darkShadow",
			"Slider.focus",
			"TabbedPane.darkShadow",
			"TextField.darkShadow",
			"controlDkShadow",
			"InternalFrame.borderDarkShadow",
			"RadioButton.darkShadow"
		)),
		Map.entry("group-32", List.of(
			"ToolTip.foregroundInactive",
			"ScrollBar.darkShadow"
		)),
		Map.entry("group-33", List.of(
			"Table.gridColor"
		)),
		Map.entry("group-34", List.of(
			"OptionPane.warningDialog.titlePane.foreground"
		)),
		Map.entry("group-35", List.of(
			"OptionPane.errorDialog.titlePane.background"
		)),
		Map.entry("group-36", List.of(
			"OptionPane.warningDialog.titlePane.background"
		)),
		Map.entry("group-37", List.of(
			"OptionPane.errorDialog.border.background"
		)),
		Map.entry("group-38", List.of(
			"OptionPane.warningDialog.titlePane.shadow"
		)),
		Map.entry("group-39", List.of(
			"OptionPane.warningDialog.border.background"
		)),
		Map.entry("group-40", List.of(
			"OptionPane.errorDialog.titlePane.shadow"
		)),
		Map.entry("group-41", List.of(
			"Table.dropCellBackground",
			"Tree.dropCellBackground",
			"List.dropCellBackground"
		)),
		Map.entry("group-42", List.of(
			"Slider.trackValueColor",
			"ProgressBar.foreground",
			"ToggleButton.tab.underlineColor",
			"TabbedPane.underlineColor",
			"MenuItem.underlineSelectionColor",
			"Slider.thumbColor"
		)),
		Map.entry("group-43", List.of(
			"Button.focus",
			"ToggleButton.focus",
			"ScrollBar.thumb",
			"RadioButton.focus",
			"CheckBox.focus"
		)),
		Map.entry("group-44", List.of(
			"OptionPane.errorDialog.titlePane.foreground"
		)),
		Map.entry("group-45", List.of(
			"TabbedPane.selected",
			"SplitPane.dividerFocusColor"
		)),
		Map.entry("group-46", List.of(
			"Slider.altTrackColor"
		)),
		Map.entry("group-47", List.of(
			"CheckBox.icon.disabledBorderColor"
		)),
		Map.entry("group-48", List.of(
			"RootPane.inactiveBorderColor",
			"PopupMenu.hoverScrollArrowBackground",
			"ScrollBar.hoverButtonBackground",
			"TabbedPane.closeHoverBackground"
		)),
		Map.entry("group-49", List.of(
			"ToggleButton.disabledSelectedBackground",
			"HelpButton.hoverBackground",
			"Button.disabledSelectedBackground",
			"Button.hoverBackground"
		)),
		Map.entry("group-50", List.of(
			"Popup.dropShadowColor"
		)),
		Map.entry("group-51", List.of(
			"Menu.icon.arrowColor",
			"ScrollBar.buttonArrowColor",
			"Spinner.buttonArrowColor",
			"PopupMenu.scrollArrowColor",
			"ComboBox.buttonArrowColor",
			"CheckBoxMenuItem.icon.checkmarkColor",
			"SplitPaneDivider.oneTouchArrowColor"
		)),
		Map.entry("group-52", List.of(
			"List.selectionInactiveBackground",
			"Table.selectionInactiveBackground",
			"Tree.selectionInactiveBackground"
		)),
		Map.entry("group-53", List.of(
			"TabbedPane.buttonPressedBackground",
			"ToggleButton.toolbar.pressedBackground",
			"Button.toolbar.pressedBackground"
		)),
		Map.entry("group-54", List.of(
			"TabbedPane.closePressedBackground",
			"Slider.disabledTrackColor",
			"Slider.disabledThumbColor",
			"ScrollBar.pressedButtonBackground"
		)),
		Map.entry("group-55", List.of(
			"Button.borderColor",
			"HelpButton.borderColor",
			"HelpButton.disabledBorderColor",
			"Button.disabledBorderColor"
		)),
		Map.entry("group-56", List.of(
			"ToggleButton.toolbar.selectedBackground",
			"InternalFrame.buttonPressedBackground",
			"Button.selectedBackground",
			"Button.toolbar.selectedBackground",
			"ToggleButton.selectedBackground"
		)),
		Map.entry("group-57", List.of(
			"Slider.trackColor"
		)),
		Map.entry("group-58", List.of(
			"ScrollBar.hoverThumbColor"
		)),
		Map.entry("group-59", List.of(
			"InternalFrame.inactiveBorderColor"
		)),
		Map.entry("group-60", List.of(
			"SplitPaneDivider.oneTouchPressedArrowColor",
			"Spinner.buttonPressedArrowColor",
			"ComboBox.buttonPressedArrowColor"
		)),
		Map.entry("group-61", List.of(
			"ScrollBar.pressedThumbColor",
			"HelpButton.disabledQuestionMarkColor"
		)),
		Map.entry("group-62", List.of(
			"ToggleButton.tab.disabledUnderlineColor",
			"TabbedPane.disabledUnderlineColor"
		)),
		Map.entry("group-63", List.of(
			"CheckBox.icon.borderColor"
		)),
		Map.entry("group-64", List.of(
			"PopupMenu.borderColor"
		)),
		Map.entry("group-65", List.of(
			"Spinner.buttonDisabledArrowColor",
			"ComboBox.buttonDisabledArrowColor",
			"ScrollBar.buttonDisabledArrowColor",
			"Menu.icon.disabledArrowColor",
			"CheckBoxMenuItem.icon.disabledCheckmarkColor"
		)),
		Map.entry("group-66", List.of(
			"CheckBox.icon.disabledCheckmarkColor"
		)),
		Map.entry("group-67", List.of(
			"Spinner.buttonHoverArrowColor",
			"SplitPaneDivider.oneTouchHoverArrowColor",
			"ComboBox.buttonHoverArrowColor"
		)),
		Map.entry("group-68", List.of(
			"InternalFrame.activeBorderColor"
		)),
		Map.entry("group-69", List.of(
			"PasswordField.capsLockIconColor"
		)),
		Map.entry("group-70", List.of(
			"Actions.Grey"
		)),
		Map.entry("group-71", List.of(
			"RootPane.activeBorderColor"
		)),
		Map.entry("group-72", List.of(
			"Button.toolbar.hoverBackground",
			"ToggleButton.toolbar.hoverBackground"
		)),
		Map.entry("group-73", List.of(
			"ToggleButton.pressedBackground",
			"CheckBox.icon.pressedBackground",
			"TableHeader.pressedBackground",
			"HelpButton.pressedBackground",
			"TableHeader.separatorColor",
			"Button.pressedBackground",
			"InternalFrame.buttonHoverBackground",
			"TitlePane.buttonHoverBackground",
			"TableHeader.bottomSeparatorColor",
			"Button.default.pressedBackground"
		)),
		Map.entry("group-74", List.of(
			"TitlePane.buttonPressedBackground",
			"MenuBar.hoverBackground",
			"MenuItem.underlineSelectionBackground",
			"ScrollBar.hoverTrackColor"
		)),
		Map.entry("group-75", List.of(
			"ToolBar.hoverButtonGroupBackground"
		)),
		Map.entry("group-76", List.of(
			"Button.default.hoverBackground",
			"CheckBox.icon.hoverBackground"
		)),
		Map.entry("group-77", List.of(
			"Objects.BlackText"
		)),
		Map.entry("group-78", List.of(
			"Button.default.focusedBackground",
			"HelpButton.focusedBackground",
			"Button.focusedBackground",
			"CheckBox.icon[filled].focusedCheckmarkColor",
			"CheckBox.icon.focusedBackground"
		)),
		Map.entry("group-79", List.of(
			"CheckBox.icon.checkmarkColor",
			"CheckBox.icon[filled].selectedBorderColor",
			"CheckBox.icon[filled].selectedBackground",
			"HelpButton.questionMarkColor"
		)),
		Map.entry("group-80", List.of(
			"Component.linkColor"
		)),
		Map.entry("group-81", List.of(
			"Objects.YellowDark"
		)),
		Map.entry("group-82", List.of(
			"Objects.GreenAndroid"
		)),
		Map.entry("group-83", List.of(
			"Objects.Blue"
		)),
		Map.entry("group-84", List.of(
			"Objects.Green"
		)),
		Map.entry("group-85", List.of(
			"Component.error.focusedBorderColor"
		)),
		Map.entry("group-86", List.of(
			"CheckBox.icon[filled].focusedSelectedBackground",
			"Button.default.borderColor",
			"CheckBox.icon.selectedBorderColor"
		)),
		Map.entry("group-87", List.of(
			"Objects.Grey"
		)),
		Map.entry("group-88", List.of(
			"Actions.Green"
		)),
		Map.entry("group-89", List.of(
			"TabbedPane.focusColor",
			"ToggleButton.tab.focusBackground"
		)),
		Map.entry("group-90", List.of(
			"Objects.RedStatus"
		)),
		Map.entry("group-91", List.of(
			"Actions.Red"
		)),
		Map.entry("group-92", List.of(
			"CheckBox.icon[filled].pressedSelectedBackground"
		)),
		Map.entry("group-93", List.of(
			"Objects.Purple"
		)),
		Map.entry("group-94", List.of(
			"HelpButton.hoverBorderColor",
			"CheckBox.icon.pressedBorderColor",
			"CheckBox.icon.focusedBorderColor",
			"CheckBox.icon.hoverBorderColor",
			"HelpButton.focusedBorderColor",
			"Button.focusedBorderColor",
			"Button.hoverBorderColor",
			"Component.focusedBorderColor"
		)),
		Map.entry("group-95", List.of(
			"Objects.Yellow"
		)),
		Map.entry("group-96", List.of(
			"TabbedPane.inactiveUnderlineColor"
		)),
		Map.entry("group-97", List.of(
			"MenuItem.underlineSelectionCheckBackground",
			"MenuItem.checkBackground"
		)),
		Map.entry("group-98", List.of(
			"Objects.Red"
		)),
		Map.entry("group-99", List.of(
			"Slider.hoverThumbColor"
		)),
		Map.entry("group-100", List.of(
			"CheckBox.icon[filled].focusedSelectedBorderColor"
		)),
		Map.entry("group-101", List.of(
			"Component.error.borderColor"
		)),
		Map.entry("group-102", List.of(
			"TitlePane.closePressedBackground"
		)),
		Map.entry("group-103", List.of(
			"Component.warning.focusedBorderColor"
		)),
		Map.entry("group-104", List.of(
			"TitlePane.closeHoverBackground"
		)),
		Map.entry("group-105", List.of(
			"Slider.pressedThumbColor"
		)),
		Map.entry("group-106", List.of(
			"CheckBox.icon[filled].hoverSelectedBackground"
		)),
		Map.entry("group-107", List.of(
			"Button.default.focusedBorderColor",
			"Button.default.hoverBorderColor"
		)),
		Map.entry("group-108", List.of(
			"Objects.Pink"
		)),
		Map.entry("group-109", List.of(
			"Actions.Yellow"
		)),
		Map.entry("group-110", List.of(
			"Actions.Blue"
		)),
		Map.entry("group-111", List.of(
			"Actions.GreyInline"
		)),
		Map.entry("group-112", List.of(
			"Component.focusColor",
			"Button.default.focusColor"
		)),
		Map.entry("group-113", List.of(
			"Component.warning.borderColor"
		)),
		Map.entry("group-114", List.of(
			"Component.custom.borderColor"
		)),
		Map.entry("group-115", List.of(
			"Slider.focusedColor"
		))
	);

	private final javax.swing.LookAndFeel parent;
	private final UIDefaults parentDefaults;
	// private final Map<Object, Object> parentDefaults;
	// private final UIDefaults superDefaults;
	// private final Map<Object, Object> superDefaults;

	public ConfigurableLookAndFeel(javax.swing.LookAndFeel parent) {
		this.parent = parent;
		this.parentDefaults = parent.getDefaults();

		// final var temp = parent.getDefaults();

		// this.parentDefaults = CONFIGURABLE_KEYS.stream()
		// 	.collect(Collectors.toMap(Function.identity(), temp::get));

		// this.superDefaults = super.getDefaults();
		// final var temp = super.getDefaults();
		// this.superDefaults = CONFIGURABLE_KEYS.stream()
		// 	.collect(Collectors.toMap(Function.identity(), temp::get));
	}

	@Override
	public String getName() {
		return "Configurable";
	}

	@Override
	public String getID() {
		return "ConfigurableLookAndFeel - " +  getName();
	}

	@Override
	public String getDescription() {
		return "BasicLookAndFeel with configurable colors";
	}

	@Override
	public boolean isNativeLookAndFeel() {
		return false;
	}

	@Override
	public boolean isSupportedLookAndFeel() {
		return true;
	}

	@Override
	public UIDefaults getDefaults() {
		final UIDefaults defaults = parent.getDefaults();

		this.initSystemColorDefaults(defaults);

		return defaults;
	}

	@Override
	protected void initSystemColorDefaults(UIDefaults table) {
		super.initSystemColorDefaults(table);

		final var parentColors = parentDefaults
			.entrySet()
			.stream()
			.map(entry -> entry.getValue() instanceof Color color ? Optional.of(Map.entry(entry.getKey(), color)) : Optional.<Map.Entry<Object, Color>>empty())
			.flatMap(Optional::stream)
			.toList();

		final var uniqueColors = new HashMap<Color, List<Object>>();
		parentColors.forEach(entry -> {
			final Color color = entry.getValue();
			if (uniqueColors.containsKey(color)) {
				uniqueColors.get(color).add(entry.getKey());
			} else {
				final var keys = new ArrayList<>();
				keys.add(entry.getKey());
				uniqueColors.put(color, keys);
			}
		});

		final Iterator<Color> repeatingTestColors = Stream.generate(TEST_COLORS::stream)
			.flatMap(Function.identity())
			// .limit(CONFIGURABLE_KEYS.size())
			.limit(parentColors.size())
			.toList()
			.iterator();

		// final Map<Object, Object> colors = CONFIGURABLE_KEYS.stream()
		// 		.collect(Collectors.toMap(Function.identity(), unused -> repeatingTestColors.next()));

		final var colors = parentColors.stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				overwritten -> repeatingTestColors.next()
			));

		table.putAll(colors);
	}
}
