package cuchaz.enigma.utils.validation;

import java.io.PrintStream;
import java.util.Arrays;

public class PrintValidatable implements Validatable, ValidationContext.Notifier {
	public static final PrintValidatable INSTANCE = new PrintValidatable();

	@Override
	public void addMessage(ParameterizedMessage message) {
		formatMessage(System.out, message);
	}

	@Override
	public void notify(ParameterizedMessage message) {
		this.addMessage(message);
	}

	public static void formatMessage(PrintStream w, ParameterizedMessage message) {
		String text = message.getText();
		String longText = message.getLongText();
		String type = switch (message.message().getType()) {
			case INFO -> "info";
			case WARNING -> "warning";
			case ERROR -> "error";
		};
		w.printf("%s: %s\n", type, text);

		if (!longText.isEmpty()) {
			Arrays.stream(longText.split("\n")).forEach(s -> w.printf("  %s\n", s));
		}
	}

	@Override
	public void clearMessages() {
	}
}
