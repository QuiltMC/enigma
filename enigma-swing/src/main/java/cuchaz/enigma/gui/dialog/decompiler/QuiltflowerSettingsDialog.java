package cuchaz.enigma.gui.dialog.decompiler;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class QuiltflowerSettingsDialog extends JDialog {
    private static final List<String> IGNORED_PREFERENCES = List.of(
            IFernflowerPreferences.BANNER,
            IFernflowerPreferences.BYTECODE_SOURCE_MAPPING,
            IFernflowerPreferences.MAX_PROCESSING_METHOD,
            IFernflowerPreferences.LOG_LEVEL,
            IFernflowerPreferences.INDENT_STRING,
            IFernflowerPreferences.THREADS,
            IFernflowerPreferences.USER_RENAMER_CLASS,
            IFernflowerPreferences.NEW_LINE_SEPARATOR,
            IFernflowerPreferences.ERROR_MESSAGE
    );

    public QuiltflowerSettingsDialog(Gui gui, JDialog parent) {
        super(parent, I18n.translate("menu.decompiler.settings.quiltflower"), true);
        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());

        JPanel preferencesPanel = new JPanel();
        preferencesPanel.setLayout(new BoxLayout(preferencesPanel, BoxLayout.Y_AXIS));

        JScrollPane preferencesScrollPanel = new JScrollPane(preferencesPanel);
        preferencesScrollPanel.setPreferredSize(new Dimension(ScaleUtil.scale(640), ScaleUtil.scale(480)));
        preferencesScrollPanel.setBorder(new EmptyBorder(ScaleUtil.scale(10), ScaleUtil.scale(20), ScaleUtil.scale(10), ScaleUtil.scale(20)));

        for (Preference preference : getPreferences()) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel label = new JLabel(preference.name());
            label.setToolTipText(preference.description());

            JComponent input = switch (preference.type()) {
                case STRING -> {
                    JTextField t = new JTextField((String) preference.value());
                    t.setColumns(20);
                    yield t;
                }
                case INTEGER -> {
                    JSpinner spinner = new JSpinner();
                    spinner.setModel(new SpinnerNumberModel(Integer.parseInt((String) preference.value()), 0, Integer.MAX_VALUE, 1));
                    yield spinner;
                }
                case BOOLEAN -> {
                    JCheckBox c = new JCheckBox();
                    c.setSelected(preference.value().equals("1"));
                    yield c;
                }
            };
            input.setMaximumSize(new Dimension(ScaleUtil.scale(100), ScaleUtil.scale(20)));


            if (input instanceof JCheckBox) {
                panel.add(input);
                panel.add(label);
            } else {
                panel.add(label);
                panel.add(input);
            }

            preferencesPanel.add(panel);
        }

        pane.add(preferencesScrollPanel, BorderLayout.CENTER);


        Container buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, ScaleUtil.scale(4), ScaleUtil.scale(4)));
        JButton okButton = new JButton(I18n.translate("prompt.ok"));
        okButton.addActionListener(event -> dispose());
        buttonContainer.add(okButton);
        pane.add(buttonContainer, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(gui.getFrame());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    @SuppressWarnings("unchecked")
    private static List<Preference> getPreferences() {
        try {
            Class<?> clazz =  IFernflowerPreferences.class;

            Map<String, Object> defaults = (Map<String, Object>) clazz.getField("DEFAULTS").get(null);
            List<Preference> preferences = new ArrayList<>();
            for (Field field : clazz.getFields()) {
                if (field.getType() != String.class || !field.isAnnotationPresent(IFernflowerPreferences.Name.class)) {
                    continue;
                }

                String key = (String) field.get(null);
                if (IGNORED_PREFERENCES.contains(key)) {
                    continue;
                }

                String name = field.getAnnotation(IFernflowerPreferences.Name.class).value();
                String description = field.getAnnotation(IFernflowerPreferences.Description.class).value();

                PreferenceType type = inferType(key, defaults);
                Object defaultValue = defaults.get(key);
                if (type != null) {
                    preferences.add(new Preference(name, description, key, type, defaultValue));
                }
            }

            return preferences;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static PreferenceType inferType(String key, Map<String, Object> defaults) {
        Object defaultValue = defaults.get(key);
        if (defaultValue == null) {
            return null;
        }

        if (defaultValue == "0" || defaultValue == "1") {
            return PreferenceType.BOOLEAN;
        }

        try {
            Integer.parseInt(defaultValue.toString());
            return PreferenceType.INTEGER;
        } catch (Exception ignored) {
        }

        return PreferenceType.STRING;
    }

    private record Preference(String name, String description, String key, PreferenceType type, Object value) {
    }

    private enum PreferenceType {
        STRING,
        INTEGER,
        BOOLEAN
    }
}
