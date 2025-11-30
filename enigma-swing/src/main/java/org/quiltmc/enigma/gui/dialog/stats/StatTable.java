package org.quiltmc.enigma.gui.dialog.stats;

import org.quiltmc.enigma.api.stats.ProjectStatsResult;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public class StatTable extends JPanel {
	public StatTable(ProjectStatsResult result) {
		this.setLayout(new BorderLayout());
		JTable table = new JTable();
		table.setModel(new StatTableModel(result));
		table.setDefaultRenderer(StatProgressBar.class, new ProgressBarRenderer());
		this.add(table.getTableHeader(), BorderLayout.NORTH);
		this.add(table, BorderLayout.CENTER);
	}

	private static class ProgressBarRenderer implements TableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			return (StatProgressBar) value;
		}
	}
}
