package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;

import javax.swing.JFrame;

public class FlexGridSparseVisualizer implements Visualizer {
	private static final int STEP = 1000;

	private static FlexGridConstraints.Absolute stepColumns(FlexGridConstraints.Absolute constraints) {
		return constraints.advanceColumns(STEP);
	}

	@Override
	public String getTitle() {
		return "Flex Grid Sparse";
	}

	@Override
	public void visualize(JFrame window) {
		FlexGridQuiltVisualiser.visualizeQuilt(
				window,
				c -> c.pos(-STEP, -STEP), FlexGridSparseVisualizer::stepColumns, FlexGridSparseVisualizer::stepColumns,
				c -> c.pos(-STEP, 0), FlexGridSparseVisualizer::stepColumns, FlexGridSparseVisualizer::stepColumns,
				c -> c.pos(-STEP, STEP), FlexGridSparseVisualizer::stepColumns, FlexGridSparseVisualizer::stepColumns
		);

		window.pack();
	}
}
