package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quilt.internal.gui.visualization.util.VisualBox;
import org.quilt.internal.gui.visualization.util.VisualUtils;
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
		window.add(VisualBox.of(VisualUtils.ORANGE));
		window.add(VisualBox.of(Color.YELLOW));

		window.add(VisualBox.of(Color.GREEN), FlexGridConstraints.createRelative().newRow());
		window.add(VisualBox.of(Color.BLUE));
		window.add(VisualBox.of(VisualUtils.PURPLE));

		window.pack();
	}
}
