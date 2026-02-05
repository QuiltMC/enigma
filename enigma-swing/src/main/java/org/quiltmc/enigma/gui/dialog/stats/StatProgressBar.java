package org.quiltmc.enigma.gui.dialog.stats;

import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.enigma.gui.util.ScaleUtil;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;

public class StatProgressBar extends JComponent {
	public static final int THICKNESS = ScaleUtil.scale(10);
	public static final int CIRCLE_DIAMETER = ScaleUtil.scale(100);
	public static final int CORNER_RADIUS = ScaleUtil.scale(10);
	public static final Color COLOR = new Color(0x19D219);
	protected final double progress;

	public StatProgressBar(double progress) {
		this.progress = progress;
	}

	protected Color getBackgroundColor() {
		Color background = this.getBackground();
		return new Color((int) (background.getRed() * 0.8), (int) (background.getGreen() * 0.8), (int) (background.getBlue() * 0.8));
	}

	public static class Circular extends StatProgressBar {
		public Circular(double progress) {
			super(progress);
			this.addLabel();
		}

		private void addLabel() {
			this.setLayout(new GridBagLayout());
			JLabel label = new JLabel(String.format("%.2f%%", this.progress));
			label.setFont(label.getFont().deriveFont(Font.BOLD, ScaleUtil.scale(18.0f)));
			this.add(label, GridBagConstraintsBuilder.create().anchor(GridBagConstraints.CENTER).build());
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(CIRCLE_DIAMETER, CIRCLE_DIAMETER);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int startX = (this.getWidth() - CIRCLE_DIAMETER) / 2;
			int startY = (this.getHeight() - CIRCLE_DIAMETER) / 2;
			g2.setColor(this.getBackgroundColor());
			Area backgroundRing = createRing(startX, startY, 0, 360);
			g2.fill(backgroundRing);
			g2.setColor(COLOR);
			Area foregroundRing = createRing(startX, startY, 0, 360 * this.progress / 100);
			g2.fill(foregroundRing);
			g2.dispose();
		}

		private static Area createRing(int x, int y, double startAngle, double arcAngle) {
			Arc2D outer = new Arc2D.Double(x, y, CIRCLE_DIAMETER, CIRCLE_DIAMETER, startAngle + 90, -arcAngle, Arc2D.PIE);
			Arc2D inner = new Arc2D.Double(x + THICKNESS, y + THICKNESS, CIRCLE_DIAMETER - 2 * THICKNESS, CIRCLE_DIAMETER - 2 * THICKNESS, startAngle + 90, -arcAngle, Arc2D.PIE);
			Area ring = new Area(outer);
			ring.subtract(new Area(inner));
			return ring;
		}
	}

	public static class Straight extends StatProgressBar {
		public Straight(double progress) {
			super(progress);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int startY = (this.getHeight() - THICKNESS) / 2;
			g2.setColor(this.getBackgroundColor());
			g2.fillRoundRect(0, startY, this.getWidth(), THICKNESS, CORNER_RADIUS, CORNER_RADIUS);
			g2.setColor(COLOR);
			g2.fillRoundRect(0, startY, (int) (this.getWidth() * this.progress / 100), THICKNESS, CORNER_RADIUS, CORNER_RADIUS);
			g2.dispose();
		}
	}
}
