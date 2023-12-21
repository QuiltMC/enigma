package org.quiltmc.enigma.gui.docker;

import org.quiltmc.config.api.values.ComplexConfigValue;
import org.quiltmc.config.api.values.ConfigSerializableObject;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.docker.component.DockerButton;
import org.quiltmc.enigma.gui.docker.component.DockerTitleBar;
import org.quiltmc.enigma.util.I18n;

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
		Location savedLocation = Config.dockers().getButtonLocation(this.getId());
		return savedLocation == null ? this.getPreferredButtonLocation() : savedLocation;
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
	public record Location(Side side, VerticalLocation verticalLocation) implements ConfigSerializableObject<String> {
		@Override
		public String toString() {
			return this.side.name() + ";" + this.verticalLocation.name();
		}

		@Override
		public ConfigSerializableObject<String> convertFrom(String representation) {
			return new Location(Side.valueOf(representation.split(";")[0]), VerticalLocation.valueOf(representation.split(";")[1]));
		}

		@Override
		public String getRepresentation() {
			return this.toString();
		}

		@Override
		public ComplexConfigValue copy() {
			return this;
		}
	}

	/**
	 * Represents the side of the screen a docker is located on.
	 * @implNote these names cannot be changed without breaking configurations
	 */
	public enum Side {
		LEFT,
		RIGHT
	}

	/**
	 * Represents the occupied vertical location of a docker.
	 * @implNote these names cannot be changed without breaking configurations
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
