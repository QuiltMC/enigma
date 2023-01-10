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

	private final Docker.Side side;

	private CompoundDock parentDock;
	private Docker.Height hovered;
	private Docker.Height location;
	private Docker hostedDocker;

	public Dock(Docker.Height height, Docker.Side side) {
		super(new BorderLayout());
		this.side = side;
		this.hovered = null;
		this.hostedDocker = null;
		this.parentDock = null;
		this.location = height;

		docks.add(this);
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		// we can rely on paint to always be called when the label is being dragged over the docker
		if (this.hovered != null) {
			Rectangle paintedBounds = this.getBoundsFor(new Point(0, 0), this.hovered);

			// paint using parent's graphics to avoid cutting off the filled box
			Graphics parentGraphics = this.parentDock.getGraphics();
			Color color = new Color(0, 0, 255, 84);
			parentGraphics.setColor(color);
			parentGraphics.fillRect(paintedBounds.x, paintedBounds.y, paintedBounds.width, paintedBounds.height);
			this.parentDock.repaint();
		}
	}

	public void receiveMouseEvent(MouseEvent e) {
		boolean b = this.hovered == null;

		if (this.isDisplayable()) {
			if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
				if (this.hovered == null) {
					if (!this.occupyingFullSide()) {
						// check top and bottom
						if (this.containsMouse(e, Docker.Height.TOP)) {
							this.hovered = Docker.Height.FULL;
						} else if (this.containsMouse(e, Docker.Height.BOTTOM)) {
							this.hovered = Docker.Height.BOTTOM;
						}
					} else {
						if (this.containsMouse(e, Docker.Height.TOP)) {
							this.hovered = Docker.Height.TOP;
						} else if (this.containsMouse(e, Docker.Height.FULL)) {
							this.hovered = Docker.Height.FULL;
						} else if (this.containsMouse(e, Docker.Height.BOTTOM)) {
							this.hovered = Docker.Height.BOTTOM;
						}
					}
				} else {
					// todo why is this like this?
					for (Docker.Height checkedLocation : Docker.Height.values()) {
						if (this.containsMouse(e, checkedLocation)) {
							this.hovered = checkedLocation;
							this.repaint();
							return;
						}
					}

					this.hovered = null;
					this.repaint();
				}
			} else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
				this.hovered = null;
				this.repaint();
			}
		}

		if (this.hovered == null && !b) {
			System.out.println();
		}
	}

	private boolean containsMouse(MouseEvent e, Docker.Height checkedLocation) {
		Rectangle screenBounds = this.getBoundsFor(this.getLocationOnScreen(), checkedLocation);
		return contains(screenBounds, e.getLocationOnScreen());
	}

	private Rectangle getBoundsFor(Point topLeft, Docker.Height location) {
		if (this.occupyingFullSide()) {
			if (location == Docker.Height.TOP) {
				// top: 0 to 1/4 y
				return new Rectangle(topLeft.x, topLeft.y, this.getWidth(), this.getHeight() / 2);
			} else if (location == Docker.Height.BOTTOM) {
				// bottom: 3/4 to 1 y
				return new Rectangle(topLeft.x, topLeft.y + (this.getHeight() / 4) * 3, this.getWidth(), this.getHeight() / 4);
			} else {
				// full: 1/4 to 3/4 y
				return new Rectangle(topLeft.x, topLeft.y + this.getHeight() / 4, this.getWidth(), this.getHeight() / 2);
			}
		} else {
			if (this.location == Docker.Height.BOTTOM) {
				if (location == Docker.Height.FULL) {
					// check top: 0 to 1/2 y
					return new Rectangle(topLeft.x, topLeft.y, this.getWidth(), this.getHeight() / 2);
				} else {
					// check bottom: 1/2 to 1 y
					return new Rectangle(topLeft.x, topLeft.y + this.getHeight() / 2, this.getWidth(), this.getHeight() / 2);
				}
			} else {
				// we know the location is top
				if (location == Docker.Height.FULL) {
					// check bottom: 1/2 to 1 y
					return new Rectangle(topLeft.x, topLeft.y + this.getHeight() / 2, this.getWidth(), this.getHeight() / 2);
				} else {
					// check top: 0 to 1/2 y
					return new Rectangle(topLeft.x, topLeft.y, this.getWidth(), this.getHeight() / 2);
				}
			}
		}
	}

	public void setHostedDocker(Docker docker) {
		this.setHostedDocker(docker, docker.getPreferredLocation().height());
	}

	public void setHostedDocker(Docker docker, Docker.Height height) {
		System.out.println("setHostedDocker: " + docker + " to " + height + " on " + this.side);

		// remove old docker
		this.removeHostedDocker();

		this.hostedDocker = docker;
		if (height == Docker.Height.FULL && this.parentDock.isSplit()) {
			System.out.println("unifying");
			this.parentDock.unify(this.getDockerLocation());
			this.location = height;

			this.add(this.hostedDocker);
		} else if (height != Docker.Height.FULL && !this.parentDock.isSplit()) {
			System.out.println("splitting");
			this.parentDock.split();
			this.location = height;

			if (height == Docker.Height.TOP) {
				this.parentDock.getTopDock().setHostedDocker(docker, height);
			} else {
				this.parentDock.getBottomDock().setHostedDocker(docker, height);
			}
		} else {
			System.out.println("adding as normal");
			this.add(this.hostedDocker);
		}

		// add new docker
		this.hostedDocker.dock(this.side, this.location);

		// revalidate to paint properly
		this.revalidate();

		// save to config
		UiConfig.setDocker(this, this.hostedDocker);
	}

	public void removeHostedDocker() {
		if (this.hostedDocker != null) {
			this.remove(this.hostedDocker);
			this.hostedDocker = null;
		}
	}

	public Docker.Height getDockerLocation() {
		return this.location;
	}

	public void setParentDock(CompoundDock parentDock) {
		if (this.parentDock == null) {
			this.parentDock = parentDock;
		} else {
			throw new IllegalStateException("parent dock is already set on this dock, cannot be set again!");
		}
	}

	private boolean contains(Rectangle rectangle, Point point) {
		return (point.x >= rectangle.x && point.x <= rectangle.x + rectangle.width)
				&& (point.y >= rectangle.y && point.y <= rectangle.y + rectangle.height);
	}

	private boolean occupyingFullSide() {
		return this.location == Docker.Height.FULL;
	}

	@Override
	public String toString() {
		return "Docker: " + this.location;
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
				if (dock.isDisplayable()) {
					for (Docker.Height location : Docker.Height.values()) {
						if (dock.containsMouse(event, location)) {
							dock.setHostedDocker(docker, location);
							return;
						}
					}
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

		public static Dock getForLocation(Docker.Height location, Docker.Side side) {
			for (Dock dock : docks) {
				if (dock.location == location && dock.side == side) {
					return dock;
				}
			}

			if (location == Docker.Height.FULL) {
				// todo using only top is a hack
				Dock dock = getForLocation(Docker.Height.TOP, side);
				dock.parentDock.unify(Docker.Height.TOP);

				return dock;
			}

			System.out.println("DEBUG: " + docks);
			throw new IllegalStateException("no dock for location " + location + "! this is a bug!");
		}
	}
}
