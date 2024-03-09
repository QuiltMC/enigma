package org.quiltmc.enigma.gui.util;

import org.quiltmc.enigma.api.ProgressListener;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ProgressBar extends ProgressListener {
	protected final JProgressBar delegate;
	private final List<Consumer<String>> titleSetListeners;
	private final List<BiConsumer<String, Boolean>> messageUpdateListeners;

	public ProgressBar() {
		this.delegate = new JProgressBar();
		this.titleSetListeners = new ArrayList<>();
		this.messageUpdateListeners = new ArrayList<>();
	}

	public JProgressBar asJProgressBar() {
		return this.delegate;
	}

	public void addTitleSetListener(Consumer<String> listener) {
		this.titleSetListeners.add(listener);
	}

	public void addMessageUpdateListener(BiConsumer<String, Boolean> listener) {
		this.messageUpdateListeners.add(listener);
	}

	@Override
	public void init(int totalWork, String title) {
		super.init(totalWork, title);
		this.titleSetListeners.forEach(listener -> listener.accept(title));

		SwingUtilities.invokeLater(() -> {
			this.delegate.setMinimum(0);
			this.delegate.setMaximum(totalWork);
			this.delegate.setValue(0);
		});
	}

	@Override
	public void step(int workDone, String message) {
		super.step(workDone, message);
		this.messageUpdateListeners.forEach(listener -> listener.accept(message, workDone == totalWork));

		SwingUtilities.invokeLater(() -> {
			if (workDone != -1) {
				this.delegate.setValue(workDone);
				this.delegate.setIndeterminate(false);
			} else {
				this.delegate.setIndeterminate(true);
			}
		});
	}
}
