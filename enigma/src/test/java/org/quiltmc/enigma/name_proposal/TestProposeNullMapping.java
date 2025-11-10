package org.quiltmc.enigma.name_proposal;

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
import org.quiltmc.enigma.test.plugin.AnyVersionEnigmaPlugin;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class TestProposeNullMapping {
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
	public void testNameProposalRemoveMapping() {
		Optional<FieldEntry> entry = project.getJarIndex().getIndex(EntryIndex.class).getFields().stream().findFirst();

		if (entry.isEmpty()) {
			throw new RuntimeException("didn't find any fields");
		}

		// should have a proposed name from jar proposal
		Assertions.assertEquals(TokenType.JAR_PROPOSED, project.getRemapper().getMapping(entry.get()).tokenType());

		// dynamic proposal proposes null and removes the mapping
		project.getRemapper().insertDynamicallyProposedMappings(null, null, null);
		Assertions.assertEquals(TokenType.OBFUSCATED, project.getRemapper().getMapping(entry.get()).tokenType());
	}

	private static class TestPlugin implements AnyVersionEnigmaPlugin {
		@Override
		public void init(EnigmaPluginContext ctx) {
			ctx.registerService(NameProposalService.TYPE, ctx1 -> new TestJarNameProposer());
		}

		// propose names for all fields statically and then remove them dynamically
		private static class TestJarNameProposer implements NameProposalService {
			@Override
			public Map<Entry<?>, EntryMapping> getProposedNames(Enigma enigma, JarIndex index) {
				Map<Entry<?>, EntryMapping> mappings = new HashMap<>();
				AtomicInteger i = new AtomicInteger();
				index.getIndex(EntryIndex.class).getFields().forEach(field -> mappings.put(field, new EntryMapping("a" + (i.getAndIncrement()), null, TokenType.JAR_PROPOSED, this.getId())));

				return mappings;
			}

			@Override
			public Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping) {
				Map<Entry<?>, EntryMapping> mappings = new HashMap<>();
				remapper.getJarIndex().getIndex(EntryIndex.class).getFields().forEach(field -> mappings.put(field, null));
				return mappings;
			}

			@Override
			public String getId() {
				return "test:name_all_fields_a";
			}
		}
	}
}
