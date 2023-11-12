package org.quiltmc.enigma.impl.translation.mapping.serde;

import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A builder class for creating an {@link EntryMapping}, used when reading mappings.
 */
public final class RawEntryMapping {
	private final String targetName;
	private final List<String> javadocs = new ArrayList<>();

	public RawEntryMapping(@Nullable String targetName) {
		this.targetName = targetName != null && !targetName.equals("-") ? targetName : null;
	}

	public void addJavadocLine(String line) {
		this.javadocs.add(line);
	}

	public EntryMapping bake() {
		return new EntryMapping(this.targetName, this.javadocs.isEmpty() ? null : String.join("\n", this.javadocs), this.targetName == null ? TokenType.OBFUSCATED : TokenType.DEOBFUSCATED, null);
	}
}
