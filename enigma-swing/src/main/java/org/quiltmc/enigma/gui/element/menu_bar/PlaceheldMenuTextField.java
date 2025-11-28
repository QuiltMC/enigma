package org.quiltmc.enigma.gui.element.menu_bar;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.gui.element.PlaceheldTextField;
import org.quiltmc.enigma.gui.util.ScaleUtil;

import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.Document;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import static org.quiltmc.enigma.gui.util.GuiUtil.EMPTY_MENU_ELEMENTS;
import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * A {@link PlaceheldTextField} that is also a {@link MenuElement}.
 *
 * <p> Displays an {@linkplain #setSelectionBorder(Border) additional border} when part of the
 * {@linkplain MenuSelectionManager#getSelectedPath() menu selection}.
 */
public class PlaceheldMenuTextField extends PlaceheldTextField implements MenuElement {
	private static final int DEFAULT_SELECTION_BORDER_LEFT = ScaleUtil.scale(3);
	private static final int DEFAULT_SELECTION_BORDER_RIGHT = DEFAULT_SELECTION_BORDER_LEFT;
	private static final int DEFAULT_SELECTION_BORDER_TOP = ScaleUtil.scale(1);
	private static final int DEFAULT_SELECTION_BORDER_BOTTOM = DEFAULT_SELECTION_BORDER_TOP;

	private CompoundBorder defaultBorder;
	private CompoundBorder selectionBorder;

	private boolean selectionIncluded;

	private int minHeight = -1;

	/**
	 * @see PlaceheldTextField#PlaceheldTextField() PlaceheldTextField
	 */
	public PlaceheldMenuTextField() {
		this(null, null);
	}

	/**
	 * @see PlaceheldTextField#PlaceheldTextField(String, String) PlaceheldTextField
	 */
	public PlaceheldMenuTextField(String text, String placeholder) {
		this(null, text, placeholder, DEFAULT_COLUMNS);
	}

	/**
	 * @see PlaceheldTextField#PlaceheldTextField(Document, String, String, int) PlaceheldTextField
	 */
	public PlaceheldMenuTextField(
			@Nullable Document doc, @Nullable String text, @Nullable String placeholder, int columns
	) {
		super(doc, text, placeholder, columns);

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
		return EMPTY_MENU_ELEMENTS;
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public Dimension getPreferredSize() {
		final Dimension size = super.getPreferredSize();

		size.height = Math.max(size.height, this.getMinHeight());

		return size;
	}

	private int getMinHeight() {
		if (this.minHeight < 0) {
			// HACK: have at least the height of a menu item
			// this fixes containing popup menus' positions being off at small scales when this is the only item
			this.minHeight = new JMenuItem().getPreferredSize().height;
		}

		return this.minHeight;
	}
}
