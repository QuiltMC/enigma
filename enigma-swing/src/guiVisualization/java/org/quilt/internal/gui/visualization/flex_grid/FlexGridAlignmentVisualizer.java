package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quilt.internal.gui.visualization.util.VisualBox;
import org.quilt.internal.gui.visualization.util.VisualUtils;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints.Absolute;

import javax.swing.JFrame;

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
		VisualUtils.visualizeFlexGridQuilt(
				window,
				Absolute::alignTopLeft, Absolute::alignTopCenter, Absolute::alignTopRight,
				Absolute::alignCenterLeft, Absolute::alignCenter, Absolute::alignCenterRight,
				Absolute::alignBottomLeft, Absolute::alignBottomCenter, Absolute::alignBottomRight
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
