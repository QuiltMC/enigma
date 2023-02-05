package cuchaz.enigma.gui.docker.component;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.util.ArrayList;
import java.util.List;

public class VerticalFlowLayout implements LayoutManager2 {
	private final List<Component> components = new ArrayList<>();
	private final int verticalGap;

	public VerticalFlowLayout(int verticalGap) {
		this.verticalGap = verticalGap;
	}

	@Override
	public float getLayoutAlignmentX(Container target) {
		return 0;
	}

	@Override
	public float getLayoutAlignmentY(Container target) {
		return 0;
	}

	@Override
	public void invalidateLayout(Container target) {
		// we have no cached data to invalidate
	}

	@Override
	public void addLayoutComponent(Component comp, Object constraints) {
		this.addComponent(comp);
	}

	@Override
	public void addLayoutComponent(String name, Component comp) {
		this.addComponent(comp);
	}

	private void addComponent(Component comp) {
		this.components.add(comp);
		this.layoutContainer(comp.getParent());
	}

	@Override
	public void layoutContainer(Container parent) {
		int x;
		int y = 0;
		for (Component c : this.components) {
			if (c.isVisible()) {
				Dimension d = c.getPreferredSize();

				if (parent.getWidth() > d.getWidth()) {
					x = (int) ((parent.getWidth() - d.getWidth()) / 2);
				} else {
					x = 0;
				}

				c.setBounds(x, y, (int) d.getWidth(), (int) d.getHeight());
				y += d.getHeight() + this.verticalGap;
			}
		}
	}

	@Override
	public Dimension preferredLayoutSize(Container target) {
		int height = 0;
		int width = 0;
		for (Component c : this.components) {
			if (c.isVisible()) {
				Dimension d = c.getPreferredSize();
				height += d.getHeight() + this.verticalGap;

				width = Math.max(width, (int) d.getWidth());
			}
		}

		return new Dimension(width, height);
	}

	@Override
	public Dimension minimumLayoutSize(Container target) {
		return this.preferredLayoutSize(target);
	}

	@Override
	public Dimension maximumLayoutSize(Container target) {
		return this.preferredLayoutSize(target);
	}

	@Override
	public void removeLayoutComponent(Component comp) {
		this.components.remove(comp);
	}
}
