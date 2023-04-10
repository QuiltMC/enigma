package cuchaz.enigma.gui.docker.component;

import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.docker.Docker;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class DockerSelector extends JPanel {
	private static final List<DockerSelector> INSTANCES = new ArrayList<>();

	private JPanel hovered;

	private final JPanel bottomSelector;
	private final JPanel topSelector;
	private final Docker.Side side;

	public DockerSelector(Docker.Side side) {
		super(new BorderLayout());
		this.bottomSelector = new JPanel(new VerticalFlowLayout(5));
		this.topSelector = new JPanel(new VerticalFlowLayout(5));
		this.side = side;

		this.add(this.topSelector, BorderLayout.NORTH);
		this.add(this.bottomSelector, BorderLayout.SOUTH);

		INSTANCES.add(this);
	}

	public JPanel getTopSelector() {
		return this.topSelector;
	}

	public JPanel getBottomSelector() {
		return this.bottomSelector;
	}

	/**
	 * Adds all buttons that match this selector's side to it. This method should be called after all dockers have been registered.
	 */
	public void configure() {
		this.topSelector.removeAll();
		this.bottomSelector.removeAll();

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

	private Rectangle getScreenBoundsFor(JPanel selector) {
		Point location = selector.getLocationOnScreen();
		return new Rectangle(location.x, selector.equals(this.bottomSelector) ? this.getLocationOnScreen().y + this.getHeight() / 2 : location.y, selector.getWidth(), this.getHeight() / 2);
	}

	private JPanel getHoveredPanel(MouseEvent event) {
		if (Draggable.contains(this.getScreenBoundsFor(this.topSelector), event.getLocationOnScreen())) {
			return this.topSelector;
		} else if (Draggable.contains(this.getScreenBoundsFor(this.bottomSelector), event.getLocationOnScreen())) {
			return this.bottomSelector;
		} else {
			return null;
		}
	}

	private void receiveMouseEvent(MouseEvent event) {
		if (this.isDisplayable()) {
			if (event.getID() == MouseEvent.MOUSE_DRAGGED) {
				this.hovered = this.getHoveredPanel(event);
			} else if (event.getID() == MouseEvent.MOUSE_RELEASED) {
				this.hovered = null;
			}
		}
	}

	private boolean dropButton(DockerButton button, MouseEvent event) {
		JPanel hoveredPanel = this.getHoveredPanel(event);
		if (hoveredPanel != null) {
			hoveredPanel.add(button);
			button.setSide(this.side);
			UiConfig.setDockerButtonLocation(button.getDocker(), new Docker.Location(this.side, hoveredPanel.equals(this.bottomSelector) ? Docker.VerticalLocation.BOTTOM : Docker.VerticalLocation.TOP));
			return true;
		}

		return false;
	}

	@Override
	public void paint(Graphics graphics) {
		super.paint(graphics);

		if (this.hovered != null) {
			Rectangle paintedBounds = this.getScreenBoundsFor(this.hovered);

			Color color = UiConfig.getDockHighlightColor();
			graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
			graphics.fillRect(0, this.hovered.equals(this.bottomSelector) ? paintedBounds.height : 0, paintedBounds.width, paintedBounds.height);
			this.repaint();
		}
	}

	public static class Util {
		/**
		 * Calls {@link DockerSelector#receiveMouseEvent(MouseEvent)}} on all selectors.
		 * @param event the mouse event to pass to the docks
		 */
		public static void receiveMouseEvent(MouseEvent event) {
			for (DockerSelector selector : INSTANCES) {
				selector.receiveMouseEvent(event);
			}
		}

		/**
		 * Drops the button after it has been dragged.
		 * Checks all selectors to see if it's positioned over one, and if yes, snaps it into to that selector on the proper side.
		 * @param button the button to drop
		 * @param event an {@link MouseEvent} to use to check if the docker was held over a dock
		 * @return true if the button was snapped into a selector, false otherwise
		 */
		public static boolean dropButton(DockerButton button, MouseEvent event) {
			for (DockerSelector selector : INSTANCES) {
				if (selector.isDisplayable() && selector.dropButton(button, event)) {
					return true;
				}
			}

			return false;
		}
	}
}
