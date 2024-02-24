package org.quiltmc.enigma.gui.util;

import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

public class WrapLayout extends FlowLayout {
	public WrapLayout() {
		super();
	}

	public WrapLayout(int align) {
		super(align);
	}

	public WrapLayout(int align, int hgap, int vgap) {
		super(align, hgap, vgap);
	}

	@Override
	public Dimension preferredLayoutSize(Container target) {
		return this.computeLayoutSize(target, true);
	}

	@Override
	public Dimension minimumLayoutSize(Container target) {
		Dimension minimum = this.computeLayoutSize(target, false);
		minimum.width -= (this.getHgap() + 1);

		return minimum;
	}

	private Dimension computeLayoutSize(Container target, boolean preferred) {
		int targetWidth = target.getSize().width;

		if (targetWidth == 0) {
			targetWidth = Integer.MAX_VALUE;
		}

		int horizontalGap = this.getHgap();
		Insets insets = target.getInsets();
		int horizontalSpacing = insets.left + insets.right + horizontalGap * 2;
		int maxWidth = targetWidth - horizontalSpacing;

		Dimension layoutSize = new Dimension(0, 0);
		int rowWidth = 0;
		int rowHeight = 0;

		int componentCount = target.getComponentCount();

		for (int i = 0; i < componentCount; i++) {
			Component component = target.getComponent(i);

			if (component.isVisible()) {
				Dimension componentSize = preferred ? component.getPreferredSize() : component.getMinimumSize();

				if (rowWidth + componentSize.width > maxWidth) {
					this.addRow(layoutSize, rowWidth, rowHeight);
					rowWidth = 0;
					rowHeight = 0;
				} else if (rowWidth != 0) {
					rowWidth += horizontalGap;
				}

				rowWidth += componentSize.width;
				rowHeight = Math.max(rowHeight, componentSize.height);
			}
		}

		this.addRow(layoutSize, rowWidth, rowHeight);

		layoutSize.width += horizontalSpacing;
		layoutSize.height += insets.top + insets.bottom + this.getVgap() * 2;

		if (SwingUtilities.getAncestorOfClass(JScrollBar.class, target) != null) {
			layoutSize.width -= (horizontalGap + 1);
		}

		return layoutSize;
	}

	private void addRow(Dimension layoutSize, int width, int height) {
		layoutSize.width = Math.max(layoutSize.width, width);

		if (layoutSize.height > 0) {
			layoutSize.height += this.getVgap();
		}

		layoutSize.height += height;
	}
}
