/*
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
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

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Segment;

/**
 * This is a default, and abstract implementation of a Lexer using JFLex
 * with some utility methods that Lexers can implement.
 *
 * @author Ayman Al-Sairafi
 */
public abstract class DefaultJFlexLexer implements Lexer {
	protected int tokenStart;
	protected int tokenLength;
	protected int offset;

	/**
	 * Create and return a Token of given type from start with length
	 * offset is added to start
	 */
	protected Token token(TokenType type, int start, int length) {
		return new Token(type, start + this.offset, length);
	}

	/**
	 * Create and return a Token of given type.  start is obtained from {@link DefaultJFlexLexer#yychar()}
	 * and length from {@link DefaultJFlexLexer#yylength()}
	 * offset is added to start
	 */
	protected Token token(TokenType type) {
		return new Token(type, this.yychar() + this.offset, this.yylength());
	}

	/**
	 * Create and return a Token of given type and pairValue.
	 * start is obtained from {@link DefaultJFlexLexer#yychar()}
	 * and length from {@link DefaultJFlexLexer#yylength()}
	 * offset is added to start
	 */
	protected Token token(TokenType type, int pairValue) {
		return new Token(type, this.yychar() + this.offset, this.yylength(), (byte) pairValue);
	}

	/**
	 * The DefaultJFlexLexer simply calls the yylex method of a JFlex compatible
	 * Lexer and adds the tokens obtained to an ArrayList.
	 */
	@Override
	public void parse(Segment segment, int offset, List<Token> tokens) {
		try {
			CharArrayReader reader = new CharArrayReader(segment.array, segment.offset, segment.count);
			this.yyreset(reader);
			this.offset = offset;
			for (Token t = this.yylex(); t != null; t = this.yylex()) {
				tokens.add(t);
			}
		} catch (IOException ex) {
			Logger.getLogger(DefaultJFlexLexer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * This will be called to reset the the lexer.
	 * This is created automatically by JFlex.
	 */
	public abstract void yyreset(Reader reader);

	/**
	 * This is called to return the next Token from the Input Reader
	 * @return next token, or null if no more tokens.
	 */
	public abstract Token yylex() throws IOException;

	/**
	 * Returns the length of the matched text region.
	 * This method is automatically implemented by JFlex lexers
	 */
	public abstract int yylength();

	/**
	 * Return the char number from beginning of input stream.
	 * This is NOT implemented by JFlex, so the code must be
	 * added to create this and return the private yychar field
	 */
	public abstract int yychar();
}
