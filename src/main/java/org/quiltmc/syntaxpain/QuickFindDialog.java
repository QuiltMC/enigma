package org.quiltmc.syntaxpain;

import javax.swing.GroupLayout;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Function;

/**
 * A dialog containing a {@link QuickFindToolBar}.
 *
 * @see SyntaxpainConfiguration#setQuickFindDialogFactory(Function)
 */
public class QuickFindDialog extends JDialog implements EscapeListener {
	protected static final int PREFERRED_TOOLBAR_WIDTH = 684;

	protected QuickFindToolBar quickFindToolBar;

	public QuickFindDialog(JTextComponent target) {
		super(SwingUtilities.getWindowAncestor(target), ModalityType.MODELESS);

		this.quickFindToolBar = new QuickFindToolBar();
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setBackground(Color.DARK_GRAY);
		this.setResizable(false);
		this.setUndecorated(true);

		GroupLayout layout = new GroupLayout(this.getContentPane());
		this.getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(this.quickFindToolBar, GroupLayout.DEFAULT_SIZE, PREFERRED_TOOLBAR_WIDTH, Short.MAX_VALUE));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(this.quickFindToolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));

		this.setName("QuickFindDialog");

		Util.addEscapeListener(this);
	}

	public void showFor(JTextComponent target) {
		Container view = target.getParent();
		Dimension size = this.getSize();

		// Set the width of the dialog to the width of the target
		size.width = target.getVisibleRect().width;
		this.setSize(size);

		// Put the dialog at the bottom of the target
		Point loc = new Point(0, view.getHeight() - size.height);
		this.setLocationRelativeTo(view);
		SwingUtilities.convertPointToScreen(loc, view);
		this.setLocation(loc);

		// Close the dialog when clicking outside it
		this.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowLostFocus(WindowEvent e) {
				QuickFindDialog.this.removeWindowListener(this);
				QuickFindDialog.this.setVisible(false);
			}
		});

		this.quickFindToolBar.showFor(target);

		this.setVisible(true);
	}

	@Override
	public void escapePressed() {
		this.setVisible(false);
	}
}
