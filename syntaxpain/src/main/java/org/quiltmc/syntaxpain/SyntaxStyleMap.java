package org.quiltmc.syntaxpain;

import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Map;

public class SyntaxStyleMap {
	private final Map<TokenType, SyntaxStyle> styles;

	public SyntaxStyleMap(
			Color highlight, Color string, Color number, Color operator, Color delimiter,
			Color type, Color identifier, Color comment, Color text, Color regex
	) {
		this.styles = Map.ofEntries(
			Map.entry(TokenType.KEYWORD, new SyntaxStyle(highlight, Font.PLAIN)),
			Map.entry(TokenType.KEYWORD2, new SyntaxStyle(highlight, Font.BOLD + Font.ITALIC)),
			Map.entry(TokenType.STRING, new SyntaxStyle(string, Font.PLAIN)),
			Map.entry(TokenType.STRING2, new SyntaxStyle(string, Font.BOLD)),
			Map.entry(TokenType.NUMBER, new SyntaxStyle(number, Font.PLAIN)),
			Map.entry(TokenType.OPERATOR, new SyntaxStyle(operator, Font.PLAIN)),
			Map.entry(TokenType.DELIMITER, new SyntaxStyle(delimiter, Font.BOLD)),
			Map.entry(TokenType.TYPE, new SyntaxStyle(type, Font.ITALIC)),
			Map.entry(TokenType.TYPE2, new SyntaxStyle(type, Font.BOLD)),
			Map.entry(TokenType.TYPE3, new SyntaxStyle(type, Font.BOLD + Font.ITALIC)),
			Map.entry(TokenType.IDENTIFIER, new SyntaxStyle(identifier, Font.PLAIN)),
			Map.entry(TokenType.COMMENT, new SyntaxStyle(comment, Font.ITALIC)),
			Map.entry(TokenType.COMMENT2, new SyntaxStyle(comment, Font.BOLD + Font.ITALIC)),
			Map.entry(TokenType.DEFAULT, new SyntaxStyle(text, Font.PLAIN)),
			Map.entry(TokenType.WARNING, new SyntaxStyle(text, Font.PLAIN)),
			Map.entry(TokenType.ERROR, new SyntaxStyle(text, Font.BOLD + Font.ITALIC)),
			Map.entry(TokenType.REGEX, new SyntaxStyle(regex, Font.PLAIN)),
			Map.entry(TokenType.REGEX2, new SyntaxStyle(regex, Font.BOLD))
		);
	}

	public SyntaxStyle getStyle(TokenType type) {
		return this.styles.get(type);
	}

	/**
	 * Draws the given Token. This will simply find the proper SyntaxStyle for
	 * the TokenType and then asks the proper Style to draw the text of the Token.
	 */
	public float drawText(Segment segment, float x, float y, Graphics2D graphics, TabExpander e, Token token) {
		return this.drawText(token.type, segment, x, y, graphics, e, token.start);
	}

	public float drawText(
			TokenType type, Segment segment, float x, float y, Graphics2D graphics, TabExpander e, int startOffset
	) {
		return this.getStyle(type).drawText(segment, x, y, graphics, e, startOffset);
	}
}
