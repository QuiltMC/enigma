package cuchaz.enigma.translation.mapping.serde;

import java.io.File;
import java.nio.file.Path;

public class MappingParseException extends Exception {
	private final int line;
	private final String message;
	private final Path filePath;

	public MappingParseException(File file, int line, String message) {
		this(file.toPath(), line, message);
	}

	public MappingParseException(Path filePath, int line, String message) {
		this.line = line;
		this.message = message;
		this.filePath = filePath;
	}

	public MappingParseException(Path filePath, int line, Throwable cause) {
		super(cause);
		this.line = line;
		this.message = cause.toString();
		this.filePath = filePath;
	}

	@Override
	public String getMessage() {
		return "Line " + this.line + ": " + this.message + " in file " + this.filePath.toString();
	}
}
