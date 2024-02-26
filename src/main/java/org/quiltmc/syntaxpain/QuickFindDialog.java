package org.quiltmc.syntaxpain;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.lang.ref.WeakReference;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A dialog used to find instances of specific strings in a {@link JTextComponent}.
 * Designed to be extensible.
 */
public class QuickFindDialog extends JDialog implements DocumentListener, ActionListener, EscapeListener {
	protected static final int SEARCH_FIELD_MAX_WIDTH = 200;
	protected static final int SEARCH_FIELD_MAX_HEIGHT = 24;
	protected static final int SEARCH_FIELD_MIN_WIDTH = 60;
	private static final int SEARCH_FIELD_MIN_HEIGHT = 24;
	protected static final int PREFERRED_TOOLBAR_WIDTH = 684;

	protected final Markers.SimpleMarker marker = new Markers.SimpleMarker(Color.PINK);
	protected WeakReference<JTextComponent> target;
	protected final WeakReference<DocumentSearchData> searchData;
	protected int prevCaretPos;

	protected JLabel statusLabel;
	protected JTextField searchField;
	protected JButton prevButton;
	protected JButton nextButton;
	protected JCheckBox ignoreCaseCheckBox;
	protected JCheckBox regexCheckBox;
	protected JCheckBox wrapCheckBox;

	public QuickFindDialog(JTextComponent target) {
		this(target, DocumentSearchData.getFromEditor(target));
	}

	public QuickFindDialog(JTextComponent target, DocumentSearchData searchData) {
		super(SwingUtilities.getWindowAncestor(target), ModalityType.MODELESS);

		this.initComponents();
		Util.addEscapeListener(this);
		this.searchData = new WeakReference<>(searchData);
	}

	public void showFor(JTextComponent target) {
		this.prevCaretPos = target.getCaretPosition();

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

		this.searchField.setFont(target.getFont());
		this.searchField.getDocument().addDocumentListener(this);

		// Close the dialog when clicking outside it
		WindowFocusListener focusListener = new WindowAdapter() {
			@Override
			public void windowLostFocus(WindowEvent e) {
				target.getDocument().removeDocumentListener(QuickFindDialog.this);
				Markers.removeMarkers(target, QuickFindDialog.this.marker);
				QuickFindDialog.this.removeWindowListener(this);
				QuickFindDialog.this.setVisible(false);
				target.requestFocus();
			}
		};
		// this.addWindowFocusListener(focusListener);

		this.target = new WeakReference<>(target);

		DocumentSearchData searchData = this.searchData.get();
		this.wrapCheckBox.setSelected(searchData.isWrap());

		// Set the search field to the current selection
		String selectedText = target.getSelectedText();
		if (selectedText != null) {
			this.searchField.setText(selectedText);
		} else {
			Pattern pattern = searchData.getPattern();
			if (pattern != null) {
				this.searchField.setText(pattern.pattern());
			}
		}

		this.searchField.selectAll();

		this.setVisible(true);
	}

	protected void initComponents() {
		JToolBar toolBar = new JToolBar();
		this.statusLabel = new JLabel();
		this.searchField = new JTextField();
		this.prevButton = new JButton("prev");
		this.nextButton = new JButton("next");
		this.ignoreCaseCheckBox = new JCheckBox();
		this.regexCheckBox = new JCheckBox();
		this.wrapCheckBox = new JCheckBox();

		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setBackground(Color.DARK_GRAY);
		this.setName("QuickFindDialog");
		this.setResizable(false);
		this.setUndecorated(true);

		toolBar.setBorder(BorderFactory.createEtchedBorder());
		toolBar.setFloatable(false);
		toolBar.setRollover(true);
		toolBar.addSeparator();

		this.searchField.setColumns(30);
		this.searchField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		this.searchField.setMaximumSize(new Dimension(SEARCH_FIELD_MAX_WIDTH, SEARCH_FIELD_MAX_HEIGHT));
		this.searchField.setMinimumSize(new Dimension(SEARCH_FIELD_MIN_WIDTH, SEARCH_FIELD_MIN_HEIGHT));
		toolBar.add(this.searchField);
		toolBar.addSeparator();

		this.prevButton.setHorizontalTextPosition(SwingConstants.CENTER);
		this.prevButton.setFocusable(false);
		this.prevButton.setOpaque(false);
		this.prevButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.prevButton.addActionListener(this::prevButtonActionPerformed);
		toolBar.add(this.prevButton);

		this.nextButton.setHorizontalTextPosition(SwingConstants.CENTER);
		this.nextButton.setMargin(new Insets(2, 2, 2, 2));
		this.nextButton.setFocusable(false);
		this.nextButton.setOpaque(false);
		this.nextButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.nextButton.addActionListener(this::nextButtonActionPerformed);
		toolBar.add(this.nextButton);

		toolBar.addSeparator();

		this.ignoreCaseCheckBox.setText("Ignore case");
		this.ignoreCaseCheckBox.setFocusable(false);
		this.ignoreCaseCheckBox.setOpaque(false);
		this.ignoreCaseCheckBox.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.ignoreCaseCheckBox.addActionListener(this);
		toolBar.add(this.ignoreCaseCheckBox);

		this.regexCheckBox.setText("Use regex");
		this.regexCheckBox.setFocusable(false);
		this.regexCheckBox.setOpaque(false);
		this.regexCheckBox.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.regexCheckBox.addActionListener(this);
		toolBar.add(this.regexCheckBox);

		this.wrapCheckBox.setText("Wrap");
		this.wrapCheckBox.setFocusable(false);
		this.wrapCheckBox.setOpaque(false);
		this.wrapCheckBox.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.wrapCheckBox.addActionListener(this);
		toolBar.add(this.wrapCheckBox);

		this.statusLabel.setFont(this.statusLabel.getFont().deriveFont(this.statusLabel.getFont().getStyle() | Font.BOLD, this.statusLabel.getFont().getSize() - 2));
		this.statusLabel.setForeground(Color.RED);
		toolBar.add(this.statusLabel);

		GroupLayout layout = new GroupLayout(this.getContentPane());
		this.getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(toolBar, GroupLayout.DEFAULT_SIZE, PREFERRED_TOOLBAR_WIDTH, Short.MAX_VALUE));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(toolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));

		this.pack();
	}

	private void prevButtonActionPerformed(ActionEvent e) {
		JTextComponent target = this.target.get();
		int caretPos = target.getCaretPosition();
		if (this.searchData.get().doFindPrev(target)) {
			this.statusLabel.setText(null);
			this.prevCaretPos = caretPos;
			this.fixOverlappedCaret();
		} else {
			this.statusLabel.setText("QuickFindDialog.NotFound");
		}
	}

	private void nextButtonActionPerformed(ActionEvent e) {
		JTextComponent target = this.target.get();
		int caretPos = target.getCaretPosition();
		if (this.searchData.get().doFindNext(target)) {
			this.statusLabel.setText(null);
			this.prevCaretPos = caretPos;
			this.fixOverlappedCaret();
		} else {
			this.statusLabel.setText("QuickFindDialog.NotFound");
		}
	}

	private void fixOverlappedCaret() {
		JTextComponent target = this.target.get();
		try {
			var caretViewPos = target.modelToView2D(target.getCaretPosition());
			int caretY = (int) caretViewPos.getY();

			if (caretY >= target.getVisibleRect().height - this.getHeight()) {
				target.scrollRectToVisible(new Rectangle((int) caretViewPos.getX(), caretY + this.getHeight() * 2, 1, 1));
			}
		} catch (BadLocationException ex) {
			// ignore
		}
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		this.updateFind();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		this.updateFind();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		this.updateFind();
	}

	private void updateFind() {
		JTextComponent target = this.target.get();
		DocumentSearchData searchData = this.searchData.get();
		String searchText = this.searchField.getText();

		if (searchText == null || searchText.isEmpty()) {
			this.statusLabel.setText(null);
			return;
		}

		try {
			searchData.setWrap(this.wrapCheckBox.isSelected());
			searchData.setPattern(searchText, this.regexCheckBox.isSelected(), this.ignoreCaseCheckBox.isSelected());
			this.statusLabel.setText(null);

			// The DocumentSearchData doFindNext will always find from current pos,
			// so we need to relocate to our saved pos before we call doFindNext
			target.setCaretPosition(this.prevCaretPos);
			if (!searchData.doFindNext(target)) {
				this.statusLabel.setText("QuickFindDialog.NotFound");
			} else {
				this.statusLabel.setText(null);
			}
		} catch (PatternSyntaxException e) {
			this.statusLabel.setText(e.getDescription());
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JCheckBox) {
			this.updateFind();
		}
	}

	@Override
	public void escapePressed() {
		this.setVisible(false);
	}
}
