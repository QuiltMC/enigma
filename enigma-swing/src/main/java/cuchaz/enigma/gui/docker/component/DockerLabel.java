package cuchaz.enigma.gui.docker.component;

import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.gui.docker.Dock;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
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

/**
 * A user-draggable label that is used for docker titles.
 */
public class DockerLabel extends JLabel {
	private JComponent initialParent;
	private Object constraints;
	private boolean beingDragged;

	private final Docker docker;

	public DockerLabel(Docker docker, String text) {
		super(text);
		this.setOpaque(false);
		// note: docker and parent are not the same!
		// the parent could be a sub-container of the docker
		this.docker = docker;
		this.initialParent = null;
		this.constraints = null;

		this.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// no-op
			}

			@Override
			public void mousePressed(MouseEvent e) {
				DockerLabel.this.beingDragged = true;

				// save parent for re-addition after dragging is finished
				DockerLabel.this.initialParent = (JComponent) DockerLabel.this.getParent();

				// validate
				DockerLabel.this.ensureConfigured();

				// configure object to be on the glass pane instead of its former pane
				DockerLabel.this.setVisible(false);
				JRootPane rootPane = DockerLabel.this.getRootPane();
				JPanel glassPane = (JPanel) rootPane.getGlassPane();
				DockerLabel.this.initialParent.remove(DockerLabel.this);
				glassPane.add(DockerLabel.this);

				// repaint former panel to display removal of element
				DockerLabel.this.initialParent.repaint();

				// set up glass pane to actually display elements
				glassPane.setOpaque(false);
				glassPane.setVisible(true);

				DockerLabel.this.setMouse(Cursor.MOVE_CURSOR);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				DockerLabel.this.ensureConfigured();

				Dock.Util.receiveMouseEvent(e);

				// remove from glass pane and repaint to display removal
				JPanel glassPane = (JPanel) DockerLabel.this.getRootPane().getGlassPane();
				glassPane.remove(DockerLabel.this);
				glassPane.repaint();

				// return label to old position
				DockerLabel.this.initialParent.add(DockerLabel.this, DockerLabel.this.constraints);
				DockerLabel.this.setVisible(true);
				DockerLabel.this.initialParent.revalidate();
				DockerLabel.this.initialParent.repaint();

				// if dropped over a docker, snap into place
				Dock.Util.dropDocker(DockerLabel.this.docker, e);

				DockerLabel.this.initialParent = null;
				// constraints are not reset, we assume that the label will stay with the same parent

				DockerLabel.this.setMouse(Cursor.DEFAULT_CURSOR);

				DockerLabel.this.beingDragged = false;
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				if (!DockerLabel.this.beingDragged) {
					DockerLabel.this.setMouse(Cursor.HAND_CURSOR);
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (!DockerLabel.this.beingDragged) {
					DockerLabel.this.setMouse(Cursor.DEFAULT_CURSOR);
				}
			}
		});

		this.addMouseMotionListener(new MouseMotionListener() {
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
				JFrame frame = (JFrame) SwingUtilities.getRoot(DockerLabel.this);
				int mouseFrameX = mouseScreenX - frame.getX();
				int mouseFrameY = mouseScreenY - frame.getY() - taskBarHeight;

				// set location and ensure visibility
				DockerLabel.this.setLocation(mouseFrameX, mouseFrameY);
				DockerLabel.this.setVisible(true);

				// update dock highlighting
				Dock.Util.receiveMouseEvent(e);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				// no-op
			}
		});
	}

	/**
	 * saves constraints for re-adding component to old parent after dragging is finished, provided the panel is not dropped in a dock when released
	 * <br> must be called when adding a draggable label to a panel!
	 * @param constraints the constraints to use when re-adding the component to the old parent
	 */
	public void setConstraints(Object constraints) {
		this.constraints = constraints;
	}

	private void setMouse(int mouse) {
		JRootPane rootPane = this.getRootPane();
		rootPane.setCursor(Cursor.getPredefinedCursor(mouse));
	}

	/**
	 * ensures that the label is properly configured and ready to be dragged.
	 * <br> should be called before dragging begins to prevent difficult-to-trace errors!
	 */
	private void ensureConfigured() {
		if (DockerLabel.this.constraints == null || DockerLabel.this.initialParent == null) {
			throw new IllegalStateException("draggable label \"" + this.getText() + "\" was not properly configured and therefore cannot properly be re-added to its parent if dropped outside a dock! (did you forget to call setConstraints()?)");
		}
	}
}
