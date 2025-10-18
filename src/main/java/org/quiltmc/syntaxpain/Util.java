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
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * General utility methods for use around the internals.
 */
public class Util {
	private Util() {
	}

	/**
	 * A helper function that will return the SyntaxDocument attached to the
	 * given text component.  Return null if the document is not a
	 * SyntaxDocument, or if the text component is null
	 */
	public static SyntaxDocument getSyntaxDocument(JTextComponent component) {
		if (component == null) {
			return null;
		}

		Document doc = component.getDocument();
		if (doc instanceof SyntaxDocument) {
			return (SyntaxDocument) doc;
		} else {
			return null;
		}
	}

	/**
	 * Gets the Line Number at the give position of the editor component.
	 * The first line number is ZERO
	 *
	 * @return line number
	 */
	public static int getLineNumber(JTextComponent editor, int pos) throws BadLocationException {
		if (getSyntaxDocument(editor) != null) {
			SyntaxDocument sdoc = getSyntaxDocument(editor);
			return sdoc.getLineNumberAt(pos);
		} else {
			Document doc = editor.getDocument();
			return doc.getDefaultRootElement().getElementIndex(pos);
		}
	}

	public static int getLineCount(JTextComponent pane) {
		final SyntaxDocument sdoc = getSyntaxDocument(pane);
		if (sdoc != null) {
			return sdoc.getLineCount();
		}

		int count = 0;
		try {
			int p = pane.getDocument().getLength() - 1;
			if (p > 0) {
				count = getLineNumber(pane, p);
			}
		} catch (BadLocationException ex) {
			Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
		}

		return count;
	}
}
