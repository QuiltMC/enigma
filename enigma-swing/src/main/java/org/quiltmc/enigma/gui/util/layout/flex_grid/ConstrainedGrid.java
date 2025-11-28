package org.quiltmc.enigma.gui.util.layout.flex_grid;

import com.google.common.collect.ImmutableSortedMap;
import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout.Constrained;

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
	private static Set<Component> createValueSet(Integer ignored) {
		return new HashSet<>();
	}

	private final SortedMap<Integer, SortedMap<Integer, Map<Component, Constrained>>> grid = new TreeMap<>();
	private final Map<Component, Position> componentPositions = new HashMap<>();
	private final SortedMap<Integer, Set<Component>> valuesByMaxX = new TreeMap<>();
	private final SortedMap<Integer, Set<Component>> valuesByMaxY = new TreeMap<>();
	private final Set<Component> xFillers = new HashSet<>();
	private final Set<Component> yFillers = new HashSet<>();

	void put(int x, int y, Constrained value) {
		final Component component = value.component();

		this.remove(value.component());

		this.grid
				.computeIfAbsent(x, ignored -> new TreeMap<>())
				.computeIfAbsent(y, ignored -> new HashMap<>(1))
				.put(component, value);

		this.valuesByMaxX.computeIfAbsent(x + value.getXExcess(), ConstrainedGrid::createValueSet).add(component);
		this.valuesByMaxY.computeIfAbsent(y + value.getYExcess(), ConstrainedGrid::createValueSet).add(component);

		if (value.fillX()) {
			this.xFillers.add(component);
		}

		if (value.fillY()) {
			this.yFillers.add(component);
		}
	}

	Stream<Constrained> get(int x, int y) {
		return this.grid.getOrDefault(x, ImmutableSortedMap.of())
			.getOrDefault(y, Map.of())
			.values()
			.stream();
	}

	void remove(Component component) {
		final Position pos = this.componentPositions.remove(component);
		if (pos != null) {
			final SortedMap<Integer, Map<Component, Constrained>> column = this.grid.get(pos.x);
			final Map<Component, Constrained> values = column.get(pos.y);
			final Constrained removed = values.remove(component);
			if (values.isEmpty()) {
				column.remove(pos.y);

				if (column.isEmpty()) {
					this.grid.remove(pos.x);
				}
			}

			final int maxX = pos.x + removed.getXExcess();
			final Set<Component> maxXValues = this.valuesByMaxX.get(maxX);
			maxXValues.remove(component);
			if (maxXValues.isEmpty()) {
				this.valuesByMaxX.remove(maxX);
			}

			final int maxY = pos.y + removed.getYExcess();
			final Set<Component> maxYValues = this.valuesByMaxY.get(maxY);
			maxYValues.remove(component);
			if (maxYValues.isEmpty()) {
				this.valuesByMaxY.remove(maxY);
			}

			this.xFillers.remove(component);
			this.yFillers.remove(component);
		}
	}

	int getMaxXOrThrow() {
		return this.valuesByMaxX.lastKey();
	}

	int getMaxYOrThrow() {
		return this.valuesByMaxY.lastKey();
	}

	boolean noneFillX() {
		return this.xFillers.isEmpty();
	}

	boolean noneFillY() {
		return this.yFillers.isEmpty();
	}

	int getSize() {
		return this.valuesByMaxX.size();
	}

	boolean isEmpty() {
		return this.valuesByMaxX.isEmpty();
	}

	void forEach(EntriesConsumer action) {
		this.grid.forEach((x, column) -> {
			column.forEach((y, constrainedByComponent) -> {
				action.accept(x, y, constrainedByComponent.values().stream());
			});
		});
	}

	private record Position(int x, int y) { }

	@FunctionalInterface
	interface EntriesConsumer {
		void accept(int x, int y, Stream<Constrained> values);
	}
}
