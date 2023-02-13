package cuchaz.enigma.gui.docker.component;

import cuchaz.enigma.gui.docker.Dock;
import cuchaz.enigma.gui.docker.Docker;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.event.MouseEvent;

/**
 * A user-draggable label that is used for docker titles.
 */
public class DockerLabel extends JLabel implements Draggable {
	private JComponent initialParent;
	private Object constraints;
	private boolean inInitialPosition;

	private final Docker docker;

	public DockerLabel(Docker docker, String text) {
		super(text);
		this.setOpaque(false);
		// note: docker and parent are not the same!
		// the parent could be a sub-container of the docker
		this.docker = docker;
		this.initialParent = null;
		this.constraints = null;

		this.addMouseListener(this.getMouseListener());
		this.addMouseMotionListener(this.getMouseMotionListener());
	}

	@Override
	public boolean inInitialPosition() {
		return this.inInitialPosition;
	}

	@Override
	public void setInInitialPosition(boolean inInitialPosition) {
		this.inInitialPosition = inInitialPosition;
	}

	@Override
	public JComponent getInitialParent() {
		return this.initialParent;
	}

	@Override
	public void setInitialParent(JComponent parent) {
		this.initialParent = parent;
	}

	@Override
	public Object getConstraints() {
		return this.constraints;
	}

	@Override
	public void setConstraints(Object constraints) {
		this.constraints = constraints;
	}

	@Override
	public JComponent get() {
		return this;
	}

	@Override
	public void drop(MouseEvent e) {
		Dock.Util.dropDocker(this.docker, e);
	}

	@Override
	public void broadcastMouseEvent(MouseEvent e) {
		Dock.Util.receiveMouseEvent(e);
	}
}
