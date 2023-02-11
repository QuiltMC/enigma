package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;

import javax.swing.JScrollPane;
import java.awt.BorderLayout;

public abstract class ClassesDocker extends Docker {
	protected final ClassSelector selector;

	protected ClassesDocker(Gui gui, ClassSelector selector) {
		super(gui);
		this.selector = selector;
		this.selector.setSelectionListener(gui.getController()::navigateTo);
		this.selector.setRenameSelectionListener(((vc, prevData, data, node) -> gui.onRenameFromClassTree(vc, data, node)));

		this.add(new JScrollPane(this.selector), BorderLayout.CENTER);
	}

	public ClassSelector getClassSelector() {
		return this.selector;
	}
}
