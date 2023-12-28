package org.quiltmc.enigma.gui.docker;

import org.quiltmc.enigma.gui.ClassSelector;
import org.quiltmc.enigma.gui.Gui;

public class AllClassesDocker extends ClassesDocker {
	public AllClassesDocker(Gui gui) {
		super(gui, new ClassSelector(gui, ClassSelector.DEOBF_CLASS_COMPARATOR));
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
