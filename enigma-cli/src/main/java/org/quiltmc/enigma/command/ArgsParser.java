package org.quiltmc.enigma.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * Encapsulates logic for parsing a list of {@link Argument}s into values. Also provides access to the argument list.
 *
 * @param <P> the type the argument values are packed in
 */
final class ArgsParser<P> implements Iterable<Argument<?>> {
	static <T> ArgsParser<T> of(Argument<T> arg) {
		return new ArgsParser<>(ImmutableList.of(arg), (values, from) -> from.parse(arg, values));
	}

	static <T1, T2, P> ArgsParser<P> of(Argument<T1> arg1, Argument<T2> arg2, BiFunction<T1, T2, P> packer) {
		return new ArgsParser<>(ImmutableList.of(arg1, arg2), (values, from) -> packer.apply(
			from.parse(arg1, values), from.parse(arg2, values)
		));
	}

	static <T1, T2, T3, P> ArgsParser<P> of(
			Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3,
			Packer3<T1, T2, T3, P> packer
	) {
		return new ArgsParser<>(ImmutableList.of(arg1, arg2, arg3), (values, from) -> packer.pack(
			from.parse(arg1, values), from.parse(arg2, values), from.parse(arg3, values)
		));
	}

	static <T1, T2, T3, T4, P> ArgsParser<P> of(
			Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4,
			Packer4<T1, T2, T3, T4, P> packer
	) {
		return new ArgsParser<>(ImmutableList.of(arg1, arg2, arg3, arg4), (values, from) -> packer.pack(
			from.parse(arg1, values), from.parse(arg2, values), from.parse(arg3, values), from.parse(arg4, values)
		));
	}

	static <T1, T2, T3, T4, T5, P> ArgsParser<P> of(
			Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5,
			Packer5<T1, T2, T3, T4, T5, P> packer
	) {
		return new ArgsParser<>(ImmutableList.of(arg1, arg2, arg3, arg4, arg5), (values, from) -> packer.pack(
			from.parse(arg1, values), from.parse(arg2, values), from.parse(arg3, values),
			from.parse(arg4, values), from.parse(arg5, values)
		));
	}

	static <T1, T2, T3, T4, T5, T6, P> ArgsParser<P> of(
			Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3,
			Argument<T4> arg4, Argument<T5> arg5, Argument<T6> arg6,
			Packer6<T1, T2, T3, T4, T5, T6, P> packer
	) {
		return new ArgsParser<>(ImmutableList.of(arg1, arg2, arg3, arg4, arg5, arg6), (values, from) -> packer.pack(
			from.parse(arg1, values), from.parse(arg2, values), from.parse(arg3, values),
			from.parse(arg4, values), from.parse(arg5, values), from.parse(arg6, values)
		));
	}

	static <T1, T2, T3, T4, T5, T6, T7, P> ArgsParser<P> of(
			Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4,
			Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7,
			Packer7<T1, T2, T3, T4, T5, T6, T7, P> packer
	) {
		return new ArgsParser<>(
				ImmutableList.of(arg1, arg2, arg3, arg4, arg5, arg6, arg7),
				(values, from) -> packer.pack(
					from.parse(arg1, values), from.parse(arg2, values), from.parse(arg3, values),
					from.parse(arg4, values), from.parse(arg5, values), from.parse(arg6, values),
					from.parse(arg7, values)
				)
		);
	}

	static <T1, T2, T3, T4, T5, T6, T7, T8, P> ArgsParser<P> of(
			Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4,
			Argument<T5> arg5, Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8,
			Packer8<T1, T2, T3, T4, T5, T6, T7, T8, P> packer
	) {
		return new ArgsParser<>(
				ImmutableList.of(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8),
				(values, from) -> packer.pack(
					from.parse(arg1, values), from.parse(arg2, values), from.parse(arg3, values),
					from.parse(arg4, values), from.parse(arg5, values), from.parse(arg6, values),
					from.parse(arg7, values), from.parse(arg8, values)
				)
		);
	}

	static <T1, T2, T3, T4, T5, T6, T7, T8, T9, P> ArgsParser<P> of(
			Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5,
			Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9,
			Packer9<T1, T2, T3, T4, T5, T6, T7, T8, T9, P> packer
	) {
		return new ArgsParser<>(
				ImmutableList.of(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9),
				(values, from) -> packer.pack(
					from.parse(arg1, values), from.parse(arg2, values), from.parse(arg3, values),
					from.parse(arg4, values), from.parse(arg5, values), from.parse(arg6, values),
					from.parse(arg7, values), from.parse(arg8, values), from.parse(arg9, values)
				)
		);
	}

	static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, P> ArgsParser<P> of(
			Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5,
			Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, Argument<T10> arg10,
			Packer10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, P> packer
	) {
		return new ArgsParser<>(
				ImmutableList.of(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10),
				(values, from) -> packer.pack(
					from.parse(arg1, values), from.parse(arg2, values), from.parse(arg3, values),
					from.parse(arg4, values), from.parse(arg5, values), from.parse(arg6, values),
					from.parse(arg7, values), from.parse(arg8, values), from.parse(arg9, values),
					from.parse(arg10, values)
				)
		);
	}

	static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, P> ArgsParser<P> of(
			Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5,
			Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, Argument<T10> arg10,
			Argument<T11> arg11,
			Packer11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, P> packer
	) {
		return new ArgsParser<>(
				ImmutableList.of(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11),
				(values, from) -> packer.pack(
					from.parse(arg1, values), from.parse(arg2, values), from.parse(arg3, values),
					from.parse(arg4, values), from.parse(arg5, values), from.parse(arg6, values),
					from.parse(arg7, values), from.parse(arg8, values), from.parse(arg9, values),
					from.parse(arg10, values), from.parse(arg11, values)
				)
		);
	}

	static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, P> ArgsParser<P> of(
			Argument<T1> arg1, Argument<T2> arg2, Argument<T3> arg3, Argument<T4> arg4, Argument<T5> arg5,
			Argument<T6> arg6, Argument<T7> arg7, Argument<T8> arg8, Argument<T9> arg9, Argument<T10> arg10,
			Argument<T11> arg11, Argument<T12> arg12,
			Packer12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, P> packer
	) {
		return new ArgsParser<>(
				ImmutableList.of(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12),
				(values, from) -> packer.pack(
					from.parse(arg1, values), from.parse(arg2, values), from.parse(arg3, values),
					from.parse(arg4, values), from.parse(arg5, values), from.parse(arg6, values),
					from.parse(arg7, values), from.parse(arg8, values), from.parse(arg9, values),
					from.parse(arg10, values), from.parse(arg11, values), from.parse(arg12, values)
				)
		);
	}

	private final ImmutableList<Argument<?>> args;

	private final BiFunction<Map<String, String>, ArgParser, P> impl;

	private ArgsParser(ImmutableList<Argument<?>> args, BiFunction<Map<String, String>, ArgParser, P> impl) {
		this.args = args;
		this.impl = impl;
	}

	P parse(Map<String, String> values, ArgParser from) {
		return this.impl.apply(values, from);
	}

	Argument<?> get(int index) {
		return this.args.get(index);
	}

	int count() {
		return this.args.size();
	}

	boolean isEmpty() {
		return this.args.isEmpty();
	}

	@Override
	@Nonnull
	public UnmodifiableIterator<Argument<?>> iterator() {
		return this.args.iterator();
	}

	Stream<Argument<?>> stream() {
		return this.args.stream();
	}

	@FunctionalInterface
	interface ArgParser {
		<T> T parse(Argument<T> arg, Map<String, String> values);
	}

	@FunctionalInterface
	interface Packer3<T1, T2, T3, P> {
		P pack(T1 v1, T2 v2, T3 v3);
	}

	@FunctionalInterface
	interface Packer4<T1, T2, T3, T4, P> {
		P pack(T1 v1, T2 v2, T3 v3, T4 v4);
	}

	@FunctionalInterface
	interface Packer5<T1, T2, T3, T4, T5, P> {
		P pack(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5);
	}

	@FunctionalInterface
	interface Packer6<T1, T2, T3, T4, T5, T6, P> {
		P pack(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6);
	}

	@FunctionalInterface
	interface Packer7<T1, T2, T3, T4, T5, T6, T7, P> {
		P pack(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7);
	}

	@FunctionalInterface
	interface Packer8<T1, T2, T3, T4, T5, T6, T7, T8, P> {
		P pack(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8);
	}

	@FunctionalInterface
	interface Packer9<T1, T2, T3, T4, T5, T6, T7, T8, T9, P> {
		P pack(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9);
	}

	@FunctionalInterface
	interface Packer10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, P> {
		P pack(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10);
	}

	@FunctionalInterface
	interface Packer11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, P> {
		P pack(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11);
	}

	@FunctionalInterface
	interface Packer12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, P> {
		P pack(T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8, T9 v9, T10 v10, T11 v11, T12 v12);
	}

	static final class Empty {
		static final Empty INSTANCE = new Empty();

		static final ArgsParser<Empty> PARSER = new ArgsParser<>(ImmutableList.of(), (values, from) -> INSTANCE);

		private Empty() { }
	}
}
