package cuchaz.enigma.gui.panels.right;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class DraggableLabel extends JLabel {
	private JComponent initialParent;

	public DraggableLabel(String text) {
		super(text);
		this.setOpaque(false);

		this.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// no-op
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// save parent for re-addition after dragging is finished
				DraggableLabel.this.initialParent = (JComponent) DraggableLabel.this.getParent();

				// configure object to be on the glass pane instead of its former pane
				DraggableLabel.this.setVisible(false);
				JPanel glassPane = (JPanel) SwingUtilities.getRootPane(DraggableLabel.this).getGlassPane();
				DraggableLabel.this.initialParent.remove(DraggableLabel.this);
				glassPane.add(DraggableLabel.this);

				// repaint former panel to display removal of element
				DraggableLabel.this.initialParent.repaint();

				// set up glass pane to actually display elements
				glassPane.setOpaque(false);
				glassPane.setVisible(true);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// remove from glass pane
				JPanel glassPane = (JPanel) SwingUtilities.getRootPane(DraggableLabel.this).getGlassPane();
				glassPane.remove(DraggableLabel.this);

				// add to former panel
				DraggableLabel.this.initialParent.add(DraggableLabel.this);
				DraggableLabel.this.initialParent.repaint();

				DraggableLabel.this.initialParent = null;
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// no-op
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// no-op
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
				JFrame frame = (JFrame) SwingUtilities.getRoot(DraggableLabel.this);
				int mouseFrameX = mouseScreenX - frame.getX();
				int mouseFrameY = mouseScreenY - frame.getY() - taskBarHeight;

				// set location and ensure visibility
				DraggableLabel.this.setLocation(mouseFrameX, mouseFrameY);
				DraggableLabel.this.setVisible(true);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				// no-op
			}
		});
	}
}

