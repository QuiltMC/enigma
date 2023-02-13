package cuchaz.enigma.gui.docker.component;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public interface Draggable {
	boolean inInitialPosition();
	void setInInitialPosition(boolean inInitialPosition);
	JComponent getInitialParent();
	void setInitialParent(JComponent parent);
	Object getConstraints();
	void setConstraints(Object constraints);
	JComponent get();
	// todo click timeouts!!!

	/**
	 * Drops the draggable in its new position if possible.
	 * @param e the mouse event that triggered the drop, provides position information
	 */
	void drop(MouseEvent e);

	/**
	 * Broadcasts a mouse event to be used for highlighting.
	 * @param e the mouse event that triggered highlighting, provides position information
	 */
	void broadcastMouseEvent(MouseEvent e);

	default MouseListener getMouseListener() {
		return new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// no-op
			}

			@Override
			public void mousePressed(MouseEvent e) {
				Draggable.this.setInInitialPosition(true);

				// save parent for re-addition after dragging is finished
				Draggable.this.setInitialParent((JComponent) Draggable.this.get().getParent());

				// validate
				Draggable.this.ensureConfigured();

				// configure object to be on the glass pane instead of its former pane
				Draggable.this.get().setVisible(false);
				JRootPane rootPane = Draggable.this.get().getRootPane();
				JPanel glassPane = (JPanel) rootPane.getGlassPane();
				Draggable.this.getInitialParent().remove(Draggable.this.get());
				glassPane.add(Draggable.this.get());

				// repaint former panel to display removal of element
				Draggable.this.getInitialParent().repaint();

				// set up glass pane to actually display elements
				glassPane.setOpaque(false);
				glassPane.setVisible(true);

				Draggable.this.setMouse(Cursor.MOVE_CURSOR);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				Draggable.this.ensureConfigured();

				Draggable.this.broadcastMouseEvent(e);
				Draggable.this.setMouse(Cursor.DEFAULT_CURSOR);
				Draggable.this.setInInitialPosition(true);

				// remove from glass pane and repaint to display removal
				JPanel glassPane = (JPanel) Draggable.this.get().getRootPane().getGlassPane();
				glassPane.remove(Draggable.this.get());
				glassPane.repaint();

				Draggable.this.drop(e);
				// return label to old position
				Draggable.this.getInitialParent().add(Draggable.this.get(), Draggable.this.getConstraints());
				Draggable.this.get().setVisible(true);
				Draggable.this.getInitialParent().revalidate();
				Draggable.this.getInitialParent().repaint();

				Draggable.this.setInitialParent(null);
				// constraints are not reset, we assume that the component will stay with the same parent
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				if (Draggable.this.inInitialPosition()) {
					Draggable.this.setMouse(Cursor.HAND_CURSOR);
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (Draggable.this.inInitialPosition()) {
					Draggable.this.setMouse(Cursor.DEFAULT_CURSOR);
				}
			}
		};
	}

	default MouseMotionListener getMouseMotionListener() {
		return new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				// get task bar height
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				Rectangle windowSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
				int taskBarHeight = (int) (screenSize.getHeight() - windowSize.getHeight());

				int mouseScreenX = e.getXOnScreen();
				int mouseScreenY = e.getYOnScreen();

				// calculate, offsetting y for the task bar
				// note: task bar offsetting will probably break if people have their taskbar at the top of the screen!
				// good thing I don't care!
				JFrame frame = (JFrame) SwingUtilities.getRoot(Draggable.this.get());
				int mouseFrameX = mouseScreenX - frame.getX();
				int mouseFrameY = mouseScreenY - frame.getY() - taskBarHeight;

				// set location and ensure visibility
				Draggable.this.get().setLocation(mouseFrameX, mouseFrameY);
				Draggable.this.get().setVisible(true);

				// update dock highlighting
				Draggable.this.broadcastMouseEvent(e);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				// no-op
			}
		};
	}

	default void setMouse(int mouse) {
		JRootPane rootPane = this.get().getRootPane();
		rootPane.setCursor(Cursor.getPredefinedCursor(mouse));
	}

	/**
	 * ensures that the label is properly configured and ready to be dragged.
	 * <br> should be called before dragging begins to prevent difficult-to-trace errors!
	 */
	default void ensureConfigured() {
		if (this.getConstraints() == null || this.getInitialParent() == null) {
			throw new IllegalStateException("a draggable component was not correctly configured and therefore cannot properly be re-added to its parent if dropped outside a dock! (did you forget to call setConstraints()?)");
		}
	}
}
