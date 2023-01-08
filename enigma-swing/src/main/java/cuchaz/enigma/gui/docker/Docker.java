package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.I18n;

import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.util.function.Supplier;

public abstract class Docker extends JPanel {
	private final Supplier<String> titleSupplier = () -> I18n.translate("docker." + this.getId() + ".title");
	protected final DockerLabel title;

	protected Location currentLocation = null;

	protected Docker(Gui gui, LayoutManager layout) {
		super(layout);
		this.title = new DockerLabel(gui, this, this.titleSupplier.get());
	}

	public void retranslateUi() {
		this.title.setText(this.titleSupplier.get());
	}

	public void dock(Location location) {
		this.currentLocation = location;
	}

	public boolean isActive() {
		return this.currentLocation != null;
	}

	public Location getCurrentLocation() {
		return this.currentLocation;
	}

	public abstract String getId();

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

	/**
	 * represents all the places a docker can be positioned
	 */
	public enum Location {
		LEFT_FULL,
		LEFT_TOP,
		LEFT_BOTTOM,
		RIGHT_FULL,
		RIGHT_TOP,
		RIGHT_BOTTOM;

		public boolean isLeft() {
			return this.ordinal() < 3;
		}

		public boolean isRight() {
			return this.ordinal() > 2;
		}

		public String getSideName() {
			return this.isLeft() ? "left" : "right";
		}
	}
}
