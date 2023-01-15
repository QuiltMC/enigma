package cuchaz.enigma.gui.docker;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.elements.DeobfPanelPopupMenu;
import cuchaz.enigma.gui.util.GuiUtil;

public class DeobfuscatedClassesDocker extends Docker {
	private final ClassSelector classSelector;
	private final DeobfPanelPopupMenu popupMenu;

	public DeobfuscatedClassesDocker(Gui gui) {
		super(gui);

		this.classSelector = new ClassSelector(gui, ClassSelector.DEOBF_CLASS_COMPARATOR, true);
		this.classSelector.setSelectionListener(gui.getController()::navigateTo);
		this.classSelector.setRenameSelectionListener(gui::onRenameFromClassTree);
		this.popupMenu = new DeobfPanelPopupMenu(this);

		this.setLayout(new BorderLayout());
		this.add(this.title, BorderLayout.NORTH);
		this.add(new JScrollPane(this.classSelector), BorderLayout.CENTER);

		this.classSelector.addMouseListener(GuiUtil.onMousePress(this::onPress));

		this.retranslateUi();
	}

	private void onPress(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			classSelector.setSelectionRow(classSelector.getClosestRowForLocation(e.getX(), e.getY()));
			int i = classSelector.getRowForPath(classSelector.getSelectionPath());
			if (i != -1) {
				popupMenu.show(classSelector, e.getX(), e.getY());
			}
		}
	}

	public ClassSelector getClassSelector() {
		return this.classSelector;
	}

	@Override
	public void retranslateUi() {
		super.retranslateUi();
		this.popupMenu.retranslateUi();
	}

	@Override
	public String getId() {
		return Type.DEOBFUSCATED_CLASSES;
	}

	@Override
	public Location getButtonPosition() {
		return new Location(Side.LEFT, VerticalLocation.BOTTOM);
	}

	@Override
	public Location getPreferredLocation() {
		return new Location(Side.LEFT, VerticalLocation.BOTTOM);
	}
}
