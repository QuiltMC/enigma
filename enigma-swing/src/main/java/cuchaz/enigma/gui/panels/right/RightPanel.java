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
	private static final Map<Class<? extends RightPanel>, RightPanel> panels = new HashMap<>();
	private static final Map<String, Class<? extends RightPanel>> panelClasses = new HashMap<>();

	protected final Gui gui;
	protected final JToggleButton button;
	protected final JLabel title;
	protected final Supplier<String> titleProvider = () -> I18n.translate("right_panel." + this.getId() + ".title");

	protected RightPanel(Gui gui) {
		super(new BorderLayout());
		this.gui = gui;
		this.button = new JToggleButton(this.titleProvider.get());
		this.button.addActionListener(e -> gui.setRightPanel(this.getClass(), true));
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

	public static void addPanel(RightPanel panel) {
		panels.put(panel.getClass(), panel);
		panelClasses.put(panel.getId(), panel.getClass());
	}

	@SuppressWarnings("unchecked")
	public static <T extends RightPanel> T getPanel(Class<T> clazz) {
		RightPanel panel = panels.get(clazz);
		if (panel != null) {
			return (T) panels.get(clazz);
		} else {
			throw new IllegalArgumentException("no panel registered for class " + clazz);
		}
	}

	public static RightPanel getPanel(String id) {
		if (!panelClasses.containsKey(id)) {
			throw new IllegalArgumentException("no panel registered for id " + id);
		}

		return getPanel(panelClasses.get(id));
	}

	public static Map<Class<? extends RightPanel>, RightPanel> getRightPanels() {
		return panels;
	}

	public static final class Type {
		public static final String STRUCTURE = "structure";
		public static final String INHERITANCE = "inheritance";
		public static final String CALLS = "calls";
		public static final String IMPLEMENTATIONS = "implementations";
		public static final String MULTIPLAYER = "multiplayer";
	}

	public enum ButtonPosition {
		TOP,
		BOTTOM
	}
}
