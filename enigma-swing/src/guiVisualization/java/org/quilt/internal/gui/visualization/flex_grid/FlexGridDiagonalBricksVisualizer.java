package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quilt.internal.gui.visualization.util.VisualBox;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;

import javax.swing.JFrame;
import java.awt.Color;

public class FlexGridDiagonalBricksVisualizer implements Visualizer {
	private static final Color BRICK_COLOR = new Color(170, 74, 68);
	private static final int BRICK_COUNT = 5;

	private static final int BRICK_X_EXTENT = 4;
	private static final int BRICK_Y_EXTENT = 2;

	private static final int HALF_BRICK_X_EXTENT = BRICK_X_EXTENT / 2;

	private static final int SIZE_UNIT = 30;
	private static final int BRICK_WIDTH = BRICK_X_EXTENT * SIZE_UNIT;
	private static final int BRICK_HEIGHT = BRICK_Y_EXTENT * SIZE_UNIT;

	@Override
	public String getTitle() {
		return "Flex Grid Diagonal Bricks";
	}

	@Override
	public void visualize(JFrame window) {
		window.setLayout(new FlexGridLayout());

		final FlexGridConstraints.Absolute constraints = FlexGridConstraints.createAbsolute()
				.extent(BRICK_X_EXTENT, BRICK_Y_EXTENT);

		final int xEnd = HALF_BRICK_X_EXTENT * BRICK_COUNT;
		for (int x = 0, y = 0; x < xEnd; x += HALF_BRICK_X_EXTENT, y += BRICK_Y_EXTENT) {
			window.add(
					VisualBox.of(BRICK_COLOR, BRICK_WIDTH, BRICK_HEIGHT),
					constraints.pos(x, y)
			);
		}

		window.pack();
	}
}
