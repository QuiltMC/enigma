package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
			NAME_10 = "name10", VALUE_10 = "value10",
			NAME_11 = "name11", VALUE_11 = "value11",
			NAME_12 = "name12", VALUE_12 = "value12";

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
			ARGUMENT_10 = Argument.ofString(NAME_10, "string", "test arg 10"),
			ARGUMENT_11 = Argument.ofString(NAME_11, "string", "test arg 11"),
			ARGUMENT_12 = Argument.ofString(NAME_12, "string", "test arg 12");

	private static final ImmutableMap<String, String> VALUES_BY_NAME = ImmutableMap.ofEntries(
			Map.entry(NAME_1, VALUE_1),
			Map.entry(NAME_2, VALUE_2),
			Map.entry(NAME_3, VALUE_3),
			Map.entry(NAME_4, VALUE_4),
			Map.entry(NAME_5, VALUE_5),
			Map.entry(NAME_6, VALUE_6),
			Map.entry(NAME_7, VALUE_7),
			Map.entry(NAME_8, VALUE_8),
			Map.entry(NAME_9, VALUE_9),
			Map.entry(NAME_10, VALUE_10),
			Map.entry(NAME_11, VALUE_11),
			Map.entry(NAME_12, VALUE_12)
	);

	private static final ImmutableList<String> ORDERED_NAMES = VALUES_BY_NAME.keySet().asList();
	private static final ImmutableList<String> ORDERED_VALUES = VALUES_BY_NAME.values().asList();

	@Test
	void test1() {
		final ArgsParser<String> parser = ArgsParser.of(ARGUMENT_1);
		assertOrder(parser);
		final String value = parseValues(parser);
		assertValues(value);
	}

	@Test
	void test2() {
		final ArgsParser<Packed2> parser = ArgsParser.of(ARGUMENT_1, ARGUMENT_2, Packed2::new);
		assertOrder(parser);
		final Packed2 packed = parseValues(parser);
		assertValues(packed.arg1, packed.arg2);
	}

	@Test
	void test3() {
		final ArgsParser<Packed3> parser = ArgsParser.of(ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, Packed3::new);
		assertOrder(parser);
		final Packed3 packed = parseValues(parser);
		assertValues(packed.arg1, packed.arg2, packed.arg3);
	}

	@Test
	void test4() {
		final ArgsParser<Packed4> parser = ArgsParser.of(ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, ARGUMENT_4, Packed4::new);
		assertOrder(parser);
		final Packed4 packed = parseValues(parser);
		assertValues(packed.arg1, packed.arg2, packed.arg3, packed.arg4);
	}

	@Test
	void test5() {
		final ArgsParser<Packed5> parser = ArgsParser.of(
				ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, ARGUMENT_4, ARGUMENT_5,
				Packed5::new
		);
		assertOrder(parser);
		final Packed5 packed = parseValues(parser);
		assertValues(packed.arg1, packed.arg2, packed.arg3, packed.arg4, packed.arg5);
	}

	@Test
	void test6() {
		final ArgsParser<Packed6> parser = ArgsParser.of(
				ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, ARGUMENT_4, ARGUMENT_5, ARGUMENT_6,
				Packed6::new
		);
		assertOrder(parser);
		final Packed6 packed = parseValues(parser);
		assertValues(packed.arg1, packed.arg2, packed.arg3, packed.arg4, packed.arg5, packed.arg6);
	}

	@Test
	void test7() {
		final ArgsParser<Packed7> parser = ArgsParser.of(
				ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, ARGUMENT_4, ARGUMENT_5, ARGUMENT_6, ARGUMENT_7,
				Packed7::new
		);
		assertOrder(parser);
		final Packed7 packed = parseValues(parser);
		assertValues(packed.arg1, packed.arg2, packed.arg3, packed.arg4, packed.arg5, packed.arg6, packed.arg7);
	}

	@Test
	void test8() {
		final ArgsParser<Packed8> parser = ArgsParser.of(
				ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, ARGUMENT_4, ARGUMENT_5, ARGUMENT_6, ARGUMENT_7, ARGUMENT_8,
				Packed8::new
		);
		assertOrder(parser);
		final Packed8 packed = parseValues(parser);
		assertValues(
				packed.arg1, packed.arg2, packed.arg3, packed.arg4, packed.arg5, packed.arg6, packed.arg7, packed.arg8
		);
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
		assertValues(
				packed.arg1, packed.arg2, packed.arg3, packed.arg4, packed.arg5,
				packed.arg6, packed.arg7, packed.arg8, packed.arg9
		);
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
		assertValues(
				packed.arg1, packed.arg2, packed.arg3, packed.arg4, packed.arg5,
				packed.arg6, packed.arg7, packed.arg8, packed.arg9, packed.arg10
		);
	}

	@Test
	void test11() {
		final ArgsParser<Packed11> parser = ArgsParser.of(
				ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, ARGUMENT_4, ARGUMENT_5, ARGUMENT_6, ARGUMENT_7, ARGUMENT_8,
				ARGUMENT_9, ARGUMENT_10, ARGUMENT_11,
				Packed11::new
		);
		assertOrder(parser);
		final Packed11 packed = parseValues(parser);
		assertValues(
				packed.arg1, packed.arg2, packed.arg3, packed.arg4, packed.arg5, packed.arg6, packed.arg7, packed.arg8,
				packed.arg9, packed.arg10, packed.arg11
		);
	}

	@Test
	void test12() {
		final ArgsParser<Packed12> parser = ArgsParser.of(
				ARGUMENT_1, ARGUMENT_2, ARGUMENT_3, ARGUMENT_4, ARGUMENT_5, ARGUMENT_6, ARGUMENT_7, ARGUMENT_8,
				ARGUMENT_9, ARGUMENT_10, ARGUMENT_11, ARGUMENT_12,
				Packed12::new
		);
		assertOrder(parser);
		final Packed12 packed = parseValues(parser);
		assertValues(
				packed.arg1, packed.arg2, packed.arg3, packed.arg4, packed.arg5, packed.arg6, packed.arg7, packed.arg8,
				packed.arg9, packed.arg10, packed.arg11, packed.arg12
		);
	}

	private static void assertOrder(ArgsParser<?> parser) {
		for (int i = 0; i < parser.count(); i++) {
			assertEquals(parser.get(i).getName(), ORDERED_NAMES.get(i));
		}
	}

	private static void assertValues(String... values) {
		for (int i = 0; i < values.length; i++) {
			assertEquals(values[i], ORDERED_VALUES.get(i));
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
	private record Packed11(
			String arg1, String arg2, String arg3, String arg4, String arg5, String arg6, String arg7, String arg8,
			String arg9, String arg10, String arg11
	) { }
	private record Packed12(
			String arg1, String arg2, String arg3, String arg4, String arg5, String arg6, String arg7, String arg8,
			String arg9, String arg10, String arg11, String arg12
	) { }
}
