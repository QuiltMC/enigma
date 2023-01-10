package cuchaz.enigma.gui.panels;

import java.awt.BorderLayout;
import java.util.Comparator;

import javax.swing.JScrollPane;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public class ObfPanel extends Docker {
	public final ClassSelector obfClasses;

	public ObfPanel(Gui gui) {
		super(gui);
		Comparator<ClassEntry> obfClassComparator = (a, b) -> {
			String aname = a.getFullName();
			String bname = b.getFullName();
			if (aname.length() != bname.length()) {
				return aname.length() - bname.length();
			}
			return aname.compareTo(bname);
		};

		this.obfClasses = new ClassSelector(gui, obfClassComparator, false);
		this.obfClasses.setSelectionListener(gui.getController()::navigateTo);
		this.obfClasses.setRenameSelectionListener(gui::onRenameFromClassTree);

		this.add(this.title, BorderLayout.NORTH);
		this.title.setConstraints(BorderLayout.NORTH);
		this.add(new JScrollPane(this.obfClasses), BorderLayout.CENTER);
	}

	public String getId() {
		return Type.OBFUSCATED_CLASSES;
	}

	@Override
	public ButtonPosition getButtonPosition() {
		return ButtonPosition.LEFT_TOP;
	}

	@Override
	public Location getPreferredLocation() {
		return new Location(Side.LEFT, Height.TOP);
	}
}
