package cuchaz.enigma.gui.docker.component;

import cuchaz.enigma.gui.docker.Docker;

import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class DockerSelector {
	private final JLayer<JPanel> panel;
	private final JPanel bottomSelector;
	private final JPanel topSelector;
	private final Docker.Side side;

	public DockerSelector(Docker.Side side) {
		JPanel mainPanel = new JPanel(new BorderLayout());
		this.bottomSelector = new JPanel(new FlowLayout());
		this.topSelector = new JPanel(new FlowLayout());
		this.side = side;

		mainPanel.add(this.topSelector, side == Docker.Side.RIGHT ? BorderLayout.WEST : BorderLayout.EAST);
		mainPanel.add(this.bottomSelector, side == Docker.Side.RIGHT ? BorderLayout.EAST : BorderLayout.WEST);

		this.panel = new JLayer<>(mainPanel);
		this.panel.setUI(new RightAngleLayerUI(side == Docker.Side.RIGHT ? RightAngleLayerUI.Rotation.CLOCKWISE : RightAngleLayerUI.Rotation.COUNTERCLOCKWISE));
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
			if (docker.getButtonPosition().side() == this.side) {
				JToggleButton button = docker.getButton();

				if (docker.getButtonPosition().verticalLocation() == Docker.VerticalLocation.TOP) {
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
