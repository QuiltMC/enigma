package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quilt.internal.gui.visualization.util.VisualBox;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;

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
	 * -------
	 * </pre></code>
	 */
	@Override
	public void visualize(JFrame window) {
		window.setLayout(new FlexGridLayout());

		final FlexGridConstraints.Absolute constraints = FlexGridConstraints.createAbsolute();
		window.add(VisualBox.of(Color.RED, 300, 100), constraints.extent(3, 1));
		window.add(VisualBox.of(Color.GREEN, 100, 300), constraints.extent(1, 3));
		window.add(VisualBox.of(Color.BLUE, 200, 200), constraints.extent(2, 2));

		window.pack();
	}
}
