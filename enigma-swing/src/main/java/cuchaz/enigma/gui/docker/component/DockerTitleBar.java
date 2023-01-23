package cuchaz.enigma.gui.docker.component;

import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.gui.docker.Dock;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.util.function.Supplier;

public class DockerTitleBar extends JPanel {
	private final Supplier<String> titleSupplier;
	private final DockerLabel label;

	public DockerTitleBar(Docker parent, Supplier<String> titleSupplier) {
		super(new BorderLayout());
		this.label = new DockerLabel(parent, titleSupplier.get());
		this.titleSupplier = titleSupplier;
		JButton minimiseButton = new JButton("-");

		minimiseButton.addActionListener(e -> {
			Docker docker = Docker.getDocker(parent.getClass());
			Dock.Util.undock(docker);
		});

		// if we set the left and right margins to 4, the button lines up *really* cutely with the scroll bar in any JScrollPane underneath it
		minimiseButton.setMargin(new Insets(0, 4, 0, 4));

		// set up
		this.add(this.label, BorderLayout.WEST);
		this.add(minimiseButton, BorderLayout.EAST);
		this.label.setConstraints(BorderLayout.WEST);
	}

	public void retranslateUi() {
		this.label.setText(this.titleSupplier.get());
	}
}
