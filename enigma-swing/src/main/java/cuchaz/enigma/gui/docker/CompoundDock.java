package cuchaz.enigma.gui.docker;

import javax.swing.JSplitPane;

public class CompoundDock extends JSplitPane {
	private final Dock topDock;
	private final Dock bottomDock;

	@SuppressWarnings("SuspiciousNameCombination")
	public CompoundDock(Dock topDock, Dock bottomDock) {
		super(JSplitPane.VERTICAL_SPLIT, true, topDock, bottomDock);
		this.topDock = topDock;
		this.bottomDock = bottomDock;
	}

	public CompoundDock(Docker.Location location) {
		this(new Dock(location.isLeft() ? Docker.Location.LEFT_TOP : Docker.Location.RIGHT_TOP), new Dock(location.isLeft() ? Docker.Location.LEFT_BOTTOM : Docker.Location.RIGHT_BOTTOM));
	}

	public Dock getTopDock() {
		return this.topDock;
	}

	public Dock getBottomDock() {
		return this.bottomDock;
	}
}
