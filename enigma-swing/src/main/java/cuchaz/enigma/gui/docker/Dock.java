package cuchaz.enigma.gui.docker;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

public class Dock extends JPanel {
	private boolean hovered = false;
	private Docker hostedDocker;

	public Dock() {
		super(new BorderLayout());
		this.hostedDocker = null;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		// we can rely on paint to always be called when the label is being dragged over the docker
		if (this.hovered) {
			Color color = new Color(0, 0, 255, 84);
			g.setColor(color);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			this.repaint();
		}
	}

	public void receiveMouseEvent(MouseEvent e) {
		if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
			if (!hovered && containsMouse(e)) {
				this.repaint();
				this.hovered = true;
			} else if (!containsMouse(e)) {
				this.hovered = false;
			}
		} else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
			this.hovered = false;
			this.repaint();
		}
	}

	public boolean containsMouse(MouseEvent e) {
		Rectangle screenBounds = new Rectangle(this.getLocationOnScreen().x, this.getLocationOnScreen().y, this.getWidth(), this.getHeight());
		return contains(screenBounds, e.getLocationOnScreen());
	}

	public void setHostedDocker(Docker docker) {
		// remove old docker
		if (this.hostedDocker != null) {
			this.remove(this.hostedDocker);
		}

		this.hostedDocker = docker;
		this.setUpDocker();
	}

	public void setUpDocker() {
		if (this.hostedDocker == null) {
			throw new IllegalStateException("cannot refresh a dock that has no docker!");
		} else {
			// add new docker and revalidate to paint properly
			this.add(this.hostedDocker);
			this.revalidate();
		}
	}

	private boolean contains(Rectangle rectangle, Point point) {
		return (point.x >= rectangle.x && point.x <= rectangle.x + rectangle.width)
				&& (point.y >= rectangle.y && point.y <= rectangle.y + rectangle.height);
	}
}
