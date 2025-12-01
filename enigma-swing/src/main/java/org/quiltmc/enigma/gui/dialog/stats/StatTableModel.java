package org.quiltmc.enigma.gui.dialog.stats;

import org.quiltmc.enigma.api.stats.ProjectStatsResult;
import org.quiltmc.enigma.api.stats.StatType;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Function;

public class StatTableModel extends AbstractTableModel {
	private final ProjectStatsResult result;

	public StatTableModel(ProjectStatsResult result) {
		this.result = result;
	}

	@Override
	public int getRowCount() {
		return this.result.getOverall().getTypes().size();
	}

	@Override
	public int getColumnCount() {
		return Column.values().length;
	}

	@Override
	public String getColumnName(int column) {
		return this.getColumn(column).getName();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return this.getColumn(columnIndex).getType();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return this.getColumn(columnIndex).getValue(this.result, this.getType(rowIndex));
	}

	private Column getColumn(int columnIndex) {
		return Column.values()[columnIndex];
	}

	private StatType getType(int rowIndex) {
		return this.result.getOverall().getTypes().stream()
			.sorted(Comparator.comparing(StatType::getName))
			.skip(rowIndex)
			.findFirst()
			.orElseThrow();
	}

	enum Column {
		TYPE("Type", String.class, (result, type) -> type.getName()),
		MAPPED("Mapped", Integer.class, ProjectStatsResult::getMapped),
		TOTAL("Total", Integer.class, ProjectStatsResult::getMappable),
		PROGRESS_BAR("Progress", Double.class, ProjectStatsResult::getPercentage);

		private final String name;
		private final Class<?> type;
		private final BiFunction<ProjectStatsResult, StatType, ?> getter;

		<T> Column(String name, Class<T> type, BiFunction<ProjectStatsResult, StatType, T> getter) {
			this.name = name;
			this.type = type;
			this.getter = getter;
		}

		//TODO localize
		public String getName() {
			return this.name;
		}

		public Class<?> getType() {
			return this.type;
		}

		public Object getValue(ProjectStatsResult result, StatType type) {
			return this.getter.apply(result, type);
		}
	}
}
