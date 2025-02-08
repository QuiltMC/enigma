package org.quiltmc.enigma.gui.dialog;

import org.quiltmc.enigma.api.stats.GenerationParameters;
import org.quiltmc.enigma.api.stats.StatsGenerator;
import org.quiltmc.enigma.gui.EditableType;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.api.stats.ProjectStatsResult;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.I18n;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class StatsDialog {
	public static void show(Gui gui) {
		StatsGenerator generator = gui.getController().getStatsGenerator();
		ProjectStatsResult nullableResult = generator.getResultNullable();

		if (nullableResult == null) {
			ProgressDialog.runOffThread(gui, listener -> {
				// hook into current stat generation progress
				if (generator.getOverallProgress() != null) {
					listener.sync(generator.getOverallProgress());
				}

				ProjectStatsResult result = gui.getController().getStatsGenerator().getResult(new GenerationParameters(EditableType.toStatTypes(gui.getEditableTypes())));
				SwingUtilities.invokeLater(() -> show(gui, result, ""));
			});
		} else {
			SwingUtilities.invokeLater(() -> show(gui, nullableResult, ""));
		}
	}

	public static void show(Gui gui, ProjectStatsResult result, String packageName) {
		// init frame
		JDialog dialog = new JDialog(gui.getFrame(), packageName.isEmpty() ? I18n.translate("menu.file.stats.title") : I18n.translateFormatted("menu.file.stats.title_filtered", packageName), true);
		Container contentPane = dialog.getContentPane();
		contentPane.setLayout(new GridBagLayout());

		GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(2);

		Map<StatType, JCheckBox> checkboxes = new EnumMap<>(StatType.class);

		String overallText = I18n.translate("menu.file.stats.overall") + " - " + String.format("%.2f%%", result.getPercentage(StatType.values()));
		contentPane.add(new JLabel(overallText), GridBagConstraintsBuilder.create().width(20).anchor(GridBagConstraints.CENTER).build());

		final int[] i = {1};
		result.getOverall().getTypes().stream().sorted(Comparator.comparing(StatType::getName)).forEach(type -> {
			JCheckBox checkBox = new JCheckBox(type.getName());
			checkboxes.put(type, checkBox);
			contentPane.add(checkBox, cb.pos(0, i[0]).weightX(1.0).anchor(GridBagConstraints.WEST).build());

			GridBagConstraintsBuilder labels = cb.anchor(GridBagConstraints.EAST);

			contentPane.add(new JLabel(Integer.toString(result.getMapped(type))), labels.pos(1, i[0]).build());
			contentPane.add(new JLabel("/"), labels.pos(2, i[0]).build());
			contentPane.add(new JLabel(Integer.toString(result.getMappable(type))), labels.pos(3, i[0]).build());
			contentPane.add(new JLabel(String.format("%.2f%%", result.getPercentage(type))), labels.pos(4, i[0]).build());

			i[0]++;
		});

		GridBagConstraintsBuilder cb1 = cb.pos(0, 0).width(5).weightX(1.0).anchor(GridBagConstraints.WEST);

		// show top-level package option
		JLabel topLevelPackageOption = new JLabel(I18n.translate("menu.file.stats.top_level_package"));
		contentPane.add(topLevelPackageOption, cb1.pos(0, result.getOverall().getTypes().size() + 1).build());

		JTextField topLevelPackage = new JTextField();
		topLevelPackage.setText(Config.main().stats.lastTopLevelPackage.value());
		contentPane.add(topLevelPackage, cb1.pos(0, result.getOverall().getTypes().size() + 2).fill(GridBagConstraints.HORIZONTAL).build());

		// show synthetic members option
		JCheckBox syntheticParametersOption = new JCheckBox(I18n.translate("menu.file.stats.synthetic_parameters"));
		syntheticParametersOption.setSelected(Config.main().stats.shouldIncludeSyntheticParameters.value());
		contentPane.add(syntheticParametersOption, cb1.pos(0, result.getOverall().getTypes().size() + 4).build());

		// show synthetic members option
		JCheckBox countFallbackOption = new JCheckBox(I18n.translate("menu.file.stats.count_fallback"));
		countFallbackOption.setSelected(Config.main().stats.shouldCountFallbackNames.value());
		contentPane.add(countFallbackOption, cb1.pos(0, result.getOverall().getTypes().size() + 3).build());

		// show filter button
		JButton filterButton = new JButton(I18n.translate("menu.file.stats.filter"));
		filterButton.addActionListener(action -> {
			dialog.dispose();
			ProgressDialog.runOffThread(gui, listener -> {
				String topLevelPackageSlashes = topLevelPackage.getText().replace('.', '/');
				Config.main().stats.lastTopLevelPackage.setValue(topLevelPackage.getText(), true);

				GenerationParameters parameters = new GenerationParameters(EditableType.toStatTypes(gui.getEditableTypes()), syntheticParametersOption.isSelected(), countFallbackOption.isSelected());
				StatsGenerator generator = gui.getController().getStatsGenerator();
				ProjectStatsResult projectResult = generator.getResult(parameters).filter(topLevelPackageSlashes);

				SwingUtilities.invokeLater(() -> show(gui, projectResult, topLevelPackageSlashes));
			});
		});
		contentPane.add(filterButton, cb1.pos(0, result.getOverall().getTypes().size() + 5).anchor(GridBagConstraints.EAST).build());

		// show generate button
		JButton button = new JButton(I18n.translate("menu.file.stats.generate"));
		button.setEnabled(false);
		button.addActionListener(action -> {
			dialog.dispose();

			Config.main().stats.lastTopLevelPackage.setValue(topLevelPackage.getText());
			Config.main().stats.shouldIncludeSyntheticParameters.setValue(syntheticParametersOption.isSelected());
			Config.main().stats.shouldCountFallbackNames.setValue(countFallbackOption.isSelected());

			generateStats(gui, checkboxes);
		});

		contentPane.add(button, cb1.pos(0, result.getOverall().getTypes().size() + 5).weightY(1.0).anchor(GridBagConstraints.SOUTHWEST).build());

		// add action listener to each checkbox
		checkboxes.forEach((key, value) -> value.addActionListener(action -> {
			if (!button.isEnabled()) {
				button.setEnabled(true);
			} else if (checkboxes.entrySet().stream().noneMatch(entry -> entry.getValue().isSelected())) {
				button.setEnabled(false);
			}
		}));

		// show the frame
		dialog.pack();
		Dimension size = dialog.getSize();
		dialog.setMinimumSize(size);
		size.width = ScaleUtil.scale(350);
		dialog.setSize(size);
		dialog.setLocationRelativeTo(gui.getFrame());
		dialog.setVisible(true);
	}

	private static void generateStats(Gui gui, Map<StatType, JCheckBox> checkboxes) {
		// get members from selected checkboxes
		Set<StatType> includedMembers = checkboxes
				.entrySet()
				.stream()
				.filter(entry -> entry.getValue().isSelected())
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		// checks if a project is open
		if (gui.getController().getProject() != null) {
			gui.getController().openStatsTree(includedMembers);
		}
	}
}
