package cuchaz.enigma.translation.mapping.tree;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

public class DeltaTrackingTree<T> implements EntryTree<T> {
	private final EntryTree<T> delegate;

	private EntryTree<T> deltaReference;
	private EntryTree<Object> changes = new HashEntryTree<>();

	public DeltaTrackingTree(EntryTree<T> delegate) {
		this.delegate = delegate;
		this.deltaReference = new HashEntryTree<>(delegate);
	}

	public DeltaTrackingTree() {
		this(new HashEntryTree<>());
	}

	@Override
	public void insert(Entry<?> entry, T value) {
		this.trackChange(entry);
		this.delegate.insert(entry, value);
	}

	@Nullable
	@Override
	public T remove(Entry<?> entry) {
		this.trackChange(entry);
		return this.delegate.remove(entry);
	}

	public void trackChange(Entry<?> entry) {
		this.changes.insert(entry, MappingDelta.PLACEHOLDER);
	}

	@Nullable
	@Override
	public T get(Entry<?> entry) {
		return this.delegate.get(entry);
	}

	@Override
	public Collection<Entry<?>> getChildren(Entry<?> entry) {
		return this.delegate.getChildren(entry);
	}

	@Override
	public Collection<Entry<?>> getSiblings(Entry<?> entry) {
		return this.delegate.getSiblings(entry);
	}

	@Nullable
	@Override
	public EntryTreeNode<T> findNode(Entry<?> entry) {
		return this.delegate.findNode(entry);
	}

	@Override
	public Stream<EntryTreeNode<T>> getRootNodes() {
		return this.delegate.getRootNodes();
	}

	@Override
	public DeltaTrackingTree<T> translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		DeltaTrackingTree<T> translatedTree = new DeltaTrackingTree<>(this.delegate.translate(translator, resolver, mappings));
		translatedTree.changes = this.changes.translate(translator, resolver, mappings);
		return translatedTree;
	}

	@Override
	public Stream<Entry<?>> getAllEntries() {
		return this.delegate.getAllEntries();
	}

	@Override
	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@Override
	public Iterator<EntryTreeNode<T>> iterator() {
		return this.delegate.iterator();
	}

	public MappingDelta<T> takeDelta() {
		MappingDelta<T> delta = new MappingDelta<>(this.deltaReference, this.changes);
		this.resetDelta();
		return delta;
	}

	private void resetDelta() {
		this.deltaReference = new HashEntryTree<>(this.delegate);
		this.changes = new HashEntryTree<>();
	}

	public boolean isDirty() {
		return !this.changes.isEmpty();
	}
}
