package cuchaz.enigma.gui.docker;

import javax.swing.JSplitPane;

public class CompoundDock extends JSplitPane {
	private final Dock upperDock;
	private final Dock lowerDock;

	public CompoundDock(Dock upperDock, Dock lowerDock) {
		super(JSplitPane.VERTICAL_SPLIT, true, upperDock, lowerDock);
		this.upperDock = upperDock;
		this.lowerDock = lowerDock;
	}

	public CompoundDock() {
		this(new Dock(), new Dock());
	}

	public Dock getUpperDock() {
		return this.upperDock;
	}

	public Dock getLowerDock() {
		return this.lowerDock;
	}
}
