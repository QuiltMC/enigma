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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;

/**
 * This class represents the Style for a TokenType. This class is responsible
 * for actually drawing a Token on the View.
 *
 * @author Ayman Al-Sairafi, Hanns Holger Rutz
 */
public record SyntaxStyle(Color color, int fontStyle) {
	private static final Map<TokenType, SyntaxStyle> styles = new HashMap<>();

	static {
		styles.put(TokenType.KEYWORD, new SyntaxStyle(SyntaxpainConfiguration.getHighlightColor(), Font.PLAIN));
		styles.put(TokenType.KEYWORD2, new SyntaxStyle(SyntaxpainConfiguration.getHighlightColor(), Font.BOLD + Font.ITALIC));
		styles.put(TokenType.STRING, new SyntaxStyle(SyntaxpainConfiguration.getStringColor(), Font.PLAIN));
		styles.put(TokenType.STRING2, new SyntaxStyle(SyntaxpainConfiguration.getStringColor(), Font.BOLD));
		styles.put(TokenType.NUMBER, new SyntaxStyle(SyntaxpainConfiguration.getNumberColor(), Font.PLAIN));
		styles.put(TokenType.OPERATOR, new SyntaxStyle(SyntaxpainConfiguration.getOperatorColor(), Font.PLAIN));
		styles.put(TokenType.DELIMITER, new SyntaxStyle(SyntaxpainConfiguration.getDelimiterColor(), Font.BOLD));
		styles.put(TokenType.TYPE, new SyntaxStyle(SyntaxpainConfiguration.getTypeColor(), Font.ITALIC));
		styles.put(TokenType.TYPE2, new SyntaxStyle(SyntaxpainConfiguration.getTypeColor(), Font.BOLD));
		styles.put(TokenType.TYPE3, new SyntaxStyle(SyntaxpainConfiguration.getTypeColor(), Font.BOLD + Font.ITALIC));
		styles.put(TokenType.IDENTIFIER, new SyntaxStyle(SyntaxpainConfiguration.getIdentifierColor(), Font.PLAIN));
		styles.put(TokenType.COMMENT, new SyntaxStyle(SyntaxpainConfiguration.getCommentColour(), Font.ITALIC));
		styles.put(TokenType.COMMENT2, new SyntaxStyle(SyntaxpainConfiguration.getCommentColour(), Font.BOLD + Font.ITALIC));
		styles.put(TokenType.DEFAULT, new SyntaxStyle(SyntaxpainConfiguration.getTextColor(), Font.PLAIN));
		styles.put(TokenType.WARNING, new SyntaxStyle(SyntaxpainConfiguration.getTextColor(), Font.PLAIN));
		styles.put(TokenType.ERROR, new SyntaxStyle(SyntaxpainConfiguration.getTextColor(), Font.BOLD + Font.ITALIC));
		styles.put(TokenType.REGEX, new SyntaxStyle(SyntaxpainConfiguration.getRegexColor(), Font.PLAIN));
		styles.put(TokenType.REGEX2, new SyntaxStyle(SyntaxpainConfiguration.getRegexColor(), Font.BOLD));
	}

	/**
	 * Returns the style for the given TokenType
	 */
	public static SyntaxStyle getStyle(TokenType type) {
		return styles.get(type);
	}

	/**
	 * Draws the given Token. This will simply find the proper SyntaxStyle for
	 * the TokenType and then asks the proper Style to draw the text of the
	 * Token.
	 */
	public static float drawText(Segment segment, float x, float y, Graphics2D graphics, TabExpander e, Token token) {
		SyntaxStyle s = getStyle(token.type);
		return s.drawText(segment, x, y, graphics, e, token.start);
	}

	/**
	 * Draw text.  This can directly call the Utilities.drawTabbedText.
	 * Subclasses can override this method to provide any other decorations.
	 *
	 * @param segment     - the source of the text
	 * @param x           - the X origin &gt;= 0
	 * @param y           - the Y origin &gt;= 0
	 * @param graphics    - the graphics context
	 * @param e           - how to expand the tabs. If this value is null, tabs will be
	 *                    expanded as a space character.
	 * @param startOffset - starting offset of the text in the document &gt;= 0
	 */
	public float drawText(Segment segment, float x, float y, Graphics2D graphics, TabExpander e, int startOffset) {
		graphics.setFont(graphics.getFont().deriveFont(this.fontStyle()));
		FontMetrics fontMetrics = graphics.getFontMetrics();
		int a = fontMetrics.getAscent();
		int h = a + fontMetrics.getDescent();
		float w = Utilities.getTabbedTextWidth(segment, fontMetrics, 0f, e, startOffset);
		int rX = (int) (x - 1);
		int rY = (int) (y - a);
		int rW = (int) w + 2;
		if ((this.fontStyle() & 0x10) != 0) {
			graphics.setColor(Color.decode("#EEEEEE"));
			graphics.fillRect(rX, rY, rW, h);
		}

		graphics.setColor(this.color());
		x = Utilities.drawTabbedText(segment, x, y, graphics, e, startOffset);
		if ((this.fontStyle() & 0x8) != 0) {
			graphics.setColor(Color.RED);
			graphics.drawRect(rX, rY, rW, h);
		}

		return x;
	}
}
