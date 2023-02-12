package cuchaz.enigma.gui.docker.component;

import cuchaz.enigma.gui.config.UiConfig;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;

public class DockerButton extends JToggleButton {
	private String text;

	public DockerButton(String text) {
		// todo rotation
		super("");
		this.text = text;
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		Graphics2D g2d = (Graphics2D) g;

		// todo better padding solution
		g2d.rotate(-3 * (Math.PI * 0.5));
		Font font = UiConfig.getDefault2Font();
		this.setPreferredSize(new Dimension(this.getPreferredSize().width, (int) font.createGlyphVector(g2d.getFontRenderContext(), text).getVisualBounds().getWidth() + 25));
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.drawString(this.text, 10, -10);
	}
}
