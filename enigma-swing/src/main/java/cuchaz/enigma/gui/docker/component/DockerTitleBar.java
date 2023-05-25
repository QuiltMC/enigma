package cuchaz.enigma.gui.docker.component;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.gui.docker.Dock;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.Objects;
import java.util.function.Supplier;

public class DockerTitleBar extends JPanel {
	private static final String SPLIT_ICON = "◫";
	private static final String FULLSCREEN_ICON = "□";

	private final Supplier<String> titleSupplier;
	private final DockerLabel label;
	private final JButton resizeButton;

	public DockerTitleBar(Gui gui, Docker parent, Supplier<String> titleSupplier) {
		super(new BorderLayout(0, 0));
		this.label = new DockerLabel(parent, titleSupplier.get());
		this.titleSupplier = titleSupplier;
		this.resizeButton = new JButton(SPLIT_ICON);

		this.resizeButton.addActionListener(e -> {
			boolean split = this.resizeButton.getText().equals(SPLIT_ICON);

			// since the button can only be pressed on a visible docker, we can assume the location will always be found
			Docker.VerticalLocation location = split ? Docker.VerticalLocation.TOP : Docker.VerticalLocation.FULL;
			Objects.requireNonNull(Dock.Util.findDock(parent)).host(parent, location, false);
			this.updateResizeButton(location);
		});

		JButton minimiseButton = new JButton("-");

		minimiseButton.addActionListener(e -> {
			Docker docker = gui.getDockerManager().getDocker(parent.getClass());
			Dock.Util.undock(docker);
		});

		// if we set the left and right margins to 4, the button lines up *really* cutely with the scroll bar in any JScrollPane underneath it
		minimiseButton.setMargin(new Insets(0, 4, 0, 4));
		this.resizeButton.setMargin(new Insets(0, 4, 0, 4));

		// by default, flow layout applies a 5px gap between components
		// we want to avoid this vertically to make sure the title bar is as compact as possible
		FlowLayout layout = new FlowLayout(FlowLayout.LEFT, 0, 0);
		JPanel buttons = new JPanel(layout);
		buttons.add(this.resizeButton);
		buttons.add(minimiseButton);

		// set up
		this.add(this.label, BorderLayout.WEST);
		this.add(buttons, BorderLayout.EAST);
		this.label.setConstraints(BorderLayout.WEST);
	}

	public void updateResizeButton(Docker.VerticalLocation location) {
		this.resizeButton.setText(location == Docker.VerticalLocation.FULL ? SPLIT_ICON : FULLSCREEN_ICON);
	}

	public void retranslateUi() {
		this.label.setText(this.titleSupplier.get());
	}
}
