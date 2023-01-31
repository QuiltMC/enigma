package cuchaz.enigma.utils.validation;

import java.util.Arrays;
import java.util.Objects;

public final class ParameterizedMessage {
	public final Message message;
	private final Object[] params;

	public ParameterizedMessage(Message message, Object[] params) {
		this.message = message;
		this.params = params;
	}

	public String getText() {
		return this.message.format(this.params);
	}

	public String getLongText() {
		return this.message.formatDetails(this.params);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ParameterizedMessage that)) return false;
		return Objects.equals(this.message, that.message) &&
				Arrays.equals(this.params, that.params);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(this.message);
		result = 31 * result + Arrays.hashCode(this.params);
		return result;
	}
}
