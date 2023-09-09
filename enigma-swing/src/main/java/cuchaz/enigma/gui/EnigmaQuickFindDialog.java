/*
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cuchaz.enigma.gui;

import cuchaz.enigma.gui.config.keybind.KeyBinds;
import cuchaz.enigma.gui.syntax.DocumentSearchData;
import cuchaz.enigma.gui.syntax.EscapeListener;
import cuchaz.enigma.gui.syntax.Markers;
import cuchaz.enigma.gui.util.GuiUtil;

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
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.lang.ref.WeakReference;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Reimplementation of to allow more customization.
 */
public class EnigmaQuickFindDialog extends JDialog implements DocumentListener, ActionListener, EscapeListener {
	private static final int SEARCH_FIELD_MAX_WIDTH = 200;
	private static final int SEARCH_FIELD_MAX_HEIGHT = 24;
	private static final int SEARCH_FIELD_MIN_WIDTH = 60;
	private static final int SEARCH_FIELD_MIN_HEIGHT = 24;
	private static final int PREFERRED_TOOLBAR_WIDTH = 684;

	private final Markers.SimpleMarker marker = new Markers.SimpleMarker(Color.PINK);
	private WeakReference<JTextComponent> target;
	private final WeakReference<DocumentSearchData> searchData;
	private int prevCaretPos;

	private JLabel statusLabel;
	private JTextField searchField;
	private JButton prevButton;
	private JButton nextButton;
	private JCheckBox ignoreCaseCheckBox;
	private JCheckBox regexCheckBox;
	private JCheckBox wrapCheckBox;

	public EnigmaQuickFindDialog(JTextComponent target) {
		this(target, DocumentSearchData.getFromEditor(target));
	}

	public EnigmaQuickFindDialog(JTextComponent target, DocumentSearchData searchData) {
		super(SwingUtilities.getWindowAncestor(target), ModalityType.MODELESS);

		this.initComponents();
		GuiUtil.addEscapeListener(this);
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
				target.getDocument().removeDocumentListener(EnigmaQuickFindDialog.this);
				Markers.removeMarkers(target, EnigmaQuickFindDialog.this.marker);
				EnigmaQuickFindDialog.this.removeWindowListener(this);
				EnigmaQuickFindDialog.this.setVisible(false);
				target.requestFocus();
			}
		};
		this.addWindowFocusListener(focusListener);

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

	private void initComponents() {
		JToolBar toolBar = new JToolBar();
		this.statusLabel = new JLabel();
		JLabel label = new JLabel();
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

		label.setLabelFor(this.searchField);
		label.setText("QuickFindDialog.jLabel1.text");
		toolBar.add(label);
		toolBar.addSeparator();

		this.searchField.setColumns(30);
		this.searchField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		this.searchField.setMaximumSize(new Dimension(SEARCH_FIELD_MAX_WIDTH, SEARCH_FIELD_MAX_HEIGHT));
		this.searchField.setMinimumSize(new Dimension(SEARCH_FIELD_MIN_WIDTH, SEARCH_FIELD_MIN_HEIGHT));
		this.searchField.addKeyListener(GuiUtil.onKeyPress(e -> {
			if (KeyBinds.QUICK_FIND_DIALOG_PREVIOUS.matches(e)) {
				this.prevButton.doClick();
			} else if (KeyBinds.QUICK_FIND_DIALOG_NEXT.matches(e)) {
				this.nextButton.doClick();
			}
		}));
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

		this.ignoreCaseCheckBox.setMnemonic(KeyBinds.QUICK_FIND_DIALOG_IGNORE_CASE.getKeyCode());
		this.ignoreCaseCheckBox.setText("QuickFindDialog.jChkIgnoreCase.text");
		this.ignoreCaseCheckBox.setFocusable(false);
		this.ignoreCaseCheckBox.setOpaque(false);
		this.ignoreCaseCheckBox.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.ignoreCaseCheckBox.addActionListener(this);
		toolBar.add(this.ignoreCaseCheckBox);

		this.regexCheckBox.setMnemonic(KeyBinds.QUICK_FIND_DIALOG_REGEX.getKeyCode());
		this.regexCheckBox.setText("QuickFindDialog.jChkRegExp.text");
		this.regexCheckBox.setFocusable(false);
		this.regexCheckBox.setOpaque(false);
		this.regexCheckBox.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.regexCheckBox.addActionListener(this);
		toolBar.add(this.regexCheckBox);

		this.wrapCheckBox.setMnemonic(KeyBinds.QUICK_FIND_DIALOG_WRAP.getKeyCode());
		this.wrapCheckBox.setText("QuickFindDialog.jChkWrap.text");
		this.wrapCheckBox.setFocusable(false);
		this.wrapCheckBox.setOpaque(false);
		this.wrapCheckBox.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.wrapCheckBox.addActionListener(this);
		toolBar.add(this.wrapCheckBox);

		toolBar.addSeparator();

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
		if (this.searchData.get().doFindPrev(this.target.get())) {
			this.statusLabel.setText(null);
		} else {
			this.statusLabel.setText("QuickFindDialog.NotFound");
		}
	}

	private void nextButtonActionPerformed(ActionEvent e) {
		if (this.searchData.get().doFindNext(this.target.get())) {
			this.statusLabel.setText(null);
		} else {
			this.statusLabel.setText("QuickFindDialog.NotFound");
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
