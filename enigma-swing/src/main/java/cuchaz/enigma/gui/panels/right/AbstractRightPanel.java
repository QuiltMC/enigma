package cuchaz.enigma.gui.panels.right;

import cuchaz.enigma.utils.I18n;

import javax.swing.JToggleButton;
import java.util.function.Supplier;

public abstract class AbstractRightPanel implements RightPanel {
    protected final JToggleButton button;
    private final Supplier<String> buttonTextProvider = () -> I18n.translate("right_panel.selector." + this.getId() + "_button");

    protected AbstractRightPanel() {
        this.button = new JToggleButton(buttonTextProvider.get());
    }

    @Override
    public void retranslateUi() {
        this.button.setText(buttonTextProvider.get());
    }

    @Override
    public JToggleButton getButton() {
        return this.button;
    }
}
