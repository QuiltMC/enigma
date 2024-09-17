package org.quiltmc.enigma;

import org.quiltmc.enigma.api.analysis.index.jar.BridgeMethodIndex;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.index.jar.MainJarIndex;
import org.quiltmc.enigma.api.class_provider.CachingClassProvider;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.class_provider.ObfuscationFixClassProvider;
import org.quiltmc.enigma.api.service.DecompilerService;
import org.quiltmc.enigma.api.source.Decompilers;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Tests to reproduce <a href="https://github.com/QuiltMC/enigma/issues/9">issue #9</a>
 */
public class TestJarIndexBridgeMethods {
	public static final Path JAR = TestUtil.obfJar("bridge");

	private final CachingClassProvider classProvider;
	private final JarIndex index;
	private final Map<DecompilerService, TokenChecker> tokenCheckers = new HashMap<>();

	private final ClassEntry baseClass = TestEntryFactory.newClass("a");
	private final ClassEntry otherClass = TestEntryFactory.newClass("b");
	private final ClassEntry subClass = TestEntryFactory.newClass("c");
	private final ClassEntry innerSubClass = TestEntryFactory.newClass("c$a");

	public TestJarIndexBridgeMethods() throws Exception {
		JarClassProvider jcp = new JarClassProvider(JAR);
		this.classProvider = new CachingClassProvider(jcp);
		this.index = MainJarIndex.empty();
		this.index.indexJar(jcp.getClassNames(), this.classProvider, ProgressListener.createEmpty());
	}

	@Test
	public void obfEntries() {
		assertThat(this.index.getIndex(EntryIndex.class).getClasses(), Matchers.containsInAnyOrder(TestEntryFactory.newClass("org/quiltmc/enigma/input/Keep"), this.baseClass,
				this.otherClass, this.subClass, this.innerSubClass));
	}

	@Test
	public void testBase() {
		BridgeMethodIndex index = this.index.getIndex(BridgeMethodIndex.class);

		assertThat(index.isBridgeMethod(TestEntryFactory.newMethod(this.baseClass, "a", "()I")), is(false));
		assertThat(index.getBridgeFromSpecialized(TestEntryFactory.newMethod(this.baseClass, "a", "()La;")), nullValue());
		assertThat(index.isSpecializedMethod(TestEntryFactory.newMethod(this.baseClass, "b", "(II)La;")), is(false));
		assertThat(index.getSpecializedFromBridge(TestEntryFactory.newMethod(this.baseClass, "c", "(I)La;")), nullValue());
	}

	@Test
	public void testSub() {
		BridgeMethodIndex index = this.index.getIndex(BridgeMethodIndex.class);

		assertThat(index.isBridgeMethod(TestEntryFactory.newMethod(this.subClass, "f", "()Lc;")), is(false));
		assertThat(index.isBridgeMethod(TestEntryFactory.newMethod(this.subClass, "d", "()La;")), is(true));
		assertThat(index.getSpecializedFromBridge(TestEntryFactory.newMethod(this.subClass, "b", "(II)La;")),
				Matchers.is(TestEntryFactory.newMethod(this.subClass, "d", "(II)Lc;")));
		assertThat(index.isSpecializedMethod(TestEntryFactory.newMethod(this.subClass, "f", "(I)Lc;")), is(true));
		assertThat(index.isSpecializedMethod(TestEntryFactory.newMethod(this.subClass, "c", "(I)La;")), is(false));
		assertThat(index.getBridgeFromSpecialized(TestEntryFactory.newMethod(this.subClass, "e", "(I)Lc;")),
				Matchers.is(TestEntryFactory.newMethod(this.subClass, "b", "(I)La;")));
		assertThat(index.getBridgeFromSpecialized(TestEntryFactory.newMethod(this.subClass, "g", "()Lc;")),
				Matchers.is(TestEntryFactory.newMethod(this.subClass, "e", "()La;")));
	}

	@Test
	public void testInnerSub() {
		BridgeMethodIndex index = this.index.getIndex(BridgeMethodIndex.class);

		assertThat(index.isBridgeMethod(TestEntryFactory.newMethod(this.innerSubClass, "d", "()La;")), is(true));
		assertThat(index.getSpecializedFromBridge(TestEntryFactory.newMethod(this.innerSubClass, "a", "(I)La;")),
				Matchers.is(TestEntryFactory.newMethod(this.subClass, "d", "(I)Lc;")));
		assertThat(index.getSpecializedFromBridge(TestEntryFactory.newMethod(this.innerSubClass, "e", "()La;")),
				Matchers.is(TestEntryFactory.newMethod(this.subClass, "g", "()Lc;")));
		assertThat(index.isSpecializedMethod(TestEntryFactory.newMethod(this.innerSubClass, "b", "(I)La;")), is(false));
		assertThat(index.getBridgeFromSpecialized(TestEntryFactory.newMethod(this.innerSubClass, "c", "(I)La;")), nullValue());
		assertThat(index.getBridgeFromSpecialized(TestEntryFactory.newMethod(this.innerSubClass, "b", "(II)La;")), nullValue());
	}

	@Test
	public void testOther() {
		BridgeMethodIndex index = this.index.getIndex(BridgeMethodIndex.class);

		assertThat(index.getBridgeFromSpecialized(TestEntryFactory.newMethod(this.otherClass, "a", "()Ljava/lang/Integer;")),
				Matchers.is(TestEntryFactory.newMethod(this.otherClass, "get", "()Ljava/lang/Object;")));
		assertThat(index.getSpecializedFromBridge(TestEntryFactory.newMethod(this.otherClass, "a", "(Ljava/lang/String;)Ljava/lang/Integer;")), nullValue());
		assertThat(index.getBridgeFromSpecialized(TestEntryFactory.newMethod(this.otherClass, "get", "()Ljava/lang/Object;")), nullValue());
		assertThat(index.getSpecializedFromBridge(TestEntryFactory.newMethod(this.otherClass, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;")),
				Matchers.is(TestEntryFactory.newMethod(this.otherClass, "a", "(Ljava/lang/String;)Ljava/lang/Integer;")));
	}

	@Test
	public void testTokensBase() {
		this.testTokensBase(Decompilers.VINEFLOWER);
		this.testTokensBase(Decompilers.CFR);
		this.testTokensBase(Decompilers.PROCYON);
	}

	private void testTokensBase(DecompilerService decompiler) {
		TokenChecker checker = this.getTokenChecker(decompiler);
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.baseClass, "d", "()La;")), is("d"));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.baseClass, "a", "(I)La;")), is("a"));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.baseClass, "b", "(II)La;")), is("b"));
	}

	@Test
	public void testTokensSub() {
		this.testTokensSub(Decompilers.VINEFLOWER);
		this.testTokensSub(Decompilers.CFR);
		this.testTokensSub(Decompilers.PROCYON);
	}

	private void testTokensSub(DecompilerService decompiler) {
		TokenChecker checker = this.getTokenChecker(decompiler);
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.subClass, "d", "()La;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.subClass, "f", "()Lc;")), is("f"));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.subClass, "a", "(I)La;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.subClass, "d", "(I)Lc;")), is("d"));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.subClass, "b", "(II)La;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.subClass, "d", "(II)Lc;")), is("d"));
	}

	@Test
	public void testTokensInnerSub() {
		this.testTokensInnerSub(Decompilers.VINEFLOWER);
		this.testTokensInnerSub(Decompilers.CFR);
		this.testTokensInnerSub(Decompilers.PROCYON);
	}

	private void testTokensInnerSub(DecompilerService decompiler) {
		TokenChecker checker = this.getTokenChecker(decompiler);
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.innerSubClass, "d", "()La;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.innerSubClass, "f", "()Lc;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.innerSubClass, "a", "(I)La;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.innerSubClass, "d", "(I)Lc;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.innerSubClass, "b", "(II)La;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.innerSubClass, "d", "(II)Lc;")), is(emptyOrNullString()));
	}

	@Test
	public void testTokensOther() {
		this.testTokensOther(Decompilers.VINEFLOWER);
		this.testTokensOther(Decompilers.CFR);
		this.testTokensOther(Decompilers.PROCYON);
	}

	private void testTokensOther(DecompilerService decompiler) {
		TokenChecker checker = this.getTokenChecker(decompiler);
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.otherClass, "a", "()Ljava/lang/Integer;")), is("a"));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.otherClass, "get", "()Ljava/lang/Object;")), is(emptyOrNullString()));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.otherClass, "a", "(Ljava/lang/String;)Ljava/lang/Integer;")), is("a"));
		assertThat(checker.getDeclarationToken(TestEntryFactory.newMethod(this.otherClass, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;")), is(emptyOrNullString()));
	}

	private TokenChecker getTokenChecker(DecompilerService decompiler) {
		return this.tokenCheckers.computeIfAbsent(decompiler, d ->
				new TokenChecker(JAR, d, new ObfuscationFixClassProvider(this.classProvider, this.index)));
	}
}
