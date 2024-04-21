package org.quiltmc.enigma.command;

import com.google.common.io.MoreFiles;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsReader;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsWriter;

import javax.annotation.Nullable;
import java.nio.file.Path;

public class CommandsUtil {
	public static ReadWriteService getReadWriteService(Enigma enigma, Path file) {
		String extension = MoreFiles.getFileExtension(file);
		var service = enigma.getReadWriteService(extension);
		if (service.isEmpty()) {
			throw new UnsupportedOperationException("No reader/writer found for file type \"" + extension + "\"");
		}

		return service.get();
	}

	public static MappingsReader getReader(Enigma enigma, Path file) {
		String extension = MoreFiles.getFileExtension(file);
		ReadWriteService service = getReadWriteService(enigma, file);

		if (!service.supportsReading()) {
			throw new UnsupportedOperationException("Read/write service for file type \"" + extension + "\" does not support reading!");
		}

		return service;
	}

	public static MappingsWriter getWriter(Enigma enigma, Path file) {
		String extension = MoreFiles.getFileExtension(file);
		ReadWriteService service = getReadWriteService(enigma, file);

		if (!service.supportsWriting()) {
			throw new UnsupportedOperationException("Read/write service for file type \"" + extension + "\" does not support writing!");
		}

		return service;
	}
}
