package org.quiltmc.enigma.impl.plugin;

import org.quiltmc.enigma.api.EnigmaPluginContext;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.serde.FileType;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.serde.enigma.EnigmaMappingsReader;
import org.quiltmc.enigma.api.translation.mapping.serde.enigma.EnigmaMappingsWriter;
import org.quiltmc.enigma.api.translation.mapping.serde.proguard.ProguardMappingsReader;
import org.quiltmc.enigma.api.translation.mapping.serde.srg.SrgMappingsWriter;
import org.quiltmc.enigma.api.translation.mapping.serde.tinyv2.TinyV2Reader;
import org.quiltmc.enigma.api.translation.mapping.serde.tinyv2.TinyV2Writer;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;

public class BuiltinMappingFormats {
	public static void register(EnigmaPluginContext ctx) {
		FileType.File enigmaMapping = new FileType.File("mapping", "mappings");

		ctx.registerService(ReadWriteService.TYPE,
			ctx1 -> ReadWriteService.create(EnigmaMappingsReader.FILE, EnigmaMappingsWriter.FILE, enigmaMapping, "enigma_file")
		);
		ctx.registerService(ReadWriteService.TYPE,
			ctx1 -> ReadWriteService.create(EnigmaMappingsReader.DIRECTORY, EnigmaMappingsWriter.DIRECTORY, new FileType.Directory(enigmaMapping), "enigma_directory")
		);
		ctx.registerService(ReadWriteService.TYPE,
			ctx1 -> ReadWriteService.create(EnigmaMappingsReader.ZIP, EnigmaMappingsWriter.ZIP, new FileType.File("zip"), "enigma_zip")
		);
		ctx.registerService(ReadWriteService.TYPE,
			ctx1 -> new ReadWriteService() {
				static final TinyV2Reader reader = new TinyV2Reader();
				static final TinyV2Writer writer = new TinyV2Writer();

				@Override
				public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
					writer.write(mappings, delta, path, progress, saveParameters);
				}

				@Override
				public EntryTree<EntryMapping> read(Path path, ProgressListener progress) throws MappingParseException, IOException {
					return reader.read(path, progress);
				}

				@Override
				public FileType getFileType() {
					return new FileType.File("tiny");
				}

				@Override
				public boolean supportsReading() {
					return true;
				}

				@Override
				public boolean supportsWriting() {
					return true;
				}

				@Override
				public String getId() {
					return "tiny_v2";
				}
			}
		);
		ctx.registerService(ReadWriteService.TYPE,
			ctx1 -> ReadWriteService.create(null, SrgMappingsWriter.INSTANCE, new FileType.File("tsrg"), "srg_file")
		);
		ctx.registerService(ReadWriteService.TYPE,
			ctx1 -> ReadWriteService.create(ProguardMappingsReader.INSTANCE, null, new FileType.File("txt"), "proguard")
		);
	}
}
