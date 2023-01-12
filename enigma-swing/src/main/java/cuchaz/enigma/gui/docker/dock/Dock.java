package cuchaz.enigma.gui.docker.dock;

import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.docker.Docker;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dock extends JPanel {
	public static final List<CompoundDock> COMPOUND_DOCKS = new ArrayList<>();
	private static final List<Dock> docks = new ArrayList<>();

	private final Docker.Side side;

	private CompoundDock parentDock;
	private Docker.VerticalLocation dockVerticalLocation;
	private Docker hostedDocker;

	public Dock(Docker.VerticalLocation dockVerticalLocation, Docker.Side side) {
		super(new BorderLayout());
		this.side = side;
		this.hostedDocker = null;
		this.parentDock = null;
		this.setHeight(dockVerticalLocation);

		docks.add(this);
	}

	public void setHostedDocker(Docker docker) {
		// remove old docker
		this.removeHostedDocker();

		this.hostedDocker = docker;
		this.add(this.hostedDocker);

		// add new docker
		this.hostedDocker.dock(this, this.dockVerticalLocation);

		// revalidate to paint properly
		this.revalidate();

		// save to config
		UiConfig.setDocker(this, this.hostedDocker);
	}

	public void removeHostedDocker() {
		if (this.hostedDocker != null) {
			this.hostedDocker.undock();
			this.hostedDocker = null;
			this.repaint();
			// todo revalidate side buttons
		}
	}

	public Docker.Side getSide() {
		return this.side;
	}

	public Docker.VerticalLocation getDockerLocation() {
		return this.dockVerticalLocation;
	}

	public void setParentDock(CompoundDock parentDock) {
		if (this.parentDock == null) {
			this.parentDock = parentDock;
		} else {
			throw new IllegalStateException("parent dock is already set on this dock, cannot be set again!");
		}
	}

	private void setHeight(Docker.VerticalLocation dockVerticalLocation) {
		for (Dock dock : docks) {
			if (dock.dockVerticalLocation == dockVerticalLocation && dock.side == this.side) {
				throw new IllegalArgumentException("attempted to switch height of docker " + this + " to " + dockVerticalLocation + " on side " + this.side);
			}
		}

		this.dockVerticalLocation = dockVerticalLocation;
	}

	public CompoundDock getParentDock() {
		return this.parentDock;
	}

	public static class Util {
		/**
		 * Calls {@link CompoundDock#receiveMouseEvent(MouseEvent)}} on all sides.
		 * @param event the mouse event to pass to the docks
		 */
		public static void receiveMouseEvent(MouseEvent event) {
			for (CompoundDock dock : COMPOUND_DOCKS) {
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
			for (CompoundDock dock : COMPOUND_DOCKS) {
				if (dock.isDisplayable()) {
					dock.dropDockerFromMouse(docker, event);
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

		/**
		 * Hides the specified dock and removes it from its parent.
		 * @param docker the docker to hide
		 */
		public static void undock(Docker docker) {
			for (Dock dock : docks) {
				if (docker.equals(dock.hostedDocker)) {
					dock.removeHostedDocker();
				}
			}
		}
	}
}
