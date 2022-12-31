package cuchaz.enigma.gui.config;

import java.awt.*;
import java.util.Optional;
import java.util.OptionalInt;

import cuchaz.enigma.config.ConfigContainer;
import cuchaz.enigma.config.ConfigSection;
import cuchaz.enigma.gui.panels.right.RightPanel;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

public final class UiConfig {
	// sections
	public static final String MAIN_WINDOW = "Main Window";
	public static final String GENERAL = "General";
	public static final String LANGUAGE = "Language";
	public static final String SCALE_FACTOR = "Scale Factor";
	public static final String RIGHT_PANEL = "Right Panel";
	public static final String RIGHT_PANEL_DIVIDER_LOCATIONS = "Right Panel Divider Locations";
	public static final String LAYOUT = "Layout";
	public static final String THEMES = "Themes";
	public static final String COLORS = "Colors";
	public static final String DECOMPILER = "Decompiler";
	public static final String FONTS = "Fonts";
	public static final String FILE_DIALOG = "File Dialog";
	public static final String MAPPING_STATS = "Mapping Stats";

	// fields
	public static final String CURRENT = "Current";
	public static final String SELECTED = "Selected";
	public static final String USE_CUSTOM = "Use Custom";
	public static final String DEFAULT = "Default";
	public static final String DEFAULT_2 = "Default 2";
	public static final String SMALL = "Small";
	public static final String EDITOR = "Editor";
	public static final String TOP_LEVEL_PACKAGE = "Top Level Package";
	public static final String SYNTHETIC_PARAMETERS = "Synthetic Parameters";
	public static final String LINE_NUMBERS_FOREGROUND = "Line Numbers Foreground";
	public static final String LINE_NUMBERS_BACKGROUND = "Line Numbers Background";
	public static final String LINE_NUMBERS_SELECTED = "Line Numbers Selected";
	public static final String OBFUSCATED = "Obfuscated";
	public static final String OBFUSCATED_ALPHA = "Obfuscated Alpha";
	public static final String OBFUSCATED_OUTLINE = "Obfuscated Outline";
	public static final String OBFUSCATED_OUTLINE_ALPHA = "Obfuscated Outline Alpha";
	public static final String PROPOSED = "Proposed";
	public static final String PROPOSED_ALPHA = "Proposed Alpha";
	public static final String PROPOSED_OUTLINE = "Proposed Outline";
	public static final String PROPOSED_OUTLINE_ALPHA = "Proposed Outline Alpha";
	public static final String DEOBFUSCATED = "Deobfuscated";
	public static final String DEOBFUSCATED_ALPHA = "Deobfuscated Alpha";
	public static final String DEOBFUSCATED_OUTLINE = "Deobfuscated Outline";
	public static final String DEOBFUSCATED_OUTLINE_ALPHA = "Deobfuscated Outline Alpha";
	public static final String EDITOR_BACKGROUND = "Editor Background";
	public static final String HIGHLIGHT = "Highlight";
	public static final String CARET = "Caret";
	public static final String SELECTION_HIGHLIGHT = "Selection Highlight";
	public static final String STRING = "String";
	public static final String NUMBER = "Number";
	public static final String OPERATOR = "Operator";
	public static final String DELIMITER = "Delimiter";
	public static final String TYPE = "Type";
	public static final String IDENTIFIER = "Identifier";
	public static final String TEXT = "Text";
	public static final String DEBUG_TOKEN = "Debug Token";
	public static final String DEBUG_TOKEN_ALPHA = "Debug Token Alpha";
	public static final String DEBUG_TOKEN_OUTLINE = "Debug Token Outline";
	public static final String DEBUG_TOKEN_OUTLINE_ALPHA = "Debug Token Outline Alpha";

	private UiConfig() {
	}

	// General UI configuration such as localization
	private static final ConfigContainer ui = ConfigContainer.getOrCreate("enigma/enigmaui");
	// Swing specific configuration such as theming
	private static final ConfigContainer swing = ConfigContainer.getOrCreate("enigma/enigmaswing");

	// These are used for getting stuff that needs to stay constant for the
	// runtime of the program, e.g. the current theme, because changing of these
	// settings without a restart isn't implemented correctly yet.
	// Don't change the values in this container with the expectation that they
	// get saved, this is purely a backup of the configuration that existed at
	// startup.
	private static ConfigSection runningSwing;

	static {
		UiConfig.snapshotConfig();
	}

	// Saves the current configuration state so a consistent user interface can
	// be provided for parts of the interface that don't support changing the
	// configuration at runtime. Calling this after any UI elements are
	// displayed can lead to visual glitches!
	public static void snapshotConfig() {
		runningSwing = swing.data().copy();
	}

	public static void save() {
		ui.save();
		swing.save();
	}

	public static String getLanguage() {
		return ui.data().section(GENERAL).setIfAbsentString(LANGUAGE, I18n.DEFAULT_LANGUAGE);
	}

	public static void setLanguage(String language) {
		ui.data().section(GENERAL).setString(LANGUAGE, language);
	}

	public static float getScaleFactor() {
		return (float) swing.data().section(GENERAL).setIfAbsentDouble(SCALE_FACTOR, 1.0);
	}

	public static float getActiveScaleFactor() {
		return (float) runningSwing.section(GENERAL).setIfAbsentDouble(SCALE_FACTOR, 1.0);
	}

	public static void setScaleFactor(float scale) {
		swing.data().section(GENERAL).setDouble(SCALE_FACTOR, scale);
	}

	public static void setSelectedRightPanel(String id) {
		swing.data().section(GENERAL).setString(RIGHT_PANEL, id);
	}

	public static String getSelectedRightPanel() {
		return swing.data().section(GENERAL).setIfAbsentString(RIGHT_PANEL, RightPanel.DEFAULT);
	}

	public static void setRightPanelDividerLocation(String id, int width) {
		swing.data().section(RIGHT_PANEL_DIVIDER_LOCATIONS).setInt(id, width);
	}

	public static int getRightPanelDividerLocation(String id, int defaultLocation) {
		return swing.data().section(RIGHT_PANEL_DIVIDER_LOCATIONS).setIfAbsentInt(id, defaultLocation);
	}

	/**
	 * Gets the dimensions of the different panels of the GUI.
	 * <p>These dimensions are used to determine the location of the separators between these panels.</p>
	 *
	 * <ul>
	 *     <li>[0] - The height of the obfuscated classes panel</li>
	 *     <li>[1] - The width of the classes panel</li>
	 *     <li>[2] - The width of the center panel</li>
	 * </ul>
	 *
	 * @return an integer array composed of these 3 dimensions
	 */
	public static int[] getLayout() {
		return swing.data().section(MAIN_WINDOW).getIntArray(LAYOUT).orElseGet(() -> new int[] { -1, -1, -1 });
	}

	public static void setLayout(int leftV, int left, int right) {
		swing.data().section(MAIN_WINDOW).setIntArray(LAYOUT, new int[] { leftV, left, right });
	}

	public static LookAndFeel getLookAndFeel() {
		return swing.data().section(THEMES).setIfAbsentEnum(LookAndFeel::valueOf, CURRENT, LookAndFeel.NONE);
	}

	public static LookAndFeel getActiveLookAndFeel() {
		return runningSwing.section(THEMES).setIfAbsentEnum(LookAndFeel::valueOf, CURRENT, LookAndFeel.NONE);
	}

	public static void setLookAndFeel(LookAndFeel laf) {
		swing.data().section(THEMES).setEnum(CURRENT, laf);
	}

	public static Decompiler getDecompiler() {
		return ui.data().section(DECOMPILER).setIfAbsentEnum(Decompiler::valueOf, CURRENT, Decompiler.CFR);
	}

	public static void setDecompiler(Decompiler d) {
		ui.data().section(DECOMPILER).setEnum(CURRENT, d);
	}

	private static Color fromComponents(int rgb, double alpha) {
		int rgba = rgb & 0xFFFFFF | (int) (alpha * 255) << 24;
		return new Color(rgba, true);
	}

	private static Color getThemeColorRgba(String colorName) {
		ConfigSection s = runningSwing.section(THEMES).section(getActiveLookAndFeel().name()).section(COLORS);
		return fromComponents(s.getRgbColor(colorName).orElse(0), s.getDouble(String.format("%s Alpha", colorName)).orElse(0));
	}

	private static Color getThemeColorRgb(String colorName) {
		ConfigSection s = runningSwing.section(THEMES).section(getActiveLookAndFeel().name()).section(COLORS);
		return new Color(s.getRgbColor(colorName).orElse(0));
	}

	public static Color getObfuscatedColor() {
		return getThemeColorRgba("Obfuscated");
	}

	public static Color getObfuscatedOutlineColor() {
		return getThemeColorRgba("Obfuscated Outline");
	}

	public static Color getProposedColor() {
		return getThemeColorRgba("Proposed");
	}

	public static Color getProposedOutlineColor() {
		return getThemeColorRgba("Proposed Outline");
	}

	public static Color getDeobfuscatedColor() {
		return getThemeColorRgba("Deobfuscated");
	}

	public static Color getDeobfuscatedOutlineColor() {
		return getThemeColorRgba("Deobfuscated Outline");
	}

	public static Color getDebugTokenColor() {
		return getThemeColorRgba("Debug Token");
	}

	public static Color getDebugTokenOutlineColor() {
		return getThemeColorRgba("Debug Token Outline");
	}

	public static Color getEditorBackgroundColor() {
		return getThemeColorRgb("Editor Background");
	}

	public static Color getHighlightColor() {
		return getThemeColorRgb("Highlight");
	}

	public static Color getCaretColor() {
		return getThemeColorRgb("Caret");
	}

	public static Color getSelectionHighlightColor() {
		return getThemeColorRgb("Selection Highlight");
	}

	public static Color getStringColor() {
		return getThemeColorRgb("String");
	}

	public static Color getNumberColor() {
		return getThemeColorRgb("Number");
	}

	public static Color getOperatorColor() {
		return getThemeColorRgb("Operator");
	}

	public static Color getDelimiterColor() {
		return getThemeColorRgb("Delimiter");
	}

	public static Color getTypeColor() {
		return getThemeColorRgb("Type");
	}

	public static Color getIdentifierColor() {
		return getThemeColorRgb("Identifier");
	}

	public static Color getTextColor() {
		return getThemeColorRgb("Text");
	}

	public static Color getLineNumbersForegroundColor() {
		return getThemeColorRgb("Line Numbers Foreground");
	}

	public static Color getLineNumbersBackgroundColor() {
		return getThemeColorRgb("Line Numbers Background");
	}

	public static Color getLineNumbersSelectedColor() {
		return getThemeColorRgb("Line Numbers Selected");
	}

	public static boolean useCustomFonts() {
		return swing.data().section(THEMES).section(getActiveLookAndFeel().name()).section(FONTS).setIfAbsentBool(USE_CUSTOM, false);
	}

	public static boolean activeUseCustomFonts() {
		return runningSwing.section(THEMES).section(getActiveLookAndFeel().name()).section(FONTS).setIfAbsentBool(USE_CUSTOM, false);
	}

	public static void setUseCustomFonts(boolean b) {
		swing.data().section(THEMES).section(getActiveLookAndFeel().name()).section(FONTS).setBool(USE_CUSTOM, b);
	}

	public static Optional<Font> getFont(String name) {
		Optional<String> spec = swing.data().section(THEMES).section(getActiveLookAndFeel().name()).section(FONTS).getString(name);
		return spec.map(Font::decode);
	}

	public static Optional<Font> getActiveFont(String name) {
		Optional<String> spec = runningSwing.section(THEMES).section(getActiveLookAndFeel().name()).section(FONTS).getString(name);
		return spec.map(Font::decode);
	}

	public static void setFont(String name, Font font) {
		swing.data().section(THEMES).section(getLookAndFeel().name()).section(FONTS).setString(name, encodeFont(font));
	}

	public static Font getDefaultFont() {
		return getActiveFont(DEFAULT).orElseGet(() -> ScaleUtil.scaleFont(Font.decode(Font.DIALOG).deriveFont(Font.BOLD)));
	}

	public static void setDefaultFont(Font font) {
		setFont(DEFAULT, font);
	}

	public static Font getDefault2Font() {
		return getActiveFont(DEFAULT_2).orElseGet(() -> ScaleUtil.scaleFont(Font.decode(Font.DIALOG)));
	}

	public static void setDefault2Font(Font font) {
		setFont(DEFAULT_2, font);
	}

	public static Font getSmallFont() {
		return getActiveFont(SMALL).orElseGet(() -> ScaleUtil.scaleFont(Font.decode(Font.DIALOG)));
	}

	public static void setSmallFont(Font font) {
		setFont(SMALL, font);
	}

	public static Font getEditorFont() {
		return getActiveFont(EDITOR).orElseGet(UiConfig::getFallbackEditorFont);
	}

	public static void setEditorFont(Font font) {
		setFont(EDITOR, font);
	}

	/**
	 * Gets the fallback editor font.
	 * It is used:
	 * <ul>
	 * <li>when there is no custom editor font chosen</li>
	 * <li>when custom fonts are disabled</li>
	 * </ul>
	 *
	 * @return the fallback editor font
	 */
	public static Font getFallbackEditorFont() {
		return ScaleUtil.scaleFont(Font.decode(Font.MONOSPACED));
	}

	public static String encodeFont(Font font) {
		int style = font.getStyle();
		String s = switch (style) {
			case Font.BOLD | Font.ITALIC -> "bolditalic";
			case Font.BOLD -> "bold";
			case Font.ITALIC -> "italic";
			default -> "plain";
		};

		return String.format("%s-%s-%s", font.getName(), s, font.getSize());
	}

	public static Dimension getWindowSize(String window, Dimension fallback) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		ConfigSection section = swing.data().section(window);
		OptionalInt width = section.getInt(String.format("Width %s", screenSize.width));
		OptionalInt height = section.getInt(String.format("Height %s", screenSize.height));
		if (width.isPresent() && height.isPresent()) {
			return new Dimension(width.getAsInt(), height.getAsInt());
		} else {
			return fallback;
		}
	}

	public static void setWindowSize(String window, Dimension dim) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		ConfigSection section = swing.data().section(window);
		section.setInt(String.format("Width %s", screenSize.width), dim.width);
		section.setInt(String.format("Height %s", screenSize.height), dim.height);
	}

	public static Point getWindowPos(String window, Point fallback) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		ConfigSection section = swing.data().section(window);
		OptionalInt x = section.getInt(String.format("X %s", screenSize.width));
		OptionalInt y = section.getInt(String.format("Y %s", screenSize.height));
		if (x.isPresent() && y.isPresent()) {
			int ix = x.getAsInt();
			int iy = y.getAsInt();

			// Ensure that the position is on the screen.
			if (ix < 0 || iy < 0 || ix > screenSize.width || iy > screenSize.height) {
				return fallback;
			}

			return new Point(ix, iy);
		} else {
			return fallback;
		}
	}

	public static void setWindowPos(String window, Point rect) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		ConfigSection section = swing.data().section(window);
		section.setInt(String.format("X %s", screenSize.width), rect.x);
		section.setInt(String.format("Y %s", screenSize.height), rect.y);
	}

	public static String getLastSelectedDir() {
		return swing.data().section(FILE_DIALOG).getString(SELECTED).orElse("");
	}

	public static void setLastSelectedDir(String directory) {
		swing.data().section(FILE_DIALOG).setString(SELECTED, directory);
	}

	public static String getLastTopLevelPackage() {
		return swing.data().section(MAPPING_STATS).getString(TOP_LEVEL_PACKAGE).orElse("");
	}

	public static void setLastTopLevelPackage(String topLevelPackage) {
		swing.data().section(MAPPING_STATS).setString(TOP_LEVEL_PACKAGE, topLevelPackage);
	}

	public static boolean shouldIncludeSyntheticParameters() {
		return swing.data().section(MAPPING_STATS).setIfAbsentBool(SYNTHETIC_PARAMETERS, false);
	}

	public static void setIncludeSyntheticParameters(boolean b) {
		swing.data().section(MAPPING_STATS).setBool(SYNTHETIC_PARAMETERS, b);
	}

	public static void setLookAndFeelDefaults(LookAndFeel laf, boolean isDark) {
		ConfigSection s = swing.data().section(THEMES).section(laf.name()).section(COLORS);
		if (!isDark) {
			// Defaults found here: https://github.com/Sciss/SyntaxPane/blob/122da367ff7a5d31627a70c62a48a9f0f4f85a0a/src/main/resources/de/sciss/syntaxpane/defaultsyntaxkit/config.properties#L139
			s.setIfAbsentRgbColor(LINE_NUMBERS_FOREGROUND, 0x333300);
			s.setIfAbsentRgbColor(LINE_NUMBERS_BACKGROUND, 0xEEEEFF);
			s.setIfAbsentRgbColor(LINE_NUMBERS_SELECTED, 0xCCCCEE);

			s.setIfAbsentRgbColor(OBFUSCATED, 0xFFDCDC);
			s.setIfAbsentDouble(OBFUSCATED_ALPHA, 1.0);
			s.setIfAbsentRgbColor(OBFUSCATED_OUTLINE, 0xA05050);
			s.setIfAbsentDouble(OBFUSCATED_OUTLINE_ALPHA, 1.0);

			s.setIfAbsentRgbColor(PROPOSED, 0x000000);
			s.setIfAbsentDouble(PROPOSED_ALPHA, 0.15);
			s.setIfAbsentRgbColor(PROPOSED_OUTLINE, 0x000000);
			s.setIfAbsentDouble(PROPOSED_OUTLINE_ALPHA, 0.75);

			s.setIfAbsentRgbColor(DEOBFUSCATED, 0xDCFFDC);
			s.setIfAbsentDouble(DEOBFUSCATED_ALPHA, 1.0);
			s.setIfAbsentRgbColor(DEOBFUSCATED_OUTLINE, 0x50A050);
			s.setIfAbsentDouble(DEOBFUSCATED_OUTLINE_ALPHA, 1.0);

			s.setIfAbsentRgbColor(EDITOR_BACKGROUND, 0xFFFFFF);
			s.setIfAbsentRgbColor(HIGHLIGHT, 0x3333EE);
			s.setIfAbsentRgbColor(CARET, 0x000000);
			s.setIfAbsentRgbColor(SELECTION_HIGHLIGHT, 0x000000);
			s.setIfAbsentRgbColor(STRING, 0xCC6600);
			s.setIfAbsentRgbColor(NUMBER, 0x999933);
			s.setIfAbsentRgbColor(OPERATOR, 0x000000);
			s.setIfAbsentRgbColor(DELIMITER, 0x000000);
			s.setIfAbsentRgbColor(TYPE, 0x000000);
			s.setIfAbsentRgbColor(IDENTIFIER, 0x000000);
			s.setIfAbsentRgbColor(TEXT, 0x000000);

			s.setIfAbsentRgbColor(DEBUG_TOKEN, 0xD9BEF9);
			s.setIfAbsentDouble(DEBUG_TOKEN_ALPHA, 1.0);
			s.setIfAbsentRgbColor(DEBUG_TOKEN_OUTLINE, 0xBD93F9);
			s.setIfAbsentDouble(DEBUG_TOKEN_OUTLINE_ALPHA, 1.0);
		} else {
			// Based off colors found here: https://github.com/dracula/dracula-theme/
			s.setIfAbsentRgbColor(LINE_NUMBERS_FOREGROUND, 0xA4A4A3);
			s.setIfAbsentRgbColor(LINE_NUMBERS_BACKGROUND, 0x313335);
			s.setIfAbsentRgbColor(LINE_NUMBERS_SELECTED, 0x606366);

			s.setIfAbsentRgbColor(OBFUSCATED, 0xFF5555);
			s.setIfAbsentDouble(OBFUSCATED_ALPHA, 0.3);
			s.setIfAbsentRgbColor(OBFUSCATED_OUTLINE, 0xFF5555);
			s.setIfAbsentDouble(OBFUSCATED_OUTLINE_ALPHA, 0.5);

			s.setIfAbsentRgbColor(PROPOSED, 0x606366);
			s.setIfAbsentDouble(PROPOSED_ALPHA, 0.3);
			s.setIfAbsentRgbColor(PROPOSED_OUTLINE, 0x606366);
			s.setIfAbsentDouble(PROPOSED_OUTLINE_ALPHA, 0.5);

			s.setIfAbsentRgbColor(DEOBFUSCATED, 0x50FA7B);
			s.setIfAbsentDouble(DEOBFUSCATED_ALPHA, 0.3);
			s.setIfAbsentRgbColor(DEOBFUSCATED_OUTLINE, 0x50FA7B);
			s.setIfAbsentDouble(DEOBFUSCATED_OUTLINE_ALPHA, 0.5);

			s.setIfAbsentRgbColor(EDITOR_BACKGROUND, 0x282A36);
			s.setIfAbsentRgbColor(HIGHLIGHT, 0xFF79C6);
			s.setIfAbsentRgbColor(CARET, 0xF8F8F2);
			s.setIfAbsentRgbColor(SELECTION_HIGHLIGHT, 0xF8F8F2);
			s.setIfAbsentRgbColor(STRING, 0xF1FA8C);
			s.setIfAbsentRgbColor(NUMBER, 0xBD93F9);
			s.setIfAbsentRgbColor(OPERATOR, 0xF8F8F2);
			s.setIfAbsentRgbColor(DELIMITER, 0xF8F8F2);
			s.setIfAbsentRgbColor(TYPE, 0xF8F8F2);
			s.setIfAbsentRgbColor(IDENTIFIER, 0xF8F8F2);
			s.setIfAbsentRgbColor(TEXT, 0xF8F8F2);

			s.setIfAbsentRgbColor(DEBUG_TOKEN, 0x4B1370);
			s.setIfAbsentDouble(DEBUG_TOKEN_ALPHA, 0.5);
			s.setIfAbsentRgbColor(DEBUG_TOKEN_OUTLINE, 0x701367);
			s.setIfAbsentDouble(DEBUG_TOKEN_OUTLINE_ALPHA, 0.5);
		}
	}
}
