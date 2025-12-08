package org.quiltmc.enigma.gui.util.layout.flex_grid;

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
	// used to find relative position
	private final SortedMap<Integer, SortedMap<Integer, SortedMap<Integer, Set<Component>>>> maxGrid = new TreeMap<>();
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

		this.maxGrid
				.computeIfAbsent(y + value.getYExcess(), ignored -> new TreeMap<>())
				.computeIfAbsent(x + value.getXExcess(), ignored -> new TreeMap<>())
				.computeIfAbsent(y, ignored -> new HashSet<>(1))
				.add(component);

		if (value.fillX()) {
			this.xFillers.add(component);
		}

		if (value.fillY()) {
			this.yFillers.add(component);
		}
	}

	void putRelative(Constrained value) {
		final int x;
		final int y;
		if (this.isEmpty()) {
			x = FlexGridConstraints.Absolute.DEFAULT_X;
			y = FlexGridConstraints.Absolute.DEFAULT_Y;
		} else {
			final int maxY = this.maxGrid.lastKey();
			final SortedMap<Integer, SortedMap<Integer, Set<Component>>> maxRow = this.maxGrid.get(maxY);
			final SortedMap<Integer, Set<Component>> maxXComponentsByY = maxRow.get(maxRow.lastKey());

			final Set<Component> minYMaxXComponents = maxXComponentsByY.get(maxXComponentsByY.firstKey());
			final Component component = minYMaxXComponents.iterator().next();
			final Position pos = this.componentPositions.get(component);

			x = pos.x + this.grid.get(pos.y).get(pos.x).get(component).getXExcess() + 1;
			y = pos.y;
		}

		this.put(x, y, value);
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
			final SortedMap<Integer, SortedMap<Integer, Set<Component>>> maxRow = this.maxGrid.get(maxY);
			final int maxX = pos.x + removed.getXExcess();
			final SortedMap<Integer, Set<Component>> maximumsByY = maxRow.get(maxX);
			final Set<Component> maxComponents = maximumsByY.get(pos.y);
			maxComponents.remove(component);

			if (maxComponents.isEmpty()) {
				maximumsByY.remove(pos.y);

				if (maximumsByY.isEmpty()) {
					maxRow.remove(maxX);

					if (maxRow.isEmpty()) {
						this.maxGrid.remove(maxY);
					}
				}
			}

			this.xFillers.remove(component);
			this.yFillers.remove(component);
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

	private record Position(int x, int y) { }

	@FunctionalInterface
	interface EntriesConsumer {
		void accept(int x, int y, Stream<Constrained> values);
	}

	@FunctionalInterface
	interface EntryFunction<T> {
		T apply(int x, int y, Constrained value);
	}
}
