package cuchaz.enigma.gui.docker.component;

import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.docker.Docker;

import javax.swing.JToggleButton;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class DockerButton extends JToggleButton {
	private String text;
	private Docker.Side side;

	public DockerButton(String text, Docker.Side side) {
		super("");
		this.text = text;
		this.side = side;
	}

	public void setSide(Docker.Side side) {
		this.side = side;
		this.repaint();
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		Graphics2D g2d = (Graphics2D) g;

		// rotate
		g2d.rotate(-(this.side == Docker.Side.RIGHT ? 3 : 1) * (Math.PI * 0.5));

		// setup text
		Font font = UiConfig.getDefault2Font();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setFont(font);

		// position
		int textSize = (int) font.createGlyphVector(g2d.getFontRenderContext(), text).getVisualBounds().getWidth() + 20;
		this.setPreferredSize(new Dimension(this.getPreferredSize().width, textSize));
		int x = this.side == Docker.Side.RIGHT ? 10 : -textSize + 10;
		int y = this.side == Docker.Side.RIGHT ? -10 : 20;

		g2d.drawString(this.text, x, y);
		this.setSize(this.getPreferredSize());
	}
}
