package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.panels.right.RightPanel;
import cuchaz.enigma.gui.panels.right.RightRotatedLayerUI;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class MainWindow {
	private final JFrame frame;
	private final JPanel workArea = new JPanel();

	private final JMenuBar menuBar = new JMenuBar();
	private final StatusBar statusBar = new StatusBar();

	public MainWindow(String title) {
		if (RightPanel.getRightPanels().isEmpty()) {
			throw new IllegalStateException("no right panels registered! right panels should be registered before creating the main window.");
		}

		JPanel rightPanelSelector = new JPanel();
		rightPanelSelector.setLayout(new BorderLayout());

		// create separate panels for top and bottom button groups
		// this is necessary because flow layout doesn't support using multiple alignments
		JPanel topButtons = new JPanel();
		topButtons.setLayout(new FlowLayout(FlowLayout.LEFT));
		JPanel bottomButtons = new JPanel();
		bottomButtons.setLayout(new FlowLayout(FlowLayout.RIGHT));

		// create buttons from right panel options
		for (Map.Entry<String, RightPanel> entry : RightPanel.getRightPanels().entrySet()) {
			RightPanel panel = entry.getValue();
			JToggleButton button = panel.getButton();

			if (panel.getButtonPosition().equals(RightPanel.ButtonPosition.TOP)) {
				topButtons.add(button);
			} else {
				bottomButtons.add(button);
			}
		}

		// set up button groups
		rightPanelSelector.add(topButtons, BorderLayout.WEST);
		rightPanelSelector.add(bottomButtons, BorderLayout.EAST);
		JLayer<JPanel> layer = new JLayer<>(rightPanelSelector);
		layer.setUI(new RightRotatedLayerUI());

		this.frame = new JFrame(title);
		this.frame.setJMenuBar(this.menuBar);

		Container contentPane = this.frame.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(this.workArea, BorderLayout.CENTER);
		contentPane.add(this.statusBar.getUi(), BorderLayout.SOUTH);
		contentPane.add(layer, BorderLayout.EAST);
	}

	public void setVisible(boolean visible) {
		this.frame.setVisible(visible);
	}

	public JMenuBar getMenuBar() {
		return this.menuBar;
	}

	public StatusBar getStatusBar() {
		return this.statusBar;
	}

	public Container getWorkArea() {
		return this.workArea;
	}

	public JFrame getFrame() {
		return this.frame;
	}

	public void setTitle(String title) {
		this.frame.setTitle(title);
	}
}
