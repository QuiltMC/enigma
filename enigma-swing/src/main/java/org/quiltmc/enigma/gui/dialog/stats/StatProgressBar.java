package org.quiltmc.enigma.gui.dialog.stats;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class StatProgressBar extends JComponent {
	public static final int HEIGHT = 10;
	private final double progress;

	public StatProgressBar(double progress) {
		this.progress = progress;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		int startY = (this.getHeight() - HEIGHT) / 2;
		g.setColor(Color.GREEN);
		g.fillRoundRect(0, startY, (int) (this.getWidth() * this.progress / 100), HEIGHT, 10, 10);
	}
}
