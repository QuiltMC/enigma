package cuchaz.enigma.gui.panels.right;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class UsersPanel extends JPanel implements RightPanel {
    private final JToggleButton button;

    public UsersPanel() {
        this.button = new JToggleButton(this.getId());
    }

    @Override
    public ButtonPosition getButtonPosition() {
        return ButtonPosition.BOTTOM;
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public String getId() {
        return "users";
    }

    @Override
    public JToggleButton getButton() {
        return this.button;
    }
}