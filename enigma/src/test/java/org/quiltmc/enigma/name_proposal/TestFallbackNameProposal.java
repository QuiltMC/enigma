package org.quiltmc.enigma.name_proposal;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestEntryFactory;
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
import org.quiltmc.enigma.api.stats.GenerationParameters;
import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.api.stats.StatsGenerator;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.impl.plugin.BuiltinPlugin;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFallbackNameProposal {
	private static final Path JAR = TestUtil.obfJar("validation");
	private static EnigmaProject project;

	@BeforeAll
	public static void setupEnigma() {
		Reader r = new StringReader("""
				{
					"services": {
						"name_proposal": [
							{
								"id": "test:name_all_fields_slay"
							},
							{
								"id": "test:name_all_methods_gaming"
							}
						]
					}
				}""");

		try {
			EnigmaProfile profile = EnigmaProfile.parse(r);
			Enigma enigma = Enigma.builder().setProfile(profile).setPlugins(List.of(new BuiltinPlugin(), new TestPlugin())).build();
			project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());
		} catch (Exception e) {
			throw new RuntimeException("Failed to open jar!", e);
		}
	}

	@Test
	public void testFallbackStats() throws IOException {
		ClassEntry bClass = TestEntryFactory.newClass("b");

		// assert a couple mappings to make sure the test plugin works
		assertMappingStartsWith(TestEntryFactory.newMethod(bClass, "c", "()V"), TestEntryFactory.newMethod(bClass, "gaming", "()V"));
		assertMappingStartsWith(TestEntryFactory.newMethod(bClass, "a", "(I)V"), TestEntryFactory.newMethod(bClass, "gaming", "(I)V"));

		assertMappingStartsWith(TestEntryFactory.newField(bClass, "a", "I"), TestEntryFactory.newField(bClass, "slay", "I"));
		assertMappingStartsWith(TestEntryFactory.newField(bClass, "a", "Ljava/lang/String;"), TestEntryFactory.newField(bClass, "slay", "Ljava/lang/String;"));

		var proposerFieldStats = new StatsGenerator(project).generate(ProgressListener.createEmpty(), bClass, new GenerationParameters(EnumSet.of(StatType.FIELDS)));
		var proposerMethodStats = new StatsGenerator(project).generate(ProgressListener.createEmpty(), bClass, new GenerationParameters(EnumSet.of(StatType.METHODS)));

		project = Enigma.create().openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());

		var controlFieldStats = new StatsGenerator(project).generate(ProgressListener.createEmpty(), bClass, new GenerationParameters(EnumSet.of(StatType.FIELDS)));
		var controlMethodStats = new StatsGenerator(project).generate(ProgressListener.createEmpty(), bClass, new GenerationParameters(EnumSet.of(StatType.METHODS)));

		// method stats should be identical since fallback proposals don't affect stats
		assertEquals(controlMethodStats.getMappable(), proposerMethodStats.getMappable());
		assertEquals(controlMethodStats.getMapped(), proposerMethodStats.getMapped());
		assertEquals(controlMethodStats.getUnmapped(), proposerMethodStats.getUnmapped());

		// field stats should be fully mapped when proposed -- normal behaviour
		assertEquals(controlFieldStats.getMappable(), proposerFieldStats.getMappable());
		assertEquals(0, proposerFieldStats.getUnmapped());
		assertEquals(controlFieldStats.getMappable(), proposerFieldStats.getMapped());
	}

	private static void assertMappingStartsWith(Entry<?> obf, Entry<?> deobf) {
		TranslateResult<? extends Entry<?>> result = project.getRemapper().getDeobfuscator().extendedTranslate(obf);
		assertThat(result, is(notNullValue()));

		String deobfName = result.getValue().getName();
		if (deobfName != null) {
			assertThat(deobfName, startsWith(deobf.getName()));
		}
	}

	private static class TestPlugin implements EnigmaPlugin {
		@Override
		public void init(EnigmaPluginContext ctx) {
			ctx.registerService(NameProposalService.TYPE, ctx1 -> new TestPlugin.TestFieldProposerNoFallback());
			ctx.registerService(NameProposalService.TYPE, ctx1 -> new TestPlugin.TestMethodProposerWithFallback());
		}

		private static class TestFieldProposerNoFallback implements NameProposalService {
			@Override
			public Map<Entry<?>, EntryMapping> getProposedNames(Enigma enigma, JarIndex index) {
				Map<Entry<?>, EntryMapping> mappings = new HashMap<>();
				AtomicInteger i = new AtomicInteger();

				index.getIndex(EntryIndex.class).getFields().forEach(
						field -> mappings.put(field, this.createMapping("slay" + i.getAndIncrement(), TokenType.JAR_PROPOSED))
				);

				return mappings;
			}

			@Override
			public Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping) {
				return null;
			}

			@Override
			public String getId() {
				return "test:name_all_fields_slay";
			}
		}

		private static class TestMethodProposerWithFallback implements NameProposalService {
			@Override
			public Map<Entry<?>, EntryMapping> getProposedNames(Enigma enigma, JarIndex index) {
				Map<Entry<?>, EntryMapping> mappings = new HashMap<>();
				AtomicInteger i = new AtomicInteger();

				index.getIndex(EntryIndex.class).getMethods().forEach(
						method -> mappings.put(method, this.createMapping("gaming" + i.getAndIncrement(), TokenType.JAR_PROPOSED))
				);

				return mappings;
			}

			@Override
			public Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping) {
				return null;
			}

			@Override
			public boolean isFallback() {
				return true;
			}

			@Override
			public String getId() {
				return "test:name_all_methods_gaming";
			}
		}
	}
}
