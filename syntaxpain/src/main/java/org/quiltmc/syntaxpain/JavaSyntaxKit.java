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

import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import java.awt.Color;

public class JavaSyntaxKit extends DefaultEditorKit implements ViewFactory {
	public static final String CONTENT_TYPE = "text/enigma-sources";

	private static SyntaxStyleMap styles = new SyntaxStyleMap(
		new Color(0x3333EE),
		new Color(0xCC6600),
		new Color(0x999933),
		new Color(0x000000),
		new Color(0x000000),
		new Color(0x000000),
		new Color(0x000000),
		new Color(0x339933),
		new Color(0x000000),
		new Color(0xcc6600)
	);

	public static void setSyntaxColors(
			Color highlight, Color string, Color number, Color operator, Color delimiter,
			Color type, Color identifier, Color comment, Color text, Color regex
	) {
		styles = new SyntaxStyleMap(
			highlight, string, number, operator, delimiter,
			type, identifier, comment, text, regex
		);
	}

	private final Lexer lexer;

	public JavaSyntaxKit() {
		super();
		// JavaLexer is generated automagically by jflex based on the java.jflex file
		this.lexer = new JavaLexer();
	}

	@Override
	public ViewFactory getViewFactory() {
		return this;
	}

	@Override
	public View create(Element element) {
		return new SyntaxView(element, styles);
	}

	@Override
	public Document createDefaultDocument() {
		return new SyntaxDocument(this.lexer);
	}

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}
}
