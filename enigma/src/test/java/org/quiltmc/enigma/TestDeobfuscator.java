package org.quiltmc.enigma;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.source.Decompiler;
import org.quiltmc.enigma.impl.source.Decompilers;
import org.quiltmc.enigma.api.source.SourceSettings;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

public class TestDeobfuscator {
	private static final Path JAR = TestUtil.obfJar("lone_class");

	private EnigmaProject openProject() throws IOException {
		Enigma enigma = Enigma.create();
		return enigma.openJar(JAR, new ClasspathClassProvider(), ProgressListener.none());
	}

	@Test
	public void loadJar() throws Exception {
		this.openProject();
	}

	@Test
	public void decompileClass() throws Exception {
		EnigmaProject project = this.openProject();
		Decompiler decompiler = Decompilers.CFR.create(project.getClassProvider(), new SourceSettings(false, false));

		decompiler.getUndocumentedSource("a").asString();
	}
}
