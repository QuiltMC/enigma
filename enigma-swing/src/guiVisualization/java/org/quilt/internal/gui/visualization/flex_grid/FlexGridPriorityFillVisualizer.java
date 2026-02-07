package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quilt.internal.gui.visualization.util.VisualUtils;

import javax.swing.JFrame;

public class FlexGridPriorityFillVisualizer implements Visualizer {
	@Override
	public String getTitle() {
		return "Flex Grid Priority Fill";
	}

	@Override
	public void visualize(JFrame window) {
		VisualUtils.visualizeFlexGridQuilt(
				window,
				c -> c.fillOnlyX().incrementPriority(),
				c -> c.fillOnlyY().incrementPriority(),
				c -> c.fillOnlyX().incrementPriority(),

				c -> c.fillOnlyY().incrementPriority(),
				c -> c.fillOnlyX().incrementPriority(),
				c -> c.fillOnlyY().incrementPriority(),

				c -> c.fillOnlyX().incrementPriority(),
				c -> c.fillOnlyY().incrementPriority(),
				c -> c.fillOnlyX().incrementPriority()
		);

		window.pack();
	}
}
