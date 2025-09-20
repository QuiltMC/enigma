package org.quiltmc.enigma;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestIsRenamable {
	public static final Path OBF = TestUtil.obfJar("translation");
	public static final Path DEOBF = TestUtil.deobfJar("translation");
	private static EnigmaProject obfProject;

	@BeforeAll
	public static void beforeClass() throws Exception {
		Enigma enigma = Enigma.create();

		Files.createDirectories(DEOBF.getParent());
		obfProject = enigma.openJar(OBF, new ClasspathClassProvider(), ProgressListener.createEmpty());
	}

	@Test
	public void obfEntries() {
		var equals = TestEntryFactory.newMethod("a", "equals", "(Ljava/lang/Object;)Z");
		Assertions.assertFalse(obfProject.isRenamable(equals));

		// does actually exist in the code
		var randomMethod = TestEntryFactory.newMethod("a", "a", "()V");
		Assertions.assertTrue(obfProject.isRenamable(randomMethod));

		var getClass = TestEntryFactory.newMethod("java/lang/Object", "getClass", "()Ljava/lang/Class;");
		Assertions.assertFalse(obfProject.isRenamable(getClass));

		var toString = TestEntryFactory.newMethod("java/lang/Record", "toString", "()Ljava/lang/String;");
		Assertions.assertFalse(obfProject.isRenamable(toString));

		var wait = TestEntryFactory.newMethod("b", "wait", "()V");
		Assertions.assertFalse(obfProject.isRenamable(wait));
	}

	@Test
	public void libraryOverrides() {
		final ClassEntry nbtCollection = TestEntryFactory.newClass("j");
		final ClassEntry nbtList = TestEntryFactory.newClass("k");

		final String sizeDescriptor = "()I";
		final String sizeName = "size";
		final MethodEntry nbtCollectionSize = TestEntryFactory.newMethod(nbtCollection, sizeName, sizeDescriptor);
		final MethodEntry nbtListSize = TestEntryFactory.newMethod(nbtList, sizeName, sizeDescriptor);

		Assertions.assertFalse(obfProject.isRenamable(nbtCollectionSize));
		Assertions.assertFalse(obfProject.isRenamable(nbtListSize));
	}
}
