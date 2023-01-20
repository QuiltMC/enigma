/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *	 Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma;

import cuchaz.enigma.source.Decompilers;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static cuchaz.enigma.TestEntryFactory.newBehaviorReferenceByMethod;
import static cuchaz.enigma.TestEntryFactory.newMethod;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class TestTokensConstructors extends TokenChecker {
	public TestTokensConstructors()
			throws Exception {
		super(Paths.get("build/test-obf/constructors.jar"),
				Decompilers.PROCYON); // Procyon is the only one that indexes constructor invocations
	}

	@Test
	void baseDeclarations() {
		assertThat(this.getDeclarationToken(newMethod("a", "<init>", "()V")), is("a"));
		assertThat(this.getDeclarationToken(newMethod("a", "<init>", "(I)V")), is("a"));
	}

	@Test
	void subDeclarations() {
		assertThat(this.getDeclarationToken(newMethod("d", "<init>", "()V")), is("d"));
		assertThat(this.getDeclarationToken(newMethod("d", "<init>", "(I)V")), is("d"));
		assertThat(this.getDeclarationToken(newMethod("d", "<init>", "(II)V")), is("d"));
		assertThat(this.getDeclarationToken(newMethod("d", "<init>", "(III)V")), is("d"));
	}

	@Test
	void subsubDeclarations() {
		assertThat(this.getDeclarationToken(newMethod("e", "<init>", "(I)V")), is("e"));
	}

	@Test
	void defaultDeclarations() {
		assertThat(this.getDeclarationToken(newMethod("c", "<init>", "()V")), nullValue());
	}

	@Test
	void baseDefaultReferences() {
		MethodEntry source = newMethod("a", "<init>", "()V");
		assertThat(
				this.getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "a", "()V")),
				containsInAnyOrder("a")
		);
		assertThat(
				this.getReferenceTokens(newBehaviorReferenceByMethod(source, "d", "<init>", "()V")),
				is(empty()) // implicit call, not decompiled to token
		);
		assertThat(
				this.getReferenceTokens(newBehaviorReferenceByMethod(source, "d", "<init>", "(III)V")),
				is(empty()) // implicit call, not decompiled to token
		);
	}

	@Test
	void baseIntReferences() {
		MethodEntry source = newMethod("a", "<init>", "(I)V");
		assertThat(
				this.getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "b", "()V")),
				containsInAnyOrder("a")
		);
	}

	@Test
	void subDefaultReferences() {
		MethodEntry source = newMethod("d", "<init>", "()V");
		assertThat(
				this.getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "c", "()V")),
				containsInAnyOrder("d")
		);
		assertThat(
				this.getReferenceTokens(newBehaviorReferenceByMethod(source, "d", "<init>", "(I)V")),
				containsInAnyOrder("this")
		);
	}

	@Test
	void subIntReferences() {
		MethodEntry source = newMethod("d", "<init>", "(I)V");
		assertThat(this.getReferenceTokens(
				newBehaviorReferenceByMethod(source, "b", "d", "()V")),
				containsInAnyOrder("d")
		);
		assertThat(this.getReferenceTokens(
				newBehaviorReferenceByMethod(source, "d", "<init>", "(II)V")),
				containsInAnyOrder("this")
		);
		assertThat(this.getReferenceTokens(
				newBehaviorReferenceByMethod(source, "e", "<init>", "(I)V")),
				containsInAnyOrder("super")
		);
	}

	@Test
	void subIntIntReferences() {
		MethodEntry source = newMethod("d", "<init>", "(II)V");
		assertThat(
				this.getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "e", "()V")),
				containsInAnyOrder("d")
		);
	}

	@Test
	void subsubIntReferences() {
		MethodEntry source = newMethod("e", "<init>", "(I)V");
		assertThat(
				this.getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "f", "()V")),
				containsInAnyOrder("e")
		);
	}

	@Test
	void defaultConstructableReferences() {
		MethodEntry source = newMethod("c", "<init>", "()V");
		assertThat(
				this.getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "g", "()V")),
				containsInAnyOrder("c")
		);
	}
}
