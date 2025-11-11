package org.quiltmc.enigma.gui.highlight;

import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.util.ScaleUtil;

import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

public class SelectionHighlightPainter implements Highlighter.HighlightPainter {
	public static final SelectionHighlightPainter INSTANCE = new SelectionHighlightPainter();

	@Override
	public void paint(Graphics g, int start, int end, Shape shape, JTextComponent text) {
		// draw a thick border
		Graphics2D g2d = (Graphics2D) g;
		Rectangle bounds = BoxHighlightPainter.getBounds(text, start, end);
		g2d.setColor(Config.getCurrentSyntaxPaneColors().selectionHighlight.value());
		g2d.setStroke(new BasicStroke(ScaleUtil.scale(2.0f)));

		int arcSize = ScaleUtil.scale(4);
		g2d.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arcSize, arcSize);
	}
}
