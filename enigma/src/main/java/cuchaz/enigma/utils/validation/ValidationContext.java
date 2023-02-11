package cuchaz.enigma.utils.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import cuchaz.enigma.utils.validation.Message.Type;

/**
 * A context for user input validation. Handles collecting error messages and
 * displaying the errors. UIs using validation
 * often have two stages of applying changes: validating all the input fields,
 * then checking if there's any errors or unconfirmed warnings, and if not,
 * then actually applying the changes. This allows for easily collecting
 * multiple errors and displaying them to the user at the same time.
 */
public class ValidationContext {
	public ValidationContext(Notifier notifier) {
		this.notifier = Objects.requireNonNullElse(notifier, PrintNotifier.INSTANCE);
	}

	private final List<ParameterizedMessage> messages = new ArrayList<>();
	private Notifier notifier;

	public void setNotifier(Notifier notifier) {
		this.notifier = notifier;
	}

	/**
	 * Raises a message. If there's a currently active element, also notifies
	 * that element about the message.
	 *
	 * @param message the message to raise
	 * @param args    the arguments used when formatting the message text
	 */
	public void raise(Message message, Object... args) {
		ParameterizedMessage pm = new ParameterizedMessage(message, args);
		if (!this.messages.contains(pm)) {
			this.messages.add(pm);
		}

		this.notifier.notify(pm);
	}

	/**
	 * Returns whether the validation context currently has no messages that
	 * block executing actions, such as errors and unconfirmed warnings.
	 *
	 * @return whether the program can proceed executing and the UI is in a
	 * valid state
	 */
	public boolean canProceed() {
		// TODO on warnings, wait until user confirms
		return this.messages.stream().noneMatch(m -> m.message().getType() == Type.ERROR);
	}

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

	public interface Notifier {
		void notify(ParameterizedMessage message);
	}
}
