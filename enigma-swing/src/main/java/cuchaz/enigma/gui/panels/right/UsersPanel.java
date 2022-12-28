package cuchaz.enigma.gui.panels.right;

import javax.swing.JPanel;

public class UsersPanel extends AbstractRightPanel {
    private final JPanel panel;

    public UsersPanel() {
        this.panel = new JPanel();
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