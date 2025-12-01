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
		g2.setColor(new Color(0x19D219));

		if (this.isCircular) {
			Arc2D outer = new Arc2D.Double(0, 0, CIRCLE_SIZE, CIRCLE_SIZE, 90, -(360.0 * this.progress / 100), Arc2D.PIE);
			Arc2D inner = new Arc2D.Double(THICKNESS, THICKNESS, CIRCLE_SIZE - 2 * THICKNESS, CIRCLE_SIZE - 2 * THICKNESS, 90, -(360.0 * this.progress / 100), Arc2D.PIE);
			Area ring = new Area(outer);
			ring.subtract(new Area(inner));
			g2.fill(ring);
		} else {
			int startY = (this.getHeight() - THICKNESS) / 2;
			g2.fillRoundRect(0, startY, (int) (this.getWidth() * this.progress / 100), THICKNESS, 10, 10);
		}

		g2.dispose();
	}
}
