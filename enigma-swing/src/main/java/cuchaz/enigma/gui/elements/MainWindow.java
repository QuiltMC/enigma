package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.gui.docker.component.DockerSelector;

import java.awt.BorderLayout;
import java.awt.Container;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

public class MainWindow {
	private final JFrame frame;
	private final JPanel workArea = new JPanel();

	private final JMenuBar menuBar = new JMenuBar();
	private final StatusBar statusBar = new StatusBar();
	private final DockerSelector rightDockerSelector;
	private final DockerSelector leftDockerSelector;

	public MainWindow(Gui gui, String title) {
		this.rightDockerSelector = new DockerSelector(gui.getDockerManager(), Docker.Side.RIGHT);
		this.leftDockerSelector = new DockerSelector(gui.getDockerManager(), Docker.Side.LEFT);

		this.frame = new JFrame(title);
		this.frame.setJMenuBar(this.menuBar);

		Container contentPane = this.frame.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(this.workArea, BorderLayout.CENTER);
		contentPane.add(this.statusBar.getUi(), BorderLayout.SOUTH);
		contentPane.add(this.rightDockerSelector, BorderLayout.EAST);
		contentPane.add(this.leftDockerSelector, BorderLayout.WEST);
	}

	public void setVisible(boolean visible) {
		this.frame.setVisible(visible);
	}

	public DockerSelector getDockerSelector(Docker.Side side) {
		return side == Docker.Side.LEFT ? this.leftDockerSelector : this.rightDockerSelector;
	}

	public JMenuBar getMenuBar() {
		return this.menuBar;
	}

	public StatusBar getStatusBar() {
		return this.statusBar;
	}

	public Container getWorkArea() {
		return this.workArea;
	}

	public JFrame getFrame() {
		return this.frame;
	}

	public void setTitle(String title) {
		this.frame.setTitle(title);
	}
}
