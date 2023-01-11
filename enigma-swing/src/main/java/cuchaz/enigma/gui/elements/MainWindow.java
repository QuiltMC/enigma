package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.gui.docker.DockerSelector;
import cuchaz.enigma.gui.panels.right.RightAngleLayerUI;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

public class MainWindow {
	private final JFrame frame;
	private final JPanel workArea = new JPanel();

	private final JMenuBar menuBar = new JMenuBar();
	private final StatusBar statusBar = new StatusBar();
	private final DockerSelector rightDockerSelector;
	private final DockerSelector leftDockerSelector;

	public MainWindow(String title) {
		this.rightDockerSelector = new DockerSelector(Docker.Side.RIGHT);
		this.leftDockerSelector = new DockerSelector(Docker.Side.LEFT);

		this.frame = new JFrame(title);
		this.frame.setJMenuBar(this.menuBar);

		Container contentPane = this.frame.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(this.workArea, BorderLayout.CENTER);
		contentPane.add(this.statusBar.getUi(), BorderLayout.SOUTH);
		contentPane.add(this.rightDockerSelector.getPanel(), BorderLayout.EAST);
		contentPane.add(this.leftDockerSelector.getPanel(), BorderLayout.WEST);
	}

	public void setVisible(boolean visible) {
		this.frame.setVisible(visible);
	}

	public DockerSelector getRightDockerSelector() {
		return this.rightDockerSelector;
	}

	public DockerSelector getLeftDockerSelector() {
		return this.leftDockerSelector;
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
