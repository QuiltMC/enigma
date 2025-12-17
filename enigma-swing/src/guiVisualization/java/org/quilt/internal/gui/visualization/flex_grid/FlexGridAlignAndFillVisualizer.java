package org.quilt.internal.gui.visualization.flex_grid;

import org.quilt.internal.gui.visualization.Visualizer;
import org.quilt.internal.gui.visualization.util.VisualUtils;

import javax.swing.JFrame;

public class FlexGridAlignAndFillVisualizer implements Visualizer {
	@Override
	public String getTitle() {
		return "Flex Grid Align & Fill";
	}

	@Override
	public void visualize(JFrame window) {
		VisualUtils.visualizeFlexGridQuilt(
				window,
				c -> c.fillNone().alignTopLeft(),
				c -> c.fillOnlyY().alignTopCenter(),
				c -> c.fillNone().alignTopRight(),

				c -> c.fillOnlyX().alignCenterLeft(),
				c -> c.fillBoth().alignCenter(),
				c -> c.fillOnlyX().alignCenterRight(),

				c -> c.fillNone().alignBottomLeft(),
				c -> c.fillOnlyY().alignBottomCenter(),
				c -> c.fillNone().alignBottomRight()
		);

		window.pack();
	}
}
