package org.quiltmc.enigma.gui.panel;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.highlight.BoxHighlightPainter;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.util.LineIndexer;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import java.awt.Color;

public class SimpleSnippetPanel extends AbstractEditorPanel<JScrollPane> {
	public SimpleSnippetPanel(Gui gui, @Nullable Token target) {
		super(gui);

		this.editor.setCaretColor(GuiUtil.TRANSPARENT);

		this.addSourceSetListener(source -> {
			this.installEditorRuler(new LineIndexer(source.toString()).getLine(this.getSourceBounds().start()));

			if (target != null) {
				final Token boundedTarget = this.navigateToTokenImpl(target);
				if (boundedTarget != null) {
					this.addHighlight(boundedTarget, BoxHighlightPainter.create(
							new Color(0, 0, 0, 0),
							Config.getCurrentSyntaxPaneColors().selectionHighlight.value()
					));
				}
			}
		});
	}

	@Override
	protected JScrollPane createEditorScrollPane(JEditorPane editor) {
		return new JScrollPane(
			editor,
			JScrollPane.VERTICAL_SCROLLBAR_NEVER,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
		);
	}
}
