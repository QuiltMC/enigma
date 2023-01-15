package cuchaz.enigma.gui.docker.dock;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.docker.Docker;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Optional;

public class CompoundDock extends JPanel {
	private final Gui gui;
	private final JSplitPane splitPane;
	private final Dock topDock;
	private final Dock bottomDock;
	private final Docker.Side side;

	/**
	 * Controls hover highlighting for this dock. A value of {@code null} represents no hover, otherwise it represents the currently hovered height.
	 */
	private Docker.VerticalLocation hovered;
	private boolean isSplit;
	private Dock unifiedDock;

	@SuppressWarnings("SuspiciousNameCombination")
	public CompoundDock(Gui gui, Docker.Side side, Dock topDock, Dock bottomDock) {
		super(new BorderLayout());

		this.topDock = topDock;
		this.bottomDock = bottomDock;
		this.splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, topDock, bottomDock);
		this.side = side;
		this.gui = gui;

		this.topDock.setParentDock(this);
		this.bottomDock.setParentDock(this);

		this.isSplit = true;
		this.unifiedDock = null;

		this.add(this.splitPane);

		Dock.COMPOUND_DOCKS.add(this);
	}

	public CompoundDock(Gui gui, Docker.Side side) {
		this(gui, side, new Dock(Docker.VerticalLocation.TOP, side), new Dock(Docker.VerticalLocation.BOTTOM, side));
	}

	/**
	 * Restores the state of this dock to the version saved in the config file.
	 */
	public void restoreState() {
		// restore docker state
		Optional<String[]> hostedDockers = UiConfig.getHostedDockers(this.side);
		if (hostedDockers.isPresent()) {
			for (String dockInfo : hostedDockers.get()) {
				if (!dockInfo.isBlank()) {

					String[] split = dockInfo.split(":");
					Docker.VerticalLocation location = Docker.VerticalLocation.valueOf(split[1]);
					Docker docker = Docker.getDocker(split[0]);

					this.host(docker, location);
				}
			}
		}

		// restore vertical divider state
		if (this.isSplit) {
			this.splitPane.setDividerLocation(UiConfig.getVerticalDockDividerLocation(this.side));
		}

		// restore horizontal divider state
		JSplitPane parentSplitPane = this.getParentSplitPane();
		parentSplitPane.setDividerLocation(UiConfig.getDividerLocation(this.side));
	}

	/**
	 * Saves the state of this dock to the config file.
	 */
	public void saveState() {
		// save hosted dockers
		UiConfig.setHostedDockers(this.side, this.encodeDockers());

		// save vertical divider state
		if (this.isSplit) {
			UiConfig.setVerticalDockDividerLocation(this.side, this.splitPane.getDividerLocation());
		}

		// save horizontal divider state
		JSplitPane parentSplitPane = this.getParentSplitPane();
		UiConfig.setDividerLocation(this.side, parentSplitPane.getDividerLocation());
	}

	/**
	 * @return the dockers hosted by this dock, in a config-file-friendly format.
	 */
	private String[] encodeDockers() {
		String[] dockers = new String[]{"", ""};
		Docker[] hostedDockers = new Docker[]{this.topDock.getHostedDocker(), this.bottomDock.getHostedDocker()};
		for (int i = 0; i < hostedDockers.length; i++) {
			Docker docker = hostedDockers[i];

			if (docker != null && docker.isDocked()) {
				dockers[i] = (docker.getId() + ":" + docker.getCurrentVerticalLocation().name());
			}
		}

		return dockers;
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
		switch (verticalLocation) {
			case BOTTOM -> {
				if (!this.isSplit) {
					this.split();
				}

				this.bottomDock.setHostedDocker(docker, verticalLocation);
			}
			case TOP -> {
				if (!this.isSplit) {
					this.split();
				}

				this.topDock.setHostedDocker(docker, verticalLocation);
			}
			case FULL -> {
				// note: always uses top, since it doesn't matter
				// since we're setting the hosted docker anyway

				if (this.isSplit) {
					this.unify(Docker.VerticalLocation.TOP);
				}

				// we cannot assume top here, since it could be called on a unified side
				this.unifiedDock.setHostedDocker(docker, verticalLocation);
			}
		}

		this.revalidate();
	}

	public boolean containsMouse(MouseEvent e, Docker.VerticalLocation checkedLocation) {
		Rectangle screenBounds = this.getBoundsFor(this.getLocationOnScreen(), checkedLocation);
		return contains(screenBounds, e.getLocationOnScreen());
	}

	public void split() {
		this.saveState();

		this.removeAll();
		this.splitPane.setBottomComponent(this.bottomDock);
		this.splitPane.setTopComponent(this.topDock);
		this.add(this.splitPane);
		this.isSplit = true;
		this.unifiedDock = null;
	}

	public void unify(Docker.VerticalLocation keptLocation) {
		this.saveState();

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

			Color color = new Color(0, 0, 255, 84);
			graphics.setColor(color);
			graphics.fillRect(paintedBounds.x, paintedBounds.y, paintedBounds.width, paintedBounds.height);
			this.repaint();
		}
	}
}
