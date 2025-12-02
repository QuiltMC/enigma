package org.quiltmc.enigma.gui.dialog.stats;

import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.enigma.gui.util.ScaleUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;

public class StatProgressBar extends JComponent {
	public static final int THICKNESS = ScaleUtil.scale(10);
	public static final int CIRCLE_SIZE = ScaleUtil.scale(100);
	private final double progress;
	private final boolean isCircular;

	public StatProgressBar(double progress, boolean isCircular) {
		this.progress = progress;
		this.isCircular = isCircular;
		if (isCircular) {
			this.addLabel();
		}
	}

	private void addLabel() {
		this.setLayout(new GridBagLayout());
		JLabel label = new JLabel(String.format("%.2f%%", this.progress));
		label.setFont(label.getFont().deriveFont(Font.BOLD, 18.0f));
		this.add(label, GridBagConstraintsBuilder.create().anchor(GridBagConstraints.CENTER).build());
	}

	@Override
	public Dimension getPreferredSize() {
		if (this.isCircular) {
			return new Dimension(CIRCLE_SIZE, CIRCLE_SIZE);
		} else {
			return super.getPreferredSize();
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g.create();

		if (this.isCircular) {
			int startX = (this.getWidth() - CIRCLE_SIZE) / 2;
			int startY = (this.getHeight() - CIRCLE_SIZE) / 2;
			g2.setColor(this.getBackground().darker());
			Area inactiveRing = createRing(startX, startY, CIRCLE_SIZE, CIRCLE_SIZE, 0, 360);
			g2.fill(inactiveRing);
			g2.setColor(new Color(0x19D219));
			Area activeRing = createRing(startX, startY, CIRCLE_SIZE, CIRCLE_SIZE, 0, 360 * this.progress / 100);
			g2.fill(activeRing);
		} else {
			int startY = (this.getHeight() - THICKNESS) / 2;
			g2.setColor(new Color((int) (this.getBackground().getRed() * 0.8), (int) (this.getBackground().getGreen() * 0.8), (int) (this.getBackground().getBlue() * 0.8)));
			g2.fillRoundRect(0, startY, this.getWidth(), THICKNESS, 10, 10);
			g2.setColor(new Color(0x19D219));
			g2.fillRoundRect(0, startY, (int) (this.getWidth() * this.progress / 100), THICKNESS, 10, 10);
		}

		g2.dispose();
	}

	private static Area createRing(int x, int y, int w, int h, double startAngle, double arcAngle) {
		Arc2D outer = new Arc2D.Double(x, y, w, h, startAngle + 90, -arcAngle, Arc2D.PIE);
		Arc2D inner = new Arc2D.Double(x + THICKNESS, y + THICKNESS, w - 2 * THICKNESS, h - 2 * THICKNESS, startAngle + 90, -arcAngle, Arc2D.PIE);
		Area ring = new Area(outer);
		ring.subtract(new Area(inner));
		return ring;
	}
}
