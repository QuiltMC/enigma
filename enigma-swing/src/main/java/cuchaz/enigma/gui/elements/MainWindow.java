package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.panels.right.RightPanel;
import cuchaz.enigma.gui.panels.right.RightAngleLayerUI;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;

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

	private final JPanel topRightPanelSelector;
	private final JPanel bottomRightPanelSelector;

	public MainWindow(String title) {
		JPanel rightPanelSelector = new JPanel();
		rightPanelSelector.setLayout(new BorderLayout());

		// create separate panels for top and bottom button groups
		// this is necessary because flow layout doesn't support using multiple alignments
		this.topRightPanelSelector = new JPanel();
		this.topRightPanelSelector.setLayout(new FlowLayout(FlowLayout.LEFT));
		this.bottomRightPanelSelector = new JPanel();
		this.bottomRightPanelSelector.setLayout(new FlowLayout(FlowLayout.RIGHT));

		// set up button groups
		rightPanelSelector.add(this.topRightPanelSelector, BorderLayout.WEST);
		rightPanelSelector.add(this.bottomRightPanelSelector, BorderLayout.EAST);
		JLayer<JPanel> layer = new JLayer<>(rightPanelSelector);
		layer.setUI(new RightAngleLayerUI(RightAngleLayerUI.Rotation.CLOCKWISE));

		this.frame = new JFrame(title);
		this.frame.setJMenuBar(this.menuBar);

		Container contentPane = this.frame.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(this.workArea, BorderLayout.CENTER);
		contentPane.add(this.statusBar.getUi(), BorderLayout.SOUTH);
		contentPane.add(layer, BorderLayout.EAST);
	}

	public void updateRightPanelSelector() {
		this.topRightPanelSelector.removeAll();
		this.bottomRightPanelSelector.removeAll();

		// create buttons from right panel options
		for (RightPanel panel : RightPanel.getRightPanels().values()) {
			JToggleButton button = panel.getButton();

			if (panel.getButtonPosition().equals(RightPanel.ButtonPosition.TOP)) {
				this.topRightPanelSelector.add(button);
			} else {
				this.bottomRightPanelSelector.add(button);
			}
		}
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
