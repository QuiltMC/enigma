package cuchaz.enigma.utils.validation;

public record ParameterizedMessage(Message.Type type, Message message, Object[] params) {
	public String getText() {
		return this.message.format(this.params);
	}

	public String getLongText() {
		return this.message.formatDetails(this.params);
	}
}
