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

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class contains static utility methods to make highlighting in text
 * components easier.
 *
 * @author Ayman Al-Sairafi, Hanns Holger Rutz
 */
public class Markers {
	// This subclass is used in our highlighting code
	public static class SimpleMarker extends DefaultHighlighter.DefaultHighlightPainter {
		public SimpleMarker(Color color) {
			super(color);
		}
	}

	/**
	 * Removes only our private highlights
	 * This is public so that we can remove the highlights when the editorKit
	 * is unregistered.  SimpleMarker can be null, in which case all instances of
	 * our Markers are removed.
	 * @param component the text component whose markers are to be removed
	 * @param marker the SimpleMarker to remove
	 */
	public static void removeMarkers(JTextComponent component, SimpleMarker marker) {
		Highlighter hilite = component.getHighlighter();
		Highlighter.Highlight[] hilites = hilite.getHighlights();

		for (Highlighter.Highlight highlight : hilites) {
			if (highlight.getPainter() instanceof SimpleMarker hMarker) {
				if (marker == null || hMarker.equals(marker)) {
					hilite.removeHighlight(highlight);
				}
			}
		}
	}

	/**
	 * Adds highlights for the given Token on the given pane
	 */
	public static void markToken(JTextComponent pane, Token token, SimpleMarker marker) {
		markText(pane, token.start, token.end(), marker);
	}

	/**
	 * Adds highlights for the given region on the given pane
	 */
	public static void markText(JTextComponent pane, int start, int end, SimpleMarker marker) {
		try {
			Highlighter highlighter = pane.getHighlighter();
			int selStart = pane.getSelectionStart();
			int selEnd = pane.getSelectionEnd();
			// if there is no selection or selection does not overlap
			if (selStart == selEnd || end < selStart || start > selStart) {
				highlighter.addHighlight(start, end, marker);
				return;
			}

			// selection starts within the highlight, highlight before selection
			if (selStart > start && selStart < end) {
				highlighter.addHighlight(start, selStart, marker);
			}

			// selection ends within the highlight, highlight remaining
			if (selEnd > start && selEnd < end) {
				highlighter.addHighlight(selEnd, end, marker);
			}
		} catch (BadLocationException ex) {
			// nothing we can do if the request is out of bound
			LOG.log(Level.SEVERE, null, ex);
		}
	}

	private static final Logger LOG = Logger.getLogger(Markers.class.getName());
}
