package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import java.util.Comparator;

public class ObfuscatedClassesDocker extends ClassesDocker {
	private static final Comparator<ClassEntry> obfuscatedClassComparator = (a, b) -> {
		String aName = a.getFullName();
		String bName = b.getFullName();
		if (aName.length() != bName.length()) {
			return aName.length() - bName.length();
		}
		return aName.compareTo(bName);
	};

	public ObfuscatedClassesDocker(Gui gui) {
		super(gui, new ClassSelector(gui, obfuscatedClassComparator, false));
	}

	@Override
	public String getId() {
		return Type.OBFUSCATED_CLASSES;
	}

	@Override
	public Location getButtonPosition() {
		return new Location(Side.LEFT, VerticalLocation.TOP);
	}

	@Override
	public Location getPreferredLocation() {
		return new Location(Side.LEFT, VerticalLocation.TOP);
	}
}
