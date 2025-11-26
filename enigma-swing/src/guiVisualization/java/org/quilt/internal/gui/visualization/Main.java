package org.quilt.internal.gui.visualization;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.Toolkit;

public final class Main {
	private static final double WINDOW_TO_SCREEN_RATIO = 2d/3d;

	private static final JFrame WINDOW = new JFrame();

	static {
		WINDOW.setTitle("Gui Visualization");
		WINDOW.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

	public static void main(String[] args) {
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final int width = (int) (screenSize.width * WINDOW_TO_SCREEN_RATIO);
		final int height = (int) (screenSize.height * WINDOW_TO_SCREEN_RATIO);
		final int x = (screenSize.width - width) / 2;
		final int y = (screenSize.height - height) / 2;

		WINDOW.setBounds(x, y, width, height);
		WINDOW.setVisible(true);
	}

	private Main() {
		throw new UnsupportedOperationException();
	}
}
