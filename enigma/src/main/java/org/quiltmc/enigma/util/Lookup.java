package org.quiltmc.enigma.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
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

public final class Lookup<R extends Lookup.Result<R>> {
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

	public static <R extends Result<R>> Lookup<R> build(LinkedListMultimap<String, R> holders, BinaryOperator<R> choose) {
		final CompositeStringMultiTrie<R> prefixBuilder = CompositeStringMultiTrie.createHashed();
		final CompositeStringMultiTrie<R> containingBuilder = CompositeStringMultiTrie.createHashed();

		holders.forEach((lowercaseAlias, holder) -> {
			prefixBuilder.put(lowercaseAlias, holder);

			final int aliasLength = lowercaseAlias.length();
			for (int start = NON_PREFIX_START; start < aliasLength; start++) {
				final int end = Math.min(start + MAX_SUBSTRING_LENGTH, aliasLength);
				MutableStringMultiTrie.Node<R> node = containingBuilder.getRoot();
				for (int i = start; i < end; i++) {
					node = node.next(lowercaseAlias.charAt(i));
				}

				node.put(holder);
			}
		});

		return new Lookup<>(choose, prefixBuilder.view(), containingBuilder.view());
	}

	private final BinaryOperator<R> chooser;
	private final ResultCache emptyCache;

	// maps complete search aliases to their corresponding items
	private final StringMultiTrie<R> holdersByPrefix;
	// maps all non-prefix MAX_SUBSTRING_LENGTH-length (or less) substrings of search
	// aliases to their corresponding items; used to narrow down the search scope for substring matches
	private final StringMultiTrie<R> holdersByContaining;

	@NonNull
	private ResultCache resultCache;

	private Lookup(BinaryOperator<R> chooser, StringMultiTrie<R> holdersByPrefix, StringMultiTrie<R> holdersByContaining) {
		this.holdersByPrefix = holdersByPrefix;
		this.holdersByContaining = holdersByContaining;
		this.chooser = chooser;
		this.emptyCache = new ResultCache(
			"", EmptyStringMultiTrie.Node.get(),
			ImmutableSet.of(), ImmutableList.of()
		);

		this.resultCache = this.emptyCache;
	}

	public Results<R> search(String term) {
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

	private record ResultWrapper<R extends Result<R>>(R result) implements Comparable<ResultWrapper<R>>{
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
			ImmutableList<R> prefixItems,
			ImmutableList<R> containingItems
		) implements Results<R> {
			static <R extends Result<R>> Different<R> of(Lookup<R>.ResultCache cache) {
				return new Different<>(
					cache.prefixedResults.stream().map(ResultWrapper::result).distinct().collect(toImmutableList()),
					cache.containingItems.stream().distinct().collect(toImmutableList())
				);
			}

			public boolean isEmpty() {
				return this.prefixItems.isEmpty() && this.containingItems.isEmpty();
			}
		}
	}

	private final class ResultCache {
		final String term;
		final StringMultiTrie.Node<R> prefixNode;
		final ImmutableSet<ResultWrapper<R>> prefixedResults;
		final ImmutableList<R> containingItems;

		ResultCache(
			String term, StringMultiTrie.Node<R> prefixNode,
			ImmutableSet<ResultWrapper<R>> prefixedResults,
			ImmutableList<R> containingItems
		) {
			this.term = term;
			this.prefixNode = prefixNode;
			this.prefixedResults = prefixedResults;
			this.containingItems = containingItems;
		}

		private boolean hasResults() {
			return !this.prefixNode.isEmpty() || !this.containingItems.isEmpty();
		}

		private boolean hasSameResults(ResultCache other) {
			return this == other
				|| this.prefixNode == other.prefixNode
				&& this.containingItems.equals(other.containingItems);
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

					final ImmutableSet<ResultWrapper<R>> prefixedResults;
					if (oneTermIsPrefix && this.prefixNode.getSize() == prefixNode.getSize()) {
						prefixedResults = this.prefixedResults;
					} else {
						prefixedResults = this.buildPrefixedResults(prefixNode);
					}

					final ImmutableList<R> containingItems;
					if (cachedTermLength == commonPrefixLength && termLength > MAX_SUBSTRING_LENGTH) {
						containingItems = this.narrowedContainingItemsOf(term);
					} else {
						containingItems = this.buildContaining(term, prefixedResults);
					}

					return new ResultCache(term, prefixNode, prefixedResults, containingItems);
				}
			}
		}

		private ResultCache createFresh(String term) {
			final StringMultiTrie.Node<R> prefixNode = Lookup.this.holdersByPrefix.get(term);
			final ImmutableSet<ResultWrapper<R>> prefixedResults = this.buildPrefixedResults(prefixNode);
			return new ResultCache(
				term, prefixNode,
				prefixedResults,
				this.buildContaining(term, prefixedResults)
			);
		}

		private ImmutableSet<ResultWrapper<R>> buildPrefixedResults(StringMultiTrie.Node<R> prefixNode) {
			return prefixNode
				.streamValues()
				.sorted()
				.collect(toImmutableMap(
					ResultWrapper::new,
					Function.identity(),
					Lookup.this.chooser
				))
				// use keySet of map so we can respect chooser
				.keySet();
		}

		private ImmutableList<R> narrowedContainingItemsOf(String term) {
			return this.containingItems.stream()
				.filter(item -> item.matches(term))
				.collect(toImmutableList());
		}

		private ImmutableList<R> buildContaining(String term, Set<ResultWrapper<R>> excluded) {
			final int termLength = term.length();
			final boolean longTerm = termLength > MAX_SUBSTRING_LENGTH;

			final Set<R> possibilities = new HashSet<>();
			final int substringLength = longTerm ? MAX_SUBSTRING_LENGTH : termLength;
			final int lastSubstringStart = termLength - substringLength;
			for (int start = 0; start <= lastSubstringStart; start++) {
				final int end = start + substringLength;
				StringMultiTrie.Node<R> node = Lookup.this.holdersByContaining.getRoot();
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
				stream = stream.filter(holder -> holder.matches(term));
			}

			return stream
				.sorted()
				.collect(toImmutableList());
		}
	}
}
