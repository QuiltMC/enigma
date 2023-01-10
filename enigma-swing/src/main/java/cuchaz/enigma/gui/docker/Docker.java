package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.I18n;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class Docker extends JPanel {
	private static final Map<Class<? extends Docker>, Docker> dockers = new LinkedHashMap<>();
	private static final Map<String, Class<? extends Docker>> dockerClasses = new HashMap<>();

	protected final Supplier<String> titleSupplier = () -> I18n.translate("docker." + this.getId() + ".title");
	protected final DockerLabel title;
	protected final JToggleButton button;
	protected final Gui gui;

	protected Height currentLocation = null;
	protected Side side = null;

	protected Docker(Gui gui) {
		super(new BorderLayout());
		this.gui = gui;
		this.title = new DockerLabel(gui, this, this.titleSupplier.get());
		this.button = new JToggleButton(this.titleSupplier.get());
		this.button.addActionListener(e -> gui.openDocker(this.getClass(), true));
	}

	public void retranslateUi() {
		String translatedTitle = this.titleSupplier.get();
		this.button.setText(translatedTitle);
		this.title.setText(translatedTitle);
	}

	public void dock(Side side, Height location) {
		this.currentLocation = location;
		this.side = side;
	}

	public boolean isActive() {
		return this.currentLocation != null;
	}

	public Height getCurrentLocation() {
		return this.currentLocation;
	}

	public Side getCurrentSide() {
		return this.side;
	}

	public JToggleButton getButton() {
		return this.button;
	}

	public abstract String getId();

	public abstract Docker.ButtonPosition getButtonPosition();

	/**
	 * dictates where the panel will open when the user clicks its button
	 * @return an {@link Location} representing the preferred position
	 */
	public abstract Location getPreferredLocation();

	@Override
	public void setVisible(boolean visible) {
		if (!visible) {
			this.currentLocation = null;
		}

		this.getButton().setSelected(visible);
	}

	public static void addDocker(Docker panel) {
		dockers.put(panel.getClass(), panel);
		dockerClasses.put(panel.getId(), panel.getClass());
	}

	@SuppressWarnings("unchecked")
	public static <T extends Docker> T getDocker(Class<T> clazz) {
		Docker panel = dockers.get(clazz);
		if (panel != null) {
			return (T) dockers.get(clazz);
		} else {
			throw new IllegalArgumentException("no panel registered for class " + clazz);
		}
	}

	public static Docker getDocker(String id) {
		if (!dockerClasses.containsKey(id)) {
			throw new IllegalArgumentException("no panel registered for id " + id);
		}

		return getDocker(dockerClasses.get(id));
	}

	public static Map<Class<? extends Docker>, Docker> getDockers() {
		return dockers;
	}

	public static Map<String, Class<? extends Docker>> getDockerClasses() {
		return dockerClasses;
	}

	/**
	 * contains the IDs for all existing dockers
	 */
	public static final class Type {
		public static final String STRUCTURE = "structure";
		public static final String INHERITANCE = "inheritance";
		public static final String CALLS = "calls";
		public static final String IMPLEMENTATIONS = "implementations";
		public static final String COLLAB = "collab";
		public static final String DEOBFUSCATED_CLASSES = "deobfuscated_classes";
		public static final String OBFUSCATED_CLASSES = "obfuscated_classes";
	}

	/**
	 * represents the position of a docker's button on the selector panels
	 */
	public enum ButtonPosition {
		RIGHT_TOP,
		RIGHT_BOTTOM,
		LEFT_TOP,
		LEFT_BOTTOM
	}

	public record Location(Side side, Height height) {

	}

	public enum Side {
		LEFT,
		RIGHT
	}

	public enum Height {
		TOP,
		BOTTOM,
		FULL
	}
}
