package org.quiltmc.enigma.gui.config.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatSystemProperties;
import org.quiltmc.config.api.values.ComplexConfigValue;
import org.quiltmc.config.api.values.ConfigSerializableObject;
import org.quiltmc.enigma.gui.config.Config;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

public enum LookAndFeel implements ConfigSerializableObject<Integer> {
	DEFAULT(false),
	DARCULA(false),
	METAL(true),
	SYSTEM(true),
	NONE(true);

	// the "JVM default" look and feel, get it at the beginning and store it so we can set it later
	private static final javax.swing.LookAndFeel NONE_LAF = UIManager.getLookAndFeel();
	private final boolean needsScaling;

	LookAndFeel(boolean needsScaling) {
		this.needsScaling = needsScaling;
	}

	public boolean needsScaling() {
		// FlatLaf-based LaFs do their own scaling so we don't have to do it.
		// Running swing-dpi for FlatLaf actually breaks fonts, so we let it scale the GUI.
		return this.needsScaling;
	}

	public void setGlobalLAF() {
		// Configure FlatLaf's UI scale to be our scale factor.
		// This is also used for the SVG icons, so it applies even when some other LaF is active.
		System.setProperty(FlatSystemProperties.UI_SCALE, Float.toString(Config.main().scaleFactor.value()));

		try {
			switch (this) {
				case NONE -> UIManager.setLookAndFeel(NONE_LAF);
				case DEFAULT -> UIManager.setLookAndFeel(new FlatLightLaf());
				case METAL -> UIManager.setLookAndFeel(new MetalLookAndFeel());
				case DARCULA -> UIManager.setLookAndFeel(new FlatDarkLaf());
				case SYSTEM -> UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		} catch (Exception e) {
			throw new Error("Failed to set global look and feel", e);
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

	@Override
	public ConfigSerializableObject<Integer> convertFrom(Integer representation) {
		return values()[representation];
	}

	@Override
	public Integer getRepresentation() {
		return this.ordinal();
	}

	@Override
	public ComplexConfigValue copy() {
		return this;
	}
}
