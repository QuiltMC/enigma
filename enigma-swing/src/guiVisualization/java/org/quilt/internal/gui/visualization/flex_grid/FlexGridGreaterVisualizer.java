package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quilt.internal.gui.visualization.util.VisualBox;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;

import javax.swing.JFrame;

public class FlexGridGreaterVisualizer implements Visualizer {
	@Override
	public String getTitle() {
		return "Flex Grid >";
	}

	@Override
	public void visualize(JFrame window) {
		window.setLayout(new FlexGridLayout());

		final FlexGridConstraints.Absolute constraints = FlexGridConstraints.createAbsolute();

		window.add(VisualBox.of(), constraints.pos(0, 2));
		window.add(VisualBox.of(), constraints.pos(0, -2));

		window.add(VisualBox.of(), constraints.pos(1, 1));
		window.add(VisualBox.of(), constraints.pos(1, -1));

		window.add(VisualBox.of(), constraints.pos(2, 0));

		window.pack();
	}
}
