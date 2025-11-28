package org.quilt.internal.gui.visualization;

import org.quiltmc.enigma.gui.util.FlexGridConstraints;
import org.quiltmc.enigma.gui.util.FlexGridLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class FlexGridColumnVisualiser extends JFrame {
	public static final String TITLE = "Flex Grid Column";

	public FlexGridColumnVisualiser() {
		super(TITLE);

		this.setLayout(new FlexGridLayout());

		final FlexGridConstraints.Absolute constraints = FlexGridConstraints.createAbsolute();
		this.add(new JLabel("Label 1"), constraints);
		this.add(new JLabel("Label 2"), constraints.nextRow());
		this.add(new JLabel("Label 3"), constraints.nextRow());

		this.pack();
	}
}
