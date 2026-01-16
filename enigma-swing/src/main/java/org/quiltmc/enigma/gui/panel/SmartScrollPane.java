package org.quiltmc.enigma.gui.panel;

import org.jspecify.annotations.Nullable;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.Component;
import java.awt.Dimension;

/**
 * A {@link JScrollPane} with QoL improvements.
 *
 * <p> Currently it requests space for its scroll bars in {@link #getPreferredSize()} and uses
 * {@link ScrollBarPolicy ScrollBarPolicy} instead of magic constants.
 */
public class SmartScrollPane extends JScrollPane {
	/**
	 * Constructs a scroll pane displaying the passed {@code view}
	 * with {@link SmartScrollPane.ScrollBarPolicy#AS_NEEDED AS_NEEDED} scroll bars.
	 *
	 * @see #SmartScrollPane(Component, ScrollBarPolicy, ScrollBarPolicy)
	 */
	public SmartScrollPane(@Nullable Component view) {
		this(view, ScrollBarPolicy.AS_NEEDED, ScrollBarPolicy.AS_NEEDED);
	}

	/**
	 * @see #SmartScrollPane(Component)
	 */
	public SmartScrollPane(@Nullable Component view, ScrollBarPolicy vertical, ScrollBarPolicy horizontal) {
		super(view, vertical.vertical, horizontal.horizontal);
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

	public enum ScrollBarPolicy {
		/**
		 * @see ScrollPaneConstants#HORIZONTAL_SCROLLBAR_AS_NEEDED
		 * @see ScrollPaneConstants#VERTICAL_SCROLLBAR_AS_NEEDED
		 */
		AS_NEEDED(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED),
		/**
		 * @see ScrollPaneConstants#HORIZONTAL_SCROLLBAR_ALWAYS
		 * @see ScrollPaneConstants#VERTICAL_SCROLLBAR_ALWAYS
		 */
		ALWAYS(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS),
		/**
		 * @see ScrollPaneConstants#HORIZONTAL_SCROLLBAR_NEVER
		 * @see ScrollPaneConstants#VERTICAL_SCROLLBAR_NEVER
		 */
		NEVER(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

		public final int horizontal;
		public final int vertical;

		ScrollBarPolicy(int horizontal, int vertical) {
			this.horizontal = horizontal;
			this.vertical = vertical;
		}
	}
}
