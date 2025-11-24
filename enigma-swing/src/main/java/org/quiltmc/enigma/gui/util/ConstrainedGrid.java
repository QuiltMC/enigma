package org.quiltmc.enigma.gui.util;

import org.quiltmc.enigma.gui.util.FlexGridLayout.Constrained;

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
 * Only designed for use with {@link FlexGridLayout}.
 *
 * <p> Multiple values can be associated with the same coordinates,
 * but a value may only be associated with one coordinate pair at a time.
 */
class ConstrainedGrid {
	private static Set<Component> createValueSet(Integer ignored) {
		return new HashSet<>();
	}

	private final Map<Integer, Map<Integer, Map<Component, Constrained>>> grid = new HashMap<>();
	private final Map<Component, Position> valuePositions = new HashMap<>();
	private final SortedMap<Integer, Set<Component>> valuesByMaxX = new TreeMap<>();
	private final SortedMap<Integer, Set<Component>> valuesByMaxY = new TreeMap<>();

	void put(int x, int y, Constrained value) {
		final Component component = value.component();
		final Position oldPos = this.valuePositions.replace(component, new Position(x, y));
		if (oldPos != null) {
			final Map<Integer, Map<Component, Constrained>> column = this.grid.get(oldPos.x);
			final Constrained oldValue = column.get(oldPos.y).get(component);
			column.get(oldPos.y).remove(component, value);
			this.valuesByMaxX.get(oldPos.x + oldValue.getXExcess()).remove(component);
			this.valuesByMaxY.get(oldPos.y + oldValue.getYExcess()).remove(component);
		}

		this.grid
				.computeIfAbsent(x, ignored -> new HashMap<>())
				.computeIfAbsent(y, ignored -> new HashMap<>(1))
				.put(component, value);
		this.valuesByMaxX.computeIfAbsent(x + value.getXExcess(), ConstrainedGrid::createValueSet).add(component);
		this.valuesByMaxY.computeIfAbsent(y + value.getYExcess(), ConstrainedGrid::createValueSet).add(component);
	}

	Stream<Constrained> get(int x, int y) {
		return this.grid.getOrDefault(x, Map.of())
			.getOrDefault(y, Map.of())
			.values()
			.stream();
	}

	boolean remove(Component value) {
		final Position pos = this.valuePositions.remove(value);
		if (pos != null) {
			final Constrained removed = this.grid.get(pos.x).get(pos.y).remove(value);
			this.valuesByMaxX.get(pos.x + removed.getXExcess()).remove(value);
			this.valuesByMaxY.get(pos.y + removed.getYExcess()).remove(value);

			return true;
		} else {
			return false;
		}
	}

	int getMaxXOrThrow() {
		return this.valuesByMaxX.lastKey();
	}

	int getMaxYOrThrow() {
		return this.valuesByMaxY.lastKey();
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
