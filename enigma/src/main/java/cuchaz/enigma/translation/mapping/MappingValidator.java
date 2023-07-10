package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.translation.representation.entry.ParentedEntry;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ValidationContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

		// parameters must be special-cased
		if (entry instanceof LocalVariableEntry parameter) {
			return this.validateParameterUniqueness(context, name, parameter);
		}

		List<ParentedEntry<?>> siblings = new ArrayList<>(this.index.getChildrenByClass().get(containingClass));

		// add sibling classes
		if (entry instanceof ClassEntry) {
			siblings.addAll(this.index.getEntryIndex().getClasses().stream().filter(e -> {
				// filter by package
				if (name.contains("/")) {
					String packageName = e.getPackageName();
					String newPackage = name.substring(0, name.lastIndexOf('/'));
					return packageName.equals(newPackage);
				}

				return true;
			}).toList());
		}

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

		if (translatedEntry != null) {
			if (!this.isUnique(translatedEntry, siblings, name)) {
				this.raiseConflict(context, translatedEntry.getParent(), name, false);
				return true;
			} else {
				Entry<?> shadowedEntry = this.getShadowedEntry(translatedEntry, siblings, name);
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
			Iterator<LocalVariableEntry> iterator = parent.getParameterIterator(this.index.getEntryIndex(), this.deobfuscator);
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

	private boolean isUnique(Entry<?> entry, List<? extends Entry<?>> siblings, String name) {
		for (Entry<?> sibling : siblings) {
			if (entry.canConflictWith(sibling) && sibling.getName().equals(name)) {
				return false;
			}
		}

		return true;
	}

	@Nullable
	private Entry<?> getShadowedEntry(Entry<?> entry, List<? extends Entry<?>> siblings, String name) {
		for (Entry<?> sibling : siblings) {
			if (entry.canShadow(sibling)) {
				// ancestry check only contains obf names, so we need to translate to deobf just in case
				Set<ClassEntry> ancestors = this.index.getInheritanceIndex().getAncestors(entry.getContainingClass());
				ancestors.addAll(
					ancestors.stream()
						.map(this.deobfuscator::translate)
						.toList()
				);

				if (ancestors.contains(sibling.getContainingClass())) {
					AccessFlags siblingFlags = this.index.getEntryIndex().getEntryAccess(sibling);
					AccessFlags flags = this.index.getEntryIndex().getEntryAccess(entry);

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
