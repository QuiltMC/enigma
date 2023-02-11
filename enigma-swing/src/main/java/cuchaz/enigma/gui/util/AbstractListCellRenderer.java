package cuchaz.enigma.gui.util;

import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.border.Border;

public abstract class AbstractListCellRenderer<E> extends JPanel implements ListCellRenderer<E> {
	private static final Border NO_FOCUS_BORDER = BorderFactory.createEmptyBorder(1, 1, 1, 1);

	private Border noFocusBorder;

	public AbstractListCellRenderer() {
		this.setBorder(this.getNoFocusBorder());
	}

	protected Border getNoFocusBorder() {
		if (this.noFocusBorder == null) {
			Border border = UIManager.getLookAndFeel().getDefaults().getBorder("List.List.cellNoFocusBorder");
			this.noFocusBorder = border != null ? border : NO_FOCUS_BORDER;
		}
		return this.noFocusBorder;
	}

	protected Border getBorder(boolean isSelected, boolean cellHasFocus) {
		Border b = null;
		if (cellHasFocus) {
			UIDefaults defaults = UIManager.getLookAndFeel().getDefaults();
			if (isSelected) {
				b = defaults.getBorder("List.focusSelectedCellHighlightBorder");
			}
			if (b == null) {
				b = defaults.getBorder("List.focusCellHighlightBorder");
			}
		} else {
			b = this.getNoFocusBorder();
		}
		return b;
	}

	public abstract void updateUiForEntry(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus);

	@Override
	public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
		this.updateUiForEntry(list, value, index, isSelected, cellHasFocus);

		if (isSelected) {
			this.setBackground(list.getSelectionBackground());
			this.setForeground(list.getSelectionForeground());
		} else {
			this.setBackground(list.getBackground());
			this.setForeground(list.getForeground());
		}

		this.setEnabled(list.isEnabled());
		this.setFont(list.getFont());

		this.setBorder(this.getBorder(isSelected, cellHasFocus));

		// This isn't the width of the cell, but it's close enough for where it's needed (getComponentAt in getToolTipText)
		this.setSize(list.getWidth(), this.getPreferredSize().height);

		return this;
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		Component c = this.getComponentAt(event.getPoint());
		if (c instanceof JComponent) {
			return ((JComponent) c).getToolTipText();
		}
		return this.getToolTipText();
	}
}
