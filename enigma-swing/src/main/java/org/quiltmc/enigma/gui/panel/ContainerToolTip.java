package org.quiltmc.enigma.gui.panel;

import javax.swing.BorderFactory;
import javax.swing.JToolTip;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/**
 * A {@link JToolTip} that does its best to act like a proper container for a root component.
 */
public class ContainerToolTip<C extends Component> extends JToolTip {
	private final C root;

	public ContainerToolTip(C root) {
		this.root = root;
		this.setLayout(new BorderLayout());
		this.add(this.root);
		this.setBorder(BorderFactory.createEmptyBorder());
	}

	public C getRoot() {
		return this.root;
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
