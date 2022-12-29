package cuchaz.enigma.gui.panels.right;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.util.HashMap;
import java.util.Map;

public interface RightPanel {
    // todo right panels sometimes forget their size when hidden
    String DEFAULT = "structure";
    Map<String, RightPanel> panels = new HashMap<>();

    ButtonPosition getButtonPosition();

    JPanel getPanel();

    String getId();

    JToggleButton getButton();

    void retranslateUi();

    static void registerPanel(RightPanel panel) {
        panels.put(panel.getId(), panel);
    }

    static RightPanel getPanel(String id) {
        return panels.get(id);
    }

    enum ButtonPosition {
        TOP,
        BOTTOM
    }
}
