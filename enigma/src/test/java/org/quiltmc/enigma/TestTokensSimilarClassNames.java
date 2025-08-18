package org.quiltmc.enigma;

import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.source.DecompiledClassSource;
import org.quiltmc.enigma.api.source.Decompiler;
import org.quiltmc.enigma.api.source.Decompilers;
import org.quiltmc.enigma.api.source.SourceSettings;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestTokensSimilarClassNames {
	private static final Path JAR = TestUtil.obfJar("similar_class_names");

	@Test
	public void testSimilarClassNames() throws IOException {
		EnigmaProject project = openProject();
		ClassEntry alpha = TestEntryFactory.newClass(getClassName("Alpha"));
		Decompiler decompiler = Decompilers.VINEFLOWER.create(new JarClassProvider(JAR), new SourceSettings(false, false));
		DecompiledClassSource decomp = new DecompiledClassSource(alpha, decompiler.getUndocumentedSource(getClassName("Alpha")).index());
		DecompiledClassSource source = decomp.remapSource(project, project.getRemapper().getDeobfuscator());
		Collection<Token> tokens = source.getHighlightedTokens().get(TokenType.OBFUSCATED);
		for (Token token : tokens) {
			assertFalse(token.text.startsWith("Alphabet"));
		}
	}

	private static String getClassName(String className) {
		return "org/quiltmc/enigma/input/similar_class_names/" + className;
	}

	private static EnigmaProject openProject() {
		try {
			Enigma enigma = Enigma.create();
			return enigma.openJar(JAR, new JarClassProvider(JAR), ProgressListener.createEmpty());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
