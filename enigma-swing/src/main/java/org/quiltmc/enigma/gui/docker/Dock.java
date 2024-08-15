package org.quiltmc.enigma.gui.docker;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.DockerConfig;
import org.quiltmc.enigma.gui.docker.component.DockerButton;
import org.quiltmc.enigma.gui.docker.component.DockerSelector;
import org.quiltmc.enigma.gui.docker.component.Draggable;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Handles the docking of {@link Docker}s.
 */
public class Dock extends JPanel {
	private static final List<Dock> INSTANCES = new ArrayList<>();

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
	private Docker toSave;

	@SuppressWarnings("SuspiciousNameCombination")
	public Dock(Gui gui, Docker.Side side) {
		super(new BorderLayout());

		this.topDock = new DockerContainer();
		this.bottomDock = new DockerContainer();
		this.unifiedDock = null;
		this.splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, this.topDock, this.bottomDock);
		this.side = side;
		this.gui = gui;

		this.isSplit = true;
		this.add(this.splitPane);
		this.setVisible(false);

		INSTANCES.add(this);
	}

	/**
	 * Restores the state of this dock to the version saved in the config file.
	 */
	public void restoreState(DockerManager manager) {
		// restore docker state
		DockerConfig.SelectedDockers hostedDockers = Config.dockers().getSelectedDockers(this.side);
		hostedDockers.asMap().forEach((id, location) -> this.host(manager.getDocker(id), location));

		this.restoreDividerState(true);

		if (this.isEmpty()) {
			this.setVisible(false);
		}
	}

	public void restoreDividerState(boolean init) {
		// restore vertical divider state
		if (this.isSplit) {
			this.splitPane.setDividerLocation(Config.dockers().getVerticalDividerLocation(this.side));
		}

		// restore horizontal divider state
		JSplitPane parentSplitPane = this.getParentSplitPane();
		int location = Config.dockers().getHorizontalDividerLocation(this.side);

		// hack fix: if the right dock is closed while the left dock is open, the divider location is saved as if the left dock is open,
		// thereby offsetting the divider location by the width of the left dock. which means, if the right dock is reopened while the left dock is closed,
		// the divider location is too far to the left by the width of the left dock. so here we offset the location to avoid that.
		if (init && this.side == Docker.Side.RIGHT && !this.gui.getSplitLeft().getLeftComponent().isVisible() && Config.dockers().savedWithLeftDockerOpen.value()) {
			location += Config.dockers().getHorizontalDividerLocation(Docker.Side.LEFT);
		}

		parentSplitPane.setDividerLocation(location);
	}

	public void saveDividerState() {
		if (this.isVisible()) {
			// save vertical divider state
			if (this.isSplit) {
				Config.dockers().setVerticalDividerLocation(this.side, this.splitPane.getDividerLocation());
			}

			// save horizontal divider state
			JSplitPane parentSplitPane = this.getParentSplitPane();
			Config.dockers().setHorizontalDividerLocation(this.side, parentSplitPane.getDividerLocation());

			// hack
			if (this.side == Docker.Side.RIGHT) {
				Config.dockers().savedWithLeftDockerOpen.setValue(this.gui.getSplitLeft().getLeftComponent().isVisible(), true);
			}
		}
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
		this.host(docker, verticalLocation, true);
	}

	public void host(Docker docker, Docker.VerticalLocation verticalLocation, boolean avoidEmptySpace) {
		Config.dockers().getSelectedDockers(this.side).add(docker.getId(), verticalLocation);

		Dock dock = Util.findDock(docker);
		if (dock != null) {
			dock.removeDocker(verticalLocation, avoidEmptySpace);
		}

		if (verticalLocation == Docker.VerticalLocation.BOTTOM || verticalLocation == Docker.VerticalLocation.TOP) {
			// if we'd be leaving empty space via opening, we want to host the docker as the full panel
			// this is to avoid wasting space
			if (avoidEmptySpace && ((this.isSplit && this.getDock(verticalLocation.inverse()).getHostedDocker() == null)
					|| (!this.isSplit && this.unifiedDock.getHostedDocker() == null)
					|| (!this.isSplit && this.unifiedDock.getHostedDocker().getId().equals(docker.getId())))) {
				this.host(docker, Docker.VerticalLocation.FULL);
				return;
			}

			if (!this.isSplit) {
				this.split();
			}

			// preserve divider location and host
			int location = this.splitPane.getDividerLocation();
			this.getDock(verticalLocation).setHostedDocker(docker);
			this.splitPane.setDividerLocation(location);

			if (this.toSave != null && !this.toSave.equals(docker)) {
				this.host(this.toSave, verticalLocation.inverse());
				this.toSave = null;
			}

			// make button follow docker from side to side
			Docker.Location previousLocation = Util.findLocation(docker);

			if (previousLocation != null && previousLocation.side() != this.side) {
				DockerButton button = docker.getButton();
				DockerSelector selector = this.gui.getMainWindow().getDockerSelector(this.side);
				Container parent = button.getParent();

				parent.remove(button);
				(verticalLocation == Docker.VerticalLocation.TOP ? selector.getTopSelector() : selector.getBottomSelector()).add(button);
				button.setSide(this.side);
				Config.dockers().putButtonLocation(docker, this.side, verticalLocation);

				button.getParent().revalidate();
				button.getParent().repaint();
				selector.revalidate();
				selector.repaint();
			}
		} else if (verticalLocation == Docker.VerticalLocation.FULL) {
			// note: always uses top, since it doesn't matter
			// since we're setting the hosted docker anyway

			if (this.isSplit) {
				this.unify(Docker.VerticalLocation.TOP);
			}

			// we cannot assume top here, since it could be called on a unified side
			this.unifiedDock.setHostedDocker(docker);
		}

		this.updateVisibility();
		this.revalidate();
	}

	public Docker[] getDockers() {
		return new Docker[]{this.topDock.hostedDocker, this.bottomDock.hostedDocker};
	}

	public void updateVisibility() {
		if (this.isVisible() && this.isEmpty()) {
			this.saveDividerState();
			this.setVisible(false);
		} else if (!this.isVisible()) {
			this.restoreDividerState(false);
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
		this.removeDocker(location, true);
	}

	private void removeDocker(Docker.VerticalLocation location, boolean avoidEmptySpace) {
		// do not leave empty dockers
		if (avoidEmptySpace && location != Docker.VerticalLocation.FULL && this.getDock(location.inverse()).getHostedDocker() != null) {
			this.host(this.getDock(location.inverse()).getHostedDocker(), Docker.VerticalLocation.FULL, false);
			return;
		}

		DockerContainer container = this.getDock(location);
		if (container != null) {
			container.setHostedDocker(null);
		}

		this.updateVisibility();
		this.revalidate();
		this.repaint();
	}

	public boolean containsMouse(MouseEvent e, Docker.VerticalLocation checkedLocation) {
		if (this.isVisible()) {
			Rectangle screenBounds = this.getBoundsFor(this.getLocationOnScreen(), checkedLocation);
			return Draggable.contains(screenBounds, e.getLocationOnScreen());
		}

		return false;
	}

	private boolean isEmpty() {
		return (!this.isSplit && this.unifiedDock.getHostedDocker() == null) || (this.topDock.getHostedDocker() == null && this.bottomDock.getHostedDocker() == null);
	}

	public void split() {
		this.saveDividerState();
		this.removeAll();

		for (Docker docker : this.getDockers()) {
			if (docker != null) {
				this.toSave = docker;
			}
		}

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

		this.removeDocker(keptLocation.inverse());
		this.isSplit = false;
	}

	/**
	 * Drops the docker from the given mouse position, if it is within the bounds of this dock.
	 * @return whether the docker was successfully positioned
	 */
	public boolean dropDockerFromMouse(Docker docker, MouseEvent event) {
		for (Docker.VerticalLocation verticalLocation : Docker.VerticalLocation.values()) {
			if (this.containsMouse(event, verticalLocation)) {
				this.host(docker, verticalLocation);
				return true;
			}
		}

		return false;
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

	@Override
	public void paint(Graphics graphics) {
		super.paint(graphics);

		// we can rely on paint to always be called when the label is being dragged over the docker
		if (this.hovered != null) {
			Rectangle paintedBounds = this.getHighlightBoundsFor(new Point(0, 0), this.hovered);

			Color color = Config.getCurrentSyntaxPaneColors().dockHighlight.value();
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

		DockerContainer() {
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

			if (hostedDocker != null) {
				this.add(hostedDocker);
				hostedDocker.setVisible(true);

				// since the docker is being hosted, we know that findLocation will succeed
				hostedDocker.getTitleBar().updateResizeButton(Objects.requireNonNull(Util.findLocation(hostedDocker)).verticalLocation());
			}
		}
	}

	public static class Util {
		/**
		 * Calls {@link Dock#receiveMouseEvent(MouseEvent)}} on all sides.
		 * @param event the mouse event to pass to the docks
		 */
		public static void receiveMouseEvent(MouseEvent event) {
			for (Dock dock : INSTANCES) {
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
			for (Dock dock : INSTANCES) {
				if (dock.isDisplayable() && dock.dropDockerFromMouse(docker, event)) {
					return;
				}
			}
		}

		/**
		 * @return the location of the provided docker, or {@code null} if it is not currently present on the screen.
		 */
		public static Docker.Location findLocation(Docker docker) {
			for (Dock dock : INSTANCES) {
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
			for (Dock dock : INSTANCES) {
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
			for (Dock dock : INSTANCES) {
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
