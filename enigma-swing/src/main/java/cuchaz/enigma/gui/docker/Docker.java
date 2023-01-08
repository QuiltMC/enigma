package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.utils.I18n;

import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.util.function.Supplier;

public abstract class Docker extends JPanel {
	private final Supplier<String> titleSupplier = () -> I18n.translate("docker." + this.getId() + ".title");

	protected final DockerLabel title;

	protected Docker(Gui gui, LayoutManager layout) {
		super(layout);
		this.title = new DockerLabel(gui, this, this.titleSupplier.get());
	}

	public void retranslateUi() {
		this.title.setText(this.titleSupplier.get());
	}

	public abstract String getId();
}
