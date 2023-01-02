package cuchaz.enigma.gui.panels.right;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.I18n;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class RightPanel extends JPanel {
    public static final String DEFAULT = Type.STRUCTURE;
    private static final Map<String, RightPanel> panels = new HashMap<>();

	protected final Gui gui;
    protected final JToggleButton button;
	protected final JLabel title;
	protected final Supplier<String> titleProvider = () -> I18n.translate("right_panel." + this.getId() + ".title");

    protected RightPanel(Gui gui) {
        super(new BorderLayout());
		this.gui = gui;
        this.button = new JToggleButton(this.titleProvider.get());
        this.button.addActionListener(e -> gui.setRightPanel(this.getId(), true));
		this.title = new JLabel(this.titleProvider.get());
		this.add(this.title, BorderLayout.NORTH);
    }

    public abstract RightPanel.ButtonPosition getButtonPosition();

    public abstract String getId();

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        this.getButton().setSelected(visible);
    }

    public void retranslateUi() {
		String translatedTitle = this.titleProvider.get();
        this.button.setText(translatedTitle);
		this.title.setText(translatedTitle);
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

	public static final class Type {
		public static final String STRUCTURE = "structure";
		public static final String INHERITANCE = "inheritance";
		public static final String CALLS = "calls";
		public static final String IMPLEMENTATIONS = "implementations";
	}

    public enum ButtonPosition {
        TOP,
        BOTTOM
    }
}
