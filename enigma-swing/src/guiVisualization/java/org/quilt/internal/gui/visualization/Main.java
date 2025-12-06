package org.quilt.internal.gui.visualization;

import org.quilt.internal.gui.visualization.flex_grid.FlexGridAlignAndFillVisualizer;
import org.quilt.internal.gui.visualization.flex_grid.FlexGridAlignmentVisualizer;
import org.quilt.internal.gui.visualization.flex_grid.FlexGridCheckersVisualizer;
import org.quilt.internal.gui.visualization.flex_grid.FlexGridColumnVisualiser;
import org.quilt.internal.gui.visualization.flex_grid.FlexGridDefaultRowVisualiser;
import org.quilt.internal.gui.visualization.flex_grid.FlexGridFillVisualizer;
import org.quilt.internal.gui.visualization.flex_grid.FlexGridExtentOverlapVisualiser;
import org.quilt.internal.gui.visualization.flex_grid.FlexGridPriorityFillVisualizer;
import org.quilt.internal.gui.visualization.flex_grid.FlexGridPriorityVisualizer;
import org.quilt.internal.gui.visualization.flex_grid.FlexGridQuiltVisualiser;
import org.quilt.internal.gui.visualization.flex_grid.FlexGridSparseVisualizer;
import org.quilt.internal.gui.visualization.flex_grid.FlexGridGreaterVisualizer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The main entrypoint for {@link Visualizer}s.
 *
 * <p> Opens a window full of buttons that open visualizer windows.
 */
public final class Main {
	private Main() {
		throw new UnsupportedOperationException();
	}

	private static final JFrame WINDOW = new JFrame("Gui Visualization");

	// bootstrap
	static {
		registerVisualizer(new FlexGridDefaultRowVisualiser());
		registerVisualizer(new FlexGridColumnVisualiser());
		registerVisualizer(new FlexGridQuiltVisualiser());
		registerVisualizer(new FlexGridExtentOverlapVisualiser());
		registerVisualizer(new FlexGridFillVisualizer());
		registerVisualizer(new FlexGridAlignmentVisualizer());
		registerVisualizer(new FlexGridAlignAndFillVisualizer());
		registerVisualizer(new FlexGridPriorityVisualizer());
		registerVisualizer(new FlexGridPriorityFillVisualizer());
		registerVisualizer(new FlexGridSparseVisualizer());
		registerVisualizer(new FlexGridGreaterVisualizer());
		registerVisualizer(new FlexGridCheckersVisualizer());
	}

	private static void position(Window window) {
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final int x = (screenSize.width - window.getWidth()) / 2;
		final int y = (screenSize.height - window.getHeight()) / 2;

		window.setLocation(x, y);
	}

	public static void main(String[] args) {
		WINDOW.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		WINDOW.setLayout(new FlowLayout());

		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final int width = screenSize.width * 2 / 3;
		final int height = screenSize.height * 2 / 3;
		final int x = (screenSize.width - width) / 2;
		final int y = (screenSize.height - height) / 2;

		WINDOW.setBounds(x, y, width, height);
		WINDOW.setVisible(true);
	}

	private static void registerVisualizer(Visualizer visualizer) {
		final JButton button = new JButton(visualizer.getTitle());
		final AtomicReference<JFrame> currentWindow = new AtomicReference<>();

		button.addActionListener(e -> {
			final JFrame window = currentWindow.updateAndGet(old -> {
				if (old != null) {
					old.dispose();
				}

				return new JFrame(visualizer.getTitle());
			});

			visualizer.visualize(window);

			window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			window.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					final JFrame window = currentWindow.get();
					if (window == null || !window.isDisplayable()) {
						WINDOW.requestFocus();
					}
				}
			});

			position(window);
			window.setVisible(true);
		});

		WINDOW.add(button);
	}
}
