package cuchaz.enigma.gui.docker.component;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public interface Draggable {
	ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

	/**
	 * Returns whether the mouse is currently pressed.
	 * @return true if the mouse is pressed, false otherwise
	 */
	boolean mousePressed();

	/**
	 * Sets whether the mouse is currently pressed.
	 * @param mousePressed the new state
	 */
	void setMousePressed(boolean mousePressed);

	/**
	 * @return whether to cancel currently pending "begin dragging" events.
	 */
	boolean cancelEvents();

	/**
	 * @param cancelEvents whether to cancel all currently pending "begin dragging" events. This should be called when an event has successfully started
	 */
	void setCancelEvents(boolean cancelEvents);

	/**
	 * Gets the draggable's initial parent - that is, the parent of the draggable immediately before dragging begins.
	 * @return the initial parent
	 */
	JComponent getInitialParent();

	/**
	 * Sets the draggable's initial parent - that is, the parent of the draggable immediately before dragging begins.
	 * @param parent the new parent
	 */
	void setInitialParent(JComponent parent);

	/**
	 * Gets the constraints to use when returning the draggable to its parent post-drag.
	 * @return the constraints as a generic object
	 */
	Object getConstraints();

	/**
	 * Sets the constraints for this draggable object.
	 * @param constraints the new constraints
	 */
	void setConstraints(Object constraints);

	/**
	 * Gets this draggable object as a Swing component.
	 * @return the component
	 */
	JComponent get();

	/**
	 * The drag delay is a delay between the mouse press and the component entering the drag state.
	 * @return the drag delay in milliseconds
	 */
	int getDragDelay();

	/**
	 * Drops the draggable in its new position if possible.
	 * @param e the mouse event that triggered the drop, provides position information
	 * @return whether to return the component to its original position
	 */
	boolean drop(MouseEvent e);

	/**
	 * Broadcasts a mouse event to be used for highlighting.
	 * @param e the mouse event that triggered highlighting, provides position information
	 */
	void broadcastMouseEvent(MouseEvent e);

	default MouseListener getMouseListener() {
		this.setCancelEvents(false);

		return new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// no-op
			}

			@Override
			public void mousePressed(MouseEvent e) {
				Draggable.this.setMousePressed(true);

				Runnable beginDragging = () -> {
					JComponent component = Draggable.this.get();
					Point mousePosition = component.getRootPane().getMousePosition();

					// cancel action if the timeout has passed and the user is not still holding the mouse down on the component
					if (Draggable.this.getDragDelay() > 0
							&& (mousePosition == null || !Draggable.this.mousePressed() || Draggable.this.cancelEvents() || !SwingUtilities.getDeepestComponentAt(component.getRootPane().getContentPane(), mousePosition.x, mousePosition.y).equals(Draggable.this.get()))) {
						return;
					}

					// save parent for re-addition after dragging is finished
					Draggable.this.setInitialParent((JComponent) component.getParent());

					// validate
					Draggable.this.ensureConfigured();

					// configure object to be on the glass pane instead of its former pane
					component.setVisible(false);
					JRootPane rootPane = component.getRootPane();
					JPanel glassPane = (JPanel) rootPane.getGlassPane();
					Draggable.this.getInitialParent().remove(component);
					glassPane.add(component);

					// repaint former panel to display removal of element
					Draggable.this.getInitialParent().repaint();

					// set up glass pane to actually display elements
					glassPane.setOpaque(false);
					glassPane.setVisible(true);

					Draggable.this.setMouse(Cursor.MOVE_CURSOR);
					Draggable.this.setCancelEvents(true);
				};

				if (Draggable.this.getDragDelay() > 0) {
					executor.schedule(beginDragging, Draggable.this.getDragDelay(), TimeUnit.MILLISECONDS);
				} else {
					beginDragging.run();
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				Draggable.this.setMousePressed(false);
				Draggable.this.broadcastMouseEvent(e);

				if (Draggable.this.getInitialParent() != null) {
					Draggable.this.setMouse(Cursor.DEFAULT_CURSOR);

					// remove from glass pane and repaint to display removal
					JPanel glassPane = (JPanel) Draggable.this.get().getRootPane().getGlassPane();
					glassPane.remove(Draggable.this.get());
					glassPane.repaint();

					if (!Draggable.this.drop(e)) {
						// return label to old position
						Draggable.this.getInitialParent().add(Draggable.this.get(), Draggable.this.getConstraints());
					}

					Draggable.this.get().setVisible(true);
					Draggable.this.getInitialParent().revalidate();
					Draggable.this.getInitialParent().repaint();

					Draggable.this.setInitialParent(null);
					Draggable.this.setCancelEvents(false);
					// constraints are not reset, we assume that the component will stay with the same parent
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				if (Draggable.this.getInitialParent() == null) {
					Draggable.this.setMouse(Cursor.HAND_CURSOR);
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (Draggable.this.getInitialParent() == null) {
					Draggable.this.setMouse(Cursor.DEFAULT_CURSOR);
				}
			}
		};
	}

	default MouseMotionListener getMouseMotionListener() {
		return new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (Draggable.this.getInitialParent() != null) {
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

					Draggable.this.broadcastMouseEvent(e);
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				// no-op
			}
		};
	}

	static boolean contains(Rectangle rectangle, Point point) {
		return (point.x >= rectangle.x && point.x <= rectangle.x + rectangle.width)
				&& (point.y >= rectangle.y && point.y <= rectangle.y + rectangle.height);
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
