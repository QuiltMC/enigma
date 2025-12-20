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

import java.io.Serializable;

/**
 * A Token in a Document.  Tokens do NOT store a reference to the
 * underlying SyntaxDocument, and must generally be obtained from
 * the SyntaxDocument methods.  The reason for not storing the
 * SyntaxDocument is simply for memory, as the number of Tokens
 * per document can be large, you may end up with twice the memory
 * in a SyntaxDocument with Tokens than a simple PlainDocument.
 *
 * @author Ayman Al-Sairafi, Hanns Holger Rutz
 */
public class Token implements Serializable, Comparable<Token> {
	public final TokenType type;
	public final int start;
	public final int length;
	/**
	 * the pair value to use if this token is one of a pair:
	 * This is how it is used:
	 * The opening part will have a positive number X
	 * The closing part will have a negative number X
	 * X should be unique for a pair:
	 *   e.g. for [ pairValue = +1
	 *        for ] pairValue = -1
	 */
	public final byte pairValue;
	/**
	 * The kind of the Document.  This is only needed if proper Parsing
	 * of a document is needed and it makes certain operations faster.
	 * You can use any of the supplied Generic Values, or create your
	 * language specific uses by using USER_FIRST + x;
	 */
	public final short kind = 0;

	/**
	 * Constructs a new token
	 */
	public Token(TokenType type, int start, int length) {
		this.type = type;
		this.start = start;
		this.length = length;
		this.pairValue = 0;
	}

	/**
	 * Construct a new part of pair token
	 */
	public Token(TokenType type, int start, int length, byte pairValue) {
		this.type = type;
		this.start = start;
		this.length = length;
		this.pairValue = pairValue;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null) {
			Token token = (Token) obj;
			return ((this.start == token.start)
					&& (this.length == token.length)
					&& (this.type.equals(token.type)));
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.start;
	}

	@Override
	public String toString() {
		if (this.pairValue == 0) {
			return String.format("%s (%d, %d)", this.type, this.start, this.length);
		} else {
			return String.format("%s (%d, %d) (%d)", this.type, this.start, this.length, this.pairValue);
		}
	}

	@Override
	public int compareTo(Token token) {
		if (this.start != token.start) {
			return (this.start - token.start);
		} else if (this.length != token.length) {
			return (this.length - token.length);
		} else {
			return this.type.compareTo(token.type);
		}
	}

	/**
	 * return the end position of the token.
	 * @return start + length
	 */
	public int end() {
		return this.start + this.length;
	}
}
