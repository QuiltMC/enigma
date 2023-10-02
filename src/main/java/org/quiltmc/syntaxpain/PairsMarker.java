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

import javax.swing.JEditorPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This class highlights any pairs of the given language.  Pairs are defined
 * with the Token.pairValue.
 *
 * @author Ayman Al-Sairafi
 */
public class PairsMarker implements CaretListener, SyntaxComponent, PropertyChangeListener {
	private JTextComponent pane;
	private Markers.SimpleMarker marker;
	private Status status;

	public PairsMarker() {
	}

	@Override
	public void caretUpdate(CaretEvent e) {
		this.removeMarkers();
		int pos = e.getDot();
		SyntaxDocument doc = Util.getSyntaxDocument(this.pane);
		Token token = doc.getTokenAt(pos);
		if (token != null && token.pairValue != 0) {
			Markers.markToken(this.pane, token, this.marker);
			Token other = doc.getPairFor(token);
			if (other != null) {
				Markers.markToken(this.pane, other, this.marker);
			}
		}
	}

	/**
	 * Remove all the highlights from the editor pane.  This should be called
	 * when the editor-kit is removed.
	 */
	public void removeMarkers() {
		Markers.removeMarkers(this.pane, this.marker);
	}

	@Override
	public void configure() {
		Color markerColor = new Color(0xffbb77);
		this.marker = new Markers.SimpleMarker(markerColor);
	}

	@Override
	public void install(JEditorPane editor) {
		this.pane = editor;
		this.pane.addCaretListener(this);
		this.status = Status.INSTALLING;
	}

	@Override
	public void deinstall(JEditorPane editor) {
		this.status = Status.DEINSTALLING;
		this.pane.removeCaretListener(this);
		this.removeMarkers();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("document")) {
			this.pane.removeCaretListener(this);
			if (this.status.equals(Status.INSTALLING)) {
				this.pane.addCaretListener(this);
				this.removeMarkers();
			}
		}
	}
}
