package org.quilt.internal.gui.visualization;

import javax.swing.JFrame;

/**
 * A visualizer populates a window with content to help visualize what GUI code does.
 *
 * <p> Visualizers are registered in {@link Main}.
 */
public interface Visualizer {
	/**
	 * The title of the visualizer.
	 *
	 * <p> Used by {@link Main} for the visualizer's button text and window titles.
	 */
	String getTitle();

	/**
	 * Populates the passed {@code window} with content for visualization.
	 */
	void visualize(JFrame window);
}
