package org.quiltmc.enigma;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.source.Decompiler;
import org.quiltmc.enigma.impl.source.Decompilers;
import org.quiltmc.enigma.api.source.SourceSettings;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;

public class TestDeobfed {
	public static final Path OBF = TestUtil.obfJar("translation");
	public static final Path DEOBF = TestUtil.deobfJar("translation");
	private static EnigmaProject deobfProject;

	@BeforeAll
	public static void beforeClass() throws Exception {
		Enigma enigma = Enigma.create();

		Files.createDirectories(DEOBF.getParent());
		EnigmaProject obfProject = enigma.openJar(OBF, new ClasspathClassProvider(), ProgressListener.none());
		obfProject.exportRemappedJar(ProgressListener.none()).write(DEOBF, ProgressListener.none());

		deobfProject = enigma.openJar(DEOBF, new ClasspathClassProvider(), ProgressListener.none());
	}

	@Test
	public void obfEntries() {
		assertThat(deobfProject.getJarIndex().getEntryIndex().getClasses(), Matchers.containsInAnyOrder(
				TestEntryFactory.newClass("org/quiltmc/enigma/input/Keep"),
				TestEntryFactory.newClass("a"),
				TestEntryFactory.newClass("b"),
				TestEntryFactory.newClass("c"),
				TestEntryFactory.newClass("d"),
				TestEntryFactory.newClass("d$1"),
				TestEntryFactory.newClass("e"),
				TestEntryFactory.newClass("f"),
				TestEntryFactory.newClass("g"),
				TestEntryFactory.newClass("g$a"),
				TestEntryFactory.newClass("g$a$a"),
				TestEntryFactory.newClass("g$b"),
				TestEntryFactory.newClass("g$b$a"),
				TestEntryFactory.newClass("h"),
				TestEntryFactory.newClass("h$a"),
				TestEntryFactory.newClass("h$a$a"),
				TestEntryFactory.newClass("h$b"),
				TestEntryFactory.newClass("h$b$a"),
				TestEntryFactory.newClass("h$b$a$a"),
				TestEntryFactory.newClass("h$b$a$b"),
				TestEntryFactory.newClass("i"),
				TestEntryFactory.newClass("i$a"),
				TestEntryFactory.newClass("i$b")
		));
	}

	@Test
	public void decompile() {
		Decompiler decompiler = Decompilers.CFR.create(deobfProject.getClassProvider(), new SourceSettings(false, false));

		decompiler.getSource("a");
		decompiler.getSource("b");
		decompiler.getSource("c");
		decompiler.getSource("d");
		decompiler.getSource("d$1");
		decompiler.getSource("e");
		decompiler.getSource("f");
		decompiler.getSource("g");
		decompiler.getSource("g$a");
		decompiler.getSource("g$a$a");
		decompiler.getSource("g$b");
		decompiler.getSource("g$b$a");
		decompiler.getSource("h");
		decompiler.getSource("h$a");
		decompiler.getSource("h$a$a");
		decompiler.getSource("h$b");
		decompiler.getSource("h$b$a");
		decompiler.getSource("h$b$a$a");
		decompiler.getSource("h$b$a$b");
		decompiler.getSource("i");
		decompiler.getSource("i$a");
		decompiler.getSource("i$b");
	}
}
