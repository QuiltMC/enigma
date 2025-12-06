package org.quiltmc.enigma.gui.util.layout.flex_grid;

import com.google.common.collect.ImmutableMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints.Alignment;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.quiltmc.enigma.util.Utils.ceilDiv;

/**
 * A layout manager that lays out components in a grid and allocates space according to priority.
 *
 * <p> Flex grids are similar to {@link GridBagLayout}s, with some key differences:
 * <ul>
 *     <li> flex grids don't {@linkplain GridBagConstraints#weightx weight} their components; instead, space is
 *          allocated according to {@linkplain FlexGridConstraints#priority(int) priority} and position
 *     <li> flex grids respect each component's {@linkplain Component#getMaximumSize() maximum size}
 *     <li> flex grids support negative coordinates
 * </ul>
 *
 * <h4>Constraints</h4>
 *
 * Flex grids are configured by passing {@link FlexGridConstraints} when
 * {@linkplain Container#add(Component, Object) adding} {@link Component}s.<br>
 * If no constraints are specified,
 * a default set of {@linkplain FlexGridConstraints#createRelative() relative} constraints are used.<br>
 * If non-{@linkplain FlexGridConstraints flex grid} constraints are passed,
 * an {@link IllegalArgumentException} is thrown.
 *
 * <h4>Space allocation</h4>
 *
 * <p> Space is allocated to components per-axis, with high-{@linkplain FlexGridConstraints#priority(int) priority}
 * components getting space first. In ties, components with the least position on the axis get priority.<br>
 * Components never get less space in an axis than their {@linkplain Component#getMinimumSize() minimum sizes}
 * allow, and they never get more space than their {@linkplain Component#getMaximumSize() maximum sizes} allow.
 * Components only ever get more space in an axis than their
 * {@linkplain Component#getPreferredSize() preferred sizes} request if they're set to
 * {@linkplain FlexGridConstraints#fill(boolean, boolean) fill} that axis.
 *
 * <p> Space allocation behavior depends on the parent container's available space and
 * child components' total required space (as before, per-axis):
 * <table>
 *     <tr>
 *         <th>less than minimum space</th>
 *         <td>each component gets its minimum size (excess is clipped)</td>
 *     </tr>
 *     <tr>
 *         <th>between minimum and preferred space</th>
 *         <td>
 *             each component gets at least its minimum size; components get additional space
 *             - up to their preferred size - according to priority
 *         </td>
 *     </tr>
 *     <tr>
 *         <th>more than preferred space</th>
 *         <td>
 *             each component gets at least is preferred size; components that
 *             {@linkplain FlexGridConstraints#fill(boolean, boolean) fill} the axis get additional space
 *             - up to their max size - according to priority
 *        </td>
 *     </tr>
 * </table>
 *
 * <h4>Grid specifics</h4>
 *
 * <ul>
 *     <li> components with {@linkplain FlexGridConstraints#createRelative() relative constraints} (or no constraints)
 *          are added to the end of the bottom row,<br>
 *          or ({@value FlexGridConstraints.Absolute#DEFAULT_X}, {@value FlexGridConstraints.Absolute#DEFAULT_Y})
 *          if no other components have been added
 *     <li> a component can occupy multiple grid cells when its constraint
 *          {@linkplain FlexGridConstraints#xExtent(int) xExtent} or
 *          {@linkplain FlexGridConstraints#yExtent(int) yExtent} exceeds {@code 1};
 *          it occupies cells starting from its coordinates and extending in the positive direction of each axis
 *     <li> any number of components can share grid cells, resulting in overlap
 *     <li> only the relative values of coordinates matter; components with the least x coordinate are left-most
 *          whether the coordinate is {@code -1000}, {@code 0}, or {@code 1000}
 *     <li> vacant rows and columns are ignored; if two components are at x {@code -10} and {@code 10} and
 *          no other component has an x coordinate in that range, the two components are adjacent (in terms of x)
 * </ul>
 */
public class FlexGridLayout implements LayoutManager2 {
	private final ConstrainedGrid grid = new ConstrainedGrid();

	/**
	 * Lazily populated cache.
	 *
	 * @see #getPreferredSizes()
	 */
	private @Nullable Sizes preferredSizes;

	/**
	 * Lazily populated cache.
	 *
	 * @see #getMinSizes()
	 */
	private @Nullable Sizes minSizes;

	/**
	 * Lazily populated cache.
	 *
	 * @see #getMaxSizes()
	 */
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

					return ops.getSpan(targetSize);
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

					return ops.getSpan(preferredSize);
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

		while (!prioritized.isEmpty() && remainingSpace > 0) {
			final Constrained.At at = prioritized.remove();

			final Dimension targetSize = large.componentSizes.get(at.constrained().component);
			assert targetSize != null;

			final int extent = ops.getExtent(at.constrained());

			final int targetSpan = ceilDiv(ops.getSpan(targetSize), extent);

			for (int i = 0; i < extent; i++) {
				final int extendedCoord = at.coord + i;
				final int currentSpan = cellSpans.get(extendedCoord);

				final int targetDiff = targetSpan - currentSpan;
				if (targetDiff > 0) {
					if (targetDiff <= remainingSpace) {
						cellSpans.put(extendedCoord, targetSpan);

						remainingSpace -= targetDiff;
						if (remainingSpace == 0) {
							break;
						}
					} else {
						final int lastOfSpace = remainingSpace;
						remainingSpace = 0;
						cellSpans.compute(extendedCoord, (ignored, span) -> {
							assert span != null;
							return span + lastOfSpace;
						});

						break;
					}
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
				final int extent = ops.getExtent(constrained);

				int extendedCellSpan = 0;
				for (int i = 0; i < extent; i++) {
					extendedCellSpan += cellSpans.get(coord + i);
				}

				final int span = Math.min(getComponentSpan.apply(constrained, coord), extendedCellSpan);

				final int constrainedPos = switch (ops.getAlignment(constrained)) {
					case BEGIN -> pos;
					case CENTER -> pos + (extendedCellSpan - span) / 2;
					case END -> pos + extendedCellSpan - span;
				};

				ops.setBounds(constrained.component, constrainedPos, span);
			});

			positions.put(oppositeCoord, pos + cellSpans.get(coord));
		});
	}

	record Constrained(
			Component component,
			int xExtent, int yExtent,
			boolean fillX, boolean fillY,
			Alignment xAlignment, Alignment yAlignment,
			int priority
	) {
		static Constrained defaultOf(Component component) {
			return new Constrained(
				component,
				FlexGridConstraints.DEFAULT_X_EXTENT, FlexGridConstraints.DEFAULT_Y_EXTENT,
				FlexGridConstraints.DEFAULT_FILL_X, FlexGridConstraints.DEFAULT_FILL_Y,
				FlexGridConstraints.DEFAULT_X_ALIGNMENT, FlexGridConstraints.DEFAULT_Y_ALIGNMENT,
				FlexGridConstraints.DEFAULT_PRIORITY
			);
		}

		Constrained(Component component, FlexGridConstraints<?> constraints) {
			this(
					component,
					constraints.getXExtent(), constraints.getYExtent(),
					constraints.fillsX(), constraints.fillsY(),
					constraints.getXAlignment(), constraints.getYAlignment(),
					constraints.getPriority()
			);
		}

		int getXExcess() {
			return this.xExtent - 1;
		}

		int getYExcess() {
			return this.yExtent - 1;
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

	/**
	 * A collection of sizes and size metrics use for calculating min/max/preferred container size and
	 * for laying out the container.
	 */
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

					final int componentCellWidth = ceilDiv(size.width, constrained.xExtent);
					final int componentCellHeight = ceilDiv(size.height, constrained.yExtent);
					for (int xOffset = 0; xOffset < constrained.xExtent; xOffset++) {
						for (int yOffset = 0; yOffset < constrained.xExtent; yOffset++) {
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

	/**
	 * Sets of operations for the {@link #X} and {@link #Y} axes of a cartesian plane.
	 */
	private enum CartesianOperations {
		X() {
			@Override
			int chooseCoord(int x, int y) {
				return x;
			}

			@Override
			int getLeadingInset(Insets insets) {
				return insets.left;
			}

			@Override
			int getTrailingInset(Insets insets) {
				return insets.right;
			}

			@Override
			int getParentSpace(Container parent) {
				return parent.getWidth();
			}

			@Override
			int getTotalSpace(Sizes sizes) {
				return sizes.totalWidth;
			}

			@Override
			ImmutableMap<Integer, Integer> getCellSpans(Sizes sizes) {
				return sizes.columnWidths;
			}

			@Override
			int getSpan(Dimension size) {
				return size.width;
			}

			@Override
			boolean fills(Constrained constrained) {
				return constrained.fillX;
			}

			@Override
			boolean noneFill(ConstrainedGrid grid) {
				return grid.noneFillX();
			}

			@Override
			Alignment getAlignment(Constrained constrained) {
				return constrained.xAlignment;
			}

			@Override
			int getExtent(Constrained constrained) {
				return constrained.xExtent;
			}

			@Override
			void setBounds(Component component, int x, int width) {
				component.setBounds(x, component.getY(), width, component.getHeight());
			}

			@Override
			CartesianOperations opposite() {
				return Y;
			}
		},
		Y() {
			@Override
			int chooseCoord(int x, int y) {
				return y;
			}

			@Override
			int getLeadingInset(Insets insets) {
				return insets.top;
			}

			@Override
			int getTrailingInset(Insets insets) {
				return insets.bottom;
			}

			@Override
			int getParentSpace(Container parent) {
				return parent.getHeight();
			}

			@Override
			int getTotalSpace(Sizes sizes) {
				return sizes.totalHeight;
			}

			@Override
			ImmutableMap<Integer, Integer> getCellSpans(Sizes sizes) {
				return sizes.rowHeights;
			}

			@Override
			int getSpan(Dimension size) {
				return size.height;
			}

			@Override
			boolean fills(Constrained constrained) {
				return constrained.fillY;
			}

			@Override
			boolean noneFill(ConstrainedGrid grid) {
				return grid.noneFillY();
			}

			@Override
			Alignment getAlignment(Constrained constrained) {
				return constrained.yAlignment;
			}

			@Override
			int getExtent(Constrained constrained) {
				return constrained.yExtent;
			}

			@Override
			void setBounds(Component component, int y, int height) {
				component.setBounds(component.getX(), y, component.getWidth(), height);
			}

			@Override
			CartesianOperations opposite() {
				return X;
			}
		};

		abstract int chooseCoord(int x, int y);

		abstract int getLeadingInset(Insets insets);
		abstract int getTrailingInset(Insets insets);
		abstract int getParentSpace(Container parent);

		abstract int getTotalSpace(Sizes sizes);
		abstract ImmutableMap<Integer, Integer> getCellSpans(Sizes sizes);
		abstract int getSpan(Dimension size);

		abstract boolean fills(Constrained constrained);
		abstract boolean noneFill(ConstrainedGrid grid);
		abstract Alignment getAlignment(Constrained constrained);
		abstract int getExtent(Constrained constrained);

		abstract void setBounds(Component component, int pos, int span);

		abstract CartesianOperations opposite();
	}
}
