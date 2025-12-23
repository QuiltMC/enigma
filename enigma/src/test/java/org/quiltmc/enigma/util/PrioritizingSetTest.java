package org.quiltmc.enigma.util;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrioritizingSetTest {
	@Test
	void test() {
		final PrioritizingSet<Element> set = new PrioritizingSet<>(HashMap::new, Element.COMPARATOR);

		for (final Element.Group group : Element.Group.BY_VALUE.values()) {
			final int expectedSize = set.size() + 1;

			assertTrue(set.add(group.mid));
			assertThat(set.size(), is(expectedSize));

			// low should not replace mid
			assertFalse(set.add(group.low));
			assertThat(set.size(), is(expectedSize));
			// value should still be mid
			assertThat(set.get(group.low), is(group.mid));

			// high should replace mid
			assertThat(set.addOrReplace(group.high), is(group.mid));
			assertThat(set.size(), is(expectedSize));
			// new value should be high
			assertThat(set.get(group.high), is(group.high));
		}
	}

	record Element(String value, int priority) {
		static Comparator<Element> COMPARATOR = Comparator.comparing(Element::priority, Comparator.reverseOrder());

		@Override
		public boolean equals(Object o) {
			return o instanceof Element other && other.value.equals(this.value);
		}

		@Override
		public int hashCode() {
			return this.value.hashCode();
		}

		record Group(Element low, Element mid, Element high) {
			static final int LOW = -1;
			static final int MID = 0;
			static final int HIGH = 1;

			static ImmutableMap<String, Group> BY_VALUE = Stream
				.of("value1", "value2", "value3")
				.collect(toImmutableMap(
					Function.identity(),
					Group::of
				));


			static Group of(String value) {
				return new Group(new Element(value, LOW), new Element(value, MID), new Element(value, HIGH));
			}
		}
	}
}
