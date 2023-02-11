package cuchaz.enigma.utils.validation;

import org.tinylog.Logger;

import java.util.Arrays;

public class PrintNotifier implements ValidationContext.Notifier {
	public static final PrintNotifier INSTANCE = new PrintNotifier();

	@Override
	public void notify(ParameterizedMessage message) {
		formatMessage(message);
	}

	public static void formatMessage(ParameterizedMessage message) {
		String text = message.getText();
		String longText = message.getLongText();

		log(message.message().getType(), text);

		if (!longText.isEmpty()) {
			Arrays.stream(longText.split("\n")).forEach(s -> log(message.message().getType(), s));
		}
	}

	private static void log(Message.Type level, String message, Object... args) {
		switch (level) {
			case WARNING -> Logger.warn(message, args);
			case ERROR -> Logger.error(message, args);
			default -> Logger.info(message, args);
		}
	}
}
