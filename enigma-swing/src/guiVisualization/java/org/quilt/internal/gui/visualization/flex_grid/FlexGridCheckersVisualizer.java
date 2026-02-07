package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quilt.internal.gui.visualization.util.VisualBox;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;

import javax.swing.JFrame;
import java.awt.Color;

public class FlexGridCheckersVisualizer implements Visualizer {
	private static final int BOARD_SIZE = 8;

	@Override
	public String getTitle() {
		return "Flex Grid Checkers";
	}

	@Override
	public void visualize(JFrame window) {
		window.setLayout(new FlexGridLayout());

		final FlexGridConstraints.Absolute constraints = FlexGridConstraints.createAbsolute();

		boolean placeOnEvenY = false;
		for (int x = 0; x < BOARD_SIZE; x++) {
			for (int y = 0; y < BOARD_SIZE; y++) {
				if (placeOnEvenY == (y % 2 == 0)) {
					window.add(VisualBox.of(Color.RED), constraints.pos(x, y));
				}
			}

			placeOnEvenY = !placeOnEvenY;
		}

		window.pack();
	}
}
