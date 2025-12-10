package org.quiltmc.enigma.gui.panel;

import javax.swing.JScrollPane;
import java.awt.Component;
import java.awt.Dimension;

/**
 * A {@link JScrollPane} with QoL improvements.
 *
 * <p> Currently it just requests space for its scroll bars in {@link #getPreferredSize()}.
 */
public class SmartScrollPane extends JScrollPane {
	// TODO create constructors using ScrollBarPolicy once #320's MarkableScrollPane is merged
	public SmartScrollPane(Component view) {
		super(view);
	}

	@Override
	public Dimension getPreferredSize() {
		final Dimension size = super.getPreferredSize();

		if (this.verticalScrollBar.isShowing()) {
			size.width += this.verticalScrollBar.getPreferredSize().width;
		}

		if (this.horizontalScrollBar.isShowing()) {
			size.height += this.horizontalScrollBar.getPreferredSize().height;
		}

		return size;
	}
}
