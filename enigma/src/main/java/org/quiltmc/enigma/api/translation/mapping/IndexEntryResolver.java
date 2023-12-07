package org.quiltmc.enigma.api.translation.mapping;

import org.quiltmc.enigma.impl.analysis.IndexTreeBuilder;
import org.quiltmc.enigma.api.analysis.tree.MethodImplementationsTreeNode;
import org.quiltmc.enigma.api.analysis.tree.MethodInheritanceTreeNode;
import org.quiltmc.enigma.api.analysis.index.jar.BridgeMethodIndex;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.VoidTranslator;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

public class IndexEntryResolver implements EntryResolver {
	private final EntryIndex entryIndex;
	private final InheritanceIndex inheritanceIndex;
	private final BridgeMethodIndex bridgeMethodIndex;

	private final IndexTreeBuilder treeBuilder;

	public IndexEntryResolver(JarIndex index) {
		this.entryIndex = index.getIndex(EntryIndex.class);
		this.inheritanceIndex = index.getIndex(InheritanceIndex.class);
		this.bridgeMethodIndex = index.getIndex(BridgeMethodIndex.class);

		this.treeBuilder = new IndexTreeBuilder(index);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E extends Entry<?>> Collection<E> resolveEntry(E entry, ResolutionStrategy strategy) {
		if (entry == null) {
			return Collections.emptySet();
		}

		Entry<ClassEntry> classChild = this.getClassChild(entry);
		if (classChild != null && !(classChild instanceof ClassEntry)) {
			AccessFlags access = this.entryIndex.getEntryAccess(classChild);

			// If we're looking for the closest and this entry exists, we're done looking
			if (strategy == ResolutionStrategy.RESOLVE_CLOSEST && access != null) {
				return Collections.singleton(entry);
			}

			if (access == null || (!access.isPrivate() && !access.isStatic())) {
				Collection<Entry<ClassEntry>> resolvedChildren = this.resolveChildEntry(classChild, strategy);
				if (!resolvedChildren.isEmpty()) {
					return resolvedChildren.stream()
							.map(resolvedChild -> (E) entry.replaceAncestor(classChild, resolvedChild))
							.toList();
				}
			}
		}

		return Collections.singleton(entry);
	}

	/**
	 * Get a direct child of any class that is an ancestor of the given entry.
	 *
	 * @param entry the descendant of a class
	 * @return the direct child of a class, which is an ancestor of the given entry or the entry itself
	 */
	@Nullable
	private Entry<ClassEntry> getClassChild(Entry<?> entry) {
		if (entry instanceof ClassEntry) {
			return null;
		}

		// get the entry in the hierarchy that is the child of a class
		List<Entry<?>> ancestry = entry.getAncestry();
		for (int i = ancestry.size() - 1; i > 0; i--) {
			Entry<?> child = ancestry.get(i);
			Entry<ClassEntry> cast = child.castParent(ClassEntry.class);
			if (cast != null && !(cast instanceof ClassEntry)) {
				// we found the entry which is a child of a class, we are now able to resolve the owner of this entry
				return cast;
			}
		}

		return null;
	}

	private Set<Entry<ClassEntry>> resolveChildEntry(Entry<ClassEntry> entry, ResolutionStrategy strategy) {
		ClassEntry ownerClass = entry.getParent();

		// Resolve specialized methods using their bridges
		if (entry instanceof MethodEntry methodEntry) {
			MethodEntry bridgeMethod = this.bridgeMethodIndex.getBridgeFromSpecialized(methodEntry);
			if (bridgeMethod != null && ownerClass.equals(bridgeMethod.getParent())) {
				Set<Entry<ClassEntry>> resolvedBridge = this.resolveChildEntry(bridgeMethod, strategy);
				if (!resolvedBridge.isEmpty()) {
					return resolvedBridge;
				} else {
					return Collections.singleton(bridgeMethod);
				}
			}
		}

		Set<Entry<ClassEntry>> resolvedEntries = new HashSet<>();

		for (ClassEntry parentClass : this.inheritanceIndex.getParents(ownerClass)) {
			Entry<ClassEntry> parentEntry = entry.withParent(parentClass);

			if (strategy == ResolutionStrategy.RESOLVE_ROOT) {
				resolvedEntries.addAll(this.resolveRoot(parentEntry, strategy));
			} else {
				resolvedEntries.addAll(this.resolveClosest(parentEntry, strategy));
			}
		}

		return resolvedEntries;
	}

	private Collection<Entry<ClassEntry>> resolveRoot(Entry<ClassEntry> entry, ResolutionStrategy strategy) {
		// When resolving root, we want to first look for the lowest entry before returning ourselves
		Set<Entry<ClassEntry>> parentResolution = this.resolveChildEntry(entry, strategy);

		if (parentResolution.isEmpty()) {
			AccessFlags parentAccess = this.entryIndex.getEntryAccess(entry);
			if (parentAccess != null && !parentAccess.isPrivate()) {
				return Collections.singleton(entry);
			}
		}

		return parentResolution;
	}

	private Collection<Entry<ClassEntry>> resolveClosest(Entry<ClassEntry> entry, ResolutionStrategy strategy) {
		// When resolving closest, we want to first check if we exist before looking further down
		AccessFlags parentAccess = this.entryIndex.getEntryAccess(entry);
		if (parentAccess != null && !parentAccess.isPrivate()) {
			return Collections.singleton(entry);
		} else {
			return this.resolveChildEntry(entry, strategy);
		}
	}

	@Override
	public Set<Entry<?>> resolveEquivalentEntries(Entry<?> entry) {
		MethodEntry relevantMethod = entry.findAncestor(MethodEntry.class);
		if (relevantMethod == null || !this.entryIndex.hasMethod(relevantMethod)) {
			return Collections.singleton(entry);
		}

		Set<MethodEntry> equivalentMethods = this.resolveEquivalentMethods(relevantMethod);
		Set<Entry<?>> equivalentEntries = new HashSet<>(equivalentMethods.size());

		for (MethodEntry equivalentMethod : equivalentMethods) {
			Entry<?> equivalentEntry = entry.replaceAncestor(relevantMethod, equivalentMethod);
			equivalentEntries.add(equivalentEntry);
		}

		return equivalentEntries;
	}

	@Override
	public Set<MethodEntry> resolveEquivalentMethods(MethodEntry methodEntry) {
		Set<MethodEntry> set = new HashSet<>();
		this.resolveEquivalentMethods(set, methodEntry);
		return set;
	}

	private void resolveEquivalentMethods(Set<MethodEntry> methodEntries, MethodEntry methodEntry) {
		AccessFlags access = this.entryIndex.getMethodAccess(methodEntry);
		if (access == null) {
			throw new IllegalArgumentException("Could not find method " + methodEntry);
		}

		if (!this.canInherit(methodEntry, access)) {
			methodEntries.add(methodEntry);
			return;
		}

		this.resolveEquivalentMethods(methodEntries, this.treeBuilder.buildMethodInheritance(VoidTranslator.INSTANCE, methodEntry));
	}

	private void resolveEquivalentMethods(Set<MethodEntry> methodEntries, MethodInheritanceTreeNode node) {
		MethodEntry methodEntry = node.getMethodEntry();
		if (methodEntries.contains(methodEntry)) {
			return;
		}

		AccessFlags flags = this.entryIndex.getMethodAccess(methodEntry);
		if (flags != null && this.canInherit(methodEntry, flags)) {
			// collect the entry
			methodEntries.add(methodEntry);
		}

		// look at bridge methods!
		MethodEntry bridgedMethod = this.bridgeMethodIndex.getBridgeFromSpecialized(methodEntry);
		while (bridgedMethod != null) {
			this.resolveEquivalentMethods(methodEntries, bridgedMethod);
			bridgedMethod = this.bridgeMethodIndex.getBridgeFromSpecialized(bridgedMethod);
		}

		// look at interface methods too
		for (MethodImplementationsTreeNode implementationsNode : this.treeBuilder.buildMethodImplementations(VoidTranslator.INSTANCE, methodEntry)) {
			this.resolveEquivalentMethods(methodEntries, implementationsNode);
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			this.resolveEquivalentMethods(methodEntries, (MethodInheritanceTreeNode) node.getChildAt(i));
		}
	}

	private void resolveEquivalentMethods(Set<MethodEntry> methodEntries, MethodImplementationsTreeNode node) {
		MethodEntry methodEntry = node.getMethodEntry();
		AccessFlags flags = this.entryIndex.getMethodAccess(methodEntry);
		if (flags != null && !flags.isPrivate() && !flags.isStatic()) {
			// collect the entry
			methodEntries.add(methodEntry);
		}

		// look at bridge methods!
		MethodEntry bridgedMethod = this.bridgeMethodIndex.getBridgeFromSpecialized(methodEntry);
		while (bridgedMethod != null) {
			this.resolveEquivalentMethods(methodEntries, bridgedMethod);
			bridgedMethod = this.bridgeMethodIndex.getBridgeFromSpecialized(bridgedMethod);
		}

		// recurse
		for (int i = 0; i < node.getChildCount(); i++) {
			this.resolveEquivalentMethods(methodEntries, (MethodImplementationsTreeNode) node.getChildAt(i));
		}
	}

	private boolean canInherit(MethodEntry entry, AccessFlags access) {
		return !entry.isConstructor() && !access.isPrivate() && !access.isStatic() && !access.isFinal();
	}
}
