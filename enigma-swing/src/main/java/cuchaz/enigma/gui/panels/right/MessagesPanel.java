package cuchaz.enigma.gui.panels.right;

import javax.swing.JPanel;

public class MessagesPanel extends AbstractRightPanel {
    private final JPanel panel;

    public MessagesPanel() {
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
        return "messages";
    }
}
