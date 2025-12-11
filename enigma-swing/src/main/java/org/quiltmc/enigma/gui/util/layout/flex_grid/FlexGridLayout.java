package org.quiltmc.enigma.gui.util.layout.flex_grid;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.gui.util.layout.flex_grid.ConstrainedGrid.Position;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints.Alignment;

import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
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
 *     <li> flex grids do <em>not</em> support insets; use {@linkplain JComponent#setBorder(Border) borders} instead
 * </ul>
 *
 * <h4>Constraints</h4>
 *
 * Configure flex grids by passing {@link FlexGridConstraints} when
 * {@linkplain Container#add(Component, Object) adding} {@link Component}s.<br>
 * When no constraints are specified, a default set of {@linkplain FlexGridConstraints#createRelative() relative}
 * constraints are used.<br>
 * Passing non-{@linkplain FlexGridConstraints flex grid} constraints will result in an
 * {@link IllegalArgumentException}.
 *
 * <h4>Space allocation</h4>
 *
 * <p> Space is allocated to components per-axis, with high-{@linkplain FlexGridConstraints#priority(int) priority}
 * components getting space first. In ties, components with the least position on the axis get priority.<br>
 * A component never gets less space in an axis than its {@linkplain Component#getMinimumSize() minimum size}
 * allows, and it never gets more space than its {@linkplain Component#getMaximumSize() maximum size} allows.
 * A component only ever gets more space in an axis than its
 * {@linkplain Component#getPreferredSize() preferred size} requests if it's set to
 * {@linkplain FlexGridConstraints#fill(boolean, boolean) fill} that axis.
 *
 * <p> Space allocation behavior depends on the parent container's available space and
 * child components' total sizes (as before, per-axis):
 * <table>
 *     <tr>
 *         <th>space ≤ min</th>
 *         <td>each component gets its minimum size (excess is clipped)</td>
 *     </tr>
 *     <tr>
 *         <th>min < space < preferred</th>
 *         <td>
 *             each component gets at least its minimum size; components get additional space
 *             - up to their preferred sizes - according to priority
 *         </td>
 *     </tr>
 *     <tr>
 *         <th>space ≥ preferred</th>
 *         <td>
 *             each component gets at least is preferred size; components that
 *             {@linkplain FlexGridConstraints#fill(boolean, boolean) fill} the axis get additional space
 *             - up to their max sizes - according to priority
 *        </td>
 *     </tr>
 * </table>
 *
 * <h4>Grid specifics</h4>
 *
 * <ul>
 *     <li> if a {@link FlexGridConstraints.Relative Relative} component is the first component to be added,
 *          it's placed at
 *          ({@value FlexGridConstraints.Absolute#DEFAULT_X}, {@value FlexGridConstraints.Absolute#DEFAULT_Y});
 *          otherwise its
 *          {@linkplain FlexGridConstraints.Relative#placement(FlexGridConstraints.Relative.Placement) placement}
 *          determines its position
 *     <li> components with no constraints are treated are treated as though they have
 *          {@link FlexGridConstraints.Relative Relative} constraints with
 *          {@link FlexGridConstraints.Relative#DEFAULT_PLACEMENT DEFAULT_PLACEMENT}
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
	// simplified integer overflow detection
	// only correctly handles overflow if all values are positive
	private static int sumOrMax(Collection<Integer> values) {
		int sum = 0;
		for (final int value : values) {
			sum += value;
			if (sum < 0) {
				return Integer.MAX_VALUE;
			}
		}

		return sum;
	}

	private static int positiveOrMax(int value) {
		return value < 0 ? Integer.MAX_VALUE : value;
	}

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
			final Constrained constrained = Constrained.of(component, constraints);
			if (constraints instanceof FlexGridConstraints.Absolute absolute) {
				this.grid.put(absolute.getX(), absolute.getY(), constrained);
			} else if (constraints instanceof FlexGridConstraints.Relative relative) {
				this.grid.putRelative(constrained, relative.getPlacement());
			} else {
				throw new AssertionError();
			}
		}
	}

	@Override
	public void addLayoutComponent(String ignored, Component component) {
		this.addDefaultConstrainedLayoutComponent(component);
	}

	private void addDefaultConstrainedLayoutComponent(Component component) {
		this.grid.putRelative(Constrained.defaultOf(component), FlexGridConstraints.Relative.DEFAULT_PLACEMENT);
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
		if (!this.grid.isEmpty()) {
			this.layoutAxis(parent, CartesianOperations.X);
			this.layoutAxis(parent, CartesianOperations.Y);
		}
	}

	private void layoutAxis(Container parent, CartesianOperations ops) {
		final Insets insets = parent.getInsets();
		final int leadingInset = ops.getLeadingInset(insets);

		final int availableSpace = ops.getParentSpace(parent) - leadingInset - ops.getTrailingInset(insets);

		final Sizes preferred = this.getPreferredSizes();

		final int extraSpace = availableSpace - ops.getTotalSpace(preferred);
		if (extraSpace >= 0) {
			if (extraSpace == 0 || ops.noneFill(this.grid)) {
				this.layoutAxisImpl(leadingInset + extraSpace / 2, ops, ops.getCellSpans(preferred));
			} else {
				final ImmutableMap<Integer, Integer> cellSpans = this.allocateCellSpace(ops, extraSpace, true);

				final int allocatedSpace = sumOrMax(cellSpans.values());
				final int startPos = leadingInset + (availableSpace - allocatedSpace) / 2;

				this.layoutAxisImpl(startPos, ops, cellSpans);
			}
		} else {
			final Sizes min = this.getMinSizes();

			final int extraMinSpace = availableSpace - ops.getTotalSpace(min);
			if (extraMinSpace <= 0) {
				this.layoutAxisImpl(leadingInset, ops, ops.getCellSpans(min));
			} else {
				final ImmutableMap<Integer, Integer> cellSpans = this.allocateCellSpace(ops, extraMinSpace, false);

				this.layoutAxisImpl(leadingInset, ops, cellSpans);
			}
		}
	}

	private ImmutableMap<Integer, Integer> allocateCellSpace(
			CartesianOperations ops, int remainingSpace, boolean fill
	) {
		final Sizes large;
		final Sizes small;
		if (fill) {
			large = this.getMaxSizes();
			small = this.getPreferredSizes();
		} else {
			large = this.getPreferredSizes();
			small = this.getMinSizes();
		}

		final SortedMap<Integer, Integer> cellSpans = new TreeMap<>(ops.getCellSpans(small));

		final List<Constrained.At> prioritized = this.grid
				.map((x, y, constrained) -> fill && !ops.fills(constrained)
						? Optional.<Constrained.At>empty()
						: Optional.of(constrained.new At(ops.chooseCoord(x, y)))
				)
				.flatMap(Optional::stream)
				.sorted()
				.toList();

		for (final Constrained.At at : prioritized) {
			final int extent = ops.getExtent(at.constrained());
			final Size targetSize = large.componentSizes.get(at.constrained().component);
			assert targetSize != null;

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

			if (remainingSpace <= 0) {
				break;
			}
		}

		// ImmutableMaps maintain order and provide O(1) lookups
		return ImmutableMap.copyOf(cellSpans);
	}

	/**
	 * @implNote the passed {@code cellSpans} <em>must</em> be ordered with increasing keys
	 */
	private void layoutAxisImpl(int startPos, CartesianOperations ops, ImmutableMap<Integer, Integer> cellSpans) {
		final Map<Integer, Integer> beginPositions = new HashMap<>();
		int currentPos = startPos;
		for (final Map.Entry<Integer, Integer> entry : cellSpans.entrySet()) {
			final int coord = entry.getKey();
			final int span = entry.getValue();

			beginPositions.put(coord, currentPos);

			currentPos += span;
		}

		final Sizes preferred = this.getPreferredSizes();
		final Sizes max = this.getMaxSizes();

		this.grid.forEach((x, y, values) -> {
			final int coord = ops.chooseCoord(x, y);

			final int beginPos = beginPositions.get(coord);

			values.forEach(constrained -> {
				final int extent = ops.getExtent(constrained);

				int extendedCellSpan = 0;
				for (int i = 0; i < extent; i++) {
					final Integer span = cellSpans.get(coord + i);
					assert span != null;
					extendedCellSpan += span;
				}

				final Sizes targets = ops.fills(constrained) ? max : preferred;
				final Size targetSize = targets.componentSizes.get(constrained.component);
				assert targetSize != null;

				final int span = Math.min(ops.getSpan(targetSize), extendedCellSpan);

				final int constrainedPos = switch (ops.getAlignment(constrained)) {
					case BEGIN -> beginPos;
					case CENTER -> beginPos + (extendedCellSpan - span) / 2;
					case END -> beginPos + extendedCellSpan - span;
				};

				ops.setBounds(constrained.component, constrainedPos, span);
			});
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

		static Constrained of(Component component, FlexGridConstraints<?> constraints) {
			return new Constrained(
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

	private record Size(int width, int height) {
		static Size of(Dimension dimension) {
			return new Size(dimension.width, dimension.height);
		}
	}

	/**
	 * A collection of sizes and size metrics used for calculating min/max/preferred container size and
	 * for laying out the container.
	 *
	 * @implNote {@link #rowHeights} and {@link #columnWidths} are ordered with increasing keys
	 */
	private record Sizes(
			int totalWidth, int totalHeight,
			ImmutableMap<Integer, Integer> rowHeights, ImmutableMap<Integer, Integer> columnWidths,
			ImmutableMap<Component, Size> componentSizes
	) {
		static Sizes EMPTY = new Sizes(
				0, 0,
				ImmutableSortedMap.of(), ImmutableSortedMap.of(),
				ImmutableMap.of()
		);

		static Sizes calculate(ConstrainedGrid grid, Function<Component, Dimension> getSize) {
			if (grid.isEmpty()) {
				return EMPTY;
			}

			final Map<Component, Size> componentSizes = new HashMap<>();

			final Map<Integer, Map<Integer, Dimension>> cellSizes = new HashMap<>();

			grid.forEach((x, y, values) -> {
				values.forEach(constrained -> {
					final Size size = componentSizes
							.computeIfAbsent(constrained.component, component -> Size.of(getSize.apply(component)));

					final int componentCellWidth = ceilDiv(size.width, constrained.xExtent);
					final int componentCellHeight = ceilDiv(size.height, constrained.yExtent);
					for (int xOffset = 0; xOffset < constrained.xExtent; xOffset++) {
						for (int yOffset = 0; yOffset < constrained.yExtent; yOffset++) {
							final Dimension cellSize = cellSizes
									.computeIfAbsent(x + xOffset, ignored -> new HashMap<>())
									.computeIfAbsent(y + yOffset, ignored -> new Dimension());

							cellSize.width = Math.max(cellSize.width, componentCellWidth);
							cellSize.height = Math.max(cellSize.height, componentCellHeight);
						}
					}
				});
			});

			final SortedMap<Integer, Integer> rowHeights = new TreeMap<>();
			final SortedMap<Integer, Integer> columnWidths = new TreeMap<>();
			cellSizes.forEach((x, column) -> {
				column.forEach((y, size) -> {
					rowHeights.compute(y, (ignored, height) -> height == null
							? size.height
							: Math.max(height, size.height)
					);
					columnWidths.compute(x, (ignored, width) -> width == null
							? size.width
							: Math.max(width, size.width)
					);
				});
			});

			return new Sizes(
				sumOrMax(columnWidths.values()),
				sumOrMax(rowHeights.values()),
				// ImmutableMaps maintain order and provide O(1) lookups
				ImmutableMap.copyOf(rowHeights), ImmutableMap.copyOf(columnWidths),
				ImmutableMap.copyOf(componentSizes)
			);
		}

		Dimension createTotalDimension(Insets insets) {
			return new Dimension(
				positiveOrMax(this.totalWidth + insets.left + insets.right),
				positiveOrMax(this.totalHeight + insets.top + insets.bottom)
			);
		}
	}

	/**
	 * Sets of operations for the {@link #X} and {@link #Y} axes of a cartesian plane.
	 */
	enum CartesianOperations {
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
			int getSpan(Size size) {
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
			int getExcess(Constrained constrained) {
				return constrained.getXExcess();
			}

			@Override
			Position createPos(int coord, int oppositeCoord) {
				return new Position(coord, oppositeCoord);
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
			int getSpan(Size size) {
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
			int getExcess(Constrained constrained) {
				return constrained.getYExcess();
			}

			@Override
			Position createPos(int coord, int oppositeCoord) {
				return new Position(oppositeCoord, coord);
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

		int chooseCoord(Position pos) {
			return this.chooseCoord(pos.x(), pos.y());
		}

		abstract int getLeadingInset(Insets insets);
		abstract int getTrailingInset(Insets insets);
		abstract int getParentSpace(Container parent);

		abstract int getTotalSpace(Sizes sizes);
		abstract ImmutableMap<Integer, Integer> getCellSpans(Sizes sizes);
		abstract int getSpan(Size size);

		abstract boolean fills(Constrained constrained);
		abstract boolean noneFill(ConstrainedGrid grid);
		abstract Alignment getAlignment(Constrained constrained);
		abstract int getExtent(Constrained constrained);
		abstract int getExcess(Constrained constrained);

		abstract Position createPos(int coord, int oppositeCoord);

		abstract void setBounds(Component component, int pos, int span);

		abstract CartesianOperations opposite();
	}
}
