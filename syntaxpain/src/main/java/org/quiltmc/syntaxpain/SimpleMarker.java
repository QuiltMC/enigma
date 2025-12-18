package org.quiltmc.syntaxpain;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ayman Al-Sairafi, Hanns Holger Rutz
 */
public class SimpleMarker extends DefaultHighlighter.DefaultHighlightPainter {
	public SimpleMarker(Color color) {
		super(color);
	}

	/**
	 * Removes only our private highlights
	 * This is public so that we can remove the highlights when the editorKit
	 * is unregistered.  SimpleMarker can be null, in which case all instances of
	 * our Markers are removed.
	 *
	 * @param component the text component whose markers are to be removed
	 */
	public void removeMarkers(JTextComponent component) {
		Highlighter hilite = component.getHighlighter();
		Highlighter.Highlight[] hilites = hilite.getHighlights();

		for (Highlighter.Highlight highlight : hilites) {
			if (this.equals(highlight.getPainter())) {
				hilite.removeHighlight(highlight);
			}
		}
	}

	/**
	 * Adds highlights for the given Token on the given pane
	 */
	public void markToken(JTextComponent pane, Token token) {
		this.markText(pane, token.start, token.end());
	}

	/**
	 * Adds highlights for the given region on the given pane
	 */
	public void markText(JTextComponent pane, int start, int end) {
		try {
			Highlighter highlighter = pane.getHighlighter();
			int selStart = pane.getSelectionStart();
			int selEnd = pane.getSelectionEnd();
			// if there is no selection or selection does not overlap
			if (selStart == selEnd || end < selStart || start > selStart) {
				highlighter.addHighlight(start, end, this);
				return;
			}

			// selection starts within the highlight, highlight before selection
			if (selStart > start && selStart < end) {
				highlighter.addHighlight(start, selStart, this);
			}

			// selection ends within the highlight, highlight remaining
			if (selEnd > start && selEnd < end) {
				highlighter.addHighlight(selEnd, end, this);
			}
		} catch (BadLocationException ex) {
			// nothing we can do if the request is out of bound
			LOG.log(Level.SEVERE, null, ex);
		}
	}

	private static final Logger LOG = Logger.getLogger(SimpleMarker.class.getName());
}
