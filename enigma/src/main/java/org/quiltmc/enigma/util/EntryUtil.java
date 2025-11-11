package org.quiltmc.enigma.util;

import org.jspecify.annotations.NonNull;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryChange;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.util.validation.ValidationContext;

public class EntryUtil {
	public static EntryMapping applyChange(ValidationContext vc, EntryRemapper remapper, EntryChange<?> change) {
		Entry<?> target = change.getTarget();
		EntryMapping prev = remapper.getMapping(target);
		EntryMapping mapping = EntryUtil.applyChange(prev, change);

		remapper.putMapping(vc, target, mapping);

		return mapping;
	}

	/**
	 * Applies all changes to the given {@link EntryMapping}.
	 * Does not modify the original mapping.
	 * @param self the base mapping to apply changes to
	 * @param change the changes to make
	 * @return the updated mapping
	 */
	public static EntryMapping applyChange(@NonNull EntryMapping self, EntryChange<?> change) {
		// note: a bit more complicated than it needs to be, to avoid tripping over validation done on EntryMapping objects.
		// this is a necessary sacrifice!

		String name = self.targetName();
		String javadoc = self.javadoc();
		TokenType tokenType = self.tokenType();
		String sourcePluginId = self.sourcePluginId();

		if (change.getDeobfName().isSet()) {
			name = change.getDeobfName().getNewValue();
		} else if (change.getDeobfName().isReset()) {
			name = null;
		}

		if (change.getJavadoc().isSet()) {
			javadoc = change.getJavadoc().getNewValue();
		} else if (change.getJavadoc().isReset()) {
			javadoc = null;
		}

		if (change.getTokenType().isSet()) {
			tokenType = change.getTokenType().getNewValue();
		}

		if (change.getSourcePluginId().isSet()) {
			sourcePluginId = change.getSourcePluginId().getNewValue();
		} else if (change.getSourcePluginId().isReset()) {
			sourcePluginId = null;
		}

		return self.withName(name, tokenType, sourcePluginId).withJavadoc(javadoc);
	}

	public static <E extends Entry<?>> EntryChange<E> changeFromMapping(E entry, EntryMapping mapping) {
		EntryChange<E> change = EntryChange.modify(entry);

		if (mapping.targetName() != null) {
			change = change.withDeobfName(mapping.targetName());
		} else {
			change = change.clearDeobfName();
		}

		if (mapping.javadoc() != null) {
			change = change.withJavadoc(mapping.javadoc());
		} else {
			change = change.clearJavadoc();
		}

		change = change.withTokenType(mapping.tokenType());

		if (mapping.sourcePluginId() != null) {
			change = change.withSourcePluginId(mapping.sourcePluginId());
		} else {
			change = change.clearSourcePluginId();
		}

		return change;
	}
}
