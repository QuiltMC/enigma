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

package cuchaz.enigma.gui.syntax;

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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This class will display line numbers for a related text component. The text
 * component must use the same line height for each line.
 * <p>
 * This class was designed to be used as a component added to the row header
 * of a JScrollPane.
 * <p>
 * Original code from http://tips4java.wordpress.com/2009/05/23/text-component-line-number/
 *
 * @author Rob Camick
 * <p>
 * Revised for de.sciss.syntaxpane
 * @author Ayman Al-Sairafi, Hanns Holger Rutz
 */
public class LineNumbersRuler extends JPanel
	implements CaretListener, DocumentListener, PropertyChangeListener, SyntaxComponent {

	public static final String PROPERTY_BACKGROUND = "LineNumbers.Background";
	public static final String PROPERTY_FOREGROUND = "LineNumbers.Foreground";
	public static final String PROPERTY_CURRENT_BACK = "LineNumbers.CurrentBack";
	public static final String PROPERTY_LEFT_MARGIN = "LineNumbers.LeftMargin";
	public static final String PROPERTY_RIGHT_MARGIN = "LineNumbers.RightMargin";
	public static final String PROPERTY_Y_OFFSET = "LineNumbers.YOFFset";
	public static final int DEFAULT_R_MARGIN = 5;
	public static final int DEFAULT_L_MARGIN = 5;
	private Status status;
	private final static int MAX_HEIGHT = 0x100000; // issue #36 - avoid overflow on HiDPI monitors
	//  Text component this TextTextLineNumber component is in sync with
	private JEditorPane editor;
	private int minimumDisplayDigits = 2;
	//  Keep history information to reduce the number of times the component
	//  needs to be repainted
	private int lastDigits;
	private int lastHeight;
	private int lastLine;
	private MouseListener mouseListener = null;
	// The formatting to use for displaying numbers.  Use in String.format(numbersFormat, line)
	private String numbersFormat = "%3d";

	private Color currentLineColor;

	private boolean isWordWrapEnabled;

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
	public void config(Configuration config) {
		int right = config.getInteger(PROPERTY_RIGHT_MARGIN, DEFAULT_R_MARGIN);
		int left  = config.getInteger(PROPERTY_LEFT_MARGIN , DEFAULT_L_MARGIN);
		Color foreground = config.getColor(PROPERTY_FOREGROUND, Color.BLACK);
		this.setForeground(foreground);
		Color back = config.getColor(PROPERTY_BACKGROUND, Color.WHITE);
		this.setBackground(back);
		this.setBorder(BorderFactory.createEmptyBorder(0, left, 0, right));
		this.currentLineColor = config.getColor(PROPERTY_CURRENT_BACK, back);
		this.isWordWrapEnabled = config.getBoolean(DefaultSyntaxKit.CONFIG_ENABLE_WORD_WRAP, false);
	}

	@Override
	public void install(final JEditorPane editor) {
		this.editor = editor;

		this.setFont(editor.getFont());

		// setMinimumDisplayDigits(3);
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
		this.mouseListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				//GotoLineDialog.showForEditor(editor);
			}
		};
		this.addMouseListener(this.mouseListener);
		this.setPreferredWidth(false);    // required for toggle-lines to correctly repaint
		this.status = Status.INSTALLING;
	}

	@Override
	public void deinstall(JEditorPane editor) {
		this.removeMouseListener(this.mouseListener);
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
	 * Gets the minimum display digits
	 *
	 * @return the minimum display digits
	 */
	public int getMinimumDisplayDigits() {
		return this.minimumDisplayDigits;
	}

	/**
	 * Specify the minimum number of digits used to calculate the preferred
	 * width of the component. Default is 3.
	 *
	 * @param minimumDisplayDigits the number digits used in the preferred
	 *                             width calculation
	 */
	public void setMinimumDisplayDigits(int minimumDisplayDigits) {
		this.minimumDisplayDigits = minimumDisplayDigits;
		this.setPreferredWidth(false);
	}

	/**
	 * Calculate the width needed to display the maximum line number
	 */
	private void setPreferredWidth(boolean force) {
		int lines = ActionUtils.getLineCount(this.editor);
		int digits = Math.max(String.valueOf(lines).length(), this.minimumDisplayDigits);

		//  Update sizes when number of digits in the line number changes

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
	 * Draw the line numbers
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		FontMetrics fontMetrics = this.getFontMetrics(this.getFont());
		Insets insets = this.getInsets();
		int currentLine = -1;
		try {
			currentLine = ActionUtils.getLineNumber(this.editor, this.editor.getCaretPosition());
		} catch (BadLocationException ex) {
			// this won't happen, even if it does, we can ignore it and we will not have
			// a current line to worry about...
		}

		SyntaxView.setRenderingHits((Graphics2D) g);

		Rectangle clip = g.getClip().getBounds();
		int topMargin = this.getInsets().top;
		int leftMargin = this.editor.getInsets().left;
		int lh = fontMetrics.getHeight();
		int topY = Math.max((clip.y - topMargin) / lh * lh, 0) + topMargin;
		int bottomY;
		if (this.isWordWrapEnabled) {
			bottomY = Math.min((clip.y - topMargin + clip.height) / lh * lh + topMargin + lh, this.editor.getHeight());
		} else {
			int topLine = (int) (clip.getY() / lh);
			int maxLines = ActionUtils.getLineCount(this.editor);
			int bottomLine = Math.min(maxLines, (int) (clip.getHeight() + lh - 1) / lh + topLine + 1);
			bottomY = bottomLine * lh;
		}

		Point p = new Point();
		p.x = leftMargin;
		int pos = this.editor.viewToModel(p);
		int previousLine = this.getLineNumber(pos) - 1;
		for (int y = topY; y < bottomY; y += lh) {
			int line;
			if (this.isWordWrapEnabled) {
				p.y = y;
				pos = this.editor.viewToModel(p);
				if (this.getY(pos) < y) break;
				line = this.getLineNumber(pos);
			} else {
				line = previousLine + 1;
			}
			if (line == currentLine) {
				g.setColor(this.currentLineColor);
				g.fillRect(0, y /* - lh + fontMetrics.getDescent() - 1 */, this.getWidth(), lh);
				g.setColor(this.getForeground());
			}
			if (!this.isWordWrapEnabled || line > previousLine && (y > topY || pos == 0 || line > this.getLineNumber(pos - 1))) {
				previousLine = line;
				String lineNumber = String.format(this.numbersFormat, line + 1);
				int yt = y + fontMetrics.getAscent();
				g.drawString(lineNumber, insets.left, yt);
			}
		}
	}

	private int getY(int pos) {
		try {
			return this.editor.modelToView(pos).y;
		} catch (BadLocationException e) {
			return -1;
		}
	}


	private int getLineNumber(int pos) {
		try {
			return ActionUtils.getLineNumber(this.editor, pos);
		} catch (BadLocationException e) {
			return -1;
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

	//
//  Implement DocumentListener interface
//
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

	/*
	 *  A document change may affect the number of displayed lines of text.
	 *  Therefore, the lines numbers will also change.
	 */
	private void documentChanged() {
		//  Preferred size of the component has not been updated at the time
		//  the DocumentEvent is fired

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				int preferredHeight = LineNumbersRuler.this.editor.getPreferredSize().height;

				//  Document change has caused a change in the number of lines.
				//  Repaint to reflect the new line numbers

				if (LineNumbersRuler.this.lastHeight != preferredHeight) {
					LineNumbersRuler.this.setPreferredWidth(false);
					LineNumbersRuler.this.repaint();
					LineNumbersRuler.this.lastHeight = preferredHeight;
				}
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
				SyntaxDocument sdoc = (SyntaxDocument) evt.getOldValue();
				sdoc.removeDocumentListener(this);
			}
			if (evt.getNewValue() instanceof SyntaxDocument && this.status.equals(Status.INSTALLING)) {
				SyntaxDocument sdoc = (SyntaxDocument) evt.getNewValue();
				sdoc.addDocumentListener(this);
				this.setPreferredWidth(false);
				this.repaint();
			}
		} else if (prop.equals("font") && evt.getNewValue() instanceof Font) {
			this.setFont((Font) evt.getNewValue());
			this.setPreferredWidth(true);
		}
		// TODO - theoretically also track "insets"
	}
}
