package cuchaz.enigma.translation.mapping.serde;

import cuchaz.enigma.translation.mapping.AccessModifier;
import cuchaz.enigma.translation.mapping.EntryMapping;

import java.util.ArrayList;
import java.util.List;

public final class RawEntryMapping {
	private final String targetName;
	private final AccessModifier access;
	private final List<String> javadocs = new ArrayList<>();

	public RawEntryMapping(String targetName) {
		this(targetName, AccessModifier.UNCHANGED);
	}

	public RawEntryMapping(String targetName, AccessModifier access) {
		this.access = access;
		this.targetName = targetName != null && !targetName.equals("-") ? targetName : null;
	}

	public void addJavadocLine(String line) {
		this.javadocs.add(line);
	}

	public EntryMapping bake() {
		return new EntryMapping(this.targetName, this.access, this.javadocs.isEmpty() ? null : String.join("\n", this.javadocs));
	}
}
