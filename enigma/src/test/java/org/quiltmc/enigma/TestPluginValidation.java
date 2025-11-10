package org.quiltmc.enigma;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaPluginContext;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.serde.FileType;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.test.plugin.AnyVersionEnigmaPlugin;

import java.nio.file.Path;
import java.util.List;

public class TestPluginValidation {
	@Test
	public void testIdValidation() {
		Enigma.builder().setPlugins(List.of(new IdTestPlugin())).build();
	}

	@Test
	public void testDuplicateServices() {
		Enigma.builder().setPlugins(List.of(new DuplicateIdTestPlugin())).build();
	}

	@Test
	public void testDuplicateFileTypes() {
		Enigma.builder().setPlugins(List.of(new DuplicateFileTypeTestPlugin())).build();
	}

	private static void registerService(EnigmaPluginContext ctx, String id) {
		registerService(ctx, id, id);
	}

	private static void registerService(EnigmaPluginContext ctx, String id, String fileType) {
		ctx.registerService(ReadWriteService.TYPE, ctx1 -> new ReadWriteService() {
			@Override
			public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
			}

			@Override
			public EntryTree<EntryMapping> read(Path path, ProgressListener progress) {
				return null;
			}

			@Override
			public FileType getFileType() {
				return new FileType.File(fileType);
			}

			@Override
			public boolean supportsReading() {
				return false;
			}

			@Override
			public boolean supportsWriting() {
				return false;
			}

			@Override
			public String getId() {
				return id;
			}
		});
	}

	private static class DuplicateFileTypeTestPlugin implements AnyVersionEnigmaPlugin {
		@Override
		public void init(EnigmaPluginContext ctx) {
			registerService(ctx, "test:grind", "gaming");
			Assertions.assertThrows(IllegalStateException.class, () -> registerService(ctx, "test:slay", "gaming"));
		}
	}

	private static class DuplicateIdTestPlugin implements AnyVersionEnigmaPlugin {
		@Override
		public void init(EnigmaPluginContext ctx) {
			registerService(ctx, "grind:ground");
			Assertions.assertThrows(IllegalStateException.class, () -> registerService(ctx, "grind:ground"));
		}
	}

	private static class IdTestPlugin implements AnyVersionEnigmaPlugin {
		@Override
		public void init(EnigmaPluginContext ctx) {
			// empty
			Assertions.assertThrows(IllegalArgumentException.class, () -> registerService(ctx, ""));
			// no namespace
			Assertions.assertThrows(IllegalArgumentException.class, () -> registerService(ctx, "grind"));
			// slashes in wrong place
			Assertions.assertThrows(IllegalArgumentException.class, () -> registerService(ctx, "grind/grind:ground"));
			// uppercase chars
			Assertions.assertThrows(IllegalArgumentException.class, () -> registerService(ctx, "grind:Ground"));
			// invalid chars
			Assertions.assertThrows(IllegalArgumentException.class, () -> registerService(ctx, "grind:ground!"));
			// valid
			registerService(ctx, "grind:ground");
			registerService(ctx, "grind:ground_");
			registerService(ctx, "grind:ground_grind/g_rind2");
			registerService(ctx, "grind:ground/grind");
			registerService(ctx, "grind:ground/grind/grind");
		}
	}
}
