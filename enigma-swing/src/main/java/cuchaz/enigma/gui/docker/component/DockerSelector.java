package cuchaz.enigma.gui.docker.component;

import cuchaz.enigma.gui.docker.Docker;

import javax.swing.JLayer;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class DockerSelector {
	private final JLayer<JPanel> panel;
	private final JPanel bottomSelector;
	private final JPanel topSelector;
	private final Docker.Side side;

	public DockerSelector(Docker.Side side) {
		JPanel mainPanel = new JPanel(new BorderLayout());
		this.bottomSelector = new JPanel(new VerticalFlowLayout(5));
		this.topSelector = new JPanel(new VerticalFlowLayout(5));
		this.side = side;

		mainPanel.add(this.topSelector, BorderLayout.NORTH);
		mainPanel.add(this.bottomSelector, BorderLayout.SOUTH);

		this.panel = new JLayer<>(mainPanel);
	}

	/**
	 * Adds all buttons that match this selector's side to it. This method should be called after all dockers have been registered.
	 */
	public void configure() {
		this.topSelector.removeAll();
		this.bottomSelector.removeAll();

		// create buttons docker options
		for (Docker docker : Docker.getDockers().values()) {
			// only use buttons that match this selector's side
			if (docker.getButtonLocation().side() == this.side) {
				DockerButton button = docker.getButton();
				button.setConstraints("");

				if (docker.getButtonLocation().verticalLocation() == Docker.VerticalLocation.TOP) {
					this.topSelector.add(button);
				} else {
					this.bottomSelector.add(button);
				}
			}
		}
	}

	public JLayer<JPanel> getPanel() {
		return this.panel;
	}
}
