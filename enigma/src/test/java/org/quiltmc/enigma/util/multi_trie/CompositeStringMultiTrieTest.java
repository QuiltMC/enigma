package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.util.multi_trie.MultiTrie.Node;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.quiltmc.enigma.util.multi_trie.AbstractMapMultiTrieAccessor.getRootChildren;

public class CompositeStringMultiTrieTest {
	private static final String VALUES = "values";
	private static final String LEAVES = "leaves";
	private static final String BRANCHES = "branches";

	@Test
	void testPut() {
		final CompositeStringMultiTrie<Association> trie = Association.createAndPopulateTrie();

		Association.BY_PREFIX.asMap().forEach((prefix, associations) -> {
			final Node<Character, Association> node = trie.get(prefix);

			assertUnorderedContentsForPrefix(prefix, VALUES, associations.stream(), node.streamValues());

			assertUnorderedContentsForPrefix(
				prefix, LEAVES,
				associations.stream().filter(association -> association.isLeafOf(prefix)),
				node.streamLeaves()
			);

			assertUnorderedContentsForPrefix(
				prefix, BRANCHES,
				associations.stream().filter(association -> association.isBranchOf(prefix)),
				node.streamBranches()
			);
		});
	}

	@Test
	void testPutMulti() {
		final CompositeStringMultiTrie<MultiAssociation> trie = MultiAssociation.createAndPopulateTrie();

		Association.BY_PREFIX.asMap().forEach((prefix, associations) -> {
			final Node<Character, MultiAssociation> node = trie.get(prefix);

			assertUnorderedContentsForPrefix(
				prefix, VALUES,
				MultiAssociation.streamWith(associations.stream()),
				node.streamValues()
			);

			assertUnorderedContentsForPrefix(
				prefix, LEAVES,
				MultiAssociation.streamWith(associations.stream().filter(association -> association.isLeafOf(prefix))),
				node.streamLeaves()
			);

			assertUnorderedContentsForPrefix(
				prefix, BRANCHES,
				MultiAssociation.streamWith(associations.stream().filter(a -> a.isBranchOf(prefix))),
				node.streamBranches()
			);
		});
	}

	@Test
	void testRemove() {
		final CompositeStringMultiTrie<Association> trie = Association.createAndPopulateTrie();

		Association.BY_PREFIX.asMap().forEach((prefix, associations) -> {
			for (final Association association : associations) {
				assertRemovalResult(trie, association.isLeafOf(prefix), prefix, association);
			}
		});

		assertEmpty(trie);
	}

	@Test
	void testRemoveMulti() {
		final CompositeStringMultiTrie<MultiAssociation> trie = MultiAssociation.createAndPopulateTrie();

		Association.BY_PREFIX.asMap().forEach((prefix, associations) -> {
			for (final Association association : associations) {
				final boolean expectRemoval = association.isLeafOf(prefix);
				for (final MultiAssociation multiAssociation : MultiAssociation.BY_ASSOCIATION.get(association)) {
					assertRemovalResult(trie, expectRemoval, prefix, multiAssociation);
				}
			}
		});

		assertEmpty(trie);
	}

	@Test
	void testRemoveAll() {
		final CompositeStringMultiTrie<MultiAssociation> trie = MultiAssociation.createAndPopulateTrie();

		Association.BY_PREFIX.asMap().forEach((prefix, associations) -> {
			final List<Association> leaves = associations.stream()
				.filter(association -> association.isLeafOf(prefix))
				.toList();

			final boolean expectRemoval = !leaves.isEmpty();
			assertEquals(expectRemoval, trie.removeAll(prefix), () -> {
				return expectRemoval
					? "Expected removal of leaves with prefix \"%s\": %s"
						.formatted(prefix, MultiAssociation.streamWith(leaves.stream()).toList())
					: "Expected no removal of nodes with prefix \"%s\": %s"
						.formatted(prefix, MultiAssociation.streamWith(associations.stream()).toList());
			});
		});

		assertEmpty(trie);
	}

	private static <T> void assertRemovalResult(
		CompositeStringMultiTrie<T> trie, boolean expectRemoval, String prefix, T value
	) {
		assertEquals(
			expectRemoval,
			trie.remove(prefix, value),
			() -> "Expected%s removal of \"%s\" with prefix \"%s\"!"
				.formatted(expectRemoval ? "" : " no", value, prefix)
		);
	}

	private static void assertEmpty(CompositeStringMultiTrie<?> trie) {
		assertTrue(
			trie.isEmpty(),
			() ->"Expected trie to be empty, but had it contents: " + trie.getRoot().streamValues().toList()
		);

		final BiMap<Character, ? extends Node<Character, ?>> rootChildren = getRootChildren(trie);
		assertTrue(
			rootChildren.isEmpty(),
			() -> "Expected root's children to be pruned, but it had children: " + rootChildren
		);
	}

	private static <T> void assertUnorderedContentsForPrefix(
			String prefix, String arrayName, Stream<T> expected, Stream<T> actual
	) {
		assertThat(
			"Unexpected %s for prefix \"%s\"!".formatted(arrayName, prefix),
			actual.toList(),
			containsInAnyOrder(expected.toArray())
		);
	}

	record Association(String key) {
		static final Association EMPTY = new Association("");

		static final Association A = new Association("A");
		static final Association AB = new Association("AB");
		static final Association ABC = new Association("ABC");

		static final Association BA = new Association("BA");
		static final Association CBA = new Association("CBA");

		static final Association I = new Association("I");
		static final Association II = new Association("II");
		static final Association III = new Association("III");

		static final Association ONE = new Association("ONE");
		static final Association TWO = new Association("TWO");
		static final Association THREE = new Association("THREE");

		static final Association ENO = new Association("ENO");
		static final Association OWT = new Association("OWT");
		static final Association EERHT = new Association("EERHT");

		static final ImmutableList<Association> ALL = ImmutableList.of(
			EMPTY,
			A, AB, ABC,
			BA, CBA,
			I, II, III,
			ONE, TWO, THREE,
			ENO, OWT, EERHT
		);

		static final ImmutableMultimap<String, Association> BY_PREFIX;

		static {
			final ImmutableMultimap.Builder<String, Association> byPrefix = ImmutableMultimap.builder();

			ALL.forEach(association -> {
				for (int i = 0; i <= association.key.length(); i++) {
					byPrefix.put(association.key.substring(0, i), association);
				}
			});

			BY_PREFIX = byPrefix.build();
		}

		static CompositeStringMultiTrie<Association> createAndPopulateTrie() {
			final CompositeStringMultiTrie<Association> trie = CompositeStringMultiTrie.createHashed();

			for (final Association association : ALL) {
				trie.put(association.key, association);
			}

			return trie;
		}

		boolean isLeafOf(String prefix) {
			return this.key.equals(prefix);
		}

		boolean isBranchOf(String prefix) {
			return this.key.length() > prefix.length() && this.key.startsWith(prefix);
		}
	}

	record MultiAssociation(Association association, int id) {
		static final int MAX_COUNT = 3;

		static final ImmutableList<MultiAssociation> ALL;
		static final ImmutableMultimap<Association, MultiAssociation> BY_ASSOCIATION;

		static {
			final ImmutableList.Builder<MultiAssociation> all = ImmutableList.builder();

			final ImmutableMultimap.Builder<Association, MultiAssociation> byAssociation = ImmutableMultimap.builder();

			int id = 0;
			int count = 1;
			for (final Association association : Association.ALL) {
				int currentCount = count;
				while (currentCount > 0) {
					final MultiAssociation multiAssociation = new MultiAssociation(association, id++);

					all.add(multiAssociation);
					byAssociation.put(association, multiAssociation);

					currentCount--;
				}

				// prevent needless exponential growth
				count = (count % MAX_COUNT) + 1;
			}

			ALL = all.build();
			BY_ASSOCIATION = byAssociation.build();
		}

		static CompositeStringMultiTrie<MultiAssociation> createAndPopulateTrie() {
			final CompositeStringMultiTrie<MultiAssociation> trie = CompositeStringMultiTrie.createHashed();

			for (final MultiAssociation multiAssociation : ALL) {
				trie.put(multiAssociation.association.key, multiAssociation);
			}

			return trie;
		}

		static Stream<MultiAssociation> streamWith(Stream<Association> associations) {
			return associations
				.map(MultiAssociation.BY_ASSOCIATION::get)
				.flatMap(Collection::stream);
		}
	}
}
