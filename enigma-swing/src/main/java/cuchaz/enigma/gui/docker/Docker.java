package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.docker.component.DockerTitleBar;
import cuchaz.enigma.utils.I18n;

import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Represents a window that can be docked on the sides of the editor panel.
 * <br> A docker is an instance of {@link JPanel} that uses a {@link BorderLayout} by default.
 */
public abstract class Docker extends JPanel {
	private static final Map<Class<? extends Docker>, Docker> dockers = new LinkedHashMap<>();
	private static final Map<String, Class<? extends Docker>> dockerClasses = new HashMap<>();

	protected final Supplier<String> titleSupplier = () -> I18n.translate("docker." + this.getId() + ".title");
	protected final DockerTitleBar title;
	protected final JToggleButton button;
	protected final Gui gui;

	protected Docker(Gui gui) {
		super(new BorderLayout());
		this.gui = gui;
		this.title = new DockerTitleBar(this, this.titleSupplier);
		this.button = new JToggleButton(this.titleSupplier.get());
		// add action listener to open and close the docker when its button is pressed
		this.button.addActionListener(e -> {
			Docker docker = getDocker(this.getClass());

			if (Dock.Util.isDocked(docker)) {
				Dock.Util.undock(docker);
			} else {
				gui.openDocker(this.getClass());
			}
		});

		this.add(this.title, BorderLayout.NORTH);

		// validate to prevent difficult-to-trace errors
		if (this.getButtonPosition().verticalLocation == VerticalLocation.FULL) {
			throw new IllegalStateException("docker button vertical location cannot be full! allowed values are top and bottom.");
		}
	}

	public void retranslateUi() {
		String translatedTitle = this.titleSupplier.get();
		this.button.setText(translatedTitle);
		this.title.retranslateUi();
	}

	/**
	 * @return the side panel button that opens and closes this docker
	 */
	public JToggleButton getButton() {
		return this.button;
	}

	/**
	 * @return an ID used to check equality and save docker information to the config
	 */
	public abstract String getId();

	/**
	 * @return the position of the docker's button in the selector panels. cannot use {@link Docker.VerticalLocation#FULL}
	 */
	public abstract Docker.Location getButtonPosition();

	/**
	 * @return an {@link Location} representing this docker's preferred position: where the panel will open when the user clicks its button
	 */
	public abstract Location getPreferredLocation();

	@Override
	public void setVisible(boolean visible) {
		this.getButton().setSelected(visible);
	}

	@Override
	public boolean equals(Object obj) {
		// there should only be one instance of each docker, so we only check ID here
		if (obj instanceof Docker docker) {
			return docker.getId().equals(this.getId());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getId());
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
			throw new IllegalArgumentException("no docker registered for class " + clazz);
		}
	}

	public static Docker getDocker(String id) {
		if (!dockerClasses.containsKey(id)) {
			throw new IllegalArgumentException("no docker registered for id " + id);
		}

		return getDocker(dockerClasses.get(id));
	}

	public static Map<Class<? extends Docker>, Docker> getDockers() {
		return dockers;
	}

	/**
	 * Contains the IDs for all existing dockers.
	 */
	public static final class Type {
		public static final String STRUCTURE = "structure";
		public static final String INHERITANCE = "inheritance";
		public static final String CALLS = "calls";
		public static final String IMPLEMENTATIONS = "implementations";
		public static final String COLLAB = "collab";
		public static final String ALL_CLASSES = "all_classes";
		public static final String DEOBFUSCATED_CLASSES = "deobfuscated_classes";
		public static final String OBFUSCATED_CLASSES = "obfuscated_classes";
	}

	/**
	 * Represents the location of a docker on the screen.
	 * @param side the side of the screen, either right or left
	 * @param verticalLocation the vertical location of the docker, being full, top or bottom
	 */
	public record Location(Side side, VerticalLocation verticalLocation) {

	}

	/**
	 * Represents the side of the screen a docker is located on.
	 */
	public enum Side {
		LEFT,
		RIGHT
	}

	/**
	 * Represents the occupied vertical location of a docker.
	 */
	public enum VerticalLocation {
		TOP,
		BOTTOM,
		FULL;

		public VerticalLocation inverse() {
			if (this == FULL) {
				throw new IllegalStateException("cannot invert vertical location \"" + this.name() + "\"");
			}

			return this == TOP ? BOTTOM : TOP;
		}
	}
}
