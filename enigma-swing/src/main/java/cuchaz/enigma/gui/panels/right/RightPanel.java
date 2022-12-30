package cuchaz.enigma.gui.panels.right;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.I18n;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class RightPanel extends JPanel {
    public static final String DEFAULT = "structure";
    private static final Map<String, RightPanel> panels = new HashMap<>();

    protected final JToggleButton button;
    private final Supplier<String> buttonTextProvider = () -> I18n.translate("right_panel.selector." + this.getId() + "_button");

    protected RightPanel(Gui gui) {
        super(new BorderLayout());
        this.button = new JToggleButton(buttonTextProvider.get());
        this.button.addActionListener(e -> gui.setRightPanel(this.getId()));
    }

    public abstract RightPanel.ButtonPosition getButtonPosition();

    public abstract String getId();

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        this.getButton().setSelected(visible);
    }

    public void retranslateUi() {
        this.button.setText(buttonTextProvider.get());
    }

    public JToggleButton getButton() {
        return this.button;
    }

    public static void registerPanel(RightPanel panel) {
        panels.put(panel.getId(), panel);
    }

    public static RightPanel getPanel(String id) {
        return panels.get(id);
    }

    public static Map<String, RightPanel> getRightPanels() {
        return panels;
    }

    public enum ButtonPosition {
        TOP,
        BOTTOM
    }
}
