package cuchaz.enigma.gui.docker.dock;

import cuchaz.enigma.gui.docker.Docker;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Objects;

public class Dock extends JPanel {

	private CompoundDock parentDock;
	private Docker hostedDocker;

	public Dock() {
		super(new BorderLayout());
		this.hostedDocker = null;
		this.parentDock = null;
	}

	public void setHostedDocker(Docker docker) {
		// remove old docker
		this.removeHostedDocker();

		this.hostedDocker = docker;
		this.add(this.hostedDocker);

		// revalidate to paint properly
		this.getParent().revalidate();
	}

	public void removeHostedDocker() {
		if (this.hostedDocker != null) {
			this.remove(this.hostedDocker);
			this.hostedDocker = null;
			this.parentDock.onDockRemoval();
			this.repaint();
		}
	}

	public Docker getHostedDocker() {
		return this.hostedDocker;
	}

	public void setParentDock(CompoundDock parentDock) {
		if (this.parentDock == null) {
			this.parentDock = parentDock;
		} else {
			throw new IllegalStateException("parent dock is already set on this dock, cannot be set again!");
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Dock dock = (Dock) o;
		return Objects.equals(parentDock, dock.parentDock) && Objects.equals(hostedDocker, dock.hostedDocker);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parentDock, hostedDocker);
	}
}
