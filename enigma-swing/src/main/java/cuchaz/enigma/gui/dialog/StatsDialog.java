package cuchaz.enigma.gui.dialog;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.stats.StatType;
import cuchaz.enigma.stats.StatsResult;
import cuchaz.enigma.gui.util.GridBagConstraintsBuilder;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

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
		ProgressDialog.runOffThread(gui, listener -> {
			StatsResult result = gui.getStatsManager().getGenerator().generate(listener, Set.of(StatType.values()), "", false);

			SwingUtilities.invokeLater(() -> show(gui, result, ""));
		});
	}

	public static void show(Gui gui, StatsResult result, String packageName) {
		// init frame
		JDialog dialog = new JDialog(gui.getFrame(), packageName.isEmpty() ? I18n.translate("menu.file.stats.title") : I18n.translateFormatted("menu.file.stats.title_filtered", packageName), true);
		Container contentPane = dialog.getContentPane();
		contentPane.setLayout(new GridBagLayout());

		GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(2);

		Map<StatType, JCheckBox> checkboxes = new EnumMap<>(StatType.class);

		final int[] i = {0};
		result.getTypes().stream().sorted(Comparator.comparing(StatType::getName)).forEach(type -> {
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
		contentPane.add(topLevelPackageOption, cb1.pos(0, result.getTypes().size() + 1).build());

		JTextField topLevelPackage = new JTextField();
		topLevelPackage.setText(UiConfig.getLastTopLevelPackage());
		contentPane.add(topLevelPackage, cb1.pos(0, result.getTypes().size() + 2).fill(GridBagConstraints.HORIZONTAL).build());

		// Show filter button
		JButton filterButton = new JButton(I18n.translate("menu.file.stats.filter"));
		filterButton.addActionListener(action -> {
			dialog.dispose();
			ProgressDialog.runOffThread(gui, listener -> {
				UiConfig.setLastTopLevelPackage(topLevelPackage.getText());
				UiConfig.save();

				StatsResult statResult = gui.getStatsManager().getGenerator().generate(listener, Set.of(StatType.values()), UiConfig.getLastTopLevelPackage(), false);

				SwingUtilities.invokeLater(() -> show(gui, statResult, UiConfig.getLastTopLevelPackage()));
			});
		});
		contentPane.add(filterButton, cb1.pos(0, result.getTypes().size() + 3).anchor(GridBagConstraints.EAST).build());

		// show synthetic members option
		JCheckBox syntheticParametersOption = new JCheckBox(I18n.translate("menu.file.stats.synthetic_parameters"));
		syntheticParametersOption.setSelected(UiConfig.shouldIncludeSyntheticParameters());
		contentPane.add(syntheticParametersOption, cb1.pos(0, result.getTypes().size() + 4).build());

		// show generate button
		JButton button = new JButton(I18n.translate("menu.file.stats.generate"));
		button.setEnabled(false);
		button.addActionListener(action -> {
			dialog.dispose();

			UiConfig.setLastTopLevelPackage(topLevelPackage.getText());
			UiConfig.setIncludeSyntheticParameters(syntheticParametersOption.isSelected());
			UiConfig.save();

			generateStats(gui, checkboxes, topLevelPackage.getText(), syntheticParametersOption.isSelected());
		});

		contentPane.add(button, cb1.pos(0, result.getTypes().size() + 5).weightY(1.0).anchor(GridBagConstraints.SOUTHWEST).build());

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

	private static void generateStats(Gui gui, Map<StatType, JCheckBox> checkboxes, String topLevelPackage, boolean includeSynthetic) {
		// get members from selected checkboxes
		Set<StatType> includedMembers = checkboxes
				.entrySet()
				.stream()
				.filter(entry -> entry.getValue().isSelected())
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		// checks if a project is open
		if (gui.getController().getProject() != null) {
			gui.getController().openStats(includedMembers, topLevelPackage, includeSynthetic);
		}
	}
}
