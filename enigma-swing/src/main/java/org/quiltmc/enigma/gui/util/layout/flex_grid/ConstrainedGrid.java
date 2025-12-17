package org.quiltmc.enigma.gui.util.layout.flex_grid;

import org.quiltmc.enigma.gui.util.CartesianOperations;
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

	private final Map<Component, Coordinates> componentCoordinates = new HashMap<>();

	// outer sorted map maps constrained max y to rows
	// mid sorted map maps constrained max x to values by min y
	// used to find relative placements
	private final SortedMap<Integer, SortedMap<Integer, SortedMap<Integer, Set<Component>>>> maxYXGrid = new TreeMap<>();
	// outer sorted map maps constrained max x to columns
	// mid sorted map maps constrained max y to values by min x
	// used to find relative placements
	private final SortedMap<Integer, SortedMap<Integer, SortedMap<Integer, Set<Component>>>> maxXYGrid = new TreeMap<>();

	// used to find min x for relative placements
	private final SortedMap<Integer, Set<Component>> componentsByX = new TreeMap<>();

	private final Set<Component> xFillers = new HashSet<>();
	private final Set<Component> yFillers = new HashSet<>();

	void put(int x, int y, Constrained value) {
		final Component component = value.component();

		this.remove(value.component());

		this.componentCoordinates.put(component, new Coordinates(x, y));

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
					final Coordinates coords = this.findEndPos(Operations.X.INSTANCE, this.maxYXGrid);
					x = coords.x;
					y = coords.y;
				}
				case NEW_ROW -> {
					// min x
					x = this.componentsByX.firstKey();
					// max y + 1
					y = this.maxYXGrid.lastKey() + 1;
				}
				case COLUMN_END -> {
					final Coordinates coords = this.findEndPos(Operations.Y.INSTANCE, this.maxXYGrid);
					x = coords.x;
					y = coords.y;
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

	private Coordinates findEndPos(
			Operations innerOps,
			SortedMap<Integer, SortedMap<Integer, SortedMap<Integer, Set<Component>>>> maxGrid
	) {
		final int max = maxGrid.lastKey();
		final SortedMap<Integer, SortedMap<Integer, Set<Component>>> maxXRow = maxGrid.get(max);
		final SortedMap<Integer, Set<Component>> maxXComponentsByY = maxXRow.get(maxXRow.lastKey());

		final Set<Component> minYMaxXComponents = maxXComponentsByY.get(maxXComponentsByY.firstKey());
		final Component component = minYMaxXComponents.iterator().next();
		final Coordinates coords = this.componentCoordinates.get(component);

		final int coord = innerOps.chooseCoord(coords)
				+ innerOps.getExcess(this.grid.get(coords.y).get(coords.x).get(component))
				+ 1;
		final int oppositeCoord = innerOps.opposite().chooseCoord(coords);

		return innerOps.createPos(coord, oppositeCoord);
	}

	void remove(Component component) {
		final Coordinates coords = this.componentCoordinates.remove(component);
		if (coords != null) {
			final SortedMap<Integer, Map<Component, Constrained>> row = this.grid.get(coords.y);
			final Map<Component, Constrained> values = row.get(coords.x);
			final Constrained removed = values.remove(component);

			if (values.isEmpty()) {
				row.remove(coords.x);

				if (row.isEmpty()) {
					this.grid.remove(coords.y);
				}
			}

			final int maxY = coords.y + removed.getYExcess();
			final int maxX = coords.x + removed.getXExcess();

			this.removeFromMaxGrid(component, coords, maxX, maxY, Operations.Y.INSTANCE, this.maxYXGrid);
			this.removeFromMaxGrid(component, coords, maxX, maxY, Operations.X.INSTANCE, this.maxXYGrid);

			final Set<Component> xComponents = this.componentsByX.get(coords.x);
			xComponents.remove(component);
			if (xComponents.isEmpty()) {
				this.componentsByX.remove(coords.x);
			}

			this.xFillers.remove(component);
			this.yFillers.remove(component);
		}
	}

	private void removeFromMaxGrid(
			Component component, Coordinates coords, int maxX, int maxY, Operations outerOps,
			SortedMap<Integer, SortedMap<Integer, SortedMap<Integer, Set<Component>>>> maxGrid
	) {
		final int coord = outerOps.chooseCoord(coords);
		final int max = outerOps.chooseCoord(maxX, maxY);
		final int oppositeMax = outerOps.opposite().chooseCoord(maxX, maxY);

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
		return this.componentCoordinates.isEmpty();
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

	private record Coordinates(int x, int y) { }

	private interface Operations extends CartesianOperations<Operations> {
		class X implements Operations, CartesianOperations.X<Operations> {
			static final Operations.X INSTANCE = new Operations.X();

			@Override
			public int getExcess(Constrained constrained) {
				return constrained.getXExcess();
			}

			@Override
			public Coordinates createPos(int coord, int oppositeCoord) {
				return new Coordinates(coord, oppositeCoord);
			}

			@Override
			public Operations opposite() {
				return Operations.Y.INSTANCE;
			}
		}

		class Y implements Operations, CartesianOperations.Y<Operations> {
			static final Operations.Y INSTANCE = new Operations.Y();

			@Override
			public int getExcess(Constrained constrained) {
				return constrained.getYExcess();
			}

			@Override
			public Coordinates createPos(int coord, int oppositeCoord) {
				return new Coordinates(oppositeCoord, coord);
			}

			@Override
			public Operations opposite() {
				return Operations.X.INSTANCE;
			}
		}

		default int chooseCoord(Coordinates coords) {
			return this.chooseCoord(coords.x, coords.y);
		}

		int getExcess(Constrained constrained);

		Coordinates createPos(int coord, int oppositeCoord);
	}
}
