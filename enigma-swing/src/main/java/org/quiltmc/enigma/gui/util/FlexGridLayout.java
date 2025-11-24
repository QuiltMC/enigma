package org.quiltmc.enigma.gui.util;

import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.util.Objects;

public class FlexGridLayout implements LayoutManager2 {
	private final ConstrainedGrid grid = new ConstrainedGrid();

	@Override
	public void addLayoutComponent(Component component, @Nullable Object constraints) throws IllegalArgumentException {
		if (constraints == null) {
			this.addDefaultConstrainedLayoutComponent(component);
		} else if (constraints instanceof Constraints<?> typedConstraints) {
			this.addLayoutComponent(component, typedConstraints);
		} else {
			throw new IllegalArgumentException(
				"constraints type must be %s, but was %s!"
					.formatted(Constraints.class.getName(), constraints.getClass().getName())
			);
		}
	}

	public void addLayoutComponent(Component component, @Nullable Constraints<?> constraints) {
		if (constraints == null) {
			this.addDefaultConstrainedLayoutComponent(component);
		} else {
			final int x;
			final int y;
			if (constraints instanceof Constraints.Absolute absolute) {
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
		return this.grid.isEmpty() ? Constraints.Absolute.DEFAULT_X : this.grid.getMaxXOrThrow() + 1;
	}

	private int getRelativeY() {
		return this.grid.isEmpty() ? Constraints.Absolute.DEFAULT_Y : this.grid.getMaxYOrThrow();
	}

	@Override
	public void removeLayoutComponent(Component component) {
		this.grid.remove(component);
	}

	@Override
	public Dimension maximumLayoutSize(Container target) { }

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
		// TODO
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) { }

	@Override
	public Dimension minimumLayoutSize(Container parent) { }

	@Override
	public void layoutContainer(Container parent) {
		null
	}

	public enum Alignment {
		BEGIN, CENTER, END
	}

	public static sealed abstract class Constraints<C extends Constraints<C>> {
		private static final int DEFAULT_PRIORITY = 0;
		private static final int DEFAULT_WIDTH = 1;
		private static final int DEFAULT_HEIGHT = 1;
		private static final boolean DEFAULT_FILL_X = false;
		private static final boolean DEFAULT_FILL_Y = false;
		private static final Alignment DEFAULT_X_ALIGNMENT = Alignment.BEGIN;
		private static final Alignment DEFAULT_Y_ALIGNMENT = Alignment.CENTER;

		public static Relative createRelative() {
			return Relative.of();
		}

		public static Absolute createAbsolute() {
			return Absolute.of();
		}

		private static Alignment requireNonNullAlignment(Alignment alignment) {
			return Objects.requireNonNull(alignment, "alignment must not be null!");
		}

		int width;
		int height;

		boolean fillX;
		boolean fillY;

		Alignment xAlignment;
		Alignment yAlignment;

		int priority;

		private Constraints(
			int width, int height,
			boolean fillX, boolean fillY,
			Alignment xAlignment, Alignment yAlignment,
			int priority
		) {
			this.width = width;
			this.height = height;

			this.fillX = fillX;
			this.fillY = fillY;

			this.xAlignment = xAlignment;
			this.yAlignment = yAlignment;

			this.priority = priority;
		}

		public C width(int width) {
			Preconditions.checkArgument(width > 0, "width must be positive!");
			this.width = width;
			return this.getSelf();
		}

		public C height(int height) {
			Preconditions.checkArgument(height > 0, "height must be positive!");
			this.height = height;
			return this.getSelf();
		}

		public C fillX(boolean fill) {
			this.fillX = fill;
			return this.getSelf();
		}

		public C fillX() {
			return this.fillX(true);
		}

		public C fillY(boolean fill) {
			this.fillY = fill;
			return this.getSelf();
		}

		public C fillY() {
			return this.fillY(true);
		}

		public C fill(boolean x, boolean y) {
			this.fillX(x);
			this.fillY(y);
			return this.getSelf();
		}

		public C fillBoth() {
			return this.fill(true, true);
		}

		public C xAlignment(Alignment alignment) {
			this.xAlignment = requireNonNullAlignment(alignment);
			return this.getSelf();
		}

		public C alignLeft() {
			return this.xAlignment(Alignment.BEGIN);
		}

		public C alignRight() {
			return this.xAlignment(Alignment.END);
		}

		public C yAlignment(Alignment alignment) {
			this.yAlignment = requireNonNullAlignment(alignment);
			return this.getSelf();
		}

		public C alignTop() {
			return this.yAlignment(Alignment.BEGIN);
		}

		public C alignBottom() {
			return this.yAlignment(Alignment.END);
		}

		public C align(Alignment x, Alignment y) {
			this.xAlignment(x);
			this.yAlignment(y);
			return this.getSelf();
		}

		public C alignCenter() {
			return this.align(Alignment.CENTER, Alignment.CENTER);
		}

		public C priority(int priority) {
			this.priority = priority;
			return this.getSelf();
		}

		public abstract C copy();

		protected abstract C getSelf();

		public static final class Relative extends Constraints<Relative> {
			public static Relative of() {
				return new Relative(
					DEFAULT_WIDTH, DEFAULT_HEIGHT,
					DEFAULT_FILL_X, DEFAULT_FILL_Y,
					DEFAULT_X_ALIGNMENT, DEFAULT_Y_ALIGNMENT,
					DEFAULT_PRIORITY
				);
			}

			private Relative(
				int width, int height,
				boolean fillX, boolean fillY,
				Alignment xAlignment, Alignment yAlignment,
				int priority
			) {
				super(width, height, fillX, fillY, xAlignment, yAlignment, priority);
			}

			@Override
			public Relative copy() {
				return new Relative(
					this.width, this.height,
					this.fillX, this.fillY,
					this.xAlignment, this.yAlignment,
					this.priority
				);
			}

			public Absolute toAbsolute() {
				return new Absolute(
					Absolute.DEFAULT_X, Absolute.DEFAULT_Y,
					this.width, this.height,
					this.fillX, this.fillY,
					this.xAlignment, this.yAlignment,
					this.priority
				);
			}

			public Absolute toAbsolute(int x, int y) {
				return this.toAbsolute().pos(x, y);
			}

			@Override
			protected Relative getSelf() {
				return this;
			}
		}

		public static final class Absolute extends Constraints<Absolute> {
			private static final int DEFAULT_X = 0;
			private static final int DEFAULT_Y = 0;

			public static Absolute of() {
				return new Absolute(
					DEFAULT_X, DEFAULT_Y,
					DEFAULT_WIDTH, DEFAULT_HEIGHT,
					DEFAULT_FILL_X, DEFAULT_FILL_Y,
					DEFAULT_X_ALIGNMENT, DEFAULT_Y_ALIGNMENT,
					DEFAULT_PRIORITY
				);
			}

			private int x;
			private int y;

			private Absolute(
				int x, int y,
				int width, int height,
				boolean fillX, boolean fillY,
				Alignment xAlignment, Alignment yAlignment,
				int priority
			) {
				super(width, height, fillX, fillY, xAlignment, yAlignment, priority);

				this.x = x;
				this.y = y;
			}

			public Absolute x(int x) {
				this.x = x;
				return this;
			}

			public Absolute nextRow() {
				this.x++;
				this.y = 0;
				return this;
			}

			public Absolute y(int y) {
				this.y = y;
				return this;
			}

			public Absolute nextColumn() {
				this.y++;
				return this;
			}

			public Absolute pos(int x, int y) {
				this.x(x);
				this.y(y);
				return this;
			}

			@Override
			public Absolute copy() {
				return new Absolute(
					this.x, this.y,
					this.width, this.height,
					this.fillX, this.fillY,
					this.xAlignment, this.yAlignment,
					this.priority
				);
			}

			public Relative toRelative() {
				return new Relative(
					this.width, this.height,
					this.fillX, this.fillY,
					this.xAlignment, this.yAlignment,
					this.priority
				);
			}

			@Override
			protected Absolute getSelf() {
				return this;
			}
		}
	}

	record Constrained(
		Component component, int width, int height, boolean fillX, boolean fillY, Alignment xAlignment,
		Alignment yAlignment, int priority
	) {
		static Constrained defaultOf(Component component) {
			return new Constrained(
				component,
				Constraints.DEFAULT_WIDTH, Constraints.DEFAULT_HEIGHT,
				Constraints.DEFAULT_FILL_X, Constraints.DEFAULT_FILL_Y,
				Constraints.DEFAULT_X_ALIGNMENT, Constraints.DEFAULT_Y_ALIGNMENT,
				Constraints.DEFAULT_PRIORITY
			);
		}

		Constrained(Component component, Constraints<?> constraints) {
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
	}
}
