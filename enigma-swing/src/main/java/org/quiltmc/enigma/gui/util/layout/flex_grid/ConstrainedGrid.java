package org.quiltmc.enigma.gui.util.layout.flex_grid;

import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout.CartesianOperations;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout.Constrained;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;

import java.awt.Component;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * A map of cartesian coordinates to {@link Constrained} values.<br>
 * Only designed for use in {@link FlexGridLayout}.
 *
 * <p> Multiple values can be associated with the same coordinates,
 * but a value may only be associated with one coordinate pair at a time.
 */
class ConstrainedGrid {
	// outer sorted map maps y coordinates to rows
	// inner sorted map maps x coordinates to values
	// component map holds values by component
	private final SortedMap<Integer, SortedMap<Integer, Map<Component, Constrained>>> grid = new TreeMap<>();
	private final Map<Component, Position> componentPositions = new HashMap<>();
	// outer sorted map maps constrained max y to rows
	// mid sorted map maps constrained max x to values by min y
	// used to find relative placements
	private final SortedMap<Integer, SortedMap<Integer, SortedMap<Integer, Set<Component>>>> maxYXGrid = new TreeMap<>();
	// outer sorted map maps constrained max x to columns
	// mid sorted map maps constrained max y to values by min x
	// used to find relative placements
	private final SortedMap<Integer, SortedMap<Integer, SortedMap<Integer, Set<Component>>>> maxXYGrid = new TreeMap<>();

	// used to find relative placements
	private final SortedMap<Integer, Set<Component>> componentsByX = new TreeMap<>();

	private final Set<Component> xFillers = new HashSet<>();
	private final Set<Component> yFillers = new HashSet<>();

	void put(int x, int y, Constrained value) {
		final Component component = value.component();

		this.remove(value.component());

		this.componentPositions.put(component, new Position(x, y));

		this.grid
				.computeIfAbsent(y, ignored -> new TreeMap<>())
				.computeIfAbsent(x, ignored -> new HashMap<>(1))
				.put(component, value);

		final int maxY = y + value.getYExcess();
		final int maxX = x + value.getXExcess();

		this.maxYXGrid
				.computeIfAbsent(maxY, ignored -> new TreeMap<>())
				.computeIfAbsent(maxX, ignored -> new TreeMap<>())
				.computeIfAbsent(y, ignored -> new HashSet<>(1))
				.add(component);

		this.maxXYGrid
				.computeIfAbsent(maxX, ignore -> new TreeMap<>())
				.computeIfAbsent(maxY, ignored -> new TreeMap<>())
				.computeIfAbsent(x, ignored -> new HashSet<>(1))
				.add(component);

		this.componentsByX.computeIfAbsent(x, ignored -> new HashSet<>()).add(component);

		if (value.fillX()) {
			this.xFillers.add(component);
		}

		if (value.fillY()) {
			this.yFillers.add(component);
		}
	}

	void putRelative(Constrained value, FlexGridConstraints.Relative.Placement placement) {
		final int x;
		final int y;
		if (this.isEmpty()) {
			x = FlexGridConstraints.Absolute.DEFAULT_X;
			y = FlexGridConstraints.Absolute.DEFAULT_Y;
		} else {
			switch (placement) {
				case ROW_END -> {
					// final int maxY = this.maxYXGrid.lastKey();
					// final SortedMap<Integer, SortedMap<Integer, Set<Component>>> maxXRow = this.maxYXGrid.get(maxY);
					// final SortedMap<Integer, Set<Component>> maxXComponentsByY = maxXRow.get(maxXRow.lastKey());
					//
					// final Set<Component> minYMaxXComponents = maxXComponentsByY.get(maxXComponentsByY.firstKey());
					// final Component component = minYMaxXComponents.iterator().next();
					// final Position pos = this.componentPositions.get(component);
					//
					// x = pos.x + this.grid.get(pos.y).get(pos.x).get(component).getXExcess() + 1;
					// y = pos.y;
					final Position pos = this.findEndPos(CartesianOperations.X, this.maxYXGrid);
					x = pos.x;
					y = pos.y;
				}
				case NEW_ROW -> {
					// min x
					x = this.componentsByX.firstKey();
					// max y + 1
					y = this.maxYXGrid.lastKey() + 1;
				}
				case COLUMN_END -> {
					// final int maxX = this.maxXYGrid.lastKey();
					// final SortedMap<Integer, SortedMap<Integer, Set<Component>>> maxYRow = this.maxXYGrid.get(maxX);
					// final SortedMap<Integer, Set<Component>> maxYComponentsByX = maxYRow.get(maxYRow.lastKey());
					//
					// final Set<Component> minXMaxYComponents = maxYComponentsByX.get(maxYComponentsByX.firstKey());
					// final Component component = minXMaxYComponents.iterator().next();
					// final Position pos = this.componentPositions.get(component);
					//
					// x = pos.x;
					// y = pos.y + this.grid.get(pos.y).get(pos.x).get(component).getYExcess() + 1;
					final Position pos = this.findEndPos(CartesianOperations.Y, this.maxXYGrid);
					x = pos.x;
					y = pos.y;
				}
				case NEW_COLUMN -> {
					// max x + 1
					x = this.maxXYGrid.lastKey() + 1;
					// min y
					y = this.grid.firstKey();
				}
				default -> throw new AssertionError();
			}
		}

		this.put(x, y, value);
	}

	private Position findEndPos(
			CartesianOperations ops,
			SortedMap<Integer, SortedMap<Integer, SortedMap<Integer, Set<Component>>>> maxGrid
	) {
		final int max = maxGrid.lastKey();
		final SortedMap<Integer, SortedMap<Integer, Set<Component>>> maxXRow = maxGrid.get(max);
		final SortedMap<Integer, Set<Component>> maxXComponentsByY = maxXRow.get(maxXRow.lastKey());

		final Set<Component> minYMaxXComponents = maxXComponentsByY.get(maxXComponentsByY.firstKey());
		final Component component = minYMaxXComponents.iterator().next();
		final Position pos = this.componentPositions.get(component);

		final int coord = ops.chooseCoord(pos) + ops.getExcess(this.grid.get(pos.y).get(pos.x).get(component)) + 1;
		final int oppositeCoord = ops.opposite().chooseCoord(pos);

		return ops.createPos(coord, oppositeCoord);
	}

	void remove(Component component) {
		final Position pos = this.componentPositions.remove(component);
		if (pos != null) {
			final SortedMap<Integer, Map<Component, Constrained>> row = this.grid.get(pos.y);
			final Map<Component, Constrained> values = row.get(pos.x);
			final Constrained removed = values.remove(component);

			if (values.isEmpty()) {
				row.remove(pos.x);

				if (row.isEmpty()) {
					this.grid.remove(pos.y);
				}
			}

			final int maxY = pos.y + removed.getYExcess();
			final int maxX = pos.x + removed.getXExcess();

			this.removeFromMaxGrid(component, pos, maxX, maxY, CartesianOperations.Y, this.maxYXGrid);
			// final SortedMap<Integer, SortedMap<Integer, Set<Component>>> maxXRow = this.maxYXGrid.get(maxY);
			// final SortedMap<Integer, Set<Component>> maximumsByY = maxXRow.get(maxX);
			// final Set<Component> maxXComponents = maximumsByY.get(pos.y);
			// maxXComponents.remove(component);
			//
			// if (maxXComponents.isEmpty()) {
			// 	maximumsByY.remove(pos.y);
			//
			// 	if (maximumsByY.isEmpty()) {
			// 		maxXRow.remove(maxX);
			//
			// 		if (maxXRow.isEmpty()) {
			// 			this.maxYXGrid.remove(maxY);
			// 		}
			// 	}
			// }

			this.removeFromMaxGrid(component, pos, maxX, maxY, CartesianOperations.X, this.maxXYGrid);
			// final SortedMap<Integer, SortedMap<Integer, Set<Component>>> maxYRow = this.maxXYGrid.get(maxX);
			// final SortedMap<Integer, Set<Component>> maximumsByX = maxYRow.get(maxY);
			// final Set<Component> maxYComponents = maximumsByX.get(pos.x);
			// maxYComponents.remove(component);
			//
			// if (maxYComponents.isEmpty()) {
			// 	maximumsByX.remove(pos.y);
			//
			// 	if (maximumsByX.isEmpty()) {
			// 		maxYRow.remove(maxY);
			//
			// 		if (maxYRow.isEmpty()) {
			// 			this.maxXYGrid.remove(maxX);
			// 		}
			// 	}
			// }

			final Set<Component> xComponents = this.componentsByX.get(pos.x);
			xComponents.remove(component);
			if (xComponents.isEmpty()) {
				this.componentsByX.remove(pos.x);
			}

			this.xFillers.remove(component);
			this.yFillers.remove(component);
		}
	}

	private void removeFromMaxGrid(
			Component component, Position pos, int maxX, int maxY, CartesianOperations ops,
			SortedMap<Integer, SortedMap<Integer, SortedMap<Integer, Set<Component>>>> maxGrid
	) {
		final int coord = ops.chooseCoord(pos);
		final int max = ops.chooseCoord(maxX, maxY);
		final int oppositeMax = ops.opposite().chooseCoord(maxX, maxY);

		final SortedMap<Integer, SortedMap<Integer, Set<Component>>> maxRow = maxGrid.get(max);
		final SortedMap<Integer, Set<Component>> maximumsByCoord = maxRow.get(oppositeMax);
		final Set<Component> maxComponents = maximumsByCoord.get(coord);
		maxComponents.remove(component);

		if (maxComponents.isEmpty()) {
			maximumsByCoord.remove(coord);

			if (maximumsByCoord.isEmpty()) {
				maxRow.remove(oppositeMax);

				if (maxRow.isEmpty()) {
					maxGrid.remove(max);
				}
			}
		}
	}

	boolean noneFillX() {
		return this.xFillers.isEmpty();
	}

	boolean noneFillY() {
		return this.yFillers.isEmpty();
	}

	boolean isEmpty() {
		return this.componentPositions.isEmpty();
	}

	void forEach(EntriesConsumer action) {
		this.grid.forEach((y, row) -> {
			row.forEach((x, constrainedByComponent) -> {
				action.accept(x, y, constrainedByComponent.values().stream());
			});
		});
	}

	<T> Stream<T> map(EntryFunction<T> mapper) {
		return this.grid.entrySet().stream().flatMap(rowEntry -> rowEntry
			.getValue()
			.entrySet()
			.stream()
			.flatMap(columnEntry -> columnEntry
				.getValue()
				.values()
				.stream()
				.map(constrained -> mapper.apply(columnEntry.getKey(), rowEntry.getKey(), constrained))
			)
		);
	}

	@FunctionalInterface
	interface EntriesConsumer {
		void accept(int x, int y, Stream<Constrained> values);
	}

	@FunctionalInterface
	interface EntryFunction<T> {
		T apply(int x, int y, Constrained value);
	}

	record Position(int x, int y) { }
}
