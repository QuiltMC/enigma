package cuchaz.enigma.gui.panels;

import java.awt.BorderLayout;
import java.util.Comparator;

import javax.swing.JScrollPane;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public class ObfuscatedClassesPanel extends Docker {
	private static final Comparator<ClassEntry> obfuscatedClassComparator = (a, b) -> {
		String aName = a.getFullName();
		String bName = b.getFullName();
		if (aName.length() != bName.length()) {
			return aName.length() - bName.length();
		}
		return aName.compareTo(bName);
	};

	private final ClassSelector classSelector;

	public ObfuscatedClassesPanel(Gui gui) {
		super(gui);

		this.classSelector = new ClassSelector(gui, obfuscatedClassComparator, false);
		this.classSelector.setSelectionListener(gui.getController()::navigateTo);
		this.classSelector.setRenameSelectionListener(gui::onRenameFromClassTree);

		this.add(this.title, BorderLayout.NORTH);
		this.add(new JScrollPane(this.classSelector), BorderLayout.CENTER);
	}

	public ClassSelector getClassSelector() {
		return this.classSelector;
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
