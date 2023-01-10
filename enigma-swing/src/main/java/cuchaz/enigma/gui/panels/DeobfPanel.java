package cuchaz.enigma.gui.panels;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.gui.elements.DeobfPanelPopupMenu;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.utils.I18n;

public class DeobfPanel extends Docker {
	public final ClassSelector deobfClasses;
	public final DeobfPanelPopupMenu deobfPanelPopupMenu;

	public DeobfPanel(Gui gui) {
		super(gui);

		this.deobfClasses = new ClassSelector(gui, ClassSelector.DEOBF_CLASS_COMPARATOR, true);
		this.deobfClasses.setSelectionListener(gui.getController()::navigateTo);
		this.deobfClasses.setRenameSelectionListener(gui::onRenameFromClassTree);
		this.deobfPanelPopupMenu = new DeobfPanelPopupMenu(this);

		this.setLayout(new BorderLayout());
		this.add(this.title, BorderLayout.NORTH);
		this.title.setConstraints(BorderLayout.NORTH);
		this.add(new JScrollPane(this.deobfClasses), BorderLayout.CENTER);

		this.deobfClasses.addMouseListener(GuiUtil.onMousePress(this::onPress));

		this.retranslateUi();
	}

	private void onPress(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			deobfClasses.setSelectionRow(deobfClasses.getClosestRowForLocation(e.getX(), e.getY()));
			int i = deobfClasses.getRowForPath(deobfClasses.getSelectionPath());
			if (i != -1) {
				deobfPanelPopupMenu.show(deobfClasses, e.getX(), e.getY());
			}
		}
	}

	@Override
	public void retranslateUi() {
		super.retranslateUi();
		this.title.setText(I18n.translate(gui.isSingleClassTree() ? "info_panel.classes" : "info_panel.classes.deobfuscated"));
		this.deobfPanelPopupMenu.retranslateUi();
	}

	@Override
	public String getId() {
		// todo
		return Type.DEOBFUSCATED_CLASSES;
	}

	@Override
	public ButtonPosition getButtonPosition() {
		return ButtonPosition.LEFT_BOTTOM;
	}

	@Override
	public Location getPreferredLocation() {
		return new Location(Side.LEFT, Height.BOTTOM);
	}
}
