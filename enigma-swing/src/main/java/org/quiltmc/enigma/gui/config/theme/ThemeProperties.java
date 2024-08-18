package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.config.api.values.ComplexConfigValue;
import org.quiltmc.config.api.values.ConfigSerializableObject;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatDarkLaf;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatLightLaf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

public enum ThemeProperties implements ConfigSerializableObject<String> {
	DEFAULT(
		ConfigurableFlatLightLaf::new,
		LookAndFeelColorsFactories::createLight,
		SyntaxPaneColorsFactories::createLight,
		false
	),
	DARCULA(
		ConfigurableFlatDarkLaf::new,
		LookAndFeelColorsFactories::createDarcula,
		SyntaxPaneColorsFactories::createDarcula,
		false
	),
	DARCERULA(
		ConfigurableFlatDarkLaf::new,
		LookAndFeelColorsFactories::createDarcerula,
		SyntaxPaneColorsFactories::createDarcerula,
		false
	),
	METAL(
		unused -> new MetalLookAndFeel(),
		null,
		SyntaxPaneColorsFactories::createLight,
		true
	),
	SYSTEM(
		null,
		null,
		SyntaxPaneColorsFactories::createLight,
		true
	),
	NONE(
		unused -> UIManager.getLookAndFeel(),
		null,
		SyntaxPaneColorsFactories::createLight,
		true
	);

	@Nullable
	public final Function<Theme.LookAndFeelColors, javax.swing.LookAndFeel> lookAndFeelFactory;

	public final Supplier<Theme.LookAndFeelColors.Builder> lookAndFeelColorsFactory;

	public final Supplier<Theme.SyntaxPaneColors.Builder> syntaxPaneColorsFactory;

	// FlatLaf-based LaFs do their own scaling so we don't have to do it.
	// Running swing-dpi for FlatLaf actually breaks fonts, so we let it scale the GUI.
	public final boolean needsScaling;

	ThemeProperties(
			@Nullable Function<Theme.LookAndFeelColors, javax.swing.LookAndFeel> lookAndFeelFactory,
			@Nullable Supplier<Theme.LookAndFeelColors.Builder> lookAndFeelColorsFactory,
			Supplier<Theme.SyntaxPaneColors.Builder> syntaxPaneColorsFactory,
			boolean needsScaling
	) {
		this.lookAndFeelFactory = lookAndFeelFactory;

		this.lookAndFeelColorsFactory = lookAndFeelColorsFactory == null
			? Theme.LookAndFeelColors.Builder::new
			: lookAndFeelColorsFactory;

		this.syntaxPaneColorsFactory = syntaxPaneColorsFactory;

		this.needsScaling = needsScaling;
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
	public ThemeProperties convertFrom(String representation) {
		return ThemeProperties.valueOf(representation);
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