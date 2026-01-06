package org.quiltmc.enigma.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbsorbingSequenceTest {
	private static void awaitStartOrThrow(CountDownLatch startLatch, int index) {
		final boolean started;
		try {
			started = startLatch.await(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		assertTrue(started, () -> "%s never started!".formatted(index));
	}

	private static void assertSequenceValue(int value, ImmutableSet<Integer> expected) {
		assertTrue(expected.contains(value), () -> "Unexpected sequence value: " + value);
	}

	@Test
	void selfAbsorptionThrows() {
		final AbsorbingSequence<Object> sequence = new AbsorbingSequence<>();
		assertThrows(IllegalArgumentException.class, () -> sequence.absorb(sequence));
	}

	@Test
	void orderedAppendAndAbsorb() {
		final int count = 99;
		final int secondMin = count / 3;
		final int thirdMin = secondMin * 2;

		final ImmutableList<Integer> values = IntStream.range(0, count).boxed().collect(toImmutableList());

		final AbsorbingSequence<Integer> first = new AbsorbingSequence<>();
		final AbsorbingSequence<Integer> second = new AbsorbingSequence<>();
		final AbsorbingSequence<Integer> third = new AbsorbingSequence<>();

		for (final int value : values) {
			if (value < secondMin) {
				first.append(value);
			} else if (value < thirdMin) {
				second.append(value);
			} else {
				third.append(value);
			}
		}

		final AbsorbingSequence<Integer> absorber = new AbsorbingSequence<>();
		absorber.absorb(first).absorb(second).absorb(third);

		final UnmodifiableIterator<Integer> sequenceItr = absorber.iterator();
		final UnmodifiableIterator<Integer> valueItr = values.iterator();
		while (sequenceItr.hasNext()) {
			assertTrue(valueItr.hasNext());

			final int sequenceValue = sequenceItr.next();
			final int expectedValue = valueItr.next();

			assertThat(sequenceValue, is(expectedValue));
		}

		assertFalse(valueItr.hasNext(), () -> "Not all values were absorbed: " + absorber);

		assertFalse(first.iterator().hasNext(), "First absorbed sequence is not empty: " + first);
		assertFalse(second.iterator().hasNext(), "Second absorbed sequence is not empty: " + second);
		assertFalse(third.iterator().hasNext(), "Third absorbed sequence is not empty: " + third);
	}

	@Test
	void asyncAppend() {
		final AbsorbingSequence<Integer> sequence = new AbsorbingSequence<>();

		final CountDownLatch startLatch = new CountDownLatch(0);

		final ImmutableSet<Integer> values = IntStream.range(0, 1000).boxed().collect(toImmutableSet());

		final ImmutableList<CompletableFuture<Void>> appendFutures = values.stream()
				.map(value -> CompletableFuture.runAsync(() -> {
					awaitStartOrThrow(startLatch, value);

					sequence.append(value);
				}))
				.collect(toImmutableList());

		startLatch.countDown();

		CompletableFuture.allOf(appendFutures.toArray(new CompletableFuture[0])).join();

		int count = 0;
		for (final int value : sequence) {
			count++;

			assertSequenceValue(value, values);
		}

		assertThat(count, is(values.size()));
	}

	@Test
	void asyncAbsorb() {
		final int valueCount = 1000;
		final int valuesPerSequence = 10;

		final ImmutableSet<Integer> values = IntStream.range(0, valueCount).boxed().collect(toImmutableSet());

		final ImmutableList<AbsorbingSequence<Integer>> sequences = IntStream
				.range(0, valueCount / valuesPerSequence)
				.mapToObj(i -> new AbsorbingSequence<Integer>())
				.collect(toImmutableList());

		for (final int value : values) {
			sequences.get(value % valuesPerSequence).append(value);
		}

		final AbsorbingSequence<Integer> absorber = new AbsorbingSequence<>();

		final CountDownLatch startLatch = new CountDownLatch(1);
		final CompletableFuture<?>[] absorbFutures = new CompletableFuture[sequences.size()];
		for (int i = 0; i < sequences.size(); i++) {
			final int iSequence = i;
			absorbFutures[iSequence] = CompletableFuture.runAsync(() -> {
				awaitStartOrThrow(startLatch, iSequence);

				absorber.absorb(sequences.get(iSequence));
			});
		}

		startLatch.countDown();

		CompletableFuture.allOf(absorbFutures).join();

		int count = 0;
		for (final int value : absorber) {
			count++;
			assertSequenceValue(value, values);
		}

		assertThat(count, is(values.size()));

		for (int i = 0; i < sequences.size(); i++) {
			final int iSequence = i;
			assertFalse(
					sequences.get(iSequence).iterator().hasNext(),
					() -> "Sequence %s not empty after absorption!".formatted(iSequence)
			);
		}
	}
}
