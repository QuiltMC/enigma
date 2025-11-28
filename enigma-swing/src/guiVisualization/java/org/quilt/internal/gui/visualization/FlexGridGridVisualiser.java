package org.quilt.internal.gui.visualization;

import org.quiltmc.enigma.gui.util.FlexGridConstraints;
import org.quiltmc.enigma.gui.util.FlexGridLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class FlexGridGridVisualiser implements Visualizer {
	@Override
	public String getTitle() {
		return "Flex Grid Grid";
	}

	@Override
	public void visualizeWindow(JFrame window) {
		window.setLayout(new FlexGridLayout());

		final FlexGridConstraints.Absolute constraints = FlexGridConstraints.createAbsolute();
		window.add(new JLabel("[0, 0]"), constraints);
		window.add(new JLabel("[1, 0]"), constraints.nextColumn());
		window.add(new JLabel("[2, 0]"), constraints.nextColumn());

		window.add(new JLabel("[0, 1]"), constraints.nextRow());
		window.add(new JLabel("[1, 1]"), constraints.nextColumn());
		window.add(new JLabel("[2, 1]"), constraints.nextColumn());

		window.add(new JLabel("[0, 2]"), constraints.nextRow());
		window.add(new JLabel("[1, 2]"), constraints.nextColumn());
		window.add(new JLabel("[2, 2]"), constraints.nextColumn());

		window.pack();
	}
}
