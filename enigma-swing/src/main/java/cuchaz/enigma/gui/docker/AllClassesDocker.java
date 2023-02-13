package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;

public class AllClassesDocker extends ClassesDocker {
	public AllClassesDocker(Gui gui) {
		super(gui, new ClassSelector(gui, ClassSelector.DEOBF_CLASS_COMPARATOR, true));
	}

	@Override
	public String getId() {
		return "all_classes";
	}

	@Override
	public Location getPreferredButtonLocation() {
		return new Location(Side.LEFT, VerticalLocation.TOP);
	}
}
