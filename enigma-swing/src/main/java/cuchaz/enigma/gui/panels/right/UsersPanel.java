package cuchaz.enigma.gui.panels.right;

import cuchaz.enigma.gui.Gui;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

public class UsersPanel extends MultiplayerOnlyRightPanel {
	private final JPanel topPanel;

    public UsersPanel(Gui gui) {
        super(gui);
		JScrollPane userScrollPane = new JScrollPane(gui.getUsers());
		this.topPanel = new JPanel(new BorderLayout());

		this.topPanel.add(this.title, BorderLayout.NORTH);
		this.topPanel.add(userScrollPane, BorderLayout.SOUTH);

		// set online state
        this.setUp(!gui.isOffline());
    }

    @Override
	protected void addComponents() {
        this.add(this.topPanel, BorderLayout.NORTH);
    }

    @Override
    public ButtonPosition getButtonPosition() {
        return ButtonPosition.BOTTOM;
    }

    @Override
    public String getId() {
        return Type.USERS;
    }
}
