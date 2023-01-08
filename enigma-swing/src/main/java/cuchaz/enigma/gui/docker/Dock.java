package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.config.UiConfig;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dock extends JPanel {
	private static final List<Dock> docks = new ArrayList<>();

	private boolean hovered = false;
	private final Docker.Location location;
	private Docker hostedDocker;

	public Dock(Docker.Location location) {
		super(new BorderLayout());
		this.hostedDocker = null;
		this.location = location;

		docks.add(this);
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		// we can rely on paint to always be called when the label is being dragged over the docker
		if (this.hovered) {
			Color color = new Color(0, 0, 255, 84);
			g.setColor(color);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			this.repaint();
		}
	}

	public void receiveMouseEvent(MouseEvent e) {
		if (this.isDisplayable()) {
			if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
				if (!hovered && containsMouse(e)) {
					this.repaint();
					this.hovered = true;
				} else if (!containsMouse(e)) {
					this.hovered = false;
				}
			} else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
				this.hovered = false;
				this.repaint();
			}
		}
	}

	public boolean containsMouse(MouseEvent e) {
		Rectangle screenBounds = new Rectangle(this.getLocationOnScreen().x, this.getLocationOnScreen().y, this.getWidth(), this.getHeight());
		return contains(screenBounds, e.getLocationOnScreen());
	}

	public void setHostedDocker(Docker docker) {
		// remove old docker
		if (this.hostedDocker != null) {
			this.remove(this.hostedDocker);
		}

		this.hostedDocker = docker;
		if (docker != null) {
			this.setUpDocker();
		}

		// save to config
		UiConfig.setDocker(this, this.hostedDocker);
	}

	public void removeHostedDocker() {
		this.setHostedDocker(null);
	}

	public Docker.Location getDockerLocation() {
		return this.location;
	}

	public void setUpDocker() {
		if (this.hostedDocker == null) {
			throw new IllegalStateException("cannot refresh a dock that has no docker!");
		} else {
			// add new docker and revalidate to paint properly
			this.add(this.hostedDocker);
			this.hostedDocker.dock(this.location);
			this.revalidate();
		}
	}

	private boolean contains(Rectangle rectangle, Point point) {
		return (point.x >= rectangle.x && point.x <= rectangle.x + rectangle.width)
				&& (point.y >= rectangle.y && point.y <= rectangle.y + rectangle.height);
	}

	public static class Util {
		/**
		 * Calls {@link Dock#receiveMouseEvent(MouseEvent)}} on all docks.
		 * @param event the mouse event to pass to the docks
		 */
		public static void receiveMouseEvent(MouseEvent event) {
			for (Dock dock : docks) {
				dock.receiveMouseEvent(event);
			}
		}

		/**
		 * Drops the docker after it has been dragged.
		 * Checks all docks to see if it's positioned over one, and if yes, snaps it into to that dock.
		 * @param docker the docker to open
		 * @param event an {@link MouseEvent} to use to check if the docker was held over a dock
		 */
		public static void dropDocker(Docker docker, MouseEvent event) {
			for (Dock dock : docks) {
				if (dock.containsMouse(event)) {
					dock.setHostedDocker(docker);
					break;
				}
			}
		}

		/**
		 * Gets all dockers that are being hosted on-screen, and their respective docks.
		 * @return the docks in a lovely and convenient map format
		 */
		public static Map<Dock, Docker> getActiveDockers() {
			Map<Dock, Docker> dockers = new HashMap<>();
			for (Dock dock : docks) {
				if (dock.hostedDocker != null) {
					dockers.put(dock, dock.hostedDocker);
				}
			}

			return dockers;
		}

		public static Dock getForLocation(Docker.Location location) {
			for (Dock dock : docks) {
				if (location == Docker.Location.LEFT_FULL || location == Docker.Location.RIGHT_FULL) {
					// todo !
				}

				if (dock.location == location) {
					return dock;
				}
			}

			throw new IllegalStateException("no dock for location " + location + "! this is a bug!");
		}
	}
}
