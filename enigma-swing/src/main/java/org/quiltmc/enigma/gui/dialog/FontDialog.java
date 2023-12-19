package org.quiltmc.enigma.gui.dialog;

import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.config.theme.Theme;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.I18n;
import org.drjekyll.fontchooser.FontChooser;

import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;

public class FontDialog extends JDialog {
	private static final List<TrackedValue<Theme.Fonts.SerializableFont>> FONTS = List.of(
			Config.currentFonts().defaultNormal,
			Config.currentFonts().defaultBold,
			Config.currentFonts().small,
			Config.currentFonts().editor
	);

	private static final List<String> CATEGORY_TEXTS = List.of(
			"fonts.cat.default_normal",
			"fonts.cat.default_bold",
			"fonts.cat.small",
			"fonts.cat.editor"
	);

	private final JList<String> entries = new JList<>(CATEGORY_TEXTS.stream().map(I18n::translate).toArray(String[]::new));
	private final FontChooser chooser = new FontChooser(Font.decode(Font.DIALOG));
	private final Theme.Fonts.SerializableFont[] fontValues = FONTS.stream().map(TrackedValue::value).toArray(Theme.Fonts.SerializableFont[]::new);

	public FontDialog(Frame owner) {
		super(owner, "Fonts", true);

		this.entries.setPreferredSize(ScaleUtil.getDimension(100, 0));

		this.entries.addListSelectionListener(e -> this.categoryChanged());
		this.chooser.addChangeListener(e -> this.selectedFontChanged());
		JButton okButton = new JButton(I18n.translate("prompt.ok"));
		okButton.addActionListener(e -> this.apply());
		JButton cancelButton = new JButton(I18n.translate("prompt.cancel"));
		cancelButton.addActionListener(e -> this.cancel());

		Container contentPane = this.getContentPane();
		contentPane.setLayout(new GridBagLayout());

		GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create()
				.insets(2);

		contentPane.add(this.entries, cb.pos(0, 0).weight(0.1, 1.0).fill(GridBagConstraints.BOTH).build());
		contentPane.add(this.chooser, cb.pos(1, 0).weight(1.0, 1.0).fill(GridBagConstraints.BOTH).size(2, 1).build());
		contentPane.add(okButton, cb.pos(1, 1).anchor(GridBagConstraints.EAST).weight(1.0, 0.0).build());
		contentPane.add(cancelButton, cb.pos(2, 1).anchor(GridBagConstraints.EAST).weight(0.0, 0.0).build());

		this.setSize(ScaleUtil.getDimension(640, 360));
		this.setLocationRelativeTo(owner);
	}

	private void categoryChanged() {
		int selectedIndex = this.entries.getSelectedIndex();
		if (selectedIndex != -1) {
			this.chooser.setSelectedFont(this.fontValues[selectedIndex]);
		}
	}

	private void selectedFontChanged() {
		int selectedIndex = this.entries.getSelectedIndex();
		if (selectedIndex != -1) {
			this.fontValues[selectedIndex] = new Theme.Fonts.SerializableFont(this.chooser.getSelectedFont());
		}
	}

	private void apply() {
		for (int i = 0; i < FONTS.size(); i++) {
			FONTS.get(i).setValue(this.fontValues[i], true);
		}

		ChangeDialog.show(this);
		this.dispose();
	}

	private void cancel() {
		this.dispose();
	}

	public static void display(Frame parent) {
		FontDialog d = new FontDialog(parent);
		d.setVisible(true);
	}
}
