package org.quiltmc.enigma.translation.mapping;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.TestEntryFactory;
import org.quiltmc.enigma.TestUtil;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.serde.FileType;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingFileNameFormat;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;

public class TestMethodOverrideParamJavadoc {
	private static final MappingSaveParameters PARAMETERS = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF, false, null, null);
	private static final Path JAR = TestUtil.obfJar("interfaces");
	private static Enigma enigma;
	private static EnigmaProject project;

	@BeforeEach
	void setupEnigma() throws IOException {
		enigma = Enigma.create();
		project = enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.createEmpty());
	}

	void test(ReadWriteService readWriteService, String tmpNameSuffix) throws IOException, MappingParseException {
		ClassEntry inheritor = TestEntryFactory.newClass("a");
		MethodEntry method = TestEntryFactory.newMethod(inheritor, "a", "(D)D");
		LocalVariableEntry param = TestEntryFactory.newParameter(method, 1);

		EntryMapping mapping = project.getRemapper().getMapping(param);
		Assertions.assertNull(mapping.javadoc());

		project.getRemapper().putMapping(TestUtil.newVC(), param, mapping.withJavadoc("gaming"));

		EntryMapping withJavadoc = project.getRemapper().getMapping(param);
		Assertions.assertEquals("gaming", withJavadoc.javadoc());

		File tempFile = File.createTempFile("testMethodOverrideParamJavadoc", tmpNameSuffix);
		tempFile.delete(); //remove the auto created file

		readWriteService.write(project.getRemapper().getMappings(), tempFile.toPath(), ProgressListener.createEmpty(), PARAMETERS);
		Assertions.assertTrue(tempFile.exists(), "Written file not created");
		EntryTree<EntryMapping> loadedMappings = readWriteService.read(tempFile.toPath(), ProgressListener.createEmpty());

		project.setMappings(loadedMappings, ProgressListener.createEmpty());

		EntryMapping newMapping = project.getRemapper().getMapping(param);
		Assertions.assertEquals("gaming", newMapping.javadoc());
	}

	@Test
	public void testEnigmaFile() throws IOException, MappingParseException {
		this.test(this.getService(file -> file.getExtensions().contains("mapping") && !file.isDirectory()), ".mapping");
	}

	@Test
	public void testTinyFile() throws IOException, MappingParseException {
		this.test(this.getService(file -> file.getExtensions().contains("tiny") && !file.isDirectory()), ".tiny");
	}

	@SuppressWarnings("all")
	private ReadWriteService getService(Predicate<FileType> predicate) {
		return this.enigma.getReadWriteService(this.enigma.getSupportedFileTypes().stream().filter(predicate).findFirst().get()).get();
	}
}
