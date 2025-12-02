package org.quiltmc.enigma.gui.dialog.stats;

import org.quiltmc.enigma.api.stats.ProjectStatsResult;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;

public class StatTable extends JPanel {
	public StatTable(ProjectStatsResult result) {
		this.setLayout(new BorderLayout());
		JTable table = new JTable();
		table.setCellSelectionEnabled(false);
		table.setModel(new StatTableModel(result));
		table.setDefaultRenderer(Double.class, new ProgressBarRenderer());
		this.add(table.getTableHeader(), BorderLayout.NORTH);
		this.add(table, BorderLayout.CENTER);
	}

	private static class ProgressBarRenderer implements TableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			return new StatProgressBar((double) value, false);
		}
	}
}
