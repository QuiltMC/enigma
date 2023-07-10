package cuchaz.enigma.utils.validation;

import cuchaz.enigma.utils.validation.Message.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A context for user input validation. Handles collecting error messages and displaying them using its {@link Notifier}.
 * Useful for collecting errors with a certain input and checking over them to see if the action should be allowed to proceed.
 */
public class ValidationContext {
	private final List<ParameterizedMessage> messages = new ArrayList<>();
	private Notifier notifier;

	/**
	 * Creates a new validation context with no messages.
	 * @param notifier the notifier to pass new messages into, defaults to {@link PrintNotifier#INSTANCE} if null
	 */
	public ValidationContext(Notifier notifier) {
		this.notifier = Objects.requireNonNullElse(notifier, PrintNotifier.INSTANCE);
	}

	/**
	 * Sets the current notifier.
	 * @param notifier the new notifier
	 */
	public void setNotifier(Notifier notifier) {
		this.notifier = notifier;
	}

	/**
	 * Raises a message and sends it into the current {@link Notifier} to inform the user.
	 *
	 * @param message the message to raise
	 * @param args the arguments used when formatting the message text
	 */
	public void raise(Message message, Object... args) {
		ParameterizedMessage pm = new ParameterizedMessage(message, args);
		if (!this.messages.contains(pm)) {
			this.messages.add(pm);
			this.notifier.notify(pm);
		}
	}

	/**
	 * Returns whether the validation context currently has no messages that
	 * block executing actions, such as errors and unconfirmed warnings.
	 * <p>If warnings are present, asks the user to confirm their intent for
	 * each one before proceeding.</p>
	 *
	 * @return whether the program can proceed executing and the UI is in a
	 * valid state
	 */
	public boolean canProceed() {
		List<ParameterizedMessage> messagesCopy = new ArrayList<>(this.messages);

		for (ParameterizedMessage m : messagesCopy) {
			if (m.getType() == Type.WARNING) {
				this.messages.remove(m);
				if (!this.notifier.verifyWarning(m)) {
					return false;
				}
			}
		}

		return this.messages.stream().noneMatch(m -> m.message().getType() == Type.ERROR);
	}

	/**
	 * Returns an unmodifiable list of all currently stored messages.
	 * @return the messages
	 */
	public List<ParameterizedMessage> getMessages() {
		return Collections.unmodifiableList(this.messages);
	}

	/**
	 * Clears all currently pending messages. This should be called whenever the
	 * interface starts getting validated, to get rid of old messages.
	 */
	public void reset() {
		this.messages.clear();
	}

	/**
	 * Represents a notifier, a service which takes messages and informs the user of them.
	 */
	public interface Notifier {
		/**
		 * Takes a generic message and displays it to the user.
		 * @param message the message to display
		 */
		default void notify(Message message) {
			this.notify(new ParameterizedMessage(message));
		}

		/**
		 * Takes a generic message and displays it to the user.
		 * @param message the message to display
		 */
		void notify(ParameterizedMessage message);

		/**
		 * Asks the user to confirm a warning. Should show the message to the user before asking for their input.
		 * @param message the warning message
		 * @return the user's decision on what to do - true to proceed, false to cancel
		 */
		boolean verifyWarning(ParameterizedMessage message);
	}
}
