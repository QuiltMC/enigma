package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.UiConfig;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles the docking of {@link Docker}s.
 */
public class Dock extends JPanel {
	private static final List<Dock> instances = new ArrayList<>();

	private final Gui gui;
	private final JSplitPane splitPane;
	private final DockerContainer topDock;
	private final DockerContainer bottomDock;
	private final Docker.Side side;

	/**
	 * Controls hover highlighting for this dock. A value of {@code null} represents no hover, otherwise it represents the currently hovered height.
	 */
	private Docker.VerticalLocation hovered;
	private boolean isSplit;
	private DockerContainer unifiedDock;

	@SuppressWarnings("SuspiciousNameCombination")
	public Dock(Gui gui, Docker.Side side) {
		super(new BorderLayout());

		this.topDock = new DockerContainer();
		this.bottomDock = new DockerContainer();
		this.splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, topDock, bottomDock);
		this.side = side;
		this.gui = gui;

		this.isSplit = true;
		this.unifiedDock = null;

		this.add(this.splitPane);

		instances.add(this);
	}

	/**
	 * Restores the state of this dock to the version saved in the config file.
	 */
	public void restoreState() {
		// restore docker state
		Map<Docker, Docker.VerticalLocation> hostedDockers = UiConfig.getHostedDockers(this.side);
		hostedDockers.forEach((this::host));

		this.restoreDividerState();

		if (this.isEmpty()) {
			this.setVisible(false);
		}
	}

	/**
	 * Saves the state of this dock to the config file.
	 */
	public void saveState() {
		UiConfig.setHostedDockers(this.side, this.getDockers());
		this.saveDividerState();
	}

	public void restoreDividerState() {
		// restore vertical divider state
		if (this.isSplit) {
			this.splitPane.setDividerLocation(UiConfig.getVerticalDockDividerLocation(this.side));
		}

		// restore horizontal divider state
		JSplitPane parentSplitPane = this.getParentSplitPane();
		parentSplitPane.setDividerLocation(UiConfig.getHorizontalDividerLocation(this.side));
	}

	public void saveDividerState() {
		// save vertical divider state
		if (this.isSplit) {
			UiConfig.setVerticalDockDividerLocation(this.side, this.splitPane.getDividerLocation());
		}

		// save horizontal divider state
		JSplitPane parentSplitPane = this.getParentSplitPane();
		UiConfig.setHorizontalDividerLocation(this.side, parentSplitPane.getDividerLocation());
	}

	public void receiveMouseEvent(MouseEvent e) {
		if (this.isDisplayable()) {
			if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
				for (Docker.VerticalLocation verticalLocation : Docker.VerticalLocation.values()) {
					if (this.containsMouse(e, verticalLocation)) {
						this.hovered = verticalLocation;
						return;
					}
				}

				// we've checked every height and can confirm the dock is not being hovered
				this.hovered = null;
			} else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
				this.hovered = null;
				this.repaint();
			}
		}
	}

	public void host(Docker docker, Docker.VerticalLocation verticalLocation) {
		Util.undock(docker);

		switch (verticalLocation) {
			case BOTTOM -> {
				if (!this.isSplit) {
					this.split(verticalLocation);
				}

				this.bottomDock.setHostedDocker(docker);
			}
			case TOP -> {
				if (!this.isSplit) {
					this.split(verticalLocation);
				}

				this.topDock.setHostedDocker(docker);
			}
			case FULL -> {
				// note: always uses top, since it doesn't matter
				// since we're setting the hosted docker anyway

				if (this.isSplit) {
					this.unify(Docker.VerticalLocation.TOP);
				}

				// we cannot assume top here, since it could be called on a unified side
				this.unifiedDock.setHostedDocker(docker);
			}
		}

		this.updateVisibility();
		this.revalidate();
	}

	public Docker[] getDockers() {
		return new Docker[]{this.topDock.hostedDocker, this.bottomDock.hostedDocker};
	}

	public void updateVisibility() {
		if (this.isEmpty()) {
			this.saveDividerState();
			this.setVisible(false);
		} else {
			this.restoreDividerState();
			this.setVisible(true);
		}
	}

	public void removeDocker(Docker docker) {
		Docker.Location location = Util.findLocation(docker);
		if (location != null) {
			this.removeDocker(location.verticalLocation());
		} else {
			throw new IllegalArgumentException("attempted to remove docker from dock for side " + this.side + " that is not added to that dock!");
		}
	}

	public void removeDocker(Docker.VerticalLocation location) {
		DockerContainer container = this.getDock(location);
		container.setHostedDocker(null);
		this.updateVisibility();
		this.revalidate();
		this.repaint();
	}

	public boolean containsMouse(MouseEvent e, Docker.VerticalLocation checkedLocation) {
		if (this.isVisible()) {
			Rectangle screenBounds = this.getBoundsFor(this.getLocationOnScreen(), checkedLocation);
			return contains(screenBounds, e.getLocationOnScreen());
		}

		return false;
	}

	private boolean isEmpty() {
		return this.topDock.getHostedDocker() == null && this.bottomDock.getHostedDocker() == null;
	}

	public void split(Docker.VerticalLocation toIntroduceDocker) {
		this.saveDividerState();

		// convenience: if we're splitting a panel that has a docker, we should keep that docker and open it in the unoccupied slot
		if (this.getDock(toIntroduceDocker).equals(this.unifiedDock)) {
			this.getDock(toIntroduceDocker.inverse()).setHostedDocker(this.getDock(toIntroduceDocker).getHostedDocker());
		}

		this.removeAll();
		this.splitPane.setBottomComponent(this.bottomDock);
		this.splitPane.setTopComponent(this.topDock);
		this.add(this.splitPane);

		this.isSplit = true;
		this.unifiedDock = null;
	}

	public void unify(Docker.VerticalLocation keptLocation) {
		this.saveDividerState();

		this.removeAll();
		if (keptLocation == Docker.VerticalLocation.TOP) {
			this.add(this.topDock);
			this.unifiedDock = this.topDock;
		} else if (keptLocation == Docker.VerticalLocation.BOTTOM) {
			this.add(this.bottomDock);
			this.unifiedDock = this.bottomDock;
		} else {
			throw new IllegalArgumentException("cannot keep nonexistent dock for location: " + keptLocation);
		}

		this.isSplit = false;
	}

	public void dropDockerFromMouse(Docker docker, MouseEvent event) {
		if (this.containsMouse(event, Docker.VerticalLocation.TOP)) {
			this.host(docker, Docker.VerticalLocation.TOP);
		} else if (this.containsMouse(event, Docker.VerticalLocation.BOTTOM)) {
			this.host(docker, Docker.VerticalLocation.BOTTOM);
		} else if (this.containsMouse(event, Docker.VerticalLocation.FULL)) {
			this.host(docker, Docker.VerticalLocation.FULL);
		}
	}

	public DockerContainer getDock(Docker.VerticalLocation verticalLocation) {
		 return switch (verticalLocation) {
			case TOP -> this.topDock;
			case BOTTOM -> this.bottomDock;
			case FULL -> this.unifiedDock;
		};
	}

	private Rectangle getHighlightBoundsFor(Point topLeft, Docker.VerticalLocation checkedLocation) {
		Rectangle bounds = this.getBoundsFor(topLeft, checkedLocation);
		int height = switch (checkedLocation) {
			case FULL -> bounds.height;
			case BOTTOM, TOP -> bounds.height * 2;
		};
		return new Rectangle(bounds.x, checkedLocation == Docker.VerticalLocation.BOTTOM ? bounds.y - this.getHeight() / 4 : bounds.y, bounds.width, height);
	}

	private Rectangle getBoundsFor(Point topLeft, Docker.VerticalLocation checkedLocation) {
		if (checkedLocation == Docker.VerticalLocation.TOP) {
			// top: 0 to 1/4 y
			return new Rectangle(topLeft.x, topLeft.y, this.getWidth(), this.getHeight() / 4);
		} else if (checkedLocation == Docker.VerticalLocation.BOTTOM) {
			// bottom: 3/4 to 1 y
			return new Rectangle(topLeft.x, topLeft.y + (this.getHeight() / 4) * 3, this.getWidth(), this.getHeight() / 4);
		} else {
			// full: 1/4 to 3/4 y
			return new Rectangle(topLeft.x, topLeft.y + this.getHeight() / 4, this.getWidth(), this.getHeight() / 2);
		}
	}

	private JSplitPane getParentSplitPane() {
		return this.side == Docker.Side.RIGHT ? this.gui.getSplitRight() : this.gui.getSplitLeft();
	}

	private static boolean contains(Rectangle rectangle, Point point) {
		return (point.x >= rectangle.x && point.x <= rectangle.x + rectangle.width)
				&& (point.y >= rectangle.y && point.y <= rectangle.y + rectangle.height);
	}

	@Override
	public void paint(Graphics graphics) {
		super.paint(graphics);

		// we can rely on paint to always be called when the label is being dragged over the docker
		if (this.hovered != null) {
			Rectangle paintedBounds = this.getHighlightBoundsFor(new Point(0, 0), this.hovered);

			Color color = UiConfig.getDockHighlightColor();
			graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
			graphics.fillRect(paintedBounds.x, paintedBounds.y, paintedBounds.width, paintedBounds.height);
			this.repaint();
		}
	}

	/**
	 * Helper class to store a docker.
	 */
	private static class DockerContainer extends JPanel {
		private Docker hostedDocker;

		public DockerContainer() {
			super(new BorderLayout());
			this.hostedDocker = null;
		}

		public Docker getHostedDocker() {
			return this.hostedDocker;
		}

		public void setHostedDocker(Docker hostedDocker) {
			if (this.hostedDocker != null) {
				this.remove(this.hostedDocker);
				this.hostedDocker.setVisible(false);
			}

			this.hostedDocker = hostedDocker;

			if (this.hostedDocker != null) {
				this.add(this.hostedDocker);
				this.hostedDocker.setVisible(true);
			}
		}
	}

	public static class Util {
		/**
		 * Calls {@link Dock#receiveMouseEvent(MouseEvent)}} on all sides.
		 * @param event the mouse event to pass to the docks
		 */
		public static void receiveMouseEvent(MouseEvent event) {
			for (Dock dock : instances) {
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
			for (Dock dock : instances) {
				if (dock.isDisplayable()) {
					dock.dropDockerFromMouse(docker, event);
				}
			}
		}

		/**
		 * @return the location of the provided docker, or {@code null} if it is not currently present on the screen.
		 */
		public static Docker.Location findLocation(Docker docker) {
			for (Dock dock : instances) {
				for (Docker d : dock.getDockers()) {
					if (d != null && d.getId().equals(docker.getId())) {
						if (dock.unifiedDock != null && d.equals(dock.unifiedDock.getHostedDocker())) {
							return new Docker.Location(dock.side, Docker.VerticalLocation.FULL);
						} else if (d.equals(dock.topDock.getHostedDocker())) {
							return new Docker.Location(dock.side, Docker.VerticalLocation.TOP);
						} else if (d.equals(dock.bottomDock.getHostedDocker())) {
							return new Docker.Location(dock.side, Docker.VerticalLocation.BOTTOM);
						}
					}
				}
			}

			return null;
		}

		/**
		 * @return the docker's parent {@link Dock}, or {@code null} if it is not currently present on the screen.
		 */
		public static Dock findDock(Docker docker) {
			for (Dock dock : instances) {
				for (Docker d : dock.getDockers()) {
					if (d != null && d.getId().equals(docker.getId())) {
						return dock;
					}
				}
			}

			return null;
		}

		/**
		 * Removes the docker from the screen.
		 */
		public static void undock(Docker docker) {
			for (Dock dock : instances) {
				for (Docker d : dock.getDockers()) {
					if (d != null && d.getId().equals(docker.getId())) {
						dock.removeDocker(d);
					}
				}
			}
		}

		/**
		 * @return whether the docker is currently visible on the screen
		 */
		public static boolean isDocked(Docker docker) {
			return findDock(docker) != null;
		}
	}
}
