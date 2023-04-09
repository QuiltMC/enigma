package cuchaz.enigma.utils.validation;

import java.io.File;

/**
 * Represents a formatted message ready for display to the user.
 * @param message the message to display
 * @param params parameters to use when formatting the message text
 */
public record ParameterizedMessage(Message message, Object... params) {
	public static ParameterizedMessage openedProject(String jar, String mappings) {
		return new ParameterizedMessage(Message.OPENED_PROJECT, jar.substring(jar.lastIndexOf(File.separator)), mappings.substring(jar.lastIndexOf(File.separator)));
	}

	public Message.Type getType() {
		return this.message.getType();
	}

	public String getText() {
		return this.message.format(this.params);
	}

	public String getLongText() {
		return this.message.formatDetails(this.params);
	}
}
