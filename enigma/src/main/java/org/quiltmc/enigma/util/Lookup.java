package org.quiltmc.enigma.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
		this.emptyCache = new ResultCache(
			"", EmptyStringMultiTrie.Node.get(),
			ImmutableMap.of(), ImmutableList.of(),
			chooser
		);

		this.resultCache = this.emptyCache;
	}

	public Results<R> search(String term) {
		if (term.isEmpty()) {
			this.resultCache = this.emptyCache;

			return (Results<R>) Results.None.INSTANCE;
		}

		final ResultCache oldCache = this.resultCache;
		this.resultCache = this.resultCache.updated(term.toLowerCase());

		if (this.resultCache.hasResults()) {
			if (this.resultCache.hasSameResults(oldCache)) {
				return (Results<R>) Results.Same.INSTANCE;
			} else {
				return Results.Different.of(this.resultCache);
			}
		} else {
			return (Results<R>) Results.None.INSTANCE;
		}
	}

	public interface Result<R extends Result<R>> extends Comparable<R> {
		boolean matches(String term);

		Object getIdentity();
	}

	public sealed interface Results<R> {
		final class None<R> implements Results<R> {
			static final None<?> INSTANCE = new None<>();
		}

		final class Same<R> implements Results<R> {
			static final Same<?> INSTANCE = new Same<>();
		}

		record Different<R extends Result<R>>(
			ImmutableList<R> prefixItems,
			ImmutableList<R> containingItems
		) implements Results<R> {
			static <R extends Result<R>> Different<R> of(Lookup<R>.ResultCache cache) {
				return new Different<>(
					cache.prefixedItemsBySearchable.values().stream().distinct().collect(toImmutableList()),
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
		final ImmutableMap<Object, R> prefixedItemsBySearchable;
		final ImmutableList<R> containingItems;
		final BinaryOperator<R> chooser;

		ResultCache(
			String term, StringMultiTrie.Node<R> prefixNode,
			ImmutableMap<Object, R> prefixedItemsBySearchable,
			ImmutableList<R> containingItems,
			BinaryOperator<R> chooser
		) {
			this.term = term;
			this.prefixNode = prefixNode;
			this.prefixedItemsBySearchable = prefixedItemsBySearchable;
			this.containingItems = containingItems;
			this.chooser = chooser;
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

					final ImmutableMap<Object, R> prefixedItemsBySearchable;
					if (oneTermIsPrefix && this.prefixNode.getSize() == prefixNode.getSize()) {
						prefixedItemsBySearchable = this.prefixedItemsBySearchable;
					} else {
						prefixedItemsBySearchable = buildPrefixedItemsBySearchable(prefixNode, this.chooser);
					}

					final ImmutableList<R> containingItems;
					if (cachedTermLength == commonPrefixLength && termLength > MAX_SUBSTRING_LENGTH) {
						containingItems = this.narrowedContainingItemsOf(term);
					} else {
						containingItems = this.buildContaining(term, prefixedItemsBySearchable.keySet());
					}

					return new ResultCache(term, prefixNode, prefixedItemsBySearchable, containingItems, this.chooser);
				}
			}
		}

		private ResultCache createFresh(String term) {
			final StringMultiTrie.Node<R> prefixNode = Lookup.this.holdersByPrefix.get(term);
			final ImmutableMap<Object, R> prefixedItemsByElement =
				buildPrefixedItemsBySearchable(prefixNode, this.chooser);
			return new ResultCache(
				term, prefixNode,
				prefixedItemsByElement,
				this.buildContaining(term, prefixedItemsByElement.keySet()),
				this.chooser
			);
		}

		private static <R extends Result<R>> ImmutableMap<Object, R> buildPrefixedItemsBySearchable(
			StringMultiTrie.Node<R> prefixNode, BinaryOperator<R> chooser
		) {
			return prefixNode
				.streamValues()
				.sorted()
				.collect(toImmutableMap(
					Result::getIdentity,
					Function.identity(),
					chooser
				));
		}

		private ImmutableList<R> narrowedContainingItemsOf(String term) {
			return this.containingItems.stream()
				.filter(item -> item.matches(term))
				.collect(toImmutableList());
		}

		private ImmutableList<R> buildContaining(String term, Set<Object> excluded) {
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
				.filter(holder -> !excluded.contains(holder.getIdentity()));

			if (longTerm) {
				stream = stream.filter(holder -> holder.matches(term));
			}

			return stream
				.sorted()
				.collect(toImmutableList());
		}
	}
}
