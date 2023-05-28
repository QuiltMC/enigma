package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;

public class DeobfuscatedClassesDocker extends ClassesDocker {
	public DeobfuscatedClassesDocker(Gui gui) {
		super(gui, new ClassSelector(gui, ClassSelector.DEOBF_CLASS_COMPARATOR));
	}

	@Override
	public String getId() {
		return "deobfuscated_classes";
	}

	@Override
	public Location getPreferredButtonLocation() {
		return new Location(Side.LEFT, VerticalLocation.BOTTOM);
	}
}
