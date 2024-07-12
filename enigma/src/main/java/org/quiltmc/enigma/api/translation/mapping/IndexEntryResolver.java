package org.quiltmc.enigma.api.translation.mapping;

import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
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

	// todo stupid
	public IndexEntryResolver(JarIndex index) {
		this.entryIndex = index.getIndex(EntryIndex.class);
		this.inheritanceIndex = index.getIndex(InheritanceIndex.class);
		this.bridgeMethodIndex = index.getIndex(BridgeMethodIndex.class);

		this.treeBuilder = new IndexTreeBuilder(index);
	}

	/**
	 * Resolves an entry, which may or may not exist in the bytecode, up to a matching non-private entry definition, by
	 * travelling up the class ancestry until finding the matching entry or entries. In most cases, this means converting
	 * something like {@code ClassWithoutField#field} to {@code ClassWithField#field}, or {@code OverridingClass#toString}
	 * to {@code Object#toString}.
	 *
	 * <p>
	 * Private and/or static entries are always resolved as the entry itself. Only entries available in the index are
	 * returned. Matching entries are ones with the exact same name & descriptor.
	 *
	 * <p>
	 * The {@code strategy} doesn't affect the result unless the entry is a {@linkplain MethodEntry} or a child of one,
	 * i.e. a {@linkplain org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry LocalVariableEntry}.
	 * Using {@link ResolutionStrategy#RESOLVE_CLOSEST}, the method closest to {@code entry} in the hierarchy will be
	 * returned, while using {@link ResolutionStrategy#RESOLVE_ROOT} will return the matching methods at the root(s) of
	 * the hierarchy. This means, that overridden methods will be replaced by their highest possible definition, and if
	 * the {@code entry} overrides multiple different methods at the same time, all the definitions of those methods
	 * will be returned.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E extends Entry<?>> Collection<E> resolveEntry(E entry, ResolutionStrategy strategy) {
		if (entry == null) {
			return Collections.emptySet();
		}

		Entry<ClassEntry> entryAsClassChild = this.getClassChild(entry);
		if (entryAsClassChild != null && !(entryAsClassChild instanceof ClassEntry)) {
			AccessFlags access = this.entryIndex.getEntryAccess(entryAsClassChild);

			// If we're looking for the closest and this entry exists, we're done looking
			if (strategy == ResolutionStrategy.RESOLVE_CLOSEST && access != null) {
				return Collections.singleton(entry);
			}

			// Don't search existing private and/or static entries up the hierarchy
			// Fields and classes can't be redefined, don't search them up the hierarchy
			if (access != null && (access.isPrivate() || access.isStatic() || entry instanceof FieldEntry || entry instanceof ClassEntry)) {
				return Collections.singleton(entry);
			} else {
				// Search the entry up the hierarchy; if the entry exists we can skip static entries, since this one isn't static
				Collection<Entry<ClassEntry>> resolvedChildren = this.resolveEntryInAncestry(entryAsClassChild, strategy, access != null);
				if (!resolvedChildren.isEmpty()) {
					return resolvedChildren.stream()
						.map(resolvedChild -> (E) entry.replaceAncestor(entryAsClassChild, resolvedChild))
						.toList();
				} else if (access == null) {
					// No matching entry was found, and this one doesn't exist
					return Collections.emptySet();
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

	private Set<Entry<ClassEntry>> resolveEntryInAncestry(Entry<ClassEntry> entry, ResolutionStrategy strategy, boolean skipStatic) {
		ClassEntry ownerClass = entry.getParent();

		// Resolve specialized methods using their bridges
		if (entry instanceof MethodEntry methodEntry) {
			MethodEntry bridgeMethod = this.bridgeMethodIndex.getBridgeFromSpecialized(methodEntry);
			if (bridgeMethod != null && ownerClass.equals(bridgeMethod.getParent())) {
				Set<Entry<ClassEntry>> resolvedBridge = this.resolveEntryInAncestry(bridgeMethod, strategy, skipStatic);
				if (!resolvedBridge.isEmpty()) {
					return resolvedBridge;
				} else {
					return Collections.singleton(bridgeMethod);
				}
			}
		}

		Set<Entry<ClassEntry>> resolvedEntries = new HashSet<>();

		for (ClassEntry ancestorClass : this.inheritanceIndex.getParents(ownerClass)) {
			Entry<ClassEntry> ancestorChildEntry = entry.withParent(ancestorClass);

			if (strategy == ResolutionStrategy.RESOLVE_ROOT) {
				resolvedEntries.addAll(this.resolveRoot(ancestorChildEntry, strategy, skipStatic));
			} else {
				resolvedEntries.addAll(this.resolveClosest(ancestorChildEntry, strategy, skipStatic));
			}
		}

		return resolvedEntries;
	}

	private Collection<Entry<ClassEntry>> resolveRoot(Entry<ClassEntry> ancestorChildEntry, ResolutionStrategy strategy, boolean skipStatic) {
		// When resolving root, we want to first look for the lowest entry before returning ourselves
		Set<Entry<ClassEntry>> ancestorResolution = this.resolveEntryInAncestry(ancestorChildEntry, strategy, skipStatic);

		if (ancestorResolution.isEmpty()) {
			AccessFlags ancestorChildAccess = this.entryIndex.getEntryAccess(ancestorChildEntry);
			if (ancestorChildAccess != null && !ancestorChildAccess.isPrivate() && (!skipStatic || !ancestorChildAccess.isStatic())) {
				return Collections.singleton(ancestorChildEntry);
			}
		}

		return ancestorResolution;
	}

	private Collection<Entry<ClassEntry>> resolveClosest(Entry<ClassEntry> ancestorChildEntry, ResolutionStrategy strategy, boolean skipStatic) {
		// When resolving closest, we want to first check if we exist before looking further down
		AccessFlags ancestorChildAccess = this.entryIndex.getEntryAccess(ancestorChildEntry);
		if (ancestorChildAccess != null && !ancestorChildAccess.isPrivate() && (!skipStatic || !ancestorChildAccess.isStatic())) {
			return Collections.singleton(ancestorChildEntry);
		} else {
			return this.resolveEntryInAncestry(ancestorChildEntry, strategy, skipStatic);
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
