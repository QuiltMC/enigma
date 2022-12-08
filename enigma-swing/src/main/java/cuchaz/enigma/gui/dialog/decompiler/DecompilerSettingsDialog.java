package cuchaz.enigma.gui.dialog.decompiler;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.Decompiler;
import cuchaz.enigma.gui.util.GridBagConstraintsBuilder;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Locale;

public class DecompilerSettingsDialog {
    public static void show(Gui gui) {
        JDialog frame = new JDialog(gui.getFrame(), I18n.translate("menu.decompiler.settings"), true);
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        JPanel decompilersPanel = new JPanel(new GridBagLayout());
        decompilersPanel.setBorder(new EmptyBorder(ScaleUtil.scale(10), ScaleUtil.scale(20), ScaleUtil.scale(10), ScaleUtil.scale(20)));

        int row = 0;
        for (Decompiler decompiler : Decompiler.values()) {
            if (decompiler.settingsDialog == null) {
                continue;
            }

            JLabel label = new JLabel(decompiler.name);
            JButton button = new JButton(I18n.translate("menu.decompiler.settings." + decompiler.name.toLowerCase(Locale.ROOT)));
            button.addActionListener(e -> decompiler.settingsDialog.accept(gui, frame));

            GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(2);
            decompilersPanel.add(label, cb.pos(0, row).weightX(0.0).anchor(GridBagConstraints.LINE_END).fill(GridBagConstraints.NONE).build());
            decompilersPanel.add(button, cb.pos(1, row).weightX(1.0).anchor(GridBagConstraints.LINE_END).fill(GridBagConstraints.HORIZONTAL).build());

            row++;
        }

        pane.add(decompilersPanel, BorderLayout.CENTER);

        Container buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, ScaleUtil.scale(4), ScaleUtil.scale(4)));
        JButton okButton = new JButton(I18n.translate("prompt.ok"));
        okButton.addActionListener(event -> frame.dispose());
        buttonContainer.add(okButton);
        pane.add(buttonContainer, BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(gui.getFrame());
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }
}
