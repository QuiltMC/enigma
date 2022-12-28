package cuchaz.enigma.gui.panels.right;

import javax.swing.JToggleButton;

public abstract class AbstractRightPanel implements RightPanel {
    protected final JToggleButton button;

    protected AbstractRightPanel() {
        this.button = new JToggleButton(this.getId());
    }

    @Override
    public JToggleButton getButton() {
        return this.button;
    }
}
