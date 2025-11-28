package org.quilt.internal.gui.visualization;

import org.quilt.internal.gui.visualization.util.VisualBox;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints.Absolute;

import javax.swing.JFrame;

import static org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints.Alignment.BEGIN;
import static org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints.Alignment.CENTER;
import static org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints.Alignment.END;

public class FlexGridAlignmentVisualizer implements Visualizer {
	private static final int SPACER_SIZE = (int) (VisualBox.DEFAULT_SIZE * 1.2);

	private static VisualBox createSpacer() {
		return VisualBox.ofFixed(SPACER_SIZE);
	}

	@Override
	public String getTitle() {
		return "Flex Grid Alignment";
	}

	@Override
	public void visualize(JFrame window) {
		FlexGridQuiltVisualiser.visualizeQuilt(
				window,
				c -> c.align(BEGIN, BEGIN), c -> c.align(CENTER, BEGIN), c -> c.align(END, BEGIN),
				c -> c.align(BEGIN, CENTER), c -> c.align(CENTER, CENTER), c -> c.align(END, CENTER),
				c -> c.align(BEGIN, END), c -> c.align(CENTER, END), c -> c.align(END, END)
		);

		final Absolute constraints = FlexGridConstraints.createAbsolute();
		window.add(createSpacer(), constraints);
		window.add(createSpacer(), constraints.nextColumn());
		window.add(createSpacer(), constraints.nextColumn());

		window.add(createSpacer(), constraints.nextRow());
		window.add(createSpacer(), constraints.nextColumn());
		window.add(createSpacer(), constraints.nextColumn());

		window.add(createSpacer(), constraints.nextRow());
		window.add(createSpacer(), constraints.nextColumn());
		window.add(createSpacer(), constraints.nextColumn());

		window.pack();
	}
}
