package org.quiltmc.enigma.gui.util;

import com.google.common.collect.ImmutableMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.util.Utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
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
	public Dimension preferredLayoutSize(Container container) {
		return this.getPreferredSizes().createTotalDimension(container.getInsets());
	}

	private Sizes getPreferredSizes() {
		if (this.preferredSizes == null) {
			this.preferredSizes = Sizes.calculate(this.grid, Component::getPreferredSize);
		}

		return this.preferredSizes;
	}

	@Override
	public Dimension minimumLayoutSize(Container container) {
		return this.getMinSizes().createTotalDimension(container.getInsets());
	}

	private Sizes getMinSizes() {
		if (this.minSizes == null) {
			this.minSizes = Sizes.calculate(this.grid, Component::getMinimumSize);
		}

		return this.minSizes;
	}

	@Override
	public Dimension maximumLayoutSize(Container container) {
		return this.getMaxSizes().createTotalDimension(container.getInsets());
	}

	private Sizes getMaxSizes() {
		if (this.maxSizes == null) {
			this.maxSizes = Sizes.calculate(this.grid, Component::getMaximumSize);
		}

		return this.maxSizes;
	}

	@Override
	public void layoutContainer(Container parent) {
		this.layoutAxis(parent, true);
		this.layoutAxis(parent, false);
	}

	private void layoutAxis(Container parent, boolean xAxis) {
		final Insets insets = parent.getInsets();
		final int leadingInset = xAxis ? insets.left : insets.top;

		final int availableSpace = xAxis
				? parent.getWidth() - insets.left - insets.right
				: parent.getHeight() - insets.top - insets.bottom;

		final Sizes preferredSizes = this.getPreferredSizes();

		final int extraSpace = availableSpace - (xAxis ? preferredSizes.totalWidth : preferredSizes.totalHeight);
		if (extraSpace >= 0) {
			this.layoutFixedAxis(preferredSizes, leadingInset + extraSpace / 2, xAxis);
		} else {
			final Sizes minSizes = this.getMinSizes();

			final int extraMinSpace = availableSpace - (xAxis ? minSizes.totalWidth : minSizes.totalHeight);
			if (extraMinSpace <= 0) {
				this.layoutFixedAxis(minSizes, leadingInset, xAxis);
			} else {
				final Map<Integer, Integer> cellSpans = this.allocateCellSpace(xAxis, extraMinSpace);

				this.layoutAxisImpl(leadingInset, xAxis, cellSpans, (component, coord) -> {
					final Dimension preferredSize = preferredSizes.componentSizes.get(component);
					assert preferredSize != null;
					return Math.min(xAxis ? preferredSize.width : preferredSize.height, cellSpans.get(coord));
				});
			}
		}
	}

	private Map<Integer, Integer> allocateCellSpace(boolean xAxis, int remainingSpace) {
		final SortedSet<Constrained.At> prioritizedConstrained = new TreeSet<>();
		this.grid.forEach((x, y, values) -> {
			values.forEach(constrained -> prioritizedConstrained.add(constrained.new At(xAxis ? x : y)));
		});

		final ImmutableMap<Component, Dimension> preferredComponentSizes = this.getPreferredSizes().componentSizes;
		final Map<Integer, Integer> cellSpans = new HashMap<>(this.getMinSizes().columnWidths);
		for (final Constrained.At at : prioritizedConstrained) {
			final int currentSpan = cellSpans.get(at.coord);
			final Dimension preferredSize = preferredComponentSizes.get(at.constrained().component);
			assert preferredSize != null;
			final int preferredSpan = xAxis ? preferredSize.width : preferredSize.height;
			final int preferredDiff = preferredSpan - currentSpan;
			if (preferredDiff > 0) {
				if (preferredDiff <= remainingSpace) {
					cellSpans.put(at.coord, preferredSpan);

					if (remainingSpace == preferredDiff) {
						break;
					} else {
						remainingSpace -= preferredDiff;
					}
				} else {
					final int lastOfSpan = remainingSpace;
					cellSpans.compute(at.coord, (ignored, span) -> {
						assert span != null;
						return span + lastOfSpan;
					});

					break;
				}
			}
		}

		return cellSpans;
	}

	@SuppressWarnings("DataFlowIssue")
	private void layoutFixedAxis(Sizes sizes, int startPos, boolean xAxis) {
		this.layoutAxisImpl(
				startPos, xAxis, xAxis ? sizes.columnWidths : sizes.rowHeights,
				(component, coord) -> {
					final Dimension size = sizes.componentSizes.get(component);
					return xAxis ? size.width : size.height;
				}
		);
	}

	// TODO respect fill/alignment
	private void layoutAxisImpl(
			int startPos, boolean xAxis,
			Map<Integer, Integer> cellSpans,
			BiFunction<Component, Integer, Integer> getComponentSpan
	) {
		final Map<Integer, Integer> positions = new HashMap<>();

		this.grid.forEach((x, y, values) -> {
			final int coord = xAxis ? x : y;
			final int pos = positions.computeIfAbsent(coord, ignored -> startPos);

			values.forEach(constrained -> {
				final int span = getComponentSpan.apply(constrained.component, coord);
				if (xAxis) {
					constrained.component.setBounds(
							pos, constrained.component.getY(),
							span, constrained.component.getHeight()
					);
				} else {
					constrained.component.setBounds(
							constrained.component.getX(), pos,
							constrained.component.getWidth(), span
					);
				}
			});

			positions.put(coord, pos + cellSpans.get(coord));
		});
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

		private class At implements Comparable<At> {
			static final Comparator<At> PRIORITY_COMPARATOR = (left, right) -> {
				return right.constrained().priority - left.constrained().priority;
			};

			static final Comparator<At> COORD_COMPARATOR = Comparator.comparingInt(At::getCoord);

			static final Comparator<At> COMPARATOR = PRIORITY_COMPARATOR.thenComparing(COORD_COMPARATOR);

			final int coord;

			At(int coord) {
				this.coord = coord;
			}

			int getCoord() {
				return this.coord;
			}

			Constrained constrained() {
				return Constrained.this;
			}

			@Override
			public int compareTo(@NonNull At other) {
				return COMPARATOR.compare(this, other);
			}
		}
	}

	private record Sizes(
			int totalWidth, int totalHeight,
			ImmutableMap<Integer, Integer> rowHeights, ImmutableMap<Integer, Integer> columnWidths,
			ImmutableMap<Component, Dimension> componentSizes
	) {
		static Sizes calculate(ConstrainedGrid grid, Function<Component, Dimension> getSize) {
			final Map<Component, Dimension> componentSizes = new HashMap<>();

			final Map<Integer, Map<Integer, Dimension>> cellSizes = new HashMap<>();

			grid.forEach((x, y, values) -> {
				values.forEach(constrained -> {
					final Dimension size = componentSizes.computeIfAbsent(constrained.component, getSize);

					final int componentCellWidth = Utils.ceilDiv(size.width, constrained.width);
					final int componentCellHeight = Utils.ceilDiv(size.height, constrained.height);
					for (int xOffset = 0; xOffset < constrained.width; xOffset++) {
						for (int yOffset = 0; yOffset < constrained.width; yOffset++) {
							final Dimension cellSize = cellSizes
									.computeIfAbsent(x + xOffset, ignored -> new HashMap<>())
									.computeIfAbsent(y +yOffset, ignored -> new Dimension());

							cellSize.width = Math.max(cellSize.width, componentCellWidth);
							cellSize.height = Math.max(cellSize.height, componentCellHeight);
						}
					}
				});
			});

			final Map<Integer, Integer> rowHeights = new HashMap<>();
			final Map<Integer, Integer> columnWidths = new HashMap<>();
			cellSizes.forEach((x, column) -> {
				column.forEach((y, size) -> {
					rowHeights.compute(y, (ignored, height) -> height == null ? size.height : Math.max(height, size.height));
					columnWidths.compute(x, (ignored, width) -> width == null ? size.width : Math.max(width, size.width));
				});
			});

			return new Sizes(
				columnWidths.values().stream().mapToInt(Integer::intValue).sum(),
				rowHeights.values().stream().mapToInt(Integer::intValue).sum(),
				ImmutableMap.copyOf(rowHeights), ImmutableMap.copyOf(columnWidths),
				ImmutableMap.copyOf(componentSizes)
			);
		}

		Dimension createTotalDimension(Insets insets) {
			return new Dimension(
				this.totalWidth + insets.left + insets.right,
				this.totalHeight + insets.top + insets.bottom
			);
		}
	}
}
