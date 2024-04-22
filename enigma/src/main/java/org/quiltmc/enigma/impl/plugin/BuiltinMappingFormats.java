package org.quiltmc.enigma.impl.plugin;

import org.quiltmc.enigma.api.EnigmaPluginContext;
import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.api.translation.mapping.serde.FileType;
import org.quiltmc.enigma.api.translation.mapping.serde.enigma.EnigmaMappingsReader;
import org.quiltmc.enigma.api.translation.mapping.serde.enigma.EnigmaMappingsWriter;
import org.quiltmc.enigma.api.translation.mapping.serde.proguard.ProguardMappingsReader;
import org.quiltmc.enigma.api.translation.mapping.serde.srg.SrgMappingsWriter;
import org.quiltmc.enigma.api.translation.mapping.serde.tinyv2.TinyV2Reader;
import org.quiltmc.enigma.api.translation.mapping.serde.tinyv2.TinyV2Writer;

public class BuiltinMappingFormats {
	public static void register(EnigmaPluginContext ctx) {
		FileType.File enigmaMapping = new FileType.File("mapping", "mappings");

		ctx.registerService(ReadWriteService.TYPE,
				ctx1 -> ReadWriteService.create(EnigmaMappingsReader.FILE, EnigmaMappingsWriter.FILE, enigmaMapping, "enigma:enigma_file")
		);
		ctx.registerService(ReadWriteService.TYPE,
				ctx1 -> ReadWriteService.create(EnigmaMappingsReader.DIRECTORY, EnigmaMappingsWriter.DIRECTORY, new FileType.Directory(enigmaMapping), "enigma:enigma_directory")
		);
		ctx.registerService(ReadWriteService.TYPE,
				ctx1 -> ReadWriteService.create(EnigmaMappingsReader.ZIP, EnigmaMappingsWriter.ZIP, new FileType.File("zip"), "enigma:enigma_zip")
		);
		ctx.registerService(ReadWriteService.TYPE,
				ctx1 -> ReadWriteService.create(TinyV2Reader.INSTANCE, TinyV2Writer.INSTANCE, new FileType.File("tinyv2", "tiny"), "enigma:tiny_v2")
		);
		ctx.registerService(ReadWriteService.TYPE,
				ctx1 -> ReadWriteService.create(null, SrgMappingsWriter.INSTANCE, new FileType.File("tsrg"), "enigma:srg_file")
		);
		ctx.registerService(ReadWriteService.TYPE,
				ctx1 -> ReadWriteService.create(ProguardMappingsReader.INSTANCE, null, new FileType.File("txt"), "enigma:proguard")
		);
	}
}
