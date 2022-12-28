package cuchaz.enigma.gui.panels.right;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class MessagesPanel extends JPanel implements RightPanel {
    private final JToggleButton button;

    public MessagesPanel() {
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
        return "messages";
    }

    @Override
    public JToggleButton getButton() {
        return this.button;
    }
}
