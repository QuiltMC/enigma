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
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
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
	/**
	 * A helper function that will return the SyntaxDocument attached to the
	 * given text component.  Return null if the document is not a
	 * SyntaxDocument, or if the text component is null
	 */
	public static SyntaxDocument getFrom(JTextComponent component) {
		if (component == null) {
			return null;
		}

		Document doc = component.getDocument();
		if (doc instanceof SyntaxDocument) {
			return (SyntaxDocument) doc;
		} else {
			return null;
		}
	}

	private final Lexer lexer;
	private List<Token> tokens;

	private int earliestTokenChangePos = -1;
	private int latestTokenChangePos = -1;

	public SyntaxDocument(Lexer lexer) {
		super();
		this.putProperty(PlainDocument.tabSizeAttribute, 4);
		this.lexer = lexer;
	}

	/*
	 * Parse the entire document and return list of tokens that do not already
	 * exist in the tokens list.  There may be overlaps, and replacements,
	 * which we will clean up later.
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

		List<Token> toks = new ArrayList<>(this.getLength() / 10);
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
		if (oldTokens == null || change == null || oldTokens.isEmpty() || newTokens.isEmpty()) {
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
		boolean canScanBackwards;

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

		pos = this.getLength();
		if (canScanBackwards) {
			int searchCutoff = Math.max(this.earliestTokenChangePos, this.latestTokenChangePos);

			oldIter = oldTokens.listIterator(oldTokens.size());
			newIter = newTokens.listIterator(newTokens.size());
			while (oldIter.hasPrevious() && newIter.hasPrevious()
				&& pos > searchCutoff) {
				Token oldToken = oldIter.previous();
				Token newToken = newIter.previous();
				if (oldToken.type == newToken.type && oldToken.length == newToken.length) {
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
	 * This class is used to iterate over tokens between two positions
	 */
	class TokenIterator implements ListIterator<Token> {
		int start;
		int end;
		int ndx = 0;

		private TokenIterator(int start, int end) {
			this.start = start;
			this.end = end;
			if (SyntaxDocument.this.tokens != null && !SyntaxDocument.this.tokens.isEmpty()) {
				Token token = new Token(TokenType.COMMENT, start, end - start);
				this.ndx = Collections.binarySearch(SyntaxDocument.this.tokens, token);
				// we will probably not find the exact token...
				if (this.ndx < 0) {
					// so, start from one before the token where we should be...
					// -1 to get the location, and another -1 to go back..
					this.ndx = Math.max(-this.ndx - 1 - 1, 0);
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
		int ndx = Collections.binarySearch(this.tokens, tKey);
		if (ndx < 0) {
			// so, start from one before the token where we should be...
			// -1 to get the location, and another -1 to go back..
			ndx = Math.max(-ndx - 1 - 1, 0);
			Token t = this.tokens.get(ndx);
			if ((t.start <= pos) && (pos <= t.end())) {
				tok = t;
			}
		} else {
			tok = this.tokens.get(ndx);
		}

		return tok;
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
		if (this.getLength() == 0 || start >= this.getLength()) {
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
		return "SyntaxDocument(" + this.lexer + ", " + ((this.tokens == null) ? 0 : this.tokens.size()) + " tokens)@"
				+ this.hashCode();
	}

	// our logger instance...
	private static final Logger log = Logger.getLogger(SyntaxDocument.class.getName());
}
