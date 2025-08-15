package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.quiltmc.enigma.command.CommonArguments.DEOBFUSCATED_NAMESPACE;
import static org.quiltmc.enigma.command.CommonArguments.INPUT_MAPPINGS;
import static org.quiltmc.enigma.command.CommonArguments.MAPPING_OUTPUT;
import static org.quiltmc.enigma.command.CommonArguments.OBFUSCATED_NAMESPACE;

public class ArgMapTest {
	private static final InvertMappingsCommand TEST_SUBJECT = InvertMappingsCommand.INSTANCE;

	private static final Argument<?>
			REQUIRED_1 = INPUT_MAPPINGS, REQUIRED_2 = MAPPING_OUTPUT,
			OPTIONAL_1 = OBFUSCATED_NAMESPACE, OPTIONAL_2 = DEOBFUSCATED_NAMESPACE;

	private static final String
			REQUIRED_VALUE_1 = "required1",
			REQUIRED_VALUE_2 = "required2",
			OPTIONAL_VALUE_1 = "optional1",
			OPTIONAL_VALUE_2 = "optional2";

	private static final ImmutableMap<String, String> COMPLETE_EXPECTED_MAP = ImmutableMap.of(
			REQUIRED_1.getName(), REQUIRED_VALUE_1,
			REQUIRED_2.getName(), REQUIRED_VALUE_2,
			OPTIONAL_1.getName(), OPTIONAL_VALUE_1,
			OPTIONAL_2.getName(), OPTIONAL_VALUE_2
	);

	private static final Collector<Argument<?>, ?, Map<String, String>> EXPECTATION_COLLECTOR = Collectors.toMap(
		Argument::getName,
		arg -> {
			final String expectedValue = COMPLETE_EXPECTED_MAP.get(arg.getName());
			assertNotNull(expectedValue);
			return expectedValue;
		}
	);

	@ParameterizedTest
	@MethodSource("streamHomogenousArgsInputs")
	void testAllUnnamedArgs(List<Argument<?>> arguments) {
		assertHomogenousArgs(arguments, ArgMapTest::getUnnamedValue);
	}

	@ParameterizedTest
	@MethodSource("streamHomogenousArgsInputs")
	void testAllNamedArgs(List<Argument<?>> arguments) {
		assertHomogenousArgs(arguments, ArgMapTest::getNamedValue);

		final List<Argument<?>> reversed = reversed(arguments);
		assertHomogenousArgs(reversed, ArgMapTest::getNamedValue);
	}

	@ParameterizedTest
	@MethodSource("streamMixedInputs")
	void testMixedArgs(MixedArgs arguments) {
		final Map<String, String> expectation = Stream
				.concat(arguments.unnamed.stream(), arguments.named.stream())
				.collect(EXPECTATION_COLLECTOR);

		final List<String> unnamedValues = arguments.unnamed.stream().map(ArgMapTest::getUnnamedValue).toList();

		final String[] values1 = Stream
				.concat(
					unnamedValues.stream(),
					arguments.named.stream().map(ArgMapTest::getNamedValue)
				)
				.toArray(String[]::new);

		final List<Argument<?>> reversedNamed = reversed(arguments.named);
		assertEquals(expectation, TEST_SUBJECT.buildValuesByName(values1));

		final String[] values2 = Stream
			.concat(
				unnamedValues.stream(),
				reversedNamed.stream().map(ArgMapTest::getNamedValue)
			)
			.toArray(String[]::new);

		assertEquals(expectation, TEST_SUBJECT.buildValuesByName(values2));
	}

	private static Stream<MixedArgs> streamMixedInputs() {
		return Stream.of(
			MixedArgs.builder().unnamed(REQUIRED_1).named(REQUIRED_2).build(),
			MixedArgs.builder().unnamed(REQUIRED_1).named(REQUIRED_2, OPTIONAL_1).build(),
			MixedArgs.builder().unnamed(REQUIRED_1).named(REQUIRED_2, OPTIONAL_1, OPTIONAL_2).build(),
			MixedArgs.builder().unnamed(REQUIRED_1, REQUIRED_2).named(OPTIONAL_1).build(),
			MixedArgs.builder().unnamed(REQUIRED_1, REQUIRED_2).named(OPTIONAL_1, OPTIONAL_2).build(),
			MixedArgs.builder().unnamed(REQUIRED_1, REQUIRED_2, OPTIONAL_1).named(OPTIONAL_2).build()
		);
	}

	private static Stream<List<Argument<?>>> streamHomogenousArgsInputs() {
		return Stream.of(
			List.of(REQUIRED_1, REQUIRED_2),
			List.of(REQUIRED_1, REQUIRED_2, OPTIONAL_1),
			List.of(REQUIRED_1, REQUIRED_2, OPTIONAL_1, OPTIONAL_2)
		);
	}

	@Test
	void testInvalidArgs() {
		//TODO
	}

	private static void assertHomogenousArgs(Collection<Argument<?>> args, Function<Argument<?>, String> valueGetter) {
		final Map<String, String> expectation = args.stream().collect(EXPECTATION_COLLECTOR);

		final String[] values = args.stream().map(valueGetter).toArray(String[]::new);
		assertEquals(expectation, TEST_SUBJECT.buildValuesByName(values));
	}

	private static String getUnnamedValue(Argument<?> argument) {
		final String value = COMPLETE_EXPECTED_MAP.get(argument.getName());
		assertNotNull(value);
		return value;
	}

	private static String getNamedValue(Argument<?> argument) {
		return argument.getName() + Argument.NAME_DELIM + getUnnamedValue(argument);
	}

	private static <T> List<T> reversed(Collection<T> arguments) {
		final List<T> reversed = new ArrayList<>(arguments);
		Collections.reverse(reversed);
		return reversed;
	}

	record MixedArgs(ImmutableList<Argument<?>> unnamed, ImmutableList<Argument<?>> named) {
		static Builder builder() {
			return new Builder();
		}

		static class Builder {
			ImmutableList<Argument<?>> unnamed;
			ImmutableList<Argument<?>> named;

			Builder unnamed(Argument<?>... unnamed) {
				this.unnamed = ImmutableList.copyOf(unnamed);
				return this;
			}

			Builder named(Argument<?>... named) {
				this.named = ImmutableList.copyOf(named);
				return this;
			}

			MixedArgs build() {
				if (this.unnamed == null || this.unnamed.isEmpty()) {
					throw new IllegalStateException("Missing unnamed args.");
				}

				if (this.named == null || this.named.isEmpty()) {
					throw new IllegalStateException("Missing named args.");
				}

				return new MixedArgs(Objects.requireNonNull(this.unnamed), Objects.requireNonNull(this.named));
			}
		}
	}
}
