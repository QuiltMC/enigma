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

import cuchaz.enigma.translation.representation.TypeDescriptor;
import org.junit.jupiter.api.Test;

import static cuchaz.enigma.TestEntryFactory.newClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TestTypeDescriptor {
	@Test
	void isVoid() {
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
	void isPrimitive() {
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
	void getPrimitive() {
		assertThat(new TypeDescriptor("Z").getPrimitive(), is(TypeDescriptor.Primitive.BOOLEAN));
		assertThat(new TypeDescriptor("B").getPrimitive(), is(TypeDescriptor.Primitive.BYTE));
		assertThat(new TypeDescriptor("C").getPrimitive(), is(TypeDescriptor.Primitive.CHARACTER));
		assertThat(new TypeDescriptor("I").getPrimitive(), is(TypeDescriptor.Primitive.INTEGER));
		assertThat(new TypeDescriptor("J").getPrimitive(), is(TypeDescriptor.Primitive.LONG));
		assertThat(new TypeDescriptor("F").getPrimitive(), is(TypeDescriptor.Primitive.FLOAT));
		assertThat(new TypeDescriptor("D").getPrimitive(), is(TypeDescriptor.Primitive.DOUBLE));
	}

	@Test
	void isClass() {
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
	void getClassEntry() {
		assertThat(new TypeDescriptor("LFoo;").getTypeEntry(), is(newClass("Foo")));
		assertThat(new TypeDescriptor("Ljava/lang/String;").getTypeEntry(), is(newClass("java/lang/String")));
	}

	@Test
	void getArrayClassEntry() {
		assertThat(new TypeDescriptor("[LFoo;").getTypeEntry(), is(newClass("Foo")));
		assertThat(new TypeDescriptor("[[[Ljava/lang/String;").getTypeEntry(), is(newClass("java/lang/String")));
	}

	@Test
	void isArray() {
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
	void getArrayDimension() {
		assertThat(new TypeDescriptor("[I").getArrayDimension(), is(1));
		assertThat(new TypeDescriptor("[[I").getArrayDimension(), is(2));
		assertThat(new TypeDescriptor("[[[I").getArrayDimension(), is(3));
	}

	@Test
	void getArrayType() {
		assertThat(new TypeDescriptor("[I").getArrayType(), is(new TypeDescriptor("I")));
		assertThat(new TypeDescriptor("[[I").getArrayType(), is(new TypeDescriptor("I")));
		assertThat(new TypeDescriptor("[[[I").getArrayType(), is(new TypeDescriptor("I")));
		assertThat(new TypeDescriptor("[Ljava/lang/String;").getArrayType(), is(new TypeDescriptor("Ljava/lang/String;")));
	}

	@Test
	void hasClass() {
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
	void parseVoid() {
		final String answer = "V";
		assertThat(TypeDescriptor.parseFirst("V"), is(answer));
		assertThat(TypeDescriptor.parseFirst("VVV"), is(answer));
		assertThat(TypeDescriptor.parseFirst("VIJ"), is(answer));
		assertThat(TypeDescriptor.parseFirst("V[I"), is(answer));
		assertThat(TypeDescriptor.parseFirst("VLFoo;"), is(answer));
		assertThat(TypeDescriptor.parseFirst("V[LFoo;"), is(answer));
	}

	@Test
	void parsePrimitive() {
		final String answer = "I";
		assertThat(TypeDescriptor.parseFirst("I"), is(answer));
		assertThat(TypeDescriptor.parseFirst("III"), is(answer));
		assertThat(TypeDescriptor.parseFirst("IJZ"), is(answer));
		assertThat(TypeDescriptor.parseFirst("I[I"), is(answer));
		assertThat(TypeDescriptor.parseFirst("ILFoo;"), is(answer));
		assertThat(TypeDescriptor.parseFirst("I[LFoo;"), is(answer));
	}

	@Test
	void parseClass() {
		{
			final String answer = "LFoo;";
			assertThat(TypeDescriptor.parseFirst("LFoo;"), is(answer));
			assertThat(TypeDescriptor.parseFirst("LFoo;I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("LFoo;JZ"), is(answer));
			assertThat(TypeDescriptor.parseFirst("LFoo;[I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("LFoo;LFoo;"), is(answer));
			assertThat(TypeDescriptor.parseFirst("LFoo;[LFoo;"), is(answer));
		}
		{
			final String answer = "Ljava/lang/String;";
			assertThat(TypeDescriptor.parseFirst("Ljava/lang/String;"), is(answer));
			assertThat(TypeDescriptor.parseFirst("Ljava/lang/String;I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("Ljava/lang/String;JZ"), is(answer));
			assertThat(TypeDescriptor.parseFirst("Ljava/lang/String;[I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("Ljava/lang/String;LFoo;"), is(answer));
			assertThat(TypeDescriptor.parseFirst("Ljava/lang/String;[LFoo;"), is(answer));
		}
	}

	@Test
	void parseArray() {
		{
			final String answer = "[I";
			assertThat(TypeDescriptor.parseFirst("[I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[III"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[IJZ"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[I[I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[ILFoo;"), is(answer));
		}
		{
			final String answer = "[[I";
			assertThat(TypeDescriptor.parseFirst("[[I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[[III"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[[IJZ"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[[I[I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[[ILFoo;"), is(answer));
		}
		{
			final String answer = "[LFoo;";
			assertThat(TypeDescriptor.parseFirst("[LFoo;"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[LFoo;II"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[LFoo;JZ"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[LFoo;[I"), is(answer));
			assertThat(TypeDescriptor.parseFirst("[LFoo;LFoo;"), is(answer));
		}
	}

	@Test
	void equals() {
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
	void testToString() {
		assertThat(new TypeDescriptor("V").toString(), is("V"));
		assertThat(new TypeDescriptor("Z").toString(), is("Z"));
		assertThat(new TypeDescriptor("B").toString(), is("B"));
		assertThat(new TypeDescriptor("C").toString(), is("C"));
		assertThat(new TypeDescriptor("I").toString(), is("I"));
		assertThat(new TypeDescriptor("J").toString(), is("J"));
		assertThat(new TypeDescriptor("F").toString(), is("F"));
		assertThat(new TypeDescriptor("D").toString(), is("D"));
		assertThat(new TypeDescriptor("LFoo;").toString(), is("LFoo;"));
		assertThat(new TypeDescriptor("[I").toString(), is("[I"));
		assertThat(new TypeDescriptor("[[[I").toString(), is("[[[I"));
		assertThat(new TypeDescriptor("[LFoo;").toString(), is("[LFoo;"));
	}
}
