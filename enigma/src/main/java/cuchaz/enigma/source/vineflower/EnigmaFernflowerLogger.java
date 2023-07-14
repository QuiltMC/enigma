package cuchaz.enigma.source.vineflower;

import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.tinylog.Logger;

public class EnigmaFernflowerLogger extends IFernflowerLogger {
	private static final String MESSAGE_TEMPLATE = "[Vineflower] {}";

	@Override
	public void writeMessage(String message, Severity severity) {
		if (accepts(severity)) {
			switch (severity) {
				case TRACE -> Logger.debug(MESSAGE_TEMPLATE, message);
				case INFO -> Logger.info(MESSAGE_TEMPLATE, message);
				case WARN -> Logger.warn(MESSAGE_TEMPLATE, message);
				case ERROR -> Logger.error(MESSAGE_TEMPLATE, message);
			}
		}
	}

	@Override
	public void writeMessage(String message, Severity severity, Throwable t) {
		if (accepts(severity)) {
			switch (severity) {
				case TRACE -> Logger.debug(t, MESSAGE_TEMPLATE, message);
				case INFO -> Logger.info(t, MESSAGE_TEMPLATE, message);
				case WARN -> Logger.warn(t, MESSAGE_TEMPLATE, message);
				case ERROR -> Logger.error(t, MESSAGE_TEMPLATE, message);
			}
		}
	}
}
