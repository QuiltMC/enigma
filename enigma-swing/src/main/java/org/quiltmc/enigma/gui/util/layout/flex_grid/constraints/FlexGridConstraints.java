package org.quiltmc.enigma.gui.util.layout.flex_grid.constraints;

import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import static org.quiltmc.enigma.util.Arguments.requirePositive;
import static org.quiltmc.enigma.util.Utils.requireNonNull;

/**
 * Constraints for components added to a {@link Container} with a {@link FlexGridLayout} using
 * {@link Container#add(Component, Object)}.<br>
 * {@link FlexGridConstraints} is to {@link GridBagConstraints} as
 * {@link FlexGridLayout} is to {@link GridBagLayout}.
 *
 * <h4>Differences from {@link GridBagConstraints}</h4>
 * <ul>
 *     <li> flex constraints have separate {@link Relative Relative} and {@link Absolute Absolute}
 *          types; {@link Absolute#toRelative() toRelative()} and {@link Relative#toAbsolute() toAbsolute()}
 *          convert between them
 *     <li> flex constraints support negative {@linkplain Absolute#pos(int, int) coordinates}
 *     <li> flex constraints don't use magic constants
 * </ul>
 *
 * <h4>Convenience</h4>
 * <ul>
 *     <li> constraints use the builder pattern; they're designed for method chaining
 *     <li> constraints are mutable, but their values are copied when
 *          {@linkplain Container#add(Component, Object) adding} to a container
 *     <li> they have numerous method variations for common use cases, including:
 *          <ul>
 *              <li> {@link Absolute#nextRow() nextRow()} and {@link Absolute#nextColumn() nextColumn()}
 *              <li> {@link #incrementPriority()} and {@link #decrementPriority()}
 *              <li> a method for each combination of vertical and horizontal alignments
 *              <li> {@link #copy()}
 *          </ul>
 * </ul>
 *
 * @param <C> the type of these constraints; usually not relevant to users
 */
@SuppressWarnings("unused")
public abstract sealed class FlexGridConstraints<C extends FlexGridConstraints<C>> {
	public static final int DEFAULT_PRIORITY = 0;
	public static final int DEFAULT_X_EXTENT = 1;
	public static final int DEFAULT_Y_EXTENT = 1;
	public static final boolean DEFAULT_FILL_X = false;
	public static final boolean DEFAULT_FILL_Y = false;
	public static final Alignment DEFAULT_X_ALIGNMENT = Alignment.BEGIN;
	public static final Alignment DEFAULT_Y_ALIGNMENT = Alignment.CENTER;

	private static final String EXTENT = "extent";
	private static final String ALIGNMENT = "alignment";

	public static Relative createRelative() {
		return Relative.of();
	}

	public static Absolute createAbsolute() {
		return Absolute.of();
	}

	/**
	 * Defaults to {@value #DEFAULT_X_EXTENT}.<br>
	 * Always positive.
	 */
	int xExtent;

	/**
	 * Defaults to {@value #DEFAULT_Y_EXTENT}.<br>
	 * Always positive.
	 */
	int yExtent;

	/**
	 * Defaults to {@value #DEFAULT_FILL_X}.
	 */
	boolean fillX;

	/**
	 * Defaults to {@value #DEFAULT_FILL_Y}.
	 */
	boolean fillY;

	/**
	 * Defaults to {@link #DEFAULT_X_ALIGNMENT}.
	 */
	Alignment xAlignment;

	/**
	 * Defaults to {@link #DEFAULT_Y_ALIGNMENT}.
	 */
	Alignment yAlignment;

	int priority;

	private FlexGridConstraints(
			int xExtent, int yExtent,
			boolean fillX, boolean fillY,
			Alignment xAlignment, Alignment yAlignment,
			int priority
	) {
		this.xExtent = xExtent;
		this.yExtent = yExtent;

		this.fillX = fillX;
		this.fillY = fillY;

		this.xAlignment = xAlignment;
		this.yAlignment = yAlignment;

		this.priority = priority;
	}

	public int getXExtent() {
		return this.xExtent;
	}

	public int getYExtent() {
		return this.yExtent;
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

	/**
	 * Sets {@link #xExtent} to the passed value.
	 *
	 * <p> The default values is {@value #DEFAULT_X_EXTENT}.
	 *
	 * <p> {@link #xExtent} controls the number of grid cells the constrained component occupies; it extends rightward.
	 *
	 * @throws IllegalArgumentException if the passed {@code extent} is not positive
	 *
	 * @see #yExtent(int)
	 * @see #extent(int, int)
	 */
	public C xExtent(int extent) {
		this.xExtent = requirePositive(extent, EXTENT);
		return this.getSelf();
	}

	/**
	 * Sets {@link #yExtent} to the passed value.
	 *
	 * <p> The default values is {@value #DEFAULT_Y_EXTENT}.
	 *
	 * <p> {@link #yExtent} controls the number of grid cells the constrained component occupies; it extends downward.
	 *
	 * @throws IllegalArgumentException if the passed {@code extent} is not positive
	 *
	 * @see #xExtent(int)
	 * @see #extent(int, int)
	 */
	public C yExtent(int extent) {
		this.yExtent = requirePositive(extent, EXTENT);
		return this.getSelf();
	}

	/**
	 * Sets {@link #xExtent} and {@link #yExtent} to the passed values.
	 *
	 * <p> The default values are {@value #DEFAULT_X_EXTENT} and {@value #DEFAULT_Y_EXTENT}, respectively.
	 *
	 * <p> {@link #xExtent} and {@link #yExtent} control the number of grid cells the constrained component occupies.
	 *
	 * @throws IllegalArgumentException if the passed {@code x} and {@code y} are not both positive
	 *
	 * @see #xExtent(int)
	 * @see #yExtent(int)
	 */
	public C extent(int x, int y) {
		return this.xExtent(x).yExtent(y);
	}

	/**
	 * Sets {@link #fillX} to the passed value.
	 *
	 * <p> The default value is {@value #DEFAULT_FILL_X}.
	 *
	 * @see #fillX()
	 * @see #fillOnlyX()
	 * @see #fillY(boolean)
	 * @see #fill(boolean, boolean)
	 */
	public C fillX(boolean fill) {
		this.fillX = fill;
		return this.getSelf();
	}

	/**
	 * Sets {@link #fillX} to {@code true}.
	 *
	 * <p> The default value is {@value #DEFAULT_FILL_X}.
	 *
	 * @see #fillX(boolean)
	 * @see #fillOnlyX()
	 * @see #fillY()
	 * @see #fill(boolean, boolean)
	 */
	public C fillX() {
		return this.fillX(true);
	}

	/**
	 * Sets {@link #fillX} to {@code true} and {@link #fillY} to {@code false}.
	 *
	 * <p> The default values are {@value #DEFAULT_FILL_X} and {@value #DEFAULT_FILL_Y}, respectively.
	 *
	 * @see #fillX()
	 * @see #fillX(boolean)
	 * @see #fillOnlyY()
	 * @see #fillBoth()
	 * @see #fillNone()
	 * @see #fill(boolean, boolean)
	 */
	public C fillOnlyX() {
		return this.fillX().fillY(false);
	}

	/**
	 * Sets {@link #fillY} to the passed value.
	 *
	 * <p> The default value is {@value #DEFAULT_FILL_Y}.
	 *
	 * @see #fillY()
	 * @see #fillOnlyY()
	 * @see #fillX(boolean)
	 * @see #fill(boolean, boolean)
	 */
	public C fillY(boolean fill) {
		this.fillY = fill;
		return this.getSelf();
	}

	/**
	 * Sets {@link #fillY} to {@code true}.
	 *
	 * <p> The default value is {@value #DEFAULT_FILL_Y}.
	 *
	 * @see #fillY(boolean)
	 * @see #fillOnlyY()
	 * @see #fillX()
	 * @see #fill(boolean, boolean)
	 */
	public C fillY() {
		return this.fillY(true);
	}

	/**
	 * Sets {@link #fillY} to {@code true} and {@link #fillX} to {@code false}.
	 *
	 * <p> The default values are {@value #DEFAULT_FILL_Y} and {@value #DEFAULT_FILL_X}, respectively.
	 *
	 * @see #fillY()
	 * @see #fillY(boolean)
	 * @see #fillOnlyX()
	 * @see #fillBoth()
	 * @see #fillNone()
	 * @see #fill(boolean, boolean)
	 */
	public C fillOnlyY() {
		return this.fillY().fillX(false);
	}

	/**
	 * Sets {@link #fillX} and {@link #fillY} to the passed values.
	 *
	 * <p> The default values are {@value #DEFAULT_FILL_X} and {@value #DEFAULT_FILL_Y}, respectively.
	 *
	 * @see #fillX()
	 * @see #fillX(boolean)
	 * @see #fillOnlyX()
	 * @see #fillY()
	 * @see #fillY(boolean)
	 * @see #fillOnlyY()
	 * @see #fillBoth()
	 * @see #fillNone()
	 */
	public C fill(boolean x, boolean y) {
		return this.fillX(x).fillY(y);
	}

	/**
	 * Sets {@link #fillX} and {@link #fillY} to {@code true}.
	 *
	 * <p> The default values are {@value #DEFAULT_FILL_X} and {@value #DEFAULT_FILL_Y}, respectively.
	 *
	 * @see #fillNone()
	 * @see #fillOnlyX()
	 * @see #fillOnlyY()
	 * @see #fill(boolean, boolean)
	 */
	public C fillBoth() {
		return this.fillX().fillY();
	}

	/**
	 * Sets {@link #fillX} and {@link #fillY} to {@code false}.
	 *
	 * <p> The default values are {@value #DEFAULT_FILL_X} and {@value #DEFAULT_FILL_Y}, respectively.
	 *
	 * @see #fillBoth()
	 * @see #fillOnlyX()
	 * @see #fillOnlyY()
	 * @see #fill(boolean, boolean)
	 */
	public C fillNone() {
		return this.fill(false, false);
	}

	/**
	 * Sets {@link #xAlignment} to the passed {@code alignment}.
	 *
	 * <p> The default value is {@link #DEFAULT_X_ALIGNMENT}.
	 *
	 * <p> Also see the methods {@linkplain #alignTopLeft() below} for setting each combination of alignments.
	 *
	 * @see #alignLeft()
	 * @see #alignCenterX()
	 * @see #alignRight()
	 * @see #align(Alignment, Alignment)
	 */
	public C alignX(Alignment alignment) {
		this.xAlignment = requireNonNull(alignment, ALIGNMENT);
		return this.getSelf();
	}

	/**
	 * Sets {@link #xAlignment} to {@link Alignment#BEGIN BEGIN}.
	 *
	 * <p> The default value is {@link #DEFAULT_X_ALIGNMENT}.
	 *
	 * <p> Also see the methods {@linkplain #alignTopLeft() below} for setting each combination of alignments.
	 *
	 * @see #alignCenterX()
	 * @see #alignRight()
	 * @see #alignX(Alignment)
	 * @see #align(Alignment, Alignment)
	 */
	public C alignLeft() {
		return this.alignX(Alignment.BEGIN);
	}

	/**
	 * Sets {@link #xAlignment} to {@link Alignment#CENTER CENTER}.
	 *
	 * <p> The default value is {@link #DEFAULT_X_ALIGNMENT}.
	 *
	 * <p> Also see the methods {@linkplain #alignTopLeft() below} for setting each combination of alignments.
	 *
	 * @see #alignLeft()
	 * @see #alignRight()
	 * @see #alignX(Alignment)
	 * @see #align(Alignment, Alignment)
	 */
	public C alignCenterX() {
		return this.alignX(Alignment.CENTER);
	}

	/**
	 * Sets {@link #xAlignment} to {@link Alignment#END END}.
	 *
	 * <p> The default value is {@link #DEFAULT_X_ALIGNMENT}.
	 *
	 * <p> Also see the methods {@linkplain #alignTopLeft() below} for setting each combination of alignments.
	 *
	 * @see #alignLeft()
	 * @see #alignCenterX()
	 * @see #alignX(Alignment)
	 * @see #align(Alignment, Alignment)
	 */
	public C alignRight() {
		return this.alignX(Alignment.END);
	}

	/**
	 * Sets {@link #yAlignment} to the passed {@code alignment}.
	 *
	 * <p> The default value is {@link #DEFAULT_Y_ALIGNMENT}.
	 *
	 * <p> Also see the methods {@linkplain #alignTopLeft() below} for setting each combination of alignments.
	 *
	 * @see #alignTop()
	 * @see #alignCenterY()
	 * @see #alignBottom()
	 * @see #align(Alignment, Alignment)
	 */
	public C alignY(Alignment alignment) {
		this.yAlignment = requireNonNull(alignment, ALIGNMENT);
		return this.getSelf();
	}

	/**
	 * Sets {@link #yAlignment} to {@link Alignment#BEGIN BEGIN}.
	 *
	 * <p> The default value is {@link #DEFAULT_Y_ALIGNMENT}.
	 *
	 * <p> Also see the methods {@linkplain #alignTopLeft() below} for setting each combination of alignments.
	 *
	 * @see #alignCenterY()
	 * @see #alignBottom()
	 * @see #alignY(Alignment)
	 * @see #align(Alignment, Alignment)
	 */
	public C alignTop() {
		return this.alignY(Alignment.BEGIN);
	}

	/**
	 * Sets {@link #yAlignment} to {@link Alignment#CENTER CENTER}.
	 *
	 * <p> The default value is {@link #DEFAULT_Y_ALIGNMENT}.
	 *
	 * <p> Also see the methods {@linkplain #alignTopLeft() below} for setting each combination of alignments.
	 *
	 * @see #alignTop()
	 * @see #alignBottom()
	 * @see #alignY(Alignment)
	 * @see #align(Alignment, Alignment)
	 */
	public C alignCenterY() {
		return this.alignY(Alignment.CENTER);
	}

	/**
	 * Sets {@link #yAlignment} to {@link Alignment#END END}.
	 *
	 * <p> The default value is {@link #DEFAULT_Y_ALIGNMENT}.
	 *
	 * <p> Also see the methods {@linkplain #alignTopLeft() below} for setting each combination of alignments.
	 *
	 * @see #alignTop()
	 * @see #alignCenterY()
	 * @see #alignY(Alignment)
	 * @see #align(Alignment, Alignment)
	 */
	public C alignBottom() {
		return this.alignY(Alignment.END);
	}

	/**
	 * Sets the {@link #xAlignment} and {@link #yAlignment} to the passed values.
	 *
	 * <p> The default values are {@link #DEFAULT_X_ALIGNMENT} and {@link #DEFAULT_Y_ALIGNMENT}.
	 *
	 * <p> Also see the methods {@linkplain #alignTopLeft() below} for setting each combination of alignments.
	 *
	 * @see #alignX(Alignment)
	 * @see #alignY(Alignment)
	 */
	public C align(Alignment x, Alignment y) {
		return this.alignX(x).alignY(y);
	}

	/**
	 * Sets {@link #yAlignment} and {@link #xAlignment} to {@link Alignment#BEGIN BEGIN}.
	 *
	 * <p> The default values are {@link #DEFAULT_Y_ALIGNMENT} and {@link #DEFAULT_X_ALIGNMENT}.
	 *
	 * @see #align(Alignment, Alignment)
	 */
	public C alignTopLeft() {
		return this.alignTop().alignLeft();
	}

	/**
	 * Sets {@link #yAlignment} to {@link Alignment#BEGIN BEGIN} and {@link #xAlignment} to
	 * {@link Alignment#CENTER CENTER}.
	 *
	 * <p> The default values are {@link #DEFAULT_Y_ALIGNMENT} and {@link #DEFAULT_X_ALIGNMENT}.
	 *
	 * @see #alignTop()
	 * @see #alignCenterX()
	 * @see #align(Alignment, Alignment)
	 */
	public C alignTopCenter() {
		return this.alignTop().alignCenterX();
	}

	/**
	 * Sets {@link #yAlignment} to {@link Alignment#BEGIN BEGIN} and {@link #xAlignment} to {@link Alignment#END END}.
	 *
	 * <p> The default values are {@link #DEFAULT_Y_ALIGNMENT} and {@link #DEFAULT_X_ALIGNMENT}.
	 *
	 * @see #alignTop()
	 * @see #alignRight()
	 * @see #align(Alignment, Alignment)
	 */
	public C alignTopRight() {
		return this.alignTop().alignRight();
	}

	/**
	 * Sets {@link #yAlignment} to {@link Alignment#CENTER CENTER} and {@link #xAlignment} to
	 * {@link Alignment#BEGIN BEGIN}.
	 *
	 * <p> The default values are {@link #DEFAULT_Y_ALIGNMENT} and {@link #DEFAULT_X_ALIGNMENT}.
	 *
	 * @see #alignCenterY()
	 * @see #alignLeft()
	 * @see #align(Alignment, Alignment)
	 */
	public C alignCenterLeft() {
		return this.alignCenterY().alignLeft();
	}

	/**
	 * Sets {@link #yAlignment} and {@link #xAlignment} to {@link Alignment#CENTER CENTER}.
	 *
	 * <p> The default values are {@link #DEFAULT_Y_ALIGNMENT} and {@link #DEFAULT_X_ALIGNMENT}.
	 *
	 * @see #alignCenterY()
	 * @see #alignCenterX()
	 * @see #align(Alignment, Alignment)
	 */
	public C alignCenter() {
		return this.alignCenterY().alignCenterX();
	}

	/**
	 * Sets {@link #yAlignment} to {@link Alignment#CENTER CENTER} and {@link #xAlignment} to {@link Alignment#END END}.
	 *
	 * <p> The default values are {@link #DEFAULT_Y_ALIGNMENT} and {@link #DEFAULT_X_ALIGNMENT}.
	 *
	 * @see #alignCenterY()
	 * @see #alignRight()
	 * @see #align(Alignment, Alignment)
	 */
	public C alignCenterRight() {
		return this.alignCenterY().alignRight();
	}

	/**
	 * Sets {@link #yAlignment} to {@link Alignment#END END} and {@link #xAlignment} to {@link Alignment#BEGIN BEGIN}.
	 *
	 * <p> The default values are {@link #DEFAULT_Y_ALIGNMENT} and {@link #DEFAULT_X_ALIGNMENT}.
	 *
	 * @see #alignBottom()
	 * @see #alignLeft()
	 * @see #align(Alignment, Alignment)
	 */
	public C alignBottomLeft() {
		return this.alignBottom().alignLeft();
	}

	/**
	 * Sets {@link #yAlignment} to {@link Alignment#END END} and {@link #xAlignment} to {@link Alignment#CENTER CENTER}.
	 *
	 * <p> The default values are {@link #DEFAULT_Y_ALIGNMENT} and {@link #DEFAULT_X_ALIGNMENT}.
	 *
	 * @see #alignBottom()
	 * @see #alignCenterX()
	 * @see #align(Alignment, Alignment)
	 */
	public C alignBottomCenter() {
		return this.alignBottom().alignCenterX();
	}

	/**
	 * Sets {@link #yAlignment} and {@link #xAlignment} to {@link Alignment#END END}.
	 *
	 * <p> The default values are {@link #DEFAULT_Y_ALIGNMENT} and {@link #DEFAULT_X_ALIGNMENT}.
	 *
	 * @see #alignBottom()
	 * @see #alignRight()
	 * @see #align(Alignment, Alignment)
	 */
	public C alignBottomRight() {
		return this.alignBottom().alignRight();
	}

	/**
	 * Sets {@link #priority}.
	 *
	 * <p> The default value is {@value #DEFAULT_PRIORITY}.
	 *
	 * @see #defaultPriority()
	 * @see #addPriority(int)
	 * @see #incrementPriority()
	 * @see #decrementPriority()
	 */
	public C priority(int priority) {
		this.priority = priority;
		return this.getSelf();
	}

	/**
	 * Sets the {@link #priority} to its default value: {@value #DEFAULT_PRIORITY}.
	 *
	 * @see #priority(int)
	 */
	public C defaultPriority() {
		return this.priority(DEFAULT_PRIORITY);
	}

	/**
	 * Adds the passed {@code amount} to {@link #priority}.
	 *
	 * <p> The default value is {@value #DEFAULT_PRIORITY}.
	 *
	 * @see #incrementPriority()
	 * @see #decrementPriority()
	 * @see #priority(int)
	 */
	public C addPriority(int amount) {
		return this.priority(this.priority + amount);
	}

	/**
	 * Adds {@code 1} to {@link #priority}.
	 *
	 * <p> The default value is {@value #DEFAULT_PRIORITY}.
	 *
	 * @see #decrementPriority()
	 * @see #addPriority(int)
	 * @see #priority(int)
	 */
	public C incrementPriority() {
		return this.addPriority(1);
	}

	/**
	 * Subtracts {@code 1} from {@link #priority}.
	 *
	 * <p> The default value is {@value #DEFAULT_PRIORITY}.
	 *
	 * @see #incrementPriority()
	 * @see #addPriority(int)
	 * @see #priority(int)
	 */
	public C decrementPriority() {
		return this.addPriority(-1);
	}

	public abstract C copy();

	abstract C getSelf();

	public enum Alignment {
		BEGIN, CENTER, END
	}

	/**
	 * {@link FlexGridConstraints} with relative coordinates.<br>
	 * Components will be placed at the end of the bottom-most row at the time of
	 * {@linkplain Container#add(Component, Object) adding}.
	 *
	 * <p> Relative components never overlap components added <em>before</em> them, but {@link Absolute Absolute}
	 * components added after them may overlap.
	 *
	 * @see Absolute#toRelative() Absolute.toRelative()
	 * @see #toAbsolute()
	 * @see #toAbsolute(int, int)
	 */
	public static final class Relative extends FlexGridConstraints<FlexGridConstraints.Relative> {
		public static Relative of() {
			return new Relative(
				DEFAULT_X_EXTENT, DEFAULT_Y_EXTENT,
				DEFAULT_FILL_X, DEFAULT_FILL_Y,
				DEFAULT_X_ALIGNMENT, DEFAULT_Y_ALIGNMENT,
				DEFAULT_PRIORITY
			);
		}

		private Relative(
				int xExtent, int yExtent,
				boolean fillX, boolean fillY,
				Alignment xAlignment, Alignment yAlignment,
				int priority
		) {
			super(xExtent, yExtent, fillX, fillY, xAlignment, yAlignment, priority);
		}

		@Override
		public Relative copy() {
			return new Relative(
				this.xExtent, this.yExtent,
				this.fillX, this.fillY,
				this.xAlignment, this.yAlignment,
				this.priority
			);
		}

		/**
		 * Creates {@link Absolute Absolute} constraints at ({@value Absolute#DEFAULT_X}, {@value Absolute#DEFAULT_Y})
		 * with these constraints' values.
		 *
		 * @see #toAbsolute(int, int)
		 * @see Absolute#toRelative() Absolute.toRelative()
		 */
		public Absolute toAbsolute() {
			return new Absolute(
				Absolute.DEFAULT_X, Absolute.DEFAULT_Y,
				this.xExtent, this.yExtent,
				this.fillX, this.fillY,
				this.xAlignment, this.yAlignment,
				this.priority
			);
		}

		/**
		 * Creates {@link Absolute Absolute} constraints with these constraints' values and the passed
		 * {@code x} and {@code y} coordinates.
		 *
		 * @see #toAbsolute()
		 * @see Absolute#toRelative() Absolute.toRelative()
		 */
		public Absolute toAbsolute(int x, int y) {
			return this.toAbsolute().pos(x, y);
		}

		@Override
		Relative getSelf() {
			return this;
		}
	}

	/**
	 * {@link FlexGridConstraints} with absolute {@link #x} and {@link #y} coordinates.
	 *
	 * <p> Absolute components will overlap with any components occupying the same grid cells, whether they're at the
	 * same coordinates or they {@linkplain #extent(int, int) extend} into each other's cells.
	 */
	public static final class Absolute extends FlexGridConstraints<FlexGridConstraints.Absolute> {
		public static final int DEFAULT_X = 0;
		public static final int DEFAULT_Y = 0;

		public static Absolute of() {
			return new Absolute(
				DEFAULT_X, DEFAULT_Y,
				DEFAULT_X_EXTENT, DEFAULT_Y_EXTENT,
				DEFAULT_FILL_X, DEFAULT_FILL_Y,
				DEFAULT_X_ALIGNMENT, DEFAULT_Y_ALIGNMENT,
				DEFAULT_PRIORITY
			);
		}

		/**
		 * Defaults to {@value #DEFAULT_X}.
		 */
		int x;

		/**
		 * Defaults to {@value #DEFAULT_Y}.
		 */
		int y;

		private Absolute(
				int x, int y,
				int xExtent, int yExtent,
				boolean fillX, boolean fillY,
				Alignment xAlignment, Alignment yAlignment,
				int priority
		) {
			super(xExtent, yExtent, fillX, fillY, xAlignment, yAlignment, priority);

			this.x = x;
			this.y = y;
		}

		public int getX() {
			return this.x;
		}

		public int getY() {
			return this.y;
		}

		/**
		 * Sets {@link #x} to the passed value.
		 *
		 * <p> The default value is {@value #DEFAULT_X}.
		 *
		 * @see #advanceRows(int)
		 * @see #nextRow()
		 * @see #y(int)
		 * @see #pos(int, int)
		 */
		public Absolute x(int x) {
			this.x = x;
			return this;
		}

		/**
		 * Adds the passed {@code count} to {@link #x}.
		 *
		 * <p> The default value is {@value #DEFAULT_X}.
		 *
		 * @see #nextColumn()
		 * @see #y(int)
		 * @see #advanceRows(int)
		 */
		public Absolute advanceColumns(int count) {
			this.x += count;
			return this.getSelf();
		}

		/**
		 * Adds {@code 1} to {@link #x}.
		 *
		 * <p> The default value is {@value #DEFAULT_X}.
		 *
		 * @see #advanceColumns(int)
		 * @see #y(int)
		 * @see #nextRow()
		 */
		public Absolute nextColumn() {
			return this.advanceColumns(1);
		}

		/**
		 * Sets {@link #y} to the passed value.
		 *
		 * <p> The default value is {@value #DEFAULT_Y}.
		 *
		 * @see #advanceColumns(int)
		 * @see #nextColumn()
		 * @see #x(int)
		 * @see #pos(int, int)
		 */
		public Absolute y(int y) {
			this.y = y;
			return this;
		}

		/**
		 * Sets {@link #x} to {@code 0} and adds the passed {@code count} to {@link #y}.
		 *
		 * <p> The default values are {@value #DEFAULT_X} and {@value #DEFAULT_Y}, respectively.
		 *
		 * @see #nextRow()
		 * @see #x(int)
		 * @see #advanceColumns(int)
		 */
		public Absolute advanceRows(int count) {
			this.x = 0;
			this.y += count;
			return this.getSelf();
		}

		/**
		 * Sets {@link #x} to {@code 0} and adds {@code 1} to {@link #y}.
		 *
		 * <p> The default values are {@value #DEFAULT_X} and {@value #DEFAULT_Y}, respectively.
		 *
		 * @see #advanceRows(int)
		 * @see #x(int)
		 * @see #nextColumn()
		 */
		public Absolute nextRow() {
			return this.advanceRows(1);
		}

		/**
		 * Sets {@link #x} and {@link #y} to the passed values.
		 *
		 * <p> The default values are {@value #DEFAULT_X} and {@value #DEFAULT_Y}, respectively.
		 *
		 * @see #x(int)
		 * @see #y(int)
		 */
		public Absolute pos(int x, int y) {
			return this.x(x).y(y);
		}

		@Override
		public Absolute copy() {
			return new Absolute(
				this.x, this.y,
				this.xExtent, this.yExtent,
				this.fillX, this.fillY,
				this.xAlignment, this.yAlignment,
				this.priority
			);
		}

		/**
		 * Creates {@link Relative Relative} constraints with these constraints' values
		 * ({@link #x} and {@link #y} ar ignored).
		 *
		 * @see Relative#toAbsolute() Relative.toAbsolute()
		 * @see Relative#toAbsolute(int, int) Relative.toAbsolute(int, int)
		 */
		public Relative toRelative() {
			return new Relative(
				this.xExtent, this.yExtent,
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
