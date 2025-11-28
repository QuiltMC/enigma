package org.quilt.internal.gui.visualization;

import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;

import javax.swing.JFrame;

public class FlexGridFillVisualizer implements Visualizer {
	@Override
	public String getTitle() {
		return "Flex Grid Fill";
	}

	@Override
	public void visualize(JFrame window) {
		FlexGridQuiltVisualiser.visualizeQuilt(
				window,
				FlexGridConstraints::fillNone, FlexGridConstraints::fillOnlyY, FlexGridConstraints::fillNone,
				FlexGridConstraints::fillOnlyX, FlexGridConstraints::fillBoth, FlexGridConstraints::fillOnlyX,
				FlexGridConstraints::fillNone, FlexGridConstraints::fillOnlyY, FlexGridConstraints::fillNone
		);

		window.pack();
	}
}
