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

import cuchaz.enigma.translation.representation.TypeDescriptor;
import org.junit.jupiter.api.Test;

import static cuchaz.enigma.TestEntryFactory.newClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTypeDescriptor {

	@Test
	public void isVoid() {
		assertTrue(new TypeDescriptor("V").isVoid());
		assertFalse(new TypeDescriptor("Z").isVoid());
		assertFalse(new TypeDescriptor("B").isVoid());
		assertFalse(new TypeDescriptor("C").isVoid());
		assertFalse(new TypeDescriptor("I").isVoid());
		assertFalse(new TypeDescriptor("J").isVoid());
		assertFalse(new TypeDescriptor("F").isVoid());
		assertFalse(new TypeDescriptor("D").isVoid());
		assertFalse(new TypeDescriptor("LFoo;").isVoid());
		assertFalse(new TypeDescriptor("[I").isVoid());
	}

	@Test
	public void isPrimitive() {
		assertFalse(new TypeDescriptor("V").isPrimitive());
		assertTrue(new TypeDescriptor("Z").isPrimitive());
		assertTrue(new TypeDescriptor("B").isPrimitive());
		assertTrue(new TypeDescriptor("C").isPrimitive());
		assertTrue(new TypeDescriptor("I").isPrimitive());
		assertTrue(new TypeDescriptor("J").isPrimitive());
		assertTrue(new TypeDescriptor("F").isPrimitive());
		assertTrue(new TypeDescriptor("D").isPrimitive());
		assertFalse(new TypeDescriptor("LFoo;").isPrimitive());
		assertFalse(new TypeDescriptor("[I").isPrimitive());
	}

	@Test
	public void getPrimitive() {
		assertEquals(TypeDescriptor.Primitive.BOOLEAN, new TypeDescriptor("Z").getPrimitive());
		assertEquals(TypeDescriptor.Primitive.BYTE, new TypeDescriptor("B").getPrimitive());
		assertEquals(TypeDescriptor.Primitive.CHARACTER, new TypeDescriptor("C").getPrimitive());
		assertEquals(TypeDescriptor.Primitive.INTEGER, new TypeDescriptor("I").getPrimitive());
		assertEquals(TypeDescriptor.Primitive.LONG, new TypeDescriptor("J").getPrimitive());
		assertEquals(TypeDescriptor.Primitive.FLOAT, new TypeDescriptor("F").getPrimitive());
		assertEquals(TypeDescriptor.Primitive.DOUBLE, new TypeDescriptor("D").getPrimitive());
	}

	@Test
	public void isClass() {
		assertFalse(new TypeDescriptor("V").isType());
		assertFalse(new TypeDescriptor("Z").isType());
		assertFalse(new TypeDescriptor("B").isType());
		assertFalse(new TypeDescriptor("C").isType());
		assertFalse(new TypeDescriptor("I").isType());
		assertFalse(new TypeDescriptor("J").isType());
		assertFalse(new TypeDescriptor("F").isType());
		assertFalse(new TypeDescriptor("D").isType());
		assertTrue(new TypeDescriptor("LFoo;").isType());
		assertFalse(new TypeDescriptor("[I").isType());
	}

	@Test
	public void getClassEntry() {
		assertThat(new TypeDescriptor("LFoo;").getTypeEntry(), is(newClass("Foo")));
		assertThat(new TypeDescriptor("Ljava/lang/String;").getTypeEntry(), is(newClass("java/lang/String")));
	}

	@Test
	public void getArrayClassEntry() {
		assertThat(new TypeDescriptor("[LFoo;").getTypeEntry(), is(newClass("Foo")));
		assertThat(new TypeDescriptor("[[[Ljava/lang/String;").getTypeEntry(), is(newClass("java/lang/String")));
	}

	@Test
	public void isArray() {
		assertFalse(new TypeDescriptor("V").isArray());
		assertFalse(new TypeDescriptor("Z").isArray());
		assertFalse(new TypeDescriptor("B").isArray());
		assertFalse(new TypeDescriptor("C").isArray());
		assertFalse(new TypeDescriptor("I").isArray());
		assertFalse(new TypeDescriptor("J").isArray());
		assertFalse(new TypeDescriptor("F").isArray());
		assertFalse(new TypeDescriptor("D").isArray());
		assertFalse(new TypeDescriptor("LFoo;").isArray());
		assertTrue(new TypeDescriptor("[I").isArray());
	}

	@Test
	public void getArrayDimension() {
		assertEquals(1, new TypeDescriptor("[I").getArrayDimension());
		assertEquals(2, new TypeDescriptor("[[I").getArrayDimension());
		assertEquals(3, new TypeDescriptor("[[[I").getArrayDimension());
	}

	@Test
	public void getArrayType() {
		assertThat(new TypeDescriptor("[I").getArrayType(), is(new TypeDescriptor("I")));
		assertThat(new TypeDescriptor("[[I").getArrayType(), is(new TypeDescriptor("I")));
		assertThat(new TypeDescriptor("[[[I").getArrayType(), is(new TypeDescriptor("I")));
		assertThat(new TypeDescriptor("[Ljava/lang/String;").getArrayType(), is(new TypeDescriptor("Ljava/lang/String;")));
	}

	@Test
	public void hasClass() {
		assertTrue(new TypeDescriptor("LFoo;").containsType());
		assertTrue(new TypeDescriptor("Ljava/lang/String;").containsType());
		assertTrue(new TypeDescriptor("[LBar;").containsType());
		assertTrue(new TypeDescriptor("[[[LCat;").containsType());

		assertFalse(new TypeDescriptor("V").containsType());
		assertFalse(new TypeDescriptor("[I").containsType());
		assertFalse(new TypeDescriptor("[[[I").containsType());
		assertFalse(new TypeDescriptor("Z").containsType());
	}

	@Test
	public void parseVoid() {
		final String answer = "V";
		assertEquals(answer, TypeDescriptor.parseFirst("V"));
		assertEquals(answer, TypeDescriptor.parseFirst("VVV"));
		assertEquals(answer, TypeDescriptor.parseFirst("VIJ"));
		assertEquals(answer, TypeDescriptor.parseFirst("V[I"));
		assertEquals(answer, TypeDescriptor.parseFirst("VLFoo;"));
		assertEquals(answer, TypeDescriptor.parseFirst("V[LFoo;"));
	}

	@Test
	public void parsePrimitive() {
		final String answer = "I";
		assertEquals(answer, TypeDescriptor.parseFirst("I"));
		assertEquals(answer, TypeDescriptor.parseFirst("III"));
		assertEquals(answer, TypeDescriptor.parseFirst("IJZ"));
		assertEquals(answer, TypeDescriptor.parseFirst("I[I"));
		assertEquals(answer, TypeDescriptor.parseFirst("ILFoo;"));
		assertEquals(answer, TypeDescriptor.parseFirst("I[LFoo;"));
	}

	@Test
	public void parseClass() {
		{
			final String answer = "LFoo;";
			assertEquals(answer, TypeDescriptor.parseFirst("LFoo;"));
			assertEquals(answer, TypeDescriptor.parseFirst("LFoo;I"));
			assertEquals(answer, TypeDescriptor.parseFirst("LFoo;JZ"));
			assertEquals(answer, TypeDescriptor.parseFirst("LFoo;[I"));
			assertEquals(answer, TypeDescriptor.parseFirst("LFoo;LFoo;"));
			assertEquals(answer, TypeDescriptor.parseFirst("LFoo;[LFoo;"));
		}
		{
			final String answer = "Ljava/lang/String;";
			assertEquals(answer, TypeDescriptor.parseFirst("Ljava/lang/String;"));
			assertEquals(answer, TypeDescriptor.parseFirst("Ljava/lang/String;I"));
			assertEquals(answer, TypeDescriptor.parseFirst("Ljava/lang/String;JZ"));
			assertEquals(answer, TypeDescriptor.parseFirst("Ljava/lang/String;[I"));
			assertEquals(answer, TypeDescriptor.parseFirst("Ljava/lang/String;LFoo;"));
			assertEquals(answer, TypeDescriptor.parseFirst("Ljava/lang/String;[LFoo;"));
		}
	}

	@Test
	public void parseArray() {
		{
			final String answer = "[I";
			assertEquals(answer, TypeDescriptor.parseFirst("[I"));
			assertEquals(answer, TypeDescriptor.parseFirst("[III"));
			assertEquals(answer, TypeDescriptor.parseFirst("[IJZ"));
			assertEquals(answer, TypeDescriptor.parseFirst("[I[I"));
			assertEquals(answer, TypeDescriptor.parseFirst("[ILFoo;"));
		}
		{
			final String answer = "[[I";
			assertEquals(answer, TypeDescriptor.parseFirst("[[I"));
			assertEquals(answer, TypeDescriptor.parseFirst("[[III"));
			assertEquals(answer, TypeDescriptor.parseFirst("[[IJZ"));
			assertEquals(answer, TypeDescriptor.parseFirst("[[I[I"));
			assertEquals(answer, TypeDescriptor.parseFirst("[[ILFoo;"));
		}
		{
			final String answer = "[LFoo;";
			assertEquals(answer, TypeDescriptor.parseFirst("[LFoo;"));
			assertEquals(answer, TypeDescriptor.parseFirst("[LFoo;II"));
			assertEquals(answer, TypeDescriptor.parseFirst("[LFoo;JZ"));
			assertEquals(answer, TypeDescriptor.parseFirst("[LFoo;[I"));
			assertEquals(answer, TypeDescriptor.parseFirst("[LFoo;LFoo;"));
		}
	}

	@Test
	public void equals() {
		assertThat(new TypeDescriptor("V"), is(new TypeDescriptor("V")));
		assertThat(new TypeDescriptor("Z"), is(new TypeDescriptor("Z")));
		assertThat(new TypeDescriptor("B"), is(new TypeDescriptor("B")));
		assertThat(new TypeDescriptor("C"), is(new TypeDescriptor("C")));
		assertThat(new TypeDescriptor("I"), is(new TypeDescriptor("I")));
		assertThat(new TypeDescriptor("J"), is(new TypeDescriptor("J")));
		assertThat(new TypeDescriptor("F"), is(new TypeDescriptor("F")));
		assertThat(new TypeDescriptor("D"), is(new TypeDescriptor("D")));
		assertThat(new TypeDescriptor("LFoo;"), is(new TypeDescriptor("LFoo;")));
		assertThat(new TypeDescriptor("[I"), is(new TypeDescriptor("[I")));
		assertThat(new TypeDescriptor("[[[I"), is(new TypeDescriptor("[[[I")));
		assertThat(new TypeDescriptor("[LFoo;"), is(new TypeDescriptor("[LFoo;")));

		assertThat(new TypeDescriptor("V"), is(not(new TypeDescriptor("I"))));
		assertThat(new TypeDescriptor("I"), is(not(new TypeDescriptor("J"))));
		assertThat(new TypeDescriptor("I"), is(not(new TypeDescriptor("LBar;"))));
		assertThat(new TypeDescriptor("I"), is(not(new TypeDescriptor("[I"))));
		assertThat(new TypeDescriptor("LFoo;"), is(not(new TypeDescriptor("LBar;"))));
		assertThat(new TypeDescriptor("[I"), is(not(new TypeDescriptor("[Z"))));
		assertThat(new TypeDescriptor("[[[I"), is(not(new TypeDescriptor("[I"))));
		assertThat(new TypeDescriptor("[LFoo;"), is(not(new TypeDescriptor("[LBar;"))));
	}

	@Test
	public void testToString() {
		assertEquals("V", new TypeDescriptor("V").toString());
		assertEquals("Z", new TypeDescriptor("Z").toString());
		assertEquals("B", new TypeDescriptor("B").toString());
		assertEquals("C", new TypeDescriptor("C").toString());
		assertEquals("I", new TypeDescriptor("I").toString());
		assertEquals("J", new TypeDescriptor("J").toString());
		assertEquals("F", new TypeDescriptor("F").toString());
		assertEquals("D", new TypeDescriptor("D").toString());
		assertEquals("LFoo;", new TypeDescriptor("LFoo;").toString());
		assertEquals("[I", new TypeDescriptor("[I").toString());
		assertEquals("[[[I", new TypeDescriptor("[[[I").toString());
		assertEquals("[LFoo;", new TypeDescriptor("[LFoo;").toString());
	}
}
