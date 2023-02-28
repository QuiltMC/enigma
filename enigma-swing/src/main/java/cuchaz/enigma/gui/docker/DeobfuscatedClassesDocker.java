package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.elements.DeobfPanelPopupMenu;
import cuchaz.enigma.gui.util.GuiUtil;

import javax.swing.SwingUtilities;
import java.awt.event.MouseEvent;

public class DeobfuscatedClassesDocker extends ClassesDocker {
	private final DeobfPanelPopupMenu popupMenu;

	public DeobfuscatedClassesDocker(Gui gui) {
		super(gui, new ClassSelector(gui, ClassSelector.DEOBF_CLASS_COMPARATOR, true));

		this.popupMenu = new DeobfPanelPopupMenu(this);
		this.selector.addMouseListener(GuiUtil.onMousePress(this::onPress));

		this.retranslateUi();
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

	@Override
	public void retranslateUi() {
		super.retranslateUi();
		this.popupMenu.retranslateUi();
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
