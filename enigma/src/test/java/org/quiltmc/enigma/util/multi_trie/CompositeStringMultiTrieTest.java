package org.quiltmc.enigma.util.multi_trie;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.quiltmc.enigma.util.multi_trie.MutableStringMultiTrie.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompositeStringMultiTrieTest {
	private static final String VALUES = "values";
	private static final String LEAVES = "leaves";
	private static final String BRANCHES = "branches";

	private static final String KEY_BY_KEY_SUBJECT = "key-by-key subject";

	private static final String IGNORE_CASE_SUBJECT = "aBrAcAdAnIeL";

	@SuppressWarnings("SameParameterValue")
	private static String caseInverted(String string) {
		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < string.length(); i++) {
			final char c = string.charAt(i);

			final char inverted;
			if (Character.isLowerCase(c)) {
				inverted = Character.toUpperCase(c);
			} else if (Character.isUpperCase(c)) {
				inverted = Character.toLowerCase(c);
			} else {
				inverted = c;
			}

			builder.append(inverted);
		}

		return builder.toString();
	}

	// test key-by-key put's orphan logic
	@Test
	void testPutKeyByKeyFromRoot() {
		final CompositeStringMultiTrie<Integer> trie = CompositeStringMultiTrie.createHashed();

		for (int depth = 0; depth < KEY_BY_KEY_SUBJECT.length(); depth++) {
			Node<Integer> node = trie.getRoot();
			for (int iKey = 0; iKey <= depth; iKey++) {
				node = node.next(KEY_BY_KEY_SUBJECT.charAt(iKey));
			}

			node.put(depth);

			assertOneLeaf(node);

			assertTrieSize(trie, depth + 1);
		}
	}

	// tests that key-by-key put's orphan logic propagates from stems to the root
	@Test
	void testPutKeyByKeyFromStems() {
		final CompositeStringMultiTrie<Integer> trie = CompositeStringMultiTrie.createHashed();

		for (int depth = KEY_BY_KEY_SUBJECT.length() - 1; depth >= 0; depth--) {
			Node<Integer> node = trie.getRoot();
			for (int iKey = 0; iKey <= depth; iKey++) {
				node = node.next(KEY_BY_KEY_SUBJECT.charAt(iKey));
			}

			node.put(depth);

			assertOneLeaf(node);

			assertTrieSize(trie, KEY_BY_KEY_SUBJECT.length() - depth);
		}
	}

	@Test
	void testDepth() {
		final CompositeStringMultiTrie<Integer> trie = CompositeStringMultiTrie.createHashed();

		for (int depth = 0; depth < KEY_BY_KEY_SUBJECT.length(); depth++) {
			final Node<Integer> root = trie.getRoot();
			assertThat("Root node depth", root.getDepth(), is(0));

			Node<Integer> node = root;
			for (int iKey = 0; iKey <= depth; iKey++) {
				node = node.next(KEY_BY_KEY_SUBJECT.charAt(iKey));
			}

			assertThat("Branch node depth", node.getDepth(), is(depth + 1));
		}
	}

	@Test
	void testPrevious() {
		final CompositeStringMultiTrie<Object> trie = CompositeStringMultiTrie.createHashed();

		final Node<Object> root = trie.getRoot();
		assertSame(root, root.previous(), "Expected root.previous() to return itself!");

		final List<Node<Object>> nodes = new ArrayList<>();
		nodes.add(root);

		Node<Object> node = root;
		for (int iKey = 0; iKey < KEY_BY_KEY_SUBJECT.length(); iKey++) {
			node = node.next(KEY_BY_KEY_SUBJECT.charAt(iKey));
			nodes.add(node);
		}

		for (int i = nodes.size() - 1; i > 0; i--) {
			final Node<Object> subjectNode = nodes.get(i);
			final Node<Object> expectedPrev = nodes.get(i - 1);

			assertThat(expectedPrev, sameInstance(subjectNode.previous()));

			for (int steps = 0; steps < subjectNode.getDepth(); steps++) {
				final Node<Object> expectedStepPrev = nodes.get(i - steps);

				assertThat(expectedStepPrev, sameInstance(subjectNode.previous(steps)));
			}
		}
	}

	private static void assertOneLeaf(Node<?> node) {
		assertEquals(
				1, node.streamLeaves().count(),
				() -> "Expected node to have only one leaf, but had the following: " + node.streamLeaves().toList()
		);
	}

	private static void assertTrieSize(CompositeStringMultiTrie<Integer> trie, int expectedSize) {
		assertEquals(
				expectedSize, trie.getSize(),
				() -> "Expected node to have %s values, but had the following: %s"
					.formatted(expectedSize, trie.getRoot().streamValues().toList())
		);
	}

	@Test
	void testPut() {
		final CompositeStringMultiTrie<Association> trie = Association.createAndPopulateTrie();

		Association.BY_PREFIX.asMap().forEach((prefix, associations) -> {
			final MultiTrie.Node<Character, Association> node = trie.get(prefix);

			assertUnorderedContentsForPrefix(prefix, VALUES, associations.stream(), node.streamValues());

			assertUnorderedContentsForPrefix(
					prefix, LEAVES,
					associations.stream().filter(association -> association.isLeafOf(prefix)),
					node.streamLeaves()
			);

			assertUnorderedContentsForPrefix(
					prefix, BRANCHES,
					associations.stream().filter(association -> association.isBranchOf(prefix)),
					node.streamStems()
			);
		});
	}

	@Test
	void testPutMulti() {
		final CompositeStringMultiTrie<MultiAssociation> trie = MultiAssociation.createAndPopulateTrie();

		Association.BY_PREFIX.asMap().forEach((prefix, associations) -> {
			final MultiTrie.Node<Character, MultiAssociation> node = trie.get(prefix);

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
					node.streamStems()
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
				() -> "Expected trie to be empty, but had it contents: " + trie.getRoot().streamValues().toList()
		);

		final Map<Character, ? extends MultiTrie.Node<Character, ?>> rootChildren =
				((CompositeStringMultiTrie.Root<?>) trie.getRoot()).getBranches();
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

	@Test
	void testNextIgnoreCase() {
		final CompositeStringMultiTrie<String> trie = CompositeStringMultiTrie.createHashed();

		trie.put(IGNORE_CASE_SUBJECT, IGNORE_CASE_SUBJECT);

		final String invertedSubject = caseInverted(IGNORE_CASE_SUBJECT);
		Node<String> node = trie.getRoot();
		for (int i = 0; i < invertedSubject.length(); i++) {
			node = node.nextIgnoreCase(invertedSubject.charAt(i));

			assertOneValue(node);
		}

		assertOneLeaf(node);
	}

	private static void assertOneValue(Node<String> node) {
		assertEquals(
				1, node.getSize(),
				"Expected node to have only one value, but had the following: " + node.streamValues().toList()
		);
	}

	@Test
	void testGetIgnoreCase() {
		final CompositeStringMultiTrie<String> trie = CompositeStringMultiTrie.createHashed();

		trie.put(IGNORE_CASE_SUBJECT, IGNORE_CASE_SUBJECT);

		final String invertedSubject = caseInverted(IGNORE_CASE_SUBJECT);

		final Node<String> node = trie.getIgnoreCase(invertedSubject);

		assertOneValue(node);

		node.streamLeaves()
				.findAny()
				.orElseThrow(() -> new AssertionFailedError("Expected node to have a leaf, but had none!"));
	}

	record Association(String key) {
		static final ImmutableList<Association> ALL = ImmutableList.of(
			new Association(""),
			new Association("A"), new Association("AB"), new Association("ABC"),
			new Association("BA"), new Association("CBA"),
			new Association("I"), new Association("II"), new Association("III"),
			new Association("ONE"), new Association("TWO"), new Association("THREE"),
			new Association("ENO"), new Association("OWT"), new Association("EERHT")
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
