package org.quiltmc.enigma.gui.util.layout.flex_grid.constraints;

import com.google.common.base.Preconditions;

import java.util.Objects;

@SuppressWarnings("unused")
public abstract sealed class FlexGridConstraints<C extends FlexGridConstraints<C>> {
	public static final int DEFAULT_PRIORITY = 0;
	public static final int DEFAULT_WIDTH = 1;
	public static final int DEFAULT_HEIGHT = 1;
	public static final boolean DEFAULT_FILL_X = false;
	public static final boolean DEFAULT_FILL_Y = false;
	public static final Alignment DEFAULT_X_ALIGNMENT = Alignment.BEGIN;
	public static final Alignment DEFAULT_Y_ALIGNMENT = Alignment.CENTER;

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

	private FlexGridConstraints(
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

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public boolean fillsX() {
		return this.fillX;
	}

	public boolean fillsY() {
		return this.fillY;
	}

	public Alignment getXAlignment() {
		return this.xAlignment;
	}

	public Alignment getYAlignment() {
		return this.yAlignment;
	}

	public int getPriority() {
		return this.priority;
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

	public C size(int width, int height) {
		this.width(width);
		this.height(height);
		return this.getSelf();
	}

	public C fillX(boolean fill) {
		this.fillX = fill;
		return this.getSelf();
	}

	public C fillX() {
		return this.fillX(true);
	}

	public C fillOnlyX() {
		return this.fill(true, false);
	}

	public C fillY(boolean fill) {
		this.fillY = fill;
		return this.getSelf();
	}

	public C fillY() {
		return this.fillY(true);
	}

	public C fillOnlyY() {
		return this.fill(false, true);
	}

	public C fill(boolean x, boolean y) {
		this.fillX(x);
		this.fillY(y);
		return this.getSelf();
	}

	public C fillBoth() {
		return this.fill(true, true);
	}

	public C fillNone() {
		return this.fill(false, false);
	}

	public C alignX(Alignment alignment) {
		this.xAlignment = requireNonNullAlignment(alignment);
		return this.getSelf();
	}

	public C alignLeft() {
		return this.alignX(Alignment.BEGIN);
	}

	public C alignCenterX() {
		return this.alignX(Alignment.CENTER);
	}

	public C alignRight() {
		return this.alignX(Alignment.END);
	}

	public C alignY(Alignment alignment) {
		this.yAlignment = requireNonNullAlignment(alignment);
		return this.getSelf();
	}

	public C alignTop() {
		return this.alignY(Alignment.BEGIN);
	}

	public C alignCenterY() {
		return this.alignY(Alignment.CENTER);
	}

	public C alignBottom() {
		return this.alignY(Alignment.END);
	}

	public C align(Alignment x, Alignment y) {
		this.alignX(x);
		this.alignY(y);
		return this.getSelf();
	}

	public C alignTopLeft() {
		return this.align(Alignment.BEGIN, Alignment.BEGIN);
	}

	public C alignTopCenter() {
		return this.align(Alignment.CENTER, Alignment.BEGIN);
	}

	public C alignTopRight() {
		return this.align(Alignment.END, Alignment.BEGIN);
	}

	public C alightCenterLeft() {
		return this.align(Alignment.BEGIN, Alignment.CENTER);
	}

	public C alignCenter() {
		return this.align(Alignment.CENTER, Alignment.CENTER);
	}

	public C alignCenterRight() {
		return this.align(Alignment.END, Alignment.CENTER);
	}

	public C alignBottomLeft() {
		return this.align(Alignment.BEGIN, Alignment.END);
	}

	public C alignBottomCenter() {
		return this.align(Alignment.CENTER, Alignment.END);
	}

	public C alignBottomRight() {
		return this.align(Alignment.END, Alignment.END);
	}

	public C priority(int priority) {
		this.priority = priority;
		return this.getSelf();
	}

	public abstract C copy();

	abstract C getSelf();

	public enum Alignment {
		BEGIN, CENTER, END
	}

	public static final class Relative extends FlexGridConstraints<FlexGridConstraints.Relative> {
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
		Relative getSelf() {
			return this;
		}
	}

	public static final class Absolute extends FlexGridConstraints<FlexGridConstraints.Absolute> {
		public static final int DEFAULT_X = 0;
		public static final int DEFAULT_Y = 0;

		public static Absolute of() {
			return new Absolute(
				DEFAULT_X, DEFAULT_Y,
				DEFAULT_WIDTH, DEFAULT_HEIGHT,
				DEFAULT_FILL_X, DEFAULT_FILL_Y,
				DEFAULT_X_ALIGNMENT, DEFAULT_Y_ALIGNMENT,
				DEFAULT_PRIORITY
			);
		}

		int x;
		int y;

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

		public int getX() {
			return this.x;
		}

		public int getY() {
			return this.y;
		}

		public Absolute x(int x) {
			this.x = x;
			return this;
		}

		public Absolute advanceRows(int count) {
			this.x = 0;
			this.y += count;
			return this.getSelf();
		}

		public Absolute nextRow() {
			return this.advanceRows(1);
		}

		public Absolute y(int y) {
			this.y = y;
			return this;
		}

		public Absolute advanceColumns(int count) {
			this.x += count;
			return this.getSelf();
		}

		public Absolute nextColumn() {
			return this.advanceColumns(1);
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
		Absolute getSelf() {
			return this;
		}
	}
}
