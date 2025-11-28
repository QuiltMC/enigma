package org.quilt.internal.gui.visualization;

import org.quilt.internal.gui.visualization.util.VisualBox;
import org.quiltmc.enigma.gui.util.FlexGridConstraints;
import org.quiltmc.enigma.gui.util.FlexGridLayout;

import javax.swing.JFrame;
import java.awt.Color;

public class FlexGridQuiltVisualiser implements Visualizer {
	private static final int PATCH_SIZE = 100;

	private static final Color PURPLE = new Color(151, 34, 255);
	private static final Color MAGENTA = new Color(220, 41, 221);
	private static final Color CYAN = new Color(39, 162, 253);
	private static final Color BLUE = new Color(51, 68, 255);

	private static VisualBox patchOf(String name, Color color) {
		return VisualBox.of(name, color, PATCH_SIZE, PATCH_SIZE);
	}

	@Override
	public String getTitle() {
		return "Flex Grid Quilt";
	}

	@Override
	public void visualizeWindow(JFrame window) {
		window.setLayout(new FlexGridLayout());

		final FlexGridConstraints.Absolute constraints = FlexGridConstraints.createAbsolute();
		window.add(patchOf("[0, 0]", PURPLE), constraints);
		window.add(patchOf("[1, 0]", MAGENTA), constraints.nextColumn());
		window.add(patchOf("[2, 0]", CYAN), constraints.nextColumn());

		window.add(patchOf("[0, 1]", MAGENTA), constraints.nextRow());
		window.add(patchOf("[1, 1]", CYAN), constraints.nextColumn());
		window.add(patchOf("[2, 1]", BLUE), constraints.nextColumn());

		window.add(patchOf("[0, 2]", PURPLE), constraints.nextRow());
		window.add(patchOf("[1, 2]", BLUE), constraints.nextColumn());
		window.add(patchOf("[2, 2]", PURPLE), constraints.nextColumn());

		window.pack();
	}
}
