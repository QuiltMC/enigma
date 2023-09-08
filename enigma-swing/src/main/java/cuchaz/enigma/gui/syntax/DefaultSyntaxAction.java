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

import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

/**
 * The DefaultSyntaxAction.  You can extend this class or implement the interface
 * SyntaxAction to create your own actions.
 *
 * @author Ayman Al-Sairafi
 */
abstract public class DefaultSyntaxAction extends TextAction implements SyntaxAction {

	public DefaultSyntaxAction(String actionName) {
		super(actionName);
		this.putValue(NAME, actionName);
	}

	@Override
	public void install(JEditorPane editor, Configuration config, String name) {
		// find setter methods for each property key:
		String actionName = name.substring(ACTION_PREFIX.length());
		for (Configuration.StringKeyMatcher m : config.getKeys(
			Pattern.compile(Pattern.quote(name) + "\\.((\\w|-)+)"))) {
			if (!ReflectUtils.callSetter(this, m.group1, m.value)) {
				this.putValue(m.group1, m.value);
			}
		}
		// if we did not put a name, use the action name
		if (this.getValue(NAME) == null) {
			this.putValue(NAME, actionName);
		}
		// if we did not put an icon, try and find one using our name
		if (this.getValue(SMALL_ICON) == null) {
			this.setSmallIcon(actionName + ".png");
		}
	}

	@Override
	public void deinstall(JEditorPane editor) {
		// nothing
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
	public void actionPerformed(JTextComponent target, SyntaxDocument sDoc,
								int dot, ActionEvent e) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public String toString() {
		return "Action " + this.getValue(NAME) + "of type " + this.getClass().getSimpleName();
	}

	/**
	 * Configure the MenuText for the Action
	 */
	public final void setMenuText(String text) {
		this.putValue(NAME, text);
		// also set the SHORT_DESCRIPTIOn if it was not set, so we have
		// at least some tooltip for toolbar buttons
		if (this.getValue(SHORT_DESCRIPTION) == null) {
			this.putValue(SHORT_DESCRIPTION, text);
		}
	}

	/**
	 * Configure the ToolTip for the Action
	 */
	public final void setToolTip(String text) {
		this.putValue(SHORT_DESCRIPTION, text);
	}

	/**
	 * Sets the Large Icon for this action from given url
	 */
	public final void setLargeIcon(String url) {
		URL loc = this.getClass().getClassLoader().getResource(LARGE_ICONS_LOC_PREFIX + url);
		if (loc != null) {
			ImageIcon i = new ImageIcon(loc);
			this.putValue(LARGE_ICON_KEY, i);
		}
	}

	/**
	 * Configure the SmallIcon for the Action
	 */
	public final void setSmallIcon(String url) {
		URL loc = this.getClass().getClassLoader().getResource(SMALL_ICONS_LOC_PREFIX + url);
		if (loc != null) {
			ImageIcon i = new ImageIcon(loc);
			this.putValue(SMALL_ICON, i);
		}
	}
	public static final String ACTION_PREFIX = "Action.";
	public static final String SMALL_ICONS_LOC_PREFIX = "de/sciss/syntaxpane/images/small-icons/";
	public static final String LARGE_ICONS_LOC_PREFIX = "de/sciss/syntaxpane/images/large-icons/";
}
