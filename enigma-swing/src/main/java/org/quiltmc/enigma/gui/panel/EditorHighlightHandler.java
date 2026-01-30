package org.quiltmc.enigma.gui.panel;

import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.gui.highlight.SelectionHighlightPainter;

import javax.swing.Timer;

/**
 * Handles selection highlights in {@linkplain AbstractEditorPanel editor panels}.
 */
class EditorHighlightHandler extends Timer {
	private static final int BLINK_INTERVAL = 2;

	private final AbstractEditorPanel<?> panel;
	private final int counterMax;

	private int counter = 0;
	private Object highlight = null;

	EditorHighlightHandler(AbstractEditorPanel<?> panel, Token token, int delay, int blinks) {
		super(delay, null);

		this.panel = panel;
		this.counterMax = blinks * BLINK_INTERVAL;

		this.setInitialDelay(0);

		this.addActionListener(e -> {
			if (this.counter < this.counterMax) {
				if (this.counter % BLINK_INTERVAL == 0) {
					this.highlight = panel.addHighlight(token, SelectionHighlightPainter.INSTANCE);
				} else {
					this.removeHighlight();
				}

				this.counter++;
			} else {
				this.finish();
			}
		});
	}

	private void removeHighlight() {
		if (this.highlight != null) {
			this.panel.editor.getHighlighter().removeHighlight(this.highlight);
		}
	}

	void finish() {
		this.stop();
		this.removeHighlight();
		if (this.panel.selectionHighlightHandler == this) {
			this.panel.selectionHighlightHandler = null;
		}
	}
}
