package org.quiltmc.enigma.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.jspecify.annotations.NonNull;
import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.EmptyStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.MutableStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

// TODO javadoc
public final class StringLookup<R extends StringLookup.Result> {
	private static final int NON_PREFIX_START = 1;

	private static int getCommonPrefixLength(String left, String right) {
		final int minLength = Math.min(left.length(), right.length());

		for (int i = 0; i < minLength; i++) {
			if (left.charAt(i) != right.charAt(i)) {
				return i;
			}
		}

		return minLength;
	}

	public static <R extends Result> StringLookup<R> of(
			int substringDepth, Comparator<R> comparator, Multimap<String, R> results
	) {
		Preconditions.checkArgument(substringDepth > 0, "substringDepth must be positive!");

		final CompositeStringMultiTrie<R> prefixBuilder = CompositeStringMultiTrie.createHashed();
		final CompositeStringMultiTrie<R> substringBuilder = CompositeStringMultiTrie.createHashed();

		results.forEach((string, result) -> {
			prefixBuilder.put(string, result);

			final int stringLength = string.length();
			for (int start = NON_PREFIX_START; start < stringLength; start++) {
				final int end = Math.min(start + substringDepth, stringLength);
				MutableStringMultiTrie.Node<R> node = substringBuilder.getRoot();
				for (int i = start; i < end; i++) {
					node = node.next(string.charAt(i));
				}

				node.put(result);
			}
		});

		return new StringLookup<>(substringDepth, comparator, prefixBuilder.view(), substringBuilder.view());
	}

	private final ResultCache emptyCache = new ResultCache(
			"", EmptyStringMultiTrie.Node.get(),
			ImmutableSet.of(), ImmutableList.of()
	);

	private final int substringDepth;
	private final Comparator<ResultWrapper<R>> comparator;

	// maps complete strings to their corresponding results
	private final StringMultiTrie<R> resultsByPrefix;
	// maps all non-prefix substringDepth-length (or less) substrings
	// to their corresponding results; used to narrow down the search scope for substring matches
	private final StringMultiTrie<R> resultsBySubstring;

	@NonNull
	private ResultCache resultCache = this.emptyCache;

	private StringLookup(
			int substringDepth, Comparator<R> comparator,
			StringMultiTrie<R> resultsByPrefix, StringMultiTrie<R> resultsBySubstring
	) {
		this.substringDepth = substringDepth;
		this.comparator = Comparator.comparing(ResultWrapper::result, comparator);

		this.resultsByPrefix = resultsByPrefix;
		this.resultsBySubstring = resultsBySubstring;
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
				return Results.Different.of(this.resultCache, this.comparator);
			}
		} else {
			return Results.None.getInstance();
		}
	}

	public interface Result {
		String searchString();

		Object identity();
	}

	private record ResultWrapper<R extends Result>(R result) {
		@Override
		public boolean equals(Object o) {
			return o instanceof ResultWrapper<?> other && other.result.identity().equals(this.result.identity());
		}

		@Override
		public int hashCode() {
			return this.result.identity().hashCode();
		}
	}

	public sealed interface Results<R extends Result> {
		final class None<R extends Result> implements Results<R> {
			private static final None<?> INSTANCE = new None<>();

			@SuppressWarnings("unchecked")
			private static <R extends Result> None<R> getInstance() {
				return (None<R>) INSTANCE;
			}
		}

		final class Same<R extends Result> implements Results<R> {
			private static final Same<?> INSTANCE = new Same<>();

			@SuppressWarnings("unchecked")
			private static <R extends Result> Same<R> getInstance() {
				return (Same<R>) INSTANCE;
			}
		}

		record Different<R extends Result>(
				ImmutableCollection<R> prefixed,
				ImmutableCollection<R> containing
		) implements Results<R> {
			private static <R extends Result> Different<R> of(
					StringLookup<R>.ResultCache cache, Comparator<ResultWrapper<R>> comparator
			) {
				return new Different<>(
					// prefixed is already sorted
					cache.prefixed.stream().map(ResultWrapper::result).collect(toImmutableList()),
					cache.containing.stream()
						.sorted(comparator)
						.distinct()
						.map(ResultWrapper::result)
						.collect(toImmutableList())
				);
			}
		}
	}

	private final class ResultCache {
		final String term;
		final StringMultiTrie.Node<R> prefixNode;
		final ImmutableSet<ResultWrapper<R>> prefixed;
		final ImmutableList<ResultWrapper<R>> containing;

		ResultCache(
				String term, StringMultiTrie.Node<R> prefixNode,
				ImmutableSet<ResultWrapper<R>> prefixed, ImmutableList<ResultWrapper<R>> containing
		) {
			this.term = term;
			this.prefixNode = prefixNode;
			this.prefixed = prefixed;
			this.containing = containing;
		}

		boolean hasResults() {
			return !this.prefixNode.isEmpty() || !this.containing.isEmpty();
		}

		boolean hasSameResults(ResultCache other) {
			return this == other
					|| this.prefixNode == other.prefixNode
					&& this.containing.equals(other.containing);
		}

		ResultCache updated(String term) {
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

					final ImmutableSet<ResultWrapper<R>> prefixed;
					if (oneTermIsPrefix && this.prefixNode.getSize() == prefixNode.getSize()) {
						prefixed = this.prefixed;
					} else {
						prefixed = this.buildPrefixed(prefixNode);
					}

					final ImmutableList<ResultWrapper<R>> containing;
					if (cachedTermLength == commonPrefixLength && termLength > StringLookup.this.substringDepth) {
						containing = this.narrowContaining(term);
					} else {
						containing = this.buildContaining(term, prefixed);
					}

					return new ResultCache(term, prefixNode, prefixed, containing);
				}
			}
		}

		ResultCache createFresh(String term) {
			final StringMultiTrie.Node<R> prefixNode = StringLookup.this.resultsByPrefix.get(term);
			final ImmutableSet<ResultWrapper<R>> prefixed = this.buildPrefixed(prefixNode);
			return new ResultCache(
					term, prefixNode,
					prefixed, this.buildContaining(term, prefixed)
			);
		}

		ImmutableSet<ResultWrapper<R>> buildPrefixed(StringMultiTrie.Node<R> prefixNode) {
			return prefixNode
				.streamValues()
				.map(ResultWrapper::new)
				// sort prefixed before results are reported because it's collected to a set
				// only the first of equivalent elements is kept
				.sorted(StringLookup.this.comparator)
				.collect(toImmutableSet());
		}

		ImmutableList<ResultWrapper<R>> narrowContaining(String term) {
			return this.containing.stream()
				.filter(wrapper -> wrapper.result.searchString().contains(term))
				.collect(toImmutableList());
		}

		ImmutableList<ResultWrapper<R>> buildContaining(String term, Set<ResultWrapper<R>> excluded) {
			final int termLength = term.length();
			final boolean longTerm = termLength > StringLookup.this.substringDepth;

			final Set<R> possibilities = new HashSet<>();
			final int substringLength = longTerm ? StringLookup.this.substringDepth : termLength;
			final int lastSubstringStart = termLength - substringLength;
			for (int start = 0; start <= lastSubstringStart; start++) {
				final int end = start + substringLength;
				StringMultiTrie.Node<R> node = StringLookup.this.resultsBySubstring.getRoot();
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
				stream = stream.filter(result -> result.searchString().contains(term));
			}

			return stream
				.map(ResultWrapper::new)
				.collect(toImmutableList());
		}
	}
}
