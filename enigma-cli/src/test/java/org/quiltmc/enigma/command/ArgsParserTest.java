package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArgsParserTest {
	private static final String
			NAME_1 = "name1", VALUE_1 = "value1",
			NAME_2 = "name2", VALUE_2 = "value2",
			NAME_3 = "name3", VALUE_3 = "value3",
			NAME_4 = "name4", VALUE_4 = "value4",
			NAME_5 = "name5", VALUE_5 = "value5",
			NAME_6 = "name6", VALUE_6 = "value6",
			NAME_7 = "name7", VALUE_7 = "value7",
			NAME_8 = "name8", VALUE_8 = "value8",
			NAME_9 = "name9", VALUE_9 = "value9",
			NAME_10 = "name10", VALUE_10 = "value10";

	private static final ImmutableList<String> ORDERED_NAMES = ImmutableList.of(
			NAME_1, NAME_2, NAME_3, NAME_4, NAME_5, NAME_6, NAME_7, NAME_8, NAME_9, NAME_10
	);

	private static final Argument<String>
			ARGUMENT_1 = Argument.ofString(NAME_1, "string", "test arg 1"),
			ARGUMENT_2 = Argument.ofString(NAME_2, "string", "test arg 2"),
			ARGUMENT_3 = Argument.ofString(NAME_3, "string", "test arg 3"),
			ARGUMENT_4 = Argument.ofString(NAME_4, "string", "test arg 4"),
			ARGUMENT_5 = Argument.ofString(NAME_5, "string", "test arg 5"),
			ARGUMENT_6 = Argument.ofString(NAME_6, "string", "test arg 6"),
			ARGUMENT_7 = Argument.ofString(NAME_7, "string", "test arg 7"),
			ARGUMENT_8 = Argument.ofString(NAME_8, "string", "test arg 8"),
			ARGUMENT_9 = Argument.ofString(NAME_9, "string", "test arg 9"),
			ARGUMENT_10 = Argument.ofString(NAME_10, "string", "test arg 10");

	private static final ImmutableMap<String, String> VALUES_BY_NAME = ImmutableMap.of(
			NAME_1, VALUE_1,
			NAME_2, VALUE_2,
			NAME_3, VALUE_3,
			NAME_4, VALUE_4,
			NAME_5, VALUE_5,
			NAME_6, VALUE_6,
			NAME_7, VALUE_7,
			NAME_8, VALUE_8,
			NAME_9, VALUE_9,
			NAME_10, VALUE_10
	);

	@Test
	void test1() {
		final ArgsParser<String> parser = ArgsParser.of(ARGUMENT_1);
		assertOrder(parser);
		final String value = parseValues(parser);
		assertEquals(value, VALUE_1);
	}

	@Test
	void test2() {
		final ArgsParser<Packed2> parser = ArgsParser.of(ARGUMENT_1, ARGUMENT_2, Packed2::new);
		assertOrder(parser);
		final Packed2 packed = parseValues(parser);
		assertEquals(packed.arg1, VALUE_1);
		assertEquals(packed.arg2, VALUE_2);
	}

	@Test
	void test3() {
		final ArgsParser<Packed3> parser = ArgsParser.of(ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, Packed3::new);
		assertOrder(parser);
		final Packed3 packed = parseValues(parser);
		assertEquals(packed.arg1, VALUE_1);
		assertEquals(packed.arg2, VALUE_2);
		assertEquals(packed.arg3, VALUE_3);
	}

	@Test
	void test4() {
		final ArgsParser<Packed4> parser = ArgsParser.of(ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, ARGUMENT_4, Packed4::new);
		assertOrder(parser);
		final Packed4 packed = parseValues(parser);
		assertEquals(packed.arg1, VALUE_1);
		assertEquals(packed.arg2, VALUE_2);
		assertEquals(packed.arg3, VALUE_3);
		assertEquals(packed.arg4, VALUE_4);
	}

	@Test
	void test5() {
		final ArgsParser<Packed5> parser = ArgsParser.of(
				ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, ARGUMENT_4, ARGUMENT_5,
				Packed5::new
		);
		assertOrder(parser);
		final Packed5 packed = parseValues(parser);
		assertEquals(packed.arg1, VALUE_1);
		assertEquals(packed.arg2, VALUE_2);
		assertEquals(packed.arg3, VALUE_3);
		assertEquals(packed.arg4, VALUE_4);
		assertEquals(packed.arg5, VALUE_5);
	}

	@Test
	void test6() {
		final ArgsParser<Packed6> parser = ArgsParser.of(
				ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, ARGUMENT_4, ARGUMENT_5, ARGUMENT_6,
				Packed6::new
		);
		assertOrder(parser);
		final Packed6 packed = parseValues(parser);
		assertEquals(packed.arg1, VALUE_1);
		assertEquals(packed.arg2, VALUE_2);
		assertEquals(packed.arg3, VALUE_3);
		assertEquals(packed.arg4, VALUE_4);
		assertEquals(packed.arg5, VALUE_5);
		assertEquals(packed.arg6, VALUE_6);
	}

	@Test
	void test7() {
		final ArgsParser<Packed7> parser = ArgsParser.of(
				ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, ARGUMENT_4, ARGUMENT_5, ARGUMENT_6, ARGUMENT_7,
				Packed7::new
		);
		assertOrder(parser);
		final Packed7 packed = parseValues(parser);
		assertEquals(packed.arg1, VALUE_1);
		assertEquals(packed.arg2, VALUE_2);
		assertEquals(packed.arg3, VALUE_3);
		assertEquals(packed.arg4, VALUE_4);
		assertEquals(packed.arg5, VALUE_5);
		assertEquals(packed.arg6, VALUE_6);
		assertEquals(packed.arg7, VALUE_7);
	}

	@Test
	void test8() {
		final ArgsParser<Packed8> parser = ArgsParser.of(
				ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, ARGUMENT_4, ARGUMENT_5, ARGUMENT_6, ARGUMENT_7, ARGUMENT_8,
				Packed8::new
		);
		assertOrder(parser);
		final Packed8 packed = parseValues(parser);
		assertEquals(packed.arg1, VALUE_1);
		assertEquals(packed.arg2, VALUE_2);
		assertEquals(packed.arg3, VALUE_3);
		assertEquals(packed.arg4, VALUE_4);
		assertEquals(packed.arg5, VALUE_5);
		assertEquals(packed.arg6, VALUE_6);
		assertEquals(packed.arg7, VALUE_7);
		assertEquals(packed.arg8, VALUE_8);
	}

	@Test
	void test9() {
		final ArgsParser<Packed9> parser = ArgsParser.of(
				ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, ARGUMENT_4, ARGUMENT_5,
				ARGUMENT_6, ARGUMENT_7, ARGUMENT_8, ARGUMENT_9,
				Packed9::new
		);
		assertOrder(parser);
		final Packed9 packed = parseValues(parser);
		assertEquals(packed.arg1, VALUE_1);
		assertEquals(packed.arg2, VALUE_2);
		assertEquals(packed.arg3, VALUE_3);
		assertEquals(packed.arg4, VALUE_4);
		assertEquals(packed.arg5, VALUE_5);
		assertEquals(packed.arg6, VALUE_6);
		assertEquals(packed.arg7, VALUE_7);
		assertEquals(packed.arg8, VALUE_8);
		assertEquals(packed.arg9, VALUE_9);
	}

	@Test
	void test10() {
		final ArgsParser<Packed10> parser = ArgsParser.of(
				ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, ARGUMENT_4, ARGUMENT_5,
				ARGUMENT_6, ARGUMENT_7, ARGUMENT_8, ARGUMENT_9, ARGUMENT_10,
				Packed10::new
		);
		assertOrder(parser);
		final Packed10 packed = parseValues(parser);
		assertEquals(packed.arg1, VALUE_1);
		assertEquals(packed.arg2, VALUE_2);
		assertEquals(packed.arg3, VALUE_3);
		assertEquals(packed.arg4, VALUE_4);
		assertEquals(packed.arg5, VALUE_5);
		assertEquals(packed.arg6, VALUE_6);
		assertEquals(packed.arg7, VALUE_7);
		assertEquals(packed.arg8, VALUE_8);
		assertEquals(packed.arg9, VALUE_9);
		assertEquals(packed.arg10, VALUE_10);
	}

	private static void assertOrder(ArgsParser<?> parser) {
		for (int i = 0; i < parser.count(); i++) {
			assertEquals(parser.get(i).getName(), ORDERED_NAMES.get(i));
		}
	}

	private static <T> T parseValues(ArgsParser<T> parser) {
		return parser.parse(VALUES_BY_NAME, Argument::from);
	}

	private record Packed2(String arg1, String arg2) { }
	private record Packed3(String arg1, String arg2, String arg3) { }
	private record Packed4(String arg1, String arg2, String arg3, String arg4) { }
	private record Packed5(String arg1, String arg2, String arg3, String arg4, String arg5) { }
	private record Packed6(String arg1, String arg2, String arg3, String arg4, String arg5, String arg6) { }
	private record Packed7(
			String arg1, String arg2, String arg3, String arg4, String arg5, String arg6, String arg7
	) { }
	private record Packed8(
			String arg1, String arg2, String arg3, String arg4, String arg5, String arg6, String arg7, String arg8
	) { }
	private record Packed9(
			String arg1, String arg2, String arg3, String arg4, String arg5,
			String arg6, String arg7, String arg8, String arg9
	) { }
	private record Packed10(
			String arg1, String arg2, String arg3, String arg4, String arg5,
			String arg6, String arg7, String arg8, String arg9, String arg10
	) { }
}
