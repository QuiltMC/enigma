package org.quiltmc.enigma;

import org.quiltmc.enigma.analysis.EntryReference;
import org.quiltmc.enigma.classprovider.CachingClassProvider;
import org.quiltmc.enigma.classprovider.ClassProvider;
import org.quiltmc.enigma.classprovider.JarClassProvider;
import org.quiltmc.enigma.source.DecompilerService;
import org.quiltmc.enigma.source.Decompilers;
import org.quiltmc.enigma.translation.representation.entry.MethodEntry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DecompilationTest {
	public static final Path JAR = TestUtil.obfJar("decompiler");
	private final Map<DecompilerService, TokenChecker> tokenCheckers = new HashMap<>();
	private final ClassProvider classProvider;

	public DecompilationTest() throws Exception {
		this.classProvider = new CachingClassProvider(new JarClassProvider(JAR));
	}

	private static Stream<DecompilerService> provideDecompilers() {
		return Stream.of(
				Decompilers.VINEFLOWER,
				Decompilers.CFR,
				Decompilers.PROCYON
		);
	}

	@ParameterizedTest
	@MethodSource("provideDecompilers")
	public void testVarargsDecompile(DecompilerService decompiler) {
		TokenChecker checker = this.getTokenChecker(decompiler);
		MethodEntry method = TestEntryFactory.newMethod("a", "a", "()V");
		assertThat(checker.getReferenceTokens(
				new EntryReference<>(TestEntryFactory.newMethod("org/enigma/inputs/Keep", "a", "([Ljava/lang/String;)V"), "", method)
		), contains("a"));
		assertThat(checker.getReferenceTokens(
				new EntryReference<>(TestEntryFactory.newClass("java/lang/String"), "", method)
		), is(empty()));
	}

	private TokenChecker getTokenChecker(DecompilerService decompiler) {
		return this.tokenCheckers.computeIfAbsent(decompiler,
				d -> new TokenChecker(JAR, d, this.classProvider));
	}
}
