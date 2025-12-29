package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class FlexGridAbsoluteColumnVisualiser implements Visualizer {
	@Override
	public String getTitle() {
		return "Flex Grid Absolute Column";
	}

	@Override
	public void visualize(JFrame window) {
		window.setLayout(new FlexGridLayout());

		final FlexGridConstraints.Absolute constraints = FlexGridConstraints.createAbsolute();
		window.add(new JLabel("Top"), constraints);
		window.add(new JLabel("Middle"), constraints.nextRow());
		window.add(new JLabel("Bottom"), constraints.nextRow());

		window.pack();
	}
}
