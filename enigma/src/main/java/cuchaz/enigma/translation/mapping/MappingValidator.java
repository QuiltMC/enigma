package cuchaz.enigma.translation.mapping;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import cuchaz.enigma.analysis.index.InheritanceIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ValidationContext;

import javax.annotation.Nullable;

public class MappingValidator {
	private final EntryTree<EntryMapping> obfToDeobf;
	private final Translator deobfuscator;
	private final JarIndex index;

	public MappingValidator(EntryTree<EntryMapping> obfToDeobf, Translator deobfuscator, JarIndex index) {
		this.obfToDeobf = obfToDeobf;
		this.deobfuscator = deobfuscator;
		this.index = index;
	}

	public void validateRename(ValidationContext vc, Entry<?> entry, String name) {
		Collection<Entry<?>> equivalentEntries = this.index.getEntryResolver().resolveEquivalentEntries(entry);
		boolean uniquenessIssue = false;

		for (Entry<?> equivalentEntry : equivalentEntries) {
			equivalentEntry.validateName(vc, name);
			if (!uniquenessIssue) {
				uniquenessIssue = this.validateUnique(vc, equivalentEntry, name);
			}
		}
	}

	/**
	 * @return whether an error or warning was raised
	 */
	private boolean validateUnique(ValidationContext vc, Entry<?> entry, String name) {
		ClassEntry containingClass = entry.getContainingClass();
		Collection<ClassEntry> relatedClasses = this.getRelatedClasses(containingClass);

		Entry<?> shadowedEntry;

		for (ClassEntry relatedClass : relatedClasses) {
			if (this.isStatic(entry) && relatedClass != containingClass) {
				// static entries can only conflict with entries in the same class
				continue;
			}

			Entry<?> relatedEntry = entry.replaceAncestor(containingClass, relatedClass);
			Entry<?> translatedEntry = this.deobfuscator.translate(relatedEntry);

			List<? extends Entry<?>> translatedSiblings = this.obfToDeobf.getSiblings(relatedEntry).stream()
					.filter(e -> !e.equals(entry)) // If the entry is a class, this could contain itself
					.map(this.deobfuscator::translate)
					.toList();

			if (!this.isUnique(translatedEntry, translatedSiblings, name)) {
				Entry<?> parent = translatedEntry.getParent();
				if (parent != null) {
					vc.raise(Message.NONUNIQUE_NAME_CLASS, name, parent);
				} else {
					vc.raise(Message.NONUNIQUE_NAME, name);
				}

				return true;
			} else if ((shadowedEntry = this.getShadowedEntry(translatedEntry, translatedSiblings, name)) != null) {
				Entry<?> parent = shadowedEntry.getParent();
				if (parent != null) {
					vc.raise(Message.SHADOWED_NAME_CLASS, name, parent);
				} else {
					vc.raise(Message.SHADOWED_NAME, name);
				}

				return true;
			}
		}

		return false;
	}

	private Collection<ClassEntry> getRelatedClasses(ClassEntry classEntry) {
		InheritanceIndex inheritanceIndex = this.index.getInheritanceIndex();

		Collection<ClassEntry> relatedClasses = new HashSet<>();
		relatedClasses.add(classEntry);
		relatedClasses.addAll(inheritanceIndex.getChildren(classEntry));
		relatedClasses.addAll(inheritanceIndex.getAncestors(classEntry));

		return relatedClasses;
	}

	private boolean isUnique(Entry<?> entry, List<? extends Entry<?>> siblings, String name) {
		for (Entry<?> sibling : siblings) {
			if (this.canConflict(entry, sibling) && sibling.getName().equals(name)) {
				return false;
			}
		}
		return true;
	}

	private boolean canConflict(Entry<?> entry, Entry<?> sibling) {
		return entry.canConflictWith(sibling);
	}

	@Nullable
	private Entry<?> getShadowedEntry(Entry<?> entry, List<? extends Entry<?>> siblings, String name) {
		for (Entry<?> sibling : siblings) {
			if (this.canShadow(entry, sibling) && sibling.getName().equals(name)) {
				return sibling;
			}
		}
		return null;
	}

	private boolean canShadow(Entry<?> entry, Entry<?> sibling) {
		return entry.canShadow(sibling);
	}

	private boolean isStatic(Entry<?> entry) {
		AccessFlags accessFlags = this.index.getEntryIndex().getEntryAccess(entry);
		return accessFlags != null && accessFlags.isStatic();
	}
}
