package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quilt.internal.gui.visualization.util.VisualUtils;

import javax.swing.JFrame;
import java.util.function.UnaryOperator;

public class FlexGridQuiltVisualiser implements Visualizer {
	@Override
	public String getTitle() {
		return "Flex Grid Quilt";
	}

	@Override
	public void visualize(JFrame window) {
		VisualUtils.visualizeFlexGridQuilt(
				window,
				UnaryOperator.identity(), UnaryOperator.identity(), UnaryOperator.identity(),
				UnaryOperator.identity(), UnaryOperator.identity(), UnaryOperator.identity(),
				UnaryOperator.identity(), UnaryOperator.identity(), UnaryOperator.identity()
		);

		window.pack();
	}
}
