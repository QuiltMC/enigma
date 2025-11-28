package org.quilt.internal.gui.visualization.util;

import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

public class VisualBox extends JPanel {
	public static VisualBox of(@Nullable Color color, int width, int height) {
		return new VisualBox(null, color, width, height);
	}

	private final int width;
	private final int height;

	public VisualBox(@Nullable String name, @Nullable Color color, int width, int height) {
		this.width = width;
		this.height = height;

		this.setBackground(new Color(0, true));

		if (color != null) {
			this.setForeground(color);
		}

		final Color foreground = this.getForeground();

		this.setLayout(new BorderLayout());

		if (name != null) {
			final JLabel nameLabel = new JLabel(name);
			nameLabel.setForeground(foreground);
			this.add(nameLabel, BorderLayout.NORTH);
		}

		final JLabel dimensions = new JLabel("%s x %s".formatted(this.width, this.height));
		dimensions.setForeground(foreground);
		this.add(dimensions, BorderLayout.EAST);

		this.setBorder(BorderFactory.createDashedBorder(foreground, 2, 2, 4, false));
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(this.width, this.height);
	}
}
