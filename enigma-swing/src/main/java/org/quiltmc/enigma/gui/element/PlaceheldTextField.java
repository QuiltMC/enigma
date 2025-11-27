package org.quiltmc.enigma.gui.element;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.util.Utils;

import javax.swing.JTextField;
import javax.swing.text.Document;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * A text field that displays placeholder text when it's empty.
 */
public class PlaceheldTextField extends JTextField {
	protected static final int DEFAULT_COLUMNS = 0;

	private Placeholder placeholder;
	@Nullable
	private Color placeholderColor;

	/**
	 * Constructs a new field with the default {@link Document},  {@value PlaceheldTextField#DEFAULT_COLUMNS} columns,
	 * and no initial text or placeholder.
	 */
	public PlaceheldTextField() {
		this(null, null);
	}

	/**
	 * Constructs a new field with the default {@link Document},  {@value PlaceheldTextField#DEFAULT_COLUMNS} columns,
	 * and the passed initial {@code text} and {@code placeholder}.
	 *
	 * @param text        the initial text; may be {@code null}
	 * @param placeholder the initial placeholder; may be {@code null}
	 */
	public PlaceheldTextField(String text, String placeholder) {
		this(null, text, placeholder, DEFAULT_COLUMNS);
	}

	/**
	 * Constructs a new field.
	 *
	 * @param doc         see {@link JTextField#JTextField(Document, String, int)}
	 * @param text        the initial text; may be {@code null}
	 * @param placeholder the initial placeholder; may be {@code null}
	 * @param columns     see {@link JTextField#JTextField(Document, String, int)}
	 *
	 * @exception IllegalArgumentException if {@code columns} is negative
	 */
	public PlaceheldTextField(Document doc, String text, @Nullable String placeholder, int columns) {
		super(doc, text, columns);
		this.setPlaceholder(placeholder);
	}

	@Override
	public Dimension getPreferredSize() {
		final Dimension size = super.getPreferredSize();

		if (this.placeholder.isFull()) {
			final Insets insets = this.getInsets();

			size.width = Math.max(insets.left + this.placeholder.getWidth() + insets.right, size.width);
		}

		return size;
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);

		if (this.placeholder.isFull() && this.getText().isEmpty()) {
			GuiUtil.trySetRenderingHints(graphics);

			Utils.findFirstNonNull(this.placeholderColor, this.getDisabledTextColor(), this.getForeground())
					.ifPresent(graphics::setColor);
			graphics.setFont(this.getFont());

			final Insets insets = this.getInsets();
			final int baseY = graphics.getFontMetrics().getMaxAscent() + insets.top;
			graphics.drawString(this.placeholder.getText(), insets.left, baseY);
		}
	}

	/**
	 * @param placeholder the placeholder text for this field; if {@code null}, no placeholder will be shown
	 */
	public void setPlaceholder(@Nullable String placeholder) {
		this.placeholder = placeholder == null || placeholder.isEmpty()
				? EmptyPlaceholder.INSTANCE
				: new FullPlaceholder(placeholder);
	}

	public String getPlaceholder() {
		return this.placeholder.getText();
	}

	protected int getPlaceholderWidth() {
		return this.placeholder.getWidth();
	}

	/**
	 * @param color the placeholder color for this field; if {@code null}, the
	 * {@linkplain #getDisabledTextColor() disabled color} will be used
	 */
	public void setPlaceholderColor(@Nullable Color color) {
		this.placeholderColor = color;
	}

	@Nullable
	public Color getPlaceholderColor() {
		return this.placeholderColor;
	}

	@Override
	public void setFont(Font f) {
		super.setFont(f);

		// placeholder is null when the super constructor calls setFont
		if (this.placeholder instanceof FullPlaceholder full) {
			full.clearWidth();
		}
	}

	private sealed interface Placeholder {
		String getText();

		boolean isFull();

		int getWidth();
	}

	private static final class EmptyPlaceholder implements Placeholder {
		static final EmptyPlaceholder INSTANCE = new EmptyPlaceholder();

		@Override
		public String getText() {
			return "";
		}

		@Override
		public boolean isFull() {
			return false;
		}

		@Override
		public int getWidth() {
			return 0;
		}
	}

	private final class FullPlaceholder implements Placeholder {
		static final int UNSET_WIDTH = -1;

		final String text;

		int width = UNSET_WIDTH;

		FullPlaceholder(String text) {
			this.text = text;
		}

		@Override
		public String getText() {
			return this.text;
		}

		@Override
		public boolean isFull() {
			return true;
		}

		@Override
		public int getWidth() {
			if (this.width < 0) {
				this.width = PlaceheldTextField.this
					.getFontMetrics(PlaceheldTextField.this.getFont()).stringWidth(this.text);
			}

			return this.width;
		}

		public void clearWidth() {
			this.width = UNSET_WIDTH;
		}
	}
}
