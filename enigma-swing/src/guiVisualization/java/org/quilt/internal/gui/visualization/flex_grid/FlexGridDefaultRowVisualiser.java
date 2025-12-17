package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class FlexGridDefaultRowVisualiser implements Visualizer {
	@Override
	public String getTitle() {
		return "Flex Grid Default Row";
	}

	@Override
	public void visualize(JFrame window) {
		window.setLayout(new FlexGridLayout());

		window.add(new JLabel("Label 1"));
		window.add(new JLabel("Label 2"));
		window.add(new JLabel("Label 3"));

		window.pack();
	}
}
