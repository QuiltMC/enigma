package org.quiltmc.enigma.api.analysis;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.translation.Translatable;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMap;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.impl.EnigmaProjectImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class EntryReference<E extends Entry<?>, C extends Entry<?>> implements Translatable {
	private static final List<String> CONSTRUCTOR_NON_NAMES = Arrays.asList("this", "super", "static", "<init>", "<clinit>");
	public final E entry;
	public final C context;
	public final ReferenceTargetType targetType;
	private final boolean declaration; // if the ref goes to the decl of the item. when true context == null
	private final boolean sourceName;

	public static <E extends Entry<?>, C extends Entry<?>> EntryReference<E, C> declaration(E entry, String sourceName) {
		return new EntryReference<>(entry, sourceName, null, ReferenceTargetType.none(), true);
	}

	public EntryReference(E entry, String sourceName) {
		this(entry, sourceName, null);
	}

	public EntryReference(E entry, String sourceName, C context) {
		this(entry, sourceName, context, ReferenceTargetType.none());
	}

	public EntryReference(E entry, String sourceName, C context, ReferenceTargetType targetType) {
		this(entry, sourceName, context, targetType, false);
	}

	protected EntryReference(E entry, String sourceName, C context, ReferenceTargetType targetType, boolean declaration) {
		if (entry == null) {
			throw new IllegalArgumentException("Entry cannot be null!");
		}

		this.entry = entry;
		this.context = context;
		this.targetType = targetType;
		this.declaration = declaration;

		this.sourceName = sourceName != null && !sourceName.isEmpty()
				&& !(entry instanceof MethodEntry method && method.isConstructor() && CONSTRUCTOR_NON_NAMES.contains(sourceName));
	}

	public EntryReference(E entry, C context, EntryReference<E, C> other) {
		this.entry = entry;
		this.context = context;
		this.sourceName = other.sourceName;
		this.targetType = other.targetType;
		this.declaration = other.declaration;
	}

	public ClassEntry getLocationClassEntry() {
		if (this.context != null) {
			return this.context.getContainingClass();
		}

		return this.entry.getContainingClass();
	}

	public boolean isNamed() {
		return this.sourceName;
	}

	/**
	 * Returns whether this refers to the declaration of an entry.
	 */
	public boolean isDeclaration() {
		return this.declaration;
	}

	public Entry<?> getNameableEntry(EnigmaProject project) {
		if (this.entry instanceof MethodEntry method) {
			if (method.isConstructor()) {
				// renaming a constructor really means renaming the class
				return this.entry.getContainingClass();
			} else {
				final FieldEntry definiteComponent = ((EnigmaProjectImpl) project).getRecordIndexingService()
						.map(service -> service.getDefiniteComponentField(method))
						.orElse(null);

				if (definiteComponent != null) {
					return definiteComponent;
				}
			}
		}

		return this.entry;
	}

	@Override
	public int hashCode() {
		if (this.context != null) {
			return Objects.hash(this.entry.hashCode(), this.context.hashCode());
		}

		return this.entry.hashCode() ^ Boolean.hashCode(this.declaration);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EntryReference<?, ?> reference && this.equals(reference);
	}

	public boolean equals(EntryReference<?, ?> other) {
		return other != null
				&& Objects.equals(this.entry, other.entry)
				&& Objects.equals(this.context, other.context)
				&& this.declaration == other.declaration;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(this.entry);

		if (this.declaration) {
			buf.append("'s declaration");
			return buf.toString();
		}

		if (this.context != null) {
			buf.append(" called from ");
			buf.append(this.context);
		}

		if (this.targetType != null && this.targetType.getKind() != ReferenceTargetType.Kind.NONE) {
			buf.append(" on target of type ");
			buf.append(this.targetType);
		}

		return buf.toString();
	}

	@Override
	public TranslateResult<EntryReference<E, C>> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		return translator.extendedTranslate(this.entry).map(e -> new EntryReference<>(e, translator.translate(this.context), this));
	}
}
