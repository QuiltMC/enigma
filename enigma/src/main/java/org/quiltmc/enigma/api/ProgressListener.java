package org.quiltmc.enigma.api;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to help user interface implementations add progress indicators.
 * Allows syncing multiple progress indicators together.
 */
public abstract class ProgressListener {
	/**
	 * To be removed in 3.0. Use {@link #createEmpty()} instead.
	 */
	@Deprecated(forRemoval = true)
	public static ProgressListener none() {
		return new ProgressListener() {
		};
	}

	/**
	 * @return a basic progress listener
	 */
	public static ProgressListener createEmpty() {
		return new ProgressListener() {
		};
	}

	protected int totalWork;
	protected int workDone;
	protected String title;
	protected String currentMessage;

	private final List<ProgressListener> syncedListeners;

	protected ProgressListener() {
		this.syncedListeners = new ArrayList<>();
	}

	/**
	 * Runs initial setup on this progress listener.
	 * @apiNote all implementors should call {@code super.init} at the beginning of their override
	 * @param totalWork the total amount of steps that the listener will need to take
	 * @param title the title — should indicate the end goal of this progress indicator
	 */
	public void init(int totalWork, String title) {
		this.totalWork = totalWork;
		this.title = title;
		for (ProgressListener listener : this.syncedListeners) {
			listener.init(totalWork, title);
		}
	}

	/**
	 * Updates the progress listener and its synced listeners as the work is completed.
	 * @apiNote all implementors should call {@code super.step} at the beginning of their override
	 * @param workDone the amount of work done so far
	 * @param message the message to show the user — should indicate what the program is currently doing
	 */
	public void step(int workDone, String message) {
		this.workDone = workDone;
		this.currentMessage = message;
		for (ProgressListener listener : this.syncedListeners) {
			listener.step(workDone, message);
		}
	}

	/**
	 * Syncs this listener with the provided one. All steps performed by the provided listener will be propagated to this one.
	 * @param listener the listener to sync with
	 */
	public void sync(ProgressListener listener) {
		this.init(listener.totalWork, listener.title);
		this.step(listener.workDone, listener.currentMessage);
		listener.syncedListeners.add(this);
	}
}
