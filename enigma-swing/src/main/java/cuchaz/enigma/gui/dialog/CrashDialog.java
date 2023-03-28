package cuchaz.enigma.gui.dialog;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.gui.util.ScaleUtil;
import org.tinylog.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Container;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

public class CrashDialog {
	private static CrashDialog instance = null;

	private final JFrame frame;
	private final JTextArea text;

	private CrashDialog(JFrame parent) {
		// init frame
		this.frame = new JFrame(String.format(I18n.translate("crash.title"), Enigma.NAME));
		final Container pane = this.frame.getContentPane();
		pane.setLayout(new BorderLayout());

		JLabel label = new JLabel(String.format(I18n.translate("crash.summary"), Enigma.NAME));
		label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pane.add(label, BorderLayout.NORTH);

		// report panel
		this.text = new JTextArea();
		this.text.setTabSize(2);
		pane.add(new JScrollPane(this.text), BorderLayout.CENTER);

		// buttons panel
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS));
		JButton exportButton = new JButton(I18n.translate("crash.export"));
		exportButton.addActionListener(event -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setSelectedFile(new File("enigma_crash.log"));
			if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
				try {
					File file = chooser.getSelectedFile();
					FileWriter writer = new FileWriter(file);
					writer.write(instance.text.getText());
					writer.close();
				} catch (IOException ex) {
					Logger.error(ex, "Failed to export crash report");
				}
			}
		});
		buttonsPanel.add(exportButton);
		buttonsPanel.add(Box.createHorizontalGlue());
		buttonsPanel.add(GuiUtil.unboldLabel(new JLabel(I18n.translate("crash.exit.warning"))));
		JButton ignoreButton = new JButton(I18n.translate("crash.ignore"));
		ignoreButton.addActionListener(event -> {
			// close (hide) the dialog
			this.frame.setVisible(false);
		});
		buttonsPanel.add(ignoreButton);
		JButton exitButton = new JButton(I18n.translate("crash.exit"));
		exitButton.addActionListener(event -> {
			// exit enigma
			System.exit(1);
		});
		buttonsPanel.add(exitButton);
		pane.add(buttonsPanel, BorderLayout.SOUTH);

		// show the frame
		this.frame.setSize(ScaleUtil.getDimension(600, 400));
		this.frame.setLocationRelativeTo(parent);
		this.frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	public static void init(JFrame parent) {
		instance = new CrashDialog(parent);
	}

	public static void show(Throwable ex) {
		// get the error report
		StringWriter buf = new StringWriter();
		ex.printStackTrace(new PrintWriter(buf));
		String report = buf.toString();

		// show it!
		instance.text.setText(report);
		instance.frame.doLayout();
		instance.frame.setVisible(true);
	}
}
