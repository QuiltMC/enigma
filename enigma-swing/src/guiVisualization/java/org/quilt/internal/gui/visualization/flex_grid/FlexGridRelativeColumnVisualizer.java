package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class FlexGridRelativeColumnVisualizer implements Visualizer {
	@Override
	public String getTitle() {
		return "Flex Grid Relative Column";
	}

	@Override
	public void visualize(JFrame window) {
		window.setLayout(new FlexGridLayout(FlexGridConstraints.Relative.Placement.NEW_ROW));

		window.add(new JLabel("Top"));
		window.add(new JLabel("Middle"));
		window.add(new JLabel("Bottom"));

		window.pack();
	}
}
