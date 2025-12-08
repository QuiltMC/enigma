package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quilt.internal.gui.visualization.util.VisualBox;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;

import javax.swing.JFrame;
import java.awt.Color;

public class FlexGridRelativeExtentOverlapVisualizer implements Visualizer {
	public static final int MAX_EXTENT = 3;
	private static final int SQUARE_SIZE = 50;

	@Override
	public String getTitle() {
		return "Flex Grid Relative Extent Overlap";
	}

	/**
	 * <pre><code>
	 * -----------------
	 * |           | R |
	 * |   --------+----
	 * |   |       |
	 * |   |   -----
	 * |   |   |   |
	 * ----+---+----
	 * |   | B |
	 * ---------
	 * </code></pre>
	 */
	@Override
	public void visualize(JFrame window) {
		window.setLayout(new FlexGridLayout());

		for (int coord = 0; coord < MAX_EXTENT; coord++) {
			final int extent = MAX_EXTENT - coord;
			window.add(VisualBox.of(SQUARE_SIZE * extent), FlexGridConstraints.createAbsolute()
					.pos(coord, coord)
					.extent(extent, extent)
			);
		}

		window.add(VisualBox.of(Color.RED, SQUARE_SIZE));

		window.add(VisualBox.of(SQUARE_SIZE), FlexGridConstraints.createAbsolute().y(MAX_EXTENT));
		window.add(VisualBox.of(Color.BLUE, SQUARE_SIZE));

		window.pack();
	}
}
