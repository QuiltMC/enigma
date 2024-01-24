/*
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
 * Copyright 2011-2022 Hanns Holger Rutz.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License
 *       at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quiltmc.syntaxpain;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This class will display line numbers for a related text component. The text
 * component must use the same line height for each line.
 * This class was designed to be used as a component added to the row header
 * of a JScrollPane.
 * Original code from <a href="http://tips4java.wordpress.com/2009/05/23/text-component-line-number/">a tips4java article</a>
 *
 * @author Rob Camick
 *
 * @author Ayman Al-Sairafi, Hanns Holger Rutz
 */
public class LineNumbersRuler extends JPanel implements CaretListener, DocumentListener, PropertyChangeListener, SyntaxComponent {
	private Status status;
	private static final int MAX_HEIGHT = 0x100000; // issue #36 - avoid overflow on HiDPI monitors
	//  Text component this TextTextLineNumber component is in sync with
	private JEditorPane editor;
	private static final int MINIMUM_DISPLAY_DIGITS = 2;
	//  Keep history information to reduce the number of times the component
	//  needs to be repainted
	private int lastDigits;
	private int lastHeight;
	private int lastLine;
	// The formatting to use for displaying numbers.  Use in String.format(numbersFormat, line)
	private String numbersFormat = "%3d";

	private Color currentLineColor;

	/**
	 * Returns the JScrollPane that contains this EditorPane, or null if no
	 * JScrollPane is the parent of this editor
	 */
	public JScrollPane getScrollPane(JTextComponent editorPane) {
		Container p = editorPane.getParent();
		while (p != null) {
			if (p instanceof JScrollPane) {
				return (JScrollPane) p;
			}

			p = p.getParent();
		}

		return null;
	}

	@Override
	public void configure() {
		Color foreground = SyntaxpainConfiguration.getLineRulerPrimaryColor();
		this.setForeground(foreground);
		Color back = SyntaxpainConfiguration.getLineRulerSecondaryColor();
		this.setBackground(back);
		this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		this.currentLineColor = SyntaxpainConfiguration.getLineRulerSelectionColor();
	}

	@Override
	public void install(final JEditorPane editor) {
		this.editor = editor;

		this.setFont(editor.getFont());

		Insets ein = editor.getInsets();
		if (ein.top != 0 || ein.bottom != 0) {
			Insets curr = this.getInsets();
			this.setBorder(BorderFactory.createEmptyBorder(ein.top, curr.left, ein.bottom, curr.right));
		}

		editor.getDocument().addDocumentListener(this);
		editor.addCaretListener(this);
		editor.addPropertyChangeListener(this);
		JScrollPane sp = this.getScrollPane(editor);
		if (sp != null) sp.setRowHeaderView(this);
		this.setPreferredWidth(false);    // required for toggle-lines to correctly repaint
		this.status = Status.INSTALLING;
	}

	@Override
	public void deinstall(JEditorPane editor) {
		this.status = Status.DEINSTALLING;
		editor.getDocument().removeDocumentListener(this);
		editor.removeCaretListener(this);
		editor.removePropertyChangeListener(this);
		JScrollPane sp = this.getScrollPane(editor);
		if (sp != null) {
			sp.setRowHeaderView(null);
		}
	}

	/**
	 * Calculate the width needed to display the maximum line number
	 */
	private void setPreferredWidth(boolean force) {
		int lines = Util.getLineCount(this.editor);
		int digits = Math.max(String.valueOf(lines).length(), MINIMUM_DISPLAY_DIGITS);

		// Update sizes when number of digits in the line number changes

		if (force || this.lastDigits != digits) {
			this.lastDigits = digits;
			this.numbersFormat = "%" + digits + "d";
			FontMetrics fontMetrics = this.getFontMetrics(this.getFont());
			int width = fontMetrics.charWidth('0') * digits;
			Insets insets = this.getInsets();
			int preferredWidth = insets.left + insets.right + width;

			Dimension d = this.getPreferredSize();
			d.setSize(preferredWidth, MAX_HEIGHT);
			this.setPreferredSize(d);
			this.setSize(d);
		}
	}

	/**
	 *  Draw the line numbers
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		FontMetrics fontMetrics = this.getFontMetrics(this.getFont());
		Insets insets = this.getInsets();
		int currentLine = -1;
		try {
			currentLine = Util.getLineNumber(this.editor, this.editor.getCaretPosition());
		} catch (BadLocationException ex) {
			// this won't happen, even if it does, we can ignore it and we will not have
			// a current line to worry about...
		}

		int lh = fontMetrics.getHeight();
		int maxLines = Util.getLineCount(this.editor);
		SyntaxView.setRenderingHits((Graphics2D) g);

		Rectangle clip = g.getClip().getBounds();
		int topLine = (int) (clip.getY() / lh);
		int bottomLine = Math.min(maxLines, (int) (clip.getHeight() + lh - 1) / lh + topLine + 1);

		for (int line = topLine; line < bottomLine; line++) {
			String lineNumber = String.format(this.numbersFormat, line + 1);
			int y = line * lh + insets.top;
			int yt = y + fontMetrics.getAscent();
			if (line == currentLine) {
				g.setColor(this.currentLineColor);
				g.fillRect(0, y, this.getWidth(), lh);
				g.setColor(this.getForeground());
				g.drawString(lineNumber, insets.left, yt);
			} else {
				g.drawString(lineNumber, insets.left, yt);
			}
		}
	}

	//
//  Implement CaretListener interface
//
	@Override
	public void caretUpdate(CaretEvent e) {
		//  Get the line the caret is positioned on

		int caretPosition = this.editor.getCaretPosition();
		Element root = this.editor.getDocument().getDefaultRootElement();
		int currentLine = root.getElementIndex(caretPosition);

		//  Need to repaint so the correct line number can be highlighted

		if (this.lastLine != currentLine) {
			this.repaint();
			this.lastLine = currentLine;
		}
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		documentChanged();
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		documentChanged();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		documentChanged();
	}

	private void documentChanged() {
		// Preferred size of the component has not been updated at the time
		// the DocumentEvent is fired

		SwingUtilities.invokeLater(() -> {
			int preferredHeight = editor.getPreferredSize().height;

			// Document change has caused a change in the number of lines.
			// Repaint to reflect the new line numbers

			if (lastHeight != preferredHeight) {
				setPreferredWidth(false);
				repaint();
				lastHeight = preferredHeight;
			}
		});
	}

	/**
	 * Implement PropertyChangeListener interface
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (prop.equals("document")) {
			if (evt.getOldValue() instanceof SyntaxDocument) {
				SyntaxDocument syntaxDocument = (SyntaxDocument) evt.getOldValue();
				syntaxDocument.removeDocumentListener(this);
			}

			if (evt.getNewValue() instanceof SyntaxDocument && this.status.equals(Status.INSTALLING)) {
				SyntaxDocument syntaxDocument = (SyntaxDocument) evt.getNewValue();
				syntaxDocument.addDocumentListener(this);
				this.setPreferredWidth(false);
				this.repaint();
			}
		} else if (prop.equals("font") && evt.getNewValue() instanceof Font) {
			this.setFont((Font) evt.getNewValue());
			this.setPreferredWidth(true);
		}
	}
}