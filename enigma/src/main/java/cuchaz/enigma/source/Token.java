/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.source;

public class Token implements Comparable<Token> {
	public int start;
	public int end;
	public String text;

	public Token(int start, int end, String text) {
		this.start = start;
		this.end = end;
		this.text = text;
	}

	public int length() {
		return this.end - this.start;
	}

	public int getRenameOffset(String to) {
		return to.length() - this.length();
	}

	public void rename(StringBuilder source, String to) {
		int oldEnd = this.end;
		this.text = to;
		this.end = this.start + to.length();

		source.replace(this.start, oldEnd, to);
	}

	public Token move(int offset) {
		Token token = new Token(this.start + offset, this.end + offset, null);
		token.text = this.text;
		return token;
	}

	public boolean contains(int pos) {
		return pos >= this.start && pos <= this.end;
	}

	@Override
	public int compareTo(Token other) {
		return this.start - other.start;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Token token && this.equals(token);
	}

	@Override
	public int hashCode() {
		return this.start * 37 + this.end;
	}

	public boolean equals(Token other) {
		return this.start == other.start && this.end == other.end && this.text.equals(other.text);
	}

	@Override
	public String toString() {
		return String.format("[%d,%d]", this.start, this.end);
	}
}
