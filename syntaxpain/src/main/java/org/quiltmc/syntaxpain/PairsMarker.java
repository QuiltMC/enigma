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

package org.quiltmc.syntaxpain;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.JTextComponent;
import java.awt.Color;

/**
 * This class highlights any pairs of the given language.  Pairs are defined
 * with the Token.pairValue.
 *
 * @author Ayman Al-Sairafi
 */
public class PairsMarker implements CaretListener {
	public static <M extends PairsMarker> M install(M marker) {
		marker.pane.addCaretListener(marker);

		return marker;
	}

	protected final JTextComponent pane;
	protected final SimpleMarker marker;

	public PairsMarker(JTextComponent pane, Color color) {
		this.pane = pane;
		this.marker = new SimpleMarker(color);
	}

	@Override
	public void caretUpdate(CaretEvent e) {
		this.removeMarkers();
		int pos = e.getDot();
		SyntaxDocument doc = SyntaxDocument.getFrom(this.pane);
		if (doc != null) {
			Token token = doc.getTokenAt(pos);
			if (token != null && token.pairValue != 0) {
				this.marker.markToken(this.pane, token);
				Token other = doc.getPairFor(token);
				if (other != null) {
					this.marker.markToken(this.pane, other);
				}
			}
		}
	}

	/**
	 * Remove all the highlights from the editor pane.  This should be called
	 * when the editor-kit is removed.
	 */
	public void removeMarkers() {
		this.marker.removeMarkers(this.pane);
	}
}
