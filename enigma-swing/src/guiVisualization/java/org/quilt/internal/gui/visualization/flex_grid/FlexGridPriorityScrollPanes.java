package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;

public class FlexGridPriorityScrollPanes implements Visualizer {
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
				5
				4
				3
				2
				1\
				"""
		);

		window.add(new JScrollPane(firstText, VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_NEVER), constraints);

		final var secondText = new JTextArea(
				"""
				e
				d
				c
				b
				a\
				"""
		);

		window.add(
				new JScrollPane(secondText, VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_NEVER),
				constraints.nextRow().incrementPriority()
		);

		window.pack();
	}
}
