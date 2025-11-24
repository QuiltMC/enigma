package org.quiltmc.enigma.gui.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.util.HashMap;
import java.util.Map;

public class FlexGridLayout implements LayoutManager2 {
	private final Map<Integer, Multimap<Integer, Constrained>> grid = new HashMap<>();
	private final Map<Component, Pos> posByComponent = new HashMap<>();

	@Override
	public void addLayoutComponent(Component comp, Object constraints) throws ClassCastException {
		this.addLayoutComponent(comp, (Constraints) constraints);
	}

	public void addLayoutComponent(Component component, Constraints constraints) {
		final Constrained constrained = new Constrained(component, constraints);
		final Pos oldPos = this.posByComponent.replace(component, new Pos(constraints.x, constraints.y));
		if (oldPos != null) {
			this.grid.get(oldPos.x).remove(oldPos.y, constrained);
		}

		this.grid.computeIfAbsent(constraints.x, ignored -> HashMultimap.create(8, 1))
			.put(constraints.y, constrained);
	}

	@Override
	public Dimension maximumLayoutSize(Container target) { }

	@Override
	public float getLayoutAlignmentX(Container target) {
		return 0;
	}

	@Override
	public float getLayoutAlignmentY(Container target) {
		return 0;
	}

	@Override
	public void invalidateLayout(Container target) {
		// TODO
	}

	@Override
	public void addLayoutComponent(String name, Component comp) {
		null
	}

	@Override
	public void removeLayoutComponent(Component comp) {
		null
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) { }

	@Override
	public Dimension minimumLayoutSize(Container parent) { }

	@Override
	public void layoutContainer(Container parent) {
		null
	}

	public static final class Constraints {
		private static final int DEFAULT_X = 0;
		private static final int DEFAULT_Y = 0;
		private static final int DEFAULT_PRIORITY = 0;
		private static final Alignment DEFAULT_X_ALIGNMENT = Alignment.BEGIN;
		private static final Alignment DEFAULT_Y_ALIGNMENT = Alignment.CENTER;
		private static final boolean DEFAULT_FILL_X = false;
		private static final boolean DEFAULT_FILL_Y = false;

		public static Constraints of() {
			return new Constraints(
				DEFAULT_X, DEFAULT_Y,
				DEFAULT_PRIORITY,
				DEFAULT_X_ALIGNMENT, DEFAULT_Y_ALIGNMENT,
				DEFAULT_FILL_X, DEFAULT_FILL_Y
			);
		}

		private int x;
		private int y;

		private int priority;

		private Alignment xAlignment;
		private Alignment yAlignment;

		private boolean fillX;
		private boolean fillY;

		private Constraints(
			int x, int y,
			int priority,
			Alignment xAlignment, Alignment yAlignment,
			boolean fillX, boolean fillY
		) {
			this.x = x;
			this.y = y;

			this.priority = priority;

			this.xAlignment = xAlignment;
			this.yAlignment = yAlignment;

			this.fillX = fillX;
			this.fillY = fillY;
		}

		public Constraints x(int x) {
			Preconditions.checkArgument(x >= 0, "x must not be negative!");
			this.x = x;
			return this;
		}

		public Constraints nextRow() {
			this.x++;
			this.y = 0;
			return this;
		}

		public Constraints y(int y) {
			Preconditions.checkArgument(y >= 0, "y must not be negative!");
			this.y = y;
			return this;
		}

		public Constraints nextColumn() {
			this.y++;
			return this;
		}

		public Constraints pos(int x, int y) {
			this.x(x);
			this.y(y);
			return this;
		}

		public Constraints priority(int priority) {
			this.priority = priority;
			return this;
		}

		public Constraints xAlignment(Alignment alignment) {
			this.xAlignment = alignment;
			return this;
		}

		public Constraints alignLeft() {
			return this.xAlignment(Alignment.BEGIN);
		}

		public Constraints alignRight() {
			return this.xAlignment(Alignment.END);
		}

		public Constraints yAlignment(Alignment alignment) {
			this.yAlignment = alignment;
			return this;
		}

		public Constraints alignTop() {
			return this.yAlignment(Alignment.BEGIN);
		}

		public Constraints alignBottom() {
			return this.yAlignment(Alignment.END);
		}

		public Constraints align(Alignment x, Alignment y) {
			this.xAlignment(x);
			this.yAlignment(y);
			return this;
		}

		public Constraints alignCenter() {
			return this.align(Alignment.CENTER, Alignment.CENTER);
		}

		public Constraints fillX(boolean fill) {
			this.fillX = fill;
			return this;
		}

		public Constraints fillX() {
			return this.fillX(true);
		}

		public Constraints fillY(boolean fill) {
			this.fillY = fill;
			return this;
		}

		public Constraints fillY() {
			return this.fillY(true);
		}

		public Constraints fill(boolean x, boolean y) {
			this.fillX(x);
			this.fillY(y);
			return this;
		}

		public Constraints fillBoth() {
			return this.fill(true, true);
		}

		public Constraints copy() {
			return new Constraints(
				this.x, this.y,
				this.priority,
				this.xAlignment, this.yAlignment,
				this.fillX, this.fillY
			);
		}

		public enum Alignment {
			BEGIN, CENTER, END
		}
	}

	private record Constrained(
		Component component, int priority,
		Constraints.Alignment xAlignment, Constraints.Alignment yAlignment,
		boolean fillX, boolean fillY
	) {
		Constrained(Component component, Constraints constraints) {
			this(
				component, constraints.priority,
				constraints.xAlignment, constraints.yAlignment,
				constraints.fillX, constraints.fillY
			);
		}
	}

	private record Pos(int x, int y) { }
}
