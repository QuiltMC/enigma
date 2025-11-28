package org.quilt.internal.gui.visualization;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Supplier;

public final class Main {
	private Main() {
		throw new UnsupportedOperationException();
	}

	private static final JFrame WINDOW = new JFrame("Gui Visualization");

	private static void position(Window window) {
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final int x = (screenSize.width - window.getWidth()) / 2;
		final int y = (screenSize.height - window.getHeight()) / 2;

		window.setLocation(x, y);
	}

	public static void main(String[] args) {
		WINDOW.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		WINDOW.setLayout(new FlowLayout());

		addVisualizer(FlexGridRelativeRowVisualiser.TITLE, FlexGridRelativeRowVisualiser::new);

		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final int width = screenSize.width * 2 / 3;
		final int height = screenSize.height * 2 / 3;
		final int x = (screenSize.width - width) / 2;
		final int y = (screenSize.height - height) / 2;

		WINDOW.setBounds(x, y, width, height);
		WINDOW.setVisible(true);
	}

	private static void addVisualizer(String title, Supplier<JFrame> factory) {
		final JButton button = new JButton(title);
		button.addActionListener(e -> {
			final JFrame window = factory.get();

			window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			window.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					WINDOW.requestFocus();
				}
			});

			position(window);
			window.setVisible(true);
		});

		WINDOW.add(button);
	}
}
