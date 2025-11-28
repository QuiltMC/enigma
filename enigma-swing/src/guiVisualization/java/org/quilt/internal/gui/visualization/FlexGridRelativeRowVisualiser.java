package org.quilt.internal.gui.visualization;

import org.quiltmc.enigma.gui.util.FlexGridLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class FlexGridRelativeRowVisualiser extends JFrame {
	public static final String TITLE = "Flex Grid Relative Row";

	public FlexGridRelativeRowVisualiser() {
		super(TITLE);

		this.setLayout(new FlexGridLayout());

		this.add(new JLabel("Label 1"));
		this.add(new JLabel("Label 2"));
		this.add(new JLabel("Label 3"));

		this.pack();
	}
}
