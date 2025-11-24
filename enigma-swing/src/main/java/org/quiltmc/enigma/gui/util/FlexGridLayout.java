package org.quiltmc.enigma.gui.util;

import com.google.common.collect.ImmutableMap;
import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.util.Utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class FlexGridLayout implements LayoutManager2 {
	private final ConstrainedGrid grid = new ConstrainedGrid();

	private @Nullable Sizes preferredSizes;
	private @Nullable Sizes minSizes;
	private @Nullable Sizes maxSizes;

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
		this.preferredSizes = null;
		this.minSizes = null;
		this.maxSizes = null;
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		return this.getPreferredSizes().createTotalDimension();
	}

	private Sizes getPreferredSizes() {
		if (this.preferredSizes == null) {
			this.preferredSizes = Sizes.calculate(this.grid, Component::getPreferredSize);
		}

		return this.preferredSizes;
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		return this.getMinSizes().createTotalDimension();
	}

	private Sizes getMinSizes() {
		if (this.minSizes == null) {
			this.minSizes = Sizes.calculate(this.grid, Component::getMinimumSize);
		}

		return this.minSizes;
	}

	@Override
	public Dimension maximumLayoutSize(Container target) {
		return this.getMaxSizes().createTotalDimension();
	}

	private Sizes getMaxSizes() {
		if (this.maxSizes == null) {
			this.maxSizes = Sizes.calculate(this.grid, Component::getMaximumSize);
		}

		return this.maxSizes;
	}

	@Override
	public void layoutContainer(Container parent) {
		null
	}

	record Constrained(
			Component component,
			int width, int height,
			boolean fillX, boolean fillY,
			FlexGridConstraints.Alignment xAlignment, FlexGridConstraints.Alignment yAlignment,
			int priority
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

	private record Sizes(
		int totalWidth, int totalHeight,
		ImmutableMap<Integer, Integer> cellWidths, ImmutableMap<Integer, Integer> cellHeights,
		ImmutableMap<Component, Dimension> componentSizes
	) {
		static Sizes calculate(ConstrainedGrid grid, Function<Component, Dimension> getSize) {
			final Map<Component, Dimension> componentSizesBuilder = new HashMap<>();

			final Map<Integer, Integer> cellWidthsBuilder = new HashMap<>();
			final Map<Integer, Integer> cellHeightsBuilder = new HashMap<>();

			grid.forEach((x, y, values) -> {
				values.forEach(constrained -> {
					final Dimension size = componentSizesBuilder
						.computeIfAbsent(constrained.component, getSize);

					final int componentCellWidth = Utils.ceilDiv(size.width, constrained.width);
					for (int offset = 0; offset < constrained.width; offset++) {
						cellWidthsBuilder.compute(x + offset, (ignored, width) -> {
							return width == null ? componentCellWidth : width + componentCellWidth;
						});
					}

					final int componentCellHeight = Utils.ceilDiv(size.height, constrained.height);
					for (int offset = 0; offset < constrained.height; offset++) {
						cellHeightsBuilder.compute(y + offset, (ignored, height) -> {
							return height == null ? componentCellHeight : height + componentCellHeight;
						});
					}
				});
			});

			final ImmutableMap<Integer, Integer> cellWidths = ImmutableMap.copyOf(cellWidthsBuilder);
			final ImmutableMap<Integer, Integer> cellHeights = ImmutableMap.copyOf(cellHeightsBuilder);

			return new Sizes(
				cellWidths.values().stream().mapToInt(Integer::intValue).sum(),
				cellHeights.values().stream().mapToInt(Integer::intValue).sum(),
				cellWidths, cellHeights,
				ImmutableMap.copyOf(componentSizesBuilder)
			);
		}

		Dimension createTotalDimension() {
			return new Dimension(this.totalWidth, this.totalHeight);
		}
	}
}
