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
		LookAndFeel laf = Config.get().lookAndFeel.value();
		laf.setGlobalLAF();
		Config.currentColors().configure(LookAndFeel.isDarkLaf());
		Themes.setFonts();
		UIManager.put("ScrollBar.showButtons", true);
		JEditorPane.registerEditorKitForContentType("text/enigma-sources", JavaSyntaxKit.class.getName());
		Map<TokenType, BoxHighlightPainter> boxHighlightPainters = getBoxHighlightPainters();
		listeners.forEach(l -> l.onThemeChanged(laf, boxHighlightPainters));
		ScaleUtil.applyScaling();
		Config.updateSyntaxpain();
	}

	private static void setFonts() {
		Font small = Config.currentFonts().small.value();
		Font bold = Config.currentFonts().defaultFont.value();

		UIManager.put("CheckBox.font", bold);
		UIManager.put("CheckBoxMenuItem.font", bold);
		UIManager.put("CheckBoxMenuItem.acceleratorFont", small);
		UIManager.put("ColorChooser.font", bold); //
		UIManager.put("ComboBox.font", bold);
		UIManager.put("DesktopIcon.font", bold);
		UIManager.put("EditorPane.font", bold); //
		UIManager.put("InternalFrame.titleFont", bold);
		UIManager.put("FormattedTextField.font", bold); //
		UIManager.put("Label.font", bold);
		UIManager.put("List.font", bold);
		UIManager.put("Menu.acceleratorFont", small);
		UIManager.put("Menu.font", bold);
		UIManager.put("MenuBar.font", bold);
		UIManager.put("MenuItem.acceleratorFont", small);
		UIManager.put("MenuItem.font", bold);
		UIManager.put("OptionPane.font", bold); //
		UIManager.put("Panel.font", bold); //
		UIManager.put("PasswordField.font", bold); //
		UIManager.put("PopupMenu.font", bold);
		UIManager.put("ProgressBar.font", bold);
		UIManager.put("RadioButton.font", bold);
		UIManager.put("RadioButtonMenuItem.acceleratorFont", small);
		UIManager.put("RadioButtonMenuItem.font", bold);
		UIManager.put("ScrollPane.font", bold); //
		UIManager.put("Slider.font", bold);
		UIManager.put("Spinner.font", bold);
		UIManager.put("TabbedPane.font", bold);
		UIManager.put("Table.font", bold); //
		UIManager.put("TableHeader.font", bold); //
		UIManager.put("TextArea.font", bold); //
		UIManager.put("TextField.font", bold); //
		UIManager.put("TextPane.font", bold); //
		UIManager.put("TitledBorder.font", bold);
		UIManager.put("ToggleButton.font", bold);
		UIManager.put("ToolBar.font", bold);
		UIManager.put("ToolTip.font", bold); //
		UIManager.put("Tree.font", bold); //
		UIManager.put("Viewport.font", bold); //
		UIManager.put("Button.font", bold);
	}

	public static Map<TokenType, BoxHighlightPainter> getBoxHighlightPainters() {
		return Map.of(
				TokenType.OBFUSCATED, BoxHighlightPainter.create(Config.currentColors().obfuscated.value(), Config.currentColors().obfuscatedOutline.value()),
				TokenType.JAR_PROPOSED, BoxHighlightPainter.create(Config.currentColors().proposed.value(), Config.currentColors().proposedOutline.value()),
				TokenType.DYNAMIC_PROPOSED, BoxHighlightPainter.create(Config.currentColors().proposed.value(), Config.currentColors().proposedOutline.value()),
				TokenType.DEOBFUSCATED, BoxHighlightPainter.create(Config.currentColors().deobfuscated.value(), Config.currentColors().deobfuscatedOutline.value()),
				TokenType.DEBUG, BoxHighlightPainter.create(Config.currentColors().debugToken.value(), Config.currentColors().debugTokenOutline.value())
		);
	}

	public static void addListener(ThemeChangeListener listener) {
		listeners.add(listener);
	}

	public static void removeListener(ThemeChangeListener listener) {
		listeners.remove(listener);
	}
}
