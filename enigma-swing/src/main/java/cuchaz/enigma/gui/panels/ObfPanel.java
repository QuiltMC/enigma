package cuchaz.enigma.gui.panels;

import java.awt.BorderLayout;
import java.util.Comparator;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.gui.docker.DockerLabel;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.I18n;

public class ObfPanel extends Docker {
	public final ClassSelector obfClasses;
	public final DockerLabel title;

	public ObfPanel(Gui gui) {
		super(new BorderLayout());
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
		this.title = new DockerLabel(gui, this, I18n.translate("info_panel.classes.obfuscated"));

		this.add(this.title, BorderLayout.NORTH);
		this.title.setConstraints(BorderLayout.NORTH);
		this.add(new JScrollPane(this.obfClasses), BorderLayout.CENTER);

		this.retranslateUi();
	}

	public void retranslateUi() {
		this.title.setText(I18n.translate("info_panel.classes.obfuscated"));
	}
}
