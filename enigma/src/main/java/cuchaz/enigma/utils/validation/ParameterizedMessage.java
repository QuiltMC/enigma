package cuchaz.enigma.utils.validation;

public record ParameterizedMessage(Message message, Object... params) {
	public static ParameterizedMessage openedProject(String jar, String mappings) {
		return new ParameterizedMessage(Message.OPENED_PROJECT, jar.substring(jar.lastIndexOf("/")), mappings.substring(jar.lastIndexOf("/")));
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
