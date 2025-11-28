package org.quilt.internal.gui.visualization.util;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.gui.docker.component.VerticalFlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class VisualBox extends JPanel {
	public static VisualBox of(@Nullable Color color, int width, int height) {
		return of(null, color, width, height);
	}

	public static VisualBox of(@Nullable String name, @Nullable Color color, int width, int height) {
		return new VisualBox(name, color, width, height, width / 2, height / 2, width * 2, height * 2);
	}

	private final int preferredWidth;
	private final int preferredHeight;

	private final int minWidth;
	private final int minHeight;

	private final int maxWidth;
	private final int maxHeight;

	protected VisualBox(
			@Nullable String name, @Nullable Color color,
			int preferredWidth, int preferredHeight,
			int minWidth, int minHeight,
			int maxWidth, int maxHeight
	) {
		this.preferredWidth = preferredWidth;
		this.preferredHeight = preferredHeight;

		this.minWidth = minWidth;
		this.minHeight = minHeight;

		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;

		this.setBackground(new Color(0, true));

		if (color != null) {
			this.setForeground(color);
		}

		final Color foreground = this.getForeground();

		this.setLayout(new GridBagLayout());
		// this.setLayout(new BorderLayout());

		final var center = new JPanel(new VerticalFlowLayout(5));
		center.setBackground(this.getBackground());

		if (name != null) {
			final JLabel nameLabel = new JLabel(name);
			nameLabel.setForeground(foreground);
			center.add(nameLabel, BorderLayout.WEST);
		}

		final JLabel dimensions = new JLabel("%s x %s".formatted(this.preferredWidth, this.preferredHeight));
		dimensions.setForeground(foreground);
		center.add(dimensions, BorderLayout.EAST);

		this.add(center, new GridBagConstraints());

		this.setBorder(BorderFactory.createDashedBorder(foreground, 2, 2, 4, false));
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(this.preferredWidth, this.preferredHeight);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(this.minWidth, this.minHeight);
	}

	@Override
	public Dimension getMaximumSize() {
		return new Dimension(this.maxWidth, this.maxHeight);
	}
}
