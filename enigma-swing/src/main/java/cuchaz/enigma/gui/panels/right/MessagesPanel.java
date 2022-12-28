package cuchaz.enigma.gui.panels.right;

import javax.swing.JPanel;

public class MessagesPanel extends JPanel implements RightPanel {
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
}
