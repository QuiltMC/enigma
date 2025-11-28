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
	public static final int DEFAULT_SIZE = 100;

	public static final Color PATCH_PURPLE = new Color(151, 34, 255);
	public static final Color PATCH_MAGENTA = new Color(220, 41, 221);
	public static final Color PATCH_CYAN = new Color(39, 162, 253);
	public static final Color PATCH_BLUE = new Color(51, 68, 255);

	public static VisualBox of() {
		return of(null);
	}

	public static VisualBox of(@Nullable Color color) {
		return of(null, color);
	}

	public static VisualBox of(@Nullable String name, @Nullable Color color) {
		return of(name, color, DEFAULT_SIZE);
	}

	public static VisualBox of(int size) {
		return of (null, size);
	}

	public static VisualBox of(@Nullable Color color, int size) {
		return of(null, color, size);
	}

	public static VisualBox of(@Nullable String name, @Nullable Color color, int size) {
		return of(name, color, size, size);
	}

	public static VisualBox of(int width, int height) {
		return of(null, width, height);
	}

	public static VisualBox of(@Nullable Color color, int width, int height) {
		return of(null, color, width, height);
	}

	public static VisualBox of(@Nullable String name, @Nullable Color color, int width, int height) {
		return new VisualBox(name, color, width, height, width / 2, height / 2, width * 2, height * 2);
	}

	public static VisualBox ofFixed() {
		return ofFixed(null);
	}

	public static VisualBox ofFixed(@Nullable Color color) {
		return ofFixed(null, color);
	}

	public static VisualBox ofFixed(@Nullable String name, @Nullable Color color) {
		return ofFixed(name, color, DEFAULT_SIZE);
	}

	public static VisualBox ofFixed(int size) {
		return ofFixed(null, size);
	}

	public static VisualBox ofFixed(@Nullable Color color, int size) {
		return ofFixed(null, color, size);
	}

	public static VisualBox ofFixed(@Nullable String name, @Nullable Color color, int size) {
		return ofFixed(name, color, size, size);
	}

	public static VisualBox ofFixed(int width, int height) {
		return ofFixed(null, width, height);
	}

	public static VisualBox ofFixed(@Nullable Color color, int width, int height) {
		return ofFixed(null, color, width, height);
	}

	public static VisualBox ofFixed(@Nullable String name, @Nullable Color color, int width, int height) {
		return new VisualBox(name, color, width, height, width, height, width, height);
	}

	public static VisualBox purplePatchOf() {
		return purplePatchOf(null);
	}

	public static VisualBox purplePatchOf(@Nullable String name) {
		return of(name, PATCH_PURPLE);
	}

	public static VisualBox magentaPatchOf() {
		return purplePatchOf(null);
	}

	public static VisualBox magentaPatchOf(@Nullable String name) {
		return of(name, PATCH_MAGENTA);
	}

	public static VisualBox cyanPatchOf() {
		return cyanPatchOf(null);
	}

	public static VisualBox cyanPatchOf(@Nullable String name) {
		return of(name, PATCH_CYAN);
	}

	public static VisualBox bluePatchOf() {
		return bluePatchOf(null);
	}

	public static VisualBox bluePatchOf(@Nullable String name) {
		return of(name, PATCH_BLUE);
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
