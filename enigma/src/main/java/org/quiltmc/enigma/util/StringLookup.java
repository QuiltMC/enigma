package org.quiltmc.enigma.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.NonNull;
import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.EmptyStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.MutableStringMultiTrie;
import org.quiltmc.enigma.util.multi_trie.StringMultiTrie;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * Allows {@linkplain #lookUp(String) looking up} {@linkplain Result results} via
 * {@linkplain Result#lookupString() strings}.<br>
 * Substrings of {@linkplain Result#lookupString() lookup strings} will be matched.<br>
 * Any number of {@linkplain Result results} may be associated with a {@linkplain Result#lookupString() string}.<br>
 * A {@link Result Result} typically wraps the actual object being searched for,
 * i.e. its {@linkplain Result#target() target}.
 *
 * @see #lookUp(String)
 * @see #of(int, Comparator, Iterable)
 * @see Result
 * @see Results
 *
 * @param <R> the type of results
 */
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

	/**
	 * @param substringDepth the length of substring that can be looked up before having to check
	 *                       {@link String#contains(CharSequence)};<br>
	 *                       higher values mean faster lookups at the cost of a greater memory footprint;<br>
	 *                       for search terms of this length or less, {@link String#contains(CharSequence)}
	 *                       is not checked;<br>
	 *                       for longer search terms, {@link String#contains(CharSequence)} is only checked for
	 *                       results containing a substring of the search term of this length
	 * @param comparator     a comparator which determines the order of returned {@link Results Results};
	 *                       note that, in the case of results with the same {@linkplain Result#target() target},
	 *                       only the first is kept
	 * @param results        the results which may be {@linkplain #lookUp(String) looked up};<br>
	 *                       the returned lookup will not reflect changes to the passed iterable;<br>
	 *                       create a new lookup if strings or results change
	 *
	 * @param <R> the type of results
	 *
	 * @return a new lookup for the passed {@code results}
	 *
	 * @see Result
	 * @see #lookUp(String)
	 */
	public static <R extends Result> StringLookup<R> of(
			int substringDepth, Comparator<R> comparator, Iterable<R> results
	) {
		// TODO replace this with Arguments::requirePositive from #346
		Preconditions.checkArgument(substringDepth > 0, "substringDepth must be positive!");

		final CompositeStringMultiTrie<R> prefixBuilder = CompositeStringMultiTrie.createHashed();
		final CompositeStringMultiTrie<R> substringBuilder = CompositeStringMultiTrie.createHashed();

		results.forEach(result -> {
			final String string = result.lookupString();
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

	private final ResultCache emptyTermCache = new ResultCache(
			"", EmptyStringMultiTrie.Node.get(),
			ImmutableSet.of(), ImmutableList.of(),
			Results.empty()
	);

	private final int substringDepth;
	private final Comparator<ResultWrapper<R>> comparator;

	// maps complete strings to their corresponding results
	private final StringMultiTrie<R> resultsByPrefix;
	// maps all non-prefix substringDepth-length (or less) substrings
	// to their corresponding results; used to narrow down the search scope for substring matches
	private final StringMultiTrie<R> resultsBySubstring;

	@NonNull
	private ResultCache cache = this.emptyTermCache;

	private StringLookup(
			int substringDepth, Comparator<R> comparator,
			StringMultiTrie<R> resultsByPrefix, StringMultiTrie<R> resultsBySubstring
	) {
		this.substringDepth = substringDepth;
		this.comparator = Comparator.comparing(ResultWrapper::result, comparator);

		this.resultsByPrefix = resultsByPrefix;
		this.resultsBySubstring = resultsBySubstring;
	}

	// TODO implement lookUpIgnoreCase
	/**
	 * @return results for the passed {@code term}; an empty {@code term} yields empty results
	 *
	 * @see #lookUpDifferent(String)
	 */
	public Results<R> lookUp(String term) {
		return this.lookUpImpl(term).getCache().results;
	}

	/**
	 * @return an {@link Optional} holding results for the passed {@code term} if they are different from results
	 * returned by the most recent call to {@code lookUpDifferent(String)} or {@link #lookUp(String)},
	 * or {@link Optional#empty()} otherwise
	 *
	 * @see #lookUp(String)
	 */
	public Optional<Results<R>> lookUpDifferent(String term) {
		return Optional
			.of(this.lookUpImpl(term))
			.filter(ResultCache.Update::changed)
			.map(ResultCache.Update::getCache)
			.map(ResultCache::getResults);
	}

	private ResultCache.Update lookUpImpl(String term) {
		final ResultCache.Update updated = this.cache.updated(term.toLowerCase());
		this.cache = updated.getCache();
		return updated;
	}

	// TODO replace result interface with final immutable entry class
	/**
	 * A result which may be looked up via a {@link StringLookup}.
	 *
	 * <p> A result associates a {@link #lookupString()} with a {@link #target()}.<br>
	 * Targets are what you actually want to find when performing a lookup.
	 *
	 * <p> If a lookup contains multiple results with the same target, only one will appear in {@link Results Results}.
	 *
	 * <p> Multiple results with the same target allow looking up the target via different strings
	 * without creating duplicate lookup results.<br>
	 * Creating multiple results with the same target and same string is wasteful and should be avoided;
	 * no attempt is made to exclude them.
	 */
	public interface Result {
		/**
		 * @return the string used to look up this result; substrings will be matched
		 *
		 * @implSpec implementations must be pure
		 */
		String lookupString();

		/**
		 * @return the target of this result
		 *
		 * @implSpec implementations must be pure
		 *
		 * @implNote the target object is used as the result's identity for certain operations;
		 * the target's {@link #equals(Object)} and {@link #hashCode()} methods determine equality and placement
		 */
		Object target();
	}

	private record ResultWrapper<R extends Result>(R result) {
		@Override
		public boolean equals(Object o) {
			return o instanceof ResultWrapper<?> other && other.result.target().equals(this.result.target());
		}

		@Override
		public int hashCode() {
			return this.result.target().hashCode();
		}
	}

	/**
	 * Represents the {@link Result Result}s found by a {@linkplain #lookUp(String) lookup}.
	 *
	 * @param <R> the type of results
	 *
	 * @see #lookUp(String)
	 */
	public static final class Results<R extends Result> {
		private static final Results<?> EMPTY = new Results<>(ImmutableList.of(), ImmutableList.of());

		@SuppressWarnings("unchecked")
		private static <R extends Result> Results<R> empty() {
			return (Results<R>) EMPTY;
		}

		private final ImmutableCollection<R> prefixed;
		private final ImmutableCollection<R> containing;

		private Results(ImmutableCollection<R> prefixed, ImmutableCollection<R> containing) {
			this.prefixed = prefixed;
			this.containing = containing;
		}

		private static <R extends Result> Results<R> of(
				Comparator<ResultWrapper<R>> comparator,
				ImmutableSet<ResultWrapper<R>> prefixed,
				ImmutableList<ResultWrapper<R>> containing
		) {
			return prefixed.isEmpty() && containing.isEmpty() ? empty() : new Results<>(
				// prefixed is already sorted
				prefixed.stream().map(ResultWrapper::result).collect(toImmutableList()),
				containing.stream()
					.sorted(comparator)
					.distinct()
					.map(ResultWrapper::result)
					.collect(toImmutableList())
			);
		}

		public boolean areEmpty() {
			return this.prefixed.isEmpty() && this.containing.isEmpty();
		}

		/**
		 * @return results prefixed with the search term
		 *
		 * @see #containing()
		 */
		public ImmutableCollection<R> prefixed() {
			return this.prefixed;
		}

		/**
		 * @return results containing the search term but not prefixed with it
		 *
		 * @see #prefixed()
		 */
		public ImmutableCollection<R> containing() {
			return this.containing;
		}
	}

	private final class ResultCache {
		final String term;
		final StringMultiTrie.Node<R> prefixNode;
		final ImmutableSet<ResultWrapper<R>> prefixed;
		final ImmutableList<ResultWrapper<R>> containing;
		final Results<R> results;

		ResultCache(
				String term, StringMultiTrie.Node<R> prefixNode,
				ImmutableSet<ResultWrapper<R>> prefixed, ImmutableList<ResultWrapper<R>> containing,
				Results<R> results
		) {
			this.term = term;
			this.prefixNode = prefixNode;
			this.prefixed = prefixed;
			this.containing = containing;
			this.results = results;
		}

		Results<R> getResults() {
			return this.results;
		}

		ResultCache.Update updated(String term) {
			if (term.isEmpty()) {
				return StringLookup.this.emptyTermCache.new Update(!this.results.areEmpty());
			} else if (this.term.isEmpty()) {
				final ResultCache fresh = this.createFresh(term);
				return fresh.new Update(!fresh.results.areEmpty());
			} else {
				final int commonPrefixLength = getCommonPrefixLength(this.term, term);
				final int termLength = term.length();
				final int cachedTermLength = this.term.length();

				if (commonPrefixLength == 0) {
					final ResultCache fresh = this.createFresh(term);
					return fresh.new Update(!fresh.sameResults(this.prefixNode, this.containing));
				} else if (commonPrefixLength == termLength && commonPrefixLength == cachedTermLength) {
					return this.new Update(false);
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

					final boolean same = this.sameResults(prefixNode, containing);
					final Results<R> results = same
							? this.results
							: Results.of(StringLookup.this.comparator, prefixed, containing);
					return new ResultCache(term, prefixNode, prefixed, containing, results).new Update(!same);
				}
			}
		}

		private boolean sameResults(StringMultiTrie.Node<R> prefixNode, ImmutableList<ResultWrapper<R>> containing) {
			return this.prefixNode == prefixNode && this.containing.equals(containing);
		}

		ResultCache createFresh(String term) {
			final StringMultiTrie.Node<R> prefixNode = StringLookup.this.resultsByPrefix.get(term);
			final ImmutableSet<ResultWrapper<R>> prefixed = this.buildPrefixed(prefixNode);
			final ImmutableList<ResultWrapper<R>> containing = this.buildContaining(term, prefixed);
			return new ResultCache(
					term, prefixNode,
					prefixed, containing,
					Results.of(StringLookup.this.comparator, prefixed, containing)
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
				.filter(wrapper -> wrapper.result.lookupString().contains(term))
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
				stream = stream.filter(result -> result.lookupString().contains(term));
			}

			return stream
				.map(ResultWrapper::new)
				.collect(toImmutableList());
		}

		class Update {
			final boolean changed;

			Update(boolean changed) {
				this.changed = changed;
			}

			ResultCache getCache() {
				return ResultCache.this;
			}

			boolean changed() {
				return this.changed;
			}
		}
	}
}
