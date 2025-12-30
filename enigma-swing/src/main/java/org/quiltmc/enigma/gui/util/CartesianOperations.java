package org.quiltmc.enigma.gui.util;

import java.awt.Component;
import java.awt.Insets;

/**
 * Sets of operations for the {@link X} and {@link Y} axes of a cartesian plane.
 */
public interface CartesianOperations<O extends CartesianOperations<O>> {
	int chooseCoord(int x, int y);

	int getLeadingInset(Insets insets);
	int getTrailingInset(Insets insets);
	int getSpan(Component component);

	O opposite();

	interface X<O extends CartesianOperations<O>> extends CartesianOperations<O> {
		@Override
		default int chooseCoord(int x, int y) {
			return x;
		}

		@Override
		default int getLeadingInset(Insets insets) {
			return insets.left;
		}

		@Override
		default int getTrailingInset(Insets insets) {
			return insets.right;
		}

		@Override
		default int getSpan(Component component) {
			return component.getWidth();
		}
	}

	interface Y<O extends CartesianOperations<O>> extends CartesianOperations<O> {
		@Override
		default int chooseCoord(int x, int y) {
			return y;
		}

		@Override
		default int getLeadingInset(Insets insets) {
			return insets.top;
		}

		@Override
		default int getTrailingInset(Insets insets) {
			return insets.bottom;
		}

		@Override
		default int getSpan(Component component) {
			return component.getHeight();
		}
	}
}
