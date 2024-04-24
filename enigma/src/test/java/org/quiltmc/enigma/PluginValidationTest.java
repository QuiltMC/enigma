package org.quiltmc.enigma;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaPlugin;
import org.quiltmc.enigma.api.EnigmaPluginContext;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.serde.FileType;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;

import java.nio.file.Path;
import java.util.List;

public class PluginValidationTest {
	@Test
	public void testIdValidation() {
		Enigma.builder().setPlugins(List.of(new IdTestPlugin())).build();
	}

	private static class IdTestPlugin implements EnigmaPlugin {
		@Override
		public void init(EnigmaPluginContext ctx) {
			// empty
			Assertions.assertThrows(IllegalArgumentException.class, () -> this.registerService(ctx, ""));
			// no namespace
			Assertions.assertThrows(IllegalArgumentException.class, () -> this.registerService(ctx, "grind"));
			// slashes in wrong place
			Assertions.assertThrows(IllegalArgumentException.class, () -> this.registerService(ctx, "grind/grind:ground"));
			// uppercase chars
			Assertions.assertThrows(IllegalArgumentException.class, () -> this.registerService(ctx, "grind:Ground"));
			// invalid chars
			Assertions.assertThrows(IllegalArgumentException.class, () -> this.registerService(ctx, "grind:ground!"));
			// valid
			this.registerService(ctx, "grind:ground");
			this.registerService(ctx, "grind:ground_");
			this.registerService(ctx, "grind:ground_grind/g_rind2");
			this.registerService(ctx, "grind:ground/grind");
			this.registerService(ctx, "grind:ground/grind/grind");
		}

		private void registerService(EnigmaPluginContext ctx, String id) {
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
					return new FileType.File(id);
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
	}
}
