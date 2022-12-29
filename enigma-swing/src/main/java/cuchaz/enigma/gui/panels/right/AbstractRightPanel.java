package cuchaz.enigma.gui.panels.right;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.I18n;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.util.function.Supplier;

public abstract class AbstractRightPanel extends JPanel implements RightPanel {
    protected final JToggleButton button;
    private final Supplier<String> buttonTextProvider = () -> I18n.translate("right_panel.selector." + this.getId() + "_button");

    protected AbstractRightPanel(Gui gui) {
        super(new BorderLayout());
        this.button = new JToggleButton(buttonTextProvider.get());
        this.button.addActionListener(e -> {
            RightPanel currentPanel = gui.getRightPanel();
            RightPanel newPanel = RightPanel.getPanel(this.getId());

            if (currentPanel.getId().equals(newPanel.getId())) {
                boolean visible = !currentPanel.getPanel().isVisible();

                currentPanel.getPanel().setVisible(visible);
                // todo maybe move right panels entirely to an abstract class instead of an interface?
                currentPanel.getButton().setSelected(visible);
            } else {
                gui.setRightPanel(this.getId());
                newPanel.getButton().setSelected(true);
                currentPanel.getButton().setSelected(false);
            }
        });
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        this.getButton().setSelected(visible);
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
