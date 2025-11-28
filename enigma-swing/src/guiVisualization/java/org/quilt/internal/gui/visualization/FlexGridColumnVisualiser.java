package org.quilt.internal.gui.visualization;

import org.quiltmc.enigma.gui.util.FlexGridConstraints;
import org.quiltmc.enigma.gui.util.FlexGridLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class FlexGridColumnVisualiser implements Visualizer {
	@Override
	public String getTitle() {
		return "Flex Grid Column";
	}

	@Override
	public void visualizeWindow(JFrame window) {
		window.setLayout(new FlexGridLayout());

		final FlexGridConstraints.Absolute constraints = FlexGridConstraints.createAbsolute();
		window.add(new JLabel("Label 1"), constraints);
		window.add(new JLabel("Label 2"), constraints.nextRow());
		window.add(new JLabel("Label 3"), constraints.nextRow());

		window.pack();
	}
}
