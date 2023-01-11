package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.config.UiConfig;

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
	private Docker.Height location;
	private Docker hostedDocker;

	public Dock(Docker.Height height, Docker.Side side) {
		super(new BorderLayout());
		this.side = side;
		this.hostedDocker = null;
		this.parentDock = null;
		this.setLocation(height);

		docks.add(this);
	}

	public void setHostedDocker(Docker docker) {
		// remove old docker
		this.removeHostedDocker();

		this.hostedDocker = docker;
		this.add(this.hostedDocker);

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

	private void setLocation(Docker.Height height) {
		for (Dock dock : docks) {
			if (dock.location == height && dock.side == this.side) {
				throw new IllegalArgumentException("attempted to switch height of docker " + this + " to " + height + " on side " + this.side);
			}
		}

		this.location = height;
	}

	public CompoundDock getParentDock() {
		return this.parentDock;
	}

	@Override
	public String toString() {
		return "Dock: " + this.location + " on side " + this.side;
	}

	public static class Util {
		/**
		 * Calls {@link CompoundDock#receiveMouseEvent(MouseEvent)}} on both sides.
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
