package cuchaz.enigma.command;

import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.serde.MappingsWriter;
import cuchaz.enigma.translation.mapping.serde.enigma.EnigmaMappingsWriter;
import cuchaz.enigma.translation.mapping.serde.tinyv2.TinyV2Writer;

public final class MappingCommandsUtil {
	private MappingCommandsUtil() {
	}

	// todo: ew
	public static MappingsWriter getWriter(String type) {
		if (type.equals("enigma")) {
			return EnigmaMappingsWriter.DIRECTORY;
		}

		if (type.startsWith("tinyv2:") || type.startsWith("tiny_v2:")) {
			String[] split = type.split(":");

			if (split.length != 3) {
				throw new IllegalArgumentException("specify column names as 'tinyv2:from_namespace:to_namespace'");
			}

			return new TinyV2Writer(split[1], split[2]);
		}

		MappingFormat format;
		try {
			format = MappingFormat.valueOf(type);
		} catch (IllegalArgumentException ignored) {
			format = MappingFormat.valueOf(type.toLowerCase());
		}

		return format.getWriter();
	}
}
