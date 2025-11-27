package org.quiltmc.enigma.gui.element;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.Utils;

import javax.swing.JTextField;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.Document;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * A text field that displays placeholder text when it's empty.
 */
public class PlaceheldTextField extends JTextField implements MenuElement {
	private static final int DEFAULT_SELECTION_BORDER_LEFT = ScaleUtil.scale(3);
	private static final int DEFAULT_SELECTION_BORDER_RIGHT = DEFAULT_SELECTION_BORDER_LEFT;
	private static final int DEFAULT_SELECTION_BORDER_TOP = ScaleUtil.scale(1);
	private static final int DEFAULT_SELECTION_BORDER_BOTTOM = DEFAULT_SELECTION_BORDER_TOP;

	@Nullable
	private Placeholder placeholder;

	@Nullable
	private Color placeholderColor;

	private CompoundBorder defaultBorder;

	private CompoundBorder selectionBorder;

	private boolean selectionIncluded;

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

		final Border originalBorder = this.getBorder();
		this.selectionBorder = createCompoundBorder(
			new MatteBorder(
				DEFAULT_SELECTION_BORDER_TOP, DEFAULT_SELECTION_BORDER_LEFT,
				DEFAULT_SELECTION_BORDER_BOTTOM, DEFAULT_SELECTION_BORDER_RIGHT,
				UIManager.getColor("MenuItem.selectionBackground")
			),
			originalBorder
		);

		this.defaultBorder = createCompoundBorder(
			createEmptyBorder(
				DEFAULT_SELECTION_BORDER_TOP, DEFAULT_SELECTION_BORDER_LEFT,
				DEFAULT_SELECTION_BORDER_BOTTOM, DEFAULT_SELECTION_BORDER_RIGHT
			),
			originalBorder
		);

		super.setBorder(this.defaultBorder);
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
	public void setBorder(Border border) {
		if (this.selectionBorder == null) {
			// JTextField sets border in constructor before selectionBorder is initialized
			super.setBorder(border);
		} else {
			this.defaultBorder = this.createDefaultBorder(border);
			this.selectionBorder = createCompoundBorder(this.selectionBorder.getOutsideBorder(), border);
			super.setBorder(this.defaultBorder);
		}
	}

	private CompoundBorder createDefaultBorder(Border border) {
		final Border selectionOuter = this.selectionBorder.getOutsideBorder();
		if (selectionOuter == null) {
			return createCompoundBorder(null, border);
		} else {
			final Insets selectionInsets = selectionOuter.getBorderInsets(this);
			return createCompoundBorder(
				createEmptyBorder(selectionInsets.top, selectionInsets.left, selectionInsets.bottom, selectionInsets.right),
				border
			);
		}
	}

	public void setSelectionBorder(Border border) {
		final Border insideBorder = this.defaultBorder.getInsideBorder();
		this.selectionBorder = createCompoundBorder(border, insideBorder);
		this.defaultBorder = this.createDefaultBorder(insideBorder);
		super.setBorder(this.defaultBorder);
	}

	@Override
	protected void paintBorder(Graphics graphics) {
		if (this.selectionIncluded) {
			final int leftInset;
			final int topInset;
			{
				final Container parent = this.getParent();

				if (parent == null) {
					leftInset = 0;
					topInset = 0;
				} else {
					final Insets insets = parent.getInsets();
					leftInset = insets.left;
					topInset = insets.top;
				}
			}

			this.selectionBorder.paintBorder(
					this, graphics,
					this.getX() - leftInset, this.getY() - topInset,
					this.getWidth(), this.getHeight()
			);
		} else {
			super.paintBorder(graphics);
		}
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
	public void menuSelectionChanged(boolean isIncluded) {
		if (this.selectionIncluded != isIncluded) {
			this.selectionIncluded = isIncluded;
			// update border
			this.repaint();
		}
	}

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
