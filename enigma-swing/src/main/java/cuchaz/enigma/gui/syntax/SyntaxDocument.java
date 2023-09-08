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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;

/**
 * A document that supports being highlighted.  The document maintains an
 * internal List of all the Tokens.  The Tokens are updated using
 * a Lexer, passed to it during construction.
 *
 * @author Ayman Al-Sairafi, Hanns Holger Rutz
 */
public class SyntaxDocument extends PlainDocument {
	public static final String CAN_UNDO = "can-undo";
	public static final String CAN_REDO = "can-redo";

	Lexer lexer;
	List<Token> tokens;

	private final PropertyChangeSupport propSupport;
	private boolean canUndoState = false;
	private boolean canRedoState = false;

	private int earliestTokenChangePos = -1;
	private int latestTokenChangePos = -1;

	public SyntaxDocument(Lexer lexer) {
		super();
		this.putProperty(PlainDocument.tabSizeAttribute, 4);
		this.lexer  = lexer;
		this.propSupport = new PropertyChangeSupport(this);
	}

	/*
	 * Parse the entire document and return list of tokens that do not already
	 * exist in the tokens list.  There may be overlaps, and replacements,
	 * which we will cleanup later.
	 *
	 * @return list of tokens that do not exist in the tokens field
	 */
	private void parse(DocumentEvent event) {
		// if we have no lexer, then we must have no tokens...
		if (this.lexer == null) {
			this.tokens = null;
			return;
		}
		List<Token> oldTokens = this.tokens;

		List<Token> toks = new ArrayList<Token>(this.getLength() / 10);
		long ts = System.nanoTime();
		int len = this.getLength();
		try {
			Segment seg = new Segment();
			this.getText(0, this.getLength(), seg);
			this.lexer.parse(seg, 0, toks);
		} catch (BadLocationException ex) {
			log.log(Level.SEVERE, null, ex);
		} finally {
			if (log.isLoggable(Level.FINEST)) {
				log.finest(String.format("Parsed %d in %d ms, giving %d tokens\n",
					len, (System.nanoTime() - ts) / 1000000, toks.size()));
			}
			this.tokens = toks;
			this.calculateEarliestAndLatestTokenChangePos(event, oldTokens, toks);
		}
	}

	// Note: For this calculation we are "cheating" a bit since we do not consider actual token
	// string content. This works in practice because if we change content, the normal Swing code will ensure
	// that repaint happens, but if anyone tried to use this information for something beyond calculating
	// needed repaints, we would have issues.
	private void calculateEarliestAndLatestTokenChangePos(DocumentEvent change, List<Token> oldTokens, List<Token> newTokens) {
		if (oldTokens == null || change == null ||
			oldTokens.isEmpty() || newTokens.isEmpty()) {
			// Not enough info for a diff
			this.earliestTokenChangePos = 0;
			this.latestTokenChangePos = this.getLength();
			return;
		}

		// First calculate the first point of difference
		int pos = 0;
		ListIterator<Token> oldIter = oldTokens.listIterator();
		ListIterator<Token> newIter = newTokens.listIterator();
		while (oldIter.hasNext() && newIter.hasNext()) {
			Token oldToken = oldIter.next();
			Token newToken = newIter.next();
			if (oldToken.equals(newToken)) {
				pos = newToken.end();
			} else {
				pos = newToken.start;
				break;
			}
		}
		if (this.earliestTokenChangePos < 0 || this.earliestTokenChangePos > pos) {
			this.earliestTokenChangePos = pos;
		}

		// Now we need to decide if it is safe to scan tokens from the last
		// for old and new tokens. This works if the last token "matches"
		// (ie start matches as expected depending on operation), but not
		// otherwise since one or both of the parsings may have failed
		// and not parsed equally far
		boolean canScanBackwards = false;

		if (change != null) {
			Token lastNew = newTokens.get(newTokens.size() - 1);
			Token lastOld = oldTokens.get(oldTokens.size() - 1);
			int oldStart;
			if (lastOld.start < change.getOffset()) {
				oldStart = lastOld.start;
			} else if (DocumentEvent.EventType.INSERT.equals(change.getType())) {
				oldStart = lastOld.start + change.getLength();
			} else if (DocumentEvent.EventType.REMOVE.equals(change.getType())) {
				oldStart = lastOld.start - change.getLength();
			} else {
				// Unexpected event.
				oldStart = -1;
			}

			canScanBackwards = oldStart == lastNew.start;
		}

		pos = this.getLength();
		if (canScanBackwards) {
			int searchCutoff = Math.max(this.earliestTokenChangePos, this.latestTokenChangePos);

			oldIter = oldTokens.listIterator(oldTokens.size());
			newIter = newTokens.listIterator(newTokens.size());
			while (oldIter.hasPrevious() && newIter.hasPrevious() &&
				pos > searchCutoff) {
				Token oldToken = oldIter.previous();
				Token newToken = newIter.previous();
				if (oldToken.type == newToken.type &&
					oldToken.length == newToken.length) {
					pos = newToken.start;
				} else {
					pos = newToken.end();
					break;
				}
			}
		}
		if (this.latestTokenChangePos < pos) {
			this.latestTokenChangePos = pos;
		}
	}

	public int getAndClearEarliestTokenChangePos() {
		int pos = this.earliestTokenChangePos;
		this.earliestTokenChangePos = -1;
		return pos;
	}

	public int getAndClearLatestTokenChangePos() {
		int pos = this.latestTokenChangePos;
		this.latestTokenChangePos = -1;
		return Math.min(pos, this.getLength());
	}

	@Override
	protected void fireChangedUpdate(DocumentEvent e) {
		this.parse(e);
		super.fireChangedUpdate(e);
	}

	@Override
	protected void fireInsertUpdate(DocumentEvent e) {
		this.parse(e);
		super.fireInsertUpdate(e);
	}

	@Override
	protected void fireRemoveUpdate(DocumentEvent e) {
		this.parse(e);
		super.fireRemoveUpdate(e);
	}

	/**
	 * Replaces the token with the replacement string
	 */
	public void replaceToken(Token token, String replacement) {
		try {
			this.replace(token.start, token.length, replacement, null);
		} catch (BadLocationException ex) {
			log.log(Level.WARNING, "unable to replace token: " + token, ex);
		}
	}

	/**
	 * This class is used to iterate over tokens between two positions
	 */
	class TokenIterator implements ListIterator<Token> {

		int start;
		int end;
		int ndx = 0;

		@SuppressWarnings("unchecked")
		private TokenIterator(int start, int end) {
			this.start = start;
			this.end = end;
			if (SyntaxDocument.this.tokens != null && !SyntaxDocument.this.tokens.isEmpty()) {
				Token token = new Token(TokenType.COMMENT, start, end - start);
				this.ndx = Collections.binarySearch((List) SyntaxDocument.this.tokens, token);
				// we will probably not find the exact token...
				if (this.ndx < 0) {
					// so, start from one before the token where we should be...
					// -1 to get the location, and another -1 to go back..
					this.ndx = (-this.ndx - 1 - 1 < 0) ? 0 : (-this.ndx - 1 - 1);
					Token t = SyntaxDocument.this.tokens.get(this.ndx);
					// if the prev token does not overlap, then advance one
					if (t.end() <= start) {
						this.ndx++;
					}

				}
			}
		}

		@Override
		public boolean hasNext() {
			if (SyntaxDocument.this.tokens == null) {
				return false;
			}
			if (this.ndx >= SyntaxDocument.this.tokens.size()) {
				return false;
			}
			Token t = SyntaxDocument.this.tokens.get(this.ndx);
			return t.start < this.end;
		}

		@Override
		public Token next() {
			return SyntaxDocument.this.tokens.get(this.ndx++);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasPrevious() {
			if (SyntaxDocument.this.tokens == null) {
				return false;
			}
			if (this.ndx <= 0) {
				return false;
			}
			Token t = SyntaxDocument.this.tokens.get(this.ndx);
			return t.end() > this.start;
		}

		@Override
		public Token previous() {
			return SyntaxDocument.this.tokens.get(this.ndx--);
		}

		@Override
		public int nextIndex() {
			return this.ndx + 1;
		}

		@Override
		public int previousIndex() {
			return this.ndx - 1;
		}

		@Override
		public void set(Token e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(Token e) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Returns an iterator of tokens between p0 and p1.
	 * @param start start position for getting tokens
	 * @param end position for last token
	 * @return Iterator for tokens that overall with range from start to end
	 */
	public Iterator<Token> getTokens(int start, int end) {
		return new TokenIterator(start, end);
	}

	/**
	 * Finds the token at a given position.  May return null if no token is
	 * found (whitespace skipped) or if the position is out of range:
	 */
	public Token getTokenAt(int pos) {
		if (this.tokens == null || this.tokens.isEmpty() || pos > this.getLength()) {
			return null;
		}
		Token tok = null;
		Token tKey = new Token(TokenType.DEFAULT, pos, 1);
		@SuppressWarnings("unchecked")
		int ndx = Collections.binarySearch((List) this.tokens, tKey);
		if (ndx < 0) {
			// so, start from one before the token where we should be...
			// -1 to get the location, and another -1 to go back..
			ndx = (-ndx - 1 - 1 < 0) ? 0 : (-ndx - 1 - 1);
			Token t = this.tokens.get(ndx);
			if ((t.start <= pos) && (pos <= t.end())) {
				tok = t;
			}
		} else {
			tok = this.tokens.get(ndx);
		}
		return tok;
	}

	public Token getWordAt(int offs, Pattern p) {
		Token word = null;
		try {
			Element line = this.getParagraphElement(offs);
			if (line == null) {
				return null;
			}
			int lineStart = line.getStartOffset();
			int lineEnd = Math.min(line.getEndOffset(), this.getLength());
			Segment seg = new Segment();
			this.getText(lineStart, lineEnd - lineStart, seg);
			if (seg.count > 0) {
				// we need to get the word using the words pattern p
				Matcher m = p.matcher(seg);
				int o = offs - lineStart;
				while (m.find()) {
					if (m.start() <= o && o <= m.end()) {
						word = new Token(TokenType.DEFAULT, m.start() + lineStart, m.end() - m.start());
						break;
					}
				}
			}
		} catch (BadLocationException ex) {
			Logger.getLogger(SyntaxDocument.class.getName()).log(Level.SEVERE, null, ex);
		}
		return word;
	}

	/**
	 * Returns the token following the current token, or null
	 * <b>This is an expensive operation, so do not use it to update the gui</b>
	 */
	public Token getNextToken(Token tok) {
		int n = this.tokens.indexOf(tok);
		if ((n >= 0) && (n < (this.tokens.size() - 1))) {
			return this.tokens.get(n + 1);
		} else {
			return null;
		}
	}

	/**
	 * Returns the token prior to the given token, or null
	 * <b>This is an expensive operation, so do not use it to update the gui</b>
	 */
	public Token getPrevToken(Token tok) {
		int n = this.tokens.indexOf(tok);
		if ((n > 0) && (!this.tokens.isEmpty())) {
			return this.tokens.get(n - 1);
		} else {
			return null;
		}
	}

	/**
	 * This is used to return the other part of a paired token in the document.
	 * A paired part has token.pairValue &lt;&gt; 0, and the paired token will
	 * have the negative of t.pairValue.
	 * This method properly handles nestings of same pairValues, but overlaps
	 * are not checked.
	 * if the document does not contain a paired token, then null is returned.
	 *
	 * @return the other pair's token, or null if nothing is found.
	 */
	public Token getPairFor(Token t) {
		if (t == null || t.pairValue == 0) {
			return null;
		}
		Token p = null;
		int ndx = this.tokens.indexOf(t);
		// w will be similar to a stack. The openners weght is added to it
		// and the closers are subtracted from it (closers are already negative)
		int w = t.pairValue;
		int direction = (t.pairValue > 0) ? 1 : -1;
		boolean done = false;
		int v = Math.abs(t.pairValue);
		while (!done) {
			ndx += direction;
			if (ndx < 0 || ndx >= this.tokens.size()) {
				break;
			}
			Token current = this.tokens.get(ndx);
			if (Math.abs(current.pairValue) == v) {
				w += current.pairValue;
				if (w == 0) {
					p = current;
					done = true;
				}
			}
		}

		return p;
	}

	// public boolean isDirty() { return dirty; }

	public void setCanUndo(boolean value) {
		if (this.canUndoState != value) {
			// System.out.println("canUndo = " + value);
			this.canUndoState = value;
			this.propSupport.firePropertyChange(CAN_UNDO, !value, value);
		}
	}

	public void setCanRedo(boolean value) {
		if (this.canRedoState != value) {
			// System.out.println("canRedo = " + value);
			this.canRedoState = value;
			this.propSupport.firePropertyChange(CAN_REDO, !value, value);
		}
	}

	public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
		// System.out.println("ADD " + property + " " + listener.hashCode() + " / " + this.hashCode());
		this.propSupport.addPropertyChangeListener(property, listener);
	}

	public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
		// System.out.println("REM " + property + " " + listener.hashCode() + " / " + this.hashCode());
		this.propSupport.removePropertyChangeListener(property, listener);
	}


	/**
	 * Returns a matcher that matches the given pattern on the entire document
	 *
	 * @return matcher object
	 */
	public Matcher getMatcher(Pattern pattern) {
		return this.getMatcher(pattern, 0, this.getLength());
	}

	/**
	 * Returns a matcher that matches the given pattern in the part of the
	 * document starting at offset start.  Note that the matcher will have
	 * offset starting from <code>start</code>
	 *
	 * @return  matcher that <b>MUST</b> be offset by start to get the proper
	 *          location within the document
	 */
	public Matcher getMatcher(Pattern pattern, int start) {
		return this.getMatcher(pattern, start, this.getLength() - start);
	}

	/**
	 * Returns a matcher that matches the given pattern in the part of the
	 * document starting at offset start and ending at start + length.
	 * Note that the matcher will have
	 * offset starting from <code>start</code>
	 *
	 * @return matcher that <b>MUST</b> be offset by start to get the proper location within the document
	 */
	public Matcher getMatcher(Pattern pattern, int start, int length) {
		Matcher matcher = null;
		if (this.getLength() == 0) {
			return null;
		}
		if (start >= this.getLength()) {
			return null;
		}
		try {
			if (start < 0) {
				start = 0;
			}
			if (start + length > this.getLength()) {
				length = this.getLength() - start;
			}
			Segment seg = new Segment();
			this.getText(start, length, seg);
			matcher = pattern.matcher(seg);
		} catch (BadLocationException ex) {
			log.log(Level.SEVERE, "Requested offset: " + ex.offsetRequested(), ex);
		}
		return matcher;
	}

	/**
	 * Gets the line at given position.  The line returned will NOT include
	 * the line terminator '\n'
	 * @param pos Position (usually from text.getCaretPosition()
	 * @return the String of text at given position
	 */
	public String getLineAt(int pos) throws BadLocationException {
		Element e = this.getParagraphElement(pos);
		Segment seg = new Segment();
		this.getText(e.getStartOffset(), e.getEndOffset() - e.getStartOffset(), seg);
		char last = seg.last();
		if (last == '\n' || last == '\r') {
			seg.count--;
		}
		return seg.toString();
	}

	/**
	 * Deletes the line at given position
	 */
	public void removeLineAt(int pos)
		throws BadLocationException {
		Element e = this.getParagraphElement(pos);
		this.remove(e.getStartOffset(), this.getElementLength(e));
	}

	/**
	 * Replaces the line at given position with the given string, which can span
	 * multiple lines
	 */
	public void replaceLineAt(int pos, String newLines)
		throws BadLocationException {
		Element e = this.getParagraphElement(pos);
		this.replace(e.getStartOffset(), this.getElementLength(e), newLines, null);
	}

	/*
	 * Helper method to get the length of an element and avoid getting
	 * a too long element at the end of the document
	 */
	private int getElementLength(Element e) {
		int end = e.getEndOffset();
		if (end >= (this.getLength() - 1)) {
			end--;
		}
		return end - e.getStartOffset();
	}

	/**
	 * Gets the text without the comments. For example for the string
	 * <code>{ // it's a comment</code> this method will return "{ ".
	 * @param aStart start of the text.
	 * @param anEnd end of the text.
	 * @return String for the line without comments (if exists).
	 */
	public synchronized String getUncommentedText(int aStart, int anEnd) {
		this.readLock();
		StringBuilder result = new StringBuilder();
		Iterator<Token> iter = this.getTokens(aStart, anEnd);
		while (iter.hasNext()) {
			Token t = iter.next();
			if (!TokenType.isComment(t)) {
				result.append(t.getText(this));
			}
		}
		this.readUnlock();
		return result.toString();
	}

	public int getOffsetAtLineStart(int line) {
		Element lineMap = this.getDefaultRootElement();
		if (line < 0)
			return 0;
		int lineCount = lineMap.getElementCount();
		return lineMap.getElement(Math.min(line, lineCount - 1)).getStartOffset();
	}


	/**
	 * Returns the starting position of the line at pos
	 *
	 * @return starting position of the line
	 */
	public int getLineStartOffset(int pos) {
		return this.getParagraphElement(pos).getStartOffset();
	}

	/**
	 * Returns the end position of the line at pos.
	 * Does a bounds check to ensure the returned value does not exceed
	 * document length
	 */
	public int getLineEndOffset(int pos) {
		int end = 0;
		end = this.getParagraphElement(pos).getEndOffset();
		if (end >= this.getLength()) {
			end = this.getLength();
		}
		return end;
	}

	/**
	 * Returns the number of lines in this document
	 */
	public int getLineCount() {
		Element e = this.getDefaultRootElement();
		return e.getElementCount();
	}

	/**
	 * Returns the line number at given position.  The line numbers are zero based
	 */
	public int getLineNumberAt(int pos) {
		return this.getDefaultRootElement().getElementIndex(pos);
	}

	@Override
	public String toString() {
		return "SyntaxDocument(" + this.lexer + ", " + ((this.tokens == null) ? 0 : this.tokens.size()) + " tokens)@" +
			this.hashCode();
	}

	/**
	 * We override this here so that the replace is treated as one operation
	 * by the undo manager
	 */
	@Override
	public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
		this.remove(offset, length);
		this.insertString(offset, text, attrs);
	}

	/**
	 * Appends the given string to the text of this document.
	 *
	 * @return this document
	 */
	public SyntaxDocument append(String str) {
		try {
			this.insertString(this.getLength(), str, null);
		} catch (BadLocationException ex) {
			log.log(Level.WARNING, "Error appending str", ex);
		}
		return this;
	}

	// our logger instance...
	private static final Logger log = Logger.getLogger(SyntaxDocument.class.getName());
}
