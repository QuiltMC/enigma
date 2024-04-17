package org.quiltmc.enigma.gui.highlight;

import org.quiltmc.enigma.gui.util.ScaleUtil;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

public class BoxHighlightPainter implements Highlighter.HighlightPainter {
	private final Color fillColor;
	private final Color borderColor;

	protected BoxHighlightPainter(Color fillColor, Color borderColor) {
		this.fillColor = fillColor;
		this.borderColor = borderColor;
	}

	public static BoxHighlightPainter create(Color color, Color outline) {
		return new BoxHighlightPainter(color, outline);
	}

	public static Rectangle getBounds(JTextComponent text, int start, int end) {
		try {
			// determine the bounds of the text
			Rectangle2D startRect = text.modelToView2D(start);
			Rectangle2D endRect = text.modelToView2D(end);
			Rectangle bounds = new Rectangle();
			Rectangle2D.union(startRect, endRect, bounds);

			// adjust the box so it looks nice
			bounds.x -= ScaleUtil.scale(2);
			bounds.width += ScaleUtil.scale(1);
			bounds.y += ScaleUtil.scale(1);
			bounds.height -= ScaleUtil.scale(2);

			return bounds;
		} catch (BadLocationException ex) {
			// don't care... just return something
			return new Rectangle(0, 0, 0, 0);
		}
	}

	@Override
	public void paint(Graphics g, int start, int end, Shape shape, JTextComponent text) {
		Rectangle bounds = getBounds(text, start, end);
		int arcSize = ScaleUtil.scale(4);

		// fill the area
		if (this.fillColor != null) {
			g.setColor(this.fillColor);
			g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arcSize, arcSize);
		}

		// draw a box around the area
		g.setColor(this.borderColor);
		g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arcSize, arcSize);
	}
}
