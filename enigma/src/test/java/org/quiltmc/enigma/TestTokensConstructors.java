package org.quiltmc.enigma;

import org.quiltmc.enigma.source.Decompilers;
import org.quiltmc.enigma.translation.representation.entry.MethodEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.quiltmc.enigma.TestEntryFactory.newMethod;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestTokensConstructors extends TokenChecker {
	private static final Path JAR = TestUtil.obfJar("constructors");

	public TestTokensConstructors() throws Exception {
		super(JAR, Decompilers.PROCYON); // Procyon is the only one that indexes constructor invocations
	}

	@Test
	public void baseDeclarations() {
		assertThat(getDeclarationToken(TestEntryFactory.newMethod("a", "<init>", "()V")), is("a"));
		assertThat(getDeclarationToken(TestEntryFactory.newMethod("a", "<init>", "(I)V")), is("a"));
	}

	@Test
	public void subDeclarations() {
		assertThat(getDeclarationToken(TestEntryFactory.newMethod("d", "<init>", "()V")), is("d"));
		assertThat(getDeclarationToken(TestEntryFactory.newMethod("d", "<init>", "(I)V")), is("d"));
		assertThat(getDeclarationToken(TestEntryFactory.newMethod("d", "<init>", "(II)V")), is("d"));
		assertThat(getDeclarationToken(TestEntryFactory.newMethod("d", "<init>", "(III)V")), is("d"));
	}

	@Test
	public void subsubDeclarations() {
		assertThat(getDeclarationToken(TestEntryFactory.newMethod("e", "<init>", "(I)V")), is("e"));
	}

	@Test
	public void defaultDeclarations() {
		assertThat(getDeclarationToken(TestEntryFactory.newMethod("c", "<init>", "()V")), nullValue());
	}

	@Test
	public void baseDefaultReferences() {
		MethodEntry source = TestEntryFactory.newMethod("a", "<init>", "()V");
		assertThat(
				getReferenceTokens(TestEntryFactory.newBehaviorReferenceByMethod(source, "b", "a", "()V")),
				containsInAnyOrder("a")
		);
		assertThat(
				getReferenceTokens(TestEntryFactory.newBehaviorReferenceByMethod(source, "d", "<init>", "()V")),
				is(empty()) // implicit call, not decompiled to token
		);
		assertThat(
				getReferenceTokens(TestEntryFactory.newBehaviorReferenceByMethod(source, "d", "<init>", "(III)V")),
				is(empty()) // implicit call, not decompiled to token
		);
	}

	@Test
	public void baseIntReferences() {
		MethodEntry source = TestEntryFactory.newMethod("a", "<init>", "(I)V");
		assertThat(
				getReferenceTokens(TestEntryFactory.newBehaviorReferenceByMethod(source, "b", "b", "()V")),
				containsInAnyOrder("a")
		);
	}

	@Test
	public void subDefaultReferences() {
		MethodEntry source = TestEntryFactory.newMethod("d", "<init>", "()V");
		assertThat(
				getReferenceTokens(TestEntryFactory.newBehaviorReferenceByMethod(source, "b", "c", "()V")),
				containsInAnyOrder("d")
		);
		assertThat(
				getReferenceTokens(TestEntryFactory.newBehaviorReferenceByMethod(source, "d", "<init>", "(I)V")),
				containsInAnyOrder("this")
		);
	}

	@Test
	public void subIntReferences() {
		MethodEntry source = TestEntryFactory.newMethod("d", "<init>", "(I)V");
		assertThat(getReferenceTokens(
				TestEntryFactory.newBehaviorReferenceByMethod(source, "b", "d", "()V")),
				containsInAnyOrder("d")
		);
		assertThat(getReferenceTokens(
				TestEntryFactory.newBehaviorReferenceByMethod(source, "d", "<init>", "(II)V")),
				containsInAnyOrder("this")
		);
		assertThat(getReferenceTokens(
				TestEntryFactory.newBehaviorReferenceByMethod(source, "e", "<init>", "(I)V")),
				containsInAnyOrder("super")
		);
	}

	@Test
	public void subIntIntReferences() {
		MethodEntry source = TestEntryFactory.newMethod("d", "<init>", "(II)V");
		assertThat(
				getReferenceTokens(TestEntryFactory.newBehaviorReferenceByMethod(source, "b", "e", "()V")),
				containsInAnyOrder("d")
		);
	}

	@Test
	public void subsubIntReferences() {
		MethodEntry source = TestEntryFactory.newMethod("e", "<init>", "(I)V");
		assertThat(
				getReferenceTokens(TestEntryFactory.newBehaviorReferenceByMethod(source, "b", "f", "()V")),
				containsInAnyOrder("e")
		);
	}

	@Test
	public void defaultConstructableReferences() {
		MethodEntry source = TestEntryFactory.newMethod("c", "<init>", "()V");
		assertThat(
				getReferenceTokens(TestEntryFactory.newBehaviorReferenceByMethod(source, "b", "g", "()V")),
				containsInAnyOrder("c")
		);
	}
}
