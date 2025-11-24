package org.quiltmc.enigma.gui.util;

import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;

public class FlexGridLayout implements LayoutManager2 {
	private final ConstrainedGrid grid = new ConstrainedGrid();

	@Override
	public void addLayoutComponent(Component component, @Nullable Object constraints) throws IllegalArgumentException {
		if (constraints == null) {
			this.addDefaultConstrainedLayoutComponent(component);
		} else if (constraints instanceof FlexGridConstraints<?> typedConstraints) {
			this.addLayoutComponent(component, typedConstraints);
		} else {
			throw new IllegalArgumentException(
				"constraints type must be %s, but was %s!"
					.formatted(FlexGridConstraints.class.getName(), constraints.getClass().getName())
			);
		}
	}

	public void addLayoutComponent(Component component, @Nullable FlexGridConstraints<?> constraints) {
		if (constraints == null) {
			this.addDefaultConstrainedLayoutComponent(component);
		} else {
			final int x;
			final int y;
			if (constraints instanceof FlexGridConstraints.Absolute absolute) {
				x = absolute.x;
				y = absolute.y;
			} else {
				x = this.getRelativeX();
				y = this.getRelativeY();
			}

			this.grid.put(x, y, new Constrained(component, constraints));
		}
	}

	@Override
	public void addLayoutComponent(String ignored, Component component) {
		this.addDefaultConstrainedLayoutComponent(component);
	}

	private void addDefaultConstrainedLayoutComponent(Component component) {
		this.grid.put(this.getRelativeX(), this.getRelativeY(), Constrained.defaultOf(component));
	}

	private int getRelativeX() {
		return this.grid.isEmpty() ? FlexGridConstraints.Absolute.DEFAULT_X : this.grid.getMaxXOrThrow() + 1;
	}

	private int getRelativeY() {
		return this.grid.isEmpty() ? FlexGridConstraints.Absolute.DEFAULT_Y : this.grid.getMaxYOrThrow();
	}

	@Override
	public void removeLayoutComponent(Component component) {
		this.grid.remove(component);
	}

	@Override
	public float getLayoutAlignmentX(Container target) {
		return 0.5f;
	}

	@Override
	public float getLayoutAlignmentY(Container target) {
		return 0.5f;
	}

	@Override
	public void invalidateLayout(Container target) {
		// TODO
	}

	@Override
	public Dimension maximumLayoutSize(Container target) { }

	@Override
	public Dimension preferredLayoutSize(Container parent) { }

	@Override
	public Dimension minimumLayoutSize(Container parent) { }

	@Override
	public void layoutContainer(Container parent) {
		null
	}

	record Constrained(
		Component component, int width, int height, boolean fillX, boolean fillY, FlexGridConstraints.Alignment xAlignment,
		FlexGridConstraints.Alignment yAlignment, int priority
	) {
		static Constrained defaultOf(Component component) {
			return new Constrained(
				component,
				FlexGridConstraints.DEFAULT_WIDTH, FlexGridConstraints.DEFAULT_HEIGHT,
				FlexGridConstraints.DEFAULT_FILL_X, FlexGridConstraints.DEFAULT_FILL_Y,
				FlexGridConstraints.DEFAULT_X_ALIGNMENT, FlexGridConstraints.DEFAULT_Y_ALIGNMENT,
				FlexGridConstraints.DEFAULT_PRIORITY
			);
		}

		Constrained(Component component, FlexGridConstraints<?> constraints) {
			this(
				component,
				constraints.width, constraints.height,
				constraints.fillX, constraints.fillY,
				constraints.xAlignment, constraints.yAlignment,
				constraints.priority
			);
		}

		int getXExcess() {
			return this.width - 1;
		}

		int getYExcess() {
			return this.height - 1;
		}
	}
}
