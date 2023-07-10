package cuchaz.enigma.translation.representation.entry;

import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.mapping.IdentifierValidation;
import cuchaz.enigma.utils.validation.ValidationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

public interface Entry<P extends Entry<?>> extends Translatable {
	/**
	 * Returns the default name of this entry.
	 *
	 * <p>For methods, fields and inner classes, it's the same as {@link #getSimpleName()}.</p>
	 * <p>For other classes, it's the same as {@link #getFullName()}.</p>
	 *
	 * <br><p>Examples:</p>
	 * <ul>
	 * 	<li>Outer class: "domain.name.ClassA"</li>
	 * 	<li>Inner class: "ClassB"</li>
	 * 	<li>Method: "methodC"</li>
	 * </ul>
	 */
	String getName();

	/**
	 * Returns the simple name of this entry.
	 *
	 * <p>For methods, fields and inner classes, it's the same as {@link #getName()}.</p>
	 * <p>For other classes, it's their name without the package name.</p>
	 *
	 * <br><p>Examples:</p>
	 * <ul>
	 * 	<li>Outer class: "ClassA"</li>
	 * 	<li>Inner class: "ClassB"</li>
	 * 	<li>Method: "methodC"</li>
	 * </ul>
	 */
	String getSimpleName();

	/**
	 * Returns the full name of this entry.
	 *
	 * <p>For methods, fields and inner classes, it's their name prefixed with the full name
	 * of their parent entry.</p>
	 * <p>For other classes, it's their name prefixed with their package name.</p>
	 *
	 * <br><p>Examples:</p>
	 * <ul>
	 * 	<li>Outer class: "domain.name.ClassA"</li>
	 * 	<li>Inner class: "domain.name.ClassA$ClassB"</li>
	 * 	<li>Method: "domain.name.ClassA.methodC"</li>
	 * </ul>
	 */
	String getFullName();

	/**
	 * Returns the contextual name of this entry.
	 *
	 * <p>For methods, fields and inner classes, it's their name prefixed with the contextual
	 * name of their parent entry.</p>
	 * <p>For other classes, it's only their simple name.</p>
	 *
	 * <br><p>Examples:</p>
	 * <ul>
	 * 	<li>Outer class: "ClassA"</li>
	 * 	<li>Inner class: "ClassA$ClassB"</li>
	 * 	<li>Method: "ClassA.methodC"</li>
	 * </ul>
	 */
	String getContextualName();

	String getJavadocs();

	default String getSourceRemapName() {
		return this.getName();
	}

	/**
	 * Returns the parent entry of this entry.
	 *
	 * <p>The parent entry should be a {@linkplain MethodEntry method} for local variables,
	 * a {@linkplain ClassEntry class} for methods, fields and inner classes, and {@code null}
	 * for other classes.</p>
	 */
	@Nullable
	P getParent();

	Class<P> getParentType();

	Entry<P> withName(String name);

	Entry<P> withParent(P parent);

	/**
	 * Determines whether this entry conflicts with the given entry.
	 * Conflicts are when two entries have the same name and will cause a compilation error when remapped.
	 */
	boolean canConflictWith(Entry<?> entry);

	/**
	 * Determines whether this entry <em>shadows</em> the given entry.
	 * Shadowing is when an entry from a child class has an identical name to an entry in its parent class,
	 * meaning that the parent entry is only accessible via a {@code Parent.this.entry} reference. Shadowing should
	 * emit a warning when committed since it will often cause unintended behavior.
	 *
	 * <pre>
	 * {@code
	 * public class D {
	 * 	 public int name;
	 * 	 public String a;
	 *
	 *  	public void b() {
	 *  	}
	 *
	 * 	 public class D.E {
	 * 		public int name;
	 *
	 * 		public void b() {
	 *         }
	 *  	}
	 *  }
	 *  }
	 *  </pre>
	 *  In this example, {@code D.E.name} shadows {@code D.name} and {@code D.E.b} shadows {@code D.b}.
	 *  This means that calling either one from {@code D.E} will use the shadowed entry instead of the original.
	 */
	boolean canShadow(Entry<?> entry);

	default ClassEntry getContainingClass() {
		ClassEntry last = null;
		Entry<?> current = this;
		while (current != null) {
			if (current instanceof ClassEntry classEntry) {
				last = classEntry;
				break;
			}

			current = current.getParent();
		}

		return Objects.requireNonNull(last, () -> String.format("%s has no containing class?", this));
	}

	default ClassEntry getTopLevelClass() {
		ClassEntry last = null;
		Entry<?> current = this;
		while (current != null) {
			if (current instanceof ClassEntry classEntry) {
				last = classEntry;
			}

			current = current.getParent();
		}

		return Objects.requireNonNull(last, () -> String.format("%s has no top level class?", this));
	}

	default List<Entry<?>> getAncestry() {
		P parent = this.getParent();
		List<Entry<?>> entries = new ArrayList<>();
		if (parent != null) {
			entries.addAll(parent.getAncestry());
		}

		entries.add(this);
		return entries;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	default <E extends Entry<?>> E findAncestor(Class<E> type) {
		List<Entry<?>> ancestry = this.getAncestry();
		for (int i = ancestry.size() - 1; i >= 0; i--) {
			Entry<?> ancestor = ancestry.get(i);
			if (type.isAssignableFrom(ancestor.getClass())) {
				return (E) ancestor;
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	default <E extends Entry<?>> Entry<P> replaceAncestor(E target, E replacement) {
		if (replacement.equals(target)) {
			return this;
		}

		if (this.equals(target)) {
			return (Entry<P>) replacement;
		}

		P parent = this.getParent();
		if (parent == null) {
			return this;
		}

		return this.withParent((P) parent.replaceAncestor(target, replacement));
	}

	default void validateName(ValidationContext vc, String name) {
		IdentifierValidation.validateIdentifier(vc, name);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	default <C extends Entry<?>> Entry<C> castParent(Class<C> parentType) {
		if (parentType.equals(this.getParentType())) {
			return (Entry<C>) this;
		}

		return null;
	}
}
