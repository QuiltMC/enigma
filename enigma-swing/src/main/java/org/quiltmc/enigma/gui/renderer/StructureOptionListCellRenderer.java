package org.quiltmc.enigma.gui.renderer;

import org.quiltmc.enigma.analysis.StructureTreeOptions;
import org.quiltmc.enigma.util.I18n;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

public class StructureOptionListCellRenderer extends DefaultListCellRenderer {
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		if (value instanceof StructureTreeOptions.Option option) {
			this.setText(I18n.translate(option.getTranslationKey()));
		}

		return c;
	}
}
