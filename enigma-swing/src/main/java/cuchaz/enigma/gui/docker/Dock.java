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

	public Dock() {
		super(new BorderLayout());
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		// we can rely on paint to always be called when the label is being dragged over the docker
		if (this.hovered) {
			Color color = new Color(0, 0, 255, 84);
			g.setColor(color);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			this.repaint(new Rectangle(0, 0, this.getWidth(), this.getHeight()));
		}
	}

	public void receiveMouseEvent(MouseEvent e) {
		if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
			Rectangle screenBounds = new Rectangle(this.getLocationOnScreen().x, this.getLocationOnScreen().y, this.getWidth(), this.getHeight());

			if (!hovered && contains(screenBounds, e.getLocationOnScreen())) {
				this.repaint(new Rectangle(0, 0, this.getWidth(), this.getHeight()));
				this.hovered = true;
			} else if (!contains(screenBounds, e.getLocationOnScreen())) {
				this.hovered = false;
			}
		}
	}

	private boolean contains(Rectangle rectangle, Point point) {
		return (point.x >= rectangle.x && point.x <= rectangle.x + rectangle.width)
				&& (point.y >= rectangle.y && point.y <= rectangle.y + rectangle.height);
	}
}
