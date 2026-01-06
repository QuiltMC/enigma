package org.quiltmc.enigma.util;

import com.google.common.collect.UnmodifiableIterator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.stream.Collector;

/**
 * A simple singly-linked-list implementation with an {@code O(1)}-complexity {@link #absorb(AbsorbingSequence)}
 * method.<br>
 * Intended for intermediate accumulation in {@link Collector}s.
 *
 * <p> {@link #append(Object)} and {@link #absorb(AbsorbingSequence)} synchronize their modifications.<br>
 * An {@linkplain #iterator() iterator's} behavior is undefined if its sequence is modified before iteration completes.
 *
 * <p> {@linkplain #iterator() Iterators} do <em>not</em> support {@linkplain java.util.Iterator#remove() removal}.
 *
 * @param <T> the type of values in this sequence
 */
public final class AbsorbingSequence<T> implements Iterable<T> {
	/**
	 * {@code null} <em>iff</em> empty
	 */
	@Nullable
	private Ends<T> ends;

	/**
	 * Appends the passed {@code value} to the end of this sequence.
	 */
	public void append(T value) {
		final Node<T> node = new Node<>(value);
		synchronized (this) {
			if (this.ends == null) {
				this.ends = new Ends<>(node, node);
			} else {
				this.ends.tail.next = node;
				this.ends.tail = node;
			}
		}
	}

	/**
	 * Appends all of the passed {@code sequence}'s values to this sequence and <em>removes</em> them from the passed
	 * {@code sequence}.
	 *
	 * <p> Completes in {@code O(1)} time.
	 *
	 * @return this sequence
	 *
	 * @throws IllegalArgumentException if the passed {@code sequence} is this sequence
	 */
	public AbsorbingSequence<T> absorb(AbsorbingSequence<T> sequence) {
		if (sequence == this) {
			throw new IllegalArgumentException("A sequence can't absorb itself!");
		}

		synchronized (sequence) {
			if (sequence.ends != null) {
				synchronized (this) {
					if (this.ends == null) {
						this.ends = new Ends<>(sequence.ends.head, sequence.ends.tail);
					} else {
						this.ends.tail.next = sequence.ends.head;
						this.ends.tail = sequence.ends.tail;
					}
				}

				sequence.ends = null;
			}
		}

		return this;
	}

	/**
	 * @return an iterator over the values of this sequence;
	 * the iterator's behavior is undefined if this sequence is modified before iteration has completed.
	 */
	@Override
	@NonNull
	public UnmodifiableIterator<T> iterator() {
		return new Iterator();
	}

	/**
	 * To avoid the potential for circular linking, a node must only belong to one sequence at a time.
	 */
	private static class Node<T> {
		final T value;
		@Nullable
		Node<T> next;

		Node(T value) {
			this.value = value;
		}
	}

	private static class Ends<T> {
		@NonNull
		Node<T> head;
		@NonNull
		Node<T> tail;

		Ends(@NonNull Node<T> head, @NonNull Node<T> tail) {
			this.head = head;
			this.tail = tail;
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(AbsorbingSequence.class.getSimpleName());
		builder.append("[");

		final UnmodifiableIterator<T> itr = this.iterator();
		if (itr.hasNext()) {
			final T first = itr.next();
			builder.append(first);

			while (itr.hasNext()) {
				final T value = itr.next();
				builder.append(", ").append(value);
			}
		}

		return builder.append("]").toString();
	}

	private class Iterator extends UnmodifiableIterator<T> {
		@Nullable
		Node<T> next = AbsorbingSequence.this.ends == null ? null : AbsorbingSequence.this.ends.head;

		@Override
		public boolean hasNext() {
			return this.next != null;
		}

		@SuppressWarnings("DataFlowIssue")
		@Override
		public T next() {
			final T value;
			try {
				value = this.next.value;
			} catch (NullPointerException e) {
				throw new NoSuchElementException(e);
			}

			this.next = this.next.next;

			return value;
		}
	}
}
