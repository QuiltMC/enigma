package cuchaz.enigma.gui.panels.right;

import cuchaz.enigma.gui.Gui;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class UsersPanel extends AbstractRightPanel {
    private final JPanel panel;

    public UsersPanel(Gui gui) {
        super(gui);
        this.panel = new JPanel();
        JScrollPane userScrollPane = new JScrollPane(gui.getUsers());

        this.panel.add(userScrollPane);
    }

    @Override
    public ButtonPosition getButtonPosition() {
        return ButtonPosition.BOTTOM;
    }

    @Override
    public JPanel getPanel() {
        return this.panel;
    }

    @Override
    public String getId() {
        return "users";
    }
}