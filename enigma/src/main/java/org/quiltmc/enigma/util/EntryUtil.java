package org.quiltmc.enigma.util;

import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.mapping.EntryChange;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.util.validation.ValidationContext;

import javax.annotation.Nonnull;

public class EntryUtil {
	public static EntryMapping applyChange(ValidationContext vc, EntryRemapper remapper, EntryChange<?> change) {
		Entry<?> target = change.getTarget();
		EntryMapping prev = remapper.getDeobfMapping(target);
		EntryMapping mapping = EntryUtil.applyChange(prev, change);

		remapper.putMapping(vc, target, mapping);

		return mapping;
	}

	public static EntryMapping applyChange(@Nonnull EntryMapping self, EntryChange<?> change) {
		if (change.getDeobfName().isSet()) {
			self = self.withName(change.getDeobfName().getNewValue());
		} else if (change.getDeobfName().isReset()) {
			self = self.withName(null);
		}

		if (change.getJavadoc().isSet()) {
			self = self.withDocs(change.getJavadoc().getNewValue());
		} else if (change.getJavadoc().isReset()) {
			self = self.withDocs(null);
		}

		return self;
	}
}
