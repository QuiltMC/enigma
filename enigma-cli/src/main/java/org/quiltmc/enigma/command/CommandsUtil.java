package org.quiltmc.enigma.command;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsReader;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsWriter;

import java.nio.file.Path;

public class CommandsUtil {
	public static ReadWriteService getReadWriteService(Enigma enigma, Path file) {
		var service = enigma.getReadWriteService(file);
		if (service.isEmpty()) {
			throw new UnsupportedOperationException("No reader/writer found for file \"" + file  + "\"");
		}

		return service.get();
	}

	public static MappingsReader getReader(Enigma enigma, Path file) {
		ReadWriteService service = getReadWriteService(enigma, file);

		if (!service.supportsReading()) {
			throw new UnsupportedOperationException("Read/write service for file \"" + file + "\" does not support reading!");
		}

		return service;
	}

	public static MappingsWriter getWriter(Enigma enigma, Path file) {
		ReadWriteService service = getReadWriteService(enigma, file);

		if (!service.supportsWriting()) {
			throw new UnsupportedOperationException("Read/write service for file \"" + file + "\" does not support writing!");
		}

		return service;
	}
}
