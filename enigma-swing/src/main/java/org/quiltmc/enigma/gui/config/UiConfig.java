package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.Config;
import org.quiltmc.config.api.Configs;
import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.api.source.Decompilers;
import org.quiltmc.enigma.gui.dialog.EnigmaQuickFindDialog;
import org.quiltmc.enigma.gui.NotificationManager;
import org.quiltmc.enigma.gui.docker.Dock;
import org.quiltmc.enigma.gui.docker.Docker;
import org.quiltmc.enigma.gui.docker.DockerManager;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Pair;
import org.quiltmc.syntaxpain.SyntaxpainConfiguration;
import org.tinylog.Logger;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public final class UiConfig extends ReflectiveConfig {


	static {
		updateSyntaxpain();
	}

	public final TrackedValue<String> language = this.value(I18n.DEFAULT_LANGUAGE);
	public final TrackedValue<Double> scaleFactor = this.value(1.0);
	public final TrackedValue<DockerConfig> dockerConfig = this.value(new DockerConfig());
	public final TrackedValue<Integer> maxRecentFiles = this.value(10);
	public final TrackedValue<RecentFiles> recentFiles = this.value(new RecentFiles());
	public final TrackedValue<LookAndFeel> lookAndFeel = this.value(LookAndFeel.DEFAULT);
	public final LookAndFeel activeLookAndFeel = lookAndFeel.value();

	public final TrackedValue<NotificationManager.ServerNotificationLevel> serverNotificationLevel = this.value(NotificationManager.ServerNotificationLevel.FULL);

	public final TrackedValue<ThemeColors> defaultColors = this.value(new ThemeColors());
	public final TrackedValue<ThemeColors> darculaColors = this.value(new ThemeColors());
	public final TrackedValue<ThemeColors> metalColors = this.value(new ThemeColors());
	public final TrackedValue<ThemeColors> systemColors = this.value(new ThemeColors());
	public final TrackedValue<ThemeColors> noneColors = this.value(new ThemeColors());


	public static final class Colors extends Section {

	}

	private static Color fromComponents(int rgb, double alpha) {
		int rgba = rgb & 0xFFFFFF | (int) (alpha * 255) << 24;
		return new Color(rgba, true);
	}

	private static Color getThemeColorRgba(String colorName) {
		ConfigSection s = swing.data().section(THEMES).section(getActiveLookAndFeel().name()).section(COLORS);
		return fromComponents(s.getRgbColor(colorName).orElse(0), s.getDouble(String.format("%s Alpha", colorName)).orElse(0));
	}

	private static Color getThemeColorRgb(String colorName) {
		ConfigSection s = swing.data().section(THEMES).section(getActiveLookAndFeel().name()).section(COLORS);
		return new Color(s.getRgbColor(colorName).orElse(0));
	}

	public static Color getObfuscatedColor() {
		return getThemeColorRgba(OBFUSCATED);
	}

	public static Color getObfuscatedOutlineColor() {
		return getThemeColorRgba(OBFUSCATED_OUTLINE);
	}

	public static Color getProposedColor() {
		return getThemeColorRgba(PROPOSED);
	}

	public static Color getProposedOutlineColor() {
		return getThemeColorRgba(PROPOSED_OUTLINE);
	}

	public static Color getDeobfuscatedColor() {
		return getThemeColorRgba(DEOBFUSCATED);
	}

	public static Color getDeobfuscatedOutlineColor() {
		return getThemeColorRgba(DEOBFUSCATED_OUTLINE);
	}

	public static Color getDebugTokenColor() {
		return getThemeColorRgba(DEBUG_TOKEN);
	}

	public static Color getDebugTokenOutlineColor() {
		return getThemeColorRgba(DEBUG_TOKEN_OUTLINE);
	}

	public static Color getEditorBackgroundColor() {
		return getThemeColorRgb(EDITOR_BACKGROUND);
	}

	public static Color getCaretColor() {
		return getThemeColorRgb(CARET);
	}

	public static Color getSelectionHighlightColor() {
		return getThemeColorRgb(SELECTION_HIGHLIGHT);
	}

	public static Color getNumberColor() {
		return getThemeColorRgb(NUMBER);
	}

	public static Color getTextColor() {
		return getThemeColorRgb(TEXT);
	}

	public static Color getDockHighlightColor() {
		return getThemeColorRgb(DOCK_HIGHLIGHT);
	}

	public static boolean useCustomFonts() {
		return swing.data().section(THEMES).section(getActiveLookAndFeel().name()).section(FONTS).setIfAbsentBool(USE_CUSTOM, false);
	}

	public static boolean activeUseCustomFonts() {
		return swing.data().section(THEMES).section(getActiveLookAndFeel().name()).section(FONTS).setIfAbsentBool(USE_CUSTOM, false);
	}

	public static void setUseCustomFonts(boolean b) {
		swing.data().section(THEMES).section(getActiveLookAndFeel().name()).section(FONTS).setBool(USE_CUSTOM, b);
	}

	public static Optional<Font> getFont(String name) {
		Optional<String> spec = swing.data().section(THEMES).section(getActiveLookAndFeel().name()).section(FONTS).getString(name);
		return spec.map(Font::decode);
	}

	public static Optional<Font> getActiveFont(String name) {
		Optional<String> spec = swing.data().section(THEMES).section(getActiveLookAndFeel().name()).section(FONTS).getString(name);
		return spec.map(Font::decode);
	}

	public static void setFont(String name, Font font) {
		swing.data().section(THEMES).section(getLookAndFeel().name()).section(FONTS).setString(name, encodeFont(font));
	}

	public static Font getDefaultFont() {
		return getActiveFont(DEFAULT).orElseGet(() -> ScaleUtil.scaleFont(Font.decode(Font.DIALOG).deriveFont(Font.BOLD)));
	}

	public static Font getDefault2Font() {
		return getActiveFont(DEFAULT_2).orElseGet(() -> ScaleUtil.scaleFont(Font.decode(Font.DIALOG)));
	}

	public static Font getSmallFont() {
		return getActiveFont(SMALL).orElseGet(() -> ScaleUtil.scaleFont(Font.decode(Font.DIALOG)));
	}

	public static Font getEditorFont() {
		return getActiveFont(EDITOR).orElseGet(UiConfig::getFallbackEditorFont);
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

		// theme-dependent colors
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

		// theme-independent colors
		s.setIfAbsentRgbColor(DOCK_HIGHLIGHT, 0x0000FF);
		s.setIfAbsentRgbColor(COMMENT, 0x339933);

		updateSyntaxpain();
	}

	/**
	 * Updates the backend library Syntaxpain, used for code highlighting and other editor things.
	 */
	private static void updateSyntaxpain() {
		SyntaxpainConfiguration.setEditorFont(getEditorFont());
		SyntaxpainConfiguration.setQuickFindDialogFactory(EnigmaQuickFindDialog::new);

		ConfigSection colors = swing.data().section(THEMES).section(getLookAndFeel().name()).section(COLORS);

		SyntaxpainConfiguration.setLineRulerPrimaryColor(colors.getColor(LINE_NUMBERS_FOREGROUND));
		SyntaxpainConfiguration.setLineRulerSecondaryColor(colors.getColor(LINE_NUMBERS_BACKGROUND));
		SyntaxpainConfiguration.setLineRulerSelectionColor(colors.getColor(LINE_NUMBERS_SELECTED));

		SyntaxpainConfiguration.setHighlightColor(colors.getColor(HIGHLIGHT));
		SyntaxpainConfiguration.setStringColor(colors.getColor(STRING));
		SyntaxpainConfiguration.setNumberColor(colors.getColor(NUMBER));
		SyntaxpainConfiguration.setOperatorColor(colors.getColor(OPERATOR));
		SyntaxpainConfiguration.setDelimiterColor(colors.getColor(DELIMITER));
		SyntaxpainConfiguration.setTypeColor(colors.getColor(TYPE));
		SyntaxpainConfiguration.setIdentifierColor(colors.getColor(IDENTIFIER));
		SyntaxpainConfiguration.setCommentColour(colors.getColor(COMMENT));
		SyntaxpainConfiguration.setTextColor(colors.getColor(TEXT));
	}
}
