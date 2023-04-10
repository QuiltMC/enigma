package cuchaz.enigma.gui.highlight;

import cuchaz.enigma.gui.config.UiConfig;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

public class SelectionHighlightPainter implements Highlighter.HighlightPainter {
	public static final SelectionHighlightPainter INSTANCE = new SelectionHighlightPainter();

	@Override
	public void paint(Graphics g, int start, int end, Shape shape, JTextComponent text) {
		// draw a thick border
		Graphics2D g2d = (Graphics2D) g;
		Rectangle bounds = BoxHighlightPainter.getBounds(text, start, end);
		g2d.setColor(UiConfig.getSelectionHighlightColor());
		g2d.setStroke(new BasicStroke(2.0f));
		g2d.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 4, 4);
	}
}
