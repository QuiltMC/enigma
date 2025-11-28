package org.quilt.internal.gui.visualization;

import javax.swing.JFrame;

public interface Visualizer {
	String getTitle();

	void visualize(JFrame window);
}
