package cuchaz.enigma.utils.validation;

public record ParameterizedMessage(Message.Type type, Message message, Object[] params) {
	public ParameterizedMessage(Message message, Object... params) {
		this(message.getType(), message, params);
	}

	public static ParameterizedMessage openedProject(String jar, String mappings) {
		return new ParameterizedMessage(Message.OPENED_PROJECT, jar.substring(jar.lastIndexOf("/")), mappings.substring(jar.lastIndexOf("/")));
	}

	public String getText() {
		return this.message.format(this.params);
	}

	public String getLongText() {
		return this.message.formatDetails(this.params);
	}
}
