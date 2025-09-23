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

package org.quiltmc.enigma.gui.dialog;

import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.gui.util.GuiUtil.FocusCondition;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.syntaxpain.QuickFindToolBar;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

import static org.quiltmc.enigma.gui.util.GuiUtil.putKeyBindAction;

/**
 * Extension of {@link QuickFindToolBar} to allow using keybindings, and improve UI.
 */
public class EnigmaQuickFindToolBar extends QuickFindToolBar {

	protected JCheckBox persistentCheckBox;
	protected JButton closeButton;

	public EnigmaQuickFindToolBar() {
		super();
		// keybinding support
		this.reloadKeyBinds();

		// configure parent components
		this.ignoreCaseCheckBox.setMnemonic(KeyBinds.QUICK_FIND_DIALOG_IGNORE_CASE.getKeyCode());
		this.regexCheckBox.setMnemonic(KeyBinds.QUICK_FIND_DIALOG_REGEX.getKeyCode());
		this.wrapCheckBox.setMnemonic(KeyBinds.QUICK_FIND_DIALOG_WRAP.getKeyCode());

		// make buttons icon-only
		this.nextButton.setText("");
		this.nextButton.setIcon(GuiUtil.getDownChevron());
		this.prevButton.setText("");
		this.prevButton.setIcon(GuiUtil.getUpChevron());

		// add custom components
		// push the rest of the components to the right
		this.add(Box.createHorizontalGlue());

		this.addSeparator();

		this.persistentCheckBox = new JCheckBox();
		this.persistentCheckBox.setFocusable(false);
		this.persistentCheckBox.setOpaque(false);
		this.persistentCheckBox.setVerticalTextPosition(SwingConstants.BOTTOM);
		this.persistentCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);
		this.persistentCheckBox.addActionListener(this);
		this.persistentCheckBox.addItemListener(e -> {
			final boolean selected = this.persistentCheckBox.isSelected();
			if (selected != Config.main().persistentEditorQuickFind.value()) {
				Config.main().persistentEditorQuickFind.setValue(selected);
			}
			// request focus so when it's lost this may be dismissed
			this.requestFocus();
		});
		this.persistentCheckBox.setSelected(Config.main().persistentEditorQuickFind.value());
		Config.main().persistentEditorQuickFind.registerCallback(callback -> {
			final Boolean configured = callback.value();
			if (this.persistentCheckBox.isSelected() != configured) {
				this.persistentCheckBox.setSelected(configured);
			}
		});
		this.add(this.persistentCheckBox);

		this.addSeparator();

		this.closeButton = new JButton();
		this.closeButton.setIcon(GuiUtil.getCloseIcon());
		this.closeButton.setFocusable(false);
		this.closeButton.setOpaque(false);
		this.closeButton.addActionListener(e -> this.dismiss());
		this.add(this.closeButton);

		this.translate();
	}

	@Override
	protected boolean dismissOnFocusLost() {
		return !this.persistentCheckBox.isSelected();
	}

	public void translate() {
		this.notFound = I18n.translate("editor.quick_find.not_found");

		this.ignoreCaseCheckBox.setText(I18n.translate("editor.quick_find.ignore_case"));
		this.regexCheckBox.setText(I18n.translate("editor.quick_find.use_regex"));
		this.wrapCheckBox.setText(I18n.translate("editor.quick_find.wrap"));

		this.persistentCheckBox.setText(I18n.translate("editor.quick_find.persistent"));
	}

	public void reloadKeyBinds() {
		putKeyBindAction(KeyBinds.QUICK_FIND_DIALOG_PREVIOUS, this.searchField, e -> this.prevButton.doClick());
		putKeyBindAction(KeyBinds.QUICK_FIND_DIALOG_NEXT, this.searchField, e -> this.nextButton.doClick());
		putKeyBindAction(
				KeyBinds.QUICK_FIND_DIALOG_CLOSE, this, FocusCondition.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
				e -> this.setVisible(false)
		);
	}
}
