package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quilt.internal.gui.visualization.util.VisualBox;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;

import javax.swing.JFrame;
import java.awt.Color;

public class FlexGridTetrisVisualizer implements Visualizer {
	private static final int SQUARE_SIZE = 50;

	private static final Color I_COLOR = Color.CYAN;
	private static final Color O_COLOR = Color.YELLOW;
	private static final Color T_COLOR = Color.MAGENTA;
	private static final Color J_COLOR = Color.BLUE;
	private static final Color L_COLOR = new Color(255, 128, 0);
	private static final Color S_COLOR = Color.GREEN;
	private static final Color Z_COLOR = Color.RED;

	private static void addPart(JFrame window, Color color, int xExtent, int yExtent, int x, int y) {
		window.add(
				VisualBox.of(color, SQUARE_SIZE * xExtent, SQUARE_SIZE * yExtent),
				FlexGridConstraints.createAbsolute().extent(xExtent, yExtent).pos(x, y)
		);
	}

	@Override
	public String getTitle() {
		return "Flex Grid Tetris";
	}

	@Override
	public void visualize(JFrame window) {
		window.setLayout(new FlexGridLayout());

		// default I
		addPart(window, I_COLOR, 1, 4, 0, 0);

		// inverted T
		addPart(window, T_COLOR, 3, 1, 1, 3);
		addPart(window, T_COLOR, 1, 2, 2, 2);

		// vertical Z
		addPart(window, Z_COLOR, 1, 2, 1, 1);
		addPart(window, Z_COLOR, 1, 2, 2, 0);

		// default J
		addPart(window, J_COLOR, 2, 1, 4, 3);
		addPart(window, J_COLOR, 1, 2, 5, 1);

		// O
		addPart(window, O_COLOR, 2, 2, 3, 1);

		// horizontal I
		addPart(window, I_COLOR, 4, 1, 3, 0);

		// default L
		addPart(window, L_COLOR, 2, 1, 6, 3);
		addPart(window, L_COLOR, 1, 3, 6, 1);

		// vertical S
		addPart(window, S_COLOR, 1, 2, 7, 1);
		addPart(window, S_COLOR, 1, 2, 8, 2);

		window.pack();
	}
}
