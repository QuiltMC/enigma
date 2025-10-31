package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractEnigmaMenu;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.util.I18n;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.quiltmc.enigma.gui.config.EntryMarkersSection.MAX_MAX_MARKERS_PER_LINE;
import static org.quiltmc.enigma.gui.config.EntryMarkersSection.MIN_MAX_MARKERS_PER_LINE;

public class EntryMarkersMenu extends AbstractEnigmaMenu {
	@SuppressWarnings("SameParameterValue")
	private static JMenu createIntConfigRadioMenu(
			TrackedValue<Integer> config, int min, int max, Runnable onUpdate
	) {
		final Map<Integer, JRadioButtonMenuItem> radiosByChoice = IntStream.range(min, max + 1)
				.boxed()
				.collect(Collectors.toMap(
					Function.identity(),
					choice -> {
						final JRadioButtonMenuItem choiceItem = new JRadioButtonMenuItem();
						choiceItem.setText(Integer.toString(choice));
						if (choice.equals(config.value())) {
							choiceItem.setSelected(true);
						}

						final int finalChoice = choice;
						choiceItem.addActionListener(e -> {
							config.setValue(finalChoice);
							onUpdate.run();
						});

						return choiceItem;
					}
				));

		final ButtonGroup choicesGroup = new ButtonGroup();
		final JMenu menu = new JMenu();
		for (final JRadioButtonMenuItem radio : radiosByChoice.values()) {
			choicesGroup.add(radio);
			menu.add(radio);
		}

		config.registerCallback(updated -> {
			final JRadioButtonMenuItem choiceItem = radiosByChoice.get(updated.value());

			if (!choiceItem.isSelected()) {
				choiceItem.setSelected(true);
				onUpdate.run();
			}
		});

		return menu;
	}

	private final JCheckBoxMenuItem tooltip = GuiUtil
			.createSyncedMenuCheckBox(Config.editor().entryMarkers.tooltip);

	private final JMenu maxMarkersPerLineMenu = createIntConfigRadioMenu(
			Config.editor().entryMarkers.maxMarkersPerLine,
			MIN_MAX_MARKERS_PER_LINE, MAX_MAX_MARKERS_PER_LINE,
			this::translateMarkersPerLineMenu
	);

	private final JMenu markMenu = new JMenu();
	private final JCheckBoxMenuItem onlyMarkDeclarations = GuiUtil
			.createSyncedMenuCheckBox(Config.editor().entryMarkers.onlyMarkDeclarations);
	private final JCheckBoxMenuItem markObfuscated = GuiUtil
			.createSyncedMenuCheckBox(Config.editor().entryMarkers.markObfuscated);
	private final JCheckBoxMenuItem markFallback = GuiUtil
			.createSyncedMenuCheckBox(Config.editor().entryMarkers.markFallback);
	private final JCheckBoxMenuItem markProposed = GuiUtil
			.createSyncedMenuCheckBox(Config.editor().entryMarkers.markProposed);
	private final JCheckBoxMenuItem markDeobfuscated = GuiUtil
			.createSyncedMenuCheckBox(Config.editor().entryMarkers.markDeobfuscated);

	public EntryMarkersMenu(Gui gui) {
		super(gui);

		this.add(this.tooltip);

		this.add(this.maxMarkersPerLineMenu);

		this.markMenu.add(this.onlyMarkDeclarations);
		this.markMenu.add(new JToolBar.Separator());
		this.markMenu.add(this.markObfuscated);
		this.markMenu.add(this.markFallback);
		this.markMenu.add(this.markProposed);
		this.markMenu.add(this.markDeobfuscated);

		this.add(this.markMenu);

		this.retranslate();
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.view.entry_markers"));

		this.tooltip.setText(I18n.translate("menu.view.entry_markers.tooltip"));

		this.translateMarkersPerLineMenu();
		this.markMenu.setText(I18n.translate("menu.view.entry_markers.mark"));

		this.onlyMarkDeclarations.setText(I18n.translate("menu.view.entry_markers.mark.only_declarations"));
		this.markObfuscated.setText(I18n.translate("menu.view.entry_markers.mark.obfuscated"));
		this.markFallback.setText(I18n.translate("menu.view.entry_markers.mark.fallback"));
		this.markProposed.setText(I18n.translate("menu.view.entry_markers.mark.proposed"));
		this.markDeobfuscated.setText(I18n.translate("menu.view.entry_markers.mark.deobfuscated"));
	}

	private void translateMarkersPerLineMenu() {
		this.maxMarkersPerLineMenu.setText(I18n.translateFormatted(
				"menu.view.entry_markers.max_markers_per_line",
				Config.editor().entryMarkers.maxMarkersPerLine.value())
		);
	}
}
