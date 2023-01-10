package cuchaz.enigma.gui.docker;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;

public class CompoundDock extends JPanel {
	private final JSplitPane splitPane;
	private final Dock topDock;
	private final Dock bottomDock;

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
	}

	public CompoundDock(Docker.Side side) {
		this(new Dock(Docker.Height.TOP, side), new Dock(Docker.Height.BOTTOM, side));
	}

	public void split() {
		this.removeAll();
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

	public JSplitPane getSplitPane() {
		return this.splitPane;
	}

	public boolean isSplit() {
		return this.isSplit;
	}

	public Dock getTopDock() {
		return this.topDock;
	}

	public Dock getBottomDock() {
		return this.bottomDock;
	}
}
