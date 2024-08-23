package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.highlight.BoxHighlightPainter;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.syntaxpain.JavaSyntaxKit;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

public final class ThemeUtil {
	private ThemeUtil() { }

	// Calling this after the UI is initialized (e.g. when the user changes
	// theme settings) is currently not functional.
	public static void setupTheme() {
		Config.activeThemeChoice = Config.main().theme.value();
		Config.configureTheme();
		Config.setGlobalLaf();
		ThemeUtil.setFonts();
		Config.updateSyntaxpain();
		UIManager.put("ScrollBar.showButtons", true);
		JEditorPane.registerEditorKitForContentType("text/enigma-sources", JavaSyntaxKit.class.getName());
		ScaleUtil.applyScaling();
	}

	private static void setFonts() {
		Font small = ScaleUtil.scaleFont(Config.currentFonts().small.value());
		Font bold = ScaleUtil.scaleFont(Config.currentFonts().defaultBold.value());
		Font normal = ScaleUtil.scaleFont(Config.currentFonts().defaultNormal.value());

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

	public static Map<TokenType, BoxHighlightPainter> getBoxHighlightPainters() {
		return Map.of(
				TokenType.OBFUSCATED, BoxHighlightPainter.create(Config.getCurrentSyntaxPaneColors().obfuscated.value(), Config.getCurrentSyntaxPaneColors().obfuscatedOutline.value()),
				TokenType.JAR_PROPOSED, BoxHighlightPainter.create(Config.getCurrentSyntaxPaneColors().proposed.value(), Config.getCurrentSyntaxPaneColors().proposedOutline.value()),
				TokenType.DYNAMIC_PROPOSED, BoxHighlightPainter.create(Config.getCurrentSyntaxPaneColors().proposed.value(), Config.getCurrentSyntaxPaneColors().proposedOutline.value()),
				TokenType.DEOBFUSCATED, BoxHighlightPainter.create(Config.getCurrentSyntaxPaneColors().deobfuscated.value(), Config.getCurrentSyntaxPaneColors().deobfuscatedOutline.value()),
				TokenType.DEBUG, BoxHighlightPainter.create(Config.getCurrentSyntaxPaneColors().debugToken.value(), Config.getCurrentSyntaxPaneColors().debugTokenOutline.value())
		);
	}

	public static <T> void resetIfAbsent(TrackedValue<T> value) {
		setIfAbsent(value, value.getDefaultValue());
	}

	public static <T> void setIfAbsent(TrackedValue<T> value, T newValue) {
		if (value.getDefaultValue().equals(value.value())) {
			value.setValue(newValue, true);
		}
	}

	public static boolean isDarkLaf() {
		// a bit of a hack because swing doesn't give any API for that, and we need colors that aren't defined in look and feel
		JPanel panel = new JPanel();
		panel.setSize(new Dimension(10, 10));
		panel.doLayout();

		BufferedImage image = new BufferedImage(panel.getSize().width, panel.getSize().height, BufferedImage.TYPE_INT_RGB);
		panel.printAll(image.getGraphics());

		Color c = new Color(image.getRGB(0, 0));

		// convert the color we got to grayscale
		int b = (int) (0.3 * c.getRed() + 0.59 * c.getGreen() + 0.11 * c.getBlue());
		return b < 85;
	}
}
