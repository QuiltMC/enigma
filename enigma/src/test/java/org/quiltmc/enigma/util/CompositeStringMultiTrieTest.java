package org.quiltmc.enigma.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.util.multi_trie.MultiTrie.Node;
import org.quiltmc.enigma.util.multi_trie.CompositeStringMultiTrie;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;

public class CompositeStringMultiTrieTest {
	@Test
	void testAssociatedPrefixes() {
		final CompositeStringMultiTrie<Association> trie = CompositeStringMultiTrie.createHashed();

		Association.BY_PREFIX.values().stream().distinct().forEach(association -> {
			trie.put(association.key, association);
		});

		Association.BY_PREFIX.asMap().forEach((prefix, associations) -> {
			final Node<Character, Association> node = trie.get(prefix);

			assertThat(
				"Unexpected values for prefix \"%s\"".formatted(prefix),
				node.streamValues().toArray(),
				arrayContainingInAnyOrder(associations.toArray())
			);

			assertThat(
				"Unexpected leaves for prefix \"%s\"".formatted(prefix),
				node.streamLeaves().toArray(),
				arrayContainingInAnyOrder(associations.stream().filter(a -> a.isLeafOf(prefix)).toArray())
			);

			assertThat(
				"Unexpected branches for prefix \"%s\"".formatted(prefix),
				node.streamBranches().toArray(),
				arrayContainingInAnyOrder(associations.stream().filter(a -> a.isBranchOf(prefix)).toArray())
			);
		});
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

		boolean isLeafOf(String prefix) {
			return this.key.equals(prefix);
		}

		boolean isBranchOf(String prefix) {
			return this.key.length() > prefix.length() && this.key.startsWith(prefix);
		}
	}
}
