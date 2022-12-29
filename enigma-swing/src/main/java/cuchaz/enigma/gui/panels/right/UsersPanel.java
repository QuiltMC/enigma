package cuchaz.enigma.gui.panels.right;

import cuchaz.enigma.gui.Gui;

import javax.swing.JScrollPane;

public class UsersPanel extends MultiplayerOnlyRightPanel {
    private final JScrollPane userScrollPane;

    public UsersPanel(Gui gui) {
        super(gui);
        this.userScrollPane = new JScrollPane(gui.getUsers());

        // set state
        this.setUp(!gui.isOffline());
    }

    @Override
    void addComponents() {
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