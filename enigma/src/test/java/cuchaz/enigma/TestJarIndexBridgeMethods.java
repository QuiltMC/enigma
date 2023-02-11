package cuchaz.enigma;

import cuchaz.enigma.analysis.index.BridgeMethodIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.classprovider.CachingClassProvider;
import cuchaz.enigma.classprovider.JarClassProvider;
import cuchaz.enigma.classprovider.ObfuscationFixClassProvider;
import cuchaz.enigma.source.DecompilerService;
import cuchaz.enigma.source.Decompilers;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static cuchaz.enigma.TestEntryFactory.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Tests to reproduce <a href="https://github.com/QuiltMC/enigma/issues/9">issue #9</a>
 */
public class TestJarIndexBridgeMethods {
	public static final Path JAR = Path.of("build/test-obf/bridge.jar");

	private final CachingClassProvider classProvider;
	private final JarIndex index;
	private final Map<DecompilerService, TokenChecker> tokenCheckers = new HashMap<>();

	private final ClassEntry baseClass = newClass("a");
	private final ClassEntry otherClass = newClass("b");
	private final ClassEntry subClass = newClass("c");
	private final ClassEntry innerSubClass = newClass("c$a");

	public TestJarIndexBridgeMethods() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
		classProvider = new CachingClassProvider(jcp);
		index = JarIndex.empty();
		index.indexJar(jcp.getClassNames(), classProvider, ProgressListener.none());
	}

	@Test
	public void obfEntries() {
		assertThat(index.getEntryIndex().getClasses(), containsInAnyOrder(newClass("cuchaz/enigma/inputs/Keep"), baseClass,
				otherClass, subClass, innerSubClass));
	}

	@Test
	public void testBase() {
		BridgeMethodIndex index = this.index.getBridgeMethodIndex();

		assertThat(index.isBridgeMethod(newMethod(baseClass, "a", "()I")), is(false));
		assertThat(index.getBridgeFromSpecialized(newMethod(baseClass, "a", "()La;")), nullValue());
		assertThat(index.isSpecializedMethod(newMethod(baseClass, "b", "(II)La;")), is(false));
		assertThat(index.getSpecializedFromBridge(newMethod(baseClass, "c", "(I)La;")), nullValue());
	}

	@Test
	public void testSub() {
		BridgeMethodIndex index = this.index.getBridgeMethodIndex();

		assertThat(index.isBridgeMethod(newMethod(subClass, "f", "()Lc;")), is(false));
		assertThat(index.isBridgeMethod(newMethod(subClass, "d", "()La;")), is(true));
		assertThat(index.getSpecializedFromBridge(newMethod(subClass, "b", "(II)La;")),
				is(newMethod(subClass, "d", "(II)Lc;")));
		assertThat(index.isSpecializedMethod(newMethod(subClass, "f", "(I)Lc;")), is(true));
		assertThat(index.isSpecializedMethod(newMethod(subClass, "c", "(I)La;")), is(false));
		assertThat(index.getBridgeFromSpecialized(newMethod(subClass, "e", "(I)Lc;")),
				is(newMethod(subClass, "b", "(I)La;")));
		assertThat(index.getBridgeFromSpecialized(newMethod(subClass, "g", "()Lc;")),
				is(newMethod(subClass, "e", "()La;")));
	}

	@Test
	public void testInnerSub() {
		BridgeMethodIndex index = this.index.getBridgeMethodIndex();

		assertThat(index.isBridgeMethod(newMethod(innerSubClass, "d", "()La;")), is(true));
		assertThat(index.getSpecializedFromBridge(newMethod(innerSubClass, "a", "(I)La;")),
				is(newMethod(subClass, "d", "(I)Lc;")));
		assertThat(index.getSpecializedFromBridge(newMethod(innerSubClass, "e", "()La;")),
				is(newMethod(subClass, "g", "()Lc;")));
		assertThat(index.isSpecializedMethod(newMethod(innerSubClass, "b", "(I)La;")), is(false));
		assertThat(index.getBridgeFromSpecialized(newMethod(innerSubClass, "c", "(I)La;")), nullValue());
		assertThat(index.getBridgeFromSpecialized(newMethod(innerSubClass, "b", "(II)La;")), nullValue());
	}

	@Test
	public void testOther() {
		BridgeMethodIndex index = this.index.getBridgeMethodIndex();

		assertThat(index.getBridgeFromSpecialized(newMethod(otherClass, "a", "()Ljava/lang/Integer;")),
				is(newMethod(otherClass, "get", "()Ljava/lang/Object;")));
		assertThat(index.getSpecializedFromBridge(newMethod(otherClass, "a", "(Ljava/lang/String;)Ljava/lang/Integer;")), nullValue());
		assertThat(index.getBridgeFromSpecialized(newMethod(otherClass, "get", "()Ljava/lang/Object;")), nullValue());
		assertThat(index.getSpecializedFromBridge(newMethod(otherClass, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;")),
				is(newMethod(otherClass, "a", "(Ljava/lang/String;)Ljava/lang/Integer;")));
	}

	@Test
	public void testTokensBase() {
		testTokensBase(Decompilers.QUILTFLOWER);
		testTokensBase(Decompilers.CFR);
		testTokensBase(Decompilers.PROCYON);
	}

	private void testTokensBase(DecompilerService decompiler) {
		TokenChecker checker = getTokenChecker(decompiler);
		assertThat(checker.getDeclarationToken(newMethod(baseClass, "d", "()La;")), is("d"));
		assertThat(checker.getDeclarationToken(newMethod(baseClass, "a", "(I)La;")), is("a"));
		assertThat(checker.getDeclarationToken(newMethod(baseClass, "b", "(II)La;")), is("b"));
	}

	@Test
	public void testTokensSub() {
		testTokensSub(Decompilers.QUILTFLOWER);
		testTokensSub(Decompilers.CFR);
		testTokensSub(Decompilers.PROCYON);
	}

	private void testTokensSub(DecompilerService decompiler) {
		TokenChecker checker = getTokenChecker(decompiler);
		assertThat(checker.getDeclarationToken(newMethod(subClass, "d", "()La;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(newMethod(subClass, "f", "()Lc;")), is("f"));
		assertThat(checker.getDeclarationToken(newMethod(subClass, "a", "(I)La;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(newMethod(subClass, "d", "(I)Lc;")), is("d"));
		assertThat(checker.getDeclarationToken(newMethod(subClass, "b", "(II)La;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(newMethod(subClass, "d", "(II)Lc;")), is("d"));
	}

	@Test
	public void testTokensInnerSub() {
		testTokensInnerSub(Decompilers.QUILTFLOWER);
		testTokensInnerSub(Decompilers.CFR);
		testTokensInnerSub(Decompilers.PROCYON);
	}

	private void testTokensInnerSub(DecompilerService decompiler) {
		TokenChecker checker = getTokenChecker(decompiler);
		assertThat(checker.getDeclarationToken(newMethod(innerSubClass, "d", "()La;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(newMethod(innerSubClass, "f", "()Lc;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(newMethod(innerSubClass, "a", "(I)La;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(newMethod(innerSubClass, "d", "(I)Lc;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(newMethod(innerSubClass, "b", "(II)La;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(newMethod(innerSubClass, "d", "(II)Lc;")), is(emptyOrNullString()));
	}

	@Test
	public void testTokensOther() {
		testTokensOther(Decompilers.QUILTFLOWER);
		testTokensOther(Decompilers.CFR);
		testTokensOther(Decompilers.PROCYON);
	}

	private void testTokensOther(DecompilerService decompiler) {
		TokenChecker checker = getTokenChecker(decompiler);
		assertThat(checker.getDeclarationToken(newMethod(otherClass, "a", "()Ljava/lang/Integer;")), is("a"));
		assertThat(checker.getDeclarationToken(newMethod(otherClass, "get", "()Ljava/lang/Object;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(newMethod(otherClass, "a", "(Ljava/lang/String;)Ljava/lang/Integer;")), is("a"));
		assertThat(checker.getDeclarationToken(newMethod(otherClass, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;")), is(emptyOrNullString()));
	}

	private TokenChecker getTokenChecker(DecompilerService decompiler) {
		return tokenCheckers.computeIfAbsent(decompiler, d ->
				new TokenChecker(JAR, d, new ObfuscationFixClassProvider(classProvider, index)));
	}
}
