package org.quiltmc.enigma.translation.mapping;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestEntryFactory;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import java.nio.file.Path;

public class EntryRemapperTest {
	public static final Path JAR = TestUtil.obfJar("interface_union");
	private static EnigmaProject project;
	private static EntryRemapper remapper;

	@BeforeAll
	public static void beforeAll() throws Exception {
		Enigma enigma = Enigma.create();
		project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.none());
		remapper = project.getRemapper();
	}

	@BeforeEach
	public void beforeEach() {
		EntryTree<EntryMapping> mappings = new HashEntryTree<>();
		project.setMappings(mappings, ProgressListener.none());
		remapper = project.getRemapper();
	}

	private static void assertName(Entry<?> entry, String expected) {
		Entry<?> deobf = remapper.deobfuscate(entry);
		Assertions.assertNotNull(deobf);
		Assertions.assertEquals(expected, deobf.getName());
	}

	@Test
	public void testUnionRename() {
		var name = "unionAB";
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newMethod("e", "a", "()V"), new EntryMapping(name));

		assertName(TestEntryFactory.newMethod("e", "a", "()V"), name);
		assertName(TestEntryFactory.newMethod("a", "a", "()V"), name);
		assertName(TestEntryFactory.newMethod("b", "a", "()V"), name);

		name = "unionBC";
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newMethod("f", "a", "()Z"), new EntryMapping(name));

		assertName(TestEntryFactory.newMethod("f", "a", "()Z"), name);
		assertName(TestEntryFactory.newMethod("b", "a", "()Z"), name);
		assertName(TestEntryFactory.newMethod("c", "a", "()Z"), name);

		name = "unionA3";
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newMethod("g", "a", "()D"), new EntryMapping(name));

		assertName(TestEntryFactory.newMethod("g", "a", "()D"), name);
		assertName(TestEntryFactory.newMethod("a", "a", "()D"), name);
	}

	@Test
	public void testElementRename() {
		var name = "unionAB";
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newMethod("e", "a", "()V"), new EntryMapping(name));

		assertName(TestEntryFactory.newMethod("a", "a", "()V"), name);
		assertName(TestEntryFactory.newMethod("e", "a", "()V"), name);
		assertName(TestEntryFactory.newMethod("b", "a", "()V"), name);

		name = "unionBC";
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newMethod("b", "a", "()Z"), new EntryMapping(name));

		assertName(TestEntryFactory.newMethod("b", "a", "()Z"), name);
		assertName(TestEntryFactory.newMethod("f", "a", "()Z"), name);
		assertName(TestEntryFactory.newMethod("c", "a", "()Z"), name);

		name = "unionA3";
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newMethod("a", "a", "()D"), new EntryMapping(name));

		assertName(TestEntryFactory.newMethod("a", "a", "()D"), name);
		assertName(TestEntryFactory.newMethod("g", "a", "()D"), name);

		name = "unionCD";
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newMethod("c", "a", "()F"), new EntryMapping(name));

		assertName(TestEntryFactory.newMethod("c", "a", "()F"), name);
		assertName(TestEntryFactory.newMethod("d", "a", "()F"), name);

		name = "unionDC";
		remapper.putMapping(TestUtil.newVC(), TestEntryFactory.newMethod("d", "a", "()F"), new EntryMapping(name));

		assertName(TestEntryFactory.newMethod("d", "a", "()F"), name);
		assertName(TestEntryFactory.newMethod("c", "a", "()F"), name);
	}
}
