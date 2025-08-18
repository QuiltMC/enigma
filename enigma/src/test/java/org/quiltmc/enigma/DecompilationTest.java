package org.quiltmc.enigma;

import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.class_provider.CachingClassProvider;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.service.DecompilerService;
import org.quiltmc.enigma.api.source.Decompilers;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
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
	public void testInvalidIdentifiers(DecompilerService decompiler) {
		TokenChecker checker = this.getTokenChecker(decompiler);

		// field declarations
		for (TypeDescriptor.Primitive type : TypeDescriptor.Primitive.values()) {
			assertThat(
					checker.getDeclarationToken(TestEntryFactory.newField("a", type.getKeyword(), "" + type.getCode())),
					equalTo(type.getKeyword())
			);
		}

		List<String> fieldNames = List.of("final", "break", "for", "static", "super", "private", "import", "synchronized", "$");
		for (String name : fieldNames) {
			assertThat(
					checker.getDeclarationToken(TestEntryFactory.newField("a", name, "I")),
					equalTo(name)
			);
		}

		assertThat(
				checker.getDeclarationToken(TestEntryFactory.newField("a$abstract", "transient", "C")),
				equalTo("transient")
		);
		assertThat(
				checker.getDeclarationToken(TestEntryFactory.newField("a$abstract", "volatile", "Z")),
				equalTo("volatile")
		);
		assertThat(
				checker.getDeclarationToken(TestEntryFactory.newField("a$abstract", "false", "Z")),
				equalTo("false")
		);

		// method declarations
		List<String> methodNames = List.of("new", "assert", "try", "switch", "void", "throws", "class", "while");
		for (String name : methodNames) {
			assertThat(
					checker.getDeclarationToken(TestEntryFactory.newMethod("a", name, "()V")),
					equalTo(name)
			);
		}

		assertThat(
				checker.getDeclarationToken(TestEntryFactory.newMethod("a", "native", "()I")),
				equalTo("native")
		);
		if (!decompiler.getId().equals("enigma:cfr")) {
			// cfr doesnt decompile the local interface, just the local class
			assertThat(
					checker.getDeclarationToken(TestEntryFactory.newMethod("a$interface", "throws", "()V")),
					equalTo("throws")
			);
			assertThat(
					checker.getDeclarationToken(TestEntryFactory.newMethod("a$interface", "enum", "()I")),
					equalTo("enum")
			);
		}

		// class declarations
		assertThat(
				checker.getDeclarationToken(TestEntryFactory.newClass("a$enum")),
				equalTo("enum")
		);
		if (!decompiler.getId().equals("enigma:cfr")) {
			assertThat(
					checker.getDeclarationToken(TestEntryFactory.newClass("a$interface")),
					equalTo("interface")
			);
		}

		assertThat(
				checker.getDeclarationToken(TestEntryFactory.newClass("a$abstract")),
				equalTo(decompiler.getId().equals("enigma:cfr") ? "Abstract" : "abstract") // cfr capitalizes the class for some reason
		);

		// field references
		if (!decompiler.getId().equals("enigma:cfr")) {
			// cfr doesn't decompile the field assign inside the constructor
			assertThat(
					checker.getReferenceTokens(new EntryReference<>(
						TestEntryFactory.newField("a$abstract", "transient", "C"),
						"",
						TestEntryFactory.newMethod("a$abstract", "<init>", "(La;)V")
					)),
					contains("transient")
			);
		}

		if (!decompiler.getId().equals("enigma:vineflower")) {
			// due to an oversight, there's no way to know where a method starts and ends when using vineflower
			// so any references after the throws() declaration are linked to said method instead of the correct one, class()
			assertThat(
					checker.getReferenceTokens(new EntryReference<>(
						TestEntryFactory.newField("a$abstract", "transient", "C"),
						"",
						TestEntryFactory.newMethod("a", "class", "()V")
					)),
					contains("transient")
			);
		}

		// method references
		assertThat(
				checker.getReferenceTokens(new EntryReference<>(
					TestEntryFactory.newMethod("a", "new", "()V"),
					"",
					TestEntryFactory.newMethod("a", "try", "()V")
				)),
				contains("new")
		);
		if (!decompiler.getId().equals("enigma:vineflower")) {
			assertThat(
					checker.getReferenceTokens(new EntryReference<>(
						TestEntryFactory.newMethod("a$abstract", "throws", "()V"),
						"",
						TestEntryFactory.newMethod("a", "class", "()V")
					)),
					contains("throws")
			);
		}

		// class references
		if (!decompiler.getId().equals("enigma:cfr")) {
			assertThat(
					checker.getReferenceTokens(new EntryReference<>(
						TestEntryFactory.newClass("a$interface"),
						"",
						decompiler.getId().equals("enigma:procyon") ? TestEntryFactory.newClass("a$abstract") : TestEntryFactory.newMethod("a", "class", "()V")
					)),
					contains("interface")
			);
		}

		if (!decompiler.getId().equals("enigma:vineflower")) {
			assertThat(
					checker.getReferenceTokens(new EntryReference<>(
						TestEntryFactory.newClass("a$abstract"),
						"",
						TestEntryFactory.newMethod("a", "class", "()V")
					)),
					hasItem(decompiler.getId().equals("enigma:cfr") ? "Abstract" : "abstract")
			);
		}
	}

	@ParameterizedTest
	@MethodSource("provideDecompilers")
	public void testVarargsDecompile(DecompilerService decompiler) {
		TokenChecker checker = this.getTokenChecker(decompiler);
		MethodEntry method = TestEntryFactory.newMethod("b", "a", "()V");
		assertThat(checker.getReferenceTokens(
				new EntryReference<>(TestEntryFactory.newMethod("org/quiltmc/enigma/input/Keep", "a", "([Ljava/lang/String;)V"), "", method)
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
