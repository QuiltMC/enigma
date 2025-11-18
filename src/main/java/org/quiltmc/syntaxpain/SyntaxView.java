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

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.ViewFactory;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyntaxView extends PlainView {
	private static final Logger log = Logger.getLogger(SyntaxView.class.getName());
	private final SyntaxStyleMap styles;

	public SyntaxView(Element element, SyntaxStyleMap styles) {
		super(element);
		this.styles = styles;
	}

	@Override
	protected float drawUnselectedText(Graphics2D graphics, float x, float y, int p0, int p1) {
		setRenderingHits(graphics);
		Font saveFont = graphics.getFont();
		Color saveColor = graphics.getColor();
		SyntaxDocument doc = (SyntaxDocument) this.getDocument();
		Segment segment = this.getLineBuffer();

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
					x = this.styles.drawText(TokenType.DEFAULT, segment, (int) x, (int) y, graphics, this, start);
				}

				// t and s are the actual start and length of what we should
				// put on the screen.  assume these are the whole token....
				int l = t.length;
				int s = t.start;
				// ... unless the token starts before p0:
				if (s < p0) {
					// token is before what is requested. adjust the length and s
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
				x = this.styles.drawText(TokenType.DEFAULT, segment, x, y, graphics, this, start);
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
	protected float drawSelectedText(Graphics2D graphics, float x, float y, int p0, int p1) throws BadLocationException {
		return super.drawSelectedText(graphics, x, y, p0, p1);
	}

	/**
	 * Sets the Rendering Hints on the Graphics. This is used so that
	 * any painters can set the Rendering Hits to match the view.
	 */
	public static void setRenderingHits(Graphics2D g2d) {
		g2d.addRenderingHints(sysHints);
	}

	@Override
	protected void updateDamage(javax.swing.event.DocumentEvent changes, Shape a, ViewFactory f) {
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
			Map<RenderingHints.Key, ?> map = (Map<RenderingHints.Key, ?>) toolkit.getDesktopProperty("awt.font.desktophints");
			sysHints = new RenderingHints(map);
		} catch (Throwable ignored) {
			// ignored!
		}
	}
}
