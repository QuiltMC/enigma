package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.docker.component.DockerButton;
import cuchaz.enigma.gui.docker.component.DockerTitleBar;
import cuchaz.enigma.utils.I18n;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Represents a window that can be docked on the sides of the editor panel.
 * <br> A docker is an instance of {@link JPanel} that uses a {@link BorderLayout} by default.
 */
public abstract class Docker extends JPanel {
	protected final Supplier<String> titleSupplier = () -> I18n.translate("docker." + this.getId() + ".title");
	protected final DockerTitleBar title;
	protected final DockerButton button;
	protected final Gui gui;

	protected Docker(Gui gui) {
		super(new BorderLayout());
		this.gui = gui;
		this.title = new DockerTitleBar(gui, this, this.titleSupplier);
		this.button = new DockerButton(this, this.titleSupplier, this.getButtonLocation().side);
		// add action listener to open and close the docker when its button is pressed
		this.button.addActionListener(e -> {
			Docker docker = gui.getDockerManager().getDocker(this.getClass());

			if (Dock.Util.isDocked(docker)) {
				Dock.Util.undock(docker);
			} else {
				gui.openDocker(this.getClass());
			}
		});

		this.add(this.title, BorderLayout.NORTH);

		// validate to prevent difficult-to-trace errors
		if (this.getButtonLocation().verticalLocation == VerticalLocation.FULL) {
			throw new IllegalStateException("docker button vertical location cannot be full! allowed values are top and bottom.");
		}
	}

	public void retranslateUi() {
		this.button.repaint();
		this.title.retranslateUi();
	}

	public DockerTitleBar getTitleBar() {
		return this.title;
	}

	/**
	 * @return the side panel button that opens and closes this docker
	 */
	public DockerButton getButton() {
		return this.button;
	}

	/**
	 * @return an ID used to check equality and save docker information to the config
	 */
	public abstract String getId();

	/**
	 * @return the position of the docker's button in the selector panels. this also represents where the docker will open when its button is clicked cannot use {@link Docker.VerticalLocation#FULL}
	 */
	public final Location getButtonLocation() {
		return UiConfig.getButtonLocation(this);
	}

	public abstract Location getPreferredButtonLocation();

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		this.getButton().setSelected(visible);
		// repaint to avoid corruption in the docker selectors
		this.gui.getMainWindow().getFrame().getContentPane().repaint();
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

	/**
	 * Represents the location of a docker on the screen.
	 * @param side the side of the screen, either right or left
	 * @param verticalLocation the vertical location of the docker, being full, top or bottom
	 */
	public record Location(Side side, VerticalLocation verticalLocation) {
		public static Location parse(String string) {
			String[] parts = string.split(UiConfig.PAIR_SEPARATOR);
			return new Location(Side.valueOf(parts[0].toUpperCase()), VerticalLocation.valueOf(parts[1].toUpperCase()));
		}

		@Override
		public String toString() {
			return this.side.name().toLowerCase() + UiConfig.PAIR_SEPARATOR + this.verticalLocation.name().toLowerCase();
		}
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
