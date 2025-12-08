package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quilt.internal.gui.visualization.util.VisualBox;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;

import javax.swing.JFrame;
import java.awt.Color;

public class FlexGridRelativeRowsVisualizer implements Visualizer {
	@Override
	public String getTitle() {
		return "Flex Grid Relative Rows";
	}

	@Override
	public void visualize(JFrame window) {
		window.setLayout(new FlexGridLayout());

		window.add(VisualBox.of(Color.RED));
		window.add(VisualBox.of(new Color(255, 128, 0)));
		window.add(VisualBox.of(Color.YELLOW));

		// force next row with absolute
		window.add(VisualBox.of(Color.GREEN), FlexGridConstraints.createAbsolute().y(1));
		window.add(VisualBox.of(Color.BLUE));
		window.add(VisualBox.of(new Color(128, 0, 255)));

		window.pack();
	}
}
