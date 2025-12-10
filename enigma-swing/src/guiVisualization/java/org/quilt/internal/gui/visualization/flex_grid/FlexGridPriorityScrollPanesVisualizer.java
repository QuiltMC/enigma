package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quiltmc.enigma.gui.panel.SmartScrollPane;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;

import javax.swing.JFrame;
import javax.swing.JTextArea;

public class FlexGridPriorityScrollPanesVisualizer implements Visualizer {
	@Override
	public String getTitle() {
		return "Flex Grid Priority Scroll Panes";
	}

	@Override
	public void visualize(JFrame window) {
		window.setLayout(new FlexGridLayout());

		final FlexGridConstraints.Absolute constraints = FlexGridConstraints.createAbsolute();

		final var firstText = new JTextArea(
				"""
				5	4	3	2	1
				4
				3
				2
				1\
				"""
		);

		window.add(new SmartScrollPane(firstText), constraints);

		final var secondText = new JTextArea(
				"""
				e	d	c	b	a
				d
				c
				b
				a\
				"""
		);

		window.add(new SmartScrollPane(secondText), constraints.nextRow().incrementPriority());

		window.pack();
	}
}
