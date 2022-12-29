package cuchaz.enigma.gui.panels.right;

import cuchaz.enigma.gui.Gui;

import javax.swing.JScrollPane;

public class UsersPanel extends MultiplayerOnlyRightPanel {
    public UsersPanel(Gui gui) {
        super(gui);

        JScrollPane userScrollPane = new JScrollPane(gui.getUsers());
        this.add(userScrollPane);
    }

    @Override
    public ButtonPosition getButtonPosition() {
        return ButtonPosition.BOTTOM;
    }

    @Override
    public String getId() {
        return "users";
    }
}