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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.ViewFactory;

public class SyntaxView extends PlainView {

	public static final String PROPERTY_RIGHT_MARGIN_COLOR  = "RightMarginColor";
	public static final String PROPERTY_RIGHT_MARGIN_COLUMN = "RightMarginColumn";
	public static final String PROPERTY_SINGLE_COLOR_SELECT = "SingleColorSelect";
	private static final Logger log = Logger.getLogger(SyntaxView.class.getName());
	private boolean singleColorSelect;
	private int rightMarginColumn;
	private Color rightMarginColor;
	private SyntaxStyles styles;
	private SyntaxStyle defaultStyle;

	/**
	 * Construct a new view using the given configuration and prefix given
	 */
	public SyntaxView(Element element, Configuration config) {
		super(element);
		this.configure(config);
	}

	public void reconfigure(Configuration config) {
		this.configure(config);
	}

	private void configure(Configuration config) {
		this.singleColorSelect = config.getBoolean(PROPERTY_SINGLE_COLOR_SELECT, false);
		this.rightMarginColor = new Color(config.getInteger(PROPERTY_RIGHT_MARGIN_COLOR,
			0xFF7777));
		this.rightMarginColumn = config.getInteger(PROPERTY_RIGHT_MARGIN_COLUMN,
			0);
		this.styles = SyntaxStyles.read(config);
		this.defaultStyle = this.styles.getStyle(TokenType.DEFAULT);
	}

	@Override
	protected int drawUnselectedText(Graphics graphics, int x, int y, int p0,
									 int p1) {
		setRenderingHits((Graphics2D) graphics);
		Font saveFont = graphics.getFont();
		Color saveColor = graphics.getColor();
		SyntaxDocument doc = (SyntaxDocument) this.getDocument();
		Segment segment = this.getLineBuffer();
		// Draw the right margin first, if needed.  This way the text overlays
		// the margin
		if (this.rightMarginColumn > 0) {
			int m_x = this.rightMarginColumn * graphics.getFontMetrics().charWidth('m');
			int h = graphics.getFontMetrics().getHeight();
			graphics.setColor(this.rightMarginColor);
			graphics.drawLine(m_x, y, m_x, y - h);
		}
		try {
			// Colour the parts
			Iterator<Token> i = doc.getTokens(p0, p1);
			int start = p0;
			while (i.hasNext()) {
				Token t = i.next();
				// if there is a gap between the next token start and where we
				// should be starting (spaces not returned in tokens), then draw
				// it in the default type
				if (start < t.start) {
					doc.getText(start, t.start - start, segment);
					x = this.defaultStyle.drawText(segment, x, y, graphics, this, start);
				}
				// t and s are the actual start and length of what we should
				// put on the screen.  assume these are the whole token....
				int l = t.length;
				int s = t.start;
				// ... unless the token starts before p0:
				if (s < p0) {
					// token is before what is requested. adgust the length and s
					l -= (p0 - s);
					s = p0;
				}
				// if token end (s + l is still the token end pos) is greater
				// than p1, then just put up to p1
				if (s + l > p1) {
					l = p1 - s;
				}
				doc.getText(s, l, segment);
				x = this.styles.drawText(segment, x, y, graphics, this, t);
				start = t.end();
			}
			// now for any remaining text not tokenized:
			if (start < p1) {
				doc.getText(start, p1 - start, segment);
				x = this.defaultStyle.drawText(segment, x, y, graphics, this, start);
			}
		} catch (BadLocationException ex) {
			log.log(Level.SEVERE, "Requested: " + ex.offsetRequested(), ex);
		} finally {
			graphics.setFont(saveFont);
			graphics.setColor(saveColor);
		}
		return x;
	}

	@Override
	protected int drawSelectedText(Graphics graphics, int x, int y, int p0, int p1)
		throws BadLocationException {
		if (this.singleColorSelect) {
			if (this.rightMarginColumn > 0) {
				int m_x = this.rightMarginColumn * graphics.getFontMetrics().charWidth('m');
				int h = graphics.getFontMetrics().getHeight();
				graphics.setColor(this.rightMarginColor);
				graphics.drawLine(m_x, y, m_x, y - h);
			}
			return super.drawSelectedText(graphics, x, y, p0, p1);
		} else {
			return this.drawUnselectedText(graphics, x, y, p0, p1);
		}
	}

	/**
	 * Sets the Rendering Hints o nthe Graphics.  This is used so that
	 * any painters can set the Rendering Hits to match the view.
	 */
	public static void setRenderingHits(Graphics2D g2d) {
		g2d.addRenderingHints(sysHints);
	}

	@Override
	protected void updateDamage(javax.swing.event.DocumentEvent changes,
								Shape a,
								ViewFactory f) {
		super.updateDamage(changes, a, f);

		// Try to limit extra repaint work

		SyntaxDocument doc = (SyntaxDocument) this.getDocument();
		int earliestTokenChangePos = doc.getAndClearEarliestTokenChangePos();
		int latestTokenChangePos = doc.getAndClearLatestTokenChangePos();

		if (earliestTokenChangePos >= 0 && latestTokenChangePos > earliestTokenChangePos) {
			JTextComponent textComponent = (JTextComponent) this.getContainer();

			Element map = this.getElement();
			int earliestLine = map.getElementIndex(earliestTokenChangePos);
			int latestLine = map.getElementIndex(latestTokenChangePos);

			// Note that there is no need to repaint a single line, since this is
			// always handled by the parent (PlainView) updateDamage call
			if (earliestLine < latestLine) {
				this.damageLineRange(earliestLine, latestLine, a, textComponent);
			}
		}
	}
	/**
	 * The values for the string key for Text Anti-Aliasing
	 */
	private static RenderingHints sysHints;

	static {
		sysHints = null;
		try {
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			@SuppressWarnings("unchecked")
			Map<RenderingHints.Key,?> map = (Map<RenderingHints.Key,?>)
				toolkit.getDesktopProperty("awt.font.desktophints");
			sysHints = new RenderingHints(map);
		} catch (Throwable ignored) {
		}
	}
}
