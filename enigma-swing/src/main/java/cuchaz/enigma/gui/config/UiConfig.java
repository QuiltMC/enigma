package cuchaz.enigma.gui.config;

import cuchaz.enigma.config.ConfigContainer;
import cuchaz.enigma.config.ConfigSection;
import cuchaz.enigma.gui.NotificationManager;
import cuchaz.enigma.gui.docker.Dock;
import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.Pair;
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

public final class UiConfig {
	// sections
	public static final String MAIN_WINDOW = "Main Window";
	public static final String GENERAL = "General";
	public static final String LANGUAGE = "Language";
	public static final String SCALE_FACTOR = "Scale Factor";
	public static final String VERTICAL_DIVIDER_LOCATIONS = "Vertical Divider Locations";
	public static final String HORIZONTAL_DIVIDER_LOCATIONS = "Horizontal Divider Locations";
	public static final String HOSTED_DOCKERS = "Hosted Dockers";
	public static final String DOCKER_BUTTON_LOCATIONS = "Docker Button Locations";
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
	public static final String SAVED_WITH_LEFT_OPEN = "Saved With Left Open";
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
	public static final String DOCK_HIGHLIGHT = "Dock Highlight";
	public static final String SERVER_NOTIFICATION_LEVEL = "Server Notification Level";
	public static final String RECENT_FILES = "Recent Files";
	public static final String MAX_RECENT_FILES = "Max Recent Files";

	public static final String PAIR_SEPARATOR = ";";
	@Deprecated
	private static final String OLD_PAIR_SEPARATOR = ":";
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

	public static void setHostedDockers(Docker.Side side, Docker[] dockers) {
		String[] dockerData = new String[]{"", ""};
		for (int i = 0; i < dockers.length; i++) {
			Docker docker = dockers[i];

			if (docker != null) {
				Docker.Location location = Dock.Util.findLocation(docker);
				if (location != null) {
					dockerData[i] = (docker.getId() + PAIR_SEPARATOR + location.verticalLocation());
				}
			}
		}

		swing.data().section(HOSTED_DOCKERS).setArray(side.name(), dockerData);
	}

	public static Optional<Map<Docker, Docker.VerticalLocation>> getHostedDockers(Docker.Side side) {
		Optional<String[]> hostedDockers = swing.data().section(HOSTED_DOCKERS).getArray(side.name());

		if (hostedDockers.isEmpty()) {
			return Optional.empty();
		}

		Map<Docker, Docker.VerticalLocation> dockers = new HashMap<>();

		for (String dockInfo : hostedDockers.get()) {
			if (!dockInfo.isBlank()) {
				String[] split = dockInfo.split(dockInfo.contains(PAIR_SEPARATOR) ? PAIR_SEPARATOR : OLD_PAIR_SEPARATOR);

				try {
					Docker.VerticalLocation location = Docker.VerticalLocation.valueOf(split[1]);
					Docker docker = Docker.getDocker(split[0]);

					dockers.put(docker, location);
				} catch (Exception e) {
					Logger.error("failed to read docker state for {}, ignoring! ({})", dockInfo, e.getMessage());
				}
			}
		}

		return Optional.of(dockers);
	}

	public static void setDockerButtonLocation(Docker docker, Docker.Location location) {
		swing.data().section(DOCKER_BUTTON_LOCATIONS).setString(docker.getId(), location.toString());
	}

	public static Docker.Location getButtonLocation(Docker docker) {
		String location = swing.data().section(DOCKER_BUTTON_LOCATIONS).setIfAbsentString(docker.getId(), docker.getPreferredButtonLocation().toString());

		try {
			return Docker.Location.parse(location);
		} catch (Exception e) {
			Logger.error("invalid docker button location: {}, ignoring!", location);
			setDockerButtonLocation(docker, docker.getPreferredButtonLocation());
			return docker.getPreferredButtonLocation();
		}
	}

	public static void setVerticalDockDividerLocation(Docker.Side side, int location) {
		swing.data().section(VERTICAL_DIVIDER_LOCATIONS).setInt(side.name(), location);
	}

	public static int getVerticalDockDividerLocation(Docker.Side side) {
		return swing.data().section(VERTICAL_DIVIDER_LOCATIONS).setIfAbsentInt(side.name(), 300);
	}

	public static void setHorizontalDividerLocation(Docker.Side side, int location) {
		swing.data().section(HORIZONTAL_DIVIDER_LOCATIONS).setInt(side.name(), location);
	}

	public static int getHorizontalDividerLocation(Docker.Side side) {
		return swing.data().section(HORIZONTAL_DIVIDER_LOCATIONS).setIfAbsentInt(side.name(), side == Docker.Side.LEFT ? 300 : 700);
	}

	public static void setSavedWithLeftOpen(boolean open) {
		swing.data().section(GENERAL).setBool(SAVED_WITH_LEFT_OPEN, open);
	}

	public static boolean getSavedWithLeftOpen() {
		return swing.data().section(GENERAL).setIfAbsentBool(SAVED_WITH_LEFT_OPEN, false);
	}

	public static void setMaxRecentFiles(int max) {
		ui.data().setInt(MAX_RECENT_FILES, max);
	}

	public static int getMaxRecentFiles() {
		return ui.data().setIfAbsentInt(MAX_RECENT_FILES, 10);
	}

	/**
	 * Adds a new file pair first in the recent files list, limiting the new list's size to {@link #MAX_RECENT_FILES}. If the pair is already in the list, moves it to the top.
	 * @param jar a path to the jar being mapped
	 * @param mappings a path to the mappings save location
	 */
	public static void addRecentFilePair(Path jar, Path mappings) {
		var pairs = getRecentFilePairs();
		var pair = new Pair<>(jar, mappings);

		pairs.remove(pair);
		pairs.add(0, pair);

		ui.data().setArray(RECENT_FILES, pairs.stream().limit(getMaxRecentFiles()).map(p -> p.a().toString() + PAIR_SEPARATOR + p.b().toString()).toArray(String[]::new));
	}

	/**
	 * Returns the most recently accessed project.
	 * @return A pair containing the jar path as its left element and the mappings path as its right element.
	 */
	public static Optional<Pair<Path, Path>> getMostRecentFilePair() {
		var recentFilePairs = getRecentFilePairs();
		if (recentFilePairs.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(recentFilePairs.get(0));
	}

	/**
	 * Returns all recently accessed projects, up to a limit of {@link #MAX_RECENT_FILES}.
	 * @return a list of pairs containing the jar path as their left element and the mappings path as their right element.
	 */
	public static List<Pair<Path, Path>> getRecentFilePairs() {
		List<Pair<Path, Path>> pairs = new ArrayList<>();

		String[] pairsArray = ui.data().getArray(RECENT_FILES).orElse(new String[0]);

		for (String filePair : pairsArray) {
			if (!filePair.isBlank()) {
				var pairOptional = parseFilePair(filePair);

				if (pairOptional.isPresent()) {
					pairs.add(pairOptional.get());
				} else {
					Logger.error("failed to read recent file state for {}, ignoring!", filePair);
				}
			}
		}

		return pairs;
	}

	private static Optional<Pair<Path, Path>> parseFilePair(String pair) {
		String[] split = pair.split(pair.contains(PAIR_SEPARATOR) ? PAIR_SEPARATOR : OLD_PAIR_SEPARATOR);

		if (split.length != 2) {
			return Optional.empty();
		}

		String jar = split[0];
		String mappings = split[1];
		return Optional.of(new Pair<>(Paths.get(jar), Paths.get(mappings)));
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
		return ui.data().section(DECOMPILER).setIfAbsentEnum(Decompiler::valueOf, CURRENT, Decompiler.QUILTFLOWER);
	}

	public static void setDecompiler(Decompiler d) {
		ui.data().section(DECOMPILER).setEnum(CURRENT, d);
	}

	public static NotificationManager.ServerNotificationLevel getServerNotificationLevel() {
		return swing.data().section(GENERAL).setIfAbsentEnum(NotificationManager.ServerNotificationLevel::valueOf, SERVER_NOTIFICATION_LEVEL, NotificationManager.ServerNotificationLevel.FULL);
	}

	public static void setServerNotificationLevel(NotificationManager.ServerNotificationLevel level) {
		swing.data().section(GENERAL).setEnum(SERVER_NOTIFICATION_LEVEL, level);
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

	public static Color getHighlightColor() {
		return getThemeColorRgb(HIGHLIGHT);
	}

	public static Color getCaretColor() {
		return getThemeColorRgb(CARET);
	}

	public static Color getSelectionHighlightColor() {
		return getThemeColorRgb(SELECTION_HIGHLIGHT);
	}

	public static Color getStringColor() {
		return getThemeColorRgb(STRING);
	}

	public static Color getNumberColor() {
		return getThemeColorRgb(NUMBER);
	}

	public static Color getOperatorColor() {
		return getThemeColorRgb(OPERATOR);
	}

	public static Color getDelimiterColor() {
		return getThemeColorRgb(DELIMITER);
	}

	public static Color getTypeColor() {
		return getThemeColorRgb(TYPE);
	}

	public static Color getIdentifierColor() {
		return getThemeColorRgb(IDENTIFIER);
	}

	public static Color getTextColor() {
		return getThemeColorRgb(TEXT);
	}

	public static Color getLineNumbersForegroundColor() {
		return getThemeColorRgb(LINE_NUMBERS_FOREGROUND);
	}

	public static Color getLineNumbersBackgroundColor() {
		return getThemeColorRgb(LINE_NUMBERS_BACKGROUND);
	}

	public static Color getLineNumbersSelectedColor() {
		return getThemeColorRgb(LINE_NUMBERS_SELECTED);
	}

	public static Color getDockHighlightColor() {
		return getThemeColorRgb(DOCK_HIGHLIGHT);
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
	}
}
