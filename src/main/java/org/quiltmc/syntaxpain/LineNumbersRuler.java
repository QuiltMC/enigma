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

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

import static javax.swing.BorderFactory.createEmptyBorder;

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
public class LineNumbersRuler extends JPanel implements CaretListener, DocumentListener, PropertyChangeListener {
	// issue #36 - avoid overflow on HiDPI monitors
	private static final int MAX_HEIGHT = 0x100000;
	private static final int MINIMUM_DISPLAY_DIGITS = 2;

	public static <R extends LineNumbersRuler> R install(R ruler) {
		ruler.editor.getDocument().addDocumentListener(ruler);
		ruler.editor.addCaretListener(ruler);
		ruler.editor.addPropertyChangeListener(ruler);
		final JScrollPane scrollPane = getScrollPane(ruler.editor);
		if (scrollPane != null) {
			scrollPane.setRowHeaderView(ruler);
		}

		return ruler;
	}

	/**
	 * Gets the Line Number at the give position of the editor component.
	 * The first line number is ZERO
	 *
	 * @return line number
	 */
	public static int getLineNumber(JTextComponent editor, int pos) {
		final SyntaxDocument doc = SyntaxDocument.getFrom(editor);
		if (doc != null) {
			return doc.getLineNumberAt(pos);
		} else {
			return editor.getDocument().getDefaultRootElement().getElementIndex(pos);
		}
	}

	public static int getLineCount(JTextComponent pane) {
		final SyntaxDocument doc = SyntaxDocument.getFrom(pane);
		if (doc != null) {
			return doc.getLineCount();
		}

		int count = 0;
		int p = pane.getDocument().getLength() - 1;
		if (p > 0) {
			count = getLineNumber(pane, p);
		}

		return count;
	}

	/**
	 * @return the {@link JScrollPane} of the {@link JEditorPane}, or null if there is no {@link JScrollPane} parent
	 */
	private static JScrollPane getScrollPane(JEditorPane editorPane) {
		Container p = editorPane.getParent();
		while (p != null) {
			if (p instanceof JScrollPane) {
				return (JScrollPane) p;
			}

			p = p.getParent();
		}

		return null;
	}

	// Text component this TextTextLineNumber component is in sync with
	protected final JEditorPane editor;
	protected final Color currentLineColor;
	protected final int lineOffset;

	//  Keep history information to reduce the number of times the component
	//  needs to be repainted
	private int lastDigits;
	private int lastHeight;
	private int lastLine;
	// The formatting to use for displaying numbers.  Use in String.format(numbersFormat, line)
	private String numbersFormat = "%3d";

	public LineNumbersRuler(JEditorPane editor, Color currentLineColor) {
		this(editor, currentLineColor, 0);
	}

	public LineNumbersRuler(JEditorPane editor, Color currentLineColor, int lineOffset) {
		this.editor = editor;
		this.currentLineColor = currentLineColor;
		this.lineOffset = lineOffset;

		final Insets editorInsets = this.editor.getInsets();
		this.setBorder(createEmptyBorder(editorInsets.top, 5, editorInsets.bottom, 5));

		// required for toggle-lines to correctly repaint
		this.setPreferredWidth(false);
	}

	/**
	 * Calculate the width needed to display the maximum line number
	 */
	private void setPreferredWidth(boolean force) {
		int lines = getLineCount(this.editor);
		int digits = Math.max(String.valueOf(lines + this.lineOffset).length(), MINIMUM_DISPLAY_DIGITS);

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

		final FontMetrics fontMetrics = this.getFontMetrics(this.getFont());
		final Insets insets = this.getInsets();
		final int currentLine = getLineNumber(this.editor, this.editor.getCaretPosition());

		final int lineHeight = fontMetrics.getHeight();
		final int maxLines = getLineCount(this.editor);
		SyntaxView.setRenderingHits((Graphics2D) g);

		final Rectangle clip = g.getClip().getBounds();
		final int topLine = (int) (clip.getY() / lineHeight);
		final int bottomLine = Math.min(maxLines, (int) (clip.getHeight() + lineHeight - 1) / lineHeight + topLine + 1);

		for (int line = topLine; line < bottomLine; line++) {
			final String lineNumber = String.format(this.numbersFormat, line + 1 + this.lineOffset);
			final int top = line * lineHeight + insets.top;
			final int stringBottom = top + fontMetrics.getAscent();
			if (line == currentLine) {
				g.setColor(this.currentLineColor);
				g.fillRect(0, top, this.getWidth(), lineHeight);
				g.setColor(this.getForeground());
				g.drawString(lineNumber, insets.left, stringBottom);
			} else {
				g.drawString(lineNumber, insets.left, stringBottom);
			}
		}
	}

	@Override
	public void caretUpdate(CaretEvent e) {
		// Get the line the caret is positioned on

		int caretPosition = this.editor.getCaretPosition();
		Element root = this.editor.getDocument().getDefaultRootElement();
		int currentLine = root.getElementIndex(caretPosition);

		// Need to repaint so the correct line number can be highlighted

		if (this.lastLine != currentLine) {
			this.repaint();
			this.lastLine = currentLine;
		}
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		this.documentChanged();
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		this.documentChanged();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		this.documentChanged();
	}

	private void documentChanged() {
		// Preferred size of the component has not been updated at the time
		// the DocumentEvent is fired

		SwingUtilities.invokeLater(() -> {
			int preferredHeight = this.editor.getPreferredSize().height;

			// Document change has caused a change in the number of lines.
			// Repaint to reflect the new line numbers

			if (this.lastHeight != preferredHeight) {
				this.setPreferredWidth(false);
				repaint();
				this.lastHeight = preferredHeight;
			}
		});
	}

	/**
	 * Implement PropertyChangeListener interface
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("font") && evt.getNewValue() instanceof Font) {
			this.setFont((Font) evt.getNewValue());
			this.setPreferredWidth(true);
		}
	}
}
