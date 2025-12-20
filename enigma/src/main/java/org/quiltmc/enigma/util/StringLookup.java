package org.quiltmc.enigma.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.jspecify.annotations.NonNull;
import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.EmptyStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.MutableStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

public final class StringLookup<R extends StringLookup.Result<R>> {
	static final int NON_PREFIX_START = 1;
	// TODO make this an instance field
	static final int MAX_SUBSTRING_LENGTH = 2;

	private static int getCommonPrefixLength(String left, String right) {
		final int minLength = Math.min(left.length(), right.length());

		for (int i = 0; i < minLength; i++) {
			if (left.charAt(i) != right.charAt(i)) {
				return i;
			}
		}

		return minLength;
	}

	public static <R extends Result<R>> StringLookup<R> of(Multimap<String, R> results, BinaryOperator<R> chooser) {
		final CompositeStringMultiTrie<R> prefixBuilder = CompositeStringMultiTrie.createHashed();
		final CompositeStringMultiTrie<R> containingBuilder = CompositeStringMultiTrie.createHashed();

		results.forEach((lowercaseAlias, result) -> {
			prefixBuilder.put(lowercaseAlias, result);

			final int aliasLength = lowercaseAlias.length();
			for (int start = NON_PREFIX_START; start < aliasLength; start++) {
				final int end = Math.min(start + MAX_SUBSTRING_LENGTH, aliasLength);
				MutableStringMultiTrie.Node<R> node = containingBuilder.getRoot();
				for (int i = start; i < end; i++) {
					node = node.next(lowercaseAlias.charAt(i));
				}

				node.put(result);
			}
		});

		return new StringLookup<>(prefixBuilder.view(), containingBuilder.view(), chooser);
	}

	// maps complete search aliases to their corresponding results
	private final StringMultiTrie<R> resultsByPrefix;
	// maps all non-prefix MAX_SUBSTRING_LENGTH-length (or less) substrings of search
	// aliases to their corresponding results; used to narrow down the search scope for substring matches
	private final StringMultiTrie<R> resultsByContaining;

	private final BinaryOperator<R> chooser;

	private final ResultCache emptyCache = new ResultCache(
			"", EmptyStringMultiTrie.Node.get(),
			ImmutableSet.of(), ImmutableList.of()
	);

	@NonNull
	private ResultCache resultCache;

	private StringLookup(
			StringMultiTrie<R> resultsByPrefix,
			StringMultiTrie<R> resultsByContaining,
			BinaryOperator<R> chooser
	) {
		this.resultsByPrefix = resultsByPrefix;
		this.resultsByContaining = resultsByContaining;
		this.chooser = chooser;

		this.resultCache = this.emptyCache;
	}

	public Results<R> lookUp(String term) {
		if (term.isEmpty()) {
			this.resultCache = this.emptyCache;

			return Results.None.getInstance();
		}

		final ResultCache oldCache = this.resultCache;
		this.resultCache = this.resultCache.updated(term.toLowerCase());

		if (this.resultCache.hasResults()) {
			if (this.resultCache.hasSameResults(oldCache)) {
				return Results.Same.getInstance();
			} else {
				return Results.Different.of(this.resultCache);
			}
		} else {
			return Results.None.getInstance();
		}
	}

	public interface Result<R extends Result<R>> extends Comparable<R> {
		boolean matches(String term);

		Object identity();
	}

	private record ResultWrapper<R extends Result<R>>(R result) implements Comparable<ResultWrapper<R>> {
		@Override
		public boolean equals(Object o) {
			return o instanceof ResultWrapper<?> other && other.result.identity().equals(this.result.identity());
		}

		@Override
		public int hashCode() {
			return this.result.identity().hashCode();
		}

		@Override
		public int compareTo(@NonNull ResultWrapper<R> other) {
			return this.result.compareTo(other.result);
		}
	}

	public sealed interface Results<R extends Result<R>> {
		final class None<R extends Result<R>> implements Results<R> {
			private static final None<?> INSTANCE = new None<>();

			@SuppressWarnings("unchecked")
			private static <R extends Result<R>> None<R> getInstance() {
				return (None<R>) INSTANCE;
			}
		}

		final class Same<R extends Result<R>> implements Results<R> {
			private static final Same<?> INSTANCE = new Same<>();

			@SuppressWarnings("unchecked")
			private static <R extends Result<R>> Same<R> getInstance() {
				return (Same<R>) INSTANCE;
			}
		}

		record Different<R extends Result<R>>(
				ImmutableList<R> prefixResults,
				ImmutableList<R> containingResults
		) implements Results<R> {
			static <R extends Result<R>> Different<R> of(StringLookup<R>.ResultCache cache) {
				return new Different<>(
					cache.prefixResults.stream().map(ResultWrapper::result).collect(toImmutableList()),
					// TODO respect chooser here?
					cache.containingResults.stream().distinct().map(ResultWrapper::result).collect(toImmutableList())
				);
			}

			// TODO is this always false?
			public boolean isEmpty() {
				return this.prefixResults.isEmpty() && this.containingResults.isEmpty();
			}
		}
	}

	private final class ResultCache {
		final String term;
		final StringMultiTrie.Node<R> prefixNode;
		final ImmutableSet<ResultWrapper<R>> prefixResults;
		final ImmutableList<ResultWrapper<R>> containingResults;

		ResultCache(
				String term, StringMultiTrie.Node<R> prefixNode,
				ImmutableSet<ResultWrapper<R>> prefixResults,
				ImmutableList<ResultWrapper<R>> containingResults
		) {
			this.term = term;
			this.prefixNode = prefixNode;
			this.prefixResults = prefixResults;
			this.containingResults = containingResults;
		}

		private boolean hasResults() {
			return !this.prefixNode.isEmpty() || !this.containingResults.isEmpty();
		}

		private boolean hasSameResults(ResultCache other) {
			return this == other
					|| this.prefixNode == other.prefixNode
					&& this.containingResults.equals(other.containingResults);
		}

		private ResultCache updated(String term) {
			if (this.term.isEmpty()) {
				return this.createFresh(term);
			} else {
				final int commonPrefixLength = getCommonPrefixLength(this.term, term);
				final int termLength = term.length();
				final int cachedTermLength = this.term.length();

				if (commonPrefixLength == 0) {
					return this.createFresh(term);
				} else if (commonPrefixLength == termLength && commonPrefixLength == cachedTermLength) {
					return this;
				} else {
					final int backSteps = cachedTermLength - commonPrefixLength;
					StringMultiTrie.Node<R> prefixNode = this.prefixNode.previous(backSteps);
					// true iff this.term is a prefix of term or vice versa
					final boolean oneTermIsPrefix;
					if (termLength > commonPrefixLength) {
						oneTermIsPrefix = backSteps == 0;

						for (int i = commonPrefixLength; i < termLength; i++) {
							prefixNode = prefixNode.next(term.charAt(i));

							if (prefixNode.isEmpty()) {
								break;
							}
						}
					} else {
						oneTermIsPrefix = true;
					}

					final ImmutableSet<ResultWrapper<R>> prefixResults;
					if (oneTermIsPrefix && this.prefixNode.getSize() == prefixNode.getSize()) {
						prefixResults = this.prefixResults;
					} else {
						prefixResults = this.buildPrefixed(prefixNode);
					}

					final ImmutableList<ResultWrapper<R>> containingResults;
					if (cachedTermLength == commonPrefixLength && termLength > MAX_SUBSTRING_LENGTH) {
						containingResults = this.narrowContaining(term);
					} else {
						containingResults = this.buildContaining(term, prefixResults);
					}

					return new ResultCache(term, prefixNode, prefixResults, containingResults);
				}
			}
		}

		private ResultCache createFresh(String term) {
			final StringMultiTrie.Node<R> prefixNode = StringLookup.this.resultsByPrefix.get(term);
			final ImmutableSet<ResultWrapper<R>> prefixResults = this.buildPrefixed(prefixNode);
			return new ResultCache(
					term, prefixNode,
					prefixResults,
					this.buildContaining(term, prefixResults)
			);
		}

		private ImmutableSet<ResultWrapper<R>> buildPrefixed(StringMultiTrie.Node<R> prefixNode) {
			return prefixNode
				.streamValues()
				.map(ResultWrapper::new)
				.sorted()
				.collect(toImmutableMap(
					Function.identity(),
					ResultWrapper::result,
					StringLookup.this.chooser
				))
				// use keySet of map so we can respect chooser
				.keySet();
		}

		private ImmutableList<ResultWrapper<R>> narrowContaining(String term) {
			return this.containingResults.stream()
				.filter(wrapper -> wrapper.result.matches(term))
				.collect(toImmutableList());
		}

		private ImmutableList<ResultWrapper<R>> buildContaining(String term, Set<ResultWrapper<R>> excluded) {
			final int termLength = term.length();
			final boolean longTerm = termLength > MAX_SUBSTRING_LENGTH;

			final Set<R> possibilities = new HashSet<>();
			final int substringLength = longTerm ? MAX_SUBSTRING_LENGTH : termLength;
			final int lastSubstringStart = termLength - substringLength;
			for (int start = 0; start <= lastSubstringStart; start++) {
				final int end = start + substringLength;
				StringMultiTrie.Node<R> node = StringLookup.this.resultsByContaining.getRoot();
				for (int i = start; i < end; i++) {
					node = node.next(term.charAt(i));

					if (node.isEmpty()) {
						break;
					}
				}

				node.streamValues().forEach(possibilities::add);
			}

			Stream<R> stream = possibilities
					.stream()
					.filter(result -> !excluded.contains(new ResultWrapper<>(result)));

			if (longTerm) {
				stream = stream.filter(result -> result.matches(term));
			}

			return stream
				.map(ResultWrapper::new)
				.sorted()
				.collect(toImmutableList());
		}
	}
}
