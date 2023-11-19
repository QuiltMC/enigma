package org.quiltmc.enigma.name_proposal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaPlugin;
import org.quiltmc.enigma.api.EnigmaPluginContext;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TestNameProposal {
	private static final Path JAR = TestUtil.obfJar("enums");
	private static EnigmaProject project;

	@BeforeAll
	public static void setupEnigma() {
		Reader r = new StringReader("""
				{
					"services": {
						"name_proposal": [
							{
								"id": "test:name_all_fields_a"
							},
							{
								"id": "test:name_all_fields_b"
							},
							{
								"id": "test:name_all_fields_c"
							},
							{
								"id": "test:name_all_fields_d"
							}
						]
					}
				}""");

		try {
			EnigmaProfile profile = EnigmaProfile.parse(r);
			Enigma enigma = Enigma.builder().setProfile(profile).setPlugins(List.of(new TestPlugin())).build();
			project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.none());
		} catch (Exception e) {
			Logger.error(e, "Failed to open jar!");
		}
	}

	@Test
	public void testProposalPriority() {
		project.getJarIndex().getIndex(EntryIndex.class).getFields().forEach(field -> {
			EntryMapping mapping = project.getRemapper().getMapping(field);

			if (!mapping.tokenType().isProposed()) {
				throw new RuntimeException("Name proposal failed to name all fields!");
			}

			Assertions.assertEquals("test:name_all_fields_a", mapping.sourcePluginId(), String.format("Expected test:name_all_fields_a to run last, but %s did!", mapping.sourcePluginId()));
		});
	}

	private static class TestPlugin implements EnigmaPlugin {
		@Override
		public void init(EnigmaPluginContext ctx) {
			nameAllFields(ctx, "d");
			nameAllFields(ctx, "a");
			nameAllFields(ctx, "c");
			nameAllFields(ctx, "b");
		}

		private static void nameAllFields(EnigmaPluginContext ctx, String prefix) {
			String id = "test:name_all_fields_" + prefix;
			ctx.registerService(NameProposalService.TYPE, ctx1 -> new TestNameProposer(prefix, id));
		}

		private record TestNameProposer(String prefix, String id) implements NameProposalService {
			@Override
			public Map<Entry<?>, EntryMapping> getProposedNames(JarIndex index) {
				Map<Entry<?>, EntryMapping> mappings = new HashMap<>();
				AtomicInteger i = new AtomicInteger();

				index.getIndex(EntryIndex.class).getFields().forEach(field -> mappings.put(field, this.createMapping(this.prefix + (i.getAndIncrement()), TokenType.JAR_PROPOSED)));

				return mappings;
			}

			@Override
			public Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping) {
				return null;
			}

			@Override
			public String getId() {
				return this.id;
			}
		}
	}
}
