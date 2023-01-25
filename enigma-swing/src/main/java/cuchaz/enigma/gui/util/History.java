package cuchaz.enigma.gui.util;

import com.google.common.collect.Queues;

import java.util.Deque;

public class History<T> {
	private final Deque<T> previous = Queues.newArrayDeque();
	private final Deque<T> next = Queues.newArrayDeque();
	private T current;

	public History(T initial) {
		this.current = initial;
	}

	public T getCurrent() {
		return this.current;
	}

	public void push(T value) {
		this.previous.addLast(this.current);
		this.current = value;
		this.next.clear();
	}

	public void replace(T value) {
		this.current = value;
	}

	public boolean canGoBack() {
		return !this.previous.isEmpty();
	}

	public T goBack() {
		this.next.addFirst(this.current);
		this.current = this.previous.removeLast();
		return this.current;
	}

	public boolean canGoForward() {
		return !this.next.isEmpty();
	}

	public T goForward() {
		this.previous.addLast(this.current);
		this.current = this.next.removeFirst();
		return this.current;
	}
}
