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

import org.quiltmc.syntaxpain.generated.JavaLexer;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class JavaSyntaxKit extends DefaultEditorKit implements ViewFactory {
	private final Lexer lexer;
	private final Map<JEditorPane, List<SyntaxComponent>> editorComponents = new WeakHashMap<>();
	private static Font font = null;

	public JavaSyntaxKit() {
		super();
		// JavaLexer is generated automagically by jflex based on the java.jflex file
		this.lexer = new JavaLexer();
	}

	public void addComponents(JEditorPane editorPane) {
		this.installComponent(editorPane, new PairsMarker());
		this.installComponent(editorPane, new LineNumbersRuler());
	}

	public void installComponent(JEditorPane pane, SyntaxComponent comp) {
		comp.configure();
		comp.install(pane);
		this.editorComponents.computeIfAbsent(pane, k -> new ArrayList<>());
		this.editorComponents.get(pane).add(comp);
	}

	public static void setFont(Font newFont) {
		font = newFont;
	}

	@Override
	public ViewFactory getViewFactory() {
		return this;
	}

	@Override
	public View create(Element element) {
		return new SyntaxView(element);
	}

	@Override
	public void install(JEditorPane editorPane) {
		super.install(editorPane);

		if (font == null) {
			font = SyntaxpainConfiguration.getEditorFont();
		}

		editorPane.setFont(font);

		Color caretColor = SyntaxpainConfiguration.getTextColor();
		editorPane.setCaretColor(caretColor);
		Color selectionColor = new Color(0x99ccff);
		editorPane.setSelectionColor(selectionColor);
		this.addQuickFindAction(editorPane);
		this.addComponents(editorPane);
	}

	@Override
	public void deinstall(JEditorPane editorPane) {
		for (SyntaxComponent c : this.editorComponents.get(editorPane)) {
			c.deinstall(editorPane);
		}

		this.editorComponents.clear();
		editorPane.getInputMap().clear();
		ActionMap m = editorPane.getActionMap();
		m.clear();
	}

	/**
	 * Sets up the quick find action.
	 */
	public void addQuickFindAction(JEditorPane editorPane) {
		InputMap inputMap = new InputMap();
		inputMap.setParent(editorPane.getInputMap());
		ActionMap actionMap = new ActionMap();
		actionMap.setParent(editorPane.getActionMap());

		QuickFindAction action = new QuickFindAction();
		actionMap.put(action.getClass().getSimpleName(), action);

		KeyStroke stroke = KeyStroke.getKeyStroke("control F");
		action.putValue(Action.ACCELERATOR_KEY, stroke);
		inputMap.put(stroke, action.getClass().getSimpleName());

		editorPane.setActionMap(actionMap);
		editorPane.setInputMap(JTextComponent.WHEN_FOCUSED, inputMap);
	}

	@Override
	public Document createDefaultDocument() {
		return new SyntaxDocument(this.lexer);
	}

	@Override
	public String getContentType() {
		return "text/enigma-sources";
	}
}
