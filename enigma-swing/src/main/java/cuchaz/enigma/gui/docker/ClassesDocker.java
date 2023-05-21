package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.elements.ClassSelectorPopupMenu;
import cuchaz.enigma.gui.util.GuiUtil;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

public abstract class ClassesDocker extends Docker {
	protected final ClassSelectorPopupMenu popupMenu;
	protected final ClassSelector selector;

	protected ClassesDocker(Gui gui, ClassSelector selector) {
		super(gui);
		this.selector = selector;
		this.selector.addMouseListener(GuiUtil.onMousePress(this::onPress));
		this.selector.setSelectionListener(gui.getController()::navigateTo);
		this.popupMenu = new ClassSelectorPopupMenu(gui, this);

		this.add(new JScrollPane(this.selector), BorderLayout.CENTER);
	}

	private void onPress(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			this.selector.setSelectionRow(this.selector.getClosestRowForLocation(e.getX(), e.getY()));
			int i = this.selector.getRowForPath(this.selector.getSelectionPath());
			if (i != -1) {
				this.popupMenu.show(this.selector, e.getX(), e.getY());
			}
		}
	}

	public ClassSelector getClassSelector() {
		return this.selector;
	}

	public ClassSelectorPopupMenu getPopupMenu() {
		return this.popupMenu;
	}

	@Override
	public void retranslateUi() {
		super.retranslateUi();
		this.popupMenu.retranslateUi();
	}
}
