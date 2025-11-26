package org.quilt.internal.gui.visualization;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.Toolkit;

public final class Main {
	private static final JFrame WINDOW = new JFrame();

	public static void main(String[] args) {
		WINDOW.setTitle("Gui Visualization");
		WINDOW.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final int width = screenSize.width * 2 / 3;
		final int height = screenSize.height * 2 / 3;
		final int x = (screenSize.width - width) / 2;
		final int y = (screenSize.height - height) / 2;

		WINDOW.setBounds(x, y, width, height);
		WINDOW.setVisible(true);
	}

	private Main() {
		throw new UnsupportedOperationException();
	}
}
