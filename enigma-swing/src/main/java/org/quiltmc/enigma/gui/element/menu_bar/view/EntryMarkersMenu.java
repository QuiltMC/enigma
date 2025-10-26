package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.EntryMarkersSection;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractEnigmaMenu;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

public class EntryMarkersMenu extends AbstractEnigmaMenu {
	private static void syncStateWithConfig(JCheckBoxMenuItem box, TrackedValue<Boolean> config) {
		box.setState(config.value());

		box.addActionListener(e -> {
			final boolean checked = box.getState();
			if (checked != config.value()) {
				config.setValue(checked);
			}
		});

		config.registerCallback(updated -> {
			final boolean configured = updated.value();
			if (configured != box.getState()) {
				box.setState(configured);
			}
		});
	}

	private final JCheckBoxMenuItem interactable = new JCheckBoxMenuItem();

	private final JMenu markMenu = new JMenu();
	private final JCheckBoxMenuItem markObfuscated = new JCheckBoxMenuItem();
	private final JCheckBoxMenuItem markFallback = new JCheckBoxMenuItem();
	private final JCheckBoxMenuItem markProposed = new JCheckBoxMenuItem();
	private final JCheckBoxMenuItem markDeobfuscated = new JCheckBoxMenuItem();

	public EntryMarkersMenu(Gui gui) {
		super(gui);

		this.add(this.interactable);

		this.markMenu.add(this.markObfuscated);
		this.markMenu.add(this.markFallback);
		this.markMenu.add(this.markProposed);
		this.markMenu.add(this.markDeobfuscated);

		this.add(this.markMenu);

		final EntryMarkersSection markerConfig = Config.editor().entryMarkers;
		syncStateWithConfig(this.interactable, markerConfig.interactable);
		syncStateWithConfig(this.markObfuscated, markerConfig.markObfuscated);
		syncStateWithConfig(this.markFallback, markerConfig.markFallback);
		syncStateWithConfig(this.markProposed, markerConfig.markProposed);
		syncStateWithConfig(this.markDeobfuscated, markerConfig.markDeobfuscated);

		this.retranslate();
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.view.entry_markers"));

		this.interactable.setText(I18n.translate("menu.view.entry_markers.interactable"));

		this.markMenu.setText(I18n.translate("menu.view.entry_markers.mark"));

		this.markObfuscated.setText(I18n.translate("menu.view.entry_markers.mark.obfuscated"));
		this.markFallback.setText(I18n.translate("menu.view.entry_markers.mark.fallback"));
		this.markProposed.setText(I18n.translate("menu.view.entry_markers.mark.proposed"));
		this.markDeobfuscated.setText(I18n.translate("menu.view.entry_markers.mark.deobfuscated"));
	}
}
