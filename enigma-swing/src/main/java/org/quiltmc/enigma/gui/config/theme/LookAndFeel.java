package org.quiltmc.enigma.gui.config.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import org.quiltmc.config.api.values.ComplexConfigValue;
import org.quiltmc.config.api.values.ConfigSerializableObject;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

public enum LookAndFeel implements ConfigSerializableObject<String> {
	DEFAULT(false, ConfigurableFlatLightLaf::new),
	DARCULA(false, unused -> new FlatDarkLaf()),
	DARCERULA(false, unused -> new FlatDarkLaf()),
	METAL(true, unused -> new MetalLookAndFeel()),
	SYSTEM(true, null),
	NONE(true, unused -> UIManager.getLookAndFeel());

	private final boolean needsScaling;

	@Nullable
	public final Function<Theme.LookAndFeelColors, javax.swing.LookAndFeel> constructor;

	LookAndFeel(boolean needsScaling, Function<Theme.LookAndFeelColors, javax.swing.LookAndFeel> constructor) {
		this.needsScaling = needsScaling;
		this.constructor = constructor;
	}

	public boolean needsScaling() {
		// FlatLaf-based LaFs do their own scaling so we don't have to do it.
		// Running swing-dpi for FlatLaf actually breaks fonts, so we let it scale the GUI.
		return this.needsScaling;
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
	public LookAndFeel convertFrom(String representation) {
		return LookAndFeel.valueOf(representation);
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
