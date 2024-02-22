package org.quiltmc.enigma.api.translation.mapping;

import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.mapping.MappingsIndex;
import org.quiltmc.enigma.api.analysis.index.mapping.PackageIndex;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.representation.AccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ParentedEntry;
import org.quiltmc.enigma.util.validation.Message;
import org.quiltmc.enigma.util.validation.ValidationContext;

import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MappingValidator {
	private final EntryResolver resolver;
	private final Translator deobfuscator;
	private final JarIndex jarIndex;
	private final MappingsIndex mappingsIndex;

	public MappingValidator(EntryResolver resolver, Translator deobfuscator, JarIndex jarIndex, MappingsIndex mappingsIndex) {
		this.resolver = resolver;
		this.deobfuscator = deobfuscator;
		this.jarIndex = jarIndex;
		this.mappingsIndex = mappingsIndex;
	}

	public void validateRename(ValidationContext vc, Entry<?> entry, String name) {
		PackageIndex packageIndex = this.mappingsIndex.getIndex(PackageIndex.class);
		if (entry instanceof ClassEntry) {
			String packageName = ClassEntry.getParentPackage(name);
			if (packageName != null && !packageIndex.getPackageNames().contains(packageName)) {
				vc.raise(Message.NEW_PACKAGE, packageName);
			}
		}

		Collection<Entry<?>> equivalentEntries = this.jarIndex.getEntryResolver().resolveEquivalentEntries(entry);
		boolean uniquenessIssue = false;

		for (Entry<?> equivalentEntry : equivalentEntries) {
			equivalentEntry.validateName(vc, name);
			if (!uniquenessIssue) {
				uniquenessIssue = this.validateUnique(vc, equivalentEntry, name);
			}
		}
	}

	/**
	 * Ensures that this name is unique: it is not used by any other entry with a signature similar enough to conflict.
	 * @return whether an error was raised
	 */
	private boolean validateUnique(ValidationContext context, Entry<?> entry, String name) {
		ClassEntry containingClass = entry.getContainingClass();
		Entry<?> translatedEntry = this.deobfuscator.translate(entry);

		// parameters must be special-cased
		if (entry instanceof LocalVariableEntry parameter) {
			return this.validateParameterUniqueness(context, name, parameter);
		}

		List<ParentedEntry<?>> siblings = new ArrayList<>(this.jarIndex.getChildrenByClass().get(containingClass));

		// add sibling classes
		if (entry instanceof ClassEntry classEntry) {
			siblings.addAll(this.jarIndex.getIndex(EntryIndex.class).getClasses().stream().filter(e -> {
				if (e.isInnerClass()) {
					return false;
				}

				// filter by package
				String packageName = e.getPackageName();
				String originalPackageName = classEntry.getPackageName();

				return (originalPackageName == null && packageName == null)
					|| (packageName != null && packageName.equals(originalPackageName));
			}).toList());
		}

		// add all ancestors
		for (ClassEntry ancestor : this.jarIndex.getIndex(InheritanceIndex.class).getAncestors(containingClass)) {
			siblings.addAll(this.jarIndex.getChildrenByClass().get(ancestor));
		}

		// remove equivalent entries -- this can sometimes happen and break mark as deobf/obf
		// noinspection all
		siblings.removeAll(this.resolver.resolveEquivalentEntries(entry));

		// collect deobfuscated versions
		Map<Entry<?>, Entry<?>> deobfSiblings = siblings.stream()
				.distinct() // May throw IllegalStateException otherwise
				.map(e -> new AbstractMap.SimpleEntry<>(e, this.deobfuscator.translate(e)))
				.collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, e -> e.getValue() != null ? e.getValue() : e.getKey()));

		if (translatedEntry != null) {
			if (!this.isUnique(translatedEntry, entry, deobfSiblings, name)) {
				this.raiseConflict(context, translatedEntry.getParent(), name, false);
				return true;
			} else {
				Entry<?> shadowedEntry = this.getShadowedEntry(translatedEntry, entry, deobfSiblings, name);
				if (shadowedEntry != null) {
					this.raiseConflict(context, shadowedEntry.getParent(), name, true);
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Ensures that the parameter's new name is not used by any other parameter of its parent method.
	 * @implNote currently, we cannot check against obfuscated parameter names, since parameters are not indexed
	 * @return whether the parameter's new name creates a conflict
	 */
	private boolean validateParameterUniqueness(ValidationContext context, String name, LocalVariableEntry parameter) {
		MethodEntry parent = parameter.getParent();
		if (parent != null) {
			Iterator<LocalVariableEntry> iterator = parent.getParameterIterator(this.jarIndex.getIndex(EntryIndex.class), this.deobfuscator);
			while (iterator.hasNext()) {
				if (iterator.next().getName().equals(name)) {
					this.raiseConflict(context, parent, name, false);
					return true;
				}
			}
		}

		return false;
	}

	private void raiseConflict(ValidationContext context, Entry<?> parent, String name, boolean shadow) {
		if (parent != null) {
			context.raise(shadow ? Message.SHADOWED_NAME_CLASS : Message.NON_UNIQUE_NAME_CLASS, name, parent);
		} else {
			context.raise(shadow ? Message.SHADOWED_NAME : Message.NON_UNIQUE_NAME, name);
		}
	}

	private boolean isUnique(Entry<?> entry, Entry<?> obfEntry, Map<Entry<?>, Entry<?>> siblings, String name) {
		// Methods need further checks
		if (entry instanceof MethodEntry methodEntry) {
			return this.isMethodUnique(methodEntry, obfEntry, siblings, name);
		}

		for (Map.Entry<Entry<?>, Entry<?>> siblingEntry : siblings.entrySet()) {
			Entry<?> deobfSibling = siblingEntry.getValue();
			Entry<?> obfSibling = siblingEntry.getKey();

			if ((entry.canConflictWith(deobfSibling) && deobfSibling.getName().equals(name) && doesNotMatch(entry, obfEntry, deobfSibling, obfSibling))
					|| (entry.canConflictWith(obfSibling) && obfSibling.getName().equals(name) && doesNotMatch(entry, obfEntry, obfSibling, obfSibling))) {
				return false;
			}
		}

		return true;
	}

	private boolean isMethodUnique(MethodEntry entry, Entry<?> obfEntry, Map<Entry<?>, Entry<?>> siblings, String name) {
		for (Map.Entry<Entry<?>, Entry<?>> siblingEntry : siblings.entrySet()) {
			Entry<?> sibling = siblingEntry.getValue();
			Entry<?> obfSibling = siblingEntry.getKey();

			if ((entry.canConflictWith(sibling) && sibling.getName().equals(name) && doesNotMatch(entry, obfEntry, sibling, obfSibling))
					|| (entry.canConflictWith(obfSibling) && obfSibling.getName().equals(name) && doesNotMatch(entry, obfEntry, obfSibling, obfSibling))) {
				AccessFlags siblingFlags = this.jarIndex.getIndex(EntryIndex.class).getEntryAccess(obfSibling);
				AccessFlags flags = this.jarIndex.getIndex(EntryIndex.class).getEntryAccess(obfEntry);

				boolean sameParent = (entry.getParent() != null && entry.getParent().equals(sibling.getParent()))
						|| (obfEntry.getParent() != null && entry.getParent().equals(sibling.getParent()));
				if (!sameParent && flags != null && siblingFlags != null) {
					// Methods from different parents don't conflict if they are both static or private
					if ((flags.isStatic() && siblingFlags.isStatic())
							|| (flags.isPrivate() && siblingFlags.isPrivate())) {
						continue;
					}
				}

				return false;
			}
		}

		return true;
	}

	private static boolean doesNotMatch(Entry<?> entry, Entry<?> obfEntry, Entry<?> deobfSibling, Entry<?> obfSibling) {
		return !entry.equals(obfSibling) && !entry.equals(deobfSibling) && !obfEntry.equals(deobfSibling) && !obfEntry.equals(obfSibling);
	}

	@Nullable
	private Entry<?> getShadowedEntry(Entry<?> entry, Entry<?> obfEntry, Map<Entry<?>, Entry<?>> siblings, String name) {
		for (Map.Entry<Entry<?>, Entry<?>> siblingEntry : siblings.entrySet()) {
			Entry<?> sibling = siblingEntry.getValue();
			Entry<?> obfSibling = siblingEntry.getKey();

			if (entry.canShadow(sibling) || entry.canShadow(obfSibling)) {
				// ancestry check only contains obf names, so we need to translate to deobf just in case
				Set<ClassEntry> ancestors = this.jarIndex.getIndex(InheritanceIndex.class).getAncestors(obfEntry.getContainingClass());
				ancestors.addAll(
						ancestors.stream()
						.map(this.deobfuscator::translate)
						.toList()
				);

				if (ancestors.contains(sibling.getContainingClass())) {
					AccessFlags siblingFlags = this.jarIndex.getIndex(EntryIndex.class).getEntryAccess(sibling);
					AccessFlags flags = this.jarIndex.getIndex(EntryIndex.class).getEntryAccess(obfEntry);

					if ((siblingFlags == null || (!siblingFlags.isPrivate() && siblingFlags.isStatic()))
							&& (flags == null || flags.isStatic())
							&& name.equals(sibling.getName())) {
						return sibling;
					}
				}
			}
		}

		return null;
	}
}
