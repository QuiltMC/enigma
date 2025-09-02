package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.EntryTreePrinter;
import org.quiltmc.enigma.util.I18n;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;

public class DevMenu extends AbstractEnigmaMenu {
	private final JCheckBoxMenuItem showMappingSourcePluginItem = new JCheckBoxMenuItem();
	private final JCheckBoxMenuItem debugTokenHighlightsItem = new JCheckBoxMenuItem();
	private final JCheckBoxMenuItem logClientPacketsItem = new JCheckBoxMenuItem();
	private final JMenuItem printMappingTreeItem = new JMenuItem();

	public DevMenu(Gui gui) {
		super(gui);

		this.add(this.showMappingSourcePluginItem);
		this.add(this.debugTokenHighlightsItem);
		this.add(this.logClientPacketsItem);
		this.add(this.printMappingTreeItem);

		this.showMappingSourcePluginItem.addActionListener(e -> this.onShowMappingSourcePluginClicked());
		this.debugTokenHighlightsItem.addActionListener(e -> this.onDebugTokenHighlightsClicked());
		this.logClientPacketsItem.addActionListener(e -> this.onLogClientPacketsClicked());
		this.printMappingTreeItem.addActionListener(e -> this.onPrintMappingTreeClicked());
	}

	@Override
	public void retranslate() {
		this.setText("Dev");

		this.showMappingSourcePluginItem.setText(I18n.translate("dev.menu.show_mapping_source_plugin"));
		this.debugTokenHighlightsItem.setText(I18n.translate("dev.menu.debug_token_highlights"));
		this.logClientPacketsItem.setText(I18n.translate("dev.menu.log_client_packets"));
		this.printMappingTreeItem.setText(I18n.translate("dev.menu.print_mapping_tree"));
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		this.printMappingTreeItem.setEnabled(jarOpen);

		this.showMappingSourcePluginItem.setState(Config.main().development.showMappingSourcePlugin.value());
		this.debugTokenHighlightsItem.setState(Config.main().development.debugTokenHighlights.value());
		this.logClientPacketsItem.setState(Config.main().development.logClientPackets.value());
	}

	private void showSavableTextAreaDialog(String title, String text, @Nullable String fileName) {
		var frame = new JFrame(title);
		var pane = frame.getContentPane();
		pane.setLayout(new BorderLayout());

		var textArea = new JTextArea(text);
		textArea.setFont(ScaleUtil.getFont(Font.MONOSPACED, Font.PLAIN, 12));
		pane.add(new JScrollPane(textArea), BorderLayout.CENTER);

		var buttonPane = new JPanel();

		var saveButton = new JButton(I18n.translate("prompt.save"));
		saveButton.addActionListener(e -> {
			var chooser = new JFileChooser();
			if (fileName != null) {
				chooser.setSelectedFile(new File(fileName));
			}

			if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
				try {
					Files.writeString(chooser.getSelectedFile().toPath(), text);
				} catch (IOException ex) {
					Logger.error(ex, "Failed to save the file");
				}
			}
		});
		buttonPane.add(saveButton);

		var closeButton = new JButton(I18n.translate("prompt.ok"));
		closeButton.addActionListener(e -> frame.dispose());
		buttonPane.add(closeButton);

		pane.add(buttonPane, BorderLayout.SOUTH);

		frame.setSize(ScaleUtil.getDimension(1200, 400));
		frame.setLocationRelativeTo(this.gui.getFrame());
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}

	private void onShowMappingSourcePluginClicked() {
		var value = this.showMappingSourcePluginItem.getState();
		Config.main().development.showMappingSourcePlugin.setValue(value, true);
	}

	private void onDebugTokenHighlightsClicked() {
		var value = this.debugTokenHighlightsItem.getState();
		Config.main().development.debugTokenHighlights.setValue(value, true);
	}

	private void onLogClientPacketsClicked() {
		var value = this.logClientPacketsItem.getState();
		Config.main().development.logClientPackets.setValue(value, true);
	}

	private void onPrintMappingTreeClicked() {
		var mappings = this.gui.getController().getProject().getRemapper().getMappings();

		var text = new StringWriter();
		EntryTreePrinter.print(new PrintWriter(text), mappings);

		this.showSavableTextAreaDialog(I18n.translate("dev.mapping_tree"), text.toString(), "mapping_tree.txt");
	}
}
