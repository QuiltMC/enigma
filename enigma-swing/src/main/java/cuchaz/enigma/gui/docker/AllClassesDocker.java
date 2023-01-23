package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;

public class AllClassesDocker extends ClassesDocker {
	public AllClassesDocker(Gui gui) {
		super(gui, new ClassSelector(gui, ClassSelector.DEOBF_CLASS_COMPARATOR, true));
	}

	@Override
	public String getId() {
		return Type.ALL_CLASSES;
	}

	@Override
	public Location getButtonPosition() {
		return new Location(Side.LEFT, VerticalLocation.TOP);
	}

	@Override
	public Location getPreferredLocation() {
		return new Location(Side.LEFT, VerticalLocation.FULL);
	}
}
