package org.quiltmc.enigma.gui.dialog;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.util.GridBagConstraintsBuilder;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.gui.util.ProgressBar;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.I18n;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.concurrent.CompletableFuture;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class ProgressDialog extends ProgressBar implements AutoCloseable {
	private final JDialog dialog;

	public ProgressDialog(JFrame parent) {
		JLabel labelTitle = new JLabel();
		this.addTitleSetListener(labelTitle::setText);
		JLabel labelText = GuiUtil.unboldLabel(new JLabel());
		this.addMessageUpdateListener((message, done) -> labelText.setText(message));

		// init frame
		this.dialog = new JDialog(parent, String.format(I18n.translate("progress.operation"), Enigma.NAME));
		Container pane = this.dialog.getContentPane();
		pane.setLayout(new GridBagLayout());

		GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create()
				.insets(2)
				.anchor(GridBagConstraints.WEST)
				.fill(GridBagConstraints.BOTH)
				.weight(1.0, 0.0);

		pane.add(labelTitle, cb.pos(0, 0).build());
		pane.add(labelText, cb.pos(0, 1).build());
		pane.add(this.delegate, cb.pos(0, 2).weight(1.0, 1.0).build());

		// Set label text since otherwise the label height is 0, which makes the
		// window size get set incorrectly
		labelTitle.setText("Idle");
		labelText.setText("Idle");
		this.delegate.setPreferredSize(ScaleUtil.getDimension(0, 20));

		// show the frame
		this.dialog.setResizable(false);
		this.reposition();
		this.dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	}

	// This tries to set the window size to the smallest it can be vertically,
	// and 400 units in width.
	// Gets called twice, including after the window opens to try to fix the
	// window size (more specifically, the progress bar size) being smaller when
	// the dialog opens for the very first time compared to afterwards. (#366)
	private void reposition() {
		this.dialog.pack();
		Dimension size = this.dialog.getSize();
		this.dialog.setMinimumSize(size);
		size.width = ScaleUtil.scale(400);
		this.dialog.setSize(size);

		this.dialog.setLocationRelativeTo(this.dialog.getParent());
	}

	public static CompletableFuture<Void> runOffThread(final Gui gui, final ProgressRunnable runnable) {
		return CompletableFuture.supplyAsync(() -> {
			ProgressDialog progress = new ProgressDialog(gui.getFrame());

			if (gui.showsProgressBars()) {
				// Somehow opening the dialog, disposing it, then reopening it
				// and then repositioning it fixes the size issues detailed above
				// most of the time.
				// Using setVisible(false) instead of dispose() does not work as
				// well.
				// Don't ask me why.
				progress.dialog.setVisible(true);
				progress.dialog.dispose();
				progress.dialog.setVisible(true);
				progress.reposition();
			}

			return progress;
		}, SwingUtilities::invokeLater).thenAcceptAsync(progress -> {
			try (progress) {
				runnable.run(progress);
			} catch (Exception e) {
				CrashDialog.show(e);
			}
		});
	}

	@Override
	public void close() {
		SwingUtilities.invokeLater(this.dialog::dispose);
	}

	public interface ProgressRunnable {
		void run(ProgressListener listener) throws Exception;
	}
}
