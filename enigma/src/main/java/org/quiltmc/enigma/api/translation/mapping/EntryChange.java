package org.quiltmc.enigma.api.translation.mapping;

import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.util.TristateChange;

import java.util.Objects;

public final class EntryChange<E extends Entry<?>> {
	private final E target;
	private final TristateChange<String> deobfName;
	private final TristateChange<String> javadoc;

	private final TristateChange<TokenType> tokenType;
	private final TristateChange<String> sourcePluginId;

	private EntryChange(E target, TristateChange<String> deobfName, TristateChange<String> javadoc, TristateChange<TokenType> tokenType, TristateChange<String> sourcePluginId) {
		this.target = target;
		this.deobfName = deobfName;
		this.javadoc = javadoc;
		this.tokenType = tokenType;
		this.sourcePluginId = sourcePluginId;
	}

	public static <E extends Entry<?>> EntryChange<E> modify(E target) {
		return new EntryChange<>(target, TristateChange.unchanged(), TristateChange.unchanged(), TristateChange.unchanged(), TristateChange.unchanged());
	}

	public EntryChange<E> withDeobfName(String name) {
		return new EntryChange<>(this.target, TristateChange.set(name), this.javadoc, TristateChange.set(TokenType.DEOBFUSCATED), TristateChange.reset());
	}

	public EntryChange<E> clearDeobfName() {
		return new EntryChange<>(this.target, TristateChange.reset(), this.javadoc, TristateChange.set(TokenType.OBFUSCATED), TristateChange.reset());
	}

	public EntryChange<E> withJavadoc(String javadoc) {
		return new EntryChange<>(this.target, this.deobfName, TristateChange.set(javadoc), this.tokenType, this.sourcePluginId);
	}

	public EntryChange<E> clearJavadoc() {
		return new EntryChange<>(this.target, this.deobfName, TristateChange.reset(), this.tokenType, this.sourcePluginId);
	}

	public EntryChange<E> withTokenType(TokenType tokenType) {
		return new EntryChange<>(this.target, this.deobfName, this.javadoc, TristateChange.set(tokenType), this.sourcePluginId);
	}

	public EntryChange<E> withSourcePluginId(String id) {
		return new EntryChange<>(this.target, this.deobfName, this.javadoc, this.tokenType, TristateChange.set(id));
	}

	public EntryChange<E> clearSourcePluginId() {
		return new EntryChange<>(this.target, this.deobfName, this.javadoc, this.tokenType, TristateChange.reset());
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

	public TristateChange<TokenType> getTokenType() {
		return this.tokenType;
	}

	public TristateChange<String> getSourcePluginId() {
		return this.sourcePluginId;
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
