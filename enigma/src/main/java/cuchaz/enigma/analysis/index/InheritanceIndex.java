package cuchaz.enigma.analysis.index;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class InheritanceIndex implements JarIndexer {
	private final EntryIndex entryIndex;

	private final Multimap<ClassEntry, ClassEntry> classParents = HashMultimap.create();
	private final Multimap<ClassEntry, ClassEntry> classChildren = HashMultimap.create();

	public InheritanceIndex(EntryIndex entryIndex) {
		this.entryIndex = entryIndex;
	}

	@Override
	public void indexClass(ClassDefEntry classEntry) {
		if (classEntry.isJre()) {
			return;
		}

		ClassEntry superClass = classEntry.getSuperClass();
		if (superClass != null && !superClass.getName().equals("java/lang/Object")) {
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

	public Set<ClassEntry> getAncestors(ClassEntry classEntry) {
		Set<ClassEntry> ancestors = Sets.newHashSet();

		LinkedList<ClassEntry> ancestorQueue = new LinkedList<>();
		ancestorQueue.push(classEntry);

		while (!ancestorQueue.isEmpty()) {
			ClassEntry ancestor = ancestorQueue.pop();
			Collection<ClassEntry> parents = this.getParents(ancestor);

			parents.forEach(ancestorQueue::push);
			ancestors.addAll(parents);
		}

		return ancestors;
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
