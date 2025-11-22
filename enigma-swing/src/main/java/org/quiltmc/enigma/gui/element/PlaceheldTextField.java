package org.quiltmc.enigma.gui.element;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.util.Utils;

import javax.swing.JTextField;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.text.Document;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * A text field that displays placeholder text when it's empty.
 */
public class PlaceheldTextField extends JTextField implements MenuElement {
	@Nullable
	private Placeholder placeholder;

	@Nullable
	private Color placeholderColor;

	/**
	 * Constructs a new field with the default {@link Document},  {@code 0} columns, and no initial text or placeholder.
	 */
	public PlaceheldTextField() {
		this(null, null);
	}

	/**
	 * Constructs a new field with the default {@link Document},  {@code 0} columns, and the passed initial
	 * {@code text} and {@code placeholder}.
	 *
	 * @param text        the initial text; may be {@code null}
	 * @param placeholder the initial placeholder; may be {@code null}
	 */
	public PlaceheldTextField(String text, String placeholder) {
		this(null, text, placeholder, 0);
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
	public PlaceheldTextField(
			@Nullable Document doc, @Nullable String text, @Nullable String placeholder, int columns
	) {
		super(doc, text, columns);

		this.placeholder = new Placeholder(placeholder);
	}

	@Override
	public Dimension getPreferredSize() {
		final Dimension size = super.getPreferredSize();

		if (this.placeholder != null) {
			final Insets insets = this.getInsets();

			size.width = Math.max(insets.left + this.placeholder.getWidth() + insets.right, size.width);
		}

		return size;
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);

		if (this.placeholder != null && this.getText().isEmpty()) {
			GuiUtil.trySetRenderingHints(graphics);

			Utils.findFirstNonNull(this.placeholderColor, this.getDisabledTextColor(), this.getForeground())
					.ifPresent(graphics::setColor);
			graphics.setFont(this.getFont());

			final Insets insets = this.getInsets();
			final int baseY = graphics.getFontMetrics().getMaxAscent() + insets.top;
			graphics.drawString(this.placeholder.text, insets.left, baseY);
		}
	}

	/**
	 * @param placeholder the placeholder text for this field; if {@code null}, no placeholder will be shown
	 */
	public void setPlaceholder(@Nullable String placeholder) {
		this.placeholder = new Placeholder(placeholder);
	}

	/**
	 * @param color the placeholder color for this field; if {@code null}, the
	 * {@linkplain #getDisabledTextColor() disabled color} will be used
	 */
	public void setPlaceholderColor(@Nullable Color color) {
		this.placeholderColor = color;
	}

	@Override
	public void processMouseEvent(MouseEvent event, MenuElement[] path, MenuSelectionManager manager) { }

	@Override
	public void processKeyEvent(KeyEvent event, MenuElement[] path, MenuSelectionManager manager) { }

	@Override
	public void menuSelectionChanged(boolean isIncluded) { }

	@Override
	public MenuElement[] getSubElements() {
		return new MenuElement[0];
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public void setFont(Font f) {
		super.setFont(f);

		if (this.placeholder != null) {
			this.placeholder.clearWidth();
		}
	}

	private class Placeholder {
		static final int UNSET_WIDTH = -1;

		final String text;

		int width = UNSET_WIDTH;

		Placeholder(String text) {
			this.text = text;
		}

		int getWidth() {
			if (this.width < 0) {
				this.width = PlaceheldTextField.this
					.getFontMetrics(PlaceheldTextField.this.getFont()).stringWidth(this.text);
			}

			return this.width;
		}

		void clearWidth() {
			this.width = UNSET_WIDTH;
		}
	}
}
