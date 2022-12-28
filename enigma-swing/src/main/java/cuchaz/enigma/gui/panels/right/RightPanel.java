package cuchaz.enigma.gui.panels.right;

import javax.swing.JPanel;
import java.util.HashMap;
import java.util.Map;

public interface RightPanel {
    Map<String, RightPanel> panels = new HashMap<>();

    ButtonPosition getButtonPosition();

    JPanel getPanel();

    String getId();

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
