package org.quiltmc.enigma.api.translation.mapping;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.source.DecompiledClassSource;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.util.TristateChange;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

public final class EntryChange<E extends Entry<?>> {
	private final E target;
	private final TristateChange<String> deobfName;
	private final TristateChange<String> javadoc;

	private EntryChange(E target, TristateChange<String> deobfName, TristateChange<String> javadoc) {
		this.target = target;
		this.deobfName = deobfName;
		this.javadoc = javadoc;
	}

	public static <E extends Entry<?>> EntryChange<E> modify(E target) {
		return new EntryChange<>(target, TristateChange.unchanged(), TristateChange.unchanged());
	}

	public EntryChange<E> withDeobfName(String name) {
		return new EntryChange<>(this.target, TristateChange.set(name), this.javadoc);
	}

	public EntryChange<E> withDefaultDeobfName(@Nullable EnigmaProject project) {
		Optional<String> proposed = project != null ? DecompiledClassSource.proposeName(project, this.target) : Optional.empty();
		return this.withDeobfName(proposed.orElse(this.target.getName()));
	}

	public EntryChange<E> clearDeobfName() {
		return new EntryChange<>(this.target, TristateChange.reset(), this.javadoc);
	}

	public EntryChange<E> withJavadoc(String javadoc) {
		return new EntryChange<>(this.target, this.deobfName, TristateChange.set(javadoc));
	}

	public EntryChange<E> clearJavadoc() {
		return new EntryChange<>(this.target, this.deobfName, TristateChange.reset());
	}

	public TristateChange<String> getDeobfName() {
		return this.deobfName;
	}

	public TristateChange<String> getJavadoc() {
		return this.javadoc;
	}

	public E getTarget() {
		return this.target;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof EntryChange<?> that)) return false;
		return Objects.equals(this.target, that.target)
				&& Objects.equals(this.deobfName, that.deobfName)
				&& Objects.equals(this.javadoc, that.javadoc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.target, this.deobfName, this.javadoc);
	}

	@Override
	public String toString() {
		return String.format("EntryChange { target: %s, deobfName: %s, javadoc: %s }", this.target, this.deobfName, this.javadoc);
	}
}
