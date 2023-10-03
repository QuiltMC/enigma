package org.quiltmc.enigma.gui.docker;

import org.quiltmc.enigma.gui.ClassSelector;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.translation.representation.entry.ClassEntry;

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
		super(gui, new ClassSelector(gui, obfuscatedClassComparator));
	}

	@Override
	public String getId() {
		return "obfuscated_classes";
	}

	@Override
	public Location getPreferredButtonLocation() {
		return new Location(Side.LEFT, VerticalLocation.TOP);
	}
}
