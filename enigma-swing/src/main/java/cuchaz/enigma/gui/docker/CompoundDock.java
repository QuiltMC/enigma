package cuchaz.enigma.gui.docker;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

public class CompoundDock extends JPanel {
	private final JSplitPane splitPane;
	private final Dock topDock;
	private final Dock bottomDock;

	private Docker.Height hovered;

	private boolean isSplit;

	@SuppressWarnings("SuspiciousNameCombination")
	public CompoundDock(Dock topDock, Dock bottomDock) {
		super(new BorderLayout());
		// todo state restoration

		this.topDock = topDock;
		this.bottomDock = bottomDock;
		this.splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, topDock, bottomDock);

		this.topDock.setParentDock(this);
		this.bottomDock.setParentDock(this);

		this.isSplit = true;

		this.add(this.splitPane);

		Dock.COMPOUND_DOCKS.add(this);
	}

	public CompoundDock(Docker.Side side) {
		this(new Dock(Docker.Height.TOP, side), new Dock(Docker.Height.BOTTOM, side));
	}

	public void receiveMouseEvent(MouseEvent e) {
		if (this.isDisplayable()) {
			if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
				if (this.hovered == null) {
					if (this.isSplit) {
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
	}

	public void host(Docker docker, Docker.Height height) {
		switch (height) {
			case BOTTOM -> {
				if (!this.isSplit) {
					this.split();
				}

				this.bottomDock.setHostedDocker(docker);
			}
			case TOP -> {
				if (!this.isSplit) {
					this.split();
				}

				this.topDock.setHostedDocker(docker);
			}
			case FULL -> {
				// note: always uses top, since it doesn't matter
				// since we're setting the hosted docker anyway

				if (this.isSplit) {
					this.unify(Docker.Height.TOP);
				}

				// we cannot assume top here, since it could be called on a unified side
				if (this.bottomDock.isDisplayable()) {
					this.bottomDock.setHostedDocker(docker);
				} else if (this.topDock.isDisplayable()) {
					this.topDock.setHostedDocker(docker);
				} else {
					throw new IllegalArgumentException();
				}
			}
		}
	}

	public boolean containsMouse(MouseEvent e, Docker.Height checkedLocation) {
		Rectangle screenBounds = this.getBoundsFor(this.getLocationOnScreen(), checkedLocation);
		return contains(screenBounds, e.getLocationOnScreen());
	}

	public void split() {
		this.removeAll();
		this.splitPane.setBottomComponent(this.bottomDock);
		this.splitPane.setTopComponent(this.topDock);
		this.add(this.splitPane);
		this.isSplit = true;
	}

	public void unify(Docker.Height keptLocation) {
		this.removeAll();
		if (keptLocation == Docker.Height.TOP) {
			this.add(this.topDock);
		} else if (keptLocation == Docker.Height.BOTTOM) {
			this.add(this.bottomDock);
		} else {
			throw new IllegalArgumentException("cannot keep nonexistent dock for location: " + keptLocation);
		}

		this.isSplit = false;
	}

	public void dropDockerFromMouse(Docker docker, MouseEvent event) {
		if (this.containsMouse(event, Docker.Height.TOP)) {
			this.host(docker, Docker.Height.TOP);
		} else if (this.containsMouse(event, Docker.Height.BOTTOM)) {
			this.host(docker, Docker.Height.BOTTOM);
		} else if (this.containsMouse(event, Docker.Height.FULL)) {
			this.host(docker, Docker.Height.FULL);
		}
	}

	private Rectangle getHighlightBoundsFor(Point topLeft, Docker.Height checkedLocation) {
		// todo this isn't good
		Rectangle bounds = this.getBoundsFor(topLeft, checkedLocation);
		int height = switch (checkedLocation) {
			case FULL -> bounds.height;
			case BOTTOM, TOP -> bounds.height * 2;
		};
		return new Rectangle(bounds.x, checkedLocation == Docker.Height.BOTTOM ? bounds.y - this.getHeight() / 4 : bounds.y, bounds.width, height);
	}

	private Rectangle getBoundsFor(Point topLeft, Docker.Height checkedLocation) {
		if (checkedLocation == Docker.Height.TOP) {
			// top: 0 to 1/4 y
			return new Rectangle(topLeft.x, topLeft.y, this.getWidth(), this.getHeight() / 4);
		} else if (checkedLocation == Docker.Height.BOTTOM) {
			// bottom: 3/4 to 1 y
			return new Rectangle(topLeft.x, topLeft.y + (this.getHeight() / 4) * 3, this.getWidth(), this.getHeight() / 4);
		} else {
			// full: 1/4 to 3/4 y
			return new Rectangle(topLeft.x, topLeft.y + this.getHeight() / 4, this.getWidth(), this.getHeight() / 2);
		}
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
