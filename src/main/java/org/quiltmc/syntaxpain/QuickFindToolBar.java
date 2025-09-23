package org.quiltmc.syntaxpain;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A toolbar used to find instances of specific strings in a {@link JTextComponent}.
 * Designed to be extensible.
 *
 * @see QuickFindDialog
 */
public class QuickFindToolBar extends JToolBar implements DocumentListener, ActionListener {
	protected static final int SEARCH_FIELD_MAX_WIDTH = 200;
	protected static final int SEARCH_FIELD_MAX_HEIGHT = 24;
	protected static final int SEARCH_FIELD_MIN_WIDTH = 60;
	private static final int SEARCH_FIELD_MIN_HEIGHT = 24;

	protected final Markers.SimpleMarker marker = new Markers.SimpleMarker(Color.PINK);
	protected WeakReference<JTextComponent> target = new WeakReference<>(null);
	protected WeakReference<DocumentSearchData> searchData = new WeakReference<>(null);
	protected int prevCaretPos;

	protected JLabel statusLabel;
	protected JTextField searchField;
	protected JButton prevButton;
	protected JButton nextButton;
	protected JCheckBox ignoreCaseCheckBox;
	protected JCheckBox regexCheckBox;
	protected JCheckBox wrapCheckBox;

	protected String prev = "prev";
	protected String next = "next";
	protected String ignoreCase = "Ignore case";
	protected String useRegex = "Use regex";
	protected String wrap = "Wrap";
	protected String notFound = "Not found";

	public QuickFindToolBar() {
		this.initComponents();
	}

	public void showFor(JTextComponent target) {
		this.searchData = new WeakReference<>(DocumentSearchData.getFrom(target.getDocument()));
		this.target = new WeakReference<>(target);

		this.prevCaretPos = target.getCaretPosition();

		Container view = target.getParent();
		Dimension size = this.getSize();

		// Set the width of the dialog to the width of the target
		size.width = target.getVisibleRect().width;
		this.setSize(size);

		// Put the dialog at the bottom of the target
		Point loc = new Point(0, view.getHeight() - size.height);
		SwingUtilities.convertPointToScreen(loc, view);
		this.setLocation(loc);

		this.searchField.setFont(target.getFont());

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

		this.setVisible(true);
		this.searchField.requestFocus();
		this.searchField.selectAll();
	}

	protected void initComponents() {
		this.statusLabel = new JLabel();
		this.searchField = new JTextField();
		this.prevButton = new JButton(this.prev);
		this.nextButton = new JButton(this.next);
		this.ignoreCaseCheckBox = new JCheckBox(this.ignoreCase);
		this.regexCheckBox = new JCheckBox(this.useRegex);
		this.wrapCheckBox = new JCheckBox(this.wrap);

		this.setBorder(BorderFactory.createEtchedBorder());
		this.setFloatable(false);
		this.setRollover(true);
		this.addSeparator();

		this.searchField.setColumns(30);
		this.searchField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		this.searchField.setMaximumSize(new Dimension(SEARCH_FIELD_MAX_WIDTH, SEARCH_FIELD_MAX_HEIGHT));
		this.searchField.setMinimumSize(new Dimension(SEARCH_FIELD_MIN_WIDTH, SEARCH_FIELD_MIN_HEIGHT));
		this.add(this.searchField);
		this.addSeparator();

		this.prevButton.setHorizontalTextPosition(SwingConstants.CENTER);
		this.prevButton.setFocusable(false);
		this.prevButton.setOpaque(false);
		this.prevButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.prevButton.addActionListener(this::prevButtonActionPerformed);
		this.add(this.prevButton);

		this.nextButton.setHorizontalTextPosition(SwingConstants.CENTER);
		this.nextButton.setMargin(new Insets(2, 2, 2, 2));
		this.nextButton.setFocusable(false);
		this.nextButton.setOpaque(false);
		this.nextButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.nextButton.addActionListener(this::nextButtonActionPerformed);
		this.add(this.nextButton);

		this.addSeparator();

		this.ignoreCaseCheckBox.setFocusable(false);
		this.ignoreCaseCheckBox.setOpaque(false);
		this.ignoreCaseCheckBox.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.ignoreCaseCheckBox.addActionListener(this);
		this.add(this.ignoreCaseCheckBox);

		this.regexCheckBox.setFocusable(false);
		this.regexCheckBox.setOpaque(false);
		this.regexCheckBox.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.regexCheckBox.addActionListener(this);
		this.add(this.regexCheckBox);

		this.wrapCheckBox.setFocusable(false);
		this.wrapCheckBox.setOpaque(false);
		this.wrapCheckBox.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.wrapCheckBox.addActionListener(this);
		this.add(this.wrapCheckBox);

		this.statusLabel.setFont(this.statusLabel.getFont().deriveFont(this.statusLabel.getFont().getStyle() | Font.BOLD));
		this.statusLabel.setForeground(Color.RED);
		this.addSeparator();
		this.add(this.statusLabel);

		final InputMap inputMap = this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		if (inputMap != null) {
			final ActionMap actionMap = this.getActionMap();
			if (actionMap != null) {
				final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
				final String actionMapKey = "close-quick-find";
				inputMap.put(escape, actionMapKey);
				actionMap.put(actionMapKey, new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						QuickFindToolBar.this.setVisible(false);
					}
				});
			}
		}

		this.searchField.getDocument().addDocumentListener(this);

		// global listener instead of FocusListener so it receives events for children
		Toolkit.getDefaultToolkit().addAWTEventListener(
				e -> {
					if (e instanceof FocusEvent focus) {
						final Component component = focus.getComponent();
						final boolean componentDescends = component != null
								&& SwingUtilities.isDescendingFrom(component, QuickFindToolBar.this);
						final Component opposite = focus.getOppositeComponent();
						final boolean oppositeDescends = opposite != null
								&& SwingUtilities.isDescendingFrom(opposite, QuickFindToolBar.this);

						if (componentDescends != oppositeDescends) {
							final boolean descendantGained = focus.getID() == FocusEvent.FOCUS_GAINED
									? componentDescends
									: oppositeDescends;

							if (descendantGained) {
								// document.addDocumentListener(QuickFindToolBar.this);
								QuickFindToolBar.this.setVisible(true);
							} else {
								// document.removeDocumentListener(QuickFindToolBar.this);
								QuickFindToolBar.this.setVisible(false);
								final JTextComponent target = QuickFindToolBar.this.target.get();
								if (target != null) {
									Markers.removeMarkers(target, QuickFindToolBar.this.marker);
									target.requestFocus();
								}
							}
						}
					}
				},
				FocusEvent.FOCUS_LOST | FocusEvent.FOCUS_GAINED
		);

		this.translate();
	}

	protected void translate() {
		this.nextButton.setText(this.next);
		this.prevButton.setText(this.prev);
		this.ignoreCaseCheckBox.setText(this.ignoreCase);
		this.regexCheckBox.setText(this.useRegex);
		this.wrapCheckBox.setText(this.wrap);
	}

	private void prevButtonActionPerformed(ActionEvent e) {
		JTextComponent target = this.target.get();
		int caretPos = target.getCaretPosition();
		if (this.searchData.get().doFindPrev(target)) {
			this.statusLabel.setText(null);
			this.prevCaretPos = caretPos;
			this.fixOverlappedCaret();
		} else {
			this.statusLabel.setText(this.notFound);
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
			this.statusLabel.setText(this.notFound);
		}
	}

	private void fixOverlappedCaret() {
		JTextComponent target = this.target.get();
		try {
			var caretViewPos = target.modelToView2D(target.getCaretPosition());
			int caretY = (int) caretViewPos.getY();

			if (caretY >= target.getVisibleRect().height - this.getHeight() * 2) {
				int lineHeight = target.getFontMetrics(target.getFont()).getHeight();
				target.scrollRectToVisible(new Rectangle((int) caretViewPos.getX(), caretY + lineHeight * 12, 1, 1));
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

		if (searchText == null || searchText.isEmpty() || target == null || searchData == null) {
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
			if (searchData.doFindNext(target)) {
				this.statusLabel.setText(null);
			} else {
				this.statusLabel.setText(this.notFound);
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
}
