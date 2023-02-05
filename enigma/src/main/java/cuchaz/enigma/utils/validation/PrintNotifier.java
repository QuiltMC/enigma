package cuchaz.enigma.utils.validation;

import java.io.PrintStream;
import java.util.Arrays;

public class PrintNotifier implements ValidationContext.Notifier {
	public static final PrintNotifier INSTANCE = new PrintNotifier();

	@Override
	public void notify(ParameterizedMessage message) {
		formatMessage(System.out, message);
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
}
