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

package cuchaz.enigma.gui.syntax;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;

/**
 * The Styles to use for each TokenType.  The defaults are created here, and
 * then the resource META-INF/services/de.sciss.syntaxpane.syntaxstyles.properties is read and
 * merged.  You can also pass a properties instance and merge your preferred
 * styles into the default styles.
 *
 * Text is drawn by forwarding the drawText request to the SyntaxStyle for the
 * that matches the given TokenType
 *
 * @author Ayman Al-Sairafi
 */
public class SyntaxStyles {

	public static final Pattern STYLE_PATTERN = Pattern.compile("Style\\.(\\w+)");

	/**
	 * You can call the mergeStyles method with a Properties file to customize
	 * the existing styles.  Any existing styles will be overwritten by the
	 * styles you provide.
	 */
	public void mergeStyles(Properties styles) {
		for (Map.Entry e : styles.entrySet()) {
			String tokenType = e.getKey().toString();
			String style = e.getValue().toString();
			try {
				TokenType tt = TokenType.valueOf(tokenType);
				SyntaxStyle tokenStyle = new SyntaxStyle(style);
				this.put(tt, tokenStyle);
			} catch (IllegalArgumentException ex) {
				LOG.warning("illegal token type or style for: " + tokenType);
			}
		}
	}
	Map<TokenType, SyntaxStyle> styles;
	private static final Logger LOG = Logger.getLogger(SyntaxStyles.class.getName());
	private static final SyntaxStyle DEFAULT_STYLE = new SyntaxStyle(Color.BLACK, Font.PLAIN);
	private static final SyntaxStyles instance = createInstance();

	private SyntaxStyles() {}

	/**
	 * Creates default styles
	 */
	private static SyntaxStyles createInstance() {
		SyntaxStyles syntaxstyles = new SyntaxStyles();
		Properties styles = JarServiceProvider.readProperties(SyntaxStyles.class);
		syntaxstyles.mergeStyles(styles);
		return syntaxstyles;
	}

	/**
	 * Returns the default singleton
	 */
	public static SyntaxStyles getInstance() {
		return instance;
	}

	public static SyntaxStyles read(Configuration config) {
		SyntaxStyles ss = createInstance();
		// Configuration styleConf = config.subConfig(STYLE_PROPERTY_KEY);

		for (Configuration.StringKeyMatcher m : config.getKeys(STYLE_PATTERN)) {
			String type = m.group1;
			try {
				ss.put(TokenType.valueOf(type), new SyntaxStyle(m.value));
			} catch (IllegalArgumentException e) {
				Logger.getLogger(SyntaxStyles.class.getName()).warning(
					String.format("Invalid Token Type [%s] for Style of ", type));
			}
		}
		return ss;
	}

	public void put(TokenType type, SyntaxStyle style) {
		if (this.styles == null) {
			this.styles = new HashMap<TokenType, SyntaxStyle>();
		}
		this.styles.put(type, style);
	}

	/**
	 * Returns the style for the given TokenType
	 */
	public SyntaxStyle getStyle(TokenType type) {
		if (this.styles != null && this.styles.containsKey(type)) {
			return this.styles.get(type);
		} else {
			return DEFAULT_STYLE;
		}
	}

	/**
	 * Draws the given Token.  This will simply find the proper SyntaxStyle for
	 * the TokenType and then asks the proper Style to draw the text of the
	 * Token.
	 */
	public int drawText(Segment segment, int x, int y,
						Graphics graphics, TabExpander e, Token token) {
		SyntaxStyle s = this.getStyle(token.type);
		return s.drawText(segment, x, y, graphics, e, token.start);
	}
}
