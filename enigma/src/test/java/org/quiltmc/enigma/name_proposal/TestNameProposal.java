package org.quiltmc.enigma.name_proposal;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
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
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.test.plugin.AnyVersionEnigmaPlugin;
import org.quiltmc.enigma.util.validation.ValidationContext;
import org.tinylog.Logger;

import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
							},
							{
								"id": "test:owner_name"
							}
						]
					}
				}""");

		try {
			EnigmaProfile profile = EnigmaProfile.parse(r);
			Enigma enigma = Enigma.builder().setProfile(profile).setPlugins(List.of(new TestPlugin())).build();
			project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());
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

	@Test
	public void testJarNameProposal() {
		project.getJarIndex().getIndex(EntryIndex.class).getFields().forEach(field -> {
			EntryMapping mapping = project.getRemapper().getMapping(field);
			Assertions.assertTrue(mapping.targetName() != null && mapping.tokenType() == TokenType.JAR_PROPOSED && mapping.sourcePluginId() != null, "Entry '" + field + "' did not have a proposed name (mapping: '" + mapping + "' !");
		});
	}

	@Test
	public void testDynamicNameProposal() {
		Optional<FieldEntry> entry = project.getJarIndex().getIndex(EntryIndex.class).getFields().stream().findFirst();

		if (entry.isEmpty()) {
			throw new RuntimeException("didn't find any fields");
		}

		project.getRemapper().putMapping(new ValidationContext(null), entry.get(), new EntryMapping("query", null, TokenType.DEOBFUSCATED, null));
		Assertions.assertEquals(new EntryMapping("QueryOwner", null, TokenType.DYNAMIC_PROPOSED, "test:owner_name"), project.getRemapper().getMapping(entry.get().getParent()));

		Optional<MethodEntry> entry2 = project.getJarIndex().getIndex(EntryIndex.class).getMethods().stream().findFirst();

		if (entry2.isEmpty()) {
			throw new RuntimeException("didn't find any methods");
		}

		project.getRemapper().putMapping(new ValidationContext(null), entry2.get(), new EntryMapping("testFoo", null, TokenType.DEOBFUSCATED, null));
		Assertions.assertEquals(new EntryMapping("TestFooOwner", null, TokenType.DYNAMIC_PROPOSED, "test:owner_name"), project.getRemapper().getMapping(entry2.get().getParent()));
	}

	private static class TestPlugin implements AnyVersionEnigmaPlugin {
		@Override
		public void init(EnigmaPluginContext ctx) {
			nameAllFields(ctx, "d");
			nameAllFields(ctx, "a");
			nameAllFields(ctx, "c");
			nameAllFields(ctx, "b");

			ctx.registerService(NameProposalService.TYPE, ctx1 -> new TestDynamicNameProposer("test:owner_name"));
		}

		private static void nameAllFields(EnigmaPluginContext ctx, String prefix) {
			String id = "test:name_all_fields_" + prefix;
			ctx.registerService(NameProposalService.TYPE, ctx1 -> new TestJarNameProposer(prefix, id));
		}

		private record TestJarNameProposer(String prefix, String id) implements NameProposalService {
			@Override
			public Map<Entry<?>, EntryMapping> getProposedNames(Enigma enigma, JarIndex index) {
				Map<Entry<?>, EntryMapping> mappings = new HashMap<>();
				AtomicInteger i = new AtomicInteger();

				index.getIndex(EntryIndex.class).getFields().forEach(field -> mappings.put(field, new EntryMapping(this.prefix + (i.getAndIncrement()), null, TokenType.JAR_PROPOSED, this.id)));

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

		private record TestDynamicNameProposer(String id) implements NameProposalService {
			@Override
			public Map<Entry<?>, EntryMapping> getProposedNames(Enigma enigma, JarIndex index) {
				return null;
			}

			@Override
			public Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping) {
				if (obfEntry != null && oldMapping != null && newMapping != null) {
					var name = newMapping.targetName();
					if (name != null && !name.isEmpty() && obfEntry.getParent() != null) {
						name = name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1) + "Owner";
						return Map.of(obfEntry.getParent(), new EntryMapping(name, null, TokenType.DYNAMIC_PROPOSED, this.id));
					}
				}

				return null;
			}

			@Override
			public String getId() {
				return this.id;
			}
		}
	}
}
