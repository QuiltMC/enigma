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

package cuchaz.enigma.gui.syntax;

import javax.swing.Action;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import java.awt.event.ActionEvent;

/**
 * The DefaultSyntaxAction.  You can extend this class or implement the interface
 * SyntaxAction to create your own actions.
 *
 * @author Ayman Al-Sairafi
 */
public abstract class DefaultSyntaxAction extends TextAction implements Action {
	public DefaultSyntaxAction(String actionName) {
		super(actionName);
		this.putValue(NAME, actionName);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JTextComponent text = this.getTextComponent(e);
		SyntaxDocument sdoc = ActionUtils.getSyntaxDocument(text);
		if (text != null) {
			this.actionPerformed(text, sdoc, text.getCaretPosition(), e);
		}
	}

	/**
	 * Convenience method that will be called if the Action is performed on a
	 * JTextComponent.  SyntaxActions should generally override this method.
	 * @param target (non-null JTextComponent from Action.getSource
	 * @param sDoc (SyntaxDOcument of the text component, could be null)
	 * @param dot (position of caret at text document)
	 * @param e actual ActionEvent passed to actionPerformed
	 */
	public void actionPerformed(JTextComponent target, SyntaxDocument sDoc, int dot, ActionEvent e) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public String toString() {
		return "Action " + this.getValue(NAME) + "of type " + this.getClass().getSimpleName();
	}
}
