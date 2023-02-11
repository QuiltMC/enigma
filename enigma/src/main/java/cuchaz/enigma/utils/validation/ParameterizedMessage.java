package cuchaz.enigma.utils.validation;

public record ParameterizedMessage(Message.Type type, Message message, Object[] params) {
	public ParameterizedMessage(Message message, Object... params) {
		this(message.getType(), message, params);
	}

	public String getText() {
		return message.format(params);
	}

	public String getLongText() {
		return this.message.formatDetails(this.params);
	}
}
