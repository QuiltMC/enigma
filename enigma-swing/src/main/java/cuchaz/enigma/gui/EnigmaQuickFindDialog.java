/*
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cuchaz.enigma.gui;

import cuchaz.enigma.gui.config.keybind.KeyBinds;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.utils.I18n;
import org.quiltmc.syntaxpain.QuickFindDialog;

import javax.swing.text.JTextComponent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Extension of {@link QuickFindDialog} to allow using keybindings, and improve UI.
 */
public class EnigmaQuickFindDialog extends QuickFindDialog {
	public EnigmaQuickFindDialog(JTextComponent target) {
		super(target);
	}

	@Override
	protected void initComponents() {
		super.initComponents();

		// keybinding support
		this.searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (KeyBinds.QUICK_FIND_DIALOG_PREVIOUS.matches(e)) {
					EnigmaQuickFindDialog.this.prevButton.doClick();
				} else if (KeyBinds.QUICK_FIND_DIALOG_NEXT.matches(e)) {
					EnigmaQuickFindDialog.this.nextButton.doClick();
				}
			}
		});

		this.ignoreCaseCheckBox.setMnemonic(KeyBinds.QUICK_FIND_DIALOG_IGNORE_CASE.getKeyCode());
		this.regexCheckBox.setMnemonic(KeyBinds.QUICK_FIND_DIALOG_REGEX.getKeyCode());
		this.wrapCheckBox.setMnemonic(KeyBinds.QUICK_FIND_DIALOG_WRAP.getKeyCode());

		// make buttons icon-only
		this.nextButton.setText("");
		this.nextButton.setIcon(GuiUtil.getDownChevron());
		this.prevButton.setText("");
		this.prevButton.setIcon(GuiUtil.getUpChevron());

		// translations
		this.ignoreCaseCheckBox.setText(I18n.translate("editor.quick_find.ignore_case"));
		this.regexCheckBox.setText(I18n.translate("editor.quick_find.use_regex"));
		this.wrapCheckBox.setText(I18n.translate("editor.quick_find.wrap"));
	}
}
