package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.Decompiler;
import org.quiltmc.enigma.gui.dialog.decompiler.DecompilerSettingsDialog;
import org.quiltmc.enigma.util.I18n;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;
import java.util.stream.Stream;

public class DecompilerMenu extends AbstractSearchableEnigmaMenu {
	private static final String TRANSLATION_KEY = "menu.decompiler";

	private final SimpleItem decompilerSettingsItem = new SimpleItem("menu.decompiler.settings");

	public DecompilerMenu(Gui gui) {
		super(gui);

		ButtonGroup decompilerGroup = new ButtonGroup();

		for (Decompiler decompiler : Decompiler.values()) {
			DecompilerItem decompilerButton = new DecompilerItem(decompiler.name);
			decompilerGroup.add(decompilerButton);
			if (decompiler.equals(Config.decompiler().activeDecompiler.value())) {
				decompilerButton.setSelected(true);
			}

			decompilerButton.addActionListener(event -> {
				this.gui.getController().setDecompiler(decompiler.service);

				Config.decompiler().activeDecompiler.setValue(decompiler, true);
			});
			this.add(decompilerButton);
		}

		this.addSeparator();
		this.add(this.decompilerSettingsItem);

		this.decompilerSettingsItem.addActionListener(e -> DecompilerSettingsDialog.show(this.gui));
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate(TRANSLATION_KEY));
		this.decompilerSettingsItem.retranslate();
	}

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return TRANSLATION_KEY;
	}

	private static final class DecompilerItem extends JRadioButtonMenuItem implements SearchableElement {
		DecompilerItem(String name) {
			super(name);
		}

		@Override
		public Stream<String> streamSearchAliases() {
			return Stream.of(this.getText());
		}

		@Override
		public String getSearchName() {
			return this.getText();
		}

		@Override
		public void onSearchClicked() {
			this.doClick();
		}
	}
}
