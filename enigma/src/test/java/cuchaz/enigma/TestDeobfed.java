package cuchaz.enigma;

import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Decompilers;
import cuchaz.enigma.source.SourceSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static cuchaz.enigma.TestEntryFactory.newClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

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
		assertThat(deobfProject.getJarIndex().getEntryIndex().getClasses(), containsInAnyOrder(
				newClass("cuchaz/enigma/inputs/Keep"),
				newClass("a"),
				newClass("b"),
				newClass("c"),
				newClass("d"),
				newClass("d$1"),
				newClass("e"),
				newClass("f"),
				newClass("g"),
				newClass("g$a"),
				newClass("g$a$a"),
				newClass("g$b"),
				newClass("g$b$a"),
				newClass("h"),
				newClass("h$a"),
				newClass("h$a$a"),
				newClass("h$b"),
				newClass("h$b$a"),
				newClass("h$b$a$a"),
				newClass("h$b$a$b"),
				newClass("i"),
				newClass("i$a"),
				newClass("i$b")
		));
	}

	@Test
	public void decompile() {
		Decompiler decompiler = Decompilers.CFR.create(deobfProject.getClassProvider(), new SourceSettings(false, false));

		decompiler.getSource("a", null);
		decompiler.getSource("b", null);
		decompiler.getSource("c", null);
		decompiler.getSource("d", null);
		decompiler.getSource("d$1", null);
		decompiler.getSource("e", null);
		decompiler.getSource("f", null);
		decompiler.getSource("g", null);
		decompiler.getSource("g$a", null);
		decompiler.getSource("g$a$a", null);
		decompiler.getSource("g$b", null);
		decompiler.getSource("g$b$a", null);
		decompiler.getSource("h", null);
		decompiler.getSource("h$a", null);
		decompiler.getSource("h$a$a", null);
		decompiler.getSource("h$b", null);
		decompiler.getSource("h$b$a", null);
		decompiler.getSource("h$b$a$a", null);
		decompiler.getSource("h$b$a$b", null);
		decompiler.getSource("i", null);
		decompiler.getSource("i$a", null);
		decompiler.getSource("i$b", null);
	}
}
