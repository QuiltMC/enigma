package org.quiltmc.enigma.gui.util.layout.flex_grid;

import com.google.common.collect.ImmutableMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;
import org.quiltmc.enigma.util.Utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
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
		} else if (constraints instanceof FlexGridConstraints<?> gridConstraints) {
			this.addLayoutComponent(component, gridConstraints);
		} else {
			throw new IllegalArgumentException(
				"constraints type %s does not extend %s!"
					.formatted(constraints.getClass().getName(), FlexGridConstraints.class.getName())
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
				x = absolute.getX();
				y = absolute.getY();
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
		this.layoutAxis(parent, CartesianOperations.X);
		this.layoutAxis(parent, CartesianOperations.Y);
	}

	private void layoutAxis(Container parent, CartesianOperations ops) {
		final Insets insets = parent.getInsets();
		final int leadingInset = ops.getLeadingInset(insets);

		final int availableSpace = ops.getParentSpace(parent) - leadingInset - ops.getTrailingInset(insets);

		final Sizes preferred = this.getPreferredSizes();

		final int extraSpace = availableSpace - ops.getTotalSpace(preferred);
		if (extraSpace >= 0) {
			if (extraSpace == 0 || ops.noneFill(this.grid)) {
				this.layoutFixedAxis(preferred, leadingInset + extraSpace / 2, ops);
			} else {
				final Map<Integer, Integer> cellSpans = this.allocateCellSpace(ops, extraSpace, true);

				final Sizes max = this.getMaxSizes();
				this.layoutAxisImpl(leadingInset, ops, cellSpans, (constrained, coord) -> {
					final Sizes targets = ops.fills(constrained) ? max : preferred;
					final Dimension targetSize = targets.componentSizes.get(constrained.component);
					assert targetSize != null;

					return Math.min(ops.getSpan(targetSize), cellSpans.get(coord));
				});
			}
		} else {
			final Sizes min = this.getMinSizes();

			final int extraMinSpace = availableSpace - ops.getTotalSpace(min);
			if (extraMinSpace <= 0) {
				this.layoutFixedAxis(min, leadingInset, ops);
			} else {
				final Map<Integer, Integer> cellSpans = this.allocateCellSpace(ops, extraMinSpace, false);

				this.layoutAxisImpl(leadingInset, ops, cellSpans, (constrained, coord) -> {
					final Dimension preferredSize = preferred.componentSizes.get(constrained.component);
					assert preferredSize != null;
					return Math.min(ops.getSpan(preferredSize), cellSpans.get(coord));
				});
			}
		}
	}

	private Map<Integer, Integer> allocateCellSpace(CartesianOperations ops, int remainingSpace, boolean fill) {
		final Sizes large;
		final Sizes small;
		final Map<Integer, Integer> cellSpans;
		if (fill) {
			large = this.getMaxSizes();
			small = this.getPreferredSizes();
		} else {
			large = this.getPreferredSizes();
			small = this.getMinSizes();
		}

		cellSpans = new HashMap<>(ops.getCellSpans(small));

		final PriorityQueue<Constrained.At> prioritized = new PriorityQueue<>(this.grid.getSize());
		this.grid.forEach((x, y, values) -> {
			if (fill) {
				values = values.filter(ops::fills);
			}

			values.forEach(constrained -> {
				prioritized.add(constrained.new At(ops.chooseCoord(x, y)));
			});
		});

		for (final Constrained.At at : prioritized) {
			final int currentSpan = cellSpans.get(at.coord);

			final Dimension targetSize = large.componentSizes.get(at.constrained().component);
			assert targetSize != null;
			final int targetSpan = ops.getSpan(targetSize);
			final int targetDiff = targetSpan - currentSpan;
			if (targetDiff > 0) {
				if (targetDiff <= remainingSpace) {
					cellSpans.put(at.coord, targetSpan);

					if (remainingSpace == targetDiff) {
						break;
					} else {
						remainingSpace -= targetDiff;
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

	private void layoutFixedAxis(Sizes sizes, int startPos, CartesianOperations ops) {
		this.layoutAxisImpl(
				startPos, ops, ops.getCellSpans(sizes),
				(constrained, coord) -> ops.getSpan(sizes.componentSizes.get(constrained.component))
		);
	}

	// TODO respect alignment
	private void layoutAxisImpl(
			int startPos, CartesianOperations ops,
			Map<Integer, Integer> cellSpans,
			BiFunction<Constrained, Integer, Integer> getComponentSpan
	) {
		final Map<Integer, Integer> positions = new HashMap<>();

		this.grid.forEach((x, y, values) -> {
			final int coord = ops.chooseCoord(x, y);
			final int oppositeCoord = ops.opposite().chooseCoord(x, y);

			final int pos = positions.computeIfAbsent(oppositeCoord, ignored -> startPos);

			values.forEach(constrained -> {
				final int span = getComponentSpan.apply(constrained, coord);
				ops.setBounds(constrained.component, pos, span);
			});

			positions.put(oppositeCoord, pos + cellSpans.get(coord));
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
					constraints.getWidth(), constraints.getHeight(),
					constraints.fillsX(), constraints.fillsY(),
					constraints.getXAlignment(), constraints.getYAlignment(),
					constraints.getPriority()
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
									.computeIfAbsent(y + yOffset, ignored -> new Dimension());

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

	private interface CartesianOperations {
		CartesianOperations X = new CartesianOperations() {
			@Override
			public int chooseCoord(int x, int y) {
				return x;
			}

			@Override
			public int getLeadingInset(Insets insets) {
				return insets.left;
			}

			@Override
			public int getTrailingInset(Insets insets) {
				return insets.right;
			}

			@Override
			public int getParentSpace(Container parent) {
				return parent.getWidth();
			}

			@Override
			public int getTotalSpace(Sizes sizes) {
				return sizes.totalWidth;
			}

			@Override
			public ImmutableMap<Integer, Integer> getCellSpans(Sizes sizes) {
				return sizes.columnWidths;
			}

			@Override
			public int getSpan(Dimension size) {
				return size.width;
			}

			@Override
			public boolean fills(Constrained constrained) {
				return constrained.fillX;
			}

			@Override
			public boolean noneFill(ConstrainedGrid grid) {
				return grid.noneFillX();
			}

			@Override
			public void setBounds(Component component, int x, int width) {
				component.setBounds(x, component.getY(), width, component.getHeight());
			}

			@Override
			public CartesianOperations opposite() {
				return Y;
			}
		};

		CartesianOperations Y = new CartesianOperations() {
			@Override
			public int chooseCoord(int x, int y) {
				return y;
			}

			@Override
			public int getLeadingInset(Insets insets) {
				return insets.top;
			}

			@Override
			public int getTrailingInset(Insets insets) {
				return insets.bottom;
			}

			@Override
			public int getParentSpace(Container parent) {
				return parent.getHeight();
			}

			@Override
			public int getTotalSpace(Sizes sizes) {
				return sizes.totalHeight;
			}

			@Override
			public ImmutableMap<Integer, Integer> getCellSpans(Sizes sizes) {
				return sizes.rowHeights;
			}

			@Override
			public int getSpan(Dimension size) {
				return size.height;
			}

			@Override
			public boolean fills(Constrained constrained) {
				return constrained.fillY;
			}

			@Override
			public boolean noneFill(ConstrainedGrid grid) {
				return grid.noneFillY();
			}

			@Override
			public void setBounds(Component component, int y, int height) {
				component.setBounds(component.getX(), y, component.getWidth(), height);
			}

			@Override
			public CartesianOperations opposite() {
				return X;
			}
		};

		int chooseCoord(int x, int y);
		int getLeadingInset(Insets insets);
		int getTrailingInset(Insets insets);
		int getParentSpace(Container parent);
		int getTotalSpace(Sizes sizes);
		ImmutableMap<Integer, Integer> getCellSpans(Sizes sizes);
		int getSpan(Dimension size);
		boolean fills(Constrained constrained);
		boolean noneFill(ConstrainedGrid grid);
		void setBounds(Component component, int pos, int span);

		CartesianOperations opposite();
	}
}
