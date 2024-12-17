package org.quiltmc.enigma.name_proposal;

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
import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.impl.plugin.BuiltinPlugin;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestNameProposalBypassValidation {
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
	public void test() throws IOException, MappingParseException {
		// assert a couple mappings to make sure the test plugin works
		assertMappingStartsWith(TestEntryFactory.newMethod("b", "c", "()V"), TestEntryFactory.newMethod("b", "gaming", "()V"));
		assertMappingStartsWith(TestEntryFactory.newMethod("b", "a", "(I)V"), TestEntryFactory.newMethod("b", "gaming", "(I)V"));

		assertMappingStartsWith(TestEntryFactory.newField("b", "a", "I"), TestEntryFactory.newField("b", "slay", "I"));
		assertMappingStartsWith(TestEntryFactory.newField("b", "a", "Ljava/lang/String;"), TestEntryFactory.newField("b", "slay", "Ljava/lang/String;"));

		// save mappings to temp file
		Path tempFile = Files.createTempFile("temp", ".mapping");
		var service = getService();
		service.write(project.getRemapper().getMappings(), tempFile, new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF, false, null, null));

		// replace project with one that does not have the test plugin
		Enigma enigma = Enigma.create();
		project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());
		project.setMappings(service.read(tempFile), ProgressListener.createEmpty());

		// check which mappings saved
		assertUnmapped(TestEntryFactory.newField("b", "a", "I"));
		assertUnmapped(TestEntryFactory.newField("b", "a", "Ljava/lang/String;"));

		assertMappingStartsWith(TestEntryFactory.newMethod("b", "c", "()V"), TestEntryFactory.newMethod("b", "gaming", "()V"));
		assertMappingStartsWith(TestEntryFactory.newMethod("b", "a", "(I)V"), TestEntryFactory.newMethod("b", "gaming", "(I)V"));
	}

	private static void assertMappingStartsWith(Entry<?> obf, Entry<?> deobf) {
		TranslateResult<? extends Entry<?>> result = project.getRemapper().getDeobfuscator().extendedTranslate(obf);
		assertThat(result, is(notNullValue()));

		String deobfName = result.getValue().getName();
		if (deobfName != null) {
			assertThat(deobfName, startsWith(deobf.getName()));
		}
	}

	private static void assertUnmapped(Entry<?> obf) {
		TranslateResult<? extends Entry<?>> result = project.getRemapper().getDeobfuscator().extendedTranslate(obf);
		assertThat(result, is(notNullValue()));
		assertThat(result.getType(), is(TokenType.OBFUSCATED));
	}

	@SuppressWarnings("all")
	private static ReadWriteService getService() {
		return project.getEnigma().getReadWriteService(project.getEnigma().getSupportedFileTypes().stream().filter(file -> file.getExtensions().contains("mapping") && !file.isDirectory()).findFirst().get()).get();
	}

	private static class TestPlugin implements EnigmaPlugin {
		@Override
		public void init(EnigmaPluginContext ctx) {
			ctx.registerService(NameProposalService.TYPE, ctx1 -> new TestFieldProposerNormal());
			ctx.registerService(NameProposalService.TYPE, ctx1 -> new TestMethodProposerWithBypass());
		}

		private static class TestFieldProposerNormal implements NameProposalService {
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

		private static class TestMethodProposerWithBypass implements NameProposalService {
			@Override
			public Map<Entry<?>, EntryMapping> getProposedNames(Enigma enigma, JarIndex index) {
				Map<Entry<?>, EntryMapping> mappings = new HashMap<>();
				AtomicInteger i = new AtomicInteger();

				index.getIndex(EntryIndex.class).getMethods().forEach(
						method -> mappings.put(method, new EntryMapping("gaming" + i.getAndIncrement(), null, TokenType.DEOBFUSCATED, null))
				);

				return mappings;
			}

			@Override
			public Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping) {
				return null;
			}

			@Override
			public boolean bypassValidation() {
				return true;
			}

			@Override
			public String getId() {
				return "test:name_all_methods_gaming";
			}
		}
	}
}
