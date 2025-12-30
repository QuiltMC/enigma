package org.quilt.internal.gui.visualization.util;

import org.quiltmc.enigma.gui.util.layout.flex_grid.FlexGridLayout;
import org.quiltmc.enigma.gui.util.layout.flex_grid.constraints.FlexGridConstraints;

import javax.swing.JFrame;
import java.awt.Color;
import java.util.function.UnaryOperator;

public final class VisualUtils {
	private VisualUtils() {
		throw new UnsupportedOperationException();
	}

	// Color.ORANGE is not very orange
	public static final Color ORANGE = new Color(255, 128, 0);
	public static final Color PURPLE = new Color(128, 0, 255);

	// Quilt patch colors
	public static final Color PATCH_PURPLE = new Color(151, 34, 255);
	public static final Color PATCH_MAGENTA = new Color(220, 41, 221);
	public static final Color PATCH_CYAN = new Color(39, 162, 253);
	public static final Color PATCH_BLUE = new Color(51, 68, 255);

	/**
	 * Visualizes Quilt's logo, giving each patch a name indicating its coordinates.
	 *
	 * @see #visualizeFlexGridQuilt(JFrame,
	 * String, UnaryOperator, String, UnaryOperator, String, UnaryOperator,
	 * String, UnaryOperator, String, UnaryOperator, String, UnaryOperator,
	 * String, UnaryOperator, String, UnaryOperator, String, UnaryOperator)
	 */
	public static void visualizeFlexGridQuilt(
			JFrame window,
			UnaryOperator<FlexGridConstraints.Absolute> constrainer1,
			UnaryOperator<FlexGridConstraints.Absolute> constrainer2,
			UnaryOperator<FlexGridConstraints.Absolute> constrainer3,

			UnaryOperator<FlexGridConstraints.Absolute> constrainer4,
			UnaryOperator<FlexGridConstraints.Absolute> constrainer5,
			UnaryOperator<FlexGridConstraints.Absolute> constrainer6,

			UnaryOperator<FlexGridConstraints.Absolute> constrainer7,
			UnaryOperator<FlexGridConstraints.Absolute> constrainer8,
			UnaryOperator<FlexGridConstraints.Absolute> constrainer9
	) {
		visualizeFlexGridQuilt(
				window,
				"(0, 0)", constrainer1, "(1, 0)", constrainer2, "(2, 0)", constrainer3,
				"(0, 1)", constrainer4, "(1, 1)", constrainer5, "(2, 1)", constrainer6,
				"(0, 2)", constrainer7, "(1, 2)", constrainer8, "(2, 2)", constrainer9
		);
	}

	/**
	 * Gives the passed {@code window} a {@link FlexGridLayout} and forms Quilt's logo out of a 3 x 3 grid of
	 * {@link VisualBox} patches.
	 *
	 * <p> The patches are given the passed names and their constraints are adjusted using the passed constrainers.<br>
	 * The same {@link FlexGridConstraints} instance is passed to each constrainer, and its x and y coordinates are
	 * updated for each patch before passing it.
	 *
	 * @see #visualizeFlexGridQuilt(JFrame,
	 * UnaryOperator, UnaryOperator, UnaryOperator, UnaryOperator, UnaryOperator,
	 * UnaryOperator, UnaryOperator, UnaryOperator, UnaryOperator)
	 */
	public static void visualizeFlexGridQuilt(
			JFrame window,
			String name1, UnaryOperator<FlexGridConstraints.Absolute> constrainer1,
			String name2, UnaryOperator<FlexGridConstraints.Absolute> constrainer2,
			String name3, UnaryOperator<FlexGridConstraints.Absolute> constrainer3,

			String name4, UnaryOperator<FlexGridConstraints.Absolute> constrainer4,
			String name5, UnaryOperator<FlexGridConstraints.Absolute> constrainer5,
			String name6, UnaryOperator<FlexGridConstraints.Absolute> constrainer6,

			String name7, UnaryOperator<FlexGridConstraints.Absolute> constrainer7,
			String name8, UnaryOperator<FlexGridConstraints.Absolute> constrainer8,
			String name9, UnaryOperator<FlexGridConstraints.Absolute> constrainer9
	) {
		window.setLayout(new FlexGridLayout());

		final FlexGridConstraints.Absolute constraints = FlexGridConstraints.createAbsolute();
		window.add(VisualBox.purplePatchOf(name1), constrainer1.apply(constraints));
		window.add(VisualBox.magentaPatchOf(name2), constrainer2.apply(constraints.nextColumn()));
		window.add(VisualBox.cyanPatchOf(name3), constrainer3.apply(constraints.nextColumn()));

		window.add(VisualBox.magentaPatchOf(name4), constrainer4.apply(constraints.nextRow()));
		window.add(VisualBox.cyanPatchOf(name5), constrainer5.apply(constraints.nextColumn()));
		window.add(VisualBox.bluePatchOf(name6), constrainer6.apply(constraints.nextColumn()));

		window.add(VisualBox.purplePatchOf(name7), constrainer7.apply(constraints.nextRow()));
		window.add(VisualBox.bluePatchOf(name8), constrainer8.apply(constraints.nextColumn()));
		window.add(VisualBox.purplePatchOf(name9), constrainer9.apply(constraints.nextColumn()));
	}
}
