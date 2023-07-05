package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.ParentedEntry;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ValidationContext;

import java.util.Collection;
import java.util.List;

public class MappingValidator {
	private final Translator deobfuscator;
	private final JarIndex index;

	public MappingValidator(Translator deobfuscator, JarIndex index) {
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
	 * Ensures that this name is unique: it is not used by any other entry with a signature similar enough to conflict.
	 * @return whether an error was raised
	 */
	private boolean validateUnique(ValidationContext context, Entry<?> entry, String name) {
		ClassEntry containingClass = entry.getContainingClass();
		Entry<?> translatedEntry = this.deobfuscator.translate(entry);
		List<ParentedEntry<?>> siblings = this.index.getChildrenByClass().get(containingClass);

		// todo: broken on parameters

		// add all ancestors
		for (ClassEntry ancestor : this.index.getInheritanceIndex().getAncestors(containingClass)) {
			siblings.addAll(this.index.getChildrenByClass().get(ancestor));
		}

		// add deobfuscated versions
		siblings.addAll(
			siblings.stream()
				.map(this.deobfuscator::translate)
				.toList()
		);

		if (translatedEntry != null && !this.isUnique(translatedEntry, siblings, name)) {
			this.raiseConflict(context, translatedEntry.getParent(), name);
			return true;
		}

		return false;
	}

	private void raiseConflict(ValidationContext context, Entry<?> parent, String name) {
		if (parent != null) {
			context.raise(Message.NONUNIQUE_NAME_CLASS, name, parent);
		} else {
			context.raise(Message.NONUNIQUE_NAME, name);
		}
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
}
