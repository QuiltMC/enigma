package org.quilt.internal.gui.visualization;

import org.quilt.internal.gui.visualization.util.VisualBox;
import org.quiltmc.enigma.gui.util.FlexGridConstraints;
import org.quiltmc.enigma.gui.util.FlexGridLayout;

import javax.swing.JFrame;
import java.awt.Color;

public class FlexGridOverlapVisualiser implements Visualizer {
	@Override
	public String getTitle() {
		return "Flex Grid Overlap";
	}

	/**
	 * <code><pre>
	 * -------------------
	 * | RGB | RB  |  R  |
	 * -------------------
	 * | GB  |  B  |
	 * -------------
	 * |  G  |
	 * </pre></code>
	 */
	@Override
	public void visualizeWindow(JFrame window) {
		window.setLayout(new FlexGridLayout());

		final FlexGridConstraints.Absolute constraints = FlexGridConstraints.createAbsolute();
		window.add(VisualBox.of(Color.RED, 300, 100), constraints.size(3, 1));
		window.add(VisualBox.of(Color.GREEN, 100, 300), constraints.size(1, 3));
		window.add(VisualBox.of(Color.BLUE, 200, 200), constraints.size(2, 2));

		window.pack();
	}
}
