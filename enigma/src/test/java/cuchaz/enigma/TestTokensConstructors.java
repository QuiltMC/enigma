/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma;

import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static cuchaz.enigma.TestEntryFactory.newBehaviorReferenceByMethod;
import static cuchaz.enigma.TestEntryFactory.newMethod;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestTokensConstructors extends TokenChecker {

	public TestTokensConstructors()
			throws Exception {
		super(Paths.get("build/test-obf/constructors.jar"));
	}

	@Test
	public void baseDeclarations() {
		assertEquals("a", getDeclarationToken(newMethod("a", "<init>", "()V")));
		assertEquals("a", getDeclarationToken(newMethod("a", "<init>", "(I)V")));
	}

	@Test
	public void subDeclarations() {
		assertEquals("d", getDeclarationToken(newMethod("d", "<init>", "()V")));
		assertEquals("d", getDeclarationToken(newMethod("d", "<init>", "(I)V")));
		assertEquals("d", getDeclarationToken(newMethod("d", "<init>", "(II)V")));
		assertEquals("d", getDeclarationToken(newMethod("d", "<init>", "(III)V")));
	}

	@Test
	public void subsubDeclarations() {
		assertEquals("e", getDeclarationToken(newMethod("e", "<init>", "(I)V")));
	}

	@Test
	public void defaultDeclarations() {
		assertNull(getDeclarationToken(newMethod("c", "<init>", "()V")));
	}

	@Test
	@Disabled // TODO needs fixing, broke when compiling against J16
	public void baseDefaultReferences() {
		MethodEntry source = newMethod("a", "<init>", "()V");
		assertThat(
				getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "a", "()V")),
				containsInAnyOrder("a")
		);
		assertTrue(getReferenceTokens(newBehaviorReferenceByMethod(source, "d", "<init>", "()V")).isEmpty());
		assertTrue(getReferenceTokens(newBehaviorReferenceByMethod(source, "d", "<init>", "(III)V")).isEmpty());
	}

	@Test
	@Disabled // TODO needs fixing, broke when compiling against J16
	public void baseIntReferences() {
		MethodEntry source = newMethod("a", "<init>", "(I)V");
		assertThat(
				getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "b", "()V")),
				containsInAnyOrder("a")
		);
	}

	@Test
	@Disabled // TODO needs fixing, broke when compiling against J16
	public void subDefaultReferences() {
		MethodEntry source = newMethod("d", "<init>", "()V");
		assertThat(
				getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "c", "()V")),
				containsInAnyOrder("d")
		);
		assertThat(
				getReferenceTokens(newBehaviorReferenceByMethod(source, "d", "<init>", "(I)V")),
				containsInAnyOrder("this")
		);
	}

	@Test
	@Disabled // TODO needs fixing, broke when compiling against J16
	public void subIntReferences() {
		MethodEntry source = newMethod("d", "<init>", "(I)V");
		assertThat(getReferenceTokens(
				newBehaviorReferenceByMethod(source, "b", "d", "()V")),
				containsInAnyOrder("d")
		);
		assertThat(getReferenceTokens(
				newBehaviorReferenceByMethod(source, "d", "<init>", "(II)V")),
				containsInAnyOrder("this")
		);
		assertThat(getReferenceTokens(
				newBehaviorReferenceByMethod(source, "e", "<init>", "(I)V")),
				containsInAnyOrder("super")
		);
	}

	@Test
	@Disabled // TODO needs fixing, broke when compiling against J16
	public void subIntIntReferences() {
		MethodEntry source = newMethod("d", "<init>", "(II)V");
		assertThat(
				getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "e", "()V")),
				containsInAnyOrder("d")
		);
	}

	@Test
	@Disabled // TODO needs fixing, broke when compiling against J16
	public void subsubIntReferences() {
		MethodEntry source = newMethod("e", "<init>", "(I)V");
		assertThat(
				getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "f", "()V")),
				containsInAnyOrder("e")
		);
	}

	@Test
	@Disabled // TODO needs fixing, broke when compiling against J16
	public void defaultConstructableReferences() {
		MethodEntry source = newMethod("c", "<init>", "()V");
		assertThat(
				getReferenceTokens(newBehaviorReferenceByMethod(source, "b", "g", "()V")),
				containsInAnyOrder("c")
		);
	}
}
