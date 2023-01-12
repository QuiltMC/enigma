package cuchaz.enigma.gui.docker.component;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.SwingUtilities;
import javax.swing.plaf.LayerUI;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Originally by 2xsaiko. Modified to only allow right-angle rotations.
 */
public class RightAngleLayerUI extends LayerUI<JComponent> {
	private final Rotation rotation;

	private Component lastEnteredTarget;
	private Component lastPressedTarget;
	private boolean dispatchingMode = false;

	public RightAngleLayerUI(Rotation rotation) {
		this.rotation = rotation;
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		Graphics2D g2d = (Graphics2D) g;
		if (rotation == Rotation.COUNTERCLOCKWISE) {
			g2d.translate(0, c.getHeight());
		} else if (rotation == Rotation.CLOCKWISE) {
			g2d.translate(c.getWidth(), 0);
		}
		g2d.rotate(-rotation.getAsInteger() * (Math.PI * 0.5));
		super.paint(g2d, c);
	}

	@Override
	public void doLayout(JLayer<? extends JComponent> l) {
		Component view = l.getView();
		Dimension d = rotate(new Dimension(l.getWidth(), l.getHeight()));
		if (view != null) {
			view.setBounds(0, 0, d.width, d.height);
		}
		Component glassPane = l.getGlassPane();
		if (glassPane != null) {
			glassPane.setBounds(0, 0, d.width, d.height);
		}
	}

	/**
	 * Find the deepest component in the AWT hierarchy
	 *
	 * @param layer the layer to which this UI is installed
	 * @param targetPoint the point in layer's coordinates
	 * @return the component in the specified point
	 */
	private Component getTarget(JLayer<? extends JComponent> layer, Point targetPoint) {
		Component view = layer.getView();
		if (view == null) {
			return null;
		} else {
			Point viewPoint = SwingUtilities.convertPoint(layer, targetPoint, view);
			return SwingUtilities.getDeepestComponentAt(view, viewPoint.x, viewPoint.y);
		}
	}

	@Override
	public Dimension getPreferredSize(JComponent c) {
		return rotate(super.getPreferredSize(c));
	}

	@Override
	public Dimension getMinimumSize(JComponent c) {
		return rotate(super.getMinimumSize(c));
	}

	@Override
	public Dimension getMaximumSize(JComponent c) {
		return rotate(super.getMaximumSize(c));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
		JLayer<Component> l = (JLayer<Component>) c;
		l.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void uninstallUI(JComponent c) {
		JLayer<Component> l = (JLayer<Component>) c;
		l.setLayerEventMask(0);
		super.uninstallUI(c);
	}

	/**
	 * Process the mouse events and map the mouse coordinates inverting the internal affine transformation.
	 *
	 * @param event the event to be dispatched
	 * @param layer the layer this LayerUI is set to
	 */
	@Override
	public void eventDispatched(AWTEvent event, JLayer<? extends JComponent> layer) {
		if (event instanceof MouseEvent mouseEvent) {
			// The if discriminates between the generated and original event.
			// Removing it will cause a stack overflow caused by the event being redispatched to this class.

			if (!dispatchingMode) {
				// Process an original mouse event
				dispatchingMode = true;
				try {
					redispatchMouseEvent(mouseEvent, layer);
				} finally {
					dispatchingMode = false;
				}
			} else {
				// Process generated mouse events
				// Added a check, because on mouse entered or exited, the cursor
				// may be set to specific dragging cursors.

				if (MouseEvent.MOUSE_ENTERED == mouseEvent.getID() || MouseEvent.MOUSE_EXITED == mouseEvent.getID()) {
					layer.getGlassPane().setCursor(null);
				} else {
					Component component = mouseEvent.getComponent();
					layer.getGlassPane().setCursor(component.getCursor());
				}
			}
		} else {
			super.eventDispatched(event, layer);
		}
		layer.repaint();
	}

	private void redispatchMouseEvent(MouseEvent originalEvent, JLayer<? extends JComponent> layer) {
		if (layer.getView() != null) {
			if (originalEvent.getComponent() != layer.getGlassPane()) {
				originalEvent.consume();
			}
			MouseEvent newEvent = null;

			Point realPoint = transform(originalEvent.getX(), originalEvent.getY(), layer.getWidth(), layer.getHeight(), rotation);
			Component realTarget = getTarget(layer, realPoint);

			if (realTarget != null) {
				realTarget = getListeningComponent(originalEvent, realTarget);
			}

			switch (originalEvent.getID()) {
				case MouseEvent.MOUSE_PRESSED -> {
					newEvent = transformMouseEvent(layer, originalEvent, realTarget, realPoint);
					if (newEvent != null) {
						lastPressedTarget = newEvent.getComponent();
					}
				}
				case MouseEvent.MOUSE_RELEASED -> {
					newEvent = transformMouseEvent(layer, originalEvent, lastPressedTarget, realPoint);
					lastPressedTarget = null;
				}
				case MouseEvent.MOUSE_CLICKED -> {
					newEvent = transformMouseEvent(layer, originalEvent, realTarget, realPoint);
					lastPressedTarget = null;
				}
				case MouseEvent.MOUSE_MOVED -> {
					newEvent = transformMouseEvent(layer, originalEvent, realTarget, realPoint);
					generateEnterExitEvents(layer, originalEvent, realTarget, realPoint);
				}
				case MouseEvent.MOUSE_ENTERED, MouseEvent.MOUSE_EXITED ->
						generateEnterExitEvents(layer, originalEvent, realTarget, realPoint);
				case MouseEvent.MOUSE_DRAGGED -> {
					newEvent = transformMouseEvent(layer, originalEvent, lastPressedTarget, realPoint);
					generateEnterExitEvents(layer, originalEvent, realTarget, realPoint);
				}
				case MouseEvent.MOUSE_WHEEL ->
						newEvent = transformMouseWheelEvent(layer, (MouseWheelEvent) originalEvent, realTarget, realPoint);
			}
			dispatchMouseEvent(newEvent);
		}
	}

	private MouseEvent transformMouseEvent(JLayer<? extends JComponent> layer, MouseEvent mouseEvent, Component target, Point realPoint) {
		return transformMouseEvent(layer, mouseEvent, target, realPoint, mouseEvent.getID());
	}

	/**
	 * Create the new event to be dispatched.
	 */
	private MouseEvent transformMouseEvent(JLayer<? extends JComponent> layer, MouseEvent mouseEvent, Component target, Point targetPoint, int id) {
		if (target == null) {
			return null;
		} else {
			Point newPoint = SwingUtilities.convertPoint(layer, targetPoint, target);
			return new MouseEvent(target,
					id,
					mouseEvent.getWhen(),
					mouseEvent.getModifiers(),
					newPoint.x,
					newPoint.y,
					mouseEvent.getClickCount(),
					mouseEvent.isPopupTrigger(),
					mouseEvent.getButton());
		}
	}

	/**
	 * Create the new mouse wheel event to be dispatched.
	 */
	private MouseWheelEvent transformMouseWheelEvent(JLayer<? extends JComponent> layer, MouseWheelEvent mouseWheelEvent, Component target, Point targetPoint) {
		if (target == null) {
			return null;
		} else {
			Point newPoint = SwingUtilities.convertPoint(layer, targetPoint, target);
			return new MouseWheelEvent(target,
					mouseWheelEvent.getID(),
					mouseWheelEvent.getWhen(),
					mouseWheelEvent.getModifiers(),
					newPoint.x,
					newPoint.y,
					mouseWheelEvent.getClickCount(),
					mouseWheelEvent.isPopupTrigger(),
					mouseWheelEvent.getScrollType(),
					mouseWheelEvent.getScrollAmount(),
					mouseWheelEvent.getWheelRotation()
			);
		}
	}

	/**
	 * dispatch the {@code mouseEvent}
	 *
	 * @param mouseEvent the event to be dispatched
	 */
	private void dispatchMouseEvent(MouseEvent mouseEvent) {
		if (mouseEvent != null) {
			Component target = mouseEvent.getComponent();
			target.dispatchEvent(mouseEvent);
		}
	}

	/**
	 * Get the listening component associated to the {@code component}'s {@code event}
	 */
	private Component getListeningComponent(MouseEvent event, Component component) {
		return switch (event.getID()) {
			case MouseEvent.MOUSE_CLICKED, MouseEvent.MOUSE_ENTERED, MouseEvent.MOUSE_EXITED, MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED ->
					getMouseListeningComponent(component);
			case MouseEvent.MOUSE_DRAGGED, MouseEvent.MOUSE_MOVED -> getMouseMotionListeningComponent(component);
			case MouseEvent.MOUSE_WHEEL -> getMouseWheelListeningComponent(component);
			default -> null;
		};
	}

	/**
	 * Cycles through the {@code component}'s parents to find the {@link Component} with associated {@link MouseListener}
	 */
	private Component getMouseListeningComponent(Component component) {
		if (component.getMouseListeners().length > 0) {
			return component;
		} else {
			Container parent = component.getParent();
			if (parent != null) {
				return getMouseListeningComponent(parent);
			} else {
				return null;
			}
		}
	}

	/**
	 * Cycles through the {@code component}'s parents to find the {@link Component} with associated {@link MouseMotionListener}
	 */
	private Component getMouseMotionListeningComponent(Component component) {
		// Mouse motion events may result in MOUSE_ENTERED and MOUSE_EXITED.
		// Therefore, components with MouseListeners registered should be
		// returned as well.

		if (component.getMouseMotionListeners().length > 0 || component.getMouseListeners().length > 0) {
			return component;
		} else {
			Container parent = component.getParent();
			if (parent != null) {
				return getMouseMotionListeningComponent(parent);
			} else {
				return null;
			}
		}
	}

	/**
	 * Cycles through the {@code component}'s parents to find the {@link Component} with associated {@link MouseWheelListener}
	 */
	private Component getMouseWheelListeningComponent(Component component) {
		if (component.getMouseWheelListeners().length > 0) {
			return component;
		} else {
			Container parent = component.getParent();
			if (parent != null) {
				return getMouseWheelListeningComponent(parent);
			} else {
				return null;
			}
		}
	}

	/**
	 * Generate a {@code MOUSE_ENTERED} and {@code MOUSE_EXITED} event when the target component is changed
	 */
	private void generateEnterExitEvents(JLayer<? extends JComponent> layer, MouseEvent originalEvent, Component newTarget, Point realPoint) {
		if (lastEnteredTarget != newTarget) {
			dispatchMouseEvent(transformMouseEvent(layer, originalEvent, lastEnteredTarget, realPoint, MouseEvent.MOUSE_EXITED));
			lastEnteredTarget = newTarget;
			dispatchMouseEvent(transformMouseEvent(layer, originalEvent, lastEnteredTarget, realPoint, MouseEvent.MOUSE_ENTERED));
		}
	}

	@SuppressWarnings("SuspiciousNameCombination")
	private static Dimension rotate(Dimension self) {
		return new Dimension(self.height, self.width);
	}

	@SuppressWarnings("SuspiciousNameCombination")
	private static Point transform(int x, int y, int width, int height, Rotation rotation) {
		if (rotation == Rotation.COUNTERCLOCKWISE) {
			return new Point(height - y, x);
		} else if (rotation == Rotation.CLOCKWISE) {
			return new Point(y, width - x);
		} else {
			throw new IllegalArgumentException("Unknown rotation: " + rotation);
		}
	}

	public enum Rotation {
		CLOCKWISE(3),
		COUNTERCLOCKWISE(1);

		private final int asInteger;

		Rotation(int rotation) {
			this.asInteger = rotation;
		}

		public int getAsInteger() {
			return asInteger;
		}
	}
}
