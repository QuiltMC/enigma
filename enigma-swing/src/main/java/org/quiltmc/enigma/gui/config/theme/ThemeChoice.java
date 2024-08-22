package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.config.api.Config;
import org.quiltmc.config.api.values.ComplexConfigValue;
import org.quiltmc.config.api.values.ConfigSerializableObject;
import org.quiltmc.enigma.gui.config.theme.properties.SyntaxPaneColorProperties;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public enum ThemeChoice implements ConfigSerializableObject<String> {
	DEFAULT(
		false
	),
	DARCULA(
		false
	),
	DARCERULA(
		false
	),
	METAL(
		true
	),
	SYSTEM(
		true
	),
	NONE(
		true
	);

	// FlatLaf-based LaFs do their own scaling so we don't have to do it.
	// Running swing-dpi for FlatLaf actually breaks fonts, so we let it scale the GUI.
	public final boolean needsScaling;

	ThemeChoice(
			boolean needsScaling
	) {

		this.needsScaling = needsScaling;
	}

	private static void testColorBuilder(Config.SectionBuilder builder) {
		final var colors = new SyntaxPaneColorProperties.SyntaxPaneColors.Builder().build();
		colors.stream().forEach(builder::field);
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

	@Override
	public ThemeChoice convertFrom(String representation) {
		return ThemeChoice.valueOf(representation);
	}

	@Override
	public String getRepresentation() {
		return this.name();
	}

	@Override
	public ComplexConfigValue copy() {
		return this;
	}
}
