package org.quiltmc.enigma.gui.dialog.stats;

import org.quiltmc.enigma.api.stats.GenerationParameters;
import org.quiltmc.enigma.api.stats.ProjectStatsResult;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.api.stats.StatsGenerator;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.dialog.ProgressDialog;
import org.quiltmc.enigma.gui.util.*;
import org.quiltmc.enigma.util.I18n;

import javax.swing.*;
import java.awt.*;
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

		JPanel header = new JPanel(new GridBagLayout());

		header.add(new StatProgressBar(result.getPercentage(), true), cb.pos(0, 1).weightX(1.0).build());

		JPanel activeSettingsPanel = new JPanel();
		activeSettingsPanel.setLayout(new BoxLayout(activeSettingsPanel, BoxLayout.Y_AXIS));
		JLabel activeSettingsLabel = new JLabel(I18n.translate("menu.file.stats.active_settings"));
		activeSettingsLabel.setFont(activeSettingsLabel.getFont().deriveFont(16.0f));
		activeSettingsLabel.setHorizontalAlignment(JLabel.CENTER);
		activeSettingsPanel.add(activeSettingsLabel);

		if (!packageName.isEmpty()) {
			activeSettingsPanel.add(GuiUtil.unboldLabel(new JLabel(I18n.translate("menu.file.stats.settings.top_level_package.short") + " " + packageName)));
		}

		if (Config.stats().shouldIncludeSyntheticParameters.value()) {
			activeSettingsPanel.add(GuiUtil.unboldLabel(new JLabel(I18n.translate("menu.file.stats.settings.synthetic_parameters"))));
		}

		if (Config.stats().shouldCountFallbackNames.value()) {
			activeSettingsPanel.add(GuiUtil.unboldLabel(new JLabel(I18n.translate("menu.file.stats.settings.count_fallback"))));
		}

		if (packageName.isEmpty() && !Config.stats().shouldIncludeSyntheticParameters.value() && !Config.stats().shouldCountFallbackNames.value()) {
			activeSettingsPanel.add(GuiUtil.unboldLabel(new JLabel(I18n.translate("menu.file.stats.settings.default"))));
		}

		activeSettingsPanel.setPreferredSize(new Dimension(200, 100));
		header.add(activeSettingsPanel, cb.pos(1, 1).weightX(2.0).anchor(GridBagConstraints.NORTHWEST).build());

		contentPane.add(header, cb.fill(GridBagConstraints.HORIZONTAL).anchor(GridBagConstraints.CENTER).build());

		contentPane.add(new JScrollPane(new StatTable(result)), cb.pos(0, 1).anchor(GridBagConstraints.EAST).fill(GridBagConstraints.HORIZONTAL).weightX(1.0).build());

		GridBagConstraintsBuilder cb1 = cb.width(5).weightX(1.0).anchor(GridBagConstraints.WEST);

		// show filter button
		JButton settingsButton = new JButton(I18n.translate("menu.file.stats.settings"));
		settingsButton.addActionListener(action -> {
			dialog.dispose();
			showSettingsDialog(gui);
		});
		contentPane.add(settingsButton, cb1.pos(0, result.getOverall().getTypes().size() + 5).anchor(GridBagConstraints.EAST).build());

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
		dialog.setMinimumSize((Dimension) size.clone());
		size.width = ScaleUtil.scale(350);
		dialog.setSize(size);
		dialog.setLocationRelativeTo(gui.getFrame());
		dialog.setVisible(true);
	}

	private static void showGenerateDiagramDialog(Gui gui, ProjectStatsResult result) {
		JDialog dialog = new JDialog(gui.getFrame(), I18n.translate("menu.file.stats.generate"), true);
		dialog.setLayout(new GridLayout(0, 1));
		Container contentPane = dialog.getContentPane();

		Map<StatType, JCheckBox> checkboxes = new EnumMap<>(StatType.class);
		for (StatType type : result.getOverall().getTypes()) {
			JCheckBox checkbox = new JCheckBox(type.getName());
			checkboxes.put(type, checkbox);
			contentPane.add(checkbox);
		}

		JButton generateButton = new JButton(I18n.translate("menu.file.stats.generate.short"));
		generateButton.addActionListener(action -> {
			dialog.dispose();
			generateStats(gui, checkboxes);
		});

		for (JCheckBox checkbox : checkboxes.values()) {
			checkbox.addActionListener(action -> generateButton.setEnabled(checkboxes.values().stream().anyMatch(JCheckBox::isSelected)));
		}

		contentPane.add(generateButton);

		dialog.pack();
		Dimension size = dialog.getSize();
		dialog.setMinimumSize((Dimension) size.clone());
		size.width = ScaleUtil.scale(175);
		dialog.setSize(size);
		dialog.setLocationRelativeTo(gui.getFrame());
		dialog.setVisible(true);
	}

	private static void showSettingsDialog(Gui gui) {
		JDialog dialog = new JDialog(gui.getFrame(), I18n.translate("menu.file.stats.settings.title"), true);
		dialog.setLayout(new GridBagLayout());
		Container contentPane = dialog.getContentPane();

		GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().pos(0, 0).width(5).weightX(1.0).anchor(GridBagConstraints.WEST);

		// show top-level package option
		JLabel topLevelPackageOption = new JLabel(I18n.translate("menu.file.stats.settings.top_level_package"));
		contentPane.add(topLevelPackageOption, cb.pos(0, 1).build());

		JTextField topLevelPackage = new JTextField();
		topLevelPackage.setText(Config.stats().lastTopLevelPackage.value());
		contentPane.add(topLevelPackage, cb.pos(0, 2).fill(GridBagConstraints.HORIZONTAL).build());

		// show synthetic members option
		JCheckBox syntheticParametersOption = new JCheckBox(I18n.translate("menu.file.stats.settings.synthetic_parameters"));
		syntheticParametersOption.setSelected(Config.stats().shouldIncludeSyntheticParameters.value());
		contentPane.add(syntheticParametersOption, cb.pos(0, 3).build());

		// show synthetic members option
		JCheckBox countFallbackOption = new JCheckBox(I18n.translate("menu.file.stats.settings.count_fallback"));
		countFallbackOption.setSelected(Config.stats().shouldCountFallbackNames.value());
		contentPane.add(countFallbackOption, cb.pos(0, 4).build());

		JButton applyButton = new JButton(I18n.translate("prompt.apply"));
		applyButton.addActionListener(action -> {
			dialog.dispose();
			ProgressDialog.runOffThread(gui, listener -> {
				String topLevelPackageSlashes = topLevelPackage.getText().replace('.', '/');

				Config.stats().lastTopLevelPackage.setValue(topLevelPackage.getText(), true);
				Config.stats().shouldIncludeSyntheticParameters.setValue(syntheticParametersOption.isSelected(), true);
				Config.stats().shouldCountFallbackNames.setValue(countFallbackOption.isSelected(), true);

				GenerationParameters parameters = new GenerationParameters(gui.getEditableStatTypes(), syntheticParametersOption.isSelected(), countFallbackOption.isSelected());
				StatsGenerator generator = gui.getController().getStatsGenerator();
				ProjectStatsResult projectResult = generator.getResult(parameters).filter(topLevelPackageSlashes);

				SwingUtilities.invokeLater(() -> show(gui, projectResult, topLevelPackageSlashes));
			});
		});
		contentPane.add(applyButton, cb.pos(0, 5).anchor(GridBagConstraints.EAST).build());

		JButton cancelButton = new JButton(I18n.translate("prompt.cancel"));
		cancelButton.addActionListener(action -> {
			dialog.dispose();
			show(gui);
		});
		contentPane.add(cancelButton, cb.pos(1, 5).anchor(GridBagConstraints.WEST).build());

		dialog.pack();
		dialog.setMinimumSize(dialog.getSize());
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
