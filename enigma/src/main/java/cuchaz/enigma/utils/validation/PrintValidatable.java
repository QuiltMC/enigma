package cuchaz.enigma.utils.validation;

import org.tinylog.Level;
import org.tinylog.Logger;

import java.io.PrintStream;
import java.util.Arrays;

public class PrintValidatable implements Validatable {
	public static final PrintValidatable INSTANCE = new PrintValidatable();

	@Override
	public void addMessage(ParameterizedMessage message) {
		formatMessage(Level.INFO, message);
	}

	public static void formatMessage(Level level, ParameterizedMessage message) {
		String text = message.getText();
		String longText = message.getLongText();
		String type = switch (message.message.type) {
			case INFO -> "info";
			case WARNING -> "warning";
			case ERROR -> "error";
		};

		log(level, "{}: {}\n", type, text);

		if (!longText.isEmpty()) {
			Arrays.stream(longText.split("\n")).forEach(s -> log(level, "  {}\n", s));
		}
	}

	@Override
	public void clearMessages() {
		// no-op
	}

	private static void log(Level level, String message, Object... args) {
		switch (level) {
			case TRACE -> Logger.trace(message, args);
			case DEBUG -> Logger.debug(message, args);
			case INFO -> Logger.info(message, args);
			case WARN -> Logger.warn(message, args);
			case ERROR -> Logger.error(message, args);
		}
	}
}
