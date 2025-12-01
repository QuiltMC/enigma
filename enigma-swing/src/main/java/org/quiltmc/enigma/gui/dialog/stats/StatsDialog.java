package org.quiltmc.enigma.gui.dialog.stats;

import org.quiltmc.enigma.api.stats.GenerationParameters;
import org.quiltmc.enigma.api.stats.ProjectStatsResult;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.api.stats.StatsGenerator;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.dialog.ProgressDialog;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StatsDialog {
	public static void show(Gui gui) {
		GenerationParameters parameters = Config.stats().createGenParameters(gui.getEditableStatTypes());
		StatsGenerator generator = gui.getController().getStatsGenerator();
		if (generator != null) {
			ProjectStatsResult nullableResult = generator.getResultNullable(parameters);

			if (nullableResult == null) {
				generateAndShow(gui, generator, parameters);
			} else {
				SwingUtilities.invokeLater(() -> show(gui, nullableResult, ""));
			}
		} else {
			throw new IllegalStateException("Cannot open stats dialog without a project open! (stats generator is null)");
		}
	}

	public static void generateAndShow(Gui gui, StatsGenerator generator, GenerationParameters parameters) {
		ProgressDialog.runOffThread(gui, listener -> {
			// hook into current stat generation progress
			if (generator.getOverallProgress() != null) {
				listener.sync(generator.getOverallProgress());
			}

			ProjectStatsResult result = generator.getResult(parameters);
			SwingUtilities.invokeLater(() -> show(gui, result, ""));
		});
	}

	public static void show(Gui gui, ProjectStatsResult result, String packageName) {
		// init frame
		JDialog dialog = new JDialog(gui.getFrame(), packageName.isEmpty() ? I18n.translate("menu.file.stats.title") : I18n.translateFormatted("menu.file.stats.title_filtered", packageName), true);
		Container contentPane = dialog.getContentPane();
		contentPane.setLayout(new GridBagLayout());

		GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(2);

		String overallText = I18n.translate("menu.file.stats.overall") + " - " + String.format("%.2f%%", result.getPercentage(StatType.values()));
		contentPane.add(new JLabel(overallText), GridBagConstraintsBuilder.create().width(20).anchor(GridBagConstraints.CENTER).build());

		contentPane.add(new JScrollPane(new StatTable(result)), cb.pos(0, 1).anchor(GridBagConstraints.EAST).fill(GridBagConstraints.HORIZONTAL).weightX(1.0).build());

		GridBagConstraintsBuilder cb1 = cb.pos(0, 0).width(5).weightX(1.0).anchor(GridBagConstraints.WEST);

		// show top-level package option
		JLabel topLevelPackageOption = new JLabel(I18n.translate("menu.file.stats.top_level_package"));
		contentPane.add(topLevelPackageOption, cb1.pos(0, result.getOverall().getTypes().size() + 1).build());

		JTextField topLevelPackage = new JTextField();
		topLevelPackage.setText(Config.stats().lastTopLevelPackage.value());
		contentPane.add(topLevelPackage, cb1.pos(0, result.getOverall().getTypes().size() + 2).fill(GridBagConstraints.HORIZONTAL).build());

		// show synthetic members option
		JCheckBox syntheticParametersOption = new JCheckBox(I18n.translate("menu.file.stats.synthetic_parameters"));
		syntheticParametersOption.setSelected(Config.stats().shouldIncludeSyntheticParameters.value());
		contentPane.add(syntheticParametersOption, cb1.pos(0, result.getOverall().getTypes().size() + 4).build());

		// show synthetic members option
		JCheckBox countFallbackOption = new JCheckBox(I18n.translate("menu.file.stats.count_fallback"));
		countFallbackOption.setSelected(Config.stats().shouldCountFallbackNames.value());
		contentPane.add(countFallbackOption, cb1.pos(0, result.getOverall().getTypes().size() + 3).build());

		// show filter button
		JButton filterButton = new JButton(I18n.translate("menu.file.stats.filter"));
		filterButton.addActionListener(action -> {
			dialog.dispose();
			ProgressDialog.runOffThread(gui, listener -> {
				String topLevelPackageSlashes = topLevelPackage.getText().replace('.', '/');
				Config.stats().lastTopLevelPackage.setValue(topLevelPackage.getText(), true);

				GenerationParameters parameters = new GenerationParameters(gui.getEditableStatTypes(), syntheticParametersOption.isSelected(), countFallbackOption.isSelected());
				StatsGenerator generator = gui.getController().getStatsGenerator();
				ProjectStatsResult projectResult = generator.getResult(parameters).filter(topLevelPackageSlashes);

				SwingUtilities.invokeLater(() -> show(gui, projectResult, topLevelPackageSlashes));
			});
		});
		contentPane.add(filterButton, cb1.pos(0, result.getOverall().getTypes().size() + 5).anchor(GridBagConstraints.EAST).build());

		// show generate button
		JButton button = new JButton(I18n.translate("menu.file.stats.generate"));
		button.setEnabled(true);
		button.addActionListener(action -> {
			dialog.dispose();
			showGenerateDiagramDialog(gui, result);
		});

		contentPane.add(button, cb1.pos(0, result.getOverall().getTypes().size() + 5).weightY(1.0).anchor(GridBagConstraints.SOUTHWEST).build());

		// show the frame
		dialog.pack();
		Dimension size = dialog.getSize();
		dialog.setMinimumSize(size);
		size.width = ScaleUtil.scale(350);
		dialog.setSize(size);
		dialog.setLocationRelativeTo(gui.getFrame());
		dialog.setVisible(true);
	}

	private static void showGenerateDiagramDialog(Gui gui, ProjectStatsResult result) {
		JDialog dialog = new JDialog();

		dialog.setTitle("Generate Diagram");
		dialog.setLayout(new GridLayout(0, 1));
		Container contentPane = dialog.getContentPane();

		Map<StatType, JCheckBox> checkboxes = new EnumMap<>(StatType.class);
		for (StatType type : result.getOverall().getTypes()) {
			JCheckBox checkbox = new JCheckBox(type.getName());
			checkboxes.put(type, checkbox);
			contentPane.add(checkbox);
		}

		JButton generateButton = new JButton("Generate");
		generateButton.addActionListener(action -> {
			dialog.dispose();
			generateStats(gui, checkboxes);
		});

		for (JCheckBox checkbox : checkboxes.values()) {
			checkbox.addActionListener(action -> generateButton.setEnabled(checkboxes.values().stream().anyMatch(JCheckBox::isSelected)));
		}

		contentPane.add(generateButton);
		dialog.setSize(300, 200);
		dialog.setLocationRelativeTo(null);
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
