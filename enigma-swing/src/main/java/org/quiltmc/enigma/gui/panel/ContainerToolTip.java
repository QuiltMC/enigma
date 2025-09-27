package org.quiltmc.enigma.gui.panel;

import javax.swing.BorderFactory;
import javax.swing.JToolTip;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/**
 * A {@link JToolTip} that does its best to act like a proper container for a root component.
 */
public class ContainerToolTip extends JToolTip {
	private final Component root;

	public ContainerToolTip(Component root) {
		this.root = root;
		this.setLayout(new BorderLayout());
		this.add(this.root);
		this.setBorder(BorderFactory.createEmptyBorder());
	}

	@Override
	public Dimension getPreferredSize() {
		if (this.isPreferredSizeSet()) {
			return super.getPreferredSize();
		} else {
			return this.root.getPreferredSize();
		}
	}
}
