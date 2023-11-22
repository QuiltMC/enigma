package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.event.ThemeChangeListener;
import org.quiltmc.enigma.gui.highlight.BoxHighlightPainter;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.syntaxpain.JavaSyntaxKit;

import java.awt.Font;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JEditorPane;
import javax.swing.UIManager;

public class Themes {
	private static final Set<ThemeChangeListener> listeners = new HashSet<>();

	// Calling this after the UI is initialized (e.g. when the user changes
	// theme settings) is currently not functional.
	public static void setupTheme() {
		LookAndFeel laf = Config.getActiveLookAndFeel();
		laf.setGlobalLAF();
		Config.setLookAndFeelDefaults(Config.getLookAndFeel(), LookAndFeel.isDarkLaf());
		Config.snapshotConfig();
		Themes.setFonts();
		UIManager.put("ScrollBar.showButtons", true);
		JEditorPane.registerEditorKitForContentType("text/enigma-sources", JavaSyntaxKit.class.getName());
		Map<TokenType, BoxHighlightPainter> boxHighlightPainters = getBoxHighlightPainters();
		listeners.forEach(l -> l.onThemeChanged(laf, boxHighlightPainters));
		ScaleUtil.applyScaling();
		Config.save();
	}

	private static void setFonts() {
		if (Config.activeUseCustomFonts()) {
			Font small = Config.getSmallFont();
			Font bold = Config.getDefaultFont();
			Font normal = Config.getDefault2Font();

			UIManager.put("CheckBox.font", bold);
			UIManager.put("CheckBoxMenuItem.font", bold);
			UIManager.put("CheckBoxMenuItem.acceleratorFont", small);
			UIManager.put("ColorChooser.font", normal);
			UIManager.put("ComboBox.font", bold);
			UIManager.put("DesktopIcon.font", bold);
			UIManager.put("EditorPane.font", normal);
			UIManager.put("InternalFrame.titleFont", bold);
			UIManager.put("FormattedTextField.font", normal);
			UIManager.put("Label.font", bold);
			UIManager.put("List.font", bold);
			UIManager.put("Menu.acceleratorFont", small);
			UIManager.put("Menu.font", bold);
			UIManager.put("MenuBar.font", bold);
			UIManager.put("MenuItem.acceleratorFont", small);
			UIManager.put("MenuItem.font", bold);
			UIManager.put("OptionPane.font", normal);
			UIManager.put("Panel.font", normal);
			UIManager.put("PasswordField.font", normal);
			UIManager.put("PopupMenu.font", bold);
			UIManager.put("ProgressBar.font", bold);
			UIManager.put("RadioButton.font", bold);
			UIManager.put("RadioButtonMenuItem.acceleratorFont", small);
			UIManager.put("RadioButtonMenuItem.font", bold);
			UIManager.put("ScrollPane.font", normal);
			UIManager.put("Slider.font", bold);
			UIManager.put("Spinner.font", bold);
			UIManager.put("TabbedPane.font", bold);
			UIManager.put("Table.font", normal);
			UIManager.put("TableHeader.font", normal);
			UIManager.put("TextArea.font", normal);
			UIManager.put("TextField.font", normal);
			UIManager.put("TextPane.font", normal);
			UIManager.put("TitledBorder.font", bold);
			UIManager.put("ToggleButton.font", bold);
			UIManager.put("ToolBar.font", bold);
			UIManager.put("ToolTip.font", normal);
			UIManager.put("Tree.font", normal);
			UIManager.put("Viewport.font", normal);
			UIManager.put("Button.font", bold);
		}
	}

	public static Map<TokenType, BoxHighlightPainter> getBoxHighlightPainters() {
		return Map.of(
				TokenType.OBFUSCATED, BoxHighlightPainter.create(Config.getObfuscatedColor(), Config.getObfuscatedOutlineColor()),
				TokenType.JAR_PROPOSED, BoxHighlightPainter.create(Config.getProposedColor(), Config.getProposedOutlineColor()),
				TokenType.DYNAMIC_PROPOSED, BoxHighlightPainter.create(Config.getProposedColor(), Config.getProposedOutlineColor()),
				TokenType.DEOBFUSCATED, BoxHighlightPainter.create(Config.getDeobfuscatedColor(), Config.getDeobfuscatedOutlineColor()),
				TokenType.DEBUG, BoxHighlightPainter.create(Config.getDebugTokenColor(), Config.getDebugTokenOutlineColor())
		);
	}

	public static void addListener(ThemeChangeListener listener) {
		listeners.add(listener);
	}

	public static void removeListener(ThemeChangeListener listener) {
		listeners.remove(listener);
	}
}
