package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quilt.internal.gui.visualization.util.VisualUtils;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints.Absolute;

import javax.swing.JFrame;
import java.util.function.UnaryOperator;

public class FlexGridPriorityVisualizer implements Visualizer {
	@Override
	public String getTitle() {
		return "Flex Grid Priority";
	}

	@Override
	public void visualize(JFrame window) {
		VisualUtils.visualizeFlexGridQuilt(
				window,
				UnaryOperator.identity(), Absolute::incrementPriority, Absolute::incrementPriority,
				Absolute::incrementPriority, Absolute::incrementPriority, Absolute::incrementPriority,
				Absolute::incrementPriority, Absolute::incrementPriority, Absolute::incrementPriority
		);

		window.pack();
	}
}
