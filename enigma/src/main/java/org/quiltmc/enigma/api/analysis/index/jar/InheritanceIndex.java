package org.quiltmc.enigma.api.analysis.index.jar;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.util.Utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;

public class InheritanceIndex implements JarIndexer {
	private final EntryIndex entryIndex;

	private final Multimap<ClassEntry, ClassEntry> classParents = HashMultimap.create();
	private final Multimap<ClassEntry, ClassEntry> classChildren = HashMultimap.create();

	public InheritanceIndex(EntryIndex entryIndex) {
		this.entryIndex = entryIndex;
	}

	@Override
	public void indexClass(ClassDefEntry classEntry) {
		ClassEntry superClass = classEntry.getSuperClass();
		if (superClass != null) {
			this.indexParent(classEntry, superClass);
		}

		for (ClassEntry interfaceEntry : classEntry.getInterfaces()) {
			this.indexParent(classEntry, interfaceEntry);
		}
	}

	private void indexParent(ClassEntry childEntry, ClassEntry parentEntry) {
		this.classParents.put(childEntry, parentEntry);
		this.classChildren.put(parentEntry, childEntry);
	}

	public Collection<ClassEntry> getParents(ClassEntry classEntry) {
		return this.classParents.get(classEntry);
	}

	public Collection<ClassEntry> getChildren(ClassEntry classEntry) {
		return this.classChildren.get(classEntry);
	}

	public Collection<ClassEntry> getDescendants(ClassEntry classEntry) {
		Collection<ClassEntry> descendants = new HashSet<>();

		LinkedList<ClassEntry> descendantQueue = new LinkedList<>();
		descendantQueue.push(classEntry);

		while (!descendantQueue.isEmpty()) {
			ClassEntry descendant = descendantQueue.pop();
			Collection<ClassEntry> children = this.getChildren(descendant);

			children.forEach(descendantQueue::push);
			descendants.addAll(children);
		}

		return descendants;
	}

	/**
	 * Prefer {@link #streamAncestors(ClassEntry)} if:
	 * <ul>
	 *     <li> performing a search that may terminate before examining all ancestors
	 *     <li> the creation of a {@link Set} is unnecessary
	 *     <li> duplicate occurrences of interfaces implemented by multiple ancestors are required
	 * </ul>
	 *
	 * @return a {@link Set} containing the passed {@code classEntry}'s ancestors
	 *
	 * @implSpec The returned set has breadth-first iteration order.<br>
	 *           Only the first (shallowest) occurrence of an interface implemented by multiple ancestors is included.<br>
	 *           Only the first (shallowest) occurrence of {@code java.lang.Object} is included.
	 *
	 * @implNote No guarantees are made about the order within a generation of ancestors.
	 *
	 * @see #streamAncestors(ClassEntry)
	 */
	public Set<ClassEntry> getAncestors(ClassEntry classEntry) {
		return this.streamAncestors(classEntry).collect(toCollection(LinkedHashSet::new));
	}

	/**
	 * @return a {@link Stream} of the passed {@code classEntry}'s ancestors in breadth-first order
	 *
	 * @implSpec Interfaces implemented by multiple ancestors will occur multiple times in the stream
	 *           (see {@link Stream#distinct()} and {@link #getAncestors(ClassEntry)}).<br>
	 *           {@code java.lang.Object} will occur once for each interface.
	 *
	 * @implNote No guarantees are made about the order within a generation of ancestors.
	 *
	 * @see #getAncestors(ClassEntry)
	 */
	public Stream<ClassEntry> streamAncestors(ClassEntry classEntry) {
		return this.streamAncestorsImpl(this.getParents(classEntry));
	}

	private Stream<ClassEntry> streamAncestorsImpl(Collection<ClassEntry> generation) {
		return generation.isEmpty() ? Stream.empty() : Utils.lazyConcat(
			generation::stream,
			() -> this.streamAncestorsImpl(generation.stream()
				.map(this::getParents)
				.flatMap(Collection::stream)
				.toList()
			)
		);
	}

	public Relation computeClassRelation(ClassEntry classEntry, ClassEntry potentialAncestor) {
		if (potentialAncestor.getName().equals("java/lang/Object")) return Relation.RELATED;
		if (!this.entryIndex.hasClass(classEntry)) return Relation.UNKNOWN;

		for (ClassEntry ancestor : this.getAncestors(classEntry)) {
			if (potentialAncestor.equals(ancestor)) {
				return Relation.RELATED;
			} else if (!this.entryIndex.hasClass(ancestor)) {
				return Relation.UNKNOWN;
			}
		}

		return Relation.UNRELATED;
	}

	public boolean isParent(ClassEntry classEntry) {
		return this.classChildren.containsKey(classEntry);
	}

	public boolean hasParents(ClassEntry classEntry) {
		Collection<ClassEntry> parents = this.classParents.get(classEntry);
		return !parents.isEmpty();
	}

	@Override
	public String getTranslationKey() {
		return "progress.jar.indexing.process.inheritance";
	}

	public enum Relation {
		RELATED,
		UNRELATED,
		UNKNOWN
	}
}
